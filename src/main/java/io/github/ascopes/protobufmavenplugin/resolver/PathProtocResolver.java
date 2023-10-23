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

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
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
 * <p>The executable name can be overridden, but defaults to "{@code protoc}".
 *
 * @author Ashley Scopes
 */
public final class PathProtocResolver implements ProtocResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(PathProtocResolver.class);

  private final String executableName;

  public PathProtocResolver(String executableName) {
    this.executableName = executableName;
  }

  @Override
  public Path resolveProtoc() throws ProtocResolutionException {
    try {
      for (var indexableDirectory : HostEnvironment.systemPath()) {
        LOGGER.debug(
            "Searching directory '{}' for protoc binary named '{}'",
            indexableDirectory,
            executableName
        );

        try (var fileStream = Files.list(indexableDirectory)) {
          var result = fileStream
              .filter(Files::isExecutable)
              .filter(this::isProtoc)
              .findFirst();

          if (result.isPresent()) {
            var path = result.get();
            LOGGER.info("Resolved protoc binary to '{}'", path);
          }
        }
      }

      throw new ProtocResolutionException("No protoc binary was found in the $PATH");

    } catch (IOException ex) {
      throw new ProtocResolutionException("File system error", ex);
    }
  }

  private boolean isProtoc(Path path) {
    var fileName = path.getFileName().toString();

    if (HostEnvironment.isWindows()) {
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
