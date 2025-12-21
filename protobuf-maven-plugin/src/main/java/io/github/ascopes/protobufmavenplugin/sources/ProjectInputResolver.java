/*
 * Copyright (C) 2023 Ashley Scopes
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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import io.github.ascopes.protobufmavenplugin.sources.filter.FileFilter;
import io.github.ascopes.protobufmavenplugin.sources.filter.IncludesExcludesGlobFilter;
import io.github.ascopes.protobufmavenplugin.sources.filter.ProtoFileFilter;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.util.Collection;
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
    var filter = new IncludesExcludesGlobFilter(request.getIncludes(), request.getExcludes());

    var listing = ImmutableProjectInputListing.builder()
        .compilableDescriptorFiles(resolveCompilableDescriptorSources(request, filter))
        .compilableProtoSources(resolveCompilableProtoSources(request, filter))
        .dependencyProtoSources(resolveDependencyProtoSources(request))
        .build();

    log.trace("Created project input listing \"{}\"", listing);
    return listing;
  }

  private Collection<SourceListing> resolveCompilableProtoSources(
      GenerationRequest request,
      FileFilter filter
  ) throws ResolutionException {
    log.debug("Discovering all compilable protobuf source files");

    filter = new ProtoFileFilter().and(filter);

    var sourcePathsListings = sourceResolver.resolveSources(
        request.getSourceDirectories(),
        filter
    );

    var sourceDependencies = artifactPathResolver.resolveDependencies(
        request.getSourceDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        false
    );

    var sourceDependencyListings = sourceResolver.resolveSources(
        sourceDependencies,
        filter
    );

    return Stream
        .concat(sourcePathsListings.stream(), sourceDependencyListings.stream())
        .distinct()
        .toList();
  }

  private Collection<SourceListing> resolveDependencyProtoSources(
      GenerationRequest request
  ) throws ResolutionException {
    // We purposely do not filter by includes/excludes on the request
    // here as we still want everything on the proto path to be visible,
    // even if we do not generate code for it.
    var filter = new ProtoFileFilter();

    var artifactPaths = artifactPathResolver.resolveDependencies(
        request.getImportDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        !request.isIgnoreProjectDependencies()
    );

    var importPaths = Stream
        .concat(request.getImportPaths().stream(), artifactPaths.stream())
        .distinct()
        .toList();

    return sourceResolver.resolveSources(importPaths, filter);
  }

  private Collection<DescriptorListing> resolveCompilableDescriptorSources(
      GenerationRequest request,
      FileFilter filter
  ) throws ResolutionException {
    // We explicitly do not filter by being a valid proto file for descriptors as we do not use
    // a physical file system to perform our checks, just string paths, and extensions do not
    // have to be present in the descriptor file names.

    var artifactPaths = artifactPathResolver.resolveDependencies(
        request.getSourceDescriptorDependencies(),
        DependencyResolutionDepth.DIRECT,
        request.getDependencyScopes(),
        false
    );

    var descriptorFilePaths = Stream
        .concat(request.getSourceDescriptorPaths().stream(), artifactPaths.stream())
        .distinct()
        .toList();

    return sourceResolver.resolveDescriptors(descriptorFilePaths, filter);
  }
}
