/*
 * Copyright (C) 2023, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.resolver;

import java.nio.file.Path;

/**
 * Base interface for a component that can resolve a {@code protoc} executable.
 *
 * @author Ashley Scopes
 */
public interface ProtocResolver {

  /**
   * Determine the path to the {@code protoc} executable.
   *
   * @return the path to the {@code protoc} executable.
   * @throws ProtocResolutionException if resolution fails for any reason.
   */
  Path resolveProtoc() throws ProtocResolutionException;
}
