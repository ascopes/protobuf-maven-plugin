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
package io.github.ascopes.protobufmavenplugin.fixtures;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenExclusion;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.quality.Strictness;

/**
 * Fixtures for configuring objects related to dependencies across the project test suite.
 *
 * @author Ashley Scopes
 */
@SuppressWarnings("SameParameterValue")
public final class DependencyFixtures {

  private DependencyFixtures() {
    // Static-only class.
  }

  public static io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency pmpDependency(
      String groupId,
      String artifactId,
      String version,
      @Nullable String classifier,
      @Nullable String type
  ) {
    return pmpDependency(
        groupId,
        artifactId,
        version,
        classifier,
        type,
        DependencyResolutionDepth.TRANSITIVE
    );
  }

  public static io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency pmpDependency(
      String groupId,
      String artifactId,
      String version,
      @Nullable String classifier,
      @Nullable String type,
      @Nullable DependencyResolutionDepth dependencyResolutionDepth,
      MavenExclusion... exclusions
  ) {
    var artifact = mock(
        io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency.class,
        defaultSettings("some protobuf maven plugin dependency")
    );
    when(artifact.getGroupId()).thenReturn(groupId);
    when(artifact.getArtifactId()).thenReturn(artifactId);
    when(artifact.getVersion()).thenReturn(version);
    when(artifact.getType()).thenReturn(type);
    when(artifact.getClassifier()).thenReturn(classifier);
    when(artifact.getDependencyResolutionDepth()).thenReturn(dependencyResolutionDepth);
    when(artifact.getExclusions()).thenReturn(Set.of(exclusions));
    return artifact;
  }

  public static io.github.ascopes.protobufmavenplugin.dependencies.MavenExclusion pmpExclusion(
      String groupId,
      String artifactId,
      @Nullable String classifier,
      @Nullable String extension
  ) {
    // Kind of gross that we have to return MavenExclusionBean here but Maven forces our hand
    // due to not being able to cope with interface types.
    var exclusion = mock(
        io.github.ascopes.protobufmavenplugin.dependencies.MavenExclusion.class,
        defaultSettings("some protobuf maven plugin exclusion")
    );
    when(exclusion.getGroupId()).thenReturn(groupId);
    when(exclusion.getArtifactId()).thenReturn(artifactId);
    when(exclusion.getClassifier()).thenReturn(classifier);
    when(exclusion.getType()).thenReturn(extension);
    return exclusion;
  }

  public static org.apache.maven.artifact.Artifact mavenArtifact(
      String groupId,
      String artifactId,
      String version,
      @Nullable String classifier,
      @Nullable String type
  ) {
    var artifact = mock(
        org.apache.maven.artifact.Artifact.class,
        defaultSettings("some maven artifact")
    );
    when(artifact.getGroupId()).thenReturn(groupId);
    when(artifact.getArtifactId()).thenReturn(artifactId);
    when(artifact.getVersion()).thenReturn(version);
    when(artifact.getType()).thenReturn(type);
    when(artifact.getClassifier()).thenReturn(classifier);
    return artifact;
  }

  public static org.apache.maven.model.Dependency mavenDependency() {
    return mavenDependency(
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString()
    );
  }

  public static org.apache.maven.model.Dependency mavenDependency(
      String groupId,
      String artifactId,
      @Nullable String version,
      @Nullable String type,
      @Nullable String classifier
  ) {
    var dependency = mock(
        org.apache.maven.model.Dependency.class,
        defaultSettings("some maven dependency")
    );
    when(dependency.getGroupId()).thenReturn(groupId);
    when(dependency.getArtifactId()).thenReturn(artifactId);
    when(dependency.getVersion()).thenReturn(version);
    when(dependency.getType()).thenReturn(type);
    when(dependency.getClassifier()).thenReturn(classifier);
    return dependency;
  }

  public static org.eclipse.aether.artifact.ArtifactType eclipseArtifactType(
      String extension,
      String classifier
  ) {
    var artifactType = mock(org.eclipse.aether.artifact.ArtifactType.class);
    when(artifactType.getExtension()).thenReturn(extension);
    when(artifactType.getClassifier()).thenReturn(classifier);
    return artifactType;
  }

  public static org.eclipse.aether.artifact.Artifact eclipseArtifact() {
    return eclipseArtifact(
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString()
    );
  }

  public static org.eclipse.aether.artifact.Artifact eclipseArtifact(
      String groupId,
      String artifactId,
      @Nullable String version,
      @Nullable String classifier,
      @Nullable String extension
  ) {
    var artifact = mock(
        org.eclipse.aether.artifact.Artifact.class,
        defaultSettings("some artifact")
    );
    when(artifact.getGroupId()).thenReturn(groupId);
    when(artifact.getArtifactId()).thenReturn(artifactId);
    when(artifact.getClassifier()).thenReturn(classifier);
    when(artifact.getExtension()).thenReturn(extension);
    when(artifact.getVersion()).thenReturn(version);
    return artifact;
  }

  public static org.eclipse.aether.graph.Dependency eclipseDependency() {
    return eclipseDependency(
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString(),
        someBasicString(),
        false
    );
  }

  public static org.eclipse.aether.graph.Dependency eclipseDependency(
      String groupId,
      String artifactId,
      @Nullable String version,
      @Nullable String classifier,
      @Nullable String extension,
      @Nullable String scope,
      @Nullable Boolean optional,
      org.eclipse.aether.graph.Exclusion... exclusions
  ) {
    var dependency = mock(
        org.eclipse.aether.graph.Dependency.class,
        defaultSettings("some dependency")
    );
    var artifact = eclipseArtifact(groupId, artifactId, version, classifier, extension);

    when(dependency.getArtifact())
        .thenReturn(artifact);
    when(dependency.getScope())
        .thenReturn(scope);
    when(dependency.getOptional())
        .thenReturn(optional);
    when(dependency.getExclusions())
        .thenReturn(Set.of(exclusions));
    return dependency;
  }

  public static org.eclipse.aether.repository.RemoteRepository remoteRepository() {
    return mock(defaultSettings("some repository"));
  }

  private static MockSettings defaultSettings(String name) {
    return withSettings()
        .defaultAnswer(Answers.RETURNS_DEEP_STUBS)
        .name(name)
        .strictness(Strictness.LENIENT);
  }
}
