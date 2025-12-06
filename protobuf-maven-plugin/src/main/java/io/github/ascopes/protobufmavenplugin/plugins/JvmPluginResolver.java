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
package io.github.ascopes.protobufmavenplugin.plugins;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.java.ImmutableJavaApp;
import io.github.ascopes.protobufmavenplugin.java.JavaAppToExecutableFactory;
import io.github.ascopes.protobufmavenplugin.utils.ArgumentFileBuilder;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.BufferedWriter;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that takes a reference to a pure-Java {@code protoc} plugin and wraps it in a shell
 * script or batch file to invoke it as if it were a single executable binary.
 *
 * <p>The aim is to support enabling protoc to invoke executables without creating native
 * binaries first, which is error-prone and increases the complexity of this Maven plugin
 * significantly.
 *
 * <p>This implementation is a rewrite as of v2.6.0 that now uses Java argument files to
 * deal with argument quoting in a platform-agnostic way, since the specification for batch files is
 * very poorly documented and full of edge cases that could cause builds to fail.
 *
 * @author Ashley Scopes
 * @since 2.6.0
 */
@Description("Resolves and packages JVM protoc plugins from various remote and local locations")
@MojoExecutionScoped
@Named
final class JvmPluginResolver {

  private static final Set<String> ALLOWED_SCOPES = Set.of("compile", "runtime", "system");

  private static final Logger log = LoggerFactory.getLogger(JvmPluginResolver.class);

  private final MavenArtifactPathResolver artifactPathResolver;
  private final JavaAppToExecutableFactory javaAppToExecutableFactory;

  @Inject
  JvmPluginResolver(
      MavenArtifactPathResolver artifactPathResolver,
      JavaAppToExecutableFactory javaAppToExecutableFactory
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.javaAppToExecutableFactory = javaAppToExecutableFactory;
  }

  Collection<ResolvedProtocPlugin> resolveMavenPlugins(
      Collection<? extends JvmMavenProtocPlugin> plugins,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedProtocPlugin>();
    var index = 0;

    for (var plugin : plugins) {
      if (plugin.isSkip()) {
        log.info("User requested to skip plugin \"{}\"", plugin);
        continue;
      }

      var resolvedPlugin = resolveMavenPlugin(
          plugin,
          defaultOutputDirectory,
          index++
      );
      resolvedPlugins.add(resolvedPlugin);
    }
    return Collections.unmodifiableList(resolvedPlugins);
  }

  private ResolvedProtocPlugin resolveMavenPlugin(
      JvmMavenProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) throws ResolutionException {

    log.debug(
        "Resolving JVM-based Maven protoc plugin \"{}\" and generating bootstrap scripts",
        plugin
    );

    var id = hashPlugin(plugin, index);

    try {
      var dependencies = artifactPathResolver
          .resolveDependencies(
              List.of(plugin),
              DependencyResolutionDepth.TRANSITIVE,
              ALLOWED_SCOPES,
              false
          )
          .stream()
          .toList();

      var app = ImmutableJavaApp.builder()
          .addAllDependencies(dependencies)
          .jvmArgs(plugin.getJvmArgs())
          .jvmConfigArgs(plugin.getJvmConfigArgs())
          .mainClass(plugin.getMainClass())
          .uniqueName(id)
          .build();

      var executablePath = javaAppToExecutableFactory.toExecutable(app);

      return ImmutableResolvedProtocPlugin
          .builder()
          .id(id)
          .options(plugin.getOptions())
          .order(plugin.getOrder())
          .outputDirectory(requireNonNullElse(plugin.getOutputDirectory(), defaultOutputDirectory))
          .path(executablePath)
          .build();
    } catch (ResolutionException ex) {
      throw new ResolutionException("Failed to resolve protoc plugin " + plugin + ": " + ex, ex);
    }
  }

  private String hashPlugin(JvmMavenProtocPlugin plugin, int index) {
    // GH-421: Ensure duplicate plugin definitions retain a unique name
    // when in the same execution, rather than trampling over each-other's
    // files.
    return Digest.compute("SHA-1", plugin.toString()).toHexString()
        + "-" + index;
  }
}
