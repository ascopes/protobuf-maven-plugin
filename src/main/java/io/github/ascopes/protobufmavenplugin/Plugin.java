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
package io.github.ascopes.protobufmavenplugin;

import java.util.Optional;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.jspecify.annotations.Nullable;

/**
 * Declaration of a plugin.
 *
 * @author Ashley Scopes
 */
public final class Plugin {

  @Parameter
  private @Nullable DefaultArtifactCoordinate artifact;

  @Parameter
  private @Nullable String executableName;

  public Plugin() {
  }

  public Optional<ArtifactCoordinate> getArtifact() {
    return Optional.ofNullable(artifact);
  }

  public Optional<String> getExecutableName() {
    return Optional.ofNullable(executableName);
  }
}
