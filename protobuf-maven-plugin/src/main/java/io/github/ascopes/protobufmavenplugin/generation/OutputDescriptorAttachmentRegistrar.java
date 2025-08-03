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

import static java.util.Objects.requireNonNullElse;

import java.nio.file.Path;
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
  private final String defaultType;
  private final @Nullable String defaultClassifier;

  public OutputDescriptorAttachmentRegistrar(
      MavenProjectHelper mavenProjectHelper,
      String defaultType,
      @Nullable String defaultClassifier
  ) {
    this.mavenProjectHelper = mavenProjectHelper;
    this.defaultType = defaultType;
    this.defaultClassifier = defaultClassifier;
  }

  public void registerAttachedArtifact(
      MavenSession session,
      Path path,
      @Nullable String type,
      @Nullable String classifier
  ) {

    var type = requireNonNullElse(type, defaultType);
    var classifier = requireNonNullElse(classifier, defaultClassifier);
    var project = session.getCurrentProject();
    var file = path.toFile();

    log.info(
        "Attaching \"{}\" to build outputs with type \"{}\" and classifier \"{}\"",
        path,
        type,
        classifier
    );

    mavenProjectHelper.attachArtifact(project, type, classifier, file);
  }
}
