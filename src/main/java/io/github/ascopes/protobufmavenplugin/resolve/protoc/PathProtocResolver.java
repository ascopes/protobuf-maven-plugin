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

package io.github.ascopes.protobufmavenplugin.resolve.protoc;

import io.github.ascopes.protobufmavenplugin.resolve.AbstractPathResolver;

/**
 * Resolver for {@code protoc} that considers any executables in the {@code $PATH} environment
 * variable.
 *
 * <p>This expects the binary to satisfy the following constraints:
 *
 * <ul>
 *   <li>The executable must be in one of the directories on the {@code $PATH}
 *       ({@code %PATH%} on Windows);
 *   <li>On POSIX systems, the binary must be named exactly "{@code protoc}";
 *   <li>On Windows systems, the binary must be named "{@code protoc}", ignoring case
 *       sensitivity, and ignoring any file extension (so "{@code PROTOC.EXE}" would be a direct
 *       match here).
 * </ul>
 *
 * @author Ashley Scopes
 */
public final class PathProtocResolver extends AbstractPathResolver {

  /**
   * Initialise this resolver.
   */
  public PathProtocResolver() {
    // Nothing to do.
  }

  @Override
  protected String binaryName() {
    return "protoc";
  }
}
