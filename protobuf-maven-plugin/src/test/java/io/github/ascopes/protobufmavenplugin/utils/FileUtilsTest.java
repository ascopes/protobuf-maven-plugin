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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


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

    var somethingOrOther = cwd
        .resolve("foo")
        .resolve("bar")
        .resolve("baz")
        .resolve("..")
        .resolve("..")
        .resolve("bork");

    var expectedResult = cwd
        .normalize()
        .toAbsolutePath()
        .resolve("foo")
        .resolve("bork");

    // When
    var actualResult = FileUtils.normalize(somethingOrOther);

    // Then
    assertThat(actualResult)
        .isNormalized()
        .isAbsolute()
        .isEqualTo(expectedResult);
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
      String fileName,
      String expected,
      @TempDir Path tempDir
  ) {
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
      String fileName,
      @Nullable String expected,
      @TempDir Path tempDir
  ) {
    // Given
    var someFile = tempDir.resolve(fileName);

    // When
    var actual = FileUtils.getFileExtension(someFile);

    // Then
    assertThat(actual.orElse(null))
        .isEqualTo(expected);
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
      assertThat(file)
          .isRegularFile()
          .isExecutable();
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
      assertThat(file)
          .isRegularFile()
          .isExecutable();
    }
  }

  @DisplayName(".openZipAsFileSystem(...) opens a file system for the given ZIP")
  @Test
  void openZipAsFileSystemOpensFileSystemForGivenZip(@TempDir Path tempDir) throws IOException {
    // Given
    var zipPath = tempDir.resolve("test.zip");
    try (
        var os = Files.newOutputStream(zipPath, StandardOpenOption.CREATE_NEW);
        var zipOs = new ZipOutputStream(os);
    ) {
      zipOs.putNextEntry(new ZipEntry("foo/bar/baz.txt"));
      zipOs.write("Hello, World!".getBytes(StandardCharsets.UTF_8));
      zipOs.closeEntry();
    }

    // When
    try (var zipFs = FileUtils.openZipAsFileSystem(zipPath)) {
      var rootDir = zipFs.getRootDirectories().iterator().next();

      // Then
      assertThat(rootDir.resolve("foo").resolve("bar").resolve("baz.txt"))
          .isRegularFile()
          .hasContent("Hello, World!");
    }
  }

  @DisplayName(".openZipAsFileSystem(...) raises an IOException if the file system fails to open")
  @SuppressWarnings("resource")
  @Test
  void openZipAsFileSystemRaisesIoExceptionIfFileSystemFailsToOpen(
      @TempDir Path tempDir
  ) throws IOException {
    // Given
    var zipPath = tempDir.resolve("test.zip");
    Files.writeString(zipPath, "invalid zip file");

    // Then
    assertThatException()
        .isThrownBy(() -> FileUtils.openZipAsFileSystem(zipPath))
        .isInstanceOf(IOException.class)
        .withCauseInstanceOf(IOException.class)
        .withMessage("Failed to open %s as a valid ZIP/JAR archive", zipPath);
  }

  @DisplayName(".rebaseFileTree(...) copies all nodes between file systems")
  @Test
  void rebaseFileTreeCopiesAllNodesBetweenFileSystems() throws IOException {
    // Given
    try (
        var fs1 = TestFileSystem.linux();
        var fs2 = TestFileSystem.windows()
    ) {
      var root1 = fs1.getRoot().resolve("base").resolve("dir");
      var dir1 = root1.resolve("foo").resolve("bar").resolve("baz");
      var dir2 = root1.resolve("do").resolve("ray").resolve("me");
      Files.createDirectories(dir1);
      Files.createDirectories(dir2);
      var file1 = Files.writeString(dir1.resolve("aaa.txt"), "foobarbaz");
      var file2 = Files.writeString(dir1.resolve("bbb.txt"), "borkqux");
      var file3 = Files.writeString(dir2.resolve("ccc.txt"), "eggsSpam");

      var root2 = fs2.getRoot().resolve("target").resolve("directory");

      // When
      List<Path> expectedResult;
      try (var files = Files.walk(root1)) {
        expectedResult = FileUtils.rebaseFileTree(root1, root2, files);
      }

      // Then
      var newDir1 = root2.resolve("foo").resolve("bar").resolve("baz");
      var newDir2 = root2.resolve("do").resolve("ray").resolve("me");

      assertThat(newDir1.resolve("aaa.txt"))
          .exists()
          .isRegularFile()
          .hasContent("foobarbaz");

      assertThat(newDir1.resolve("bbb.txt"))
          .exists()
          .isRegularFile()
          .hasContent("borkqux");

      assertThat(newDir2.resolve("ccc.txt"))
          .exists()
          .isRegularFile()
          .hasContent("eggsSpam");

      assertThat(expectedResult)
          .isNotNull()
          .containsExactlyInAnyOrder(
              newDir1.resolve("aaa.txt"),
              newDir1.resolve("bbb.txt"),
              newDir2.resolve("ccc.txt")
          );
    }
  }
}
