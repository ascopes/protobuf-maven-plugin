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
package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultEclipseArtifactType tests")
class FallbackEclipseArtifactTypeTest {

  @DisplayName(".getId() returns the extension")
  @Test
  void getIdReturnsTheExtension() {
    // Given
    var dependencyExtension = someBasicString();
    var artifactType = new FallbackEclipseArtifactType(dependencyExtension);

    // Then
    assertThat(artifactType.getId()).isEqualTo(dependencyExtension);
  }

  @DisplayName(".getExtension() returns the extension")
  @Test
  void getExtensionReturnsTheExtension() {
    // Given
    var dependencyExtension = someBasicString();
    var artifactType = new FallbackEclipseArtifactType(dependencyExtension);

    // Then
    assertThat(artifactType.getExtension()).isEqualTo(dependencyExtension);
  }

  @DisplayName(".getClassifier() returns an empty string")
  @Test
  void getClassifierReturnsAnEmptyString() {
    // Given
    var artifactType = new FallbackEclipseArtifactType(someBasicString());

    // Then
    assertThat(artifactType.getClassifier()).isEmpty();
  }

  @DisplayName(".getProperties() returns an empty map")
  @Test
  void getPropertiesReturnsAnEmptyMap() {
    // Given
    var artifactType = new FallbackEclipseArtifactType(someBasicString());

    // Then
    assertThat(artifactType.getProperties()).isEmpty();
  }
}
