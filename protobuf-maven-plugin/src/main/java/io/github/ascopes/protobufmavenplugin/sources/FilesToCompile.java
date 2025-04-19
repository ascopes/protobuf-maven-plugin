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
import java.util.List;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

/**
 * Collection of sources to compile which can be passed to protoc. This is derived from zero or more
 * source listings.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
@Immutable
public interface FilesToCompile {

  Collection<Path> getProtoSources();

  Collection<String> getDescriptorFiles();

  @Derived
  default boolean isEmpty() {
    return getProtoSources().isEmpty() && getDescriptorFiles().isEmpty();
  }

  /**
   * Produce a {@link FilesToCompile} containing all the proto sources and
   * descriptor files from the given {@link ProjectInputListing}.
   *
   * @return the files to compile.
   */
  static FilesToCompile allOf(ProjectInputListing listing) {
    return ImmutableFilesToCompile.builder()
        .descriptorFiles(DescriptorListing.flatten(listing.getCompilableDescriptorFiles()))
        .protoSources(SourceListing.flatten(listing.getCompilableProtoSources()))
        .build();
  }

  /**
   * Produce a {@link FilesToCompile} that is totally empty.
   *
   * @return the files to compile.
   */
  static FilesToCompile empty() {
    return ImmutableFilesToCompile.builder()
        .descriptorFiles(List.of())
        .protoSources(List.of())
        .build();
  }
}
