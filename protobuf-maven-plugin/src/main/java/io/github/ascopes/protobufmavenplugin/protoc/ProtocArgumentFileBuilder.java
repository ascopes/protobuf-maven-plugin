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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builder for a {@code protoc} command line invocation {@link ArgumentFileBuilder}.
 *
 * @author Ashley Scopes
 */
@SuppressWarnings("UnusedReturnValue")
public final class ProtocArgumentFileBuilder {

  private boolean fatalWarnings;
  private final List<Path> importPaths;
  private final List<Path> sourcePaths;
  private final Set<Target> targets;

  public ProtocArgumentFileBuilder() {
    fatalWarnings = false;
    importPaths = new ArrayList<>();
    sourcePaths = new ArrayList<>();
    targets = new TreeSet<>();
  }

  public ProtocArgumentFileBuilder addImportPaths(Collection<Path> importPaths) {
    this.importPaths.addAll(importPaths);
    return this;
  }

  public ProtocArgumentFileBuilder addLanguages(
      Collection<Language> languages,
      Path outputPath,
      boolean lite
  ) {
    languages.stream()
        .map(language -> new LanguageTarget(language, outputPath, lite))
        .forEachOrdered(targets::add);
    return this;
  }

  public ProtocArgumentFileBuilder addPlugins(
      Collection<ResolvedProtocPlugin> plugins,
      Path outputPath
  ) {
    plugins.stream()
        .map(plugin -> new PluginTarget(plugin, outputPath))
        .forEachOrdered(targets::add);
    return this;
  }

  public ProtocArgumentFileBuilder addSourcePaths(Collection<Path> sourcePaths) {
    this.sourcePaths.addAll(sourcePaths);
    return this;
  }

  public ProtocArgumentFileBuilder setFatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
    return this;
  }

  public ProtocArgumentFileBuilder setOutputDescriptorFile(
      Path outputDescriptorFile,
      boolean includeImports,
      boolean includeSourceInfo,
      boolean retainOptions
  ) {
    targets.add(new ProtoDescriptorTarget(
        outputDescriptorFile,
        includeImports,
        includeSourceInfo,
        retainOptions
    ));
    return this;
  }

  public ArgumentFileBuilder toArgumentFileBuilder() {
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

  private abstract static class Target implements Comparable<Target> {

    private final String key;

    Target(String key) {
      this.key = key;
    }

    abstract void addArgsTo(ArgumentFileBuilder argumentFileBuilder);

    abstract int getOrder();

    @Override
    public final int compareTo(Target that) {
      // Compare by order first, then by the key string representation. The latter
      // enables stable ordering between instances of the same class and
      // instances of different classes between builds and machines.
      return Comparator
          .comparingInt(Target::getOrder)
          .thenComparing(target -> target.key)
          .compare(this, that);
    }
  }

  private static final class LanguageTarget extends Target {

    private final Language language;
    private final Path outputPath;
    private final boolean lite;

    private LanguageTarget(Language language, Path outputPath, boolean lite) {
      super("language:" + language);
      this.language = language;
      this.outputPath = outputPath;
      this.lite = lite;
    }

    @Override
    void addArgsTo(ArgumentFileBuilder argumentFileBuilder) {
      var flag = "--" + language.getFlagName() + "_out"
          + "="
          + (lite ? "lite:" : "")
          + outputPath;

      argumentFileBuilder.add(flag);
    }

    @Override
    int getOrder() {
      return 0;
    }
  }

  private static final class PluginTarget extends Target {

    private final ResolvedProtocPlugin plugin;
    private final Path outputPath;

    private PluginTarget(ResolvedProtocPlugin plugin, Path outputPath) {
      super("plugin:" + plugin + ":" + outputPath);
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

    @Override
    public int getOrder() {
      return plugin.getOrder();
    }
  }

  private static final class ProtoDescriptorTarget extends Target {

    private final Path outputDescriptorFile;
    private final boolean includeImports;
    private final boolean includeSourceInfo;
    private final boolean retainOptions;

    private ProtoDescriptorTarget(
        Path outputDescriptorFile,
        boolean includeImports,
        boolean includeSourceInfo,
        boolean retainOptions
    ) {
      super("descriptor:" + outputDescriptorFile);
      this.outputDescriptorFile = outputDescriptorFile;
      this.includeImports = includeImports;
      this.includeSourceInfo = includeSourceInfo;
      this.retainOptions = retainOptions;
    }

    @Override
    void addArgsTo(ArgumentFileBuilder argumentFileBuilder) {
      argumentFileBuilder.add("--descriptor_set_out=" + outputDescriptorFile);

      if (includeImports) {
        argumentFileBuilder.add("--include_imports");
      }

      if (includeSourceInfo) {
        argumentFileBuilder.add("--include_source_info");
      }

      if (retainOptions) {
        argumentFileBuilder.add("--retain_options");
      }
    }

    @Override
    int getOrder() {
      return 0;
    }
  }
}
