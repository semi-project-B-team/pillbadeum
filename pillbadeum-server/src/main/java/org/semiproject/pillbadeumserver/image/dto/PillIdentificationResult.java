package org.semiproject.pillbadeumserver.image.dto;

import java.util.List;

/** 이미지에서 읽은 식별 문자와 그 문자·낱알 특징으로 검색한 후보 목록. */
public record PillIdentificationResult(
        String recognizedFrontText,
        String recognizedBackText,
        List<PillCandidate> candidates
) {
}
