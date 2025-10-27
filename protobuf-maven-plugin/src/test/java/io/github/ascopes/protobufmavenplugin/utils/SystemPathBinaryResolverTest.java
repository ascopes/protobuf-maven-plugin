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
package io.github.ascopes.protobufmavenplugin.utils;

import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.linux;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.macOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.path;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.pathExtensions;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.windows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures;
import io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SystemPathBinaryResolver tests")
@ExtendWith(MockitoExtension.class)
class SystemPathBinaryResolverTest {

  @Mock(strictness = Strictness.LENIENT)
  HostSystem hostSystem;

  @InjectMocks
  SystemPathBinaryResolver underTest;

  @DisplayName(".resolve(String) returns the first match on POSIX file systems")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveReturnsTheFirstMatchForPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(bazDir, "sh");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");
      final var expectedResult = fs.givenExecutableFileExists(barDir, "sh");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("sh");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) returns the first symbolic link match on POSIX file system")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveReturnsTheFirstSymbolicLinkMatchForPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(bazDir, "sh");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");

      final var actualSh = fs.givenExecutableFileExists(bazDir, "bash");
      final var expectedResult = fs.givenExecutableSymbolicLinkExists(actualSh, barDir, "sh");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("sh");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) does not operate recursively on POSIX file systems")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveDoesNotOperateRecursivelyOnPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var fooBinDir = fs.givenDirectoryExists("foo", "bin");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(fooBinDir, "protoc");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");
      final var expectedResult = fs.givenExecutableFileExists(barDir, "protoc");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("protoc");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(
      ".resolve(String) ignores the first match if it is not executable for POSIX file systems"
  )
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveIgnoresFirstMatchIfItIsNotExecutableForPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");

      fs.givenFileExists(fooDir, "bash");
      final var expectedResult = fs.givenExecutableFileExists(barDir, "bash");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("bash");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) ignores case insensitive matches for POSIX file systems")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveIgnoresCaseInsensitiveMatchesForPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(fooDir, "PROTOC-GEN-GRPC-JAVA");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");

      final var expectedResult = fs.givenExecutableFileExists(borkDir, "protoc-gen-grpc-java");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("protoc-gen-grpc-java");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) does not ignore file extensions on POSIX file systems")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveDoesNotIgnoreFileExtensionsOnPosixFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenExecutableFileExists(fooDir, "do");
      fs.givenExecutableFileExists(fooDir, "ray");
      fs.givenExecutableFileExists(fooDir, "me");
      fs.givenExecutableFileExists(fooDir, "fah.exe");
      fs.givenExecutableFileExists(barDir, "soh");
      fs.givenExecutableFileExists(bazDir, "soh");

      final var expectedResult = fs.givenExecutableFileExists(borkDir, "fah");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("fah");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) returns the first match on Windows")
  @MethodSource("windowsFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveReturnsFirstMatchOnWindows(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray");
      fs.givenFileExists(fooDir, "me");
      fs.givenFileExists(barDir, "soh");
      fs.givenFileExists(bazDir, "soh");

      final var expectedResult = fs.givenFileExists(fooDir, "trojan-horse.EXE");
      fs.givenFileExists(barDir, "trojan-horse.COM");
      fs.givenFileExists(bazDir, "trojan-horse.BAT");

      path(fooDir, barDir, bazDir, borkDir)
          .and(pathExtensions(".COM", ".EXE", ".BAT", ".CMD", ".VBS", ".JS", ".MSC"))
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      var result = underTest.resolve("trojan-horse");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) returns the first case insensitive file name match on Windows")
  @MethodSource("windowsFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveReturnsFirstCaseInsensitiveFileNameMatchOnWindows(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray.CMD");
      fs.givenFileExists(fooDir, "ray.COM");
      fs.givenFileExists(fooDir, "me");
      fs.givenFileExists(barDir, "soh.EXE");
      fs.givenFileExists(bazDir, "soh");

      final var expectedResult = fs.givenFileExists(barDir, "pROtOc-GeN-sCAlA.ExE");
      fs.givenFileExists(bazDir, "pROtOc-GeN-sCAlA.com");
      fs.givenFileExists(borkDir, "pROtOc-GeN-sCAlA.MSC");

      path(fooDir, barDir, bazDir, borkDir)
          .and(pathExtensions(".COM", ".EXE", ".BAT", ".CMD", ".VBS", ".JS", ".MSC"))
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result1 = underTest.resolve("protoc-gen-scala");
      final var result2 = underTest.resolve("PROTOC-GEN-SCALA");
      final var result3 = underTest.resolve("pRoToC-gen-sCaLa");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result1).as("result1").isPresent()
          .get().isEqualTo(expectedResult);
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result2).as("result2").isPresent()
          .get().isEqualTo(expectedResult);
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result3).as("result3").isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) ignores matches with non-executable file extensions on Windows")
  @MethodSource("windowsFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveIgnoresMatchesWithNonExecutableFileExtensionsOnWindowsFileSystems(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray");
      fs.givenFileExists(fooDir, "me");
      fs.givenFileExists(barDir, "soh");
      fs.givenFileExists(bazDir, "soh");

      fs.givenFileExists(fooDir, "protoc.jpeg");
      fs.givenFileExists(barDir, "protoc.png");
      fs.givenFileExists(barDir, "protoc.BIN");
      fs.givenFileExists(bazDir, "protoc.TXT");
      final var expectedResult = fs.givenFileExists(borkDir, "protoc.EXE");

      path(fooDir, barDir, bazDir, borkDir)
          .and(pathExtensions(".COM", ".EXE", ".BAT", ".CMD", ".VBS", ".JS", ".MSC"))
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("protoc");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expectedResult);
    }
  }

  @DisplayName(".resolve(String) returns an empty result if no match is found")
  @MethodSource("posixFileSystemProviders")
  @MethodSource("windowsFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveReturnsEmptyResultIfNoMatchIsFound(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray");
      fs.givenFileExists(fooDir, "me");

      path(fooDir, barDir, bazDir, borkDir)
          .and(pathExtensions(".COM", ".EXE", ".BAT", ".CMD", ".VBS", ".JS", ".MSC"))
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("protoc");

      // Then
      assertThat(result).isEmpty();
    }
  }

  @DisplayName(".resolve(String) raises a ResolutionException if an unexpected exception occurs")
  @MethodSource("posixFileSystemProviders")
  @MethodSource("windowsFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveRaisesResolutionExceptionIfUnexpectedExceptionOccurs(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray");
      fs.givenFileExists(fooDir, "me");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);
    }

    // Purposely close the file system to force it to raise exceptions on subsequent accesses.
    // When
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> underTest.resolve("protoc"))
        .withMessage("An exception occurred while scanning the system PATH: "
            + "java.nio.file.ClosedFileSystemException")
        .havingCause()
        .isNotNull();
  }

  // We simulate this by passing a directory that we cannot read due to POSIX file permissions.
  // Windows file systems will not support this, so we ignore them for this test case.
  @DisplayName(".resolve(String) ignores exceptions if access is denied")
  @MethodSource("posixFileSystemProviders")
  @ParameterizedTest(name = "for file system = {argumentSetName}")
  void resolveIgnoresExceptionsIfPathResolutionFailsDueToAccessDenied(
      Supplier<TestFileSystem> fileSystemSupplier,
      HostSystemFixtures.HostSystemMockConfigurer osConfigurer
  ) throws ResolutionException {
    try (var fs = fileSystemSupplier.get()) {
      // Given
      final var fooDir = fs.givenDirectoryExists("foo");
      final var barDir = fs.givenDirectoryExists("bar");
      final var bazDir = fs.givenDirectoryExists("baz");
      final var borkDir = fs.givenDirectoryExists("bork");

      fs.givenFileExists(fooDir, "do");
      fs.givenFileExists(fooDir, "ray");
      fs.givenFileExists(fooDir, "me");
      fs.givenExecutableFileExists(fooDir, "protoc");
      fs.changePermissions(fooDir, Set::clear);
      final var expected = fs.givenExecutableFileExists(borkDir, "protoc");

      path(fooDir, barDir, bazDir, borkDir)
          .and(osConfigurer)
          .configure(hostSystem);

      // When
      final var result = underTest.resolve("protoc");

      // Then
      //noinspection AssertBetweenInconvertibleTypes
      assertThat(result).isPresent()
          .get().isEqualTo(expected);
    }
  }

  static Stream<Arguments> posixFileSystemProviders() {
    return Stream.of(
        argumentSet("Linux", (Supplier<TestFileSystem>) TestFileSystem::linux, linux()),
        argumentSet("MacOS", (Supplier<TestFileSystem>) TestFileSystem::macOs, macOs())
    );
  }

  static Stream<Arguments> windowsFileSystemProviders() {
    return Stream.of(
        argumentSet("Windows", (Supplier<TestFileSystem>) TestFileSystem::windows, windows())
    );
  }
}
