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

import static java.util.function.Predicate.not;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bean that exposes information about the underlying platform and the context of the invocation.
 *
 * @author Ashley Scopes
 */
@Description("Discovers information about the platform that the plugin is being invoked on")
@MojoExecutionScoped
@Named
public final class HostSystem {

  private static final Logger log = LoggerFactory.getLogger(HostSystem.class);

  private final String operatingSystem;
  private final String cpuArchitecture;
  private final String pathSeparator;
  private final Path javaHome;
  private final String javaVendor;
  private final List<Path> path;
  private final SortedSet<String> pathExt;

  @Inject
  public HostSystem() {
    // GH-271: Use System.getenv(String) for retrieving case insensitive environment variables
    this(
        optionalFunction(System::getProperty),
        optionalFunction(System::getenv)
    );
  }

  // Visible for testing only.
  HostSystem(
      Function<String, Optional<String>> propertyProvider,
      Function<String, Optional<String>> envProvider
  ) {
    operatingSystem = propertyProvider.apply("os.name")
        .orElse("");
    log.debug("Reported OS: '{}'", operatingSystem);

    cpuArchitecture = propertyProvider.apply("os.arch")
        .orElse("");
    log.debug("Reported CPU: '{}'", cpuArchitecture);

    pathSeparator = propertyProvider.apply("path.separator")
        .orElse(":");
    log.debug("Reported path separator: '{}'", pathSeparator);

    javaHome = propertyProvider.apply("java.home")
        .map(Path::of)
        .map(FileUtils::normalize)
        .orElseGet(() -> FileUtils.normalize(Path.of("")));
    log.debug("Reported java.home: '{}'", javaHome);

    javaVendor = propertyProvider.apply("java.vendor")
        .orElse("");
    log.debug("Reported java.vendor: '{}'", javaVendor);

    path = envProvider.apply("PATH")
        .map(value -> parsePath(value, pathSeparator))
        .orElseGet(Collections::emptyList);
    log.debug("Parsed system path: {}", path);

    pathExt = envProvider.apply("PATHEXT")
        .map(value -> parsePathExt(value, pathSeparator))
        .orElseGet(Collections::emptySortedSet);
    log.debug("Parsed path extensions: {}", pathExt);
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

  public Path getJavaExecutablePath() {
    var executableName = isProbablyWindows()
        ? "java.exe"
        : "java";
    return javaHome.resolve("bin").resolve(executableName);
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
          .flatMap(trimmedPath -> {
            try {
              return Stream.of(Path.of(trimmedPath));
            } catch (InvalidPathException ex) {
              // GH-557: Do not crash if the user has garbage contents in their $PATH.
              // Warn and drop instead.
              log.warn(
                  "Ignoring path {} in $PATH environment variable. "
                      + "Please check your system settings!",
                  trimmedPath,
                  ex
              );
              return Stream.empty();
            }
          })
          .map(FileUtils::normalize);
    };
  }

  private static <A, R> Function<A, Optional<R>> optionalFunction(Function<A, @Nullable R> fn) {
    return fn.andThen(Optional::ofNullable);
  }
}
