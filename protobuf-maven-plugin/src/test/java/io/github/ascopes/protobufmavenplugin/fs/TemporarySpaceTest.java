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
package io.github.ascopes.protobufmavenplugin.fs;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.quality.Strictness;

@DisplayName("TemporarySpace tests")
class TemporarySpaceTest {

  @TempDir Path tempDir;
  String goal;
  String executionId;
  TemporarySpace temporarySpace;

  @BeforeEach
  void setUp() {
    var mavenProject = mock(MavenProject.class, withSettings()
        .strictness(Strictness.LENIENT)
        .defaultAnswer(Answers.RETURNS_SMART_NULLS));

    var mavenBuild = mock(Build.class, withSettings()
        .strictness(Strictness.LENIENT)
        .defaultAnswer(Answers.RETURNS_SMART_NULLS));

    when(mavenProject.getBuild())
        .thenReturn(mavenBuild);

    when(mavenBuild.getDirectory())
        .thenReturn(tempDir.toAbsolutePath().toString());

    goal = "goal-" + someBasicString();
    executionId = "executionId-" + someBasicString();

    var execution = mock(MojoExecution.class, withSettings()
        .strictness(Strictness.LENIENT)
        .defaultAnswer(Answers.RETURNS_SMART_NULLS));

    when(execution.getExecutionId())
        .thenReturn(executionId);
    when(execution.getGoal())
        .thenReturn(goal);

    temporarySpace = new TemporarySpace(mavenProject, execution);
  }

  @DisplayName("temporary spaces are created in the expected place")
  @Test
  void temporarySpacesAreCreatedInTheExpectedPlace() {
    // Given
    var id = someBasicString();

    // When
    var actualPath = temporarySpace.createTemporarySpace("foo", "bar", "baz", id);

    // Then
    assertThat(actualPath)
        .isEqualTo(tempDir
            .resolve("protobuf-maven-plugin")
            .resolve(goal)
            .resolve(executionId)
            .resolve("foo")
            .resolve("bar")
            .resolve("baz")
            .resolve(id))
        .isDirectory();
  }

  @DisplayName("nothing happens if the temporary directory already exists")
  @Test
  void nothingHappensIfTheTemporaryDirectoryAlreadyExists() throws IOException {
    // Given
    var id = someBasicString();
    var existingPath = tempDir
        .resolve("protobuf-maven-plugin")
        .resolve(goal)
        .resolve(executionId)
        .resolve("foo")
        .resolve("bar")
        .resolve("baz")
        .resolve(id);
    Files.createDirectories(existingPath);

    // When
    var actualPath = temporarySpace.createTemporarySpace("foo", "bar", "baz", id);

    // Then
    assertThat(actualPath)
        .isEqualTo(existingPath)
        .isDirectory();
  }

  @DisplayName("directory creation failures are propagated")
  @Test
  void directoryCreationFailuresArePropagated() {
    try (var filesMock = mockStatic(Files.class)) {
      // Given
      var expectedCause = new IOException("boom");
      filesMock.when(() -> Files.createDirectories(any()))
          .thenThrow(expectedCause);

      var id = someBasicString();

      // Then
      assertThatExceptionOfType(UncheckedIOException.class)
          .isThrownBy(() -> temporarySpace.createTemporarySpace("foo", id))
          .withMessage("Failed to create temporary location!")
          .withCause(expectedCause);
    }
  }
}
