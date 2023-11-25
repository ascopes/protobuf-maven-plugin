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

package io.github.ascopes.protobufmavenplugin.platform;

import static io.github.ascopes.protobufmavenplugin.fixture.RandomData.oneOf;
import static io.github.ascopes.protobufmavenplugin.fixture.RandomData.someString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import io.github.ascopes.protobufmavenplugin.fixture.FileSystemTestSupport;
import io.github.ascopes.protobufmavenplugin.fixture.MockedSystemProperties;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

@DisplayName("HostEnvironment tests")
@MockedSystemProperties
class HostEnvironmentTest extends FileSystemTestSupport {

  @DisplayName(".isWindows() returns true when on Windows")
  @ValueSource(strings = {
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isWindowsReturnsTrueWhenOnWindows(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isWindows()).isTrue();
  }

  @DisplayName(".isWindows() returns false when not on Windows")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Linux",
      "LINUX",
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isWindowsReturnsFalseWhenNotOnWindows(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isWindows()).isFalse();
  }

  @DisplayName(".isWindows() raises an IllegalStateException if no os.name property is set")
  @Test
  void isWindowsRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isWindows)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }

  @DisplayName(".isMacOs() returns true when on Mac OS")
  @ValueSource(strings = {
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isMacOsReturnsTrueWhenOnMacOs(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isMacOs()).isTrue();
  }

  @DisplayName(".isMacOs() returns false when not on Mac OS")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Linux",
      "LINUX",
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isMacOsReturnsFalseWhenNotOnMacOs(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isMacOs()).isFalse();
  }

  @DisplayName(".isMacOs() raises an IllegalStateException if no os.name property is set")
  @Test
  void isMacOsRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isMacOs)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }

  @DisplayName(".isLinux() returns true when on Linux")
  @ValueSource(strings = {
      "Linux",
      "LINUX"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isLinuxReturnsTrueWhenOnLinux(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isLinux()).isTrue();
  }

  @DisplayName(".isLinux() returns false when not on Linux")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isLinuxReturnsFalseWhenNotOnLinux(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isLinux()).isFalse();
  }

  @DisplayName(".isLinux() raises an IllegalStateException if no os.name property is set")
  @Test
  void isLinuxRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isLinux)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }

  @DisplayName(".systemPath() returns the parsed $PATH environment variable")
  @Test
  void systemPathReturnsTheParsedPathEnvironmentVariable() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class, Answers.CALLS_REAL_METHODS)) {
      // Given
      var dir1 = givenDirectoryExists("test", "foo", "bar");
      var dir2 = givenDirectoryExists("test", "foo", "baz");
      var dir3 = givenDirectoryExists("test", "foo", "bork");
      var dir4 = givenDirectoryExists("test", "foo", "qux");
      var dir5 = givenDirectoryDoesNotExist("test", "foo", "eggs");
      var dir6 = givenDirectoryDoesNotExist("test", "foo", "spam");

      var rawPath = Stream
          .of(dir1, dir2, dir3, dir4, dir5, dir6)
          .map(Path::toString)
          .collect(Collectors.joining(File.pathSeparator));

      hostEnvironment.when(() -> HostEnvironment.environmentVariable(any()))
          .thenReturn(Optional.of(rawPath));

      // When
      var actualPath = HostEnvironment.systemPath();

      // Then
      assertThat(actualPath).containsExactly(dir1, dir2, dir3, dir4, dir5, dir6);
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable(any()), times(1));
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable("PATH"));
    }
  }

  @DisplayName(".systemPath() returns an empty list if $PATH is unset")
  @Test
  void systemPathReturnsEmptyListIfPathEnvironmentVariableIsUnset() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class, Answers.CALLS_REAL_METHODS)) {
      // Given
      hostEnvironment.when(() -> HostEnvironment.environmentVariable(any()))
          .thenReturn(Optional.empty());

      // When
      var actualPath = HostEnvironment.systemPath();

      // Then
      assertThat(actualPath).isEmpty();
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable(any()), times(1));
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable("PATH"));
    }
  }

  @DisplayName(".systemPathExtensions() returns the parsed $PATHEXT environment variable")
  @Test
  void systemPathExtensionsReturnsTheParsedPathExtEnvironmentVariable() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class, Answers.CALLS_REAL_METHODS)) {
      // Given
      var extString = String.join(File.pathSeparator, "", ".EXE", ".MSI", ".BAT", ".CMD", ".PS1");

      hostEnvironment.when(() -> HostEnvironment.environmentVariable(any()))
          .thenReturn(Optional.of(extString));

      // When
      var actualExtensions = HostEnvironment.systemPathExtensions();

      // Then
      assertThat(actualExtensions).containsOnly(
          ".EXE", ".exe", ".ExE", ".eXe",
          ".MSI", ".msi", ".MsI", ".mSI",
          ".BAT", ".bat", ".bAt", ".BaT",
          ".CMD", ".cmd", ".cMd", ".CmD",
          ".PS1", ".ps1", ".Ps1", ".pS1"
      );
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable(any()), times(1));
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable("PATHEXT"));
    }
  }

  @DisplayName(".systemPathExtensions() returns an empty list if $PATHEXT is unset")
  @Test
  void systemPathExtensionsReturnsEmptyListIfPathExtEnvironmentVariableIsUnset() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class, Answers.CALLS_REAL_METHODS)) {
      // Given
      hostEnvironment.when(() -> HostEnvironment.environmentVariable(any()))
          .thenReturn(Optional.empty());

      // When
      var actualExtensions = HostEnvironment.systemPathExtensions();

      // Then
      assertThat(actualExtensions).isEmpty();
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable(any()), times(1));
      hostEnvironment.verify(() -> HostEnvironment.environmentVariable("PATHEXT"));
    }
  }

  @DisplayName(".environmentVariable(...) returns the environment variable when set")
  @RepeatedTest(5)
  void environmentVariableReturnsTheEnvironmentVariableWhenSet() {
    // Given
    // We can't mock this directly, so we have to make a best-effort guess.
    var env = System.getenv();
    assumeThat(env).isNotEmpty();
    var expectedVariableName = oneOf(env.keySet());
    var expectedVariableValue = env.get(expectedVariableName);

    // Then
    assertThat(HostEnvironment.environmentVariable(expectedVariableName))
        .isPresent()
        .get()
        .asString()
        .isEqualTo(expectedVariableValue);
  }

  @DisplayName(".environmentVariable(...) returns an empty optional when the variable is not set")
  @Test
  void environmentVariableReturnsEmptyOptionalWhenVariableIsNotSet() {
    // Given
    // We can't mock this directly, so we have to make a best-effort guess.
    var env = System.getenv();
    var unexpectedVariableName = someString();
    assumeThat(env)
        .withFailMessage("Randomly generated envvar is is already defined somehow")
        .doesNotContainKey(unexpectedVariableName);

    // Then
    assertThat(HostEnvironment.environmentVariable(unexpectedVariableName))
        .isEmpty();
  }

  @DisplayName(".workingDirectory() returns the working directory")
  @Test
  void workingDirectoryReturnsTheWorkingDirectory() {
    // Then
    assertThat(HostEnvironment.workingDirectory())
        .isEqualTo(Path.of("").toAbsolutePath());
  }
}
