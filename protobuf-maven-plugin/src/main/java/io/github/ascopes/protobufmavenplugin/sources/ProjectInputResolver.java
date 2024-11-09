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

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that resolves all protobuf source and import paths for a given plugin invocation,
 * ensuring they are in a location that is accessible by the {@code protoc} binary.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
@Named
public final class ProjectInputResolver {
  private static final Logger log = LoggerFactory.getLogger(ProjectInputResolver.class);

  private final MavenArtifactPathResolver artifactPathResolver;
  private final SourceResolver sourceResolver;

  @Inject
  public ProjectInputResolver(
      MavenArtifactPathResolver artifactPathResolver,
      SourceResolver sourceResolver
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.sourceResolver = sourceResolver;
  }

  public ProjectInputListing resolveProjectInputs(
      GenerationRequest request
  ) throws ResolutionException {
    var compilableSources = resolveCompilableSources(request);
    var imports = resolveImports(request, compilableSources);

    return ImmutableProjectInputListing.builder()
        .compilableSources(compilableSources)
        .imports(imports)
        .build();
  }

  private Collection<SourceListing> resolveCompilableSources(
      GenerationRequest request
  ) throws ResolutionException {
    log.debug("Discovering all compilable protobuf source files");

    var filter = new SourceGlobFilter(request.getIncludes(), request.getExcludes());

    var sourcePathsListings = sourceResolver.resolveSources(
        request.getSourceRoots(),
        filter
    );

    var sourceDependencies = artifactPathResolver.resolveDependencies(
        request.getSourceDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        false,
        request.isFailOnInvalidDependencies()
    );

    var sourceDependencyListings = sourceResolver.resolveSources(
        sourceDependencies,
        filter
    );

    var sourcePaths = concat(sourcePathsListings, sourceDependencyListings);

    var sourceFileCount = sourcePaths.stream()
        .mapToInt(sourcePath -> sourcePath.getSourceProtoFiles().size())
        .sum();

    log.info(
        "Generating source code for {} from {}",
        pluralize(sourceFileCount, "protobuf file"),
        pluralize(sourcePaths.size(), "source file tree")
    );

    return sourcePaths;
  }

  private Collection<SourceListing> resolveImports(
      GenerationRequest request,
      Collection<SourceListing> knownSourceListings
  ) throws ResolutionException {
    var artifactPaths = artifactPathResolver.resolveDependencies(
        request.getImportDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        !request.isIgnoreProjectDependencies(),
        request.isFailOnInvalidDependencies()
    );

    var filter = new SourceGlobFilter();

    var importListings = sourceResolver.resolveSources(
        concat(request.getImportPaths(), artifactPaths),
        filter
    );

    // Use the source paths here as well and use them first to give them precedence. This works
    // around GH-172 where we can end up with different versions on the import and source paths
    // depending on how dependency conflicts arise.
    return Stream.concat(knownSourceListings.stream(), importListings.stream())
        .distinct()
        .collect(Collectors.toUnmodifiableList());
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static <T> List<T> concat(Collection<? extends T>... collections) {
    return Stream.of(collections)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  private static String pluralize(int count, String name) {
    return count == 1
        ? "1 " + name
        : count + " " + name + "s";
  }
}
