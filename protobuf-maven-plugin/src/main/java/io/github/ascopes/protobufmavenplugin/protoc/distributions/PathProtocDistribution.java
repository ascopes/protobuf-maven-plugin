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

import io.github.ascopes.protobufmavenplugin.plexus.KindHint;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/**
 * Model base for a {@code protoc} distribution that is located on the system {@code $PATH}.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Immutable
@KindHint(kind = "path", implementation = PathProtocDistributionBean.class)
@Modifiable
public abstract non-sealed class PathProtocDistribution
    implements ProtocDistribution {

  /**
   * Get the name.
   *
   * <p>Defaults to {@code "protoc"} if unset.
   *
   * @return the name.
   */
  @Default
  public String getName() {
    return "protoc";
  }
}
