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
package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves proto files or proto file roots, including those in JAR files.
 *
 * <p>Supports discovery of proto files within a JAR archive by extracting them to a temporary
 * location if candidates are found upon inspecting the archive.
 *
 * @author Ashley Scopes
 */
@Named
public class ProtoFileResolver implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ProtoFileResolver.class);

  private final ExecutorService executorService;
  private final FileSystemProvider jarFileSystemProvider;

  @Inject
  public ProtoFileResolver() {
    executorService = new ForkJoinPool(4 * Runtime.getRuntime().availableProcessors());
    jarFileSystemProvider = FileSystemProvider.installedProviders()
        .stream()
        .filter(provider -> provider.getScheme().equalsIgnoreCase("jar"))
        .findFirst()
        .orElseThrow();
  }

  @Override
  @PreDestroy
  public void close() {
    executorService.shutdown();
  }

  /**
   * Attempt to find any roots containing proto files.
   *
   * <p>If any paths correspond to JAR files rather than directories, then they will first be
   * extracted into a temporary location within the build directory in the given Maven session. This
   * allows protoc to be able to see the files later. The corresponding extracted directory root
   * will be returned instead in this case.
   *
   * <p>IO operations will be performed concurrently in a thread pool to speed up the operation.
   *
   * @param session the current Maven session.
   * @param roots   the paths to search.
   * @return the collection of paths containing proto roots, on the root file system.
   * @throws ResolutionException if resolution fails due to an IO issue.
   */
  public Collection<Path> findProtoFileRoots(
      MavenSession session,
      Collection<Path> roots
  ) throws ResolutionException {
    log.debug("Discovering roots containing proto files in {} location(s)", roots.size());

    var futures = roots
        .stream()
        .map(root -> submit(() -> findProtoFilesRootSync(session, root)))
        .iterator();

    var matchingRoots = new ArrayList<Path>();

    while (futures.hasNext()) {
      try {
        futures.next().join().ifPresent(matchingRoots::add);
      } catch (CompletionException ex) {
        futures.forEachRemaining(f -> f.cancel(true));
        throw new ResolutionException("Failed to discover proto file roots", ex);
      }
    }

    return matchingRoots;
  }

  /**
   * Attempt to find all proto files in the given directory.
   *
   * <p>Passing any non-directories here will result in an exception being raised eagerly. JARs
   * should have been extracted via the {@link #findProtoFileRoots} check prior to this call.
   *
   * <p>IO operations will be performed concurrently in a thread pool to speed up the operation.
   *
   * @param roots the directories to check.
   * @return all proto files that were found.
   * @throws ResolutionException if an IO exception occurs, or if any roots are not directories.
   */
  public Collection<Path> findProtoFiles(Collection<Path> roots) throws ResolutionException {
    log.debug("Discovering proto files in {} location(s)", roots.size());

    var futures = roots
        .stream()
        .map(root -> submit(() -> findProtoFilesSync(root)))
        .iterator();

    var matchingFiles = new ArrayList<Path>();

    while (futures.hasNext()) {
      try {
        matchingFiles.addAll(futures.next().join());
      } catch (CompletionException ex) {
        futures.forEachRemaining(f -> f.cancel(true));
        throw new ResolutionException("Failed to discover proto files", ex);
      }
    }

    return matchingFiles;
  }

  private Optional<Path> findProtoFilesRootSync(
      MavenSession session,
      Path directoryOrJar
  ) throws IOException {
    if (Files.isDirectory(directoryOrJar)) {
      log.debug("Checking if directory '{}' contains proto files", directoryOrJar);
      try (var stream = Files.walk(directoryOrJar)) {
        return stream.anyMatch(this::isProtoFile)
            ? Optional.of(directoryOrJar)
            : Optional.empty();
      }
    }

    if (Files.isRegularFile(directoryOrJar)) {
      return extractJarIfContainsProtoFiles(session, directoryOrJar);
    }

    return Optional.empty();
  }

  private Collection<Path> findProtoFilesSync(Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      throw new NotDirectoryException(directory.toString());
    }

    log.debug("Looking for proto files within '{}'", directory);
    try (var stream = Files.walk(directory)) {
      return stream.filter(this::isProtoFile).collect(Collectors.toUnmodifiableList());
    }
  }

  private Optional<Path> extractJarIfContainsProtoFiles(
      MavenSession session,
      Path jar
  ) throws IOException {
    log.debug("Inspecting the contents of '{}'", jar);

    var newRoot = Path.of(session.getCurrentProject().getBuild().getDirectory())
        .resolve("protobuf-maven-plugin")
        .resolve("extracted")
        .resolve(uniqueFileName(jar));

    var created = new AtomicBoolean(false);

    try (var virtualFileSystem = jarFileSystemProvider.newFileSystem(jar, Map.of())) {
      var virtualRoot = virtualFileSystem.getRootDirectories().iterator().next();

      Files.walkFileTree(virtualRoot, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (isProtoFile(file)) {
            var newFile = changeRoot(newRoot, virtualRoot, file);

            log.trace(
                "Extracting {} to root file system at {}",
                file.toUri(),
                newFile
            );

            Files.createDirectories(newFile.getParent());
            Files.copy(file, newFile);
            created.set(true);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }

    if (created.get()) {
      log.info("Extracted contents of {} to {}, since it contained protobuf sources", jar, newRoot);
      return Optional.of(newRoot);
    }

    return Optional.empty();
  }

  private Path changeRoot(Path newRoot, Path oldRoot, Path absolutePath) {
    var path = newRoot;

    for (var part : oldRoot.relativize(absolutePath)) {
      path = path.resolve(part.toString());
    }

    return path;
  }

  private String uniqueFileName(Path path) {
    try {
      var hash = MessageDigest.getInstance("SHA-1").digest(path.toString().getBytes());
      // Removal of padding is important as protoc will interpret the '=' character in a special
      // way if not.
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private boolean isProtoFile(Path file) {
    return Files.isRegularFile(file) && FileUtils
        .getFileExtension(file)
        .filter(".proto"::endsWith)
        .isPresent();
  }

  private <T> CompletableFuture<T> submit(Callable<T> call) {
    var completableFuture = new CompletableFuture<T>();
    executorService.submit(() -> {
      try {
        completableFuture.complete(call.call());
      } catch (Throwable throwable) {
        completableFuture.completeExceptionally(throwable);
      }
    });
    return completableFuture;
  }
}
