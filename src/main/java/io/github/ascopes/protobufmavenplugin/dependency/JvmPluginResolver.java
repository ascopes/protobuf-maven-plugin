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

package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.generate.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.platform.Digests;
import io.github.ascopes.protobufmavenplugin.platform.FileUtils;
import io.github.ascopes.protobufmavenplugin.platform.HostSystem;
import io.github.ascopes.protobufmavenplugin.platform.Shlex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;

/**
 * Wraps a JVM-based plugin invocation using an OS-native script that calls Java.
 *
 * <p>This script can be marked as executable and passed to the {@code protoc} invocation
 * as a path to ensure the script gets called correctly. By doing this, we avoid the need to
 * build OS-native executables during the protobuf compilation process.
 *
 * @author Ashley Scopes
 */
@Named
public final class JvmPluginResolver {

  private static final Set<String> SCOPES = Set.of("compile", "runtime", "system");

  private final HostSystem hostSystem;
  private final MavenDependencyPathResolver dependencyPathResolver;
  private final TemporarySpace temporarySpace;

  @Inject
  public JvmPluginResolver(
      HostSystem hostSystem,
      MavenDependencyPathResolver dependencyPathResolver,
      TemporarySpace temporarySpace
  ) {
    this.hostSystem = hostSystem;
    this.dependencyPathResolver = dependencyPathResolver;
    this.temporarySpace = temporarySpace;
  }

  public Collection<ResolvedPlugin> resolveMavenPlugins(
      MavenSession session,
      Collection<MavenArtifact> plugins
  ) throws IOException, ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedPlugin>();
    for (var plugin : plugins) {
      resolvedPlugins.add(resolve(session, plugin));
    }
    return resolvedPlugins;
  }

  private ResolvedPlugin resolve(
      MavenSession session,
      MavenArtifact plugin
  ) throws IOException, ResolutionException {
    var pluginId = pluginIdDigest(plugin);
    var argLine = resolveAndBuildArgLine(session, plugin);

    Path scriptPath;
    if (hostSystem.isProbablyWindows()) {
      scriptPath = writeWindowsBatchScript(pluginId, argLine);
    } else {
      scriptPath = writeShellScript(pluginId, argLine);
    }

    return ImmutableResolvedPlugin
        .builder()
        .id(pluginId)
        .path(scriptPath)
        .build();
  }

  private List<String> resolveAndBuildArgLine(
      MavenSession session,
      MavenArtifact plugin
  ) throws ResolutionException {

    // Resolve dependencies first.
    var dependencyIterator = dependencyPathResolver
        .resolveDependencyTreePaths(
            session,
            SCOPES,
            DependencyResolutionScope.TRANSITIVE,
            plugin
        )
        .iterator();

    // First dependency is always the thing we actually want to execute,
    // so is guaranteed to be present.
    var args = new ArrayList<String>();
    args.add("java");
    args.add("-jar");
    args.add(dependencyIterator.next().toString());

    if (dependencyIterator.hasNext()) {
      var sb = new StringBuilder().append(dependencyIterator.next());

      while (dependencyIterator.hasNext()) {
        sb.append(":").append(dependencyIterator.next());
      }

      args.add("-classpath");
      args.add(sb.toString());
    }

    return args;
  }

  private String pluginIdDigest(MavenArtifact plugin) {
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
