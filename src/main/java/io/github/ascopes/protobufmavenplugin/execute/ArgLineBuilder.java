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

package io.github.ascopes.protobufmavenplugin.execute;

import io.github.ascopes.protobufmavenplugin.plugin.ResolvedPlugin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Builder for a {@code protoc} commandline invocation.
 *
 * @author Ashley Scopes
 */
public final class ArgLineBuilder {
  private final List<String> args;
  private int outputTargetCount;

  public ArgLineBuilder(Path protocPath) {
    args = new ArrayList<>();
    args.add(protocPath.toString());
    outputTargetCount = 0;
  }

  public ArgLineBuilder cppOut(Path outputPath, boolean lite) {
    return langOut("cpp", outputPath, lite);
  }

  public ArgLineBuilder csharpOut(Path outputPath, boolean lite) {
    return langOut("csharp", outputPath, lite);
  }

  public List<String> compile(Collection<Path> sourcesToCompile) {
    if (outputTargetCount == 0) {
      throw new IllegalStateException("No output targets were provided");
    }

    var finalArgs = new ArrayList<>(args);

    for (var path : sourcesToCompile) {
      finalArgs.add(path.toString());
    }

    return Collections.unmodifiableList(finalArgs);
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
    return langOut("java", outputPath, lite);
  }

  public ArgLineBuilder kotlinOut(Path outputPath, boolean lite) {
    return langOut("kotlin", outputPath, lite);
  }

  public ArgLineBuilder objcOut(Path outputPath, boolean lite) {
    return langOut("objc", outputPath, lite);
  }

  public ArgLineBuilder phpOut(Path outputPath, boolean lite) {
    return langOut("php", outputPath, lite);
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

  public ArgLineBuilder pyiOut(Path outputPath, boolean lite) {
    return langOut("pyi", outputPath, lite);
  }

  public ArgLineBuilder pythonOut(Path outputPath, boolean lite) {
    return langOut("python", outputPath, lite);
  }

  public ArgLineBuilder rubyOut(Path outputPath, boolean lite) {
    return langOut("ruby", outputPath, lite);
  }

  public ArgLineBuilder rustOut(Path outputPath, boolean lite) {
    return langOut("rust", outputPath, lite);
  }

  public List<String> version() {
    return List.of(args.get(0), "--version");
  }

  private ArgLineBuilder langOut(String type, Path outputPath, boolean lite) {
    ++outputTargetCount;
    var flag = lite
        ? "--" + type + "_out=lite:"
        : "--" + type + "_out=";
    args.add(flag + outputPath);
    return this;
  }
}
