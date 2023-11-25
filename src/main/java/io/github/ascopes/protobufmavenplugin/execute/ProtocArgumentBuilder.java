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
package io.github.ascopes.protobufmavenplugin.execute;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Builder for a {@code protoc} invocation.
 *
 * <p>Instances of this class should be single-use only and may produce
 * undefined behaviour if used after being built into an executor.
 *
 * @author Ashley Scopes
 */
public final class ProtocArgumentBuilder {

  // See the following source code for all supported flags:
  // https://github.com/protocolbuffers/protobuf/blob/7f94235e552599141950d7a4a3eaf93bc87d1b22/src/google/protobuf/compiler/command_line_interface.cc#L2438

  private final Path executablePath;
  private final List<String> arguments;

  /**
   * Initialise this builder.
   *
   * @param executablePath the path to the {@code protoc} executable.
   */
  public ProtocArgumentBuilder(Path executablePath) {
    this.executablePath = executablePath;
    arguments = new ArrayList<>();
  }

  /**
   * Generate the arguments to use to capture the {@code protoc} version.
   *
   * @return the arguments.
   */
  public List<String> version() {
    return List.of(executablePath.toString(), "--version");
  }

  /**
   * Add base directories to resolve imports in.
   *
   * @param directories the directories to include.
   * @return this builder.
   */
  public ProtocArgumentBuilder includeDirectories(Collection<Path> directories) {
    for (var directory : directories) {
      arguments.add("--proto_path=" + directory);
    }
    return this;
  }

  /**
   * Add an output directory.
   *
   * @param outputType      the output type, e.g. {@code java} or {@code kotlin}.
   * @param outputDirectory the directory to write outputs to.
   * @return this builder.
   */
  public ProtocArgumentBuilder outputDirectory(String outputType, Path outputDirectory) {
    arguments.add("--" + outputType + "_out=" + outputDirectory);
    return this;
  }

  /**
   * Enable/disable fatal warnings.
   *
   * <p>Calling this more than once is undefined behaviour.
   *
   * @param fatalWarnings whether to enable fatal warnings or not.
   * @return this builder.
   */
  public ProtocArgumentBuilder fatalWarnings(boolean fatalWarnings) {
    if (fatalWarnings) {
      arguments.add("--fatal_warnings");
    }
    return this;
  }

  /**
   * Add the proto files to the flags and return them
   *
   * <p>This object should not be used after this invocation.
   *
   * @param protoFiles the proto files to add to the command line.
   * @return the arguments.
   */
  public List<String> build(Collection<Path> protoFiles) {
    var finalArguments = new ArrayList<String>(1 + arguments.size() + protoFiles.size());
    finalArguments.add(executablePath.toString());
    finalArguments.addAll(arguments);
    for (var protoFile : protoFiles) {
      finalArguments.add(protoFile.toString());
    }
    return Collections.unmodifiableList(finalArguments);
  }
}
