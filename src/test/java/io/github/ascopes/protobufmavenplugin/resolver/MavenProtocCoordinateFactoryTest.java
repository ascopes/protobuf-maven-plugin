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
package io.github.ascopes.protobufmavenplugin.resolver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mockStatic;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

@DisplayName("MavenProtocCoordinateFactory tests")
class MavenProtocCoordinateFactoryTest {

  private MavenProtocCoordinateFactory factory;

  @BeforeEach
  void setUp() {
    factory = new MavenProtocCoordinateFactory();
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
  ) throws ProtocResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenWindowsWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = factory.create("1.2.3");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "1.2.3", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Windows architectures result in an exception")
  @Test
  void supportedWindowsArchitecturesResultInException() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenWindowsWithArch(hostEnvironment, "x86_16");

      // Then
      assertThatThrownBy(() -> factory.create("1.2.3"))
          .isInstanceOf(ProtocResolutionException.class)
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
  ) throws ProtocResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenLinuxWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = factory.create("4.5.6");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "4.5.6", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Linux architectures result in an exception")
  @Test
  void unsupportedLinuxArchitecturesResultInException() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenLinuxWithArch(hostEnvironment, "IA_64");

      // Then
      assertThatThrownBy(() -> factory.create("4.5.6"))
          .isInstanceOf(ProtocResolutionException.class)
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
  ) throws ProtocResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenMacOsWithArch(hostEnvironment, architecture);

      // When
      var actualCoordinate = factory.create("7.8.9");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "7.8.9", expectedClassifier);
    }
  }

  @DisplayName("Unsupported Mac OS architectures fall back to the universal binary")
  @Test
  void unsupportedMacOsArchitecturesFallBackToTheUniversalBinary() throws ProtocResolutionException {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenMacOsWithArch(hostEnvironment, "something-crazy-unknown");

      // When
      var actualCoordinate = factory.create("7.8.9");

      // Then
      thenAssertCoordinateMatches(actualCoordinate, "7.8.9", "osx-universal_binary");
    }
  }

  @DisplayName("Unsupported operating systems result in an exception being raised")
  @Test
  void unsupportedOperatingSystemsResultInAnExceptionBeingRaised() {
    try (var hostEnvironment = mockStatic(HostEnvironment.class)) {
      // Given
      givenUnknownOs(hostEnvironment);

      // Then
      assertThatThrownBy(() -> factory.create("9.8.7"))
          .isInstanceOf(ProtocResolutionException.class)
          .hasMessage("No resolvable protoc version for the current OS found")
          .hasNoCause();
    }
  }

  ///
  /// Helpers
  ///

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

  private void thenAssertCoordinateMatches(
      ArtifactCoordinate coordinate,
      String version,
      String classifier
  ) {
    assertSoftly(softly -> {
      softly.assertThat(coordinate.getGroupId())
          .as("Group ID")
          .isEqualTo("com.google.protobuf");
      softly.assertThat(coordinate.getArtifactId())
          .as("Artifact ID")
          .isEqualTo("protoc");
      softly.assertThat(coordinate.getVersion())
          .as("Version")
          .isEqualTo(version);
      softly.assertThat(coordinate.getClassifier())
          .as("Classifier")
          .isEqualTo(classifier);
      softly.assertThat(coordinate.getExtension())
          .as("Extension")
          .isEqualTo("exe");

      var estimatedFileName = coordinate.getGroupId().replace('.', '/')
          + "/"
          + coordinate.getArtifactId()
          + "/"
          + coordinate.getArtifactId()
          + "-"
          + coordinate.getVersion()
          + "-"
          + coordinate.getClassifier()
          + "."
          + coordinate.getExtension();

      softly.assertThat(estimatedFileName)
          .as("estimated file name")
          .isEqualTo("com/google/protobuf/protoc/protoc-%s-%s.exe", version, classifier);
    });
  }
}
