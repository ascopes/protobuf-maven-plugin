/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import java.nio.file.Path;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

abstract class AbstractGenerateMojoTestTemplate<A extends AbstractGenerateMojo> {

  abstract A newInstance();

  ///
  /// Tests for functionality provided by AbstractGenerateMojo itself.
  ///

  ///
  /// Tests for derived functionality only.
  ///

  abstract SourceRootRegistrar expectedSourceRootRegistrar();

  abstract Path expectedDefaultSourceDirectory(MavenProject session);

  abstract Path expectedDefaultOutputDirectory(MavenProject session);

  @DisplayName("the sourceRootRegistrar is the expected value")
  @Test
  void sourceRootRegistrarIsTheExpectedValue() {
    // Then
    assertThat(newInstance().sourceRootRegistrar())
        .isEqualTo(expectedSourceRootRegistrar());
  }

  @DisplayName("the default source directory is the expected path")
  @Test
  void defaultSourceDirectoryIsTheExpectedPath(@TempDir Path tempDir) {
    // Given
    var mockCurrentProject = mock(MavenProject.class);
    when(mockCurrentProject.getBasedir())
        .thenReturn(tempDir.toFile());

    var pluginMojo = newInstance();
    pluginMojo.mavenProject = mockCurrentProject;

    // Then
    assertThat(pluginMojo.defaultSourceDirectory())
        .isEqualTo(expectedDefaultSourceDirectory(mockCurrentProject));
  }

  @DisplayName("the default output directory is the expected path")
  @Test
  void defaultOutputDirectoryIsTheExpectedPath(@TempDir Path tempDir) {
    // Given
    var mockBuild = mock(Build.class);
    when(mockBuild.getDirectory())
        .thenReturn(tempDir.toString());

    var mockCurrentProject = mock(MavenProject.class);
    when(mockCurrentProject.getBuild())
        .thenReturn(mockBuild);

    var pluginMojo = newInstance();
    pluginMojo.mavenProject = mockCurrentProject;

    // Then
    assertThat(pluginMojo.defaultOutputDirectory())
        .isEqualTo(expectedDefaultOutputDirectory(mockCurrentProject));
  }
}
