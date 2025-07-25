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
package io.github.ascopes.protobufmavenplugin.dependencies;

import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.arch;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.hostSystem;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.linux;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.macOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.otherOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.windows;
import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.HostSystemMockConfigurer;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("PlatformClassifierFactory tests")
class PlatformClassifierFactoryTest {

  @DisplayName(".getClassifier(...) returns the expected results on valid systems")
  @MethodSource("validClassifierCases")
  @ParameterizedTest(name = "for system {0}, expect classifier {1}")
  void getClassifierReturnsExpectedResultsOnValidSystems(
      HostSystemMockConfigurer configurer,
      String expectedClassifier
  ) throws ResolutionException {
    // Given
    var hostSystem = hostSystem();
    configurer.configure(hostSystem);
    var artifactId = someBasicString();
    var factory = new PlatformClassifierFactory(hostSystem);

    // When
    var actualClassifier = factory.getClassifier(artifactId);

    // Then
    assertThat(actualClassifier).isEqualTo(expectedClassifier);
  }

  @DisplayName(".getClassifier(...) raises an exception for unknown systems")
  @MethodSource("invalidClassifierCases")
  @ParameterizedTest(name = "when {0}, then expect an exception")
  void getClassifierRaisesAnExceptionForUnknownSystems(
      HostSystemMockConfigurer configurer
  ) {
    // Given
    var hostSystem = hostSystem();
    configurer.configure(hostSystem);
    var artifactId = someBasicString();
    var factory = new PlatformClassifierFactory(hostSystem);

    // Then
    assertThatThrownBy(() -> factory.getClassifier(artifactId))
        .isInstanceOf(ResolutionException.class)
        .hasMessageMatching(
            "No '[^']+' binary is available for reported OS '[^']+' and CPU architecture '[^']+'"
        );
  }

  static Stream<Arguments> validClassifierCases() {
    return Stream.of(
        arguments(linux().and(arch("amd64")), "linux-x86_64"),
        arguments(linux().and(arch("aarch64")), "linux-aarch_64"),
        arguments(linux().and(arch("s390x")), "linux-s390_64"),
        arguments(linux().and(arch("zarch_64")), "linux-s390_64"),
        arguments(linux().and(arch("ppc64le")), "linux-ppcle_64"),
        arguments(linux().and(arch("ppc64")), "linux-ppcle_64"),
        arguments(macOs().and(arch("amd64")), "osx-x86_64"),
        arguments(macOs().and(arch("x86_64")), "osx-x86_64"),
        arguments(macOs().and(arch("aarch64")), "osx-aarch_64"),
        arguments(windows().and(arch("amd64")), "windows-x86_64"),
        arguments(windows().and(arch("x86_64")), "windows-x86_64"),
        arguments(windows().and(arch("x86")), "windows-x86_32"),
        arguments(windows().and(arch("x86_32")), "windows-x86_32"),
        arguments(windows().and(arch("aarch64")), "windows-x86_64")
    );
  }

  static Stream<HostSystemMockConfigurer> invalidClassifierCases() {
    return Stream.of(
        linux().and(arch("some unknown CPU arch")),
        macOs().and(arch("some unknown CPU arch")),
        windows().and(arch("some unknown CPU arch")),
        otherOs().and(arch("x86_32")),
        otherOs().and(arch("x86_64")),
        otherOs().and(arch("amd64")),
        otherOs().and(arch("aarch64")),
        otherOs().and(arch("ppc64")),
        otherOs().and(arch("ppc64le")),
        otherOs().and(arch("s390")),
        otherOs().and(arch("zarch_64")),
        otherOs().and(arch("some unknown CPU arch"))
    );
  }

}
