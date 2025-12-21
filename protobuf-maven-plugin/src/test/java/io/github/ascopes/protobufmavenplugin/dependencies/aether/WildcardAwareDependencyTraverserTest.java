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

import static io.github.ascopes.protobufmavenplugin.dependencies.aether.WildcardAwareDependencyTraverser.WILDCARD_EXCLUSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ashley Scopes
 */
@DisplayName("WildcardAwareDependencyTraverser tests")
@ExtendWith(MockitoExtension.class)
class WildcardAwareDependencyTraverserTest {

  @Mock
  DependencyTraverser delegate;

  @InjectMocks
  WildcardAwareDependencyTraverser underTest;

  @DisplayName("the delegate is assigned correctly")
  @Test
  void delegateIsAssignedCorrectly() {
    // Then
    assertThat(underTest.getDelegate()).isSameAs(delegate);
  }

  @DisplayName(".traverseDependency(Dependency) returns false if the dependency contains a "
      + "wildcard exclusion")
  @Test
  void traverseDependencyReturnsFalseIfDependencyContainsWildcardExclusion() {
    // Given
    var dependency = mock(Dependency.class);
    when(dependency.getExclusions())
        .thenReturn(List.of(mock(), mock(), WILDCARD_EXCLUSION, mock()));

    // When
    var actual = underTest.traverseDependency(dependency);

    // Then
    assertThat(actual).isFalse();
    verifyNoInteractions(delegate);
  }

  @DisplayName(".traverseDependency(Dependency) returns the delegate result if no wildcard "
      + "exclusions are present")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when delegate.tranverseDependency(Dependency) returns {0}")
  void traverseDependencyReturnsDelegateResultIfNoWildcardExclusionsArePresent(boolean expected) {
    // Given
    var dependency = mock(Dependency.class);
    when(dependency.getExclusions())
        .thenReturn(List.of(mock(), mock(), mock()));

    when(delegate.traverseDependency(any()))
        .thenReturn(expected);

    // When
    var actual = underTest.traverseDependency(dependency);

    // Then
    assertThat(actual).isEqualTo(expected);
    verify(delegate).traverseDependency(dependency);
    verifyNoMoreInteractions(delegate);
  }

  @DisplayName(".deriveChildTraverser(DependencyCollectionContext) creates the expected child "
      + "traverser")
  @Test
  void deriveChildTraverserCreatesTheExpectedChildTraverser() {
    // Given
    var dependencyCollectionContext = mock(DependencyCollectionContext.class);
    var expectedDelegateChildTraverser = mock(DependencyTraverser.class);
    when(delegate.deriveChildTraverser(any()))
        .thenReturn(expectedDelegateChildTraverser);

    // When
    var childTraverser = underTest.deriveChildTraverser(dependencyCollectionContext);

    // Then
    assertThat(childTraverser)
        .isInstanceOf(WildcardAwareDependencyTraverser.class)
        .isNotSameAs(underTest);

    var actualDelegateChildTraverser = childTraverser.getDelegate();
    assertThat(actualDelegateChildTraverser)
        .isSameAs(expectedDelegateChildTraverser);

    verify(delegate).deriveChildTraverser(dependencyCollectionContext);
    verifyNoMoreInteractions(delegate);
  }
}
