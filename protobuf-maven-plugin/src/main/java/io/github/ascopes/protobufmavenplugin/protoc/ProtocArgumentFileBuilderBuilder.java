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

import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.plugins.ResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.utils.ArgumentFileBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builder for a {@code protoc} command line invocation {@link ArgumentFileBuilder}.
 *
 * @author Ashley Scopes
 */
public final class ProtocArgumentFileBuilderBuilder {

  private boolean fatalWarnings;
  private final List<Path> importPaths;
  private final List<Path> sourcePaths;
  private final List<Target> targets;

  public ProtocArgumentFileBuilderBuilder() {
    fatalWarnings = false;
    importPaths = new ArrayList<>();
    sourcePaths = new ArrayList<>();
    targets = new ArrayList<>();
  }

  public ProtocArgumentFileBuilderBuilder addImportPaths(Collection<Path> importPaths) {
    this.importPaths.addAll(importPaths);
    return this;
  }

  public ProtocArgumentFileBuilderBuilder addLanguages(
      Collection<Language> languages,
      Path outputPath,
      boolean lite
  ) {
    languages.stream()
        .map(language -> new LanguageTarget(language, outputPath, lite))
        .forEachOrdered(targets::add);
    return this;
  }

  public ProtocArgumentFileBuilderBuilder addPlugins(
      Collection<ResolvedProtocPlugin> plugins,
      Path outputPath
  ) {
    plugins.stream()
        .map(plugin -> new PluginTarget(plugin, outputPath))
        .forEachOrdered(targets::add);
    return this;
  }

  public ProtocArgumentFileBuilderBuilder addSourcePaths(Collection<Path> sourcePaths) {
    this.sourcePaths.addAll(sourcePaths);
    return this;
  }

  public ProtocArgumentFileBuilderBuilder setFatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
    return this;
  }

  public ProtocArgumentFileBuilderBuilder setOutputDescriptorFile(Path outputDescriptorFile) {
    targets.add(new ProtoDescriptorTarget(outputDescriptorFile));
    return this;
  }

  public ArgumentFileBuilder build() {
    if (targets.isEmpty()) {
      throw new IllegalStateException("No output target operations were provided");
    }

    var argumentFileBuilder = new ArgumentFileBuilder();

    if (fatalWarnings) {
      argumentFileBuilder.add("--fatal_warnings");
    }

    for (var target : targets) {
      target.addArgsTo(argumentFileBuilder);
    }

    for (var sourcePath : sourcePaths) {
      argumentFileBuilder.add(sourcePath);
    }

    for (var importPath : importPaths) {
      argumentFileBuilder.add("--proto_path=" + importPath);
    }

    return argumentFileBuilder;
  }

  private interface Target {

    void addArgsTo(ArgumentFileBuilder argumentFileBuilder);
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
    public void addArgsTo(ArgumentFileBuilder argumentFileBuilder) {
      var flag = "--" + language.getFlagName() + "_out"
          + "="
          + (lite ? "lite:" : "")
          + outputPath;

      argumentFileBuilder.add(flag);
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
    public void addArgsTo(ArgumentFileBuilder argumentFileBuilder) {
      // protoc always maps a flag `--xxx_out` to a plugin named `protoc-gen-xxx`, so we have
      // to inject this flag to be consistent.
      argumentFileBuilder.add("--plugin=protoc-gen-" + plugin.getId() + "=" + plugin.getPath());
      argumentFileBuilder.add("--" + plugin.getId() + "_out=" + outputPath);
      plugin.getOptions()
          .map(options -> "--" + plugin.getId() + "_opt=" + options)
          .ifPresent(argumentFileBuilder::add);
    }
  }

  private static final class ProtoDescriptorTarget implements Target {

    private final Path outputDescriptorFile;

    private ProtoDescriptorTarget(Path outputDescriptorFile) {
      this.outputDescriptorFile = outputDescriptorFile;
    }

    @Override
    public void addArgsTo(ArgumentFileBuilder argumentFileBuilder) {
      argumentFileBuilder.add("--descriptor_set_out=" + outputDescriptorFile);
    }
  }
}
