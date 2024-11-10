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

package io.github.ascopes.protobufmavenplugin.sources.incremental;

import static io.github.ascopes.protobufmavenplugin.sources.SourceListing.flattenSourceProtoFiles;
import static java.util.function.Predicate.not;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputListing;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.utils.ConcurrentExecutor;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager that detects situations where incremental compilation may be faster on large codebases.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
@Named
public class IncrementalCacheManager {

  private static final Logger log = LoggerFactory.getLogger(IncrementalCacheManager.class);

  private final ConcurrentExecutor concurrentExecutor;
  private final TemporarySpace temporarySpace;
  private final ObjectMapper objectMapper;

  @Inject
  public IncrementalCacheManager(
      ConcurrentExecutor concurrentExecutor,
      TemporarySpace temporarySpace
  ) {
    this.concurrentExecutor = concurrentExecutor;
    this.temporarySpace = temporarySpace;

    objectMapper = new JsonMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(new JavaTimeModule())
        .registerModule(new SerializedIncrementalCacheModule());
  }

  public void updateIncrementalCache() throws IOException {
    var previousCache = getPreviousIncrementalCachePath();
    var newCache = getNewIncrementalCachePath();
    if (Files.exists(newCache)) {
      log.debug("Overwriting incremental compilation cache at {} with {}", previousCache, newCache);
      Files.move(newCache, previousCache, StandardCopyOption.REPLACE_EXISTING);
    } else {
      log.debug("No new incremental cache was created, so nothing will be updated...");
    }
  }

  public Collection<Path> determineSourcesToCompile(
      ProjectInputListing listing
  ) throws IOException {
    // TODO(ascopes): we can eventually demote this to debug logging once the feature is stable.
    var startTime = System.nanoTime();
    var sourcesToCompile = determineSourcesToCompileUntimed(listing);
    var timeTaken = (System.nanoTime() - startTime) / 1_000_000L;
    log.info("Detected {} sources to compile in {}ms", sourcesToCompile.size(), timeTaken);
    return sourcesToCompile;
  }

  private Collection<Path> determineSourcesToCompileUntimed(
      ProjectInputListing listing
  ) throws IOException {
    // Always update the cache to catch changes in the next builds.
    var newBuildCache = buildIncrementalCache(listing);
    writeIncrementalCache(getNewIncrementalCachePath(), newBuildCache);
    var maybePreviousBuildCache = readIncrementalCache(getPreviousIncrementalCachePath());

    // If we lack a cache from a previous build, then we cannot determine what we should compile
    // and what we should ignore, so we'll have to rebuild everything anyway.
    if (maybePreviousBuildCache.isEmpty()) {
      log.info("All sources will be compiled, as no previous build data was detected");
      return flattenSourceProtoFiles(listing.getCompilableSources());
    }

    var previousBuildCache = maybePreviousBuildCache.get();

    // If dependencies change, we should recompile everything so that we can spot any compilation
    // failures that have been created by changes to imported messages.
    if (!previousBuildCache.getDependencies().equals(newBuildCache.getDependencies())) {
      log.info("Detected a change in dependencies, all sources will be recompiled");
      return flattenSourceProtoFiles(listing.getCompilableSources());
    }

    var filesDeletedSinceLastBuild = previousBuildCache.getSources().keySet()
        .stream()
        .anyMatch(not(newBuildCache.getSources().keySet()::contains));

    // If any sources were deleted, we should rebuild everything, as those files being deleted may
    // have caused a compilation failure.
    if (filesDeletedSinceLastBuild) {
      log.info("Detected that source files have been deleted, all sources will be recompiled");
      return flattenSourceProtoFiles(listing.getCompilableSources());
    }

    return newBuildCache.getSources().keySet()
        .stream()
        .filter(file -> !Objects.equals(
            newBuildCache.getSources().get(file),
            previousBuildCache.getSources().get(file)
        ))
        .collect(Collectors.toUnmodifiableSet());
  }

  private Optional<SerializedIncrementalCache> readIncrementalCache(Path path) throws IOException {
    log.debug("Reading incremental cache in from {}", path);

    try (var inputStream = Files.newInputStream(path)) {
      return Optional.of(objectMapper.readValue(inputStream, SerializedIncrementalCache.class));
    } catch (NoSuchFileException ex) {
      log.debug("No file found at {}", path);
      return Optional.empty();
    }
  }

  private void writeIncrementalCache(
      Path path,
      SerializedIncrementalCache cache
  ) throws IOException {
    log.debug("Writing incremental cache out to {}", path);

    try (var outputStream = Files.newOutputStream(path)) {
      objectMapper.writeValue(outputStream, cache);
    }
  }

  private SerializedIncrementalCache buildIncrementalCache(ProjectInputListing listing) {
    var dependencyDigests = listing.getDependencySources().stream()
        .map(SourceListing::getSourceProtoFiles)
        .flatMap(Collection::stream)
        .map(this::createSerializedFileDigestAsync)
        .collect(concurrentExecutor.awaiting())
        .stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    var sourceDigests = listing.getCompilableSources().stream()
        .map(SourceListing::getSourceProtoFiles)
        .flatMap(Collection::stream)
        .map(this::createSerializedFileDigestAsync)
        .collect(concurrentExecutor.awaiting())
        .stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    return ImmutableSerializedIncrementalCache.builder()
        .generatedAt(OffsetDateTime.now())
        .dependencies(dependencyDigests)
        .sources(sourceDigests)
        .build();
  }

  private FutureTask<Entry<Path, String>> createSerializedFileDigestAsync(Path file) {
    return concurrentExecutor.submit(() -> {
      log.trace("Generating digest for {}", file);
      try (var inputStream = Files.newInputStream(file)) {
        var digest = Digests.sha512ForStream(inputStream);
        return new SimpleImmutableEntry<>(file, digest);
      }
    });
  }

  private Path getIncrementalCacheRoot() {
    return temporarySpace.createTemporarySpace("incremental");
  }

  private Path getPreviousIncrementalCachePath() {
    return getIncrementalCacheRoot().resolve("previous-build-cache.json");
  }

  private Path getNewIncrementalCachePath() {
    return getIncrementalCacheRoot().resolve("current-build-cache.json");
  }
}
