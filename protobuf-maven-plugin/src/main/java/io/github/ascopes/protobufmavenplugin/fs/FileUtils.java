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
package io.github.ascopes.protobufmavenplugin.fs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
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

  public static FileSystem openZipAsFileSystem(Path zipPath) throws IOException {
    // Impl note: unlike other constructors, calling this multiple times on the same path
    // will open multiple file system objects. Other constructors do not appear to do this,
    // so would not be thread-safe for concurrent plugin executions.
    try {
      log.trace("Opening a new NIO virtual file system for {}", zipPath);

      return FileSystemProvider.installedProviders()
          .stream()
          .filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
          .findFirst()
          .orElseThrow(FileSystemNotFoundException::new)
          .newFileSystem(zipPath, Map.of());
    } catch (Exception ex) {
      // The JDK will raise vague exceptions if we try to read something that is not a zip file.
      // See ZipFileSystemProvider#getZipFileSystem for an example.
      throw new IOException("Failed to open " + zipPath + " as a valid ZIP/JAR archive", ex);
    }
  }

  public static void makeExecutable(Path file) throws IOException {
    try {
      log.trace("Ensuring {} is executable", file);
      var perms = new HashSet<>(Files.getPosixFilePermissions(file));
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(file, perms);
    } catch (UnsupportedOperationException ex) {
      log.trace(
          "File system does not support setting POSIX file permissions, "
              + "this is probably fine, continuing anyway...");
    }
  }

  public static List<Path> rebaseFileTree(
      Path existingRoot,
      Path newRoot,
      Stream<Path> paths
  ) throws IOException {
    var iter = paths.iterator();
    var newPaths = new ArrayList<Path>();

    while (iter.hasNext()) {
      var existingPath = iter.next();
      var newPath = newRoot;
      // Rebuild the new target path using the fragments from the original relative
      // path. This enables us to relativize paths on different file systems correctly.
      for (var part : existingRoot.relativize(existingPath)) {
        newPath = newPath.resolve(part.toString());
      }

      log.trace(
          "Copying {} to {} (existing root={}, new root={})",
          existingPath,
          newPath,
          existingRoot,
          newRoot
      );

      Files.createDirectories(newPath.getParent());

      Files.copy(
          existingPath,
          newPath,
          StandardCopyOption.COPY_ATTRIBUTES,
          StandardCopyOption.REPLACE_EXISTING
      );

      if (!Files.isDirectory(newPath)) {
        newPaths.add(newPath);
      }
    }

    return Collections.unmodifiableList(newPaths);
  }

  public static InputStream newBufferedInputStream(
      Path path,
      OpenOption... options
  ) throws IOException {
    return new BufferedInputStream(Files.newInputStream(path, options));
  }

  public static OutputStream newBufferedOutputStream(
      Path path,
      OpenOption... options
  ) throws IOException {
    return new BufferedOutputStream(Files.newOutputStream(path, options));
  }

  public static void deleteTree(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }

    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(
          Path file,
          BasicFileAttributes attrs
      ) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(
          Path directory,
          BasicFileAttributes attrs
      ) throws IOException {
        // We do not want to recurse if the directory is actually just a link,
        // as this might be exploited to allow the plugin to damage files outside
        // the intended location by making a strategicly positioned link.

        // XXX: we do not check for hard links. If we need to do that
        // for security purposes, we'll have to check for the unix:nlink
        // attribute being present on Unix, or some other attribute for Windows
        // which I have yet to find the documentation for. For now, we'll just
        // habdle symbolic links for basic security.
        return Files.isSymbolicLink(directory)
            ? FileVisitResult.SKIP_SUBTREE
            : FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(
          Path directory,
          @Nullable IOException ex
      ) throws IOException {
        Files.delete(directory);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
