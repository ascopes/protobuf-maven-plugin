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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyTraverser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ashley Scopes
 */
@DisplayName("ProtobufMavenPluginRepositorySession tests")
@ExtendWith(MockitoExtension.class)
class ProtobufMavenPluginRepositorySessionTest {

  @Mock
  RepositorySystemSession delegate;

  @InjectMocks
  ProtobufMavenPluginRepositorySession underTest;

  @DisplayName(".getSession() returns the delegate RepositorySystemSession")
  @Test
  void getSessionReturnsDelegateRepositorySystemSession() {
    // Then
    assertThat(underTest.getSession())
        .isSameAs(delegate);
  }

  @DisplayName(".getDependencyTraverser() wraps the RepositorySystemSession DependencyTraverser")
  @Test
  void getDependencyTraverserWrapsRepositorySystemSessionDependencyTraverser() {
    // Given
    var delegateDependencyTraverser = mock(DependencyTraverser.class);
    when(delegate.getDependencyTraverser())
        .thenReturn(delegateDependencyTraverser);

    // When
    var actualDependencyTraverser = underTest.getDependencyTraverser();

    // Then
    assertThat(actualDependencyTraverser.getDelegate())
        .isSameAs(delegateDependencyTraverser);
    verify(delegate).getDependencyTraverser();
    verifyNoMoreInteractions(delegate);
  }

  @DisplayName(".getDependencyTraverser() returns new instances on every call")
  @Test
  void getDependencyTraverserReturnsNewInstancesOnEveryCall() {
    // Then
    assertThat(underTest.getDependencyTraverser())
        .isNotSameAs(underTest.getDependencyTraverser());
  }

  @DisplayName(".getResolutionErrorPolicy() returns the expected value")
  @Test
  void getResolutionErrorPolicyReturnsTheExpectedValue() {
    // Then
    assertThat(underTest.getResolutionErrorPolicy())
        .isInstanceOf(NoCacheResolutionErrorPolicy.class);
  }
}
