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
package io.github.ascopes.protobufmavenplugin.system;

import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common helper logic for file handling operations.
 *
 * @author Ashley Scopes
 */
public final class FileUtils {
  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  private FileUtils() {
    // Static-only class
  }

  public static Path normalize(Path path) {
    return path.normalize().toAbsolutePath();
  }

  public static String getFileNameWithoutExtension(Path path) {
    var fileName = path.getFileName().toString();
    var lastDotIndex = fileName.lastIndexOf('.');

    // -1 means no extension, 0 means the file name starts with a dot (e.g. `.gitignore'),
    // so we don't want to extract a file extension from that.
    return lastDotIndex <= 0
        ? fileName
        : fileName.substring(0, lastDotIndex);
  }

  public static Optional<String> getFileExtension(Path path) {
    var fileName = path.getFileName().toString();
    var lastDotIndex = fileName.lastIndexOf('.');

    // -1 means no extension, 0 means the file name starts with a dot (e.g. `.gitignore'),
    // so we don't want to extract a file extension from that.
    return lastDotIndex <= 0
        ? Optional.empty()
        : Optional.of(fileName.substring(lastDotIndex));
  }

  public static FileSystemProvider getFileSystemProvider(String scheme) {
    return FileSystemProvider
        .installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equalsIgnoreCase(scheme))
        .peek(provider -> log.debug("Found {} file system provider {}", scheme, provider))
        .findFirst()
        .orElseThrow(() -> new FileSystemNotFoundException(
            "No file system provider for " + scheme + " was found"
        ));
  }

  public static void makeExecutable(Path file) throws IOException {
    try {
      log.debug("Ensuring {} is executable", file);
      var perms = new HashSet<>(Files.getPosixFilePermissions(file));
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(file, perms);
    } catch (UnsupportedOperationException ex) {
      log.debug("File system does not support setting POSIX file permissions");
    }
  }
}
