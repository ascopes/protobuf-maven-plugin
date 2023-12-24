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

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts {@code proto} files from compatible archives and writes them to a temporary location.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoArchiveExtractor {

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final MavenProject mavenProject;
  private final ProtoFileResolver protoFileResolver;
  private final FileSystemProvider jarFileSystemProvider;

  @Inject
  public ProtoArchiveExtractor(MavenProject mavenProject, ProtoFileResolver protoFileResolver) {
    this.mavenProject = mavenProject;
    this.protoFileResolver = protoFileResolver;

    jarFileSystemProvider = FileSystemProvider.installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
        .findFirst()
        .orElseThrow();
  }

  public Optional<Path> extractArchiveContents(Path archivePath) throws IOException {
    log.trace("Discovering proto files in archive {}", archivePath);
    try (var fs = jarFileSystemProvider.newFileSystem(archivePath, Map.of())) {
      var protoFiles = protoFileResolver.findProtoFiles(fs.getRootDirectories());
      if (protoFiles.isEmpty()) {
        return Optional.empty();
      }

      var extractedDir = Path.of(mavenProject.getBuild().getOutputDirectory())
          .resolve("protobuf-maven-plugin")
          .resolve("extracted")
          .resolve(FileUtils.getFileNameWithoutExtension(archivePath));

      for (var protoFile : protoFiles) {
        var targetFile = extractedDir.resolve(protoFile.toString());
        log.trace("Copying {} to {}", protoFile.toUri(), targetFile.toUri());

        Files.createDirectories(targetFile.getParent());
        Files.copy(protoFile, targetFile);
      }

      return Optional.of(extractedDir);
    }
  }
}
