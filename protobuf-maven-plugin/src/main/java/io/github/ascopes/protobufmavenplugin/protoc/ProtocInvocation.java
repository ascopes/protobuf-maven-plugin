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

/**
 * Model that holds information about the exact {@code protoc} invocation to perform,
 * after all paths have been resolved.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
@Immutable
public interface ProtocInvocation {

  // The executable protoc binary.
  Path getProtocPath();

  // Fail if we get warnings, rather than continuing.
  boolean isFatalWarnings();

  // Additional arguments to pass to protoc.
  List<String> getArguments();

  // Environment variables to explicitly set.
  Map<String, String> getEnvironmentVariables();

  // Paths to proto source files on the root file system to compile.
  List<Path> getImportPaths();

  // The physical descriptor files to build.
  List<Path> getInputDescriptorFiles();

  // The files that are described within the provided input descriptors which
  // we want protoc to generate source code from.
  List<String> getDescriptorSourceFiles();

  // Paths to proto source files on the root file system to compile.
  List<Path> getSourcePaths();

  // "Things" to make or output. This can be built-in language generators,
  // protoc plugins, Java plugins that are decorated in an OS-specific set of
  // scripts to invoke it via the kernel fork/exec mechanism, or descriptor
  // files to generate.
  SortedSet<ProtocTarget> getTargets();
}
