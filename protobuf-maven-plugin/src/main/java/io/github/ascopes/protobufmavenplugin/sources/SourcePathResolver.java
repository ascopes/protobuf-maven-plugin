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
package io.github.ascopes.protobufmavenplugin.sources;

import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.utils.ConcurrentExecutor;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that can index and resolve protobuf sources in a file tree.
 *
 * <p>In addition, it can discover sources within archives recursively. These results will be
 * extracted to a location within the Maven build directory to enable {@code protoc} and other
 * plugins to be able to view them without needing access to the Java NIO file system APIs.
 *
 * @author Ashley Scopes
 */
@MojoExecutionScoped
@Named
public final class SourcePathResolver {

  private static final Set<String> POM_FILE_EXTENSIONS = Set.of(".pom", ".xml");
  private static final Set<String> ZIP_FILE_EXTENSIONS = Set.of(".jar", ".zip");

  private static final Logger log = LoggerFactory.getLogger(SourcePathResolver.class);

  private final ConcurrentExecutor concurrentExecutor;
  private final TemporarySpace temporarySpace;

  @Inject
  public SourcePathResolver(
      ConcurrentExecutor concurrentExecutor,
      TemporarySpace temporarySpace
  ) {
    this.concurrentExecutor = concurrentExecutor;
    this.temporarySpace = temporarySpace;
  }

  public Collection<SourceListing> resolveSources(
      Collection<Path> rootPaths,
      SourceGlobFilter filter
  ) {
    return rootPaths
        .stream()
        // GH-132: Normalize to ensure different paths to the same file do not
        //   get duplicated across more than one extraction site.
        .map(FileUtils::normalize)
        // GH-132: Avoid running multiple times on the same location.
        .distinct()
        .map(path -> concurrentExecutor.submit(() -> resolveSources(path, filter)))
        .collect(concurrentExecutor.awaiting())
        .stream()
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  private Optional<SourceListing> resolveSources(
      Path rootPath,
      SourceGlobFilter filter
  ) throws IOException {
    if (!Files.exists(rootPath)) {
      log.debug("Skipping lookup in path {} as it does not exist", rootPath);
      return Optional.empty();
    }

    return Files.isRegularFile(rootPath)
        ? resolveSourcesWithinFile(rootPath, filter)
        : resolveSourcesWithinDirectory(rootPath, filter);
  }

  private Optional<SourceListing> resolveSourcesWithinFile(
      Path rootPath,
      SourceGlobFilter filter
  ) throws IOException {
    // XXX: we do not convert the extension to lowercase, as there
    //  is some nuanced logic within the ZipFileSystemProvider that appears
    //  to be case-sensitive.
    //  See https://github.com/openjdk/jdk/blob/cafb3dc49157daf12c1a0e5d78acca8188c56918/src/jdk.zipfs/share/classes/jdk/nio/zipfs/ZipFileSystemProvider.java#L128
    var fileExtension = FileUtils.getFileExtension(rootPath);

    // GH-327: We filter out non-zip archives to prevent vague errors if
    // users include non-zip dependencies such as POMs, which cannot be extracted.
    if (fileExtension.filter(ZIP_FILE_EXTENSIONS::contains).isPresent()) {
      return resolveSourcesWithinArchive(rootPath, filter);
    }

    if (fileExtension.filter(POM_FILE_EXTENSIONS::contains).isPresent()) {
      log.debug("Ignoring invalid dependency on potential POM at {}", rootPath);
      return Optional.empty();
    }

    log.warn("Ignoring unknown archive type at {}", rootPath);
    return Optional.empty();
  }

  private Optional<SourceListing> resolveSourcesWithinArchive(
      Path rootPath,
      SourceGlobFilter filter
  ) throws IOException {
    // We move the source files out of the archive and place them in a location on the root
    // file system so that protoc is able to see their contents.
    try (var vfs = FileUtils.openZipAsFileSystem(rootPath)) {
      var vfsRoot = vfs.getRootDirectories().iterator().next();
      var sourceFiles = resolveSources(vfsRoot, filter);

      if (sourceFiles.isEmpty()) {
        return Optional.empty();
      }

      var extractionRoot = getArchiveExtractionRoot().resolve(generateUniqueName(rootPath));
      var relocatedSourceFiles = FileUtils.rebaseFileTree(
          vfsRoot,
          extractionRoot,
          sourceFiles.get().getSourceProtoFiles().stream()
      );

      return Optional.of(createSourceListing(relocatedSourceFiles, extractionRoot));
    }
  }

  private Optional<SourceListing> resolveSourcesWithinDirectory(
      Path rootPath,
      SourceGlobFilter filter
  ) throws IOException {
    try (var stream = Files.walk(rootPath)) {
      return stream
          .filter(filePath -> filter.matches(rootPath, filePath))
          .peek(protoFile -> log.trace(
              "Found proto file in root {}: {}", 
              rootPath.toUri(), 
              protoFile
          ))
          .collect(Collectors.collectingAndThen(
              // Terminal operation, means we do not return a closed stream
              // by mistake.
              Collectors.toCollection(LinkedHashSet::new),
              Optional::of
          ))
          .filter(not(Collection::isEmpty))
          .map(protoFiles -> createSourceListing(protoFiles, rootPath));
    }
  }

  private SourceListing createSourceListing(Collection<Path> sourceFiles, Path rootPath) {
    return ImmutableSourceListing.builder()
        .addAllSourceProtoFiles(sourceFiles)
        .sourceRoot(rootPath)
        .build();
  }

  private Path getArchiveExtractionRoot() {
    return temporarySpace.createTemporarySpace("archives");
  }

  private String generateUniqueName(Path path) {
    // Use a URI here as the URI will correctly encapsulate archives within archives. Paths may have
    // name collisions between archives using the same relative file paths internally.
    var digest = Digests.sha1(FileUtils.normalize(path).toUri().toASCIIString());
    return FileUtils.getFileNameWithoutExtension(path) + "-" + digest;
  }
}
