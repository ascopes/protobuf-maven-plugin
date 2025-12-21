/*
 * Copyright (C) 2023 Ashley Scopes
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

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.oneOf;
import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@DisplayName("AetherMavenArtifactPathResolver tests")
@ExtendWith(MockitoExtension.class)
class AetherMavenArtifactPathResolverTest {

  @TempDir
  Path tempDir;

  Path temporarySpacePath;

  @Mock
  MavenSession mavenSession;

  @Mock
  AetherArtifactMapper aetherArtifactMapper;

  @Mock
  AetherDependencyManagement aetherDependencyManagement;

  @Mock
  AetherResolver aetherResolver;

  @Mock(strictness = Strictness.LENIENT)
  TemporarySpace temporarySpace;

  @InjectMocks
  AetherMavenArtifactPathResolver resolver;

  @BeforeEach
  void setUp() throws IOException {
    temporarySpacePath = tempDir.resolve("temporary-space");
    when(temporarySpace.createTemporarySpace(any(String[].class)))
        .thenReturn(temporarySpacePath);
    Files.createDirectories(temporarySpacePath);
  }

  @DisplayName(".resolveExecutable(...) resolves the artifact")
  @Test
  void resolveExecutableResolvesTheArtifact() throws ResolutionException, IOException {
    // Given
    var artifactId = "someArtifactId-" + someBasicString();
    var inputArtifact = mock(MavenArtifact.class, "SomeArtifact-" + someBasicString());
    var unresolvedArtifact = mock(Artifact.class);
    var resolvedArtifact = mock(Artifact.class);
    var originalPath = Files.writeString(
        tempDir.resolve("some-artifact.exe"),
        "Some expected binary content here " + someBasicString()
    );

    when(inputArtifact.getArtifactId())
        .thenReturn(artifactId);
    when(aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(any()))
        .thenReturn(unresolvedArtifact);
    when(aetherResolver.resolveRequiredArtifact(any()))
        .thenReturn(resolvedArtifact);
    when(aetherArtifactMapper.mapEclipseArtifactToPath(any()))
        .thenReturn(originalPath);

    var expectedFileName = artifactId
        + "-"
        + Digest.compute("SHA-1", inputArtifact.toString())
        .toHexString()
        + ".exe";

    // When
    var resolvedPath = resolver.resolveExecutable(inputArtifact);

    // Then
    assertThat(resolvedPath)
        .isEqualTo(temporarySpacePath.resolve(expectedFileName))
        .hasSameBinaryContentAs(originalPath)
        .isExecutable();

    verify(aetherArtifactMapper).mapPmpArtifactToEclipseArtifact(inputArtifact);
    verify(aetherResolver).resolveRequiredArtifact(unresolvedArtifact);
    verify(aetherArtifactMapper).mapEclipseArtifactToPath(resolvedArtifact);
    verifyNoMoreInteractions(aetherArtifactMapper, aetherResolver);
  }

  @DisplayName(".resolveExecutable(...) resolves the artifact if already present")
  @Test
  void resolveExecutableResolvesTheArtifactWhenAlreadyPresent()
      throws ResolutionException, IOException {
    // Given
    var artifactId = "someArtifactId-" + someBasicString();
    var inputArtifact = mock(MavenArtifact.class, "SomeArtifact-" + someBasicString());
    var unresolvedArtifact = mock(Artifact.class);
    var resolvedArtifact = mock(Artifact.class);
    var originalPath = Files.writeString(
        tempDir.resolve("some-artifact.exe"),
        "Some expected binary content here " + someBasicString()
    );

    when(inputArtifact.getArtifactId())
        .thenReturn(artifactId);
    when(aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(any()))
        .thenReturn(unresolvedArtifact);
    when(aetherResolver.resolveRequiredArtifact(any()))
        .thenReturn(resolvedArtifact);
    when(aetherArtifactMapper.mapEclipseArtifactToPath(any()))
        .thenReturn(originalPath);

    var expectedFileName = artifactId
        + "-"
        + Digest.compute("SHA-1", inputArtifact.toString())
        .toHexString()
        + ".exe";

    // Given some file is already in the location... we don't expect this to fail.
    Files.writeString(temporarySpacePath.resolve(expectedFileName), "some garbage I don't want");

    // When
    var resolvedPath = resolver.resolveExecutable(inputArtifact);

    // Then
    assertThat(resolvedPath)
        .isEqualTo(temporarySpacePath.resolve(expectedFileName))
        .hasSameBinaryContentAs(originalPath)
        .isExecutable();

    verify(aetherArtifactMapper).mapPmpArtifactToEclipseArtifact(inputArtifact);
    verify(aetherResolver).resolveRequiredArtifact(unresolvedArtifact);
    verify(aetherArtifactMapper).mapEclipseArtifactToPath(resolvedArtifact);
    verifyNoMoreInteractions(aetherArtifactMapper, aetherResolver);
  }

  @DisplayName("resolveDependencies(...) without project artifacts resolves the dependencies")
  @MethodSource("resolveDependenciesParams")
  @ParameterizedTest(name = "{argumentSetName}")
  void resolveDependenciesWithoutProjectArtifactsResolvesTheDependencies(
      DependencyResolutionDepth dependencyResolutionDepth,
      Set<String> dependencyScopes
  ) throws ResolutionException {
    // Given
    try (var depMgmtStatic = mockStatic(AetherDependencyManagement.class)) {
      depMgmtStatic.when(AetherDependencyManagement::deduplicateArtifacts)
          .thenReturn(Collectors.toMap(
              Objects::toString,
              Function.identity(),
              (a, b) -> {
                throw new IllegalArgumentException("welp");
              },
              LinkedHashMap::new
          ));

      var inputArtifact1 = mock(MavenArtifact.class, "input artifact 1");
      var inputArtifact2 = mock(MavenArtifact.class, "input artifact 2");
      var inputArtifact3 = mock(MavenArtifact.class, "input artifact 3");
      var inputArtifact4 = mock(MavenArtifact.class, "input artifact 4");

      var aetherUnfilledDependency1 = mock(Dependency.class, "aether unfilled dependency 1");
      var aetherUnfilledDependency2 = mock(Dependency.class, "aether unfilled dependency 2");
      var aetherUnfilledDependency3 = mock(Dependency.class, "aether unfilled dependency 3");
      var aetherUnfilledDependency4 = mock(Dependency.class, "aether unfilled dependency 4");

      when(aetherArtifactMapper.mapPmpArtifactToEclipseDependency(any(), any()))
          .then(givenReturn(
              entry(inputArtifact1, aetherUnfilledDependency1),
              entry(inputArtifact2, aetherUnfilledDependency2),
              entry(inputArtifact3, aetherUnfilledDependency3),
              entry(inputArtifact4, aetherUnfilledDependency4)
          ));

      var aetherFilledDependency1 = mock(Dependency.class, "aether filled dependency 1");
      var aetherFilledDependency2 = mock(Dependency.class, "aether filled dependency 2");
      var aetherFilledDependency3 = mock(Dependency.class, "aether filled dependency 3");
      var aetherFilledDependency4 = mock(Dependency.class, "aether filled dependency 4");

      when(aetherDependencyManagement.fillManagedAttributes(any()))
          .then(givenReturn(
              entry(aetherUnfilledDependency1, aetherFilledDependency1),
              entry(aetherUnfilledDependency2, aetherFilledDependency2),
              entry(aetherUnfilledDependency3, aetherFilledDependency3),
              entry(aetherUnfilledDependency4, aetherFilledDependency4)
          ));

      var aetherOutputArtifact1 = mock(Artifact.class, "output artifact 1");
      var aetherOutputArtifact2 = mock(Artifact.class, "output artifact 2");
      var aetherOutputArtifact3 = mock(Artifact.class, "output artifact 3");
      var aetherOutputArtifact4 = mock(Artifact.class, "output artifact 4");
      var aetherOutputArtifact5 = mock(Artifact.class, "output artifact 5");
      var aetherOutputArtifact6 = mock(Artifact.class, "output artifact 6");

      when(aetherResolver.resolveDependencies(any(), any()))
          .thenReturn(List.of(
              aetherOutputArtifact1,
              aetherOutputArtifact2,
              aetherOutputArtifact3,
              aetherOutputArtifact4,
              aetherOutputArtifact5,
              aetherOutputArtifact6
          ));

      var path1 = mock(Path.class, "path 1");
      var path2 = mock(Path.class, "path 2");
      var path3 = mock(Path.class, "path 3");
      var path4 = mock(Path.class, "path 4");
      var path5 = mock(Path.class, "path 5");
      var path6 = mock(Path.class, "path 6");

      when(aetherArtifactMapper.mapEclipseArtifactToPath(any()))
          .then(givenReturn(
              entry(aetherOutputArtifact1, path1),
              entry(aetherOutputArtifact2, path2),
              entry(aetherOutputArtifact3, path3),
              entry(aetherOutputArtifact4, path4),
              entry(aetherOutputArtifact5, path5),
              entry(aetherOutputArtifact6, path6)
          ));

      // When
      var returnedPaths = resolver.resolveDependencies(
          List.of(inputArtifact1, inputArtifact2, inputArtifact3, inputArtifact4),
          dependencyResolutionDepth,
          dependencyScopes,
          false
      );

      // Then
      assertThat(returnedPaths)
          .containsExactly(path1, path2, path3, path4, path5, path6);

      Stream
          .of(inputArtifact1, inputArtifact2, inputArtifact3, inputArtifact4)
          .forEach(arg -> verify(aetherArtifactMapper).mapPmpArtifactToEclipseDependency(
              arg, dependencyResolutionDepth
          ));

      Stream
          .of(
              aetherUnfilledDependency1,
              aetherUnfilledDependency2,
              aetherUnfilledDependency3,
              aetherUnfilledDependency4
          )
          .forEach(arg -> verify(aetherDependencyManagement).fillManagedAttributes(arg));

      verify(aetherResolver).resolveDependencies(
          List.of(
              aetherFilledDependency1,
              aetherFilledDependency2,
              aetherFilledDependency3,
              aetherFilledDependency4
          ),
          dependencyScopes
      );

      depMgmtStatic.verify(AetherDependencyManagement::deduplicateArtifacts);

      Stream
          .of(
              aetherOutputArtifact1,
              aetherOutputArtifact2,
              aetherOutputArtifact3,
              aetherOutputArtifact4,
              aetherOutputArtifact5,
              aetherOutputArtifact6
          )
          .forEach(arg -> verify(aetherArtifactMapper).mapEclipseArtifactToPath(arg));

      verifyNoMoreInteractions(aetherArtifactMapper, aetherResolver, aetherDependencyManagement);
      verifyNoInteractions(mavenSession);
    }
  }

  @DisplayName("resolveDependencies(...) with project artifacts resolves the dependencies")
  @MethodSource("resolveDependenciesParams")
  @ParameterizedTest(name = "{argumentSetName}")
  void resolveDependenciesWithProjectArtifactsResolvesTheDependencies(
      DependencyResolutionDepth dependencyResolutionDepth,
      Set<String> dependencyScopes
  ) throws ResolutionException {
    // Given
    try (var depMgmtStatic = mockStatic(AetherDependencyManagement.class)) {
      depMgmtStatic.when(AetherDependencyManagement::deduplicateArtifacts)
          .thenReturn(Collectors.toMap(
              Objects::toString,
              Function.identity(),
              (a, b) -> {
                throw new IllegalArgumentException("welp");
              },
              LinkedHashMap::new
          ));

      var inputArtifact1 = mock(MavenArtifact.class, "input artifact 1");
      var inputArtifact2 = mock(MavenArtifact.class, "input artifact 2");
      var inputArtifact3 = mock(MavenArtifact.class, "input artifact 3");
      var inputArtifact4 = mock(MavenArtifact.class, "input artifact 4");

      var aetherUnfilledDependency1 = mock(Dependency.class, "aether unfilled dependency 1");
      var aetherUnfilledDependency2 = mock(Dependency.class, "aether unfilled dependency 2");
      var aetherUnfilledDependency3 = mock(Dependency.class, "aether unfilled dependency 3");
      var aetherUnfilledDependency4 = mock(Dependency.class, "aether unfilled dependency 4");

      when(aetherArtifactMapper.mapPmpArtifactToEclipseDependency(any(), any()))
          .then(givenReturn(
              entry(inputArtifact1, aetherUnfilledDependency1),
              entry(inputArtifact2, aetherUnfilledDependency2),
              entry(inputArtifact3, aetherUnfilledDependency3),
              entry(inputArtifact4, aetherUnfilledDependency4)
          ));

      var aetherFilledDependency1 = mock(Dependency.class, "aether filled dependency 1");
      var aetherFilledDependency2 = mock(Dependency.class, "aether filled dependency 2");
      var aetherFilledDependency3 = mock(Dependency.class, "aether filled dependency 3");
      var aetherFilledDependency4 = mock(Dependency.class, "aether filled dependency 4");

      when(aetherDependencyManagement.fillManagedAttributes(any()))
          .then(givenReturn(
              entry(aetherUnfilledDependency1, aetherFilledDependency1),
              entry(aetherUnfilledDependency2, aetherFilledDependency2),
              entry(aetherUnfilledDependency3, aetherFilledDependency3),
              entry(aetherUnfilledDependency4, aetherFilledDependency4)
          ));

      var mavenOutputArtifact1 = mock(org.apache.maven.artifact.Artifact.class);
      when(mavenOutputArtifact1.getScope()).thenReturn(oneOf(dependencyScopes));

      var mavenOutputArtifact2 = mock(org.apache.maven.artifact.Artifact.class);
      when(mavenOutputArtifact2.getScope()).thenReturn(oneOf(dependencyScopes));

      var mavenOutputArtifact3 = mock(org.apache.maven.artifact.Artifact.class);

      var project = mock(MavenProject.class);
      when(mavenSession.getCurrentProject())
          .thenReturn(project);
      when(project.getArtifacts())
          // Order matters, so don't use Set.of here.
          .thenReturn(new LinkedHashSet<>(List.of(mavenOutputArtifact1, mavenOutputArtifact2)));

      var aetherOutputArtifact1 = mock(Artifact.class, "output artifact 1");
      var aetherOutputArtifact2 = mock(Artifact.class, "output artifact 2");
      var aetherOutputArtifact3 = mock(Artifact.class, "output artifact 3");
      var aetherOutputArtifact4 = mock(Artifact.class, "output artifact 4");
      var aetherOutputArtifact5 = mock(Artifact.class, "output artifact 5");
      var aetherOutputArtifact6 = mock(Artifact.class, "output artifact 6");
      var aetherOutputArtifact7 = mock(Artifact.class, "output artifact 7");
      var aetherOutputArtifact8 = mock(Artifact.class, "output artifact 8");

      when(aetherResolver.resolveDependencies(any(), any()))
          .thenReturn(List.of(
              aetherOutputArtifact1,
              aetherOutputArtifact2,
              aetherOutputArtifact3,
              aetherOutputArtifact4,
              aetherOutputArtifact5,
              aetherOutputArtifact6
          ));

      when(aetherArtifactMapper.mapMavenArtifactToEclipseArtifact(any()))
          .then(givenReturn(
              entry(mavenOutputArtifact1, aetherOutputArtifact7),
              entry(mavenOutputArtifact2, aetherOutputArtifact8),
              entry(mavenOutputArtifact3, mock("you shouldn't see this"))
          ));

      var path1 = mock(Path.class, "path 1");
      var path2 = mock(Path.class, "path 2");
      var path3 = mock(Path.class, "path 3");
      var path4 = mock(Path.class, "path 4");
      var path5 = mock(Path.class, "path 5");
      var path6 = mock(Path.class, "path 6");
      var path7 = mock(Path.class, "path 7");
      var path8 = mock(Path.class, "path 8");

      when(aetherArtifactMapper.mapEclipseArtifactToPath(any()))
          .then(givenReturn(
              entry(aetherOutputArtifact1, path1),
              entry(aetherOutputArtifact2, path2),
              entry(aetherOutputArtifact3, path3),
              entry(aetherOutputArtifact4, path4),
              entry(aetherOutputArtifact5, path5),
              entry(aetherOutputArtifact6, path6),
              entry(aetherOutputArtifact7, path7),
              entry(aetherOutputArtifact8, path8)
          ));

      // When
      var returnedPaths = resolver.resolveDependencies(
          List.of(inputArtifact1, inputArtifact2, inputArtifact3, inputArtifact4),
          dependencyResolutionDepth,
          dependencyScopes,
          true
      );

      // Then
      assertThat(returnedPaths)
          // Project artifacts are first, as we then override them if we want
          .containsExactly(path7, path8, path1, path2, path3, path4, path5, path6);

      Stream
          .of(inputArtifact1, inputArtifact2, inputArtifact3, inputArtifact4)
          .forEach(arg -> verify(aetherArtifactMapper).mapPmpArtifactToEclipseDependency(
              arg, dependencyResolutionDepth
          ));

      Stream
          .of(
              aetherUnfilledDependency1,
              aetherUnfilledDependency2,
              aetherUnfilledDependency3,
              aetherUnfilledDependency4
          )
          .forEach(arg -> verify(aetherDependencyManagement).fillManagedAttributes(arg));

      verify(aetherResolver).resolveDependencies(
          List.of(
              aetherFilledDependency1,
              aetherFilledDependency2,
              aetherFilledDependency3,
              aetherFilledDependency4
          ),
          dependencyScopes
      );

      depMgmtStatic.verify(AetherDependencyManagement::deduplicateArtifacts);

      Stream
          .of(mavenOutputArtifact1, mavenOutputArtifact2)
          .forEach(arg -> verify(aetherArtifactMapper).mapMavenArtifactToEclipseArtifact(arg));

      Stream
          .of(
              aetherOutputArtifact1,
              aetherOutputArtifact2,
              aetherOutputArtifact3,
              aetherOutputArtifact4,
              aetherOutputArtifact5,
              aetherOutputArtifact6,
              aetherOutputArtifact7,
              aetherOutputArtifact8
          )
          .forEach(arg -> verify(aetherArtifactMapper).mapEclipseArtifactToPath(arg));

      verify(mavenSession).getCurrentProject();
      verify(project).getArtifacts();

      verifyNoMoreInteractions(
          aetherArtifactMapper,
          aetherResolver,
          aetherDependencyManagement,
          mavenSession,
          project
      );

      verifyNoInteractions(mavenOutputArtifact3);
    }
  }

  static Stream<Arguments> resolveDependenciesParams() {
    return Stream.of(
        argumentSet(
            "DIRECT depth, compile and test scopes, fail on invalid dependencies",
            DependencyResolutionDepth.DIRECT,
            Set.of("compile", "test")
        ),
        argumentSet(
            "DIRECT depth, compile, provided, system scopes, allow invalid dependencies",
            DependencyResolutionDepth.DIRECT,
            Set.of("compile", "provided", "system")
        ),
        argumentSet(
            "TRANSITIVE depth, compile scope, allow invalid dependencies",
            DependencyResolutionDepth.TRANSITIVE,
            Set.of("compile")
        )
    );
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  static <A, R> Answer<R> givenReturn(
      Entry<A, R>... entries
  ) {
    return (ctx) -> {
      @SuppressWarnings("unchecked")
      var arg = (A) ctx.getArgument(0);
      for (var entry : entries) {
        if (entry.getKey().equals(arg)) {
          return entry.getValue();
        }
      }
      throw new IllegalArgumentException("Invalid stub, got unexpected argument " + arg);
    };
  }
}
