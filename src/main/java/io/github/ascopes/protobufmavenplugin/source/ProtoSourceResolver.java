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

import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.platform.FileUtils;
import io.github.ascopes.protobufmavenplugin.platform.HostSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
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
 * <p>This object maintains an internal work stealing thread pool to enable performing IO
 * concurrently. This should be closed when shutting down this application to prevent leaking
 * resources.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoSourceResolver implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final ProtoArchiveExtractor protoArchiveExtractor;
  private final ExecutorService executorService;

  @Inject
  public ProtoSourceResolver(ProtoArchiveExtractor protoArchiveExtractor) {
    var concurrency = Runtime.getRuntime().availableProcessors() * 8;
    this.protoArchiveExtractor = protoArchiveExtractor;
    executorService = Executors.newWorkStealingPool(concurrency);
  }

  @PreDestroy
  @SuppressWarnings({"auto-closeable", "ResultOfMethodCallIgnored"})
  @Override
  public void close() {
    executorService.shutdown();
    try {
      executorService.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      log.warn("Shutdown was interrupted and will be aborted", ex);
      Thread.currentThread().interrupt();
    }
  }

  public Optional<ProtoFileListing> createProtoFileListing(Path path) throws IOException {
    if (!Files.exists(path)) {
      log.debug("Skipping lookup in path {} as it does not exist", path);
      return Optional.empty();
    }

    if (Files.isRegularFile(path)) {
      return protoArchiveExtractor.extractProtoFiles(path);
    }

    try (var stream = Files.walk(path)) {
      return stream
          .filter(ProtoFilePredicates::isProtoFile)
          .peek(protoFile -> log.debug("Found proto file in root {}: {}", path, protoFile))
          .collect(Collectors.collectingAndThen(
              Collectors.toCollection(LinkedHashSet::new),
              Optional::of
          ))
          .filter(not(Set::isEmpty))
          .map(protoFiles -> ImmutableProtoFileListing
              .builder()
              .addAllProtoFiles(protoFiles)
              .protoFilesRoot(path)
              .originalRoot(path)
              .build());
    }
  }

  public Collection<ProtoFileListing> createProtoFileListings(
      Collection<Path> originalPaths
  ) throws IOException {
    var results = new ArrayList<Optional<ProtoFileListing>>();
    var exceptions = new ArrayList<Exception>();

    originalPaths
        .stream()
        // GH-132: Normalize to ensure different paths to the same file do not
        //   get duplicated across more than one extraction site.
        .map(FileUtils::normalize)
        // GH-132: Avoid running multiple times on the same location.
        .distinct()
        .map(this::submitProtoFileListingTask)
        // terminal operation to ensure all are scheduled prior to joining.
        .collect(Collectors.toList())
        .forEach(task -> {
          try {
            results.add(task.get());
          } catch (ExecutionException | InterruptedException ex) {
            exceptions.add(ex);
          }
        });

    if (!exceptions.isEmpty()) {
      var causeIterator = exceptions.iterator();
      var ex = new IOException(
          "Failed to discover protobuf sources in some locations", causeIterator.next()
      );
      causeIterator.forEachRemaining(ex::addSuppressed);
      throw ex;
    }

    return results
        .stream()
        .flatMap(Optional::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private FutureTask<Optional<ProtoFileListing>> submitProtoFileListingTask(Path path) {
    log.debug("Searching for proto files in '{}' asynchronously...", path);
    var task = new FutureTask<>(() -> createProtoFileListing(path));
    executorService.submit(task);
    return task;
  }
}
