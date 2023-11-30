/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ascopes.protobufmavenplugin.fixture.FileSystemTestSupport;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ProtoSourceResolver tests")
class ProtoSourceResolverTest extends FileSystemTestSupport {

  @DisplayName(".proto sources are detected and returned recursively")
  @Test
  void protoSourcesAreDetectedAndReturnedRecursively() throws IOException {
    // Given
    var expectedProtoFiles = List.of(
        givenFileExists("protobufs", "test.proto"),
        givenFileExists("protobufs", "org", "example", "user.proto"),
        givenFileExists("protobufs", "org", "example", "account.proto"),
        givenFileExists("protobufs", "org", "example", "message.proto"),
        givenFileExists("protobufs", "org", "example", "channel.proto"),
        givenFileExists("src", "main", "protobuf", "bot.proto"),
        givenFileExists("src", "main", "protobuf", "history.proto"),
        givenFileExists("src", "main", "protobuf", "nested-dir", "something_else.proto")
    );

    var sourceDirs = List.of(
        givenDirectoryExists("protobufs"),
        givenDirectoryExists("src", "main", "protobuf")
    );

    // When
    var actualProtoFiles = ProtoSourceResolver.resolve(sourceDirs);

    // Then
    assertThat(actualProtoFiles).containsExactlyInAnyOrderElementsOf(expectedProtoFiles);
  }

  @DisplayName("Files without the '.proto' extension get ignored")
  @ValueSource(strings = {
      "README.md",
      "foo.csv",
      "uppercase.PROTO",
      "mangledCase.PRoTo",
      "rawStream.pb",
      "this_has_no_file_extension",
  })
  @ParameterizedTest(name = "expect \"{0}\" to be ignored")
  void filesWithoutProtoExtensionGetIgnored(String ignoredFileName) throws IOException {
    // Given
    givenFileExists("protobufs", "org", "example", "user.proto");
    givenFileExists("protobufs", "org", "example", "account.proto");
    var ignoredFile1 = givenFileExists("protobufs", "org", "example", ignoredFileName);
    var ignoredFile2 = givenFileExists("junk-directory", ignoredFileName);
    var sourceDirs = List.of(
        givenDirectoryExists("protobufs"),
        givenDirectoryExists("junk-directory")
    );

    // When
    var actualProtoFiles = ProtoSourceResolver.resolve(sourceDirs);

    // Then
    assertThat(actualProtoFiles)
        .isNotEmpty()
        .doesNotContain(ignoredFile1, ignoredFile2);
  }

  @DisplayName("No exception is raised if no files are discovered")
  @Test
  void noExceptionIsRaisedIfNoFilesAreDiscovered() throws IOException {
    // Given
    givenFileExists("protobufs", "org", "example", "not-proto");
    var sourceDirs = List.of(givenDirectoryExists("protobufs"));

    // When
    var actualProtoFiles = ProtoSourceResolver.resolve(sourceDirs);

    // Then
    assertThat(actualProtoFiles).isEmpty();
  }

  @DisplayName("No exception is raised if a directory does not exist")
  @Test
  void noExceptionIsRaisedIfDirectoryDoesNotExist() throws IOException {
    // Given
    givenFileExists("protobufs", "org", "example", "foo.proto");
    var sourceDirs = List.of(
        givenDirectoryExists("protobufs"),
        givenDirectoryDoesNotExist("missing-dir")
    );

    // When
    var actualProtoFiles = ProtoSourceResolver.resolve(sourceDirs);

    // Then
    assertThat(actualProtoFiles).isNotEmpty();
  }
}

