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

package io.github.ascopes.protobufmavenplugin.resolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolver for {@code protoc} that considers any executables in the {@code $PATH} environment
 * variable.
 *
 * <p>This expects the binary to satisfy the following constraints:
 *
 * <ul>
 *   <li>The executable must be in one of the directories on the {@code $PATH}
 *       ({@code %PATH%} on Windows);
 *   <li>The binary must be marked as being executable ({@code chmod +x});
 *   <li>On POSIX systems, the binary must be named exactly "{@code protoc}";
 *   <li>On Windows systems, the binary must be named "{@code protoc}", ignoring case
 *       sensitivity and any file extension.
 * </ul>
 *
 * <p>The executable name can be overridden, but defaults to "{@code protoc}".
 *
 * @author Ashley Scopes
 */
public final class PathProtocResolver implements ProtocResolver {

  private static final String PROTOC_DEFAULT_BINARY = "protoc";

  private String executableName;

  /**
   * Initialise this resolver.
   */
  public PathProtocResolver() {
    executableName = PROTOC_DEFAULT_BINARY;
  }

  /**
   * Get the executable name to use.
   *
   * @return the executable name.
   */
  public String getExecutableName() {
    return executableName;
  }

  /**
   * Set the executable name.
   *
   * @param executableName the name of the executable to look for.
   */
  @Parameter(name = "executableName", defaultValue = PROTOC_DEFAULT_BINARY)
  public void setExecutableName(String executableName) {
    this.executableName = Objects.requireNonNull(executableName);
  }

  @Override
  public Path resolveProtoc() throws ProtocResolutionException {
    try {
      var pathVariableValue = SystemUtils.getEnvironmentVariable("PATH", "");

      for (var indexableDirectory : determineIndexableDirectories(pathVariableValue)) {
        try (var fileStream = Files.list(indexableDirectory)) {
          var result = fileStream
              .filter(Files::isExecutable)
              .filter(this::isProtoc)
              .findFirst();

          if (result.isPresent()) {
            return result.get();
          }
        }
      }

      throw new ProtocResolutionException("No protoc binary was found in the $PATH");
    } catch (IOException ex) {
      throw new ProtocResolutionException("File system error", ex);
    }
  }

  private List<Path> determineIndexableDirectories(String pathVariableValue) {
    var separator = SystemProperties.getPathSeparator();

    try (var scanner = new Scanner(pathVariableValue).useDelimiter(separator)) {
      return scanner
          .tokens()
          .map(Path::of)
          .filter(Files::isDirectory)
          .collect(Collectors.toList());
    }
  }

  private boolean isProtoc(Path path) {
    var fileName = path.getFileName().toString();

    if (SystemUtils.IS_OS_WINDOWS) {
      // Windows filename lookups will always be case-insensitive.
      fileName = fileName.toLowerCase(Locale.ROOT);

      // Windows filename lookups will ignore the file extension, so we have to strip that out.
      // We take the last part of the extension only, since Windows appears to only consider this
      // (e.g. `foo.bar.baz` is considered to be named `foo.bar` with extension `.baz`.
      var periodIndex = fileName.lastIndexOf('.');
      if (periodIndex != -1) {
        fileName = fileName.substring(0, periodIndex);
      }
    }

    return fileName.equals(executableName);
  }
}
