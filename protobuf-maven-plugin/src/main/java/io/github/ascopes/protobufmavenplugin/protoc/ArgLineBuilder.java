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

package io.github.ascopes.protobufmavenplugin.protoc;

import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.plugins.ResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
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
@SuppressWarnings("UnusedReturnValue")
public final class ArgLineBuilder {

  private final Path protocPath;
  private boolean fatalWarnings;
  private final List<Target> targets;
  private final List<Path> importPaths;

  public ArgLineBuilder(Path protocPath) {
    this.protocPath = protocPath;
    fatalWarnings = false;
    targets = new ArrayList<>();
    importPaths = new ArrayList<>();
  }

  public List<String> compile(Collection<SourceListing> sourceListings) {
    if (targets.isEmpty()) {
      throw new IllegalStateException("No output target operations were provided");
    }

    var args = new ArrayList<String>();
    args.add(protocPath.toString());

    if (fatalWarnings) {
      args.add("--fatal_warnings");
    }

    for (var target : targets) {
      target.addArgsTo(args);
    }

    for (var sourceListing : sourceListings) {
      for (var sourcePath : sourceListing.getSourceProtoFiles()) {
        args.add(sourcePath.toString());
      }
    }

    for (var importPath : importPaths) {
      args.add("--proto_path=" + importPath.toString());
    }

    return Collections.unmodifiableList(args);
  }

  public ArgLineBuilder fatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
    return this;
  }

  @SuppressWarnings("UnusedReturnValue")
  public ArgLineBuilder generateCodeFor(Language language, Path outputPath, boolean lite) {
    targets.add(new LanguageTarget(language, outputPath, lite));
    return this;
  }

  public ArgLineBuilder importPaths(Collection<SourceListing> importPathListings) {
    for (var importPathListing : importPathListings) {
      importPaths.add(importPathListing.getSourceRoot());
    }

    return this;
  }

  public ArgLineBuilder plugins(Collection<ResolvedProtocPlugin> plugins, Path outputPath) {
    for (var plugin : plugins) {
      targets.add(new PluginTarget(plugin, outputPath));
    }
    return this;
  }

  private interface Target {

    void addArgsTo(List<String> list);
  }

  private static final class LanguageTarget implements Target {

    private final Language language;
    private final Path outputPath;
    private final boolean lite;

    private LanguageTarget(Language language, Path outputPath, boolean lite) {
      this.language = language;
      this.outputPath = outputPath;
      this.lite = lite;
    }

    @Override
    public void addArgsTo(List<String> list) {
      var flag = "--" + language.getFlagName() + "_out"
          + "="
          + (lite ? "lite:" : "")
          + outputPath;

      list.add(flag);
    }
  }

  private static final class PluginTarget implements Target {

    private final ResolvedProtocPlugin plugin;
    private final Path outputPath;

    private PluginTarget(ResolvedProtocPlugin plugin, Path outputPath) {
      this.plugin = plugin;
      this.outputPath = outputPath;
    }

    @Override
    public void addArgsTo(List<String> list) {
      // protoc always maps a flag `--xxx_out` to a plugin named `protoc-gen-xxx`, so we have
      // to inject this flag to be consistent.
      list.add("--plugin=protoc-gen-" + plugin.getId() + "=" + plugin.getPath());
      list.add("--" + plugin.getId() + "_out=" + outputPath);
      plugin.getOptions()
          .map(options -> "--" + plugin.getId() + "_opt=" + options)
          .ifPresent(list::add);
    }
  }
}
