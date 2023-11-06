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

package io.github.ascopes.protobufmavenplugin.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ascopes.protobufmavenplugin.fixture.MockedSystemProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HostEnvironment tests")
@MockedSystemProperties
class HostEnvironmentTest {

  @DisplayName(".isWindows() returns true when on Windows")
  @ValueSource(strings = {
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isWindowsReturnsTrueWhenOnWindows(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isWindows()).isTrue();
  }

  @DisplayName(".isWindows() returns false when not on Windows")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Linux",
      "LINUX",
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isWindowsReturnsFalseWhenNotOnWindows(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isWindows()).isFalse();
  }

  @DisplayName(".isWindows() raises an IllegalStateException if no os.name property is set")
  @Test
  void isWindowsRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isWindows)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }

  @DisplayName(".isMacOs() returns true when on Mac OS")
  @ValueSource(strings = {
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isMacOsReturnsTrueWhenOnMacOs(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isMacOs()).isTrue();
  }

  @DisplayName(".isMacOs() returns false when not on Mac OS")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Linux",
      "LINUX",
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isMacOsReturnsFalseWhenNotOnMacOs(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isMacOs()).isFalse();
  }

  @DisplayName(".isMacOs() raises an IllegalStateException if no os.name property is set")
  @Test
  void isMacOsRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isMacOs)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }

  @DisplayName(".isLinux() returns true when on Linux")
  @ValueSource(strings = {
      "Linux",
      "LINUX"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isLinuxReturnsTrueWhenOnLinux(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isLinux()).isTrue();
  }

  @DisplayName(".isLinux() returns false when not on Linux")
  @EmptySource
  @ValueSource(strings = {
      "FreeBSD",
      "OpenBSD",
      "OS/2",
      "Solaris",
      "SunOS",
      "Irix",
      "OS/400",
      "HP-UX",
      "AIX",
      "Mac OS X 10.0",
      "Mac OS X 10.15",
      "Mac OS X 11",
      "Mac OS X 12",
      "Mac OS X 13",
      "Windows 7",
      "Windows 8",
      "Windows 8.1",
      "Windows 10",
      "Windows 11",
      "Windows Server 2019"
  })
  @ParameterizedTest(name = "for os.name = {0}")
  void isLinuxReturnsFalseWhenNotOnLinux(String osName) {
    // Given
    System.setProperty("os.name", osName);

    // Then
    assertThat(HostEnvironment.isLinux()).isFalse();
  }

  @DisplayName(".isLinux() raises an IllegalStateException if no os.name property is set")
  @Test
  void isLinuxRaisesIllegalStateExceptionIfNoOsNamePropertyIsSet() {
    // Given
    System.clearProperty("os.name");

    // Then
    assertThatThrownBy(HostEnvironment::isLinux)
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessage("No 'os.name' system property is set");
  }
}
