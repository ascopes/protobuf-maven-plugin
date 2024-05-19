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

package io.github.ascopes.protobufmavenplugin.plugins;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.Nullable;

/**
 * Implementation independent descriptor for a protoc plugin that can be resolved from a Maven
 * repository.
 *
 * @author Ashley Scopes
 * @since 2.0.0
 */
@Immutable
@Modifiable
public abstract class MavenProtocPlugin implements MavenArtifact, ProtocPlugin {

  @Derived
  @Nullable
  @Override
  public DependencyResolutionDepth getDependencyResolutionDepth() {
    // We never allow this to be specified for protoc plugins.
    return null;
  }
}
