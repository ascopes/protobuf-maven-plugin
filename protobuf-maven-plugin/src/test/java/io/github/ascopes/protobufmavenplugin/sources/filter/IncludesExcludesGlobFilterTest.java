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
package io.github.ascopes.protobufmavenplugin.sources.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("IncludesExcludesGlobFilter tests")
class IncludesExcludesGlobFilterTest {

  @DisplayName("only expected paths are matched")
  @MethodSource("testCases")
  @ParameterizedTest(name = "{argumentSetName}")
  void onlyExpectedPathsAreMatched(
      IncludesExcludesGlobFilter filter,
      Function<Path, Path> pathSupplier,
      boolean expectedResult,
      @TempDir Path dir
  ) {
    // Given
    var path = pathSupplier.apply(dir);

    // Then
    assertThat(filter.matches(dir, path))
        .isEqualTo(expectedResult);
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
        argumentSet(
            "files are matched when no includes or excludes are present",
            new IncludesExcludesGlobFilter(List.of(), List.of()),
            pathSupplier("foo", "bar", "baz.png"),
            true
        ),
        argumentSet(
            "files with matching exclusions are excluded when no includes are present",
            new IncludesExcludesGlobFilter(
                List.of(),
                List.of(
                    "foo/*/bar.proto",
                    "i/like/cats.png",
                    "this/should/*/be/excluded"
                )
            ),
            pathSupplier("this", "should", "really", "be", "excluded"),
            false
        ),
        argumentSet(
            "files with no matching exclusions are included when no includes are present",
            new IncludesExcludesGlobFilter(
                List.of(),
                List.of(
                    "foo/*/bar.proto",
                    "i/like/cats.png",
                    "this/should/*/be/excluded"
                )
            ),
            pathSupplier("this", "should", "really", "be", "included"),
            true
        ),
        argumentSet(
            "files with matching exclusions and inclusions are excluded",
            new IncludesExcludesGlobFilter(
                List.of(
                    "this/should/*/be/*",
                    "ba/ba/ba/ba/ba/da/da/da!!!!"
                ),
                List.of(
                    "foo/*/bar.proto",
                    "i/like/cats.png",
                    "this/should/*/be/*"
                )
            ),
            pathSupplier("this", "should", "really", "be", "excluded"),
            false
        ),
        argumentSet(
            "files with no exclusions and matching inclusions are included",
            new IncludesExcludesGlobFilter(
                List.of(
                    "this/should/*/be/*",
                    "ba/ba/ba/ba/ba/da/da/da!!!!"
                ),
                List.of()
            ),
            pathSupplier("this", "should", "really", "be", "included"),
            true
        ),
        argumentSet(
            "files with no exclusions and no matching inclusions are excluded",
            new IncludesExcludesGlobFilter(
                List.of(
                    "ba/ba/ba/ba/ba/da/da/da!!!!"
                ),
                List.of()
            ),
            pathSupplier("this", "should", "really", "be", "excluded"),
            false
        ),
        argumentSet(
            "files with no matching exclusions and matching inclusions are included",
            new IncludesExcludesGlobFilter(
                List.of(
                    "this/should/*/be/*"
                ),
                List.of(
                    "ba/ba/ba/ba/ba/da/da/da!!!!"
                )
            ),
            pathSupplier("this", "should", "really", "be", "included"),
            true
        ),
        argumentSet(
            "files with no matching exclusions and no matching inclusions are excluded",
            new IncludesExcludesGlobFilter(
                List.of(
                    "include/this/please"
                ),
                List.of(
                    "exclude/this/please"
                )
            ),
            pathSupplier("this", "should", "really", "be", "excluded"),
            false
        )
    );
  }

  static Function<Path, Path> pathSupplier(String... bits) {
    return path -> {
      for (var bit : bits) {
        path = path.resolve(bit);
      }
      return path;
    };
  }
}
