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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  private static final Set<String> ALLOWED_SCOPES = Set.of("compile", "runtime", "system");
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

    // Assumption: this always has at least one item in it, and the first item is the plugin
    // artifact itself.
    var dependencies = artifactPathResolver
        .resolveDependencies(
            List.of(plugin),
            DependencyResolutionDepth.TRANSITIVE,
            ALLOWED_SCOPES,
            false,
            true
        );

    var argLineBuilder = Stream.<String>builder();
    argLineBuilder.add(hostSystem.getJavaExecutablePath().toString());
    var pluginPath = dependencies.get(0);

    if (Files.isDirectory(pluginPath)) {
      buildArgLineForClassTreePlugin(argLineBuilder, plugin, pluginPath, dependencies);
    } else {
      buildArgLineForJarPlugin(argLineBuilder, plugin, pluginPath, dependencies);
    }

    return argLineBuilder.build()
        .collect(Collectors.toUnmodifiableList());
  }

  private void buildArgLineForJarPlugin(
      Stream.Builder<String> argLineBuilder,
      MavenProtocPlugin plugin,
      Path pluginPath,
      List<Path> dependencies
  ) {
    log.debug("Treating JVM plugin at {} as a bundled JAR", pluginPath);

    if (plugin.getMainClass() != null) {
      log.warn("The plugin at {} has been provided with a 'mainClass' attribute, but this is "
          + "not applicable for packaged JARs. Please remove this argument. This may be promoted "
          + "to an error in a future release.", pluginPath);
    }

    if (dependencies.size() > 1) {
      argLineBuilder.add("-classpath");
      argLineBuilder.add(buildJavaPath(dependencies.stream().skip(1)));
    }

    argLineBuilder.add("-jar");
    argLineBuilder.add(pluginPath.toString());
  }

  private void buildArgLineForClassTreePlugin(
      Stream.Builder<String> argLineBuilder,
      MavenProtocPlugin plugin,
      Path pluginPath,
      List<Path> dependencies
  ) {
    log.debug("Treating JVM plugin at {} as an unbundled class tree", pluginPath);

    if (plugin.getMainClass() == null) {
      throw new IllegalArgumentException(
          "The plugin at " + pluginPath
              + " is not a bundled JAR. Please provide the 'mainClass' attribute in "
              + "the configuration!");
    }

    argLineBuilder.add("-classpath");
    argLineBuilder.add(buildJavaPath(dependencies.stream()));
    argLineBuilder.add(plugin.getMainClass());
  }

  private String buildJavaPath(Stream<Path> paths) {
    // Expectation: at least one path is in the iterator.
    var iterator = paths.iterator();

    var sb = new StringBuilder()
        .append(iterator.next());

    while (iterator.hasNext()) {
      sb.append(":").append(iterator.next());
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

    var script = String.join(
        "\r\n",
        "@echo off",
        "",
        ":: ##################################################",
        ":: ### Generated by ascopes/protobuf-maven-plugin ###",
        ":: ###   Users should not invoke this script      ###",
        ":: ###   directly, unless they know what they are ###",
        ":: ###   doing.                                   ###",
        ":: ##################################################",
        "",
        Shlex.quoteBatchArgs(argLine),
        ""  // Trailing newline.
    );

    Files.writeString(fullScriptPath, script, StandardCharsets.ISO_8859_1);
    return fullScriptPath;
  }

  private Path writeShellScript(
      String pluginId,
      List<String> argLine
  ) throws IOException {
    var fullScriptPath = resolvePluginScriptPath().resolve(pluginId + ".sh");

    var script = String.join(
        "\n",
        "#!/usr/bin/env sh",
        "",
        "##################################################",
        "### Generated by ascopes/protobuf-maven-plugin ###",
        "###   Users should not invoke this script      ###",
        "###   directly unless they know what they are  ###",
        "###   doing.                                   ###",
        "##################################################",
        "",
        "set -eu",
        "",
        Shlex.quoteShellArgs(argLine),
        ""  // Trailing newline
    );

    Files.writeString(fullScriptPath, script, StandardCharsets.UTF_8);
    FileUtils.makeExecutable(fullScriptPath);
    return fullScriptPath;
  }
}
