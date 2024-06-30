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

package io.github.ascopes.protobufmavenplugin.plugins;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependencies.aether.AetherMavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.generation.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.Shlex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a JVM-based plugin invocation using an OS-native script that calls Java.
 *
 * <p>This script can be marked as executable and passed to the {@code protoc} invocation
 * as a path to ensure the script gets called correctly. By doing this, we avoid the need to build
 * OS-native executables during the protobuf compilation process.
 *
 * @author Ashley Scopes
 */
@Named
public final class JvmPluginResolver {

  private static final Logger log = LoggerFactory.getLogger(BinaryPluginResolver.class);

  private final HostSystem hostSystem;
  private final AetherMavenArtifactPathResolver artifactPathResolver;
  private final TemporarySpace temporarySpace;

  @Inject
  public JvmPluginResolver(
      HostSystem hostSystem,
      AetherMavenArtifactPathResolver artifactPathResolver,
      TemporarySpace temporarySpace
  ) {
    this.hostSystem = hostSystem;
    this.artifactPathResolver = artifactPathResolver;
    this.temporarySpace = temporarySpace;
  }

  public Collection<ResolvedProtocPlugin> resolveMavenPlugins(
      Collection<? extends MavenProtocPlugin> plugins
  ) throws IOException, ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedProtocPlugin>();
    for (var plugin : plugins) {
      if (plugin.isSkip()) {
        log.info("Skipping plugin {}", plugin);
        continue;
      }

      resolvedPlugins.add(resolve(plugin));
    }
    return resolvedPlugins;
  }

  private ResolvedProtocPlugin resolve(
      MavenProtocPlugin plugin
  ) throws IOException, ResolutionException {

    log.debug(
        "Resolving JVM-based Maven protoc plugin {} and generating OS-specific boostrap scripts",
        plugin
    );

    var pluginId = pluginIdDigest(plugin);
    var argLine = resolveAndBuildArgLine(plugin);

    var scriptPath = hostSystem.isProbablyWindows()
        ? writeWindowsBatchScript(pluginId, argLine)
        : writeShellScript(pluginId, argLine);

    return ImmutableResolvedProtocPlugin
        .builder()
        .id(pluginId)
        .path(scriptPath)
        .options(plugin.getOptions())
        .order(plugin.getOrder())
        .build();
  }

  private List<String> resolveAndBuildArgLine(
      MavenProtocPlugin plugin
  ) throws ResolutionException {

    // Resolve dependencies first.
    var dependencyIterator = artifactPathResolver
        .resolveDependencies(List.of(plugin), DependencyResolutionDepth.TRANSITIVE, false)
        .iterator();

    // First dependency is always the thing we actually want to execute,
    // so is guaranteed to be present. Marked as final to avoid checkstyle complaining
    // about the distance between declaration and usage.
    final var pluginPath = dependencyIterator.next();
    var args = new ArrayList<String>();
    args.add("java");

    if (dependencyIterator.hasNext()) {
      args.add("-classpath");
      args.add(buildClasspath(dependencyIterator));
    }

    args.add("-jar");
    args.add(pluginPath.toString());

    return args;
  }

  private String buildClasspath(Iterator<Path> paths) {
    // Expectation: at least one path is in the iterator.
    var sb = new StringBuilder()
        .append(paths.next());

    while (paths.hasNext()) {
      sb.append(":").append(paths.next());
    }

    return sb.toString();
  }

  private String pluginIdDigest(MavenProtocPlugin plugin) {
    return Digests.sha1(plugin.toString());
  }

  private Path resolvePluginScriptPath() {
    return temporarySpace.createTemporarySpace("plugins", "jvm");
  }

  private Path writeWindowsBatchScript(
      String pluginId,
      List<String> argLine
  ) throws IOException {
    var fullScriptPath = resolvePluginScriptPath().resolve(pluginId + ".bat");

    var script = "@echo off\r\n"
        + Shlex.quoteBatchArgs(argLine)
        + "\r\n";

    Files.writeString(fullScriptPath, script, StandardCharsets.ISO_8859_1);
    return fullScriptPath;
  }

  private Path writeShellScript(
      String pluginId,
      List<String> argLine
  ) throws IOException {
    var fullScriptPath = resolvePluginScriptPath().resolve(pluginId + ".sh");

    var script = "#!/usr/bin/env sh\n"
        + "set -eu\n"
        + Shlex.quoteShellArgs(argLine)
        + "\n";

    Files.writeString(fullScriptPath, script, StandardCharsets.UTF_8);
    FileUtils.makeExecutable(fullScriptPath);
    return fullScriptPath;
  }
}
