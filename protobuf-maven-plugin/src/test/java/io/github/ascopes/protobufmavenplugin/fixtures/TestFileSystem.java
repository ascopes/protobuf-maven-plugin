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

/**
 * Test file system support for testing logic across multiple platforms.
 *
 * @author Ashley Scopes
 */
public final class TestFileSystem implements Closeable {

  private final FileSystem fileSystem;
  private final boolean isPosix;

  private TestFileSystem(MemoryFileSystemBuilder builder, boolean isPosix) {
    fileSystem = unchecked(() -> builder.build());
    this.isPosix = isPosix;
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

      if (isPosix) {
        // Apparently we need executable permissions too otherwise we cannot make the child
        // executable.
        changePermissions(dir, perms -> {
          perms.clear();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          perms.add(PosixFilePermission.OWNER_EXECUTE);
        });
      }

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

      if (isPosix) {
        changePermissions(file, perms -> {
          perms.clear();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
        });
      }

      return file;
    });
  }

  public Path givenExecutableFileExists(Path root, String... bits) {
    return unchecked(() -> {
      var file = reduce(root, bits);
      Files.createDirectories(file.getParent());
      Files.createFile(file);

      if (isPosix) {
        changePermissions(file, perms -> {
          perms.clear();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          perms.add(PosixFilePermission.OWNER_EXECUTE);
        });
      }

      return file;
    });
  }

  public Path givenExecutableSymbolicLinkExists(Path target, Path root, String... bits) {
    return unchecked(() -> {
      var source = reduce(root, bits);
      Files.createDirectories(source.getParent());
      Files.createSymbolicLink(source, target);

      if (isPosix) {
        changePermissions(source, perms -> {
          perms.clear();
          perms.add(PosixFilePermission.OWNER_READ);
          perms.add(PosixFilePermission.OWNER_WRITE);
          perms.add(PosixFilePermission.OWNER_EXECUTE);
        });
      }

      return source;
    });
  }

  public Path givenFileExists(String... bits) {
    return givenFileExists(getRoot(), bits);
  }

  public void changePermissions(
      Path file,
      Consumer<Set<PosixFilePermission>> modifier
  ) {
    unchecked(() -> {
      // Wrap in a hashset so that we guarantee we can modify the result.
      var originalPermissions = Files.getPosixFilePermissions(file);
      var permissions = new HashSet<>(originalPermissions);
      modifier.accept(permissions);
      Files.setPosixFilePermissions(file, permissions);
    });
  }

  private Path reduce(Path root, String... bits) {
    for (var bit : bits) {
      root = root.resolve(bit);
    }
    return root;
  }

  public static TestFileSystem windows() {
    return unchecked(() -> new TestFileSystem(MemoryFileSystemBuilder.newWindows(), false));
  }

  public static TestFileSystem macOs() {
    return unchecked(() -> new TestFileSystem(MemoryFileSystemBuilder.newMacOs(), true));
  }

  public static TestFileSystem linux() {
    return unchecked(() -> new TestFileSystem(MemoryFileSystemBuilder.newLinux(), true));
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
