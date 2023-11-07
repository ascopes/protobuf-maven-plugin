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
package io.github.ascopes.protobufmavenplugin.fixture;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import org.junit.jupiter.api.io.TempDir;

/**
 * Base class for tests to extend that work with file system elements.
 *
 * @author Ashley Scopes
 */
public abstract class FileSystemTestSupport {

  private static final SimpleFileVisitor<Path> recursiveRemover = new SimpleFileVisitor<>() {
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  };

  @TempDir
  Path baseDir;

  public Path givenFileExists(String... bits) {
    var path = foldRelative(bits);
    try {
      Files.createDirectories(path.getParent());
      return Files.createFile(path);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public Path givenDirectoryExists(String... bits) {
    try {
      return Files.createDirectories(foldRelative(bits));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public Path givenDirectoryDoesNotExist(String... bits) {
    var path = foldRelative(bits);
    try {
      // Parent should exist, just for sanity.
      Files.createDirectories(path.getParent());
      Files.walkFileTree(path, recursiveRemover);
      return path;
    } catch (NoSuchFileException ex) {
      // Usually should be raised as it is unlikely the test data already existed...
      return path;
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void givenFileIsExecutable(Path path) {
    try {
      var perms = new HashSet<>(Files.getPosixFilePermissions(path));
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      perms.add(PosixFilePermission.GROUP_EXECUTE);
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(path, perms);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void givenFileIsNotExecutable(Path path) {
    try {
      var perms = new HashSet<>(Files.getPosixFilePermissions(path));
      perms.remove(PosixFilePermission.OWNER_EXECUTE);
      perms.remove(PosixFilePermission.GROUP_EXECUTE);
      perms.remove(PosixFilePermission.OTHERS_EXECUTE);
      Files.setPosixFilePermissions(path, perms);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public void givenFileOrDirectoryIsInaccessible(Path path) {
    try {
      Files.setPosixFilePermissions(path, EnumSet.noneOf(PosixFilePermission.class));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private Path foldRelative(String... bits) {
    var path = baseDir;
    for (var bit : bits) {
      path = path.resolve(bit);
    }
    return path;
  }
}
