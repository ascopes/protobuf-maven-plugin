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
package io.github.ascopes.protobufmavenplugin.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers for extracting archives to the root file system.
 *
 * @author Ashley Scopes
 */
public final class ArchiveExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveExtractor.class);

  private ArchiveExtractor() {
    // Static-only class.
  }

  /**
   * Extract the contents of multiple archives to a given base directory.
   *
   * @param archives  the archives to extract.
   * @param targetDir the target directory to write outputs to.
   * @return the created path on the root file system to use with {@code protoc}.
   * @throws IOException if an error occurs performing IO.
   */
  public static Collection<Path> extractArchives(
      Collection<Path> archives,
      Path targetDir
  ) throws IOException {
    var jarFsProvider = FileSystemProvider
        .installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equals("jar"))
        .findFirst()
        .orElseThrow();

    var resolved = new ArrayList<Path>();

    for (var archive : archives) {
      var friendlyDirName = archive.getFileName().toString();
      var fileExtensionIndex = friendlyDirName.lastIndexOf('.');
      friendlyDirName = fileExtensionIndex == -1
          ? friendlyDirName
          : friendlyDirName.substring(0, fileExtensionIndex);
      var outputDir = targetDir.resolve(friendlyDirName);
      LOGGER.info("Extracting contents of '{}' to {}", archive, outputDir);

      // We could support "releaseVersion" and "multi-release" attributes in the future.
      var env = Map.<String, String>of();
      try (var fs = jarFsProvider.newFileSystem(archive, env)) {
        var archiveRoot = fs.getRootDirectories().iterator().next();

        Files.walkFileTree(archiveRoot, new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(
              Path sourceFile,
              BasicFileAttributes attrs
          ) throws IOException {
            var relativeFile = archiveRoot.relativize(sourceFile);
            var targetFile = resolveCrossFileSystem(outputDir, relativeFile);
            Files.createDirectories(targetFile.getParent());
            Files.copy(
                sourceFile,
                targetFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES
            );

            return FileVisitResult.CONTINUE;
          }
        });

        resolved.add(outputDir);
      }
    }

    return Collections.unmodifiableList(resolved);
  }

  private static Path resolveCrossFileSystem(Path root, Path toResolve) {
    for (var path : toResolve) {
      root = root.resolve(path.getFileName().toString());
    }
    return root;
  }
}
