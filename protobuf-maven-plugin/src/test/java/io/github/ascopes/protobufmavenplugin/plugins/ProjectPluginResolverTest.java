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
package io.github.ascopes.protobufmavenplugin.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("ProjectPluginResolver tests")
@ExtendWith(MockitoExtension.class)
class ProjectPluginResolverTest {

  @Mock
  BinaryPluginResolver binaryPluginResolver;

  @Mock
  JvmPluginResolver jvmPluginResolver;

  @InjectMocks
  ProjectPluginResolver projectPluginResolver;

  @DisplayName(".resolveProjectPlugins(...) delegates to the internal plugin resolvers")
  @Test
  void resolveProjectPluginsDelegatesToTheInternalPluginResolvers() throws Exception {
    // Given
    var resolvedBinaryMavenPlugins = List.<ResolvedProtocPlugin>of(mock(), mock(), mock());
    when(binaryPluginResolver.resolveMavenPlugins(any(), any()))
        .thenReturn(resolvedBinaryMavenPlugins);

    var resolvedBinaryPathPlugins = List.<ResolvedProtocPlugin>of(mock(), mock());
    when(binaryPluginResolver.resolvePathPlugins(any(), any()))
        .thenReturn(resolvedBinaryPathPlugins);

    var resolvedBinaryUrlPlugins = List.<ResolvedProtocPlugin>of(mock(), mock());
    when(binaryPluginResolver.resolveUrlPlugins(any(), any()))
        .thenReturn(resolvedBinaryUrlPlugins);

    var resolvedJvmMavenPlugins = List.<ResolvedProtocPlugin>of(mock(), mock(), mock());
    when(jvmPluginResolver.resolveMavenPlugins(any(), any()))
        .thenReturn(resolvedJvmMavenPlugins);

    var expectedResult = Stream
        .of(
            resolvedBinaryMavenPlugins,
            resolvedBinaryPathPlugins,
            resolvedBinaryUrlPlugins,
            resolvedJvmMavenPlugins
        )
        .flatMap(Collection::stream)
        .toList();

    var binaryMavenPlugins = List.<BinaryMavenProtocPlugin>of(mock(), mock(), mock());
    var binaryPathPlugins = List.<PathProtocPlugin>of(mock(), mock());
    var binaryUrlPlugins = List.<UriProtocPlugin>of(mock(), mock());
    var jvmMavenPlugins = List.<JvmMavenProtocPlugin>of(mock(), mock(), mock());
    var outputDirectory = mock(Path.class);

    var generationRequest = mock(GenerationRequest.class);

    // Use .thenAnswer to work around generic wildcards.
    when(generationRequest.getProtocPlugins())
        .thenAnswer(ctx -> Stream
            .of(binaryMavenPlugins, binaryPathPlugins, binaryUrlPlugins, jvmMavenPlugins)
            .flatMap(List::stream)
            .toList());
    when(generationRequest.getOutputDirectory())
        .thenAnswer(ctx -> outputDirectory);

    // When
    var actualResult = projectPluginResolver.resolveProjectPlugins(generationRequest);

    // Then
    assertThat(actualResult)
        .containsExactlyInAnyOrderElementsOf(expectedResult);

    verify(binaryPluginResolver)
        .resolveMavenPlugins(binaryMavenPlugins, outputDirectory);
    verify(binaryPluginResolver)
        .resolvePathPlugins(binaryPathPlugins, outputDirectory);
    verify(binaryPluginResolver)
        .resolveUrlPlugins(binaryUrlPlugins, outputDirectory);
    verify(jvmPluginResolver)
        .resolveMavenPlugins(jvmMavenPlugins, outputDirectory);

    verifyNoMoreInteractions(binaryPluginResolver, jvmPluginResolver);
  }
}
