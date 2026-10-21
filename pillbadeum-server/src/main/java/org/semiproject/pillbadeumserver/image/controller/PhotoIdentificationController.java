package org.semiproject.pillbadeumserver.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.semiproject.pillbadeumserver.common.exception.*;
import org.semiproject.pillbadeumserver.common.file.FileStorage;
import org.semiproject.pillbadeumserver.image.domain.*;
import org.semiproject.pillbadeumserver.image.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/** REG-002~007의 상태 있는 사진 확인 API. 기존 /api/images 보조 API와 구분한다. */
@Tag(name = "사진으로 약 등록", description = "식별·본인 후보/사진 확인·다시 찍기")
@RestController
@RequestMapping("/api")
public class PhotoIdentificationController {
  private final PhotoIdentificationWorkflow workflow;
  private final ImageKeywordRegistrationService store;
  private final PhotoLifecycleService lifecycle;
  private final FileStorage files;

  public PhotoIdentificationController(
      PhotoIdentificationWorkflow workflow,
      ImageKeywordRegistrationService store,
      PhotoLifecycleService lifecycle,
      FileStorage files) {
    this.workflow = workflow;
    this.store = store;
    this.lifecycle = lifecycle;
    this.files = files;
  }

  @Operation(
      summary = "처방전·약봉투·알약 사진 식별",
      description = "처방전/약봉투 image 1장에서 여러 약 이름을 배열로 저장하고 각 이름별 공공데이터 후보를 조회합니다. 알약은 front/back 각 1장입니다.")
  @PostMapping(
      value = "/profiles/{profileId}/photo-identifications",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PhotoIdentificationResponse identify(
      @PathVariable long profileId,
      MultipartHttpServletRequest multipart,
      @RequestParam(defaultValue = "PRESCRIPTION") ImagePhotoKind kind,
      @RequestParam(required = false) MultipartFile image,
      @RequestParam(required = false) MultipartFile front,
      @RequestParam(required = false) MultipartFile back,
      @RequestParam(required = false) List<MultipartFile> images,
      @RequestParam(defaultValue = "false") boolean namesOnly) {
    List<MultipartFile> input;
    if (images != null) {
      if (image != null || front != null || back != null)
        throw new BusinessException(ErrorCode.INVALID_INPUT, "사진 입력 방식을 하나만 사용해 주세요.");
      input = images;
    } else if (kind == ImagePhotoKind.PILL) {
      if (image != null || front == null || back == null)
        throw new BusinessException(ErrorCode.INVALID_INPUT, "앞면과 뒷면 사진이 모두 필요해요.");
      input = List.of(front, back);
    } else {
      if (image == null || front != null || back != null)
        throw new BusinessException(ErrorCode.INVALID_INPUT, "처방전·약봉투 사진1장을 올려 주세요.");
      input = List.of(image);
    }
    // 같은 이름의 파일을 여러 번 보내면 단일 MultipartFile 바인딩은 첫 장만 선택한다.
    int uploadedCount = multipart.getMultiFileMap().values().stream().mapToInt(List::size).sum();
    if (uploadedCount != input.size())
      throw new BusinessException(ErrorCode.INVALID_INPUT, "사진 개수를 확인해 주세요.");
    return workflow.identify(profileId, kind, input, namesOnly);
  }

  @Operation(summary = "처방전·약봉투 약 이름만 추출하여 임시 저장",
      description = "본인 프로필의 photo.medicine_names에 약 이름 배열을 30분 임시 저장합니다. /1정 같은 수량을 제외하고 공공데이터 후보 검색은 호출하지 않습니다.")
  @PostMapping(value = "/profiles/{profileId}/photo-name-extractions",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PhotoIdentificationResponse extractMedicineNames(@PathVariable long profileId, MultipartHttpServletRequest multipart,
      @RequestParam(defaultValue = "PRESCRIPTION") ImagePhotoKind kind,
      @RequestParam(required = false) MultipartFile image,
      @RequestParam(required = false) List<MultipartFile> images) {
    return identify(profileId, multipart, kind, image, null, null, images, true);
  }

  @Operation(summary = "처방전·약봉투의 여러 약 이름 식별 및 약별 후보 조회",
      description = "모든 약 이름을 medicineNames 배열로 임시 저장하고 drugs 배열의 이름별 후보를 각각 조회합니다. 이름만 저장하려면 namesOnly=true를 지정합니다.")
  @PostMapping(value = "/images/profiles/{profileId}/prescription-photo/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public PhotoIdentificationResponse identifyPrescription(@PathVariable long profileId, MultipartHttpServletRequest multipart,
      @RequestParam(defaultValue = "PRESCRIPTION") ImagePhotoKind kind,
      @RequestParam(required = false) MultipartFile image,
      @RequestParam(required = false) List<MultipartFile> images,
      @RequestParam(defaultValue = "false") boolean namesOnly) {
    if (kind == ImagePhotoKind.PILL) throw new BusinessException(ErrorCode.INVALID_INPUT, "처방전 또는 약봉투 사진을 올려 주세요.");
    return identify(profileId, multipart, kind, image, null, null, images, namesOnly);
  }

  @Operation(summary = "본인 사진의 저장된 후보 재조회")
  @GetMapping("/photos/{photoId}/candidates")
  public PhotoIdentificationResponse candidates(@PathVariable long photoId) {
    return store.view(photoId);
  }

  @Operation(summary = "본인 확인용 원본 사진 조회")
  @GetMapping("/photos/{photoId}/image")
  public ResponseEntity<Resource> image(@PathVariable long photoId) {
    var photo = store.ownedImage(photoId);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(photo.mime()))
        .body(files.loadAsResource(photo.path(), FileStorage.DIR_TEMP));
  }

  @Operation(summary = "다시 찍기·나가기", description = "알약은 앞뒤 사진을 함께 지움. IO 실패 행은 만료 정리가 재시도함.")
  @DeleteMapping("/photos/{photoId}")
  public ResponseEntity<Void> delete(@PathVariable long photoId) {
    lifecycle.deleteOwned(photoId);
    return ResponseEntity.noContent().build();
  }
}
