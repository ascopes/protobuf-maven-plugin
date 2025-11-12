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
package io.github.ascopes.protobufmavenplugin.protoc.targets;

import io.github.ascopes.protobufmavenplugin.plugins.ResolvedProtocPlugin;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

/**
 * Base model for a {@code protoc} target that calls a binary plugin.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
@Immutable
public interface PluginProtocTarget extends ProtocTarget {
  ResolvedProtocPlugin getPlugin();

  @Derived
  @Override
  default int getOrder() {
    return getPlugin().getOrder();
  }
}
