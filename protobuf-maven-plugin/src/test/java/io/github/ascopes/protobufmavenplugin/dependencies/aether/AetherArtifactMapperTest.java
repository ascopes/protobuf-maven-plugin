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

import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.eclipseArtifact;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.eclipseArtifactType;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.eclipseDependency;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.mavenArtifact;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.mavenDependency;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.pmpDependency;
import static io.github.ascopes.protobufmavenplugin.fixtures.DependencyFixtures.pmpExclusion;
import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.RepositoryUtils;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("AetherArtifactMapper tests")
class AetherArtifactMapperTest {

  ArtifactTypeRegistry artifactTypeRegistry;
  ProtobufMavenPluginRepositorySession repositorySession;
  AetherArtifactMapper aetherArtifactMapper;

  @BeforeEach
  void setUp() {
    artifactTypeRegistry = mock();
    repositorySession = mock();
    when(repositorySession.getArtifactTypeRegistry()).thenReturn(artifactTypeRegistry);

    aetherArtifactMapper = new AetherArtifactMapper(repositorySession);
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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
            pmpDependency(
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

  @DisplayName(".mapPmpArtifactToEclipseDependency(MavenDependency, DependencyResolutionDepth) "
      + "returns the expected result")
  @MethodSource("mapPmpArtifactToEclipseDependencyTestCases")
  @ParameterizedTest(name = "when {argumentSetName}")
  void mapPmpArtifactDependencyToEclipseDependencyReturnsTheExpectedResult(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency inputMavenArtifact,
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
        var expectedExclusions = inputMavenArtifact.getExclusions()
            .stream()
            .map(exclusion -> new org.eclipse.aether.graph.Exclusion(
                exclusion.getGroupId(),
                exclusion.getArtifactId(),
                exclusion.getClassifier(),
                exclusion.getType()
            ))
            .collect(Collectors.toList());

        softly.assertThat(actualOutputDependency.getExclusions())
            .as("exclusions")
            .containsExactlyInAnyOrderElementsOf(expectedExclusions);
      }
    });
  }

  static Stream<Arguments> mapPmpArtifactToEclipseDependencyTestCases() {
    // argumentSet(
    //   description,
    //   input MavenDependency,
    //   default DependencyResolutionDepth,
    //   boolean expected wildcard exclusion present
    // )
    return Stream.of(
        argumentSet(
            "when no explicit depth, DIRECT default depth",
            pmpDependency(
                "org.foo",
                "bar",
                "69.420",
                "xxx",
                "yyy",
                null
            ),
            DependencyResolutionDepth.DIRECT,
            true
        ),
        argumentSet(
            "when no explicit depth, TRANSITIVE default depth",
            pmpDependency("org.foo",
                "bar",
                "69.420",
                "xxx",
                "yyy",
                null),
            DependencyResolutionDepth.TRANSITIVE,
            false
        ),
        argumentSet(
            "DIRECT explicit depth, DIRECT default depth",
            pmpDependency("org.foo",
                "bar",
                "69",
                "xxx",
                "yyy",
                DependencyResolutionDepth.DIRECT),
            DependencyResolutionDepth.DIRECT,
            true
        ),
        argumentSet(
            "DIRECT explicit depth, TRANSITIVE default depth",
            pmpDependency("BAZ",
                "bar",
                "69.420",
                "xxx",
                "yyy",
                DependencyResolutionDepth.DIRECT),
            DependencyResolutionDepth.TRANSITIVE,
            true
        ),
        argumentSet(
            "TRANSITIVE explicit depth, DIRECT default depth",
            pmpDependency("org",
                "bar",
                "69",
                null,
                null,
                DependencyResolutionDepth.TRANSITIVE),
            DependencyResolutionDepth.DIRECT,
            false
        ),
        argumentSet(
            "DIRECT explicit depth, TRANSITIVE default depth",
            pmpDependency("xxx",
                "bar",
                "420",
                null,
                null,
                DependencyResolutionDepth.TRANSITIVE),
            DependencyResolutionDepth.TRANSITIVE,
            false
        ),
        argumentSet(
            "TRANSITIVE explicit depth, exclusions present",
            pmpDependency(
                "xxx",
                "bar",
                "420",
                null,
                null,
                DependencyResolutionDepth.TRANSITIVE,
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-webflux",
                    "*",
                    "*"
                ),
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-logging",
                    "*",
                    "jar"
                ),
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-parent",
                    "test",
                    "pom"
                )
            ),
            DependencyResolutionDepth.DIRECT,
            false
        ),
        argumentSet(
            "TRANSITIVE default depth, exclusions present",
            pmpDependency(
                "xxx",
                "bar",
                "420",
                null,
                null,
                null,
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-webflux",
                    "*",
                    "*"
                ),
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-logging",
                    "*",
                    "jar"
                ),
                pmpExclusion(
                    "org.springframework.boot",
                    "spring-boot-starter-parent",
                    "test",
                    "pom"
                )
            ),
            DependencyResolutionDepth.TRANSITIVE,
            false
        )
    );
  }

  @DisplayName(".mapPmpArtifactToEclipseDependency(MavenArtifact, DependencyResolutionDepth) "
      + "returns the expected result")
  @Test
  void mapPmpArtifactArtifactToEclipseDependencyReturnsTheExpectedResult() {
    // Given
    var inputMavenArtifact = mock(io.github.ascopes.protobufmavenplugin.dependencies
        .MavenArtifact.class);
    when(inputMavenArtifact.getGroupId())
        .thenReturn("foo");
    when(inputMavenArtifact.getArtifactId())
        .thenReturn("bar");
    when(inputMavenArtifact.getVersion())
        .thenReturn("baz");
    when(inputMavenArtifact.getClassifier())
        .thenReturn("eggs");
    when(inputMavenArtifact.getType())
        .thenReturn("spam");

    when(artifactTypeRegistry.get(any())).thenReturn(null);
    var expectedOutputArtifact = eclipseArtifact(
        inputMavenArtifact.getGroupId(),
        inputMavenArtifact.getArtifactId(),
        inputMavenArtifact.getVersion(),
        inputMavenArtifact.getClassifier(),
        inputMavenArtifact.getType()
    );

    // When
    var actualOutputDependency = aetherArtifactMapper.mapPmpArtifactToEclipseDependency(
        inputMavenArtifact,
        DependencyResolutionDepth.TRANSITIVE
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
      softly.assertThat(actualOutputDependency.getExclusions())
          .as("exclusions").isEmpty();
    });
  }

  @DisplayName(".mapMavenDependencyToEclipseArtifact(Dependency) returns the expected result")
  @Test
  void mapMavenDependencyToEclipseArtifactReturnsTheExpectedResult() {
    try (var repositoryUtils = mockStatic(RepositoryUtils.class)) {
      // Given
      var expectedDependency = eclipseDependency(
          "org.example",
          "foo-bar",
          "1.2.3.4",
          "blah",
          "ping",
          "compile",
          null
      );
      repositoryUtils.when(() -> RepositoryUtils.toDependency(any(), same(artifactTypeRegistry)))
          .thenReturn(expectedDependency);
      var inputDependency = mavenDependency(
          "org.example",
          "foo-bar",
          "1.2.3.4",
          "ping",
          "blah"
      );

      // When
      var actualArtifact = aetherArtifactMapper
          .mapMavenDependencyToEclipseArtifact(inputDependency);

      // Then
      repositoryUtils
          .verify(() -> RepositoryUtils.toDependency(inputDependency, artifactTypeRegistry));
      repositoryUtils
          .verifyNoMoreInteractions();

      assertThat(actualArtifact)
          .isSameAs(expectedDependency.getArtifact());
    }
  }

  @DisplayName(".mapMavenArtifactToEclipseArtifact(Artifact) returns the expected result")
  @Test
  void mapMavenArtifactToEclipseArtifactReturnsTheExpectedResult() {
    try (var repositoryUtils = mockStatic(RepositoryUtils.class)) {
      // Given
      var expectedArtifact = eclipseArtifact(
          "org.example",
          "foo-bar",
          "1.2.3.4",
          "blah",
          "png"
      );
      repositoryUtils
          .when(() -> RepositoryUtils.toArtifact(any(org.apache.maven.artifact.Artifact.class)))
          .thenReturn(expectedArtifact);
      var inputArtifact = mavenArtifact(
          "org.example",
          "foo-bar",
          "1.2.3.4",
          "blah",
          "png"
      );

      // When
      var actualArtifact = aetherArtifactMapper
          .mapMavenArtifactToEclipseArtifact(inputArtifact);

      // Then
      repositoryUtils
          .verify(() -> RepositoryUtils.toArtifact(inputArtifact));
      repositoryUtils
          .verifyNoMoreInteractions();

      assertThat(actualArtifact)
          .isSameAs(expectedArtifact);
    }
  }
}
