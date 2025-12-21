/*
 * Copyright (C) 2023 Ashley Scopes
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
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import io.github.ascopes.protobufmavenplugin.java.ImmutableJavaApp;
import io.github.ascopes.protobufmavenplugin.java.JavaAppToExecutableFactory;
import io.github.ascopes.protobufmavenplugin.urls.UriResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.ConcurrentExecutor;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resolver for plugins within a project that may be located in several
 * different places.
 *
 * @author Ashley Scopes
 * @since 4.1.1
 */
@Description("Resolves and packages protoc plugins from various remote and local locations")
@MojoExecutionScoped
@Named
public final class ProtocPluginResolver {

  private static final Logger log = LoggerFactory.getLogger(ProtocPluginResolver.class);

  private final ConcurrentExecutor concurrentExecutor;
  private final MavenArtifactPathResolver artifactPathResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UriResourceFetcher urlResourceFetcher;
  private final JavaAppToExecutableFactory javaAppToExecutableFactory;

  @Inject
  ProtocPluginResolver(
      ConcurrentExecutor concurrentExecutor,
      MavenArtifactPathResolver artifactPathResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UriResourceFetcher urlResourceFetcher,
      JavaAppToExecutableFactory javaAppToExecutableFactory
  ) {
    this.concurrentExecutor = concurrentExecutor;
    this.artifactPathResolver = artifactPathResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
    this.javaAppToExecutableFactory = javaAppToExecutableFactory;
  }

  public Collection<ResolvedProtocPlugin> resolvePlugins(
      GenerationRequest request
  ) throws ResolutionException {
    var requestedPlugins = request.getProtocPlugins();

    var futures = Stream.<FutureTask<Optional<ResolvedProtocPlugin>>>builder();
    for (var index = 0; index < requestedPlugins.size(); ++index) {
      futures.accept(resolvePluginSoon(
          requestedPlugins.get(index),
          request.getOutputDirectory(),
          index
      ));
    }

    return futures.build()
        .collect(concurrentExecutor.awaiting())
        .stream()
        .flatMap(Optional::stream)
        .toList();
  }

  private FutureTask<Optional<ResolvedProtocPlugin>> resolvePluginSoon(
      ProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) {
    return concurrentExecutor.submit(() -> {
      if (plugin instanceof BinaryMavenProtocPlugin pluginImpl) {
        return resolveBinaryMavenPlugin(pluginImpl, defaultOutputDirectory, index);
      } else if (plugin instanceof PathProtocPlugin pluginImpl) {
        return resolveBinaryPathPlugin(pluginImpl, defaultOutputDirectory, index);
      } else if (plugin instanceof UriProtocPlugin pluginImpl) {
        return resolveBinaryUrlPlugin(pluginImpl, defaultOutputDirectory, index);
      } else if (plugin instanceof JvmMavenProtocPlugin pluginImpl) {
        return resolveJvmMavenPlugin(pluginImpl, defaultOutputDirectory, index);
      } else {
        // Unreachable, but needed until we use a Java version with pattern matching
        // for types.
        throw new UnsupportedOperationException();
      }
    });
  }

  private Optional<ResolvedProtocPlugin> resolveBinaryMavenPlugin(
      BinaryMavenProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) throws ResolutionException {
    var pluginBuilder = ImmutableBinaryMavenProtocPlugin.builder()
        .from(plugin);

    if (plugin.getClassifier() == null) {
      var classifier = platformClassifierFactory.getClassifier(plugin.getArtifactId());
      pluginBuilder.classifier(classifier);
    }

    if (plugin.getType() == null) {
      pluginBuilder.type("exe");
    }

    plugin = pluginBuilder.build();

    log.debug("Resolving binary Maven protoc plugin \"{}\"", plugin);

    var path = artifactPathResolver.resolveExecutable(plugin);

    var id = computeId(path, index);

    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path, id));
  }

  private Optional<ResolvedProtocPlugin> resolveBinaryPathPlugin(
      PathProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) throws ResolutionException {

    log.debug("Resolving binary path protoc plugin \"{}\"", plugin);
    var maybePath = systemPathResolver.resolve(plugin.getName());

    if (maybePath.isEmpty() && plugin.isOptional()) {
      return Optional.empty();
    }

    var path = maybePath.orElseThrow(() -> new ResolutionException(
        "No plugin named \"" + plugin.getName() + "\" was found on the system path"
    ));

    var id = computeId(path, index);

    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path, id));
  }

  private Optional<ResolvedProtocPlugin> resolveBinaryUrlPlugin(
      UriProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) throws ResolutionException {
    log.debug("Resolving binary URL protoc plugin \"{}\"", plugin);

    var maybePath = urlResourceFetcher.fetchFileFromUri(plugin.getUrl(), ".exe", true);

    if (maybePath.isEmpty() && plugin.isOptional()) {
      return Optional.empty();
    }

    var path = maybePath.orElseThrow(() -> new ResolutionException(
        "Plugin at " + plugin.getUrl() + " does not exist"
    ));

    if (plugin.getDigest() != null) {
      log.debug("Verifying digest of \"{}\" against \"{}\"", plugin.getUrl(), plugin.getDigest());

      try (var is = new BufferedInputStream(Files.newInputStream(path))) {
        plugin.getDigest().verify(is);
      } catch (IOException ex) {
        throw new ResolutionException(
            "Failed to compute digest of \"" + plugin.getUrl() + "\": " + ex,
            ex
        );
      }
    }

    var id = computeId(path, index);

    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path, id));
  }

  private Optional<ResolvedProtocPlugin> resolveJvmMavenPlugin(
      JvmMavenProtocPlugin plugin,
      Path defaultOutputDirectory,
      int index
  ) throws ResolutionException {
    log.debug(
        "Resolving JVM-based Maven protoc plugin \"{}\" and generating bootstrap scripts",
        plugin
    );

    try {
      var dependencies = artifactPathResolver
          .resolveDependencies(
              List.of(plugin),
              DependencyResolutionDepth.TRANSITIVE,
              Set.of("compile", "runtime", "system"),
              false
          )
          .stream()
          .toList();

      var id = computeId(dependencies.get(0), index);

      var app = ImmutableJavaApp.builder()
          .addAllDependencies(dependencies)
          .jvmArgs(plugin.getJvmArgs())
          .jvmConfigArgs(plugin.getJvmConfigArgs())
          .mainClass(plugin.getMainClass())
          .uniqueName(id)
          .build();

      var path = javaAppToExecutableFactory.toExecutable(app);

      return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path, id));
    } catch (ResolutionException ex) {
      throw new ResolutionException("Failed to resolve protoc plugin " + plugin + ": " + ex, ex);
    }
  }

  private ResolvedProtocPlugin createResolvedProtocPlugin(
      ProtocPlugin plugin,
      Path defaultOutputDirectory,
      Path path,
      String id
  ) {
    return ImmutableResolvedProtocPlugin
        .builder()
        .id(id)
        .options(plugin.getOptions())
        .order(plugin.getOrder())
        .outputDirectory(requireNonNullElse(plugin.getOutputDirectory(), defaultOutputDirectory))
        .path(path)
        .build();
  }

  private String computeId(Path path, int index) {
    return index + "_" + Digest.compute("SHA-1", path.toString()).toHexString();
  }
}
