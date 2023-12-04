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

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collector;

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
   * @throws IllegalStateException if the JVM is not configured to give this information.
   */
  public static boolean isWindows() {
    return operatingSystem().startsWith("windows");
  }

  /**
   * Determine if the host OS is running Mac OS X.
   *
   * @return {@code true} if running Mac OS X, or {@code false} otherwise.
   * @throws IllegalStateException if the JVM is not configured to give this information.
   */
  public static boolean isMacOs() {
    return operatingSystem().startsWith("mac os");
  }

  /**
   * Determine if the host OS is running Linux.
   *
   * @return {@code true} if running Linux, or {@code false} otherwise.
   * @throws IllegalStateException if the JVM is not configured to give this information.
   */
  public static boolean isLinux() {
    return operatingSystem().startsWith("linux");
  }

  /**
   * Determine the reported CPU architecture.
   *
   * @return the reported CPU architecture.
   * @throws IllegalStateException if the JVM is not configured to give this information.
   */
  public static String cpuArchitecture() {
    return mandatoryProperty("os.arch");
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
    var rawPath = environmentVariable("PATH").orElse("");
    try (var scanner = new Scanner(rawPath).useDelimiter(File.pathSeparator)) {
      return scanner
          .tokens()
          .filter(not(String::isBlank))
          .distinct()
          .map(Path::of)
          .collect(toUnmodifiableList());
    }
  }

  /**
   * Get the Windows {@code PATHEXT} environment variable, split by the system path separator and
   * placed in a case-insensitive set.
   *
   * @return the path extensions for the system, or an empty set if unspecified or not applicable.
   */
  public static Set<String> systemPathExtensions() {
    var rawPathExtensions = environmentVariable("PATHEXT").orElse("");
    try (var scanner = new Scanner(rawPathExtensions).useDelimiter(File.pathSeparator)) {
      return scanner
          .tokens()
          .filter(not(String::isBlank))
          .collect(collectingAndThen(
              toCollection(() -> new TreeSet<>(String::compareToIgnoreCase)),
              Collections::unmodifiableSortedSet
          ));
    }
  }

  /**
   * Get the current working directory.
   *
   * @return the current working directory.
   */
  public static Path workingDirectory() {
    // Path.of("") returns the current directory by default.
    return Path.of("").toAbsolutePath();
  }

  // Visible for testing purposes only.
  static Optional<String> environmentVariable(String name) {
    return ofNullable(System.getenv(name));
  }

  private static String operatingSystem() {
    return mandatoryProperty("os.name").toLowerCase(Locale.ROOT);
  }

  private static String mandatoryProperty(String property) {
    return ofNullable(System.getProperty(property))
        .orElseThrow(() -> new IllegalStateException("No '" + property + "' system property is set"));
  }

  private HostEnvironment() {
    // Static-only class.
  }
}
