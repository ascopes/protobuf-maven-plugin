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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts {@code proto} files from compatible archives and writes them to a temporary location.
 *
 * <p>This is performed concurrently to reduce build times.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoArchiveExtractor implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ProtoArchiveExtractor.class);

  private final ExecutorService executorService;
  private final MavenProject mavenProject;
  private final ProtoFileResolver protoFileResolver;
  private final FileSystemProvider jarFileSystemProvider;

  @Inject
  public ProtoArchiveExtractor(MavenProject mavenProject, ProtoFileResolver protoFileResolver) {
    executorService = Executors.newWorkStealingPool(determineConcurrency());

    this.mavenProject = mavenProject;
    this.protoFileResolver = protoFileResolver;

    jarFileSystemProvider = FileSystemProvider.installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
        .findFirst()
        .orElseThrow();
  }

  @PreDestroy
  @Override
  public void close() {
    executorService.shutdown();
  }

  public Collection<Path> extractArchiveContents(Collection<Path> archivePaths) throws IOException {
    var futures = archivePaths
        .stream()
        .map(this::extractArchiveContentsAsync)
        .collect(Collectors.toUnmodifiableList());

    var paths = new ArrayList<Path>();
    var exceptions = new ArrayList<Throwable>();

    for (var future : futures) {
      try {
        future.join().ifPresent(paths::add);
      } catch (CompletionException ex) {
        exceptions.add(ex);
      }
    }

    if (!exceptions.isEmpty()) {
      var causeIterator = exceptions.iterator();
      var ex = new IOException("Failed to extract archives", causeIterator.next());

      while (causeIterator.hasNext()) {
        ex.addSuppressed(causeIterator.next());
      }

      throw ex;
    }

    return paths;
  }

  private CompletableFuture<Optional<Path>> extractArchiveContentsAsync(Path archivePath) {
    var future = new CompletableFuture<Optional<Path>>();
    var internalFuture = executorService.submit(() -> {
      try {
        future.complete(extractArchiveContents(archivePath));
      } catch (Throwable ex) {
        future.completeExceptionally(ex);
      }
    });

    // If we cancel the completable future, then ensure we cancel the actual internal future
    // running the task as well.
    future.whenComplete((result, ex) -> internalFuture.cancel(true));
    return future;
  }

  private Optional<Path> extractArchiveContents(Path archivePath) throws IOException {
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

  private static int determineConcurrency() {
    return Runtime.getRuntime().availableProcessors() * 4;
  }
}
