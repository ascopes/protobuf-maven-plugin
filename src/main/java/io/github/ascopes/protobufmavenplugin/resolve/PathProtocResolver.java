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

package io.github.ascopes.protobufmavenplugin.resolve;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for {@code protoc} that considers any executables in the {@code $PATH} environment
 * variable.
 *
 * <p>This expects the binary to satisfy the following constraints:
 *
 * <ul>
 *   <li>The executable must be in one of the directories on the {@code $PATH}
 *       ({@code %PATH%} on Windows);
 *   <li>On POSIX systems, the binary must be named exactly "{@code protoc}";
 *   <li>On Windows systems, the binary must be named "{@code protoc}", ignoring case
 *       sensitivity, and ignoring any file extension (so "{@code PROTOC.EXE}" would be a direct
 *       match here).
 * </ul>
 *
 * @author Ashley Scopes
 */
public final class PathProtocResolver implements ProtocResolver {

  private static final String PROTOC = "protoc";
  private static final Logger LOGGER = LoggerFactory.getLogger(PathProtocResolver.class);

  @Override
  public Path resolveProtoc() throws ProtocResolutionException {
    var predicate = HostEnvironment.isWindows()
        ? isProtocWindows()
        : isProtocPosix();

    try {
      for (var indexableDirectory : HostEnvironment.systemPath()) {
        if (!Files.isDirectory(indexableDirectory)) {
          LOGGER.warn("Ignoring non-existent directory '{}' within $PATH", indexableDirectory);
          continue;
        }

        LOGGER.debug(
            "Searching directory '{}' for protoc binary named '{}'",
            indexableDirectory,
            PROTOC
        );

        try (var fileStream = Files.list(indexableDirectory)) {
          var result = fileStream
              .peek(this::logFile)
              .filter(predicate)
              .findFirst()
              .orElse(null);

          if (result != null) {
            LOGGER.info("Resolved protoc binary to '{}'", result);
            return result;
          }
        }
      }
    } catch (IOException ex) {
      throw new ProtocResolutionException("File system error", ex);
    }

    throw new ProtocResolutionException("No protoc binary was found in the $PATH");
  }

  private void logFile(Path file) {
    LOGGER.trace("Checking if '{}' is a potential '{}' candidate", file, PROTOC);
  }

  private Predicate<Path> isProtocWindows() {
    var pathExt = HostEnvironment.systemPathExtensions();

    return path -> {
      var fileName = path.getFileName().toString();
      var fileExtensionIndex = fileName.lastIndexOf('.');
      var fileExtension = fileExtensionIndex < 0
          ? ""
          : fileName.substring(fileExtensionIndex);
      var baseFileName = fileExtensionIndex < 0
          ? fileName
          : fileName.substring(0, fileExtensionIndex);

      return baseFileName.equalsIgnoreCase(PROTOC) && pathExt.contains(fileExtension);
    };
  }

  private Predicate<Path> isProtocPosix() {
    // On POSIX systems, we check if the file is named 'protoc' exactly. If it is, then we check
    // that it has the executable bit set for the current user.
    return path -> path.getFileName().toString().equals(PROTOC) && Files.isExecutable(path);
  }
}
