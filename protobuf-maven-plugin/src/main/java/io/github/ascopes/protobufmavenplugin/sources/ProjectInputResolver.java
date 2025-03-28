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

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that resolves all protobuf source and import paths for a given plugin invocation,
 * ensuring they are in a location that is accessible by the {@code protoc} binary.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
@Description("Resolves all protobuf sources to compile and/or make importable in the project")
@MojoExecutionScoped
@Named
public final class ProjectInputResolver {
  private static final Logger log = LoggerFactory.getLogger(ProjectInputResolver.class);

  private final MavenArtifactPathResolver artifactPathResolver;
  private final ProtoSourceResolver sourceResolver;

  @Inject
  ProjectInputResolver(
      MavenArtifactPathResolver artifactPathResolver,
      ProtoSourceResolver sourceResolver
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.sourceResolver = sourceResolver;
  }

  public ProjectInputListing resolveProjectInputs(
      GenerationRequest request
  ) throws ResolutionException {
    // XXX: We might want to run these two resolution steps in parallel in the future.
    return ImmutableProjectInputListing.builder()
        .compilableSources(resolveCompilableSources(request))
        .dependencySources(resolveDependencySources(request))
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

    return Stream
        .concat(sourcePathsListings.stream(), sourceDependencyListings.stream())
        .collect(Collectors.toUnmodifiableList());
  }

  private Collection<SourceListing> resolveDependencySources(
      GenerationRequest request
  ) throws ResolutionException {
    var artifactPaths = artifactPathResolver.resolveDependencies(
        request.getImportDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        !request.isIgnoreProjectDependencies(),
        request.isFailOnInvalidDependencies()
    );

    var importPaths = Stream
        .concat(request.getImportPaths().stream(), artifactPaths.stream())
        .collect(Collectors.toUnmodifiableList());

    var filter = new SourceGlobFilter(List.of(), List.of());

    return sourceResolver.resolveSources(importPaths, filter);
  }
}
