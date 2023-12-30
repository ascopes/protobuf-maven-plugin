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
package io.github.ascopes.protobufmavenplugin.dependency;

import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.hostSystem;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.otherOs;
import static io.github.ascopes.protobufmavenplugin.fixtures.HostSystemFixtures.windows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem;
import io.github.ascopes.protobufmavenplugin.system.HostSystem;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

@DisplayName("PathResolver tests")
class PathResolverTest {

  HostSystem hostSystem;
  PathResolver pathResolver;

  @BeforeEach
  void setUp() {
    hostSystem = hostSystem();
    pathResolver = new PathResolver(hostSystem);
  }

  @DisplayName("Windows tests")
  @Nested
  class WindowsTest {

    TestFileSystem fs;

    @BeforeEach
    void setUp() {
      fs = TestFileSystem.windows();
      windows().configure(hostSystem);
    }

    @AfterEach
    void tearDown() {
      fs.close();
    }

    @DisplayName("The first matching executable is returned")
    @Test
    void firstMatchingExecutableIsReturned() throws ResolutionException {
      // Given
      var directory1 = fs.givenDirectoryExists("foo", "emptydir");
      var file1 = fs.givenFileExists("foo", "bar", "baz.txt");
      var file2 = fs.givenFileExists("foo", "bar", "bork.txt");
      var file3 = fs.givenFileExists("foo", "lib", "libtest.dll");
      var file4 = fs.givenFileExists("foo", "lib", "not-the-right-one.exe");
      var file5 = fs.givenFileExists("foo", "bin", "a-thingy.lol");
      var file6 = fs.givenFileExists("foo", "bin", "thingy.exe");
      var file7 = fs.givenFileExists("foo", "bin2", "another-thingy.exe");
      var file8 = fs.givenFileExists("foo", "blep", "thingy.exe");

      givenPath(
          directory1, file1.getParent(), file2.getParent(), file3.getParent(),
          file4.getParent(), file5.getParent(), file6.getParent(), file7.getParent(),
          file8.getParent()
      );
      givenPathExt(".bin", ".msi", ".EXE", ".com");

      // When
      var actualResult = pathResolver.resolve("thingy");

      // Then
      assertThat(actualResult)
          .isPresent()
          .hasValue(file6);
    }

    @DisplayName("Any matching extension is acceptable")
    @Test
    void anyMatchingExtensionIsAcceptable() throws ResolutionException {
      // Given
      var extensions = new String[]{".EXE", ".BIN", ".MSI"};
      givenPathExt(extensions);

      var exe = fs.givenFileExists("foo", "bar", "a.EXE");
      var bin = fs.givenFileExists("foo", "baz", "b.BIN");
      var msi = fs.givenFileExists("foo", "bork", "c.MSI");

      givenPath(exe.getParent(), bin.getParent(), msi.getParent());

      // When
      var resolvedExe = pathResolver.resolve("a");
      var resolvedBin = pathResolver.resolve("b");
      var resolvedMsi = pathResolver.resolve("c");

      // Then
      assertSoftly(softly -> {
        softly.assertThat(resolvedExe)
            .isPresent()
            .hasValue(exe);
        softly.assertThat(resolvedMsi)
            .isPresent()
            .hasValue(msi);
        softly.assertThat(resolvedBin)
            .isPresent()
            .hasValue(bin);
      });
    }

    @DisplayName("Extension case sensitivity is ignored")
    @Test
    void extensionCaseSensitivityIsIgnored() throws ResolutionException {
      // Given
      var extensions = new String[]{".EXE", ".BIN", ".MSI"};
      givenPathExt(extensions);

      var expected = fs.givenFileExists("foo", "bar", "match.exe");
      givenPath(expected.getParent());

      // When
      var actual = pathResolver.resolve("match");

      // Then
      assertThat(actual)
          .isPresent()
          .hasValue(expected);
    }

    @DisplayName("File name case sensitivity is ignored")
    @Test
    void fileNameCaseSensitivityIsIgnored() throws ResolutionException {
      // Given
      var extensions = new String[]{".EXE", ".BIN", ".MSI"};
      givenPathExt(extensions);

      var expected = fs.givenFileExists("foo", "bar", "MATCH.EXE");
      givenPath(expected.getParent());

      // When
      var actual = pathResolver.resolve("match");

      // Then
      assertThat(actual)
          .isPresent()
          .hasValue(expected);
    }

    @DisplayName("An empty optional is returned if no match is found")
    @Test
    void emptyOptionalIsReturnedIfNoMatchIsFound() throws ResolutionException {
      // Given
      var directory1 = fs.givenDirectoryExists("foo", "emptydir");
      var file1 = fs.givenFileExists("foo", "bar", "baz.txt");
      var file2 = fs.givenFileExists("foo", "bar", "bork.txt");
      var file3 = fs.givenFileExists("foo", "lib", "libtest.dll");
      var file4 = fs.givenFileExists("foo", "lib", "not-the-right-one.exe");
      var file5 = fs.givenFileExists("foo", "bin", "a-thingy.lol");
      var file6 = fs.givenFileExists("foo", "bin", "thingy.exe");
      var file7 = fs.givenFileExists("foo", "bin2", "another-thingy.exe");
      var file8 = fs.givenFileExists("foo", "blep", "thingy.exe");

      givenPath(
          directory1, file1.getParent(), file2.getParent(), file3.getParent(),
          file4.getParent(), file5.getParent(), file6.getParent(), file7.getParent(),
          file8.getParent()
      );
      givenPathExt(".bin", ".msi", ".EXE", ".com");

      // When
      var actualResult = pathResolver.resolve("doesnt-exist");

      // Then
      assertThat(actualResult).isEmpty();
    }
  }

  @DisplayName("Non-Windows tests")
  @Nested
  class NonWindowsTest {

    TestFileSystem fs;

    @BeforeEach
    void setUp() {
      fs = TestFileSystem.linux();
      otherOs().configure(hostSystem);
    }

    @AfterEach
    void tearDown() {
      fs.close();
    }

    @DisplayName("The first matching executable is returned")
    @Test
    void firstMatchingExecutableIsReturned() throws ResolutionException {
      // Given
      var directory1 = fs.givenDirectoryExists("foo", "emptydir");
      var file1 = fs.givenFileExists("foo", "bar", "thingy");
      var file2 = fs.givenFileExists("foo", "bar", "something");
      var file3 = fs.givenFileExists("foo", "bar", "something.that.does.not.match");
      var file4 = fs.givenFileExists("foo", "baz", "thingy");
      var file5 = fs.givenFileExists("foo", "baz", "something");
      var file6 = fs.givenFileExists("foo", "bork", "something");
      var file7 = fs.givenFileExists("foo", "qux", "something");

      givenPath(
          directory1, file1.getParent(), file2.getParent(), file3.getParent(),
          file4.getParent(), file5.getParent(), file6.getParent(), file6.getParent(),
          file7.getParent()
      );

      fs.changePermissions(file1, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file3, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file4, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file6, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));

      // When
      var actualResult = pathResolver.resolve("something");

      // Then
      assertThat(actualResult)
          .isPresent()
          .hasValue(file6);
    }

    @DisplayName("An empty optional is returned if no match is found")
    @Test
    void emptyOptionalIsReturnedIfNoMatchIsFound() throws ResolutionException {
      // Given
      var directory1 = fs.givenDirectoryExists("foo", "emptydir");
      var file1 = fs.givenFileExists("foo", "bar", "thingy");
      var file2 = fs.givenFileExists("foo", "bar", "something");
      var file3 = fs.givenFileExists("foo", "bar", "something.that.does.not.match");
      var file4 = fs.givenFileExists("foo", "baz", "thingy");
      var file5 = fs.givenFileExists("foo", "baz", "something");
      var file6 = fs.givenFileExists("foo", "bork", "something");
      var file7 = fs.givenFileExists("foo", "qux", "something");

      givenPath(
          directory1, file1.getParent(), file2.getParent(), file3.getParent(),
          file4.getParent(), file5.getParent(), file6.getParent(), file6.getParent(),
          file7.getParent()
      );

      fs.changePermissions(file1, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file3, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file4, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file6, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));

      // When
      var actualResult = pathResolver.resolve("doesnt-exist");

      // Then
      assertThat(actualResult).isEmpty();
    }

    @DisplayName("Different file name case sensitivity does not match")
    @Test
    void differentFileNameCaseSensitivityDoesNotMatch() throws ResolutionException {
      // Given
      var expected = fs.givenFileExists("foo", "bar", "match");
      givenPath(expected.getParent());

      // When
      var actual = pathResolver.resolve("MATCH");

      // Then
      assertThat(actual).isEmpty();
    }

    @DisplayName("File extensions are considered part of the file name to match")
    @Test
    void fileExtensionsAreConsideredPartOfTheFileNameToMatch() throws ResolutionException {
      // Given
      var file1 = fs.givenFileExists("foo", "bar", "match.exe");
      var file2 = fs.givenFileExists("foo", "baz", "match.bin");

      fs.changePermissions(file1, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));
      fs.changePermissions(file2, perms -> perms.add(PosixFilePermission.OWNER_EXECUTE));

      givenPath(file1.getParent(), file2.getParent());

      // When
      var actual = pathResolver.resolve("match.bin");

      // Then
      assertThat(actual)
          .isPresent()
          .hasValue(file2);
    }

    @DisplayName("Other bits not matching OWNER_EXECUTE do not result in a match")
    @EnumSource(
        value = PosixFilePermission.class,
        names = "OWNER_EXECUTE",
        mode = Mode.EXCLUDE
    )
    @ParameterizedTest(name = "executable bit {0} does not make the file executable")
    void otherBitsNotMatchingOwnerExecuteDoNotResultInMatch(
        PosixFilePermission permission
    ) throws ResolutionException {
      var expected = fs.givenFileExists("foo", "bar", "match");
      fs.changePermissions(expected, perms -> {
        perms.clear();
        perms.add(permission);
      });

      givenPath(expected.getParent());

      // When
      var actual = pathResolver.resolve("match");

      // Then
      assertThat(actual).isEmpty();
    }
  }

  void givenPath(Path... paths) {
    var list = Stream.of(paths)
        .map(Path::toAbsolutePath)
        .map(Path::normalize)
        .distinct()
        .collect(Collectors.toUnmodifiableList());
    when(hostSystem.getSystemPath()).thenReturn(list);

  }

  void givenPathExt(String... exts) {
    var set = new TreeSet<>(String::compareToIgnoreCase);
    set.addAll(Arrays.asList(exts));
    when(hostSystem.getSystemPathExtensions()).thenReturn(set);
  }
}
