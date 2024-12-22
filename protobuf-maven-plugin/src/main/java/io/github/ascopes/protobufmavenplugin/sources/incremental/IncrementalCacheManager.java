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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.FutureTask;
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
public class IncrementalCacheManager {

  // If we make breaking changes to the format of the cache, increment this value. This prevents
  // builds failing for users between versions if they do not perform a clean install first.
  private static final String SPEC_VERSION = "1.1";
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
        .registerModule(new SerializedIncrementalCacheModule());
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

  public Collection<Path> determineSourcesToCompile(
      ProjectInputListing listing
  ) throws IOException {
    // Always update the cache to catch changes in the next builds.
    var nextBuildCache = buildIncrementalCache(listing);
    writeIncrementalCache(getNextIncrementalCachePath(), nextBuildCache);
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
    if (!previousBuildCache.getDependencies().equals(nextBuildCache.getDependencies())) {
      log.info("Detected a change in dependencies, all sources will be recompiled");
      return flattenSourceProtoFiles(listing.getCompilableSources());
    }

    var sourceFilesChanged = nextBuildCache.getSources().keySet()
        .stream()
        .filter(isSourceFileDifferent(previousBuildCache, nextBuildCache))
        .findAny()
        .isPresent();

    if (sourceFilesChanged) {
      log.info("Detected that source files have changed, all sources will be recompiled.");
      return flattenSourceProtoFiles(listing.getCompilableSources());
    }

    return List.of();
  }

  private Predicate<Path> isSourceFileDifferent(
      SerializedIncrementalCache previousBuildCache,
      SerializedIncrementalCache nextBuildCache
  ) {
    return file -> !Objects.equals(
        previousBuildCache.getSources().get(file),
        nextBuildCache.getSources().get(file)
    );
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
    // Done this way for now to propagate errors correctly. Probably worth refactoring in the future
    // to be less of a hack?
    var results = Stream.of(listing.getDependencySources(), listing.getCompilableSources())
        .map(this::createSerializedFileDigestsAsync)
        .collect(concurrentExecutor.awaiting())
        .iterator();

    return ImmutableSerializedIncrementalCache.builder()
        .dependencies(results.next())
        .sources(results.next())
        .build();
  }

  private FutureTask<Map<Path, String>> createSerializedFileDigestsAsync(
      Collection<SourceListing> listings
  ) {
    return concurrentExecutor.submit(() -> listings.stream()
        .map(SourceListing::getSourceProtoFiles)
        .flatMap(Collection::stream)
        .map(this::createSerializedFileDigestAsync)
        .collect(concurrentExecutor.awaiting())
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private FutureTask<Map.Entry<Path, String>> createSerializedFileDigestAsync(Path file) {
    return concurrentExecutor.submit(() -> {
      log.trace("Generating digest for {}", file);
      try (var inputStream = Files.newInputStream(file)) {
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
