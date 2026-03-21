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
import java.net.URI;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;

/**
 * Model base for a {@code protoc} distribution that is located at a URI.
 *
 * @author Ashley Scopes
 * @since 5.1.0
 */
@Immutable
@KindHint(kind = "url", implementation = UriProtocDistributionBean.class)
@Modifiable
public abstract non-sealed class UriProtocDistribution implements ProtocDistribution {

  /**
   * Get the URI.
   *
   * @return the URI.
   */
  public abstract URI getUrl();
}
