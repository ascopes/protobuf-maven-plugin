/*
 * Copyright (C) 2023, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PathProtocResolver tests")
@ExtendWith(MockitoExtension.class)
class PathProtocResolverTest {

  @Mock
  MockedStatic<HostEnvironment> hostEnvironmentMock;

  @TempDir
  Path temporaryDirectory;

  @DisplayName("An empty $PATH results in a resolution exception being raised")
  @Test
  void emptyPathThrowsResolutionException() {
    // Given
    hostEnvironmentMock.when(HostEnvironment::systemPath).thenReturn(List.of());
    var resolver = new PathProtocResolver("foo");

    // Then
    assertThatThrownBy(resolver::resolveProtoc)
        .isInstanceOf(ProtocResolutionException.class)
        .hasNoCause()
        .hasMessage("No protoc binary was found in the $PATH");
  }

  @DisplayName("Irrelevant files in PATH directories are ignored")
  @Test
  void irrelevantFilesInPathDirectoriesAreIgnored() throws IOException {
    // Given
    var resolver = new PathProtocResolver("bork");

    givenFileExists(temporaryDirectory, "foo", "bar");
    givenFileExists(temporaryDirectory, "foo", "baz.exe");

    var path = List.of(temporaryDirectory.resolve("foo"));
    hostEnvironmentMock.when(HostEnvironment::systemPath).thenReturn(path);

    // Then
    assertThatThrownBy(resolver::resolveProtoc)
        .isInstanceOf(ProtocResolutionException.class)
        .hasNoCause()
        .hasMessage("No protoc binary was found in the $PATH");
  }

  ///
  /// Helpers
  ///

  private Path givenFileExists(Path root, String... parts) throws IOException {
    var path = root;

    for (var part : parts) {
      path = path.resolve(part);
    }

    Files.createDirectories(path.getParent());
    return Files.createFile(path);
  }
}
