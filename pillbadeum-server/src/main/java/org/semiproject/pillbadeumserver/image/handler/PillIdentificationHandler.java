package org.semiproject.pillbadeumserver.image.handler;

import org.semiproject.pillbadeumserver.common.ai.OpenAiClient;
import org.semiproject.pillbadeumserver.common.exception.InvalidFileException;
import org.semiproject.pillbadeumserver.common.external.PublicDataClient;
import org.semiproject.pillbadeumserver.common.file.FileStorage;
import org.semiproject.pillbadeumserver.image.dto.PillCandidate;
import org.semiproject.pillbadeumserver.image.dto.PillIdentificationResult;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 이미지에서 식별 문자·낱알 특징을 읽고 공공데이터포털의 약 후보를 검색한다. */
@Component
public class PillIdentificationHandler {

    private static final String PILL_IDENTIFICATION_PATH =
            "/1471000/MdcinGrnIdntfcInfoService03/getMdcinGrnIdntfcInfoList03";

    private static final String INSTRUCTIONS = """
            당신은 의약품 낱알 식별 보조자입니다.
            사진에서 보이는 정보만 추출하세요. 약 이름을 추측하거나 만들어 내지 마세요.
            식별 문자는 대문자로 반환하고, 읽을 수 없으면 빈 문자열로 반환하세요.
            모양과 색상도 확실히 보일 때만 반환하세요.
            """;

    private final FileStorage fileStorage;
    private final OpenAiClient openAiClient;
    private final PublicDataClient publicDataClient;

    public PillIdentificationHandler(FileStorage fileStorage,
                                     OpenAiClient openAiClient,
                                     PublicDataClient publicDataClient) {
        this.fileStorage = fileStorage;
        this.openAiClient = openAiClient;
        this.publicDataClient = publicDataClient;
    }

    public PillIdentificationResult identify(MultipartFile frontImage, MultipartFile backImage) {
        fileStorage.validateImage(frontImage);
        if (backImage != null && !backImage.isEmpty()) {
            fileStorage.validateImage(backImage);
        }

        PillMark pillMark = extractPillMark(frontImage, backImage);
        return new PillIdentificationResult(
                pillMark.printFront(),
                pillMark.printBack(),
                searchCandidates(pillMark)
        );
    }

    private PillMark extractPillMark(MultipartFile frontImage, MultipartFile backImage) {
        List<String> images = new ArrayList<>();
        images.add(toDataUrl(frontImage));
        if (backImage != null && !backImage.isEmpty()) {
            images.add(toDataUrl(backImage));
        }

        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "printFront", Map.of("type", "string"),
                        "printBack", Map.of("type", "string"),
                        "shape", Map.of("type", "string"),
                        "color", Map.of("type", "string")
                ),
                "required", List.of("printFront", "printBack", "shape", "color"),
                "additionalProperties", false
        );

        JsonNode result = openAiClient.createJsonFromImages(
                INSTRUCTIONS,
                "첫 번째 사진은 앞면, 두 번째 사진이 있으면 뒷면입니다. 식별 문자와 낱알 모양, 색상을 추출하세요.",
                images,
                200,
                "pill_mark",
                schema
        );

        return new PillMark(
                result.path("printFront").asString(),
                result.path("printBack").asString(),
                result.path("shape").asString(),
                result.path("color").asString()
        );
    }

    private List<PillCandidate> searchCandidates(PillMark pillMark) {
        if (pillMark.printFront().isBlank() && pillMark.printBack().isBlank()
                && pillMark.shape().isBlank() && pillMark.color().isBlank()) {
            return List.of();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("print_front", blankToNull(pillMark.printFront()));
        params.put("print_back", blankToNull(pillMark.printBack()));
        params.put("drug_shape", blankToNull(pillMark.shape()));
        params.put("color_class1", blankToNull(pillMark.color()));
        params.put("pageNo", 1);
        params.put("numOfRows", 5);

        JsonNode body = publicDataClient.get(PILL_IDENTIFICATION_PATH, params);
        return PublicDataClient.items(body).stream()
                .map(item -> new PillCandidate(
                        item.path("ITEM_SEQ").asString(),
                        item.path("ITEM_NAME").asString(),
                        item.path("ENTP_NAME").asString(),
                        item.path("ITEM_IMAGE").asString(),
                        item.path("PRINT_FRONT").asString(),
                        item.path("PRINT_BACK").asString(),
                        item.path("DRUG_SHAPE").asString(),
                        item.path("COLOR_CLASS1").asString()
                ))
                .toList();
    }

    private String toDataUrl(MultipartFile image) {
        try {
            return OpenAiClient.toDataUrl(image.getBytes(), image.getContentType());
        } catch (IOException e) {
            throw new InvalidFileException("이미지 파일을 읽을 수 없습니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record PillMark(String printFront, String printBack, String shape, String color) {
    }
}
