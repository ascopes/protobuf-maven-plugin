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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ashley Scopes
 */
@DisplayName("WildcardAwareDependencyTraverser tests")
class WildcardAwareDependencyTraverserTest {

  WildcardAwareDependencyTraverser underTest;

  @BeforeEach
  void setUp() {
    underTest = new WildcardAwareDependencyTraverser();
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
  }

  @DisplayName(".traverseDependency(Dependency) returns true if no wildcard is present")
  @Test
  void traverseDependencyReturnsTrueIfNoWildcardExclusionsArePresent() {
    // Given
    var dependency = mock(Dependency.class);
    when(dependency.getExclusions())
        .thenReturn(List.of(mock(), mock(), mock()));

    // When
    var actual = underTest.traverseDependency(dependency);

    // Then
    assertThat(actual).isTrue();
  }

  @DisplayName(".deriveChildTraverser(DependencyCollectionContext) returns itself")
  @Test
  void deriveChildTraverserReturnsItself() {
    // Given
    var dependencyCollectionContext = mock(DependencyCollectionContext.class);

    // When
    var childTraverser = underTest.deriveChildTraverser(dependencyCollectionContext);

    // Then
    assertThat(childTraverser).isSameAs(underTest);
  }
}
