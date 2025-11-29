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
package io.github.ascopes.protobufmavenplugin.protoc.dists;

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.NonNull;


/**
 * Implementation independent descriptor for a {@code protoc} distribution that can be resolved
 * from a Maven repository, and is bundled as a platform executable artifact.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Modifiable
public abstract non-sealed class BinaryMavenProtocDistribution
    extends MavenArtifact
    implements ProtocDistribution {

  // Version is never null here as we do not infer from dependency management.
  @Override
  public abstract @NonNull String getVersion();

  // Must be provided to keep immutables happy.
  @Override
  public String toString() {
    return super.toString();
  }
}
