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
package io.github.ascopes.protobufmavenplugin.sources.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FileFilter test")
class FileFilterTest {

  @CsvSource({
      "false, false, false",
      "false,  true, false",
      " true, false, false",
      " true,  true,  true",
  })
  @DisplayName(".and() returns a filter that only matches when both filters are satisfied ")
  @ParameterizedTest(name = "when predicates return {0} and {1}, expect an overall result of {2}")
  void andMatchesBothFiltersOnly(boolean left, boolean right, boolean expected) {
    // Given
    var leftMatcher = fileFilter(left);
    var rightMatcher = fileFilter(right);
    var root = Path.of(RandomFixtures.someBasicString());
    var file = Path.of(RandomFixtures.someBasicString());

    // Then
    assertThat(leftMatcher.and(rightMatcher).matches(root, file))
        .isEqualTo(expected);
    assertThat(rightMatcher.and(leftMatcher).matches(root, file))
        .isEqualTo(expected);
  }

  @DisplayName(".matches(String) performs the expected call on .matches(Path, Path)")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when .matches(Path, Path) returns {0}")
  void matchesStringPerformsExpectedCall(boolean expected) {
    // Given
    var wasCalled = new AtomicBoolean();
    var filter = fileFilter((root, file) -> {
      wasCalled.setRelease(true);
      assertThat(root).isEqualTo(Path.of(""));
      assertThat(file).isEqualTo(Path.of("foo", "bar", "baz", "bork.proto"));
      return expected;
    });

    // When
    var actual = filter.matches("foo/bar/baz/bork.proto");

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  static FileFilter fileFilter(BiPredicate<Path, Path> predicate) {
    return predicate::test;
  }

  static FileFilter fileFilter(boolean result) {
    return fileFilter((a, b) -> result);
  }
}
