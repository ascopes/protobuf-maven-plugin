/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.source;

import static io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem.linux;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


@DisplayName("ProtoFilePredicates tests")
class ProtoFilePredicatesTest {

  @DisplayName("Expect isProtoFile to return true if a file with a '*.proto' extension")
  @ValueSource(strings = {
      "foo/bar/baz/file.proto",
      "beep/boop/thing.PROTO",
      "do/ray/me/fah.pRoTo",
      "file-in-root.proto",
  })
  @ParameterizedTest(name = "\"{0}\" is a valid proto file")
  void expectIsProtoFileTrueIfFileWithProtoExtension(String pathString) {
    try (var fs = linux()) {
      // Given
      var path = fs.givenFileExists(pathString);

      // Then
      assertThat(ProtoFilePredicates.isProtoFile(path))
          .isTrue();
    }
  }

  @DisplayName("Expect isProtoFile to return true if a file symlink with a '*.proto' extension")
  @ValueSource(strings = {
      "foo/bar/baz/file.proto",
      "beep/boop/thing.PROTO",
      "do/ray/me/fah.pRoTo",
      "file-in-root.proto",
  })
  @ParameterizedTest(name = "\"{0}\" is a valid proto file")
  void expectIsProtoFileTrueIfFileSymlinkWithProtoExtension(String pathString) {
    try (var fs = linux()) {
      // Given
      var realPath = fs.givenFileExists("some-file");
      var linkPath = fs.givenSymbolicLinkExists(realPath, pathString);

      // Then
      assertThat(ProtoFilePredicates.isProtoFile(linkPath))
          .isTrue();
    }
  }

  @DisplayName("Expect isProtoFile to return false if a file with a '*.proto' extension")
  @ValueSource(strings = {
      "foo/bar/baz/dir.proto",
      "beep/boop/thing.PROTO",
      "do/ray/me/fah.pRoTo",
      "dir-in-root.proto",
  })
  @ParameterizedTest(name = "\"{0}\" is not a valid proto file")
  void expectIsProtoFileFalseIfDirectoryWithProtoExtension(String pathString) {
    try (var fs = linux()) {
      // Given
      var path = fs.givenDirectoryExists(pathString);

      // Then
      assertThat(ProtoFilePredicates.isProtoFile(path))
          .isFalse();
    }
  }

  @DisplayName("Expect isProtoFile to return false if a file without a '*.proto' extension")
  @ValueSource(strings = {
      "file.txt",
      "FILE.TXT",
      "file",
      "FILE",
      "proto.txt",
      "PROTO.TXT",
      ".proto.gz",
      ".PROTO.GZ",
      "file.proto.gz",
      "file.PROTO.GZ",
      "file.protobuf",
      "file.PROTOBUF",
  })
  @ParameterizedTest(name = "\"{0}\" is not a valid proto file")
  void expectIsProtoFileFalseIfFileWithoutProtoExtension(String pathString) {
    try (var fs = linux()) {
      // Given
      var path = fs.givenFileExists(pathString);

      // Then
      assertThat(ProtoFilePredicates.isProtoFile(path))
          .isFalse();
    }
  }

  @DisplayName("Expect isProtoFile to return false if the file does not exist")
  @ValueSource(strings = {
      "file.txt",
      "FILE.TXT",
      "file",
      "FILE",
      "proto.txt",
      "PROTO.TXT",
      ".proto.gz",
      ".PROTO.GZ",
      "file.proto.gz",
      "file.PROTO.GZ",
      "file.protobuf",
      "file.PROTOBUF",
      "foo/bar/baz/file.proto",
      "beep/boop/thing.PROTO",
      "do/ray/me/fah.pRoTo",
      "file-in-root.proto",
  })
  @ParameterizedTest(name = "\"{0}\" is not a valid proto file")
  void expectIsProtoFileFalseIfFileDoesNotExist(String pathString) throws IOException {
    try (var fs = linux()) {
      // Given
      var path = fs.givenFileExists(pathString);
      // But
      Files.delete(path);

      // Then
      assertThat(ProtoFilePredicates.isProtoFile(path))
          .isFalse();
    }
  }
}
