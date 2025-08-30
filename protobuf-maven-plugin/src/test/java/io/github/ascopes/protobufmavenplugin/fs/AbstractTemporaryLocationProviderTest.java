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
package io.github.ascopes.protobufmavenplugin.fs;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.quality.Strictness;

@DisplayName("AbstractTemporaryLocationProvider tests")
class AbstractTemporaryLocationProviderTest {

  @TempDir
  Path tempDir;
  MojoExecution mojoExecution;
  String executionId;
  String goal;
  SomeTemporaryLocationProvider provider;

  @BeforeEach
  void setUp() {
    goal = "goal-" + someBasicString();
    executionId = "executionId-" + someBasicString();

    mojoExecution = mock(MojoExecution.class, withSettings()
        .strictness(Strictness.LENIENT)
        .defaultAnswer(Answers.RETURNS_SMART_NULLS));

    when(mojoExecution.getExecutionId())
        .thenReturn(executionId);
    when(mojoExecution.getGoal())
        .thenReturn(goal);

    provider = new SomeTemporaryLocationProvider();
  }

  @DisplayName("temporary locations are created in the expected place")
  @Test
  void temporaryLocationsAreCreatedInTheExpectedPlace() throws IOException {
    // Given
    var id = someBasicString();

    // When
    var actualPath = provider.resolveAndCreateDirectory(tempDir, "foo", "bar", "baz", id);

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
    var actualPath = provider.resolveAndCreateDirectory(tempDir, "foo", "bar", "baz", id);

    // Then
    assertThat(actualPath)
        .isEqualTo(existingPath)
        .isDirectory();
  }

  private final class SomeTemporaryLocationProvider extends AbstractTemporaryLocationProvider {

    SomeTemporaryLocationProvider() {
      super(mojoExecution);
    }
  }
}
