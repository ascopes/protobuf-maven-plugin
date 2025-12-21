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
package io.github.ascopes.protobufmavenplugin.sources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments.ArgumentSet;
import org.junit.jupiter.params.provider.MethodSource;


@DisplayName("FilesToCompile tests")
class FilesToCompileTest {

  @DisplayName(".isEmpty() returns the expected value")
  @MethodSource("isEmptyTestCases")
  @ParameterizedTest(name = "such that {argumentSetName}")
  void isEmptyReturnsExpectedValue(
      Collection<String> descriptorFiles,
      Collection<Path> protoSources,
      boolean expected
  ) {
    // Given
    var files = ImmutableFilesToCompile.builder()
        .descriptorFiles(descriptorFiles)
        .protoSources(protoSources)
        .build();

    // Then
    assertThat(files.isEmpty())
        .as(".isEmpty()")
        .isEqualTo(expected);
  }

  @DisplayName(".allOf() returns the expected value")
  @Test
  void allOfReturnsTheExpectedValue() {
    // Given
    var listing = ImmutableProjectInputListing.builder()
        .compilableProtoSources(List.of(
            ImmutableSourceListing.builder()
                .sourceRoot(Path.of("foo", "bar"))
                .sourceFiles(List.of(
                    Path.of("foo", "bar", "baz.proto"),
                    Path.of("foo", "bar", "bork.proto")
                ))
                .build(),
            ImmutableSourceListing.builder()
                .sourceRoot(Path.of("eggs", "spam"))
                .sourceFiles(List.of(
                    Path.of("eggs", "spam", "baz.proto"),
                    Path.of("eggs", "spam", "bork.proto")
                ))
                .build()
        ))
        .compilableDescriptorFiles(List.of(
            ImmutableDescriptorListing.builder()
                .descriptorFilePath(Path.of("aaa", "bbb", "ccc.binpb"))
                .sourceFiles(List.of(
                    "aaa/bbb/ccc.proto"
                ))
                .build(),
            ImmutableDescriptorListing.builder()
                .descriptorFilePath(Path.of("aaa", "bbb", "ddd.binpb"))
                .sourceFiles(List.of(
                    "aaa/bbb/ddd.proto"
                ))
                .build()
        ))
        .dependencyProtoSources(List.of(
            ImmutableSourceListing.builder()
                .sourceRoot(Path.of("this", "should", "be", "ignored"))
                .sourceFiles(List.of(
                    Path.of("this", "should", "be", "ignored", "baz.proto")
                ))
                .build()
        ))
        .build();

    // When
    var files = FilesToCompile.allOf(listing);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(files.getDescriptorFiles())
          .as(".getDescriptorFiles()")
          .containsExactlyInAnyOrder(
              "aaa/bbb/ccc.proto",
              "aaa/bbb/ddd.proto"
          );
      softly.assertThat(files.getProtoSources())
          .as(".getProtoSources()")
          .containsExactlyInAnyOrder(
              Path.of("foo", "bar", "baz.proto"),
              Path.of("foo", "bar", "bork.proto"),
              Path.of("eggs", "spam", "baz.proto"),
              Path.of("eggs", "spam", "bork.proto")
          );
    });
  }

  @DisplayName(".empty() returns an empty instance")
  @Test
  void emptyReturnsAnEmptyInstance() {
    // When
    var files = FilesToCompile.empty();

    // Then
    assertSoftly(softly -> {
      softly.assertThat(files.getDescriptorFiles())
          .as(".getDescriptorFiles()")
          .isEmpty();
      softly.assertThat(files.getProtoSources())
          .as(".getProtoSources()")
          .isEmpty();
    });
  }

  static Stream<ArgumentSet> isEmptyTestCases() {
    return Stream.of(
        argumentSet(
            "no descriptors or protos <==> empty",
            List.of(),
            List.of(),
            true
        ),
        argumentSet(
            "protos without descriptors <==> not empty",
            List.of(),
            List.of(Path.of("foo", "bar", "baz.proto")),
            false
        ),
        argumentSet(
            "descriptors without protos <==> not empty",
            List.of(Path.of("foo", "bar", "baz.binpb")),
            List.of(),
            false
        ),
        argumentSet(
            "descriptors and protos <==> not empty",
            List.of(Path.of("foo", "bar", "baz.binpb")),
            List.of(Path.of("foo", "bar", "bork.proto")),
            false
        )
    );
  }
}
