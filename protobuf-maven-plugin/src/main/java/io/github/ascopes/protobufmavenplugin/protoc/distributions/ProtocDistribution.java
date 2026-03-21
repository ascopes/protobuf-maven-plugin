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

import io.github.ascopes.protobufmavenplugin.plexus.FromString;
import java.net.URI;

/**
 * Base interface for a {@code protoc} distribution.
 *
 * @author Ashley Scopes
 * @since 5.1.0
 */
public sealed interface ProtocDistribution
    permits BinaryMavenProtocDistribution,
            PathProtocDistribution,
            UriProtocDistribution {

  @FromString
  static ProtocDistribution fromString(String value) {
    if (value.contains(":")) {
      return ImmutableUriProtocDistribution.builder()
          .url(URI.create(value))
          .build();
    }

    if (value.equalsIgnoreCase("PATH")) {
      return ImmutablePathProtocDistribution.builder().build();
    }

    return ImmutableBinaryMavenProtocDistribution.builder()
         .version(value)
         .build();
  }
}
