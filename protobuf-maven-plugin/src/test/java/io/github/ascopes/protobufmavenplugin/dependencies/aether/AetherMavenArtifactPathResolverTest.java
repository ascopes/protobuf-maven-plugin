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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.quality.Strictness;

/**
 * @author Ashley Scopes
 */
@DisplayName("AetherMavenArtifactPathResolver tests")
class AetherMavenArtifactPathResolverTest {

  MavenSession mavenSession;
  MavenProject currentProject;
  List<RemoteRepository> remoteRepositories;
  RepositorySystemSession repositorySystemSession;
  ArtifactTypeRegistry artifactTypeRegistry;
  RepositorySystem repositorySystem;
  AetherMavenArtifactPathResolver underTest;

  @BeforeEach
  void setUp() {
    mavenSession = mock(lenient());
    currentProject = mock(lenient());
    remoteRepositories = List.of(mock(deep()), mock(deep()), mock(deep()));
    repositorySystemSession = mock(lenient());
    artifactTypeRegistry = mock(lenient());

    when(mavenSession.getCurrentProject()).thenReturn(currentProject);
    when(currentProject.getRemoteProjectRepositories()).thenReturn(remoteRepositories);
    when(mavenSession.getRepositorySession()).thenReturn(repositorySystemSession);
    when(repositorySystemSession.getArtifactTypeRegistry()).thenReturn(artifactTypeRegistry);

    repositorySystem = mock();

    underTest = new AetherMavenArtifactPathResolver(
        mavenSession, repositorySystem);
  }

  @DisplayName("the MavenSession is initialised as expected")
  @Test
  void mavenSessionIsInitialisedAsExpected() {
    // Then
    assertThat(underTest.getMavenSession())
        .isSameAs(mavenSession);
  }

  @DisplayName("the AetherMapper is initialised as expected")
  @Test
  void aetherMapperIsInitialisedAsExpected() {
    // Then
    assertThat(underTest.getAetherMapper().getArtifactTypeRegistry())
        .isSameAs(artifactTypeRegistry);
  }

  @DisplayName("the AetherResolver is initialised as expected")
  @Test
  void aetherResolverIsInitialisedAsExpected() {
    // Then
    assertThat(underTest.getAetherResolver().getRepositorySystem())
        .isSameAs(repositorySystem);
    assertThat(underTest.getAetherResolver().getRepositorySystemSession().getSession())
        .isSameAs(repositorySystemSession);
    assertThat(underTest.getAetherResolver().getRemoteRepositories())
        .isEqualTo(remoteRepositories);
  }

  static MockSettings lenient() {
    return withSettings().strictness(Strictness.LENIENT);
  }

  static MockSettings deep() {
    return withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS);
  }
}
