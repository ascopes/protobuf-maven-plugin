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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

@DisplayName("AetherArtifactMapper tests")
@ExtendWith(MockitoExtension.class)
class AetherArtifactMapperTest {

  @Mock
  org.eclipse.aether.artifact.ArtifactTypeRegistry artifactTypeRegistry;

  @InjectMocks
  AetherArtifactMapper aetherArtifactMapper;

  @DisplayName(".getArtifactRegistry() returns the artifact registry")
  @Test
  void getArtifactRegistryReturnsTheArtifactRegistry() {
    // Then
    assertThat(aetherArtifactMapper.getArtifactTypeRegistry())
        .isSameAs(artifactTypeRegistry);
  }

  @DisplayName(".mapEclipseArtifactToPath(Artifact) returns the artifact path")
  @Test
  @SuppressWarnings("deprecation")
  void mapEclipseArtifactToPathReturnsTheArtifactPath(@TempDir Path dir) {
    // Given
    var file = dir.resolve(someBasicString() + ".xml");
    var artifact = mock(org.eclipse.aether.artifact.Artifact.class);
    when(artifact.getFile()).thenReturn(file.toFile());

    // When
    var actualFile = aetherArtifactMapper.mapEclipseArtifactToPath(artifact);

    // Then
    assertThat(actualFile).isEqualTo(file);
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "missing types, missing classifiers and unknown artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact0(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, null);
    when(artifactTypeRegistry.get(any())).thenReturn(null);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo("jar");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "missing types, known classifiers and unknown artifact types")
  @Test
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact1() {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", "a jar", null);
    when(artifactTypeRegistry.get(any())).thenReturn(null);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("a jar");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo("jar");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "provided types, missing classifiers and unknown artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact2(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, "png");
    when(artifactTypeRegistry.get(any())).thenReturn(null);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo("png");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "known types, known classifiers and unknown artifact types")
  @Test
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact3() {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", "a png", "png");
    when(artifactTypeRegistry.get(any())).thenReturn(null);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("a png");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo("png");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "provided types, missing classifiers and unknown artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact4(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, "png");
    when(artifactTypeRegistry.get(any())).thenReturn(null);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo("png");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "missing types, missing classifiers and known artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact5(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, null);
    var artifactType = eclipseArtifactType("boop", "beep");
    when(artifactTypeRegistry.get(any())).thenReturn(artifactType);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("gzipped-tarball");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo(".tar.gz");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "missing types, known classifiers and known artifact types")
  @Test
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact6() {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", "a jar", null);
    var artifactType = eclipseArtifactType("boop", "beep");
    when(artifactTypeRegistry.get(any())).thenReturn(artifactType);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("a jar");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo(".tar.gz");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "provided types, missing classifiers and known artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact7(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, "png");
    var artifactType = eclipseArtifactType("boop", "beep");
    when(artifactTypeRegistry.get(any())).thenReturn(artifactType);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("gzipped-tarball");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo(".tar.gz");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "known types, known classifiers and known artifact types")
  @Test
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact8() {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", "a png", "png");
    var artifactType = eclipseArtifactType("boop", "beep");
    when(artifactTypeRegistry.get(any())).thenReturn(artifactType);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("a png");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo(".tar.gz");
  }

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected artifact for "
      + "provided types, missing classifiers and known artifact types")
  @NullAndEmptySource
  @ValueSource(strings = "        ")
  @ParameterizedTest(name = "for classifier = \"{0}\"")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedArtifact(
      String classifier
  ) {
    // Given
    var mavenArtifact = mavenArtifact("org.example", "sketchy-product", "1.2.3", classifier, "png");
    var artifactType = eclipseArtifactType("boop", "beep");
    when(artifactTypeRegistry.get(any())).thenReturn(artifactType);

    // When
    var artifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);

    // Then
    assertThat(artifact.getGroupId()).as("groupId")
        .isEqualTo(mavenArtifact.getGroupId());
    assertThat(artifact.getArtifactId()).as("artifactId")
        .isEqualTo(mavenArtifact.getArtifactId());
    assertThat(artifact.getVersion()).as("version")
        .isEqualTo(mavenArtifact.getVersion());
    assertThat(artifact.getClassifier()).as("classifier")
        .isEqualTo("gzipped-tarball");
    assertThat(artifact.getExtension()).as("extension")
        .isEqualTo(".tar.gz");
  }

  // TODO: continue these

  @SuppressWarnings("SameParameterValue")
  static io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact(
      String groupId,
      String artifactId,
      String version,
      @Nullable String classifier,
      @Nullable String type
  ) {
    return mavenArtifact(
        groupId,
        artifactId,
        version,
        classifier,
        type,
        DependencyResolutionDepth.TRANSITIVE
    );
  }

  @SuppressWarnings("SameParameterValue")
  static io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact(
      String groupId,
      String artifactId,
      String version,
      @Nullable String classifier,
      @Nullable String type,
      DependencyResolutionDepth dependencyResolutionDepth
  ) {
    var artifact = mock(
        io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact.class,
        withSettings().strictness(Strictness.LENIENT)
    );
    when(artifact.getGroupId()).thenReturn(groupId);
    when(artifact.getArtifactId()).thenReturn(artifactId);
    when(artifact.getVersion()).thenReturn(version);
    when(artifact.getType()).thenReturn(type);
    when(artifact.getClassifier()).thenReturn(classifier);
    when(artifact.getDependencyResolutionDepth()).thenReturn(dependencyResolutionDepth);
    return artifact;
  }

  static org.eclipse.aether.artifact.ArtifactType eclipseArtifactType(
      String extension,
      String classifier
  ) {
    var artifactType = mock(org.eclipse.aether.artifact.ArtifactType.class);
    when(artifactType.getExtension()).thenReturn(".tar.gz");
    when(artifactType.getClassifier()).thenReturn("gzipped-tarball");
    return artifactType;
  }
}
