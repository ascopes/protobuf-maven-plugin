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

package io.github.ascopes.protobufmavenplugin.sources;

import io.github.ascopes.protobufmavenplugin.generation.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
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
  private final TemporarySpace temporarySpace;

  @Inject
  public ProtoArchiveExtractor(TemporarySpace temporarySpace) {
    this.temporarySpace = temporarySpace;
  }

  public Optional<ProtoFileListing> extractProtoFiles(
      Path zipPath,
      ProtoFileFilter filter
  ) throws IOException {
    var modifiedTime = Files.getLastModifiedTime(zipPath);

    try (var vfs = openZip(zipPath)) {
      var vfsRoot = vfs.getRootDirectories().iterator().next();
      var sourceFiles = findProtoFilesInArchive(vfsRoot, filter);

      if (sourceFiles.isEmpty()) {
        return Optional.empty();
      }

      var extractionRoot = getExtractionRoot().resolve(generateUniqueName(zipPath));
      Files.createDirectories(extractionRoot);

      var targetFiles = new ArrayList<Path>();

      for (var sourceFile : sourceFiles) {
        var targetFile = FileUtils.changeRelativePath(extractionRoot, vfsRoot, sourceFile);
        log.debug("Copying {} to {}", sourceFile.toUri(), targetFile);

        // We have to do this on each iteration to ensure the directory hierarchy exists.
        Files.createDirectories(targetFile.getParent());
        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        // Retain the timestamp of the archive to produce reproducible builds.
        Files.setLastModifiedTime(targetFile, modifiedTime);

        targetFiles.add(targetFile);
      }

      var listing = ImmutableProtoFileListing
          .builder()
          .addAllProtoFiles(targetFiles)
          .protoFilesRoot(extractionRoot)
          .build();

      return Optional.of(listing);
    }
  }

  private Collection<Path> findProtoFilesInArchive(
      Path archiveRootPath,
      ProtoFileFilter filter
  ) throws IOException {
    try (var stream = Files.walk(archiveRootPath)) {
      return stream
          .filter(filePath -> filter.matches(archiveRootPath, filePath))
          .peek(protoFile -> log.debug(
              "Found proto file {} in archive {}",
              protoFile.toUri(),
              archiveRootPath
          ))
          .collect(Collectors.toUnmodifiableList());
    }
  }

  private FileSystem openZip(Path path) throws IOException {
    // Impl note: unlike other constructors, calling this multiple times on the same path
    // will open multiple file system objects. Other constructors do not appear to do this,
    // so would not be thread-safe for concurrent plugin executions.
    try {
      return FileUtils.getFileSystemProvider("jar")
          .newFileSystem(path, Map.of());

    } catch (Exception ex) {
      // The JDK will raise vague exceptions if we try to read something that is not a zip file.
      // See ZipFileSystemProvider#getZipFileSystem for an example.
      throw new IOException("Failed to open " + path + " as a valid ZIP/JAR archive", ex);
    }
  }

  private Path getExtractionRoot() {
    return temporarySpace.createTemporarySpace("archives");
  }

  private String generateUniqueName(Path path) {
    var digest = Digests.sha1(path.toAbsolutePath().toString());
    return FileUtils.getFileNameWithoutExtension(path) + "-" + digest;
  }
}
