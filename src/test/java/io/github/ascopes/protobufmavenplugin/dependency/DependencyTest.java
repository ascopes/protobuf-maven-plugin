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
package io.github.ascopes.protobufmavenplugin.dependency;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someText;
import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Dependency tests")
class DependencyTest {

  @DisplayName("Main constructor produces the expected object")
  @MethodSource("artifactAttributes")
  @ParameterizedTest(name = "for type = {3} and classifier = {4}")
  void mainConstructorProducesTheExpectedObject(
      String groupId,
      String artifactId,
      String version,
      @Nullable String type,
      @Nullable String classifier
  ) {
    // When
    var newDependency = new Dependency(groupId, artifactId, version, type, classifier);

    // Then
    assertEqual(
        newDependency,
        groupId,
        artifactId,
        version,
        requireNonNullElse(type, "jar"),
        classifier
    );
  }

  @DisplayName("Copy constructor for ArtifactCoordinates produces the expected object")
  @MethodSource("artifactAttributes")
  @ParameterizedTest(name = "for extension = {3} and classifier = {4}")
  void copyConstructorForArtifactCoordinatesProducesTheExpectedObject(
      String groupId,
      String artifactId,
      String version,
      @Nullable String extension,
      @Nullable String classifier
  ) {
    // Given
    var existingDependency = mock(ArtifactCoordinate.class);
    when(existingDependency.getGroupId()).thenReturn(groupId);
    when(existingDependency.getArtifactId()).thenReturn(artifactId);
    when(existingDependency.getVersion()).thenReturn(version);
    when(existingDependency.getExtension()).thenReturn(extension);
    when(existingDependency.getClassifier()).thenReturn(classifier);

    // When
    var newDependency = new Dependency(existingDependency);

    // Then
    assertEqual(
        newDependency,
        groupId,
        artifactId,
        version,
        requireNonNullElse(extension, "jar"),
        classifier
    );
  }

  @DisplayName("Copy constructor for DependableCoordinates produces the expected object")
  @MethodSource("artifactAttributes")
  @ParameterizedTest(name = "for type = {3} and classifier = {4}")
  void copyConstructorForDependableCoordinatesProducesTheExpectedObject(
      String groupId,
      String artifactId,
      String version,
      @Nullable String type,
      @Nullable String classifier
  ) {
    // Given
    var existingDependency = mock(DependableCoordinate.class);
    when(existingDependency.getGroupId()).thenReturn(groupId);
    when(existingDependency.getArtifactId()).thenReturn(artifactId);
    when(existingDependency.getVersion()).thenReturn(version);
    when(existingDependency.getType()).thenReturn(type);
    when(existingDependency.getClassifier()).thenReturn(classifier);

    // When
    var newDependency = new Dependency(existingDependency);

    // Then
    assertEqual(
        newDependency,
        groupId,
        artifactId,
        version,
        requireNonNullElse(type, "jar"),
        classifier
    );
  }

  @DisplayName(".toString() returns the expected value when all fields are set")
  @Test
  void toStringReturnsExpectedValueWhenAllFieldsSet() {
    // Given
    var groupId = someText();
    var artifactId = someText();
    var version = someText();
    var type = someText();
    var classifier = someText();
    var dependency = new Dependency(groupId, artifactId, version, type, classifier);

    // Then
    assertThat(dependency.toString())
        .isEqualTo(
            "mvn:%s:%s:%s:%s:%s", groupId, artifactId, version, type, classifier
        );
  }

  @DisplayName(".toString() returns the expected value when the type is null")
  @Test
  void toStringReturnsExpectedValueWhenTypeNull() {
    // Given
    var groupId = someText();
    var artifactId = someText();
    var version = someText();
    var classifier = someText();
    var dependency = new Dependency(groupId, artifactId, version, null, classifier);

    // Then
    assertThat(dependency.toString())
        .isEqualTo(
            "mvn:%s:%s:%s:jar:%s", groupId, artifactId, version, classifier
        );
  }

  @DisplayName(".toString() returns the expected value when the classifier is null")
  @Test
  void toStringReturnsExpectedValueWhenClassifierNull() {
    // Given
    var groupId = someText();
    var artifactId = someText();
    var version = someText();
    var type = someText();
    var dependency = new Dependency(groupId, artifactId, version, type, null);

    // Then
    assertThat(dependency.toString())
        .isEqualTo(
            "mvn:%s:%s:%s:%s", groupId, artifactId, version, type
        );
  }

  private void assertEqual(
      @Nullable Dependency dependency,
      @Nullable String groupId,
      @Nullable String artifactId,
      @Nullable String version,
      @Nullable String extension,
      @Nullable String classifier
  ) {
    assertThat(dependency)
        .as("Dependency <%s>", dependency)
        .isNotNull()
        .satisfies(
            d -> assertThat(d.getGroupId())
                .as("groupId")
                .isEqualTo(groupId),
            d -> assertThat(d.getArtifactId())
                .as("artifactId")
                .isEqualTo(artifactId),
            d -> assertThat(d.getVersion())
                .as("version")
                .isEqualTo(version),
            d -> assertThat(d.getType())
                .as("type")
                .isEqualTo(extension),
            d -> assertThat(d.getExtension())
                .as("extension")
                .isEqualTo(extension),
            d -> assertThat(d.getClassifier())
                .as("classifier")
                .isEqualTo(classifier)
        );
  }

  static Stream<Arguments> artifactAttributes() {
    var groupId = someText();
    var artifactId = someText();
    var version = someText();

    return Stream.of(
        arguments(groupId, artifactId, version, "zip", "archive"),
        arguments(groupId, artifactId, version, "zip", null),
        arguments(groupId, artifactId, version, null, null)
    );
  }
}
