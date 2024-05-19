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

package io.github.ascopes.protobufmavenplugin.utils;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ashley Scopes
 */
@DisplayName("FileUtils tests")
class FileUtilsTest {

  @DisplayName(".normalize(Path) returns the normalized path")
  @Test
  void normalizeReturnsTheNormalizedPath() {
    // Given
    var cwd = Path.of("");

    var somethingOrOther =
        cwd.resolve("foo")
            .resolve("bar")
            .resolve("baz")
            .resolve("..")
            .resolve("..")
            .resolve("bork");

    var expectedResult = cwd.normalize().toAbsolutePath().resolve("foo").resolve("bork");

    // When
    var actualResult = FileUtils.normalize(somethingOrOther);

    // Then
    assertThat(actualResult).isNormalized().isAbsolute().isEqualTo(expectedResult);
  }

  @DisplayName(".getFileNameWithoutExtension(Path) returns the expected result")
  @CsvSource({
    "foo.bar,    foo",
    "foo.BAR,    foo",
    "    x.y,      x",
    " .yzabc, .yzabc",
  })
  @ParameterizedTest(name = ".getFileNameWithoutExtension(\".../{0}\") returns \"{1}\"")
  void getFileNameWithoutExtensionReturnsTheExpectedValue(
      String fileName, String expected, @TempDir Path tempDir) {
    // Given
    var someFile = tempDir.resolve(fileName);

    // When
    var actual = FileUtils.getFileNameWithoutExtension(someFile);

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @DisplayName(".getFileExtension(Path) returns the expected result")
  @CsvSource({
    "foo.bar,   .bar",
    "foo.BAR,   .BAR",
    "    x.y,     .y",
    " .yzabc,       ",
    "      '',      ",
  })
  @ParameterizedTest(name = ".getFileExtension(\".../{0}\") returns \"{1}\"")
  void getFileExtensionReturnsTheExpectedValue(
      String fileName, @Nullable String expected, @TempDir Path tempDir) {
    // Given
    var someFile = tempDir.resolve(fileName);

    // When
    var actual = FileUtils.getFileExtension(someFile);

    // Then
    assertThat(actual.orElse(null)).isEqualTo(expected);
  }

  @DisplayName(".getFileSystemProvider(String) returns the file system provider")
  @ValueSource(strings = {"jar", "JAR", "file", "memory"})
  @ParameterizedTest(name = "for scheme \"{0}\"")
  void getFileSystemProviderReturnsTheFileSystemProvider(String scheme) {
    // Then
    assertThatNoException().isThrownBy(() -> FileUtils.getFileSystemProvider(scheme));

    // Then
    assertThat(FileUtils.getFileSystemProvider(scheme).getScheme()).isEqualToIgnoringCase(scheme);

    // Then
    var firstResult = FileUtils.getFileSystemProvider(scheme);
    var secondResult = FileUtils.getFileSystemProvider(scheme);
    assertThat(firstResult).isSameAs(secondResult);
  }

  @DisplayName(".getFileSystemProvider(String) throws an exception if the provider does not exist")
  @Test
  void getFileSystemProviderThrowsExceptionIfTheProviderDoesNotExist() {
    // Given
    var scheme = someText();

    // Then
    assertThatThrownBy(() -> FileUtils.getFileSystemProvider(scheme))
        .isInstanceOf(FileSystemNotFoundException.class)
        .hasMessage("No file system provider for %s was found", scheme);
  }

  @DisplayName(".makeExecutable makes the path executable when supported")
  @Test
  void makeExecutableMakesThePathExecutableWhenSupported() throws IOException {
    // Given
    try (var tempFs = TestFileSystem.linux()) {
      var file = tempFs.givenFileExists("foo", "bar", "baz");

      assumeThat(Files.isExecutable(file)).isFalse();

      // When
      FileUtils.makeExecutable(file);

      // Then
      assertThat(file).isRegularFile().isExecutable();
    }
  }

  @DisplayName(".makeExecutable exits silently when not supported")
  @Test
  void makeExecutableMakesThePathExecutableWhenNotSupported() throws IOException {
    // Given
    try (var tempFs = TestFileSystem.windows()) {
      var file = tempFs.givenFileExists("foo", "bar", "baz");

      // When
      FileUtils.makeExecutable(file);

      // Then
      assertThat(file).isRegularFile().isExecutable();
    }
  }

  @DisplayName(".changeRelativePath produces the expected result on the same file system")
  @Test
  void changeRelativePathProducesTheExpectedResultOnTheSameFileSystem(@TempDir Path tempDir)
      throws IOException {
    // Given
    var dir1 = tempDir.resolve("foo").resolve("bar").resolve("baz");
    var dir2 = tempDir.resolve("do").resolve("ray").resolve("me");
    var existingPath = dir1.resolve("some").resolve("file.txt");
    Files.createDirectories(dir1);
    Files.createDirectories(dir2);

    // When
    var actualPath = FileUtils.changeRelativePath(dir2, dir1, existingPath);

    // Then
    assertThat(actualPath).isEqualTo(dir2.resolve("some").resolve("file.txt"));
  }

  @DisplayName(".changeRelativePath produces the expected result across file systems")
  @Test
  void changeRelativePathProducesTheExpectedResultAcrossFileSystems() throws IOException {
    // Given
    try (var fs1 = TestFileSystem.linux();
        var fs2 = TestFileSystem.windows()) {
      var dir1 = fs1.getRoot().resolve("foo").resolve("bar").resolve("baz");
      var dir2 = fs2.getRoot().resolve("do").resolve("ray").resolve("me");
      var existingPath = dir1.resolve("some").resolve("file.txt");
      Files.createDirectories(dir1);
      Files.createDirectories(dir2);

      // When
      var actualPath = FileUtils.changeRelativePath(dir2, dir1, existingPath);

      // Then
      assertThat(actualPath).isEqualTo(dir2.resolve("some").resolve("file.txt"));
    }
  }
}
