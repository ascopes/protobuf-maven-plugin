package io.github.ascopes.protobufmavenplugin.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Language tests")
class LanguageTest {

  @CsvSource({
      "        JAVA,   --java_out=",
      "      KOTLIN, --kotlin_out=",
      "      PYTHON, --python_out=",
      "PYTHON_STUBS,    --pyi_out=",
      "        RUBY,   --ruby_out=",
  })
  @DisplayName("Language enum members have the expected output flag format")
  @ParameterizedTest(name = "for {0}")
  void languageEnumMembersHaveExpectedOutputFlagFormat(Language language, String expectedFlag) {
    // When
    var actualFlag = "--%s_out=".formatted(language.getFlagName());

    // Then
    assertThat(actualFlag).isEqualTo(expectedFlag);
  }

  @DisplayName("LanguageSetBuilder creates an enum of the enabled languages")
  @Test
  void languageSetBuilderCreatesEnumOfEnabledLanguages() {
    // When
    var enumSet = Language.setBuilder()
        .addIf(true, Language.PYTHON_STUBS)
        .addIf(true, Language.PYTHON)
        .addIf(false, Language.RUBY)
        .addIf(true, Language.JAVA)
        .build();

    // Then
    assertThat(enumSet)
        .containsExactlyInAnyOrder(Language.PYTHON_STUBS, Language.PYTHON, Language.JAVA);
  }
}
