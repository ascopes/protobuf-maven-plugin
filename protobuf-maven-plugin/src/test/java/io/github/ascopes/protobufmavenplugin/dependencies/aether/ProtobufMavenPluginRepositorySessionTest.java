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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyTraverser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Ashley Scopes
 */
@DisplayName("ProtobufMavenPluginRepositorySession tests")
class ProtobufMavenPluginRepositorySessionTest {

  RepositorySystemSession delegatedRepositorySession;
  MavenSession mavenSession;
  ProtobufMavenPluginRepositorySession repositorySession;

  @BeforeEach
  void setUp() {
    delegatedRepositorySession = mock();
    mavenSession = mock();
    when(mavenSession.getRepositorySession()).thenReturn(delegatedRepositorySession);
    repositorySession = new ProtobufMavenPluginRepositorySession(mavenSession);
  }

  @DisplayName(".getSession() returns the delegate RepositorySystemSession")
  @Test
  void getSessionReturnsDelegateRepositorySystemSession() {
    // Then
    assertThat(repositorySession.getSession())
        .isSameAs(delegatedRepositorySession);
  }

  @DisplayName(".getDependencyTraverser() wraps the RepositorySystemSession DependencyTraverser")
  @Test
  void getDependencyTraverserWrapsRepositorySystemSessionDependencyTraverser() {
    // Given
    var delegateDependencyTraverser = mock(DependencyTraverser.class);
    when(delegatedRepositorySession.getDependencyTraverser())
        .thenReturn(delegateDependencyTraverser);

    // When
    var actualDependencyTraverser = repositorySession.getDependencyTraverser();

    // Then
    assertThat(actualDependencyTraverser.getDelegate())
        .isSameAs(delegateDependencyTraverser);
    verify(delegatedRepositorySession).getDependencyTraverser();
    verifyNoMoreInteractions(delegatedRepositorySession);
  }

  @DisplayName(".getDependencyTraverser() returns new instances on every call")
  @Test
  void getDependencyTraverserReturnsNewInstancesOnEveryCall() {
    // Then
    assertThat(repositorySession.getDependencyTraverser())
        .isNotSameAs(repositorySession.getDependencyTraverser());
  }

  @DisplayName(".getResolutionErrorPolicy() returns the expected value")
  @Test
  void getResolutionErrorPolicyReturnsTheExpectedValue() {
    // Then
    assertThat(repositorySession.getResolutionErrorPolicy())
        .isInstanceOf(NoCacheResolutionErrorPolicy.class);
  }
}
