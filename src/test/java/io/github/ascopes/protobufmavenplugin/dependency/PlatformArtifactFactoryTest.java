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

package io.github.ascopes.protobufmavenplugin.dependency;

import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.arch;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.hostSystem;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.linux;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.macOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.otherOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.windows;
import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someText;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.HostSystemMockConfigurer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("PlatformArtifactFactory tests")
class PlatformArtifactFactoryTest {

  @DisplayName(".createArtifact(...) returns the expected results on valid systems")
  @MethodSource("validClassifierCases")
  @ParameterizedTest(name = "for system {0}, extension {1}, classifier {3}, expect "
      + "extension {2} and classifier {4}")
  void createArtifactReturnsExpectedResultsOnValidSystems(
      HostSystemMockConfigurer configurer,
      @Nullable String givenExtension,
      String expectedExtension,
      @Nullable String givenClassifier,
      String expectedClassifier
  ) {
    // Given
    var hostSystem = hostSystem();
    configurer.configure(hostSystem);
    var groupId = someText();
    var artifactId = someText();
    var version = someText();
    var factory = new PlatformArtifactFactory(hostSystem);

    // When
    var dependency = factory.createArtifact(
        groupId, 
        artifactId, 
        version, 
        givenExtension, 
        givenClassifier
    );

    // Then
    assertSoftly(softly -> {
      softly.assertThat(dependency.getGroupId())
          .as("groupId")
          .isEqualTo(groupId);
      softly.assertThat(dependency.getArtifactId())
          .as("artifactId")
          .isEqualTo(artifactId);
      softly.assertThat(dependency.getVersion())
          .as("version")
          .isEqualTo(version);
      softly.assertThat(dependency.getExtension())
          .as("extension")
          .isEqualTo(expectedExtension);
      softly.assertThat(dependency.getClassifier())
          .as("type")
          .isEqualTo(expectedClassifier);
    });
  }

  @DisplayName(".createArtifact(...) raises an exception for unknown systems")
  @MethodSource("invalidClassifierCases")
  @ParameterizedTest(name = "when {0}, then expect an exception")
  void createArtifactRaisesAnExceptionForUnknownSystems(
      HostSystemMockConfigurer configurer
  ) {
    // Given
    var hostSystem = hostSystem();
    configurer.configure(hostSystem);
    var groupId = someText();
    var artifactId = someText();
    var version = someText();
    var factory = new PlatformArtifactFactory(hostSystem);

    // Then
    assertThatThrownBy(() -> factory.createArtifact(groupId, artifactId, version, null, null))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageMatching(
            "No '[^']+' binary is available for reported OS '[^']+' and CPU architecture '[^']+'"
        );
  }

  static Stream<Arguments> validClassifierCases() {
    var systemToClassifiers = new HashMap<HostSystemMockConfigurer, String>();
    systemToClassifiers.put(linux().and(arch("amd64")), "linux-x86_64");
    systemToClassifiers.put(linux().and(arch("aarch64")), "linux-aarch_64");
    systemToClassifiers.put(linux().and(arch("s390x")), "linux-s390_64");
    systemToClassifiers.put(linux().and(arch("zarch_64")), "linux-s390_64");
    systemToClassifiers.put(linux().and(arch("ppc64le")), "linux-ppcle_64");
    systemToClassifiers.put(linux().and(arch("ppc64")), "linux-ppcle_64");
    systemToClassifiers.put(macOs().and(arch("amd64")), "osx-x86_64");
    systemToClassifiers.put(macOs().and(arch("x86_64")), "osx-x86_64");
    systemToClassifiers.put(macOs().and(arch("aarch64")), "osx-aarch_64");
    systemToClassifiers.put(windows().and(arch("amd64")), "windows-x86_64");
    systemToClassifiers.put(windows().and(arch("x86_64")), "windows-x86_64");
    systemToClassifiers.put(windows().and(arch("x86")), "windows-x86_32");
    systemToClassifiers.put(windows().and(arch("x86_32")), "windows-x86_32");

    var cases = new ArrayList<Arguments>();
    systemToClassifiers.forEach((system, expectedClassifier) -> {
      cases.add(arguments(system, null, "exe", null, expectedClassifier));
      cases.add(arguments(system, "foobar", "foobar", null, expectedClassifier));
    });

    systemToClassifiers.keySet().forEach(system -> {
      cases.add(arguments(system, null, "exe", "bazbork", "bazbork"));
      cases.add(arguments(system, "foobar", "foobar", "bazbork", "bazbork"));
    });

    return cases.stream();
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
