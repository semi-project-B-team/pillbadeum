package org.semiproject.pillbadeumserver.image.controller;



import org.semiproject.pillbadeumserver.common.ResponseMessage;
import org.semiproject.pillbadeumserver.common.web.PreventDuplicateRequest;
import org.semiproject.pillbadeumserver.image.dto.PillIdentificationResult;
import org.semiproject.pillbadeumserver.image.handler.PillIdentificationHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final PillIdentificationHandler pillIdentificationHandler;

    public ImageController(PillIdentificationHandler pillIdentificationHandler) {
        this.pillIdentificationHandler = pillIdentificationHandler;
    }

    @PreventDuplicateRequest
    @PostMapping(value = "/pills/identify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseMessage> identifyPill(
        @RequestPart("frontImage") MultipartFile frontImage,
        @RequestPart(value = "backImage", required = false) MultipartFile backImage
    ) {
        PillIdentificationResult result = pillIdentificationHandler.identify(frontImage, backImage);
        return ResponseMessage.of(HttpStatus.OK,
            "식별 가능한 약 후보를 찾았습니다. 약이 맞는지 확인해 주세요.",
            Map.of("identification", result));
    }
}
