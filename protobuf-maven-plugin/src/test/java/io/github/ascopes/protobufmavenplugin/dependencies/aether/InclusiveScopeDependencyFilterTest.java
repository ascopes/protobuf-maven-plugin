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
import static org.mockito.Mockito.when;

import java.util.Set;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InclusiveScopeDependencyFilter test")
class InclusiveScopeDependencyFilterTest {
  InclusiveScopeDependencyFilter underTest;

  @BeforeEach
  void setUp() {
    underTest = new InclusiveScopeDependencyFilter(Set.of("foo", "bar", "baz"));
  }

  @DisplayName("dependencies are filtered if the dependency is null")
  @Test
  void dependenciesAreFilteredIfTheDependencyIsNull() {
    // Given
    var node = mock(DependencyNode.class);
    when(node.getDependency()).thenReturn(null);

    // When
    var result = underTest.accept(node, mock());

    // Then
    assertThat(result).isFalse();
  }

  @DisplayName("dependencies are filtered if the dependency has a filtered scope")
  @Test
  void dependenciesAreFilteredIfTheDependencyHasFilteredScope() {
    // Given
    var dependency = mock(Dependency.class);
    when(dependency.getScope()).thenReturn("bar");

    var node = mock(DependencyNode.class);
    when(node.getDependency()).thenReturn(dependency);

    // When
    var result = underTest.accept(node, mock());

    // Then
    assertThat(result).isTrue();
  }

  @DisplayName("dependencies are not filtered if the dependency has a non-filtered scope")
  @Test
  void dependenciesAreNotFilteredIfTheDependencyHasNonFilteredScope() {
    // Given
    var dependency = mock(Dependency.class);
    when(dependency.getScope()).thenReturn("bork");

    var node = mock(DependencyNode.class);
    when(node.getDependency()).thenReturn(dependency);

    // When
    var result = underTest.accept(node, mock());

    // Then
    assertThat(result).isFalse();
  }
}
