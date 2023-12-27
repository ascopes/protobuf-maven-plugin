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
package io.github.ascopes.protobufmavenplugin.fixtures;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test file system support for testing logic across multiple platforms.
 *
 * @author Ashley Scopes
 */
public final class TestFileSystem implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(TestFileSystem.class);

  private final FileSystem fileSystem;

  private TestFileSystem(MemoryFileSystemBuilder builder) {
    fileSystem = unchecked(() -> builder.build());
  }

  @Override
  public void close() {
    unchecked(fileSystem::close);
  }

  public Path getRoot() {
    return fileSystem.getRootDirectories().iterator().next();
  }

  public Path givenDirectoryExists(Path root, String... bits) {
    return unchecked(() -> {
      var dir = reduce(root, bits);
      Files.createDirectories(dir);
      log.trace("Created directory '{}'", dir.toUri());
      return dir;
    });
  }

  public Path givenDirectoryExists(String... bits) {
    return givenDirectoryExists(getRoot(), bits);
  }

  public Path givenFileExists(Path root, String... bits) {
    return unchecked(() -> {
      var file = reduce(root, bits);
      Files.createDirectories(file.getParent());
      Files.createFile(file);
      log.trace("Created file '{}'", file.toUri());

      try {
        changePermissions(file, perms -> {
          perms.clear();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          log.trace("Updated permissions for file '{}'", file.toUri());
        });
      } catch (UnsupportedOperationException ex) {
        // Ignore.
      }

      return file;
    });
  }

  public Path givenFileExists(String... bits) {
    return givenFileExists(getRoot(), bits);
  }

  public void changePermissions(
      Path file,
      Consumer<Set<PosixFilePermission>> modifier
  )  {
    unchecked(() -> {
      // Wrap in a hashset so that we guarantee we can modify the result.
      var permissions = new HashSet<>(Files.getPosixFilePermissions(file));
      modifier.accept(permissions);
      Files.setPosixFilePermissions(file, permissions);
    });
  }

  private static Path reduce(Path root, String... bits) {
    for (var bit : bits) {
      root = root.resolve(bit);
    }
    return root;
  }

  public static TestFileSystem windows() {
    return unchecked(() -> new TestFileSystem(MemoryFileSystemBuilder.newWindows()));
  }

  public static TestFileSystem linux() {
    return unchecked(() -> new TestFileSystem(MemoryFileSystemBuilder.newLinux()));
  }

  private static <T> T unchecked(IoExceptionThrowableSupplier<T> func) {
    try {
      return func.run();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void unchecked(IoExceptionThrowableProcedure proc) {
    try {
      proc.run();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private interface IoExceptionThrowableSupplier<T> {
    T run() throws IOException;
  }

  private interface IoExceptionThrowableProcedure {
    void run() throws IOException;
  }
}
