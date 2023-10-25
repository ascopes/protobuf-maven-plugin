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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Host environment inspection facilities.
 *
 * @author Ashley Scopes
 */
public final class HostEnvironment {

  /**
   * Determine if the host OS is running Windows.
   *
   * @return {@code true} if running Windows, or {@code false} otherwise.
   */
  public static boolean isWindows() {
    return operatingSystem().toLowerCase(Locale.ROOT).startsWith("windows");
  }

  /**
   * Determine if the host OS is running Mac OS X.
   *
   * @return {@code true} if running Mac OS X, or {@code false} otherwise.
   */
  public static boolean isMacOs() {
    return operatingSystem().toLowerCase(Locale.ROOT).startsWith("mac os");
  }

  /**
   * Determine if the host OS is running Linux.
   *
   * @return {@code true} if running Linux, or {@code false} otherwise.
   */
  public static boolean isLinux() {
    return operatingSystem().toLowerCase(Locale.ROOT).startsWith("linux");
  }

  /**
   * Get the paths in the system {@code PATH} environment variable, if set.
   *
   * <p>Results will be split by the OS-specific path separator, and parsed as file system paths
   * that may or may not correspond to existing directories.
   *
   * @return the list of paths in the {@code PATH} environment variable.
   */
  public static List<Path> systemPath() {
    var rawPath = Optional.ofNullable(System.getenv("PATH")).orElse("");

    try (var scanner = new Scanner(rawPath).useDelimiter(File.pathSeparator)) {
      return scanner
          .tokens()
          .map(Path::of)
          .collect(Collectors.toUnmodifiableList());
    }
  }

  private static String operatingSystem() {
    return Optional.ofNullable(System.getProperty("os.name"))
        .orElseThrow(() -> new IllegalStateException("No 'os.name' system property is set"));
  }

  private HostEnvironment() {
    // Static-only class.
  }
}
