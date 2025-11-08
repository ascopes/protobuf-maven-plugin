/*
 * Copyright (C) 2023 - 2025, Ashley Scopes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
