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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ProtoFileGlobFilter tests")
class ProtoFileGlobFilterTest {

  @DisplayName("Expect no match if the file does not exist")
  @Test
  void expectNoMatchIfFileDoesNotExist(@TempDir Path dir) {
    // Given
    var file = dir.resolve("foo").resolve("bar").resolve("baz.proto");
    var filter = new ProtoFileGlobFilter();

    // Then
    assertThat(filter.matches(dir, file)).isFalse();
  }

  @DisplayName("Expect no match if the path is not a file")
  @Test
  void expectNoMatchIfPathIsNotFile(@TempDir Path dir) throws IOException {
    // Given
    var nonFile = dir.resolve("foo").resolve("bar").resolve("baz.proto");
    Files.createDirectories(nonFile);
    var filter = new ProtoFileGlobFilter();

    // Then
    assertThat(filter.matches(dir, nonFile)).isFalse();
  }

  @DisplayName("Expect no match if the file lacks a proto file extension")
  @ValueSource(strings = {
      "foo.png",
      "bar.txt",
      "users.proto.gz",
      "thishasnofileextension",
  })
  @ParameterizedTest(name = "for {0}")
  void expectNoMatchIfFileLacksProtoFileExtension(
      String fileName,
      @TempDir Path dir
  ) throws IOException {
    // Given
    var baseDir = dir.resolve("foo").resolve("bar");
    Files.createDirectories(baseDir);
    var file = baseDir.resolve(fileName);
    Files.createFile(file);
    var filter = new ProtoFileGlobFilter();

    // Then
    assertThat(filter.matches(dir, file)).isFalse();
  }

  @DisplayName("Expect a match if a proto file extension is present and the file exists")
  @ValueSource(strings = {
      "cat.proto",
      "dog.PROTO",
      "mouse.pRoTo",
  })
  @ParameterizedTest(name = "for {0}")
  void expectMatchIfProtoFileExtensionIsPresentAndTheFileExists(
      String fileName,
      @TempDir Path dir
  ) throws IOException {
    // Given
    var baseDir = dir.resolve("foo").resolve("bar");
    Files.createDirectories(baseDir);
    var file = baseDir.resolve(fileName);
    Files.createFile(file);
    var filter = new ProtoFileGlobFilter();

    // Then
    assertThat(filter.matches(dir, file)).isTrue();
  }
}
