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


import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PathProtocResolver tests")
@ExtendWith(MockitoExtension.class)
class PathProtocResolverTest {

  @TempDir
  Path temporaryDirectory;

  @DisplayName("An empty $PATH results in an exception being raised")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when HostEnvironment.isWindows() returns {0}")
  void emptyPathResultsInExceptionBeingRaised(boolean isWindows) {
    try (var envMock = mockedHostEnvironment()) {
      // Given
      envMock.when(HostEnvironment::isWindows).thenReturn(isWindows);
      envMock.when(HostEnvironment::systemPath).thenReturn(List.of());

      // When

    }
  }

  ///
  /// Helpers
  ///

  MockedStatic<HostEnvironment> mockedHostEnvironment() {
    return Mockito.mockStatic(HostEnvironment.class);
  }

  Path makeDirectory(String... bits) throws IOException {
    try {
      return Files.createDirectories(foldPath(bits));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  Path makeFile(String... bits) {
    try {
      var path = foldPath(bits);
      Files.createDirectories(path.getParent());
      return Files.createFile(path);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  Path foldPath(String... bits) {
    var path = temporaryDirectory;
    for (var bit : bits) {
      path = path.resolve(bit);
    }
    return path;
  }

}
