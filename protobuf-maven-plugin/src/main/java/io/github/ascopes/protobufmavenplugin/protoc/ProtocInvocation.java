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
package io.github.ascopes.protobufmavenplugin.protoc;

import io.github.ascopes.protobufmavenplugin.protoc.targets.ProtocTarget;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.immutables.value.Value.Immutable;
import org.jspecify.annotations.Nullable;

/**
 * Model that holds information about the exact {@code protoc} invocation to perform,
 * after all paths have been resolved.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
@Immutable
public interface ProtocInvocation {

  Path getProtocPath();

  boolean isFatalWarnings();

  List<String> getArguments();

  Map<String, String> getEnvironmentVariables();

  List<Path> getImportPaths();

  List<Path> getInputDescriptorFiles();

  List<String> getDescriptorSourceFiles();

  List<Path> getSourcePaths();

  SortedSet<ProtocTarget> getTargets();

  @Nullable Path getSanctionedExecutablePath();
}
