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

package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.DependencyResolutionDepth;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;

/**
 * Component that fetches paths for known project dependencies.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
@Named
public final class MavenProjectDependencyPathResolver {

  // Important note: we use the org.apache.maven.artifact.Artifact type here. For actual
  // dependency resolution elsewhere, we use the org.eclipse.aether APIs instead which are
  // almost identical but incompatible with these calls. I have separated this concern
  // out to avoid confusion.

  @Inject
  public MavenProjectDependencyPathResolver() {
    // Nothing to do.
  }

  public Collection<Path> resolveProjectDependencies(
      MavenSession session,
      DependencyResolutionDepth dependencyResolutionDepth
  ) {
    // This assumes the mojo executing this request has specified the correct
    // requiresDependencyCollection and requiresDependencyResolution scopes in
    // the @Mojo annotation. This is far more efficient than what we used to do,
    // which is re-resolve everything from scratch in a more error-prone way.
    //
    // This also avoids needing to call the resolver again directly.
    return session.getCurrentProject().getArtifacts()
        .stream()
        .filter(dependencyResolutionDepth == DependencyResolutionDepth.DIRECT
            ? artifactIsDirectDependency(session)
            : always())
        .map(Artifact::getFile)
        .map(File::toPath)
        .distinct()
        .collect(Collectors.toList());
  }

  private Predicate<Artifact> always() {
    return anything -> true;
  }

  private Predicate<Artifact> artifactIsDirectDependency(
      MavenSession session
  ) {
    var dependencies = session.getCurrentProject().getDependencies();
    return artifact -> dependencies.stream()
        .anyMatch(dependency -> Objects.equals(dependency.getGroupId(), artifact.getGroupId())
            && Objects.equals(dependency.getArtifactId(), artifact.getArtifactId())
            && Objects.equals(dependency.getVersion(), artifact.getVersion())
            && Objects.equals(dependency.getClassifier(), artifact.getClassifier())
            && Objects.equals(dependency.getType(), artifact.getType())
            && Objects.equals(dependency.getScope(), artifact.getScope()));
  }
}
