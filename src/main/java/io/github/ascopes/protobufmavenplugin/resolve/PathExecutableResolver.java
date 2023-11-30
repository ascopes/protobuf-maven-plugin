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
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An executable resolver that searches the system {@code $PATH} for a match.
 *
 * @author Ashley Scopes
 */
public final class PathExecutableResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(PathExecutableResolver.class);

  /**
   * Initialise this resolver.
   */
  public PathExecutableResolver() {
    // Nothing to do here.
  }

  /**
   * Determine the path to the executable.
   *
   * @param executable the executable to resolve.
   * @return the executable path.
   * @throws ExecutableResolutionException if resolution fails for any reason.
   */
  public Path resolve(Executable executable) throws ExecutableResolutionException {
    var binaryName = executable.getExecutableName();

    var predicate = HostEnvironment.isWindows()
        ? windowsMatchPredicate(binaryName)
        : posixMatchPredicate(binaryName);

    try {
      for (var indexableDirectory : HostEnvironment.systemPath()) {
        if (!Files.isDirectory(indexableDirectory)) {
          LOGGER.warn("Ignoring non-existent directory '{}' within $PATH", indexableDirectory);
          continue;
        }

        LOGGER.debug("Searching directory '{}' for {} binary", indexableDirectory, binaryName);

        try (var fileStream = Files.list(indexableDirectory)) {
          var result = fileStream
              .peek(pathCandidateLogger(binaryName))
              .filter(predicate)
              .findFirst()
              .orElse(null);

          if (result != null) {
            LOGGER.info("Resolved {} binary to '{}'", binaryName, result);
            return result;
          }
        }
      }
    } catch (IOException ex) {
      throw new ExecutableResolutionException(
          "File system error while searching for " + binaryName, ex
      );
    }

    throw new ExecutableResolutionException("No " + binaryName + " binary was found in the $PATH");
  }

  private Predicate<Path> windowsMatchPredicate(String binaryName) {
    // On Windows, we have to ignore case sensitivity. For example, protoc.exe and
    // PROTOC.EXE are the same thing. We have to look at the file name without the
    // extension to determine the program name, and should match only if the file
    // extension is listed in the $PATHEXT.
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

      return baseFileName.equalsIgnoreCase(binaryName) && pathExt.contains(fileExtension);
    };
  }

  private Predicate<Path> posixMatchPredicate(String binaryName) {
    // On POSIX systems, we check if the file is named 'protoc' exactly. If it is, then we check
    // that it has the executable bit set for the current user.
    return path -> path.getFileName().toString().equals(binaryName) && Files.isExecutable(path);
  }

  private Consumer<Path> pathCandidateLogger(String binaryName) {
    return path -> LOGGER.trace("Checking if '{}' is a match for {}", path, binaryName);
  }
}
