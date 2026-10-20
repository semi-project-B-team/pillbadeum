package org.semiproject.pillbadeumserver.image.service;

import org.semiproject.pillbadeumserver.common.ai.OpenAiClient;
import org.semiproject.pillbadeumserver.common.exception.BusinessException;
import org.semiproject.pillbadeumserver.common.exception.ErrorCode;
import org.semiproject.pillbadeumserver.common.exception.ExternalApiException;
import org.semiproject.pillbadeumserver.common.file.FileStorage;
import org.semiproject.pillbadeumserver.common.security.SecurityUtil;
import org.semiproject.pillbadeumserver.drug.service.ProfileAccessChecker;
import org.semiproject.pillbadeumserver.image.domain.PillIdentification;
import org.semiproject.pillbadeumserver.image.domain.PillIdentificationCandidate;
import org.semiproject.pillbadeumserver.image.domain.PillPhotoIdentificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 알약 사진을 임시 저장하고 각인/외형을 읽어 공공데이터 후보로 변환합니다. */
@Service
public class PillPhotoIdentificationService {

    private static final Logger log = LoggerFactory.getLogger(PillPhotoIdentificationService.class);
    private static final String IMG_TEMP_DIRECTORY = "img_temp";
    private static final int MAX_PHOTOS = 2;
    private static final int MAX_IMAGE_OUTPUT_TOKENS = 300;
    private static final long IMAGE_READING_TIMEOUT_SECONDS = 20;
    private static final long CANDIDATE_LOOKUP_TIMEOUT_SECONDS = 15;
    private static final Map<String, Object> SCHEMA = schema();
    private static final String INSTRUCTIONS = """
            사진에 있는 알약의 식별 정보를 읽으세요. 사진이 알약이 아니면 모든 문자열 필드를 빈 문자열로 반환하세요.
            약품명과 제조사명은 사진에서 명확히 읽히는 경우에만 적고, 추측하지 마세요.
            인쇄 문자는 보이는 그대로 옮기고, 앞면과 뒷면은 업로드 순서(첫 번째 사진=앞면, 두 번째 사진=뒷면)를 따르세요.
            모양/색상은 이미지에서 확인 가능한 값만 기록하세요. 불확실한 값은 빈 문자열로 반환하세요.
            """;

    private final FileStorage fileStorage;
    private final OpenAiClient openAiClient;
    private final PillDrugIdentificationService identificationService;
    private final ImageKeywordRegistrationService keywordRegistrationService;
    private final ProfileAccessChecker profileAccessChecker;

    public PillPhotoIdentificationService(FileStorage fileStorage,
                                          OpenAiClient openAiClient,
                                          PillDrugIdentificationService identificationService,
                                          ImageKeywordRegistrationService keywordRegistrationService,
                                          ProfileAccessChecker profileAccessChecker) {
        this.fileStorage = fileStorage;
        this.openAiClient = openAiClient;
        this.identificationService = identificationService;
        this.keywordRegistrationService = keywordRegistrationService;
        this.profileAccessChecker = profileAccessChecker;
    }

    public PillPhotoIdentificationResponse identify(Long profileId, List<MultipartFile> images) {
        SecurityUtil.getCurrentMemberId();
        if (profileId != null) {
            profileAccessChecker.checkOwner(profileId);
        }
        if (images == null || images.isEmpty() || images.size() > MAX_PHOTOS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알약 사진은 앞면과 뒷면을 최대 2장까지 올려 주세요.");
        }

        List<String> savedNames = new ArrayList<>();
        boolean registered = false;
        try {
            List<String> dataUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                String savedName = fileStorage.saveImage(image, IMG_TEMP_DIRECTORY);
                savedNames.add(savedName);
                dataUrls.add(OpenAiClient.toDataUrl(
                        fileStorage.readBytes(savedName, IMG_TEMP_DIRECTORY),
                        fileStorage.getMimeType(savedName)));
            }

            JsonNode output = readPillPhotoWithinTimeout(dataUrls);
            PillIdentification extracted = toIdentification(output);
            if (!hasAttributes(extracted)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "알약 정보를 읽지 못했습니다. 알약 앞면이 선명하게 보이도록 다시 촬영해 주세요.");
            }

            List<PillIdentificationCandidate> candidates;
            String lookupStatus;
            try {
                candidates = identifyCandidatesWithinTimeout(extracted);
                lookupStatus = candidates.isEmpty() ? "NOT_FOUND" : "OK";
            } catch (BusinessException exception) {
                candidates = List.of();
                lookupStatus = "FAILED";
            }

            // profileId 없이 Swagger 등에서 실행하는 식별 요청은 결과 조회만 수행한다.
            // 이 경로에서 사진 이력을 저장하려고 DB 연결을 기다리면, 식별이 이미 끝난 뒤에도
            // HTTP 응답이 지연될 수 있다. 프로필을 지정한 요청만 기존처럼 이력을 저장한다.
            List<Long> photoIds = List.of();
            if (profileId != null) {
                photoIds = keywordRegistrationService.registerPillKeywords(
                        profileId, images, savedNames, extracted, candidates, lookupStatus);
                registered = true;
            }
            return new PillPhotoIdentificationResponse(extracted, candidates, photoIds, lookupStatus);
        } finally {
            if (!registered) {
                savedNames.forEach(name -> fileStorage.delete(name, IMG_TEMP_DIRECTORY));
            }
        }
    }

    /**
     * 사진 판독(OpenAI)은 성공했는데 공공데이터 상세 조회가 느린 경우에도 HTTP 요청을 끝낸다.
     * 후보 정보는 부가 정보이므로 시간 초과 시 사진 판독 결과를 버리지 않고 FAILED 상태로 돌려준다.
     */
    private List<PillIdentificationCandidate> identifyCandidatesWithinTimeout(PillIdentification extracted) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<List<PillIdentificationCandidate>> task = executor.submit(() -> identificationService.identify(extracted));
        try {
            return task.get(CANDIDATE_LOOKUP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            task.cancel(true);
            log.warn("[알약 사진 식별] MFDS 후보 조회가 {}초를 초과했습니다.",
                    CANDIDATE_LOOKUP_TIMEOUT_SECONDS);
            throw ExternalApiException.timeout("MFDS Public Data", timeout);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw ExternalApiException.timeout("MFDS Public Data", interrupted);
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ExternalApiException.failed("MFDS Public Data", "후보 조회를 완료할 수 없습니다.", cause);
        } finally {
            // close()는 실행 중인 작업의 종료를 기다릴 수 있으므로, 응답 경로에서는 기다리지 않는다.
            executor.shutdownNow();
        }
    }

    /**
     * HTTP 클라이언트의 개별 read timeout과 별개로 사진 판독 전체에도 상한을 둔다.
     * 외부 연결이 라이브러리 수준에서 끝나지 않아도 API 요청이 무한 대기하지 않게 한다.
     */
    private JsonNode readPillPhotoWithinTimeout(List<String> dataUrls) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<JsonNode> task = executor.submit(() -> openAiClient.createJsonFromImages(
                INSTRUCTIONS,
                "알약 사진을 식별하고 다음 필드에 정보를 채우세요. 사진 순서는 업로드 순서입니다.",
                dataUrls, MAX_IMAGE_OUTPUT_TOKENS, "pill_image_reading", SCHEMA));
        try {
            return task.get(IMAGE_READING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            task.cancel(true);
            log.warn("[알약 사진 식별] OpenAI 사진 판독이 {}초를 초과했습니다.", IMAGE_READING_TIMEOUT_SECONDS);
            throw ExternalApiException.timeout("OpenAI", timeout);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw ExternalApiException.timeout("OpenAI", interrupted);
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ExternalApiException.failed("OpenAI", "사진 판독을 완료할 수 없습니다.", cause);
        } finally {
            executor.shutdownNow();
        }
    }

    private static PillIdentification toIdentification(JsonNode output) {
        return new PillIdentification(
                value(output, "itemName"),
                value(output, "entpName"),
                value(output, "printFront"),
                value(output, "printBack"),
                value(output, "shape"),
                value(output, "colorClass1"),
                value(output, "colorClass2"),
                value(output, "lineFront"),
                value(output, "lineBack"));
    }

    private static String value(JsonNode node, String key) {
        String value = node.path(key).asString("").strip();
        return value.isEmpty() ? null : value;
    }

    private static boolean hasAttributes(PillIdentification input) {
        return input.itemName() != null || input.entpName() != null || input.printFront() != null
                || input.printBack() != null || input.shape() != null || input.colorClass1() != null
                || input.colorClass2() != null || input.lineFront() != null || input.lineBack() != null;
    }

    private static Map<String, Object> schema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : List.of("itemName", "entpName", "printFront", "printBack", "shape",
                "colorClass1", "colorClass2", "lineFront", "lineBack")) {
            properties.put(field, Map.of("type", "string"));
        }
        return Map.of("type", "object", "properties", properties,
                "required", List.copyOf(properties.keySet()), "additionalProperties", false);
    }
}
