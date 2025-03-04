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

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Predicate.not;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bean that exposes information about the underlying platform and the context of the invocation.
 *
 * @author Ashley Scopes
 */
@Named
@Singleton  // Global singleton, shared between plugin instances potentially.
public final class HostSystem {

  private static final Logger log = LoggerFactory.getLogger(HostSystem.class);

  private final String operatingSystem;
  private final String cpuArchitecture;
  private final String pathSeparator;
  private final Path workingDirectory;
  private final Path javaHome;
  private final String javaVendor;
  private final List<Path> path;
  private final SortedSet<String> pathExt;

  @Inject
  public HostSystem() {
    // GH-271: Use System.getenv(String) for retrieving case insensitive environment variables
    this(System.getProperties(), System::getenv);
  }

  // Visible for testing only.
  public HostSystem(Properties properties, Function<String, String> envProvider) {
    operatingSystem = properties.getProperty("os.name", "");
    cpuArchitecture = properties.getProperty("os.arch", "");
    pathSeparator = properties.getProperty("path.separator", "");
    workingDirectory = FileUtils.normalize(Path.of(""));
    javaHome = FileUtils.normalize(Path.of(properties.getProperty("java.home", "")));
    javaVendor = properties.getProperty("java.vendor", "");
    path = parsePath(requireNonNullElse(envProvider.apply("PATH"), ""), pathSeparator);
    pathExt = parsePathExt(requireNonNullElse(envProvider.apply("PATHEXT"), ""), pathSeparator);
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  public String getCpuArchitecture() {
    return cpuArchitecture;
  }

  public boolean isProbablyLinux() {
    return operatingSystem.toLowerCase(Locale.ROOT).startsWith("linux");
  }

  public boolean isProbablyTermux() {
    return javaVendor.equalsIgnoreCase("termux");
  }

  public boolean isProbablyMacOs() {
    return operatingSystem.toLowerCase(Locale.ROOT).startsWith("mac os");
  }

  public boolean isProbablyWindows() {
    return operatingSystem.toLowerCase(Locale.ROOT).startsWith("windows");
  }

  public Path getWorkingDirectory() {
    return workingDirectory;
  }

  public Path getJavaExecutablePath() {
    return javaHome.resolve("bin").resolve(isProbablyWindows() ? "java.exe" : "java");
  }

  public List<Path> getSystemPath() {
    return path;
  }

  public String getPathSeparator() {
    return pathSeparator;
  }

  public SortedSet<String> getSystemPathExtensions() {
    return pathExt;
  }

  private static List<Path> parsePath(String rawPath, String pathSeparator) {
    return tokenizeFilePath(
        rawPath,
        pathSeparator,
        paths -> paths
            .flatMap(tryParseSystemFilePath())
            .distinct()
            .filter(Files::isDirectory)
            .collect(Collectors.toUnmodifiableList())
    );
  }

  private static SortedSet<String> parsePathExt(String rawPathExt, String pathSeparator) {
    return tokenizeFilePath(
        rawPathExt,
        pathSeparator,
        extensions -> extensions
            .map(String::toLowerCase)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(String::compareToIgnoreCase)),
                Collections::unmodifiableSortedSet
            ))
    );
  }

  private static <T> T tokenizeFilePath(
      String rawValue,
      String separator,
      Function<Stream<String>, T> mapper
  ) {
    var separatorRegex = Pattern.quote(separator);

    try (var scanner = new Scanner(rawValue).useDelimiter(separatorRegex)) {
      var stream = scanner.tokens()
          .map(String::trim)
          .filter(not(String::isBlank));

      return mapper.apply(stream);
    }
  }

  private static Function<String, Stream<Path>> tryParseSystemFilePath() {
    return path -> {
      return Stream.of(path)
          .map(String::trim)
          .filter(not(String::isBlank))
          .map(trimmedPath -> {
            try {
              return Path.of(trimmedPath);
            } catch (InvalidPathException ex) {
              // GH-557: Do not crash if the user has garbage contents in their $PATH.
              // Warn and drop instead.
              log.warn(
                  "Ignoring path {} in $PATH environment variable. "
                      + "Please check your system settings!",
                  trimmedPath,
                  ex
              );
              return null;
            }
          })
          .filter(Objects::nonNull)
          .map(FileUtils::normalize);
    };
  }
}
