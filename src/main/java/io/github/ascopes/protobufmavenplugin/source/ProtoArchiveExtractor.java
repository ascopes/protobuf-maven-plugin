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
package io.github.ascopes.protobufmavenplugin.source;

import static io.github.ascopes.protobufmavenplugin.source.ProtoFilePredicates.isProtoFile;

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that extracts proto sources from archives into a location on the root file system.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoArchiveExtractor {

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final FileSystemProvider jarFileSystemProvider;
  private final Path extractionRoot;

  @Inject
  public ProtoArchiveExtractor(MavenProject mavenProject) {
    jarFileSystemProvider = FileSystemProvider.installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
        .findFirst()
        .orElseThrow();

    extractionRoot = Path.of(mavenProject.getBuild().getDirectory())
        .resolve("protobuf-maven-plugin")
        .resolve("extracted");
  }

  public Optional<Path> tryExtractProtoFiles(Path zipPath) throws IOException {
    var extractionTarget = extractionRoot.resolve(generateUniqueName(zipPath));

    try (var vfs = jarFileSystemProvider.newFileSystem(zipPath, Map.of())) {
      var vfsRoot = vfs.getRootDirectories().iterator().next();

      Files.walkFileTree(vfsRoot, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(
            Path sourceFile,
            BasicFileAttributes attrs
        ) throws IOException {
          if (isProtoFile(sourceFile)) {
            var targetFile = changeRelativePath(extractionTarget, vfsRoot, sourceFile);
            log.trace("Extracting {} to {}", sourceFile.toUri(), targetFile);
            Files.createDirectories(targetFile.getParent());
            Files.copy(sourceFile, targetFile);
          }

          return FileVisitResult.CONTINUE;
        }
      });
    }

    // Will this create odd behaviour on reruns if archives change? Do we care? If it becomes
    // a problem, we can use a boolean flag instead.
    return Optional.of(extractionTarget).filter(Files::isDirectory);
  }

  private String generateUniqueName(Path archive) {
    var archiveName = FileUtils.getFileNameWithoutExtension(archive);

    try {
      var digest = MessageDigest.getInstance("MD5").digest();
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest) + "-" + archiveName;
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Failed to perform hash operation", ex);
    }
  }

  private Path changeRelativePath(Path newRoot, Path existingRoot, Path existingPath) {
    var path = newRoot;

    for (var part : existingRoot.relativize(existingPath)) {
      path = path.resolve(part.toString());
    }

    return path;
  }
}
