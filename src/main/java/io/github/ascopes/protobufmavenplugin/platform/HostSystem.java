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
package io.github.ascopes.protobufmavenplugin.platform;

import static java.util.function.Predicate.not;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A bean that exposes information about the underlying platform and the context of the
 * invocation.
 *
 * @author Ashley Scopes
 */
@Named
public final class HostSystem {
  private final String operatingSystem;
  private final String cpuArchitecture;
  private final Path workingDirectory;
  private final Collection<Path> path;
  private final SortedSet<String> pathExt;

  @Inject
  public HostSystem() {
    this(System.getProperties(), System.getenv());
  }

  public HostSystem(Properties properties, Map<String, String> environmentVariables) {
    operatingSystem = properties.getProperty("os.name", "");
    cpuArchitecture = properties.getProperty("os.arch", "");
    workingDirectory = FileUtils.normalize(Path.of(""));
    path = tokenizeFilePath(
        environmentVariables.getOrDefault("PATH", ""),
        paths -> paths
            .map(Path::of)
            .map(FileUtils::normalize)
            .distinct()
            .filter(Files::isDirectory)
            .collect(Collectors.toUnmodifiableList()));
    pathExt = tokenizeFilePath(
        environmentVariables.getOrDefault("PATHEXT", ""),
        extensions -> extensions
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(String::compareToIgnoreCase)),
                Collections::unmodifiableSortedSet
            )));
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

  public boolean isProbablyMacOs() {
    return operatingSystem.toLowerCase(Locale.ROOT).startsWith("mac os");
  }

  public boolean isProbablyWindows() {
    return operatingSystem.toLowerCase(Locale.ROOT).startsWith("windows");
  }

  public boolean isProbablyAndroidTermux() {
    return isProbablyLinux()
        && getWorkingDirectory().toString().startsWith("/data/data/com.termux/");
  }

  public Path getWorkingDirectory() {
    return workingDirectory;
  }

  public Collection<Path> getSystemPath() {
    return path;
  }

  public SortedSet<String> getSystemPathExtensions() {
    return pathExt;
  }

  private static <T> T tokenizeFilePath(String rawValue, Function<Stream<String>, T> mapper) {
    try (var scanner = new Scanner(rawValue).useDelimiter(File.pathSeparator)) {
      var stream = scanner.tokens()
          .map(String::trim)
          .filter(not(String::isBlank));

      return mapper.apply(stream);
    }
  }
}
