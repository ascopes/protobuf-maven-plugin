/*
 * Copyright (C) 2023 Ashley Scopes
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
package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("StringUtils tests")
class StringUtilsTest {
  @DisplayName(".pluralise(int, String) returns the expected results")
  @CsvSource({
      "0, cat, 0 cats",
      "1, cat, 1 cat",
      "2, cat, 2 cats",
      "2718281, cat, 2718281 cats",
  })
  @ParameterizedTest(name = "for quantity {0} and singular {1}, expect {2}")
  void pluraliseIntStringReturnsExpectedValue(int quantity, String singular, String expected) {
    // Then
    assertThat(StringUtils.pluralize(quantity, singular))
        .isEqualTo(expected);
  }

  @DisplayName(".pluralise(int, String, String) returns the expected results")
  @CsvSource({
      "0, cat, cats, 0 cats",
      "0, sheep, sheep, 0 sheep",
      "0, dependency, dependencies, 0 dependencies",
      "1, cat, cats, 1 cat",
      "1, sheep, sheep, 1 sheep",
      "1, dependency, dependencies, 1 dependency",
      "2, cat, cats, 2 cats",
      "2, sheep, sheep, 2 sheep",
      "2, dependency, dependencies, 2 dependencies",
      "2718281, cat, cats, 2718281 cats",
      "2718281, sheep, sheep, 2718281 sheep",
      "2718281, dependency, dependencies, 2718281 dependencies",
  })
  @ParameterizedTest(name = "for quantity {0}, singular {1}, and plural {2}, expect {3}")
  void pluraliseIntStringReturnsExpectedValue(
      int quantity,
      String singular,
      String plural,
      String expected
  ) {
    // Then
    assertThat(StringUtils.pluralize(quantity, singular, plural))
        .isEqualTo(expected);
  }

  @DisplayName(".quoted(String) returns the expected value")
  @MethodSource("quotedCases")
  @ParameterizedTest(name = "for {argumentSetName}")
  void quotedReturnsExpectedValue(@Nullable String input, String expectedOutput) {
    // When
    var actualOutput = StringUtils.quoted(input);

    // Then
    assertThat(actualOutput).isEqualTo(expectedOutput);
  }

  static Stream<Arguments> quotedCases() {
    return Stream.of(
        argumentSet("null input", null, "null"),
        argumentSet("empty input", "", "\"\""),
        argumentSet("blank input", "  \t\r\n ", "\"  \t\r\n \""),
        argumentSet("filled input", "flamboogins", "\"flamboogins\"")
    );
  }
}
