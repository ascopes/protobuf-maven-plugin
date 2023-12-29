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

import io.github.ascopes.protobufmavenplugin.dependency.ResolvedPlugin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a {@code protoc} commandline invocation.
 *
 * @author Ashley Scopes
 */
public final class ArgLineBuilder {
  private final List<String> args;
  private volatile int outputTargetCount;

  public ArgLineBuilder(Path protocPath) {
    args = new ArrayList<>();
    args.add(protocPath.toString());
    outputTargetCount = 0;
  }

  public List<String> compile(Collection<Path> sourcesToCompile) {
    if (outputTargetCount == 0) {
      throw new IllegalStateException("No output targets were provided");
    }

    for (var path : sourcesToCompile) {
      args.add(path.toString());
    }
    return args;
  }

  public ArgLineBuilder fatalWarnings(boolean fatalWarnings) {
    if (fatalWarnings) {
      args.add("--fatal_warnings");
    }
    return this;
  }

  public ArgLineBuilder importPaths(Collection<Path> includePaths) {
    for (var includePath : includePaths) {
      args.add("--proto_path=" + includePath);
    }
    return this;
  }

  public ArgLineBuilder javaOut(Path outputPath, boolean lite) {
    ++outputTargetCount;
    var flag = lite
        ? "--java_out=lite:"
        : "--java_out=";
    args.add(flag + outputPath);
    return this;
  }

  public ArgLineBuilder kotlinOut(Path outputPath, boolean lite) {
    ++outputTargetCount;
    var flag = lite
        ? "--kotlin_out=lite:"
        : "--kotlin_out=";
    args.add(flag + outputPath);
    return this;
  }

  public ArgLineBuilder plugins(Collection<ResolvedPlugin> plugins, Path outputPath) {
    for (var plugin : plugins) {
      // protoc always maps a flag `--xxx_out` to a plugin named `protoc-gen-xxx`, so we have
      // to inject this flag to be consistent.
      ++outputTargetCount;
      args.add("--plugin=protoc-gen-" + plugin.getId() + "=" + plugin.getPath());
      args.add("--" + plugin.getId() + "_out=" + outputPath);
    }
    return this;
  }
}
