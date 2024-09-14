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
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.jspecify.annotations.Nullable;
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
  ) throws ResolutionException, IOException {

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

    var args = new ArrayList<String>();
    args.add(hostSystem.getJavaExecutablePath().toString());

    // Caveat: we currently ignore the Class-Path JAR manifest entry. Not sure why we would want
    // to be using that here though, so I am leaving it unimplemented until such a time that someone
    // requests it.
    args.add("-classpath");
    args.add(buildJavaPath(dependencies));

    var modules = findJavaModules(dependencies);

    if (!modules.isEmpty()) {
      args.add("--module-path");
      args.add(buildJavaPath(modules));
    }

    args.add(determineMainClass(plugin, dependencies.get(0)));

    return Collections.unmodifiableList(args);
  }

  private String determineMainClass(MavenProtocPlugin plugin, Path pluginPath) throws IOException {
    // GH-363: It appears that we have to avoid calling `java -jar` when running JARs as the
    // classpath argument is totally ignored by Java in this case, meaning no dependencies
    // get loaded correctly, and we get NoClassDefFoundErrors being raised for non-shaded JARs.
    // This means we have to explicitly provide the main class entrypoint due to the way we
    // have to invoke the java executable, and this in turn means we have to do some sniffing
    // around to make a best-effort guess at what the main class really is... which is not very
    // fun.

    if (plugin.getMainClass() != null) {
      // The user provided it explicitly in the configuration, so trust their judgement.
      log.debug("Using user-provided main class for {}", plugin);
      return plugin.getMainClass();
    }

    // If we don't have a JAR, we can't really guess the main class, as Maven will not emit
    // the MANIFEST.MF directly in a place we can see it. I guess we could try and scrape the
    // POM of the project but that is likely to be awkward and at best flaky due to the numerous
    // ways this attribute could be injected into any manifest. Let's just keep it simple for now.
    if (!Files.isDirectory(pluginPath)) {
      var mainClass = tryToDetermineMainClassFromJarManifest(pluginPath);

      if (mainClass == null) {
        // Not my fault! Please provide a Main-Class attribute on the JAR instead...
        log.warn(
            "No Main-Class manifest attribute found in {}, this is probably a bug with how that"
                + " JAR was built",
            pluginPath
        );
      } else {
        log.debug("Determined main class to be {} from manifest for {}", mainClass, pluginPath);
        return mainClass;
      }
    }

    throw new IllegalArgumentException(
        "No main class was described for "
            + pluginPath
            + ", please provide an explicit "
            + "'mainClass' attribute when configuring the "
            + plugin.getArtifactId()
            + " JVM plugin"
    );
  }

  private @Nullable String tryToDetermineMainClassFromJarManifest(
      Path pluginPath
  ) throws IOException {
    try (
        var zip = FileUtils.openZipAsFileSystem(pluginPath);
        var manifestStream = Files.newInputStream(zip.getPath("META-INF", "MANIFEST.MF"))
    ) {
      var manifest = new Manifest(manifestStream);
      return manifest.getMainAttributes().getValue("Main-Class");
    }
  }

  private String buildJavaPath(Iterable<Path> iterable) {
    // Expectation: at least one path is in the iterator.
    var iterator = iterable.iterator();
    var sb = new StringBuilder()
        .append(iterator.next());

    while (iterator.hasNext()) {
      sb.append(hostSystem.getPathSeparator()).append(iterator.next());
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

    writeScript(fullScriptPath, script, StandardCharsets.ISO_8859_1);
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

    writeScript(fullScriptPath, script, StandardCharsets.UTF_8);
    return fullScriptPath;
  }

  private void writeScript(Path path, String content, Charset charset) throws IOException {
    log.debug("Writing the following script to {} as {}:\n{}", path, charset, content);
    Files.writeString(path, content, charset);
    FileUtils.makeExecutable(path);
  }

  private List<Path> findJavaModules(List<Path> paths) {
    // TODO: is using a module finder here an overkill?
    return ModuleFinder.of(paths.toArray(Path[]::new))
        .findAll()
        .stream()
        .map(ModuleReference::location)
        .flatMap(Optional::stream)
        .map(Path::of)
        .map(FileUtils::normalize)
        .peek(modulePath -> log.debug("Looks like {} is a JPMS module!", modulePath))
        .collect(Collectors.toUnmodifiableList());
  }
}
