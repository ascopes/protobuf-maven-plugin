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
package io.github.ascopes.protobufmavenplugin.generation;

import java.nio.file.Path;
import java.util.Optional;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProjectHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy for registration of additional artifacts to attach
 * to the Maven project.
 *
 * @since 2.11.0
 */
public final class OutputDescriptorAttachmentRegistrar {
  private static final Logger log = LoggerFactory
      .getLogger(OutputDescriptorAttachmentRegistrar.class);

  private final MavenProjectHelper mavenProjectHelper;
  private final String defaultArtifactType;
  private final @Nullable String defaultArtifactClassifier;

  public OutputDescriptorAttachmentRegistrar(
      MavenProjectHelper mavenProjectHelper,
      String defaultArtifactType,
      @Nullable String defaultArtifactClassifier
  ) {
    this.mavenProjectHelper = mavenProjectHelper;
    this.defaultArtifactType = defaultArtifactType;
    this.defaultArtifactClassifier = defaultArtifactClassifier;
  }

  public void registerAttachedArtifact(
      MavenSession session,
      Path artifactPath,
      @Nullable String artifactType,
      @Nullable String artifactClassifier
  ) {

    var resolvedArtifactType = Optional.ofNullable(artifactType)
        .orElse(defaultArtifactType);
    var resolvedArtifactClassifier = Optional.ofNullable(artifactClassifier)
        .orElse(defaultArtifactClassifier);

    log.info(
        "Attaching \"{}\" to build outputs with type \"{}\" and classifier \"{}\"",
        artifactPath,
        resolvedArtifactType,
        resolvedArtifactClassifier
    );

    mavenProjectHelper.attachArtifact(
        session.getCurrentProject(),
        resolvedArtifactType,
        resolvedArtifactClassifier,
        artifactPath.toFile()
    );
  }
}
