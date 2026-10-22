package org.semiproject.pillbadeumserver.drug;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.semiproject.pillbadeumserver.drug.domain.DrugIngredient;
import org.semiproject.pillbadeumserver.common.exception.ExternalApiException;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;

class DrugIngredientTests {
  final JsonMapper json = new JsonMapper();

  @Test void koreanNamesAndCodeRemovalPreserveTheFullCompoundIngredient() {
    String korean = "한글복합성분".repeat(8);
    var row = json.valueToTree(Map.of("MAIN_ITEM_INGR", "[M123]" + korean + "|[M456]보조성분",
        "ITEM_INGR_NAME", "English alternative"));
    assertThat(DrugIngredient.fromOfficial(row)).isEqualTo(korean + ", 보조성분");
  }

  @Test void exactlyOneHundredCharactersArePreservedAndLongerTextIsNotSilentlyCut() {
    assertThat(DrugIngredient.forStorage("가".repeat(100))).hasSize(100);
    assertThatThrownBy(() -> DrugIngredient.forStorage("가".repeat(101))).isInstanceOf(ExternalApiException.class);
  }

  @Test void absentKoreanNameFallsBackToTheCompleteEnglishIngredient() {
    String english = "Compound ingredient longer than the previous thirty character column";
    assertThat(DrugIngredient.fromOfficial(json.valueToTree(Map.of("MAIN_ITEM_INGR", "", "ITEM_INGR_NAME", english)))).isEqualTo(english);
  }
}
