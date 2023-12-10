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
package io.github.ascopes.protobufmavenplugin.dependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

@DisplayName("Executable tests")
class ExecutableTest {

  Executable executable;

  @BeforeEach
  void setUp() {
    executable = new Executable("org.example", "protoc");
  }

  @DisplayName("Supported Windows architectures resolve correctly")
  @CsvSource({
      " amd64, windows-x86_64",
      "x86_64, windows-x86_64",
      "x86, windows-x86_32",
      "x86_32, windows-x86_32",
  })
  @ParameterizedTest(name = "for architecture {0}, expect classifier matching \"{1}\"")
  void supportedWindowsArchitecturesResolveCorrectly(
      String architecture,
      String expectedClassifier
  ) throws DependencyResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenWindowsWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = executable.getMavenArtifactCoordinate("1.2.3");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "1.2.3", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Windows architectures result in an exception")
  @Test
  void supportedWindowsArchitecturesResultInException() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenWindowsWithArch(hostEnvironment, "x86_16");

      // Then
      assertThatThrownBy(() -> executable.getMavenArtifactCoordinate("1.2.3"))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No resolvable protoc version for Windows 'x86_16' systems found")
          .hasNoCause();
    }
  }

  @DisplayName("Supported Linux architectures resolve correctly")
  @CsvSource({
      "   amd64,   linux-x86_64",
      " aarch64, linux-aarch_64",
      "    s390,  linux-s390_64",
      "zarch_64,  linux-s390_64",
      " ppc64le, linux-ppcle_64",
      "   ppc64, linux-ppcle_64",
  })
  @ParameterizedTest(name = "for architecture {0}, expect classifier matching \"{1}\"")
  void supportedLinuxArchitecturesResolveCorrectly(
      String architecture,
      String expectedClassifier
  ) throws DependencyResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenLinuxWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = executable.getMavenArtifactCoordinate("4.5.6");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "4.5.6", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Linux architectures result in an exception")
  @Test
  void unsupportedLinuxArchitecturesResultInException() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenLinuxWithArch(hostEnvironment, "IA_64");

      // Then
      assertThatThrownBy(() -> executable.getMavenArtifactCoordinate("4.5.6"))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No resolvable protoc version for Linux 'IA_64' systems found")
          .hasNoCause();
    }
  }

  @DisplayName("Supported Mac OS architectures resolve correctly")
  @CsvSource({
      "aarch64, osx-aarch_64",
      "  amd64,   osx-x86_64",
      " x86_64,   osx-x86_64",
  })
  @ParameterizedTest(name = "for architecture {0}, expect classifier matching \"{1}\"")
  void supportedMacOsArchitecturesResolveCorrectly(
      String architecture,
      String expectedClassifier
  ) throws DependencyResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenMacOsWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = executable.getMavenArtifactCoordinate("7.8.9");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "7.8.9", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Mac OS architectures result in an exception")
  @Test
  void unsupportedMacOsArchitecturesResultInException() throws DependencyResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenMacOsWithArch(hostEnvironment, "something-crazy-unknown");

      // Then
      assertThatThrownBy(() -> executable.getMavenArtifactCoordinate("7.8.9"))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage(
              "No resolvable protoc version for Mac OS 'something-crazy-unknown' systems found")
          .hasNoCause();
    }
  }

  @DisplayName("Unsupported operating systems result in an exception being raised")
  @Test
  void unsupportedOperatingSystemsResultInAnExceptionBeingRaised() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenValidWorkingDirectory(hostEnvironment);
      givenUnknownOs(hostEnvironment);

      // Then
      assertThatThrownBy(() -> executable.getMavenArtifactCoordinate("9.8.7"))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No resolvable version of protoc for the current OS found")
          .hasNoCause();
    }
  }

  ///
  /// Helpers
  ///

  private void givenValidWorkingDirectory(MockedStatic<HostEnvironment> hostEnvironment) {
    hostEnvironment.when(HostEnvironment::workingDirectory).thenCallRealMethod();
  }

  private void givenWindowsWithArch(MockedStatic<HostEnvironment> hostEnvironment, String arch) {
    hostEnvironment.when(HostEnvironment::isWindows).thenReturn(true);
    hostEnvironment.when(HostEnvironment::isLinux).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isMacOs).thenReturn(false);
    hostEnvironment.when(HostEnvironment::cpuArchitecture).thenReturn(arch);
  }

  private void givenLinuxWithArch(MockedStatic<HostEnvironment> hostEnvironment, String arch) {
    hostEnvironment.when(HostEnvironment::isWindows).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isLinux).thenReturn(true);
    hostEnvironment.when(HostEnvironment::isMacOs).thenReturn(false);
    hostEnvironment.when(HostEnvironment::cpuArchitecture).thenReturn(arch);
  }

  private void givenMacOsWithArch(MockedStatic<HostEnvironment> hostEnvironment, String arch) {
    hostEnvironment.when(HostEnvironment::isWindows).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isLinux).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isMacOs).thenReturn(true);
    hostEnvironment.when(HostEnvironment::cpuArchitecture).thenReturn(arch);
  }

  private void givenUnknownOs(MockedStatic<HostEnvironment> hostEnvironment) {
    hostEnvironment.when(HostEnvironment::isWindows).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isLinux).thenReturn(false);
    hostEnvironment.when(HostEnvironment::isMacOs).thenReturn(false);
  }

  private void thenAssertCoordinateMatches(ArtifactCoordinate coordinate, String version, String classifier) {
    assertThat(coordinate.getGroupId())
        .as("Group ID")
        .isEqualTo("org.example");
    assertThat(coordinate.getArtifactId())
        .as("Artifact ID")
        .isEqualTo("protoc");
    assertThat(coordinate.getVersion())
        .as("Version")
        .isEqualTo(version);
    assertThat(coordinate.getClassifier())
        .as("Classifier")
        .isEqualTo(classifier);
    assertThat(coordinate.getExtension())
        .as("Extension")
        .isEqualTo("exe");
  }
}
