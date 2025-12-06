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

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.plexus.KindHint;
import java.net.URI;
import java.util.Optional;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.Nullable;


/**
 * Implementation independent descriptor for a {@code protoc}
 * distribution that can be resolved from a URI.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@KindHint(kind = "url", implementation = UriProtocDistributionBean.class)
@Modifiable
public abstract non-sealed class UriProtocDistribution
    implements ProtocDistribution {

  public abstract URI getUrl();

  @Default
  public @Nullable Digest getDigest() {
    return null;
  }

  @Override
  public String toString() {
    var sb = new StringBuilder()
        .append(getUrl());

    Optional.ofNullable(getDigest())
        .map(Digest::toString)
        .map("#digest="::concat)
        .ifPresent(sb::append);

    return sb.toString();
  }
}

