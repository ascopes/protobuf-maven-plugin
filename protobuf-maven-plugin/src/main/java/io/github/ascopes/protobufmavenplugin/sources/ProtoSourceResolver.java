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

import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.utils.ConcurrentExecutor;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that can index and resolve proto files in a file tree.
 *
 * <p>In addition, it can discover proto files within archives. These results will be
 * extracted to a location within the Maven build directory to enable {@code protoc} and other
 * plugins to be able to view them without needing access to the Java NIO file system APIs.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoSourceResolver {

  private static final Set<String> POM_FILE_EXTENSIONS = Set.of(".pom", ".xml");
  private static final Set<String> ZIP_FILE_EXTENSIONS = Set.of(".jar", ".zip");

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final ConcurrentExecutor concurrentExecutor;
  private final ProtoArchiveExtractor protoArchiveExtractor;

  @Inject
  public ProtoSourceResolver(
      ConcurrentExecutor concurrentExecutor,
      ProtoArchiveExtractor protoArchiveExtractor
  ) {
    this.concurrentExecutor = concurrentExecutor;
    this.protoArchiveExtractor = protoArchiveExtractor;
  }

  public Collection<ProtoFileListing> createProtoFileListings(
      Collection<Path> rootPaths,
      ProtoFileFilter filter
  ) {
    return rootPaths
        .stream()
        // GH-132: Normalize to ensure different paths to the same file do not
        //   get duplicated across more than one extraction site.
        .map(FileUtils::normalize)
        // GH-132: Avoid running multiple times on the same location.
        .distinct()
        .map(path -> concurrentExecutor.submit(() -> createProtoFileListing(path, filter)))
        .collect(concurrentExecutor.awaiting())
        .stream()
        .flatMap(Optional::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  public Optional<ProtoFileListing> createProtoFileListing(
      Path rootPath,
      ProtoFileFilter filter
  ) throws IOException {
    if (!Files.exists(rootPath)) {
      log.debug("Skipping lookup in path {} as it does not exist", rootPath);
      return Optional.empty();
    }

    if (Files.isRegularFile(rootPath)) {
      return createProtoFileListingForArchive(rootPath, filter);
    }

    try (var stream = Files.walk(rootPath)) {
      return stream
          .filter(filePath -> filter.matches(rootPath, filePath))
          .peek(protoFile -> log.trace("Found proto file in root {}: {}", rootPath, protoFile))
          .collect(Collectors.collectingAndThen(
              // Terminal operation, means we do not return a closed stream
              // by mistake.
              Collectors.toCollection(LinkedHashSet::new),
              Optional::of
          ))
          .filter(not(Collection::isEmpty))
          .map(protoFiles -> ImmutableProtoFileListing
              .builder()
              .addAllProtoFiles(protoFiles)
              .protoFilesRoot(rootPath)
              .build());
    }
  }

  private Optional<ProtoFileListing> createProtoFileListingForArchive(
      Path rootPath,
      ProtoFileFilter filter
  ) throws IOException {
    // XXX: we do not convert the extension to lowercase, as there
    //  is some nuanced logic within the ZipFileSystemProvider that appears
    //  to be case-sensitive.
    //  See https://github.com/openjdk/jdk/blob/cafb3dc49157daf12c1a0e5d78acca8188c56918/src/jdk.zipfs/share/classes/jdk/nio/zipfs/ZipFileSystemProvider.java#L128
    var fileExtension = FileUtils.getFileExtension(rootPath);

    // GH-327: We filter out non-zip archives to prevent vague errors if
    // users include non-zip dependencies such as POMs, which cannot be extracted.
    if (fileExtension.filter(ZIP_FILE_EXTENSIONS::contains).isPresent()) {
      return protoArchiveExtractor.extractProtoFiles(rootPath, filter);
    }

    if (fileExtension.filter(POM_FILE_EXTENSIONS::contains).isPresent()) {
      log.debug("Ignoring invalid dependency on potential POM at {}", rootPath);
      return Optional.empty();
    }

    log.warn("Ignoring unknown archive type at {}", rootPath);
    return Optional.empty();
  }
}
