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

package io.github.ascopes.protobufmavenplugin.utils;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("HostSystem tests")
class HostSystemTest {

  @DisplayName(".getOperatingSystem() returns the operating system")
  @Test
  void getOperatingSystemReturnsTheOperatingSystem() {
    // Given
    var properties = new Properties();
    var env = new HashMap<String, String>();
    var osName = someText();
    properties.put("os.name", osName);

    // When
    var hostSystemBean = new HostSystem(properties, env);

    // Then
    assertThat(hostSystemBean.getOperatingSystem()).isEqualTo(osName);
  }

  @DisplayName(".getCpuArchitecture() returns the CPU architecture")
  @Test
  void getCpuArchitectureReturnsTheCpuArchitecture() {
    // Given
    var properties = new Properties();
    var env = new HashMap<String, String>();
    var cpuArch = someText();
    properties.put("os.arch", cpuArch);

    // When
    var hostSystemBean = new HostSystem(properties, env);

    // Then
    assertThat(hostSystemBean.getCpuArchitecture()).isEqualTo(cpuArch);
  }

  @DisplayName(".isProbablyLinux() returns true if the OS is probably Linux")
  @CsvSource({
      "              LINUX,  true",
      "              Linux,  true",
      "              linux,  true",
      "            FreeBSD, false",
      "            OpenBSD, false",
      "               OS/2, false",
      "            Solaris, false",
      "              SunOS, false",
      "               Irix, false",
      "             OS/400, false",
      "              HP-UX, false",
      "                AIX, false",
      "      Mac OS X 10.0, false",
      "     Mac OS X 10.15, false",
      "        Mac OS X 11, false",
      "        Mac OS X 12, false",
      "        Mac OS X 13, false",
      "          Windows 7, false",
      "          Windows 8, false",
      "        Windows 8.1, false",
      "         Windows 10, false",
      "         Windows 11, false",
      "Windows Server 2019, false",
  })
  @ParameterizedTest(name = "returns {1} on {0}")
  void isProbablyLinuxReturnsTrueIfTheOsIsProbablyLinux(String osName, boolean expectedResult) {
    // Given
    var properties = new Properties();
    var env = new HashMap<String, String>();
    properties.put("os.name", osName);
    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualResult = hostSystemBean.isProbablyLinux();

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @DisplayName(".isProbablyMacOs() returns true if the OS is probably Mac OS")
  @CsvSource({
      "              LINUX, false",
      "              Linux, false",
      "              linux, false",
      "            FreeBSD, false",
      "            OpenBSD, false",
      "               OS/2, false",
      "            Solaris, false",
      "              SunOS, false",
      "               Irix, false",
      "             OS/400, false",
      "              HP-UX, false",
      "                AIX, false",
      "      Mac OS X 10.0,  true",
      "     Mac OS X 10.15,  true",
      "        Mac OS X 11,  true",
      "        Mac OS X 12,  true",
      "        Mac OS X 13,  true",
      "          Windows 7, false",
      "          Windows 8, false",
      "        Windows 8.1, false",
      "         Windows 10, false",
      "         Windows 11, false",
      "Windows Server 2019, false",
  })
  @ParameterizedTest(name = "returns {1} on {0}")
  void isProbablyMacOsReturnsTrueIfTheOsIsProbablyMacOs(String osName, boolean expectedResult) {
    // Given
    var properties = new Properties();
    var env = new HashMap<String, String>();
    properties.put("os.name", osName);
    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualResult = hostSystemBean.isProbablyMacOs();

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @DisplayName(".isProbablyWindows() returns true if the OS is probably Windows")
  @CsvSource({
      "              LINUX, false",
      "              Linux, false",
      "              linux, false",
      "            FreeBSD, false",
      "            OpenBSD, false",
      "               OS/2, false",
      "            Solaris, false",
      "              SunOS, false",
      "               Irix, false",
      "             OS/400, false",
      "              HP-UX, false",
      "                AIX, false",
      "      Mac OS X 10.0, false",
      "     Mac OS X 10.15, false",
      "        Mac OS X 11, false",
      "        Mac OS X 12, false",
      "        Mac OS X 13, false",
      "          Windows 7,  true",
      "          Windows 8,  true",
      "        Windows 8.1,  true",
      "         Windows 10,  true",
      "         Windows 11,  true",
      "Windows Server 2019,  true",
  })
  @ParameterizedTest(name = "returns {1} on {0}")
  void isProbablyWindowsReturnsTrueIfTheOsIsProbablyWindows(String osName, boolean expectedResult) {
    // Given
    var properties = new Properties();
    var env = new HashMap<String, String>();
    properties.put("os.name", osName);
    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualResult = hostSystemBean.isProbablyWindows();

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  // Avoid non-UNIX path declarations as it messes up our test data.
  @DisabledOnOs({OS.WINDOWS, OS.OTHER})
  @DisplayName(".isProbablyAndroidTermux() returns true if running in Termux")
  @CsvSource({
      " true, /data/data/com.termux/home,  true",
      "false, /data/data/com.termux/home, false",
      " true,          /foo/bar/baz/bork, false",
  })
  @ParameterizedTest(name = "expect {2} when isProbablyLinux is {0} and the directory is {1}")
  void isProbablyAndroidTermuxReturnsTrueIfRunningInTermux(
      boolean isProbablyLinux,
      String workingDirectory,
      boolean expectedResult
  ) {
    // Given
    var hostSystemBean = mock(HostSystem.class);
    when(hostSystemBean.isProbablyLinux()).thenReturn(isProbablyLinux);
    when(hostSystemBean.getWorkingDirectory()).thenReturn(Path.of(workingDirectory));
    when(hostSystemBean.isProbablyAndroidTermux()).thenCallRealMethod();

    // Then
    assertThat(hostSystemBean.isProbablyAndroidTermux())
        .isEqualTo(expectedResult);
  }

  @DisplayName(".getWorkingDirectory() returns the working directory")
  @Test
  void getWorkingDirectoryReturnsTheWorkingDirectory() {
    // Given
    var hostSystemBean = new HostSystem();

    // When
    var actualWorkingDirectory = hostSystemBean.getWorkingDirectory();

    // Then
    assertThat(actualWorkingDirectory)
        .isNormalized()
        .isAbsolute()
        .isEqualTo(Path.of("").toAbsolutePath().normalize());
  }

  @DisplayName(".getSystemPath() returns the existing system paths")
  @Test
  void getSystemPathReturnsTheExistingSystemPaths(@TempDir Path tempDir) throws IOException {
    // Given
    var existingDir1 = Files.createDirectories(tempDir.resolve("foo").resolve("bar"));
    var existingDir2 = Files.createDirectories(tempDir.resolve("do").resolve("ray"));
    var existingDir3 = Files.createDirectories(tempDir.resolve("xxx"));
    var nonExistingDir1 = tempDir.resolve("foo").resolve("bork");
    var nonExistingDir2 = tempDir.resolve("lorem").resolve("ipsum");

    var path = existingDir1.normalize().toAbsolutePath() + File.pathSeparator
        + existingDir2.normalize().toAbsolutePath() + File.pathSeparator
        + existingDir3.normalize().toAbsolutePath() + File.pathSeparator
        + nonExistingDir1.normalize().toAbsolutePath() + File.pathSeparator
        + nonExistingDir2.normalize().toAbsolutePath() + File.pathSeparator
        // Empty path that gets ignored at the end to test sanitising the inputs.
        + File.pathSeparator;

    var env = Map.of("PATH", path);
    var properties = new Properties();

    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualPath = hostSystemBean.getSystemPath();

    // Then

    // Expect only the existing directories, and only in the exact order they were specified.
    assertThat(actualPath).containsExactly(existingDir1, existingDir2, existingDir3);
  }

  @DisplayName(".getSystemPathExtensions() returns empty if the environment variable is unset")
  @Test
  void getSystemPathExtensionsReturnsEmptyIfEnvironmentVariableIsUnset() {
    // Given
    var env = Map.of("ANYTHING_EXCEPT", "PATHEXT");
    var properties = new Properties();
    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualPathExt = hostSystemBean.getSystemPathExtensions();

    // Then
    assertThat(actualPathExt).isEmpty();
  }

  @DisplayName(".getSystemPathExtensions() returns the extensions (case insensitive) when set")
  @Test
  void getSystemPathExtensionsReturnsExtensionsCaseInsensitiveWhenSet() {
    // Given

    var pathExt = ".foo" + File.pathSeparator
        + ".bar" + File.pathSeparator
        + ".BAZ" + File.pathSeparator
        + ".baz" + File.pathSeparator
        + "  .bork  " + File.pathSeparator;

    var env = Map.of("PATHEXT", pathExt);
    var properties = new Properties();
    var hostSystemBean = new HostSystem(properties, env);

    // When
    var actualPathExt = hostSystemBean.getSystemPathExtensions();

    // Then
    assertThat(actualPathExt)
        .hasSize(4)
        .contains(".foo", ".bar", ".baz", ".bork", ".BORK");
  }
}
