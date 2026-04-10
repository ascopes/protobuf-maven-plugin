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

import io.github.ascopes.protobufmavenplugin.plexus.KindHint;
import org.immutables.datatype.Data;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;


/**
 * Implementation independent descriptor for a protoc plugin that can be resolved from the system
 * {@code $PATH}.
 *
 * <p>Path-based plugins can be marked as optional if they should be skipped when the resource is
 * unable to be resolved.
 *
 * @author Ashley Scopes
 * @since 2.0.0
 */
@Data
@Immutable
@KindHint("path")
public abstract non-sealed class PathProtocPlugin implements ProtocPlugin {

  public abstract String getName();

  @Default.Boolean(false)
  public abstract boolean isOptional();

  @Override
  public String toString() {
    return "path:" + getName();
  }
}
