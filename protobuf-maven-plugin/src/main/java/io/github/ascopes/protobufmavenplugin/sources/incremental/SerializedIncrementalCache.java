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

package io.github.ascopes.protobufmavenplugin.sources.incremental;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.nio.file.Path;
import java.util.Map;
import org.immutables.value.Value.Immutable;

/**
 * Serialized format of the incremental compilation cache.
 *
 * <p>If changing the structure of this, update the {@code SPEC_VERSION} within
 * {@link IncrementalCacheManager} to avoid breaking changes for users.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
@Immutable
@JsonDeserialize(builder = ImmutableSerializedIncrementalCache.Builder.class)
@JsonSerialize(as = ImmutableSerializedIncrementalCache.class)
interface SerializedIncrementalCache {
  Map<Path, String> getDependencies();

  Map<Path, String> getSources();
}
