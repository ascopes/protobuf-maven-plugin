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
package io.github.ascopes.protobufmavenplugin.sources;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.immutables.value.Value.Immutable;

/**
 * Listing for a root containing proto files.
 *
 * @author Ashley Scopes
 */
@Immutable
public interface SourceListing {

  Path getSourceRoot();

  Set<Path> getSourceProtoFiles();

  static Collection<Path> flattenSourceProtoFiles(Collection<SourceListing> listings) {
    return listings.stream()
        .map(SourceListing::getSourceProtoFiles)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }
}
