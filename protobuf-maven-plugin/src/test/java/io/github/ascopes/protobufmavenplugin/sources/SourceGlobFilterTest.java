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
package io.github.ascopes.protobufmavenplugin.sources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SourceGlobFilter tests")
class SourceGlobFilterTest {

  @DisplayName("glob patterns match expected paths")
  @ParameterizedTest(name = "expect matching [{1}]/{2} against {0} to return {3}")
  @MethodSource("pathTestCases")
  void globPatternMatchesExpectedPaths(
      SourceGlobFilter filter,
      Path root,
      Path file,
      boolean expectedResult
  ) {
    // Then
    assertThat(filter.matches(root, file))
        .isEqualTo(expectedResult);
  }

  @DisplayName("glob patterns match expected strings")
  @ParameterizedTest(name = "expect matching \"{1}\" against {0} to return {2}")
  @MethodSource("stringTestCases")
  void globPatternMatchesExpectedStrings(
      SourceGlobFilter filter,
      String path,
      boolean expectedResult
  ) {
    // Then
    assertThat(filter.matches(path))
        .isEqualTo(expectedResult);
  }

  static Stream<Arguments> pathTestCases() {
    return Stream.of();
  }

  static Stream<Arguments> stringTestCases() {
    return Stream.of();
  }

}

