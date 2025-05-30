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
package io.github.ascopes.protobufmavenplugin.utils;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Ashley Scopes
 */
@DisplayName("HostSystem tests")
@SuppressWarnings("AssertBetweenInconvertibleTypes")
class HostSystemTest {

  @DisplayName(".getOperatingSystem() returns the operating system")
  @Test
  void getOperatingSystemReturnsTheOperatingSystem() {
    // Given
    var properties = new Properties();
    var env = Map.<String, String>of();
    var osName = someBasicString();
    properties.put("os.name", osName);

    // When
    var hostSystemBean = newInstance(properties, env);

    // Then
    assertThat(hostSystemBean.getOperatingSystem()).isEqualTo(osName);
  }

  @DisplayName(".getCpuArchitecture() returns the CPU architecture")
  @Test
  void getCpuArchitectureReturnsTheCpuArchitecture() {
    // Given
    var properties = new Properties();
    var env = Map.<String, String>of();
    var cpuArch = someBasicString();
    properties.put("os.arch", cpuArch);

    // When
    var hostSystemBean = newInstance(properties, env);

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
    var env = Map.<String, String>of();
    properties.put("os.name", osName);
    var hostSystemBean = newInstance(properties, env);

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
    var env = Map.<String, String>of();
    properties.put("os.name", osName);
    var hostSystemBean = newInstance(properties, env);

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
    var env = Map.<String, String>of();
    properties.put("os.name", osName);
    var hostSystemBean = newInstance(properties, env);

    // When
    var actualResult = hostSystemBean.isProbablyWindows();

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @DisplayName(".isProbablyTermux() returns the expected results")
  @CsvSource({
      "oracle, false",
      "termux,  true",
  })
  @ParameterizedTest(name = "when java.vendor is {0}, expect the result to be {1}")
  void isProbablyTermuxReturnsTheExpectedResults(
      String javaVendor,
      boolean expectedResult
  ) {
    // Given
    var properties = new Properties();
    var env = Map.<String, String>of();
    properties.put("java.vendor", javaVendor);
    var hostSystemBean = newInstance(properties, env);

    // When
    var actualResult = hostSystemBean.isProbablyTermux();

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @DisplayName(".getJavaExecutablePath() returns the Java executable")
  @CsvSource({
      " Windows, java.exe",
      "   Linux,     java",
      "Mac OS X,     java",
  })
  @ParameterizedTest(name = "for {0}, expect {1}")
  void getJavaExecutablePathReturnsTheJavaExecutable(
      String osName,
      String expectedExecutableName
  ) {
    // Given
    var properties = new Properties();
    var env = Map.<String, String>of();
    properties.put("os.name", osName);
    properties.put("java.home", Path.of("foo", "bar", "baz", "jdk").toString());
    var hostSystemBean = newInstance(properties, env);

    // When
    var actualExecutablePath = hostSystemBean.getJavaExecutablePath();

    // Then
    var expectedExecutablePath = Path.of("foo", "bar", "baz", "jdk", "bin", expectedExecutableName)
        .toAbsolutePath()
        .normalize();

    assertThat(actualExecutablePath)
        .isNormalized()
        .isAbsolute()
        .isEqualTo(expectedExecutablePath);
  }

  @DisplayName(".getSystemPath() returns the existing system paths")
  @Test
  void getSystemPathReturnsTheExistingSystemPaths(@TempDir Path tempDir) throws IOException {
    // Given
    // Use OS specific to avoid weirdness with OS path escaping (looking at you, Windows).
    var pathSeparator = File.pathSeparator;

    var existingDir1 = Files.createDirectories(tempDir.resolve("foo").resolve("bar"));
    var existingDir2 = Files.createDirectories(tempDir.resolve("do").resolve("ray"));
    var existingDir3 = Files.createDirectories(tempDir.resolve("xxx"));
    var nonExistingDir1 = tempDir.resolve("foo").resolve("bork");
    var nonExistingDir2 = tempDir.resolve("lorem").resolve("ipsum");

    var path = existingDir1.normalize().toAbsolutePath() + pathSeparator
        + existingDir2.normalize().toAbsolutePath() + pathSeparator
        + existingDir3.normalize().toAbsolutePath() + pathSeparator
        + nonExistingDir1.normalize().toAbsolutePath() + pathSeparator
        + nonExistingDir2.normalize().toAbsolutePath() + pathSeparator
        // Empty path that gets ignored at the end to test sanitising the inputs.
        + pathSeparator;

    var env = Map.of("PATH", path);
    var properties = new Properties();
    properties.put("path.separator", pathSeparator);

    var hostSystemBean = newInstance(properties, env);

    // When
    var actualPath = hostSystemBean.getSystemPath();

    // Then
    // Expect only the existing directories, and only in the exact order they were specified.
    assertThat(actualPath).containsExactly(existingDir1, existingDir2, existingDir3);
  }

  // GH-557, if the user has junk in their $PATH, we do not want to crash the plugin.
  @DisplayName(".getSystemPath() ignores invalid file paths")
  @Test
  void getSystemPathIgnoresInvalidFilePaths(@TempDir Path tempDir) throws IOException {

    // Given
    // Use OS specific to avoid weirdness with OS path escaping (looking at you, Windows).
    var pathSeparator = File.pathSeparator;
    var fooDir = Files.createDirectories(tempDir.resolve("foo"));
    var barDir = Files.createDirectories(tempDir.resolve("bar"));
    var borkDir = Files.createDirectories(tempDir.resolve("bork"));

    var path = fooDir + pathSeparator
        + barDir + pathSeparator
        // Hopefully invalid on all platforms
        + "bazInvalid&&%$\0\r\n!?#broken" + pathSeparator
        + borkDir;

    var env = Map.of("PATH", path);
    var properties = new Properties();
    properties.put("path.separator", pathSeparator);

    var hostSystemBean = newInstance(properties, env);

    // When
    var actualPath = hostSystemBean.getSystemPath();

    // Then
    assertThat(actualPath).containsExactly(fooDir, barDir, borkDir);
  }

  @DisplayName(".getPathSeparator() returns the expected platform-specific separator")
  @Test
  void getPathSeparatorReturnsTheExpectedPlatformSpecificSeparator() {
    // Given
    var env = Map.<String, String>of();
    var properties = new Properties();
    properties.setProperty("path.separator", "$!");
    var hostSystemBean = newInstance(properties, env);

    // Then
    assertThat(hostSystemBean.getPathSeparator())
        .isEqualTo("$!");
  }

  @DisplayName(".getSystemPathExtensions() returns empty if the environment variable is unset")
  @Test
  void getSystemPathExtensionsReturnsEmptyIfEnvironmentVariableIsUnset() {
    // Given
    var env = Map.of("ANYTHING_EXCEPT", "PATHEXT");
    var properties = new Properties();
    var hostSystemBean = newInstance(properties, env);

    // When
    var actualPathExt = hostSystemBean.getSystemPathExtensions();

    // Then
    assertThat(actualPathExt).isEmpty();
  }

  @DisplayName(".getSystemPathExtensions() returns the extensions (case insensitive) when set")
  @Test
  void getSystemPathExtensionsReturnsExtensionsCaseInsensitiveWhenSet() {
    // Given
    var pathExt = ".foo;"
        + ".bar;"
        + ".BAZ;"
        + ".baz;"
        + "  .bork;";

    var env = Map.of("PATHEXT", pathExt);
    var properties = new Properties();
    properties.setProperty("path.separator", ";");
    var hostSystemBean = newInstance(properties, env);

    // When
    var actualPathExt = hostSystemBean.getSystemPathExtensions();

    // Then
    assertThat(actualPathExt)
        .hasSize(4)
        .contains(".foo", ".bar", ".baz", ".bork", ".BORK");
  }

  static HostSystem newInstance(Properties properties, Map<String, String> env) {
    return new HostSystem(
        optionalFunction(properties::getProperty),
        optionalFunction(env::get)
    );
  }

  static <A, R> Function<A, Optional<R>> optionalFunction(Function<A, R> fn) {
    return fn.andThen(Optional::ofNullable);
  }
}
