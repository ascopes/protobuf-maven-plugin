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
package io.github.ascopes.protobufmavenplugin.protoc.distributions;

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.plexus.KindHint;
import java.util.List;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Model base for a {@code protoc} distribution that is resolved from a Maven
 * repository, and is a pure-Java application.
 *
 * <p>This is experimental.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@KindHint(kind = "jvm-maven", implementation = JvmMavenProtocDistributionBean.class)
@Modifiable
public abstract non-sealed class JvmMavenProtocDistribution
    extends MavenArtifact
    implements ProtocDistribution {

  // Version is never null here as we do not infer from dependency management.
  @Override
  public abstract @NonNull String getVersion();

  // Arguments are always passed first before any standard protoc arguments are passed in.
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
