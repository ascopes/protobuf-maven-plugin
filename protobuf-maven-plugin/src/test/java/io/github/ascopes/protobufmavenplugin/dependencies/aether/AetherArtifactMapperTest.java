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
import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.RepositoryUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @DisplayName(".mapPmpArtifactToEclipseArtifact(MavenArtifact) returns the expected result")
  @MethodSource("mapPmpArtifactToEclipseArtifactTestCases")
  @ParameterizedTest(name = "when {argumentSetName}")
  void mapPmpArtifactToEclipseArtifactReturnsTheExpectedResult(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact inputMavenArtifact,
      org.eclipse.aether.artifact.@Nullable ArtifactType determinedArtifactType,
      org.eclipse.aether.artifact.Artifact expectedOutputArtifact
  ) {
    // Given
    when(artifactTypeRegistry.get(any())).thenReturn(determinedArtifactType);

    // When
    var actualOutputArtifact = aetherArtifactMapper
        .mapPmpArtifactToEclipseArtifact(inputMavenArtifact);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(actualOutputArtifact.getGroupId())
          .as("groupId").isEqualTo(expectedOutputArtifact.getGroupId());
      softly.assertThat(actualOutputArtifact.getArtifactId())
          .as("artifactId").isEqualTo(expectedOutputArtifact.getArtifactId());
      softly.assertThat(actualOutputArtifact.getVersion())
          .as("version").isEqualTo(expectedOutputArtifact.getVersion());
      softly.assertThat(actualOutputArtifact.getClassifier())
          .as("classifier").isEqualTo(expectedOutputArtifact.getClassifier());
      softly.assertThat(actualOutputArtifact.getExtension())
          .as("extension").isEqualTo(expectedOutputArtifact.getExtension());
    });
  }

  static Stream<Arguments> mapPmpArtifactToEclipseArtifactTestCases() {
    // argumentSet(
    //   description,
    //   input MavenArtifact,
    //   @Nullable result of ArtifactRegistry.get(type),
    //   expected output DefaultArtifact
    // )
    return Stream.of(
        argumentSet(
            "missing input type, null input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                null,
                null
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "jar",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, empty input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "",
                null
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "jar",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, blank input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "\t\t\r\t\n ",
                null
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "jar",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, null input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                null,
                "png"
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, empty input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "",
                "png"
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, blank input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "\n\r\t ",
                "png"
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, null input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                null,
                null
            ),
            eclipseArtifactType("png", "portable-network-graphic"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "portable-network-graphic",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, empty input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "",
                null
            ),
            eclipseArtifactType("png", "portable-network-graphic"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "portable-network-graphic",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, blank input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "\t\t\r\t\n ",
                null
            ),
            eclipseArtifactType("png", "portable-network-graphic"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "portable-network-graphic",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, null input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                null,
                "tiff"
            ),
            eclipseArtifactType("tif", "a-tiff-file"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "a-tiff-file",
                "tif",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, empty input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "",
                "tiff"
            ),
            eclipseArtifactType("tif", "a-tiff-file"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "a-tiff-file",
                "tif",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, blank input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "\t\t\r\t\n ",
                "tiff"
            ),
            eclipseArtifactType("tif", "a-tiff-file"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "a-tiff-file",
                "tif",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, known input classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "ashley",
                null
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "ashley",
                "jar",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, known classifiers, unknown default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "ashley",
                "png"
            ),
            null,
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "ashley",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "missing input type, known input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "ashley",
                null
            ),
            eclipseArtifactType("png", "portable-network-graphic"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "ashley",
                "png",
                "1.2.3"
            )
        ),
        argumentSet(
            "provided input type, known input classifiers, known default artifact type",
            mavenArtifact(
                "org.example",
                "test1",
                "1.2.3",
                "ashley",
                "tiff"
            ),
            eclipseArtifactType("tif", "a-tiff-file"),
            new org.eclipse.aether.artifact.DefaultArtifact(
                "org.example",
                "test1",
                "ashley",
                "tif",
                "1.2.3"
            )
        )
    );
  }

  @DisplayName(".mapPmpArtifactToEclipseDependency(MavenArtifact, DependencyResolutionDepth) "
      + "returns the expected result")
  @MethodSource("mapPmpArtifactToEclipseDependencyTestCases")
  @ParameterizedTest(name = "when {argumentSetName}")
  void mapPmpArtifactToEclipseDependencyReturnsTheExpectedResult(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact inputMavenArtifact,
      io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth defaultDepth,
      boolean expectWildcardExclusion
  ) {
    // Given
    when(artifactTypeRegistry.get(any())).thenReturn(null);
    var expectedOutputArtifact = eclipseArtifact(
        inputMavenArtifact.getGroupId(),
        inputMavenArtifact.getArtifactId(),
        inputMavenArtifact.getVersion(),
        requireNonNullElse(inputMavenArtifact.getClassifier(), ""),
        requireNonNullElse(inputMavenArtifact.getType(), "jar")
    );

    // When
    var actualOutputDependency = aetherArtifactMapper.mapPmpArtifactToEclipseDependency(
        inputMavenArtifact,
        defaultDepth
    );

    // Then
    assertSoftly(softly -> {
      softly.assertThat(actualOutputDependency.getArtifact().getGroupId())
          .as("artifact.groupId").isEqualTo(expectedOutputArtifact.getGroupId());
      softly.assertThat(actualOutputDependency.getArtifact().getArtifactId())
          .as("artifact.artifactId").isEqualTo(expectedOutputArtifact.getArtifactId());
      softly.assertThat(actualOutputDependency.getArtifact().getVersion())
          .as("artifact.version").isEqualTo(expectedOutputArtifact.getVersion());
      softly.assertThat(actualOutputDependency.getArtifact().getClassifier())
          .as("artifact.classifier").isEqualTo(expectedOutputArtifact.getClassifier());
      softly.assertThat(actualOutputDependency.getArtifact().getExtension())
          .as("artifact.extension").isEqualTo(expectedOutputArtifact.getExtension());
      softly.assertThat(actualOutputDependency.getScope())
          .as("scope").isEqualTo("compile");
      softly.assertThat(actualOutputDependency.getOptional())
          .as("optional").isFalse();

      if (expectWildcardExclusion) {
        softly.assertThat(actualOutputDependency.getExclusions())
            .as("exclusions")
            .singleElement()
            .isSameAs(WildcardAwareDependencyTraverser.WILDCARD_EXCLUSION);
      } else {
        softly.assertThat(actualOutputDependency.getExclusions())
            .as("exclusions")
            .isEmpty();
      }
    });
  }

  static Stream<Arguments> mapPmpArtifactToEclipseDependencyTestCases() {
    // argumentSet(
    //   description,
    //   input MavenArtifact,
    //   default DependencyResolutionDepth,
    //   boolean expected wildcard exclusion present
    // )
    return Stream.of(
        argumentSet(
            "when no explicit depth, DIRECT default depth",
            mavenArtifact("org.foo", "bar", "69.420", "xxx", "yyy", null),
            DependencyResolutionDepth.DIRECT,
            true
        ),
        argumentSet(
            "when no explicit depth, TRANSITIVE default depth",
            mavenArtifact("org.foo", "bar", "69.420", "xxx", "yyy", null),
            DependencyResolutionDepth.TRANSITIVE,
            false
        ),
        argumentSet(
            "DIRECT explicit depth, DIRECT default depth",
            mavenArtifact("org.foo", "bar", "69", "xxx", "yyy", DependencyResolutionDepth.DIRECT),
            DependencyResolutionDepth.DIRECT,
            true
        ),
        argumentSet(
            "DIRECT explicit depth, TRANSITIVE default depth",
            mavenArtifact("BAZ", "bar", "69.420", "xxx", "yyy", DependencyResolutionDepth.DIRECT),
            DependencyResolutionDepth.TRANSITIVE,
            true
        ),
        argumentSet(
            "TRANSITIVE explicit depth, DIRECT default depth",
            mavenArtifact("org", "bar", "69", null, null, DependencyResolutionDepth.TRANSITIVE),
            DependencyResolutionDepth.DIRECT,
            false
        ),
        argumentSet(
            "DIRECT explicit depth, TRANSITIVE default depth",
            mavenArtifact("xxx", "bar", "420", null, null, DependencyResolutionDepth.TRANSITIVE),
            DependencyResolutionDepth.TRANSITIVE,
            false
        )
    );
  }

  @DisplayName(".mapMavenDependencyToEclipseDependency(Dependency) returns the expected result")
  @Test
  void mapMavenDependencyToEclipseDependencyReturnsTheExpectedResult() {
    try (var repositoryUtils = mockStatic(RepositoryUtils.class)) {
      // Given
      var expectedDependency = mock(org.eclipse.aether.graph.Dependency.class);
      repositoryUtils.when(() -> RepositoryUtils.toDependency(any(), same(artifactTypeRegistry)))
          .thenReturn(expectedDependency);
      var inputDependency = mock(org.apache.maven.model.Dependency.class);

      // When
      var actualDependency = aetherArtifactMapper
          .mapMavenDependencyToEclipseDependency(inputDependency);

      // Then
      repositoryUtils.verify(() -> RepositoryUtils
          .toDependency(inputDependency, artifactTypeRegistry));
      repositoryUtils.verifyNoMoreInteractions();

      assertThat(actualDependency)
          .isSameAs(expectedDependency);
    }
  }

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
      @Nullable DependencyResolutionDepth dependencyResolutionDepth
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
    when(artifactType.getExtension()).thenReturn(extension);
    when(artifactType.getClassifier()).thenReturn(classifier);
    return artifactType;
  }

  static org.eclipse.aether.artifact.Artifact eclipseArtifact(
      String groupId,
      String artifactId,
      String version,
      String classifier,
      String extension
  ) {
    return new org.eclipse.aether.artifact.DefaultArtifact(
        groupId,
        artifactId,
        classifier,
        extension,
        version
    );
  }
}
