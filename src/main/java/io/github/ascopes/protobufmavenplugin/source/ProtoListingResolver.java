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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public final class ProtoListingResolver implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final ProtoArchiveExtractor protoArchiveExtractor;
  private final ExecutorService executorService;

  @Inject
  public ProtoListingResolver(ProtoArchiveExtractor protoArchiveExtractor) {
    var concurrency = Runtime.getRuntime().availableProcessors() * 4;

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
      Thread.currentThread().interrupt();
    }
  }

  public Collection<ProtoFileListing> createProtoFileListings(
      Collection<Path> originalPaths
  ) throws IOException {
    var futures = new ArrayList<CompletableFuture<Optional<ProtoFileListing>>>();

    for (var originalPath : originalPaths) {
      futures.add(createProtoFileListingAsync(originalPath));
    }

    var results = new ArrayList<ProtoFileListing>();
    var exceptions = new ArrayList<Exception>();

    for (var future : futures) {
      try {
        future.get().ifPresent(results::add);
      } catch (ExecutionException | InterruptedException ex) {
        exceptions.add(ex);
      }
    }

    if (!exceptions.isEmpty()) {
      var ex = new IOException("Failed to create listings asynchronously");
      exceptions.forEach(ex::addSuppressed);
      throw ex;
    }

    return results;
  }

  public Optional<ProtoFileListing> createProtoFileListing(Path originalPath) throws IOException {
    Path rootPath;

    if (Files.isRegularFile(originalPath)) {
      // Treat it as a ZIP archive. We might want to support other formats in the future but
      // for now, let's leave it alone. If we get no result from this call then we already know
      // that there are no proto files to extract, so we can skip the lookup.

      var extractionResult = protoArchiveExtractor.tryExtractProtoFiles(originalPath);

      if (extractionResult.isPresent()) {
        rootPath = extractionResult.get();
      } else {
        return Optional.empty();
      }
    } else {
      rootPath = originalPath;
    }

    try (var stream = Files.walk(rootPath)) {
      var protoFiles = stream
          .filter(ProtoFilePredicates::isProtoFile)
          .peek(protoFile -> log.trace("Found proto file in root {}: {}", rootPath, protoFile))
          .collect(Collectors.toUnmodifiableSet());

      if (protoFiles.isEmpty()) {
        return Optional.empty();
      }

      var listing = ImmutableProtoFileListing
          .builder()
          .addAllProtoFiles(protoFiles)
          .protoFilesRoot(rootPath)
          .originalRoot(originalPath)
          .build();

      return Optional.of(listing);
    }
  }

  private CompletableFuture<Optional<ProtoFileListing>> createProtoFileListingAsync(
      Path originalPath
  ) {
    var completableFuture = new CompletableFuture<Optional<ProtoFileListing>>();
    executorService.submit(() -> {
      try {
        completableFuture.complete(createProtoFileListing(originalPath));
      } catch (Exception ex) {
        completableFuture.completeExceptionally(ex);
      }
    });
    return completableFuture;
  }
}
