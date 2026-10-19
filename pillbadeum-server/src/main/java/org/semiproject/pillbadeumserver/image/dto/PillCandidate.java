package org.semiproject.pillbadeumserver.image.dto;

/** 공공데이터 API에서 찾은 사용자 확인용 약 후보. */
public record PillCandidate(
        String itemSeq,
        String name,
        String maker,
        String imageUrl,
        String printFront,
        String printBack,
        String shape,
        String color
) {
}
