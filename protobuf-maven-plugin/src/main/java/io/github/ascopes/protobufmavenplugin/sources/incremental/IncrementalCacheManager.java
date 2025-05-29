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
package io.github.ascopes.protobufmavenplugin.sources.incremental;

import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.sources.DescriptorListing;
import io.github.ascopes.protobufmavenplugin.sources.FilesToCompile;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputListing;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.utils.ConcurrentExecutor;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public final class IncrementalCacheManager {

  // If we make breaking changes to the format of the cache, increment this value. This prevents
  // builds failing for users between versions if they do not perform a clean install first.
  private static final String SPEC_VERSION = "3.0";
  private static final Logger log = LoggerFactory.getLogger(IncrementalCacheManager.class);

  private final ConcurrentExecutor concurrentExecutor;
  private final TemporarySpace temporarySpace;
  private final IncrementalCacheSerializer serializedIncrementalCacheSerializer;

  @Inject
  IncrementalCacheManager(
      ConcurrentExecutor concurrentExecutor,
      TemporarySpace temporarySpace,
      IncrementalCacheSerializer serializedIncrementalCacheSerializer
  ) {
    this.concurrentExecutor = concurrentExecutor;
    this.temporarySpace = temporarySpace;
    this.serializedIncrementalCacheSerializer = serializedIncrementalCacheSerializer;
  }

  public void updateIncrementalCache() throws IOException {
    var previousCache = getPreviousIncrementalCachePath();
    var nextCache = getNextIncrementalCachePath();
    if (Files.exists(nextCache)) {
      log.debug("Overwriting incremental build cache at {} with {}", previousCache, nextCache);
      Files.move(nextCache, previousCache, StandardCopyOption.REPLACE_EXISTING);
    } else {
      log.debug("No new incremental cache was created, so nothing will be updated...");
    }
  }

  public FilesToCompile determineSourcesToCompile(
      ProjectInputListing listing
  ) throws IOException {
    // Always update the cache to catch changes in the next builds.
    var nextCache = buildIncrementalCache(listing);
    writeIncrementalCache(getNextIncrementalCachePath(), nextCache);
    var maybePreviousBuildCache = readIncrementalCache(getPreviousIncrementalCachePath());

    // If we lack a cache from a previous build, then we cannot determine what we should compile
    // and what we should ignore, so we'll have to rebuild everything anyway.
    if (maybePreviousBuildCache.isEmpty()) {
      log.info("All sources will be compiled, as no previous build data was detected");
      return FilesToCompile.allOf(listing);
    }

    var previousCache = maybePreviousBuildCache.get();

    // If dependencies change, we should recompile everything so that we can spot any compilation
    // failures that have been created by changes to imported messages.
    if (!previousCache.getProtoDependencies().equals(nextCache.getProtoDependencies())) {
      log.info("Detected a change in dependencies, all sources will be recompiled");
      return FilesToCompile.allOf(listing);
    }

    var protoSourceFilesChanged = nextCache.getProtoSources().keySet()
        .stream()
        .anyMatch(isFileUpdated(IncrementalCache::getProtoSources, previousCache, nextCache));

    var descriptorSourceFilesChanged = nextCache.getDescriptorFiles().keySet()
        .stream()
        .anyMatch(isFileUpdated(IncrementalCache::getDescriptorFiles, previousCache, nextCache));

    if (protoSourceFilesChanged || descriptorSourceFilesChanged) {
      log.info("Detected that source files have changed, all sources will be recompiled.");
      return FilesToCompile.allOf(listing);
    }

    return FilesToCompile.empty();
  }

  private Predicate<Path> isFileUpdated(
      Function<IncrementalCache, Map<Path, String>> cacheAccessor,
      IncrementalCache previousBuildCache,
      IncrementalCache nextBuildCache
  ) {
    return file -> !Objects.equals(
        cacheAccessor.apply(previousBuildCache).get(file),
        cacheAccessor.apply(nextBuildCache).get(file)
    );
  }

  private Optional<IncrementalCache> readIncrementalCache(Path path) throws IOException {
    log.debug("Reading incremental cache in from {}", path);

    try (var reader = Files.newBufferedReader(path)) {
      return Optional.of(serializedIncrementalCacheSerializer.deserialize(reader));
    } catch (NoSuchFileException ex) {
      log.debug("No file found at {}", path);
      return Optional.empty();
    }
  }

  private void writeIncrementalCache(
      Path path,
      IncrementalCache cache
  ) throws IOException {
    log.debug("Writing incremental cache out to {}", path);

    try (var writer = Files.newBufferedWriter(path)) {
      serializedIncrementalCacheSerializer.serialize(cache, writer);
    }
  }

  private IncrementalCache buildIncrementalCache(ProjectInputListing listing) {
    var importFutures = generateProtoFileDigests(listing.getDependencyProtoSources());
    var sourceFutures = generateProtoFileDigests(listing.getCompilableProtoSources());
    var descriptorFutures = generateDescriptorDigests(listing.getCompilableDescriptorFiles());

    var results = Stream
        .of(importFutures, sourceFutures, descriptorFutures)
        .map(stream -> concurrentExecutor.submit(() -> stream
            .collect(concurrentExecutor.awaiting())
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
        // Schedule all concurrent tasks together by using a terminal operation
        .collect(Collectors.toUnmodifiableList())
        .stream()
        .collect(concurrentExecutor.awaiting())
        .iterator();

    return ImmutableIncrementalCache.builder()
        .protoDependencies(results.next())
        .protoSources(results.next())
        .descriptorFiles(results.next())
        .build();
  }

  private Stream<FutureTask<Map.Entry<Path, String>>> generateProtoFileDigests(
      Collection<SourceListing> listings
  ) {
    return listings.stream()
        .map(SourceListing::getSourceFiles)
        .flatMap(Collection::stream)
        .map(this::generateFileDigest);
  }

  private Stream<FutureTask<Map.Entry<Path, String>>> generateDescriptorDigests(
      Collection<DescriptorListing> listings
  ) {
    return listings.stream()
        .map(DescriptorListing::getDescriptorFilePath)
        .map(this::generateFileDigest);
  }

  private FutureTask<Map.Entry<Path, String>> generateFileDigest(Path file) {
    return concurrentExecutor.submit(() -> {
      log.trace("Generating digest for {}", file);
      try (var inputStream = FileUtils.newBufferedInputStream(file)) {
        var digest = Digests.sha512ForStream(inputStream);
        return Map.entry(file, digest);
      }
    });
  }

  private Path getIncrementalCacheRoot() {
    return temporarySpace.createTemporarySpace("incremental-build-cache", SPEC_VERSION);
  }

  private Path getPreviousIncrementalCachePath() {
    return getIncrementalCacheRoot().resolve("previous.json");
  }

  private Path getNextIncrementalCachePath() {
    return getIncrementalCacheRoot().resolve("next.json");
  }
}
