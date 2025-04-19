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

import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.eclipseDependency;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.mavenDependency;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.quality.Strictness;

@DisplayName("AetherDependencyManagement tests")
class AetherDependencyManagementTest {

  MavenSession mavenSession;
  AetherArtifactMapper artifactMapper;

  @BeforeEach
  void setUp() {
    var settings = withSettings()
        .strictness(Strictness.LENIENT)
        .defaultAnswer(RETURNS_DEEP_STUBS);
    mavenSession = mock(settings);
    artifactMapper = new AetherArtifactMapper(mock());
  }

  @DisplayName("dependency management is not applied to irrelevant dependencies")
  @Test
  void dependencyManagementIsNotAppliedToIrrelevantDependencies() {
    // Given
    when(mavenSession.getCurrentProject().getDependencyManagement().getDependencies())
        .thenAnswer(ctx -> List.of(mavenDependency(), mavenDependency(), mavenDependency()));

    var inputDependency = eclipseDependency(
        "org.springframework.boot",
        "spring-boot",
        "3.0.0",
        null,
        null,
        "compile",
        null
    );

    var aetherDependencyManagement = new AetherDependencyManagement(mavenSession, artifactMapper);

    // When
    var outputDependency = aetherDependencyManagement.fillManagedAttributes(inputDependency);

    // Then
    assertThat(outputDependency)
        .isSameAs(inputDependency);
  }

  @DisplayName("dependency management is applied to relevant dependencies with missing versions")
  @NullAndEmptySource
  @ValueSource(strings = " ")
  @ParameterizedTest(name = "for version = {0}")
  void dependencyManagementIsAppliedToRelevantDependenciesWithMissingVersions(
      String missingVersion
  ) {
    when(mavenSession.getCurrentProject().getDependencyManagement().getDependencies())
        .thenAnswer(ctx -> List.of(
            mavenDependency(),
            mavenDependency(),
            mavenDependency(),
            mavenDependency(
                "org.springframework.boot",
                "spring-boot",
                "3.0.0",
                "jar",
                null
            ),
            mavenDependency()
        ));

    var inputDependency = eclipseDependency(
        "org.springframework.boot",
        "spring-boot",
        missingVersion,
        "",
        "jar",
        "compile",
        null
    );

    var aetherDependencyManagement = new AetherDependencyManagement(mavenSession, artifactMapper);

    // When
    var outputDependency = aetherDependencyManagement.fillManagedAttributes(inputDependency);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(outputDependency.getArtifact().getGroupId())
          .as("artifact.groupId")
          .isEqualTo("org.springframework.boot");
      softly.assertThat(outputDependency.getArtifact().getArtifactId())
          .as("artifact.artifactId")
          .isEqualTo("spring-boot");
      softly.assertThat(outputDependency.getArtifact().getVersion())
          .as("artifact.version")
          .isEqualTo("3.0.0");
      softly.assertThat(outputDependency.getArtifact().getClassifier())
          .as("artifact.classifier")
          .isEmpty();
      softly.assertThat(outputDependency.getArtifact().getExtension())
          .as("artifact.extension")
          .isEqualTo("jar");
      softly.assertThat(outputDependency.getScope())
          .as("scope")
          .isEqualTo(inputDependency.getScope());
      softly.assertThat(outputDependency.getOptional())
          .as("optional")
          .isEqualTo(inputDependency.getOptional());
      softly.assertThat(outputDependency.getExclusions())
          .as("exclusions")
          .isEqualTo(inputDependency.getExclusions());
    });
  }

  @DisplayName("dependency management is not applied to relevant dependencies with different types")
  @Test
  void dependencyManagementIsAppliedToRelevantDependenciesWithDifferentTypes() {
    when(mavenSession.getCurrentProject().getDependencyManagement().getDependencies())
        .thenAnswer(ctx -> List.of(
            mavenDependency(),
            mavenDependency(),
            mavenDependency(),
            mavenDependency(
                "org.springframework.boot",
                "spring-boot",
                "3.0.0",
                "jar",
                null
            ),
            mavenDependency()
        ));

    var inputDependency = eclipseDependency(
        "org.springframework.boot",
        "spring-boot",
        null,
        "",
        "war",
        "compile",
        null
    );

    var aetherDependencyManagement = new AetherDependencyManagement(mavenSession, artifactMapper);

    // When
    var outputDependency = aetherDependencyManagement.fillManagedAttributes(inputDependency);

    // Then
    assertThat(outputDependency).isSameAs(inputDependency);
  }

  @DisplayName(
      "Dependency management is not applied to relevant dependencies with different classifiers"
  )
  @NullAndEmptySource
  @ValueSource(strings = "bar")
  @ParameterizedTest(name = "for classifier = {0}")
  void dependencyManagementIsNotAppliedToRelevantDependenciesWithDifferentClassifiers(
      String classifier
  ) {
    when(mavenSession.getCurrentProject().getDependencyManagement().getDependencies())
        .thenAnswer(ctx -> List.of(
            mavenDependency(),
            mavenDependency(),
            mavenDependency(),
            mavenDependency(
                "org.springframework.boot",
                "spring-boot",
                "3.0.0",
                "jar",
                classifier
            ),
            mavenDependency()
        ));

    var inputDependency = eclipseDependency(
        "org.springframework.boot",
        "spring-boot",
        null,
        "foo",
        "jar",
        "compile",
        null
    );

    var aetherDependencyManagement = new AetherDependencyManagement(mavenSession, artifactMapper);

    // When
    var outputDependency = aetherDependencyManagement.fillManagedAttributes(inputDependency);

    // Then
    assertThat(outputDependency).isSameAs(inputDependency);
  }

  @DisplayName(
      "Dependency management is not applied to relevant dependencies with provided versions"
  )
  @Test
  void dependencyManagementIsNotAppliedToRelevantDependenciesWithProvidedVersions() {
    when(mavenSession.getCurrentProject().getDependencyManagement().getDependencies())
        .thenAnswer(ctx -> List.of(
            mavenDependency(),
            mavenDependency(),
            mavenDependency(),
            mavenDependency(
                "org.springframework.boot",
                "spring-boot",
                "3.0.0",
                "jar",
                null
            ),
            mavenDependency()
        ));

    var inputDependency = eclipseDependency(
        "org.springframework.boot",
        "spring-boot",
        "3.0.1",
        null,
        "war",
        "compile",
        null
    );

    var aetherDependencyManagement = new AetherDependencyManagement(mavenSession, artifactMapper);

    // When
    var outputDependency = aetherDependencyManagement.fillManagedAttributes(inputDependency);

    // Then
    assertThat(outputDependency).isSameAs(inputDependency);
  }
}
