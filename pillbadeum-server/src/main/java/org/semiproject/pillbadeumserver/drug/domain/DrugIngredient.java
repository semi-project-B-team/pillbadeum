package org.semiproject.pillbadeumserver.drug.domain;

import org.semiproject.pillbadeumserver.common.exception.ExternalApiException;
import tools.jackson.databind.JsonNode;

/** 공식 성분의 한글 우선 해석과 SQL11의 100자 저장 계약을 공유함. 원문을 조용히 자르지 않음. */
public final class DrugIngredient {
  public static final int MAX_LENGTH = 100;
  private DrugIngredient() {}

  public static String fromOfficial(JsonNode row) {
    for (String field : java.util.List.of("MAIN_ITEM_INGR", "ITEM_INGR_NAME")) {
      for (var property : row.properties()) {
        if (!property.getKey().equalsIgnoreCase(field)) continue;
        String value = java.util.Arrays.stream(property.getValue().asString("").split("\\|"))
            .map(part -> part.replaceAll("\\[[^\\]]*]", "").strip()).filter(part -> !part.isBlank())
            .distinct().collect(java.util.stream.Collectors.joining(", "));
        if (!value.isBlank()) return forStorage(value);
      }
    }
    return null;
  }

  public static String forStorage(String value) {
    if (value != null && value.codePointCount(0, value.length()) > MAX_LENGTH)
      throw ExternalApiException.failed("공공데이터", "성분명이 저장 가능한 100자를 넘습니다. 원문 확인이 필요합니다.", null);
    return value;
  }
}
