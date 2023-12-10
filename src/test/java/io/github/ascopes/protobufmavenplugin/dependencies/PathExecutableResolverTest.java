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

package io.github.ascopes.protobufmavenplugin.dependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ascopes.protobufmavenplugin.fixture.FileSystemTestSupport;
import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

@DisplayName("PathExecutableResolver tests")
class PathExecutableResolverTest extends FileSystemTestSupport {

  PathExecutableResolver resolver;
  Executable executable;

  @BeforeEach
  void setUp() {
    resolver = new PathExecutableResolver();
    executable = new Executable("org.example", "protoc");
  }

  @DisplayName("An empty $PATH results in an exception being raised")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when HostEnvironment.isWindows() returns {0}")
  void emptyPathResultsInExceptionBeingRaised(boolean isWindows) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      envMock.when(HostEnvironment::isWindows)
          .thenReturn(isWindows);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of());
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of(".exe"));

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("No matches result in an exception being raised")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when HostEnvironment.isWindows() returns {0}")
  void noMatchesResultsInExceptionBeingRaised(boolean isWindows) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var notProtoc1 = givenFileExists("foo", "bar", "not-protoc");
      var notProtoc2 = givenFileExists("foo", "bar", "also-not-protoc");
      var notProtoc3 = givenFileExists("definitely", "not", "protoc-executable");

      if (!isWindows) {
        givenFileIsExecutable(notProtoc1);
        givenFileIsExecutable(notProtoc2);
        givenFileIsExecutable(notProtoc3);
      }

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(isWindows);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(
              existentDirectory,
              notProtoc1.getParent(),
              notProtoc2.getParent(),
              notProtoc3.getParent()
          ));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of(".exe", ".bang"));

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("Executables on POSIX are ignored if they do not match the expected name")
  @ValueSource(strings = {
      "PROTOC",
      "Protoc",
      "protoc.exe",
      "this-is-not-protoc",
      "proto",
      "protobuf",
      "firefox",
  })
  @ParameterizedTest(name = "for executable named \"{0}\"")
  void executablesOnPosixAreIgnoredIfTheyDoNotMatchTheExpectedName(String name) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var notProtoc = givenFileExists("foo", "bar", name);
      givenFileIsExecutable(notProtoc);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(false);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(existentDirectory, notProtoc.getParent()));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of());

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("Executables on POSIX are ignored when non-executable")
  @Test
  void executablesOnPosixAreIgnoredWhenNotExecutable() {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var protoc = givenFileExists("foo", "bar", "protoc");
      givenFileIsNotExecutable(protoc);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(false);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(existentDirectory, protoc.getParent()));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of());

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("The first matching executable on POSIX is returned")
  @Test
  void firstMatchingExecutableOnPosixIsReturned() throws DependencyResolutionException {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var nonExecutableProtoc = givenFileExists("foo", "bar", "protoc");
      var protoc1 = givenFileExists("foo", "qux", "protoc");
      var protoc2 = givenFileExists("it", "is", "protobuf", "protoc");

      givenFileIsNotExecutable(protoc1);
      givenFileIsExecutable(protoc1);
      givenFileIsExecutable(protoc2);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(false);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(
              existentDirectory,
              nonExecutableProtoc.getParent(),
              protoc1.getParent(),
              protoc2.getParent()
          ));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of());

      // When
      var result = resolver.resolve(executable);

      // Then
      assertThat(result)
          .isEqualTo(protoc1);
    }
  }

  @DisplayName("Executables on Windows are ignored if not matching by case-insensitive name")
  @ValueSource(strings = {
      "this-is-not-protoc",
      "proto",
      "protobuf",
      "firefox",
  })
  @ParameterizedTest(name = "for executable named \"{0}\"")
  void executablesOnWindowsAreIgnoredIfNotMatchingByCaseInsensitiveName(String name) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var notProtoc = givenFileExists("foo", "bar", name);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(true);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(existentDirectory, notProtoc.getParent()));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(caseInsensitiveSetOf());

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("Executables on Windows are ignored when not covered by PATHEXT")
  @Test
  void executablesOnWindowsAreIgnoredWhenNotCoveredByPathext() {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var protocDdd = givenFileExists("foo", "bar", "d", "protoc.ddd");
      var protocEee = givenFileExists("foo", "bar", "e", "protoc.eee");
      var protocFff = givenFileExists("foo", "bar", "f", "protoc.fff");
      var protoc = givenFileExists("foo", "bar", "protoc");

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(true);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(
              existentDirectory,
              protocDdd.getParent(),
              protocEee.getParent(),
              protocFff.getParent(),
              protoc.getParent()
          ));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(caseInsensitiveSetOf(".aaa", ".bbb", ".ccc"));

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("No protoc binary was found in the $PATH");
    }
  }

  @DisplayName("The first matching executable on Windows is returned")
  @ValueSource(strings = {
      "protoc.exe",
      "PROTOC.exe",
      "Protoc.EXE",
      "pROTOC.eXe",
      "PROTOC.EXE"
  })
  @ParameterizedTest(name = "for executable named \"{0}\"")
  void firstMatchingExecutableOnWindowsIsReturned(String name) throws DependencyResolutionException {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var protocDdd = givenFileExists("foo", "bar", "d", "protoc.ddd");
      var protocEee = givenFileExists("foo", "bar", "e", "protoc.eee");
      var protocFff = givenFileExists("foo", "bar", "f", "protoc.fff");
      var protoc = givenFileExists("foo", "bar", name);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(true);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(
              existentDirectory,
              protocDdd.getParent(),
              protocEee.getParent(),
              protocFff.getParent(),
              protoc.getParent()
          ));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(caseInsensitiveSetOf(".aaa", ".bbb", ".ccc", ".exe"));

      // Then
      assertThat(resolver.resolve(executable))
          .isEqualTo(protoc);
    }
  }

  @DisplayName("Missing directories in the $PATH get ignored")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when HostEnvironment.isWindows() returns {0}")
  void missingDirectoriesInThePathGetIgnored(boolean isWindows) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var nonExistentDirectory = givenDirectoryDoesNotExist("foo", "bar", "non-existent");
      var existentDirectory = givenDirectoryExists("foo", "bar", "existent");
      var protoc = givenFileExists("foo", "bar", "protoc");

      if (!isWindows) {
        givenFileIsExecutable(protoc);
      }

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(isWindows);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(nonExistentDirectory, existentDirectory, protoc.getParent()));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of("", ".exe"));

      // Then
      assertThatNoException()
          .isThrownBy(() -> resolver.resolve(executable));
    }
  }

  @DisplayName("File system IO errors get raised as exceptions")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when HostEnvironment.isWindows() returns {0}")
  void fileSystemIoErrorsGetRaisedAsExceptions(boolean isWindows) {
    try (var envMock = Mockito.mockStatic(HostEnvironment.class)) {
      // Given
      var directory = givenDirectoryExists("foo", "bar", "existent");
      var protoc = givenFileExists("foo", "bar", "protoc");
      // Triggers an IO exception due to lack of RWX access.
      givenFileOrDirectoryIsInaccessible(directory);

      envMock.when(HostEnvironment::isWindows)
          .thenReturn(isWindows);
      envMock.when(HostEnvironment::systemPath)
          .thenReturn(List.of(directory, protoc.getParent()));
      envMock.when(HostEnvironment::systemPathExtensions)
          .thenReturn(Set.of(""));

      // Then
      assertThatThrownBy(() -> resolver.resolve(executable))
          .isInstanceOf(DependencyResolutionException.class)
          .hasMessage("File system error while searching for protoc")
          .hasCauseInstanceOf(IOException.class);
    }
  }

  @SuppressWarnings("varargs")
  private static SortedSet<String> caseInsensitiveSetOf(String... items) {
    var ts = new TreeSet<>(String::compareToIgnoreCase);
    ts.addAll(Arrays.asList(items));
    return Collections.unmodifiableSortedSet(ts);
  }
}
