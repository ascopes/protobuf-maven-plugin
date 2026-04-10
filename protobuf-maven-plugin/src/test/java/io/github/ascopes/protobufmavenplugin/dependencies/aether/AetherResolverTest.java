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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.util.List;
import java.util.Set;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("AetherResolver tests")
@ExtendWith(MockitoExtension.class)
class AetherResolverTest {

  @Mock(strictness = Strictness.LENIENT)
  RepositorySystem repositorySystem;

  @Mock
  RepositorySystemSession repositorySystemSession;

  @Mock(strictness = Strictness.LENIENT)
  MavenProject mavenProject;

  @InjectMocks
  AetherResolver aetherResolver;

  // Internal details
  @Mock
  List<RemoteRepository> inputRemoteRepositories;

  @Mock
  List<RemoteRepository> expectedRemoteRepositories;

  @BeforeEach
  void setUp() {
    when(mavenProject.getRemoteProjectRepositories())
        .thenReturn(inputRemoteRepositories);
    when(repositorySystem.newResolutionRepositories(any(), any()))
        .thenReturn(expectedRemoteRepositories);
  }

  @DisplayName("resolveArtifact(Artifact) returns the resolved artifact")
  @Test
  void resolveArtifactReturnsTheResolvedArtifact() throws Exception {
    // Given
    var inputArtifact = mock(Artifact.class);
    var expectedOutputArtifact = mock(Artifact.class);

    var artifactResult = mock(ArtifactResult.class);
    when(artifactResult.isResolved())
        .thenReturn(true);
    when(artifactResult.getArtifact())
        .thenReturn(expectedOutputArtifact);

    when(repositorySystem.resolveArtifact(any(), any()))
        .thenReturn(artifactResult);

    var artifactRequestCaptor = ArgumentCaptor.forClass(ArtifactRequest.class);

    // When
    var actualOutputArtifact = aetherResolver.resolveArtifact(inputArtifact);

    // Then
    assertThat(actualOutputArtifact).isSameAs(expectedOutputArtifact);

    verifyRemoteRepositoriesConfiguredCorrectly();
    verify(repositorySystem)
        .resolveArtifact(same(repositorySystemSession), artifactRequestCaptor.capture());
    assertThat(artifactRequestCaptor.getValue())
        .satisfies(
            request -> assertThat(request.getArtifact()).isEqualTo(inputArtifact),
            request -> assertThat(request.getRepositories()).isEqualTo(expectedRemoteRepositories)
        );
  }

  @DisplayName("resolveArtifact(Artifact) throws an exception if Aether raises an exception")
  @Test
  void resolveArtifactThrowsExceptionIfAetherResolvesException() throws Exception {
    // Given
    var inputArtifact = mock(Artifact.class, "com.example.foo:bar:1.3.5");
    var artifactResult = mock(ArtifactResult.class);
    var exception = new ArtifactResolutionException(
        List.of(artifactResult),
        "repo system broke!",
        new RuntimeException("something unexpected")
    );

    when(repositorySystem.resolveArtifact(any(), any()))
        .thenThrow(exception);

    // When/then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> aetherResolver.resolveArtifact(inputArtifact))
        .withMessage(
            "Failed to resolve artifact com.example.foo:bar:1.3.5. Check the "
                + "coordinates and connection, and try again. Cause was: %s",
            exception
        )
        .withCause(exception);
  }

  @DisplayName(
      "resolveArtifact(Artifact) throws an exception with suppressions if collection failed"
  )
  @Test
  void resolveArtifactThrowsExceptionWithSuppressionsIfCollectionFailed() throws Exception {
    // Given
    var inputArtifact = mock(Artifact.class, "com.example.foo:bar:1.3.5");
    var exceptions = new Exception[]{
        new RuntimeException("foo"),
        new RuntimeException("bar"),
        new IndexOutOfBoundsException("baz")
    };

    var artifactResult = mock(ArtifactResult.class);
    when(artifactResult.isResolved())
        .thenReturn(false);
    when(artifactResult.getExceptions())
        .thenReturn(List.of(exceptions));

    when(repositorySystem.resolveArtifact(any(), any()))
        .thenReturn(artifactResult);

    // When/then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> aetherResolver.resolveArtifact(inputArtifact))
        .withMessage(
            "Failed to resolve artifact com.example.foo:bar:1.3.5. Check the "
                + "coordinates and connection, and try again."
        )
        .withNoCause()
        .satisfies(ex -> assertThat(ex.getSuppressed())
            .containsExactly(exceptions));
  }

  @DisplayName(
      "resolveDependencies(List<Dependency>, Set<String>) returns an empty list if passed "
  )
  @Test
  void resolveDependenciesReturnsEmptyListIfPassedNoDependencies() throws Exception {
    // When
    var actualOutputArtifacts = aetherResolver
        .resolveDependencies(List.of(), Set.of("foo", "bar"));

    // Then
    assertThat(actualOutputArtifacts)
        .isEmpty();
  }

  @DisplayName(
      "resolveDependencies(List<Dependency>, Set<String>) returns the resolved dependencies"
  )
  @Test
  void resolveDependenciesReturnsTheResolvedDependencies() throws Exception {
    // Given
    var inputDependencies = List.of(
        mock(Dependency.class),
        mock(Dependency.class),
        mock(Dependency.class)
    );
    var expectedOutputArtifacts = List.of(
        mock(Artifact.class),
        mock(Artifact.class),
        mock(Artifact.class)
    );
    var expectedOutputArtifactResults = expectedOutputArtifacts.stream()
        .map(this::someArtifactResult)
        .toList();

    // When
    var dependencyResult = mock(DependencyResult.class);
    when(dependencyResult.getCollectExceptions())
        .thenReturn(List.of());
    when(dependencyResult.getArtifactResults())
        .thenReturn(expectedOutputArtifactResults);

    when(repositorySystem.resolveDependencies(any(), any()))
        .thenReturn(dependencyResult);

    var dependencyRequestCaptor = ArgumentCaptor
        .forClass(DependencyRequest.class);

    var scopes = Set.of("foo", "bar", "baz");

    // When
    var actualOutputArtifacts = aetherResolver
        .resolveDependencies(inputDependencies, scopes);

    // Then
    assertThat(actualOutputArtifacts).isEqualTo(expectedOutputArtifacts);

    verifyRemoteRepositoriesConfiguredCorrectly();
    verify(repositorySystem)
        .resolveDependencies(same(repositorySystemSession), dependencyRequestCaptor.capture());
    assertThat(dependencyRequestCaptor.getValue())
        .satisfies(
            request -> assertThat(request.getCollectRequest().getDependencies())
                .isEqualTo(inputDependencies),
            request -> assertThat(request.getCollectRequest().getRepositories())
                .isEqualTo(expectedRemoteRepositories),
            request -> assertThat(request.getFilter())
                .isInstanceOfSatisfying(
                    InclusiveScopeDependencyFilter.class,
                    f -> assertThat(f.getAllowedScopes())
                        .containsExactlyInAnyOrderElementsOf(scopes)));
  }

  @DisplayName(
      "resolveDependencies(List<Dependency>, Set<String>) throws an exception if Aether raises an "
          + "exception"
  )
  @Test
  void resolveDependenciesRaisesExceptionIfAetherRaisesException() throws Exception {
    // Given
    var inputDependencies = List.of(
        mock(Dependency.class),
        mock(Dependency.class),
        mock(Dependency.class)
    );

    var exception = new DependencyResolutionException(
        mock(),
        "repo system broke!",
        new RuntimeException("something unexpected")
    );

    var scopes = Set.of("foo", "bar", "baz");
    when(repositorySystem.resolveDependencies(any(), any()))
        .thenThrow(exception);

    // When/then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> aetherResolver.resolveDependencies(inputDependencies, scopes))
        .withMessage("Failed to resolve dependencies. Cause was: %s", exception)
        .withCause(exception);
  }

  @DisplayName(
      "resolveDependencies(List<Dependency>, Set<String>) throws an exception if collection fails "
          + "with no GAVs"
  )
  @Test
  void resolveDependenciesRaisesExceptionIfCollectionFailsWithNoGavs() throws Exception {
    // Given
    var inputDependencies = List.of(
        mock(Dependency.class),
        mock(Dependency.class),
        mock(Dependency.class)
    );

    var exceptions = new Exception[]{
        new Exception("foo"),
        new RuntimeException("bar"),
        new ArrayIndexOutOfBoundsException("baz")
    };

    var dependencyResult = mock(DependencyResult.class);
    when(dependencyResult.getCollectExceptions())
        .thenReturn(List.of(exceptions));

    var scopes = Set.of("foo", "bar", "baz");
    when(repositorySystem.resolveDependencies(any(), any()))
        .thenReturn(dependencyResult);

    // When/then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> aetherResolver.resolveDependencies(inputDependencies, scopes))
        .withMessage("Failed to resolve dependencies.")
        .satisfies(ex -> assertThat(ex.getSuppressed())
            .containsExactly(exceptions));
  }

  @DisplayName(
      "resolveDependencies(List<Dependency>, Set<String>) throws an exception if collection fails, "
          + "listing expected GAVs"
  )
  @Test
  void resolveDependenciesRaisesExceptionIfCollectionFailsListingExpectedGavs() throws Exception {
    // Given
    var inputDependencies = List.of(
        mock(Dependency.class),
        mock(Dependency.class),
        mock(Dependency.class)
    );
    var artifactResult1 = mock(ArtifactResult.class);
    var artifactResult2 = mock(ArtifactResult.class);
    var artifactResult3 = mock(ArtifactResult.class);
    var artifactResult4 = mock(ArtifactResult.class);

    var artifactRequest1 = mock(ArtifactRequest.class);
    
    when(artifactResult1.isResolved()).thenReturn(true);
    when(artifactResult2.isResolved()).thenReturn(false);
    when(artifactResult2.getArtifact()).thenReturn(mock(Artifact.class, "do:ray:me"));
    when(artifactResult3.isResolved()).thenReturn(true);
    when(artifactResult4.isResolved()).thenReturn(false);
    when(artifactResult4.getRequest()).thenReturn(artifactRequest1);
    
    when(artifactRequest1.getArtifact()).thenReturn(mock(Artifact.class, "aaa:bbb:ccc"));

    var expectedOutputArtifactResults = List.of(
        artifactResult1,
        artifactResult2,
        artifactResult3,
        artifactResult4
    );

    var dependencyResult = mock(DependencyResult.class);
    when(dependencyResult.getCollectExceptions()).thenReturn(List.of(new RuntimeException("bang")));
    when(dependencyResult.getArtifactResults()).thenReturn(expectedOutputArtifactResults);

    var scopes = Set.of("foo", "bar", "baz");
    when(repositorySystem.resolveDependencies(any(), any()))
        .thenReturn(dependencyResult);

    // When/then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> aetherResolver.resolveDependencies(inputDependencies, scopes))
        .withMessage(
            "Failed to resolve 2 dependencies including do:ray:me, aaa:bbb:ccc. Check the direct and "
                + "transitive coordinates, and network connection, then try again."
        );
  }

  private void verifyRemoteRepositoriesConfiguredCorrectly() {
    verify(mavenProject)
        .getRemoteProjectRepositories();
    verify(repositorySystem)
        .newResolutionRepositories(repositorySystemSession, inputRemoteRepositories);
  }

  private ArtifactResult someArtifactResult(Artifact artifact) {
    var result = mock(ArtifactResult.class);
    when(result.getArtifact()).thenReturn(artifact);
    return result;
  }
}
