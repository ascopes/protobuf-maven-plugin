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
package io.github.ascopes.protobufmavenplugin.plugins;

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.plexus.KindHint;
import java.util.List;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


/**
 * Implementation independent descriptor for a protoc plugin that can be resolved from a Maven
 * repository.
 *
 * @author Ashley Scopes
 * @since 2.0.0
 */
@Modifiable
@KindHint(kind = "jvm-maven", implementation = JvmMavenProtocPluginBean.class)
public abstract non-sealed class JvmMavenProtocPlugin
    extends MavenArtifact
    implements ProtocPlugin {

  // Version is never null here as we do not infer from dependency management.
  @Override
  public abstract @NonNull String getVersion();

  public abstract @Nullable List<String> getJvmArgs();

  public abstract @Nullable List<String> getJvmConfigArgs();

  // Null if unset or inferred from MANIFEST.MF.
  public abstract @Nullable String getMainClass();

  // Must be provided to keep immutables happy.
  @Override
  public String toString() {
    return super.toString();
  }
}
