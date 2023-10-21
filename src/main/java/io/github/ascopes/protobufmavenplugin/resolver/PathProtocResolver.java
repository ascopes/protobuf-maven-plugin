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
import java.util.Scanner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.SystemUtils;

/**
 * Resolver for {@code protoc} that considers any executables in the {@code PATH} environment
 * variable.
 *
 * <p>This expects all executable candidates
 *
 */
public final class PathProtocResolver implements ProtocResolver {

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
      return scanner.tokens()
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
      var periodIndex = fileName.lastIndexOf('.');
      if (periodIndex != -1) {
        fileName = fileName.substring(0, periodIndex);
      }
    }

    return fileName.equals("protoc");
  }
}
