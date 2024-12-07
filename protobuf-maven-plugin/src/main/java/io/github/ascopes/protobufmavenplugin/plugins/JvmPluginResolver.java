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

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.utils.ArgumentFileBuilder;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
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
@Named
public final class JvmPluginResolver {

  private static final Set<String> ALLOWED_SCOPES = Set.of("compile", "runtime", "system");
  private static final List<String> DEFAULT_JVM_ARGS = List.of();
  private static final List<String> DEFAULT_JVM_CONFIG_ARGS = List.of(
      "-Xshare:auto",
      "-XX:+TieredCompilation",
      "-XX:TieredStopAtLevel=1"
  );
  private static final Logger log = LoggerFactory.getLogger(JvmPluginResolver.class);

  private final HostSystem hostSystem;
  private final MavenArtifactPathResolver artifactPathResolver;
  private final TemporarySpace temporarySpace;
  private final SystemPathBinaryResolver pathResolver;

  @Inject
  public JvmPluginResolver(
      HostSystem hostSystem,
      MavenArtifactPathResolver artifactPathResolver,
      TemporarySpace temporarySpace,
      SystemPathBinaryResolver pathResolver
  ) {
    this.hostSystem = hostSystem;
    this.artifactPathResolver = artifactPathResolver;
    this.temporarySpace = temporarySpace;
    this.pathResolver = pathResolver;
  }

  public Collection<? extends ResolvedProtocPlugin> resolveMavenPlugins(
      Collection<? extends MavenProtocPlugin> pluginDescriptors
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedProtocPlugin>();
    var index = 0;

    for (var pluginDescriptor : pluginDescriptors) {
      if (pluginDescriptor.isSkip()) {
        log.info("Skipping plugin {}", pluginDescriptor);
        continue;
      }

      var resolvedPlugin = resolveMavenPlugin(pluginDescriptor, index++);
      resolvedPlugins.add(resolvedPlugin);
    }
    return Collections.unmodifiableList(resolvedPlugins);
  }

  private ResolvedProtocPlugin resolveMavenPlugin(
      MavenProtocPlugin pluginDescriptor,
      int index
  ) throws ResolutionException {

    log.debug(
        "Resolving JVM-based Maven protoc plugin {} and generating OS-specific boostrap scripts",
        pluginDescriptor
    );

    var id = hashPlugin(pluginDescriptor, index);
    var argLine = buildArgLine(pluginDescriptor);
    var javaPath = hostSystem.getJavaExecutablePath();
    var scratchDir = temporarySpace.createTemporarySpace("plugins", "jvm", id);

    var scriptPath = hostSystem.isProbablyWindows()
        ? writeWindowsScripts(javaPath, scratchDir, argLine)
        : writePosixScripts(javaPath, scratchDir, argLine);

    return ImmutableResolvedProtocPlugin
        .builder()
        .id(id)
        .path(scriptPath)
        .options(pluginDescriptor.getOptions())
        .order(pluginDescriptor.getOrder())
        .build();
  }

  private String hashPlugin(MavenProtocPlugin plugin, int index) {
    // GH-421: Ensure duplicate plugin definitions retain a unique name
    // when in the same execution, rather than trampling over eachother's
    // files.
    return Digests.sha1(plugin.toString()) + "-" + index;
  }

  private ArgumentFileBuilder buildArgLine(MavenProtocPlugin plugin) throws ResolutionException {
    // Expectation: this always has at least one item in it, and the first item is the plugin
    // artifact itself.
    var dependencies = artifactPathResolver
        .resolveDependencies(
            List.of(plugin),
            DependencyResolutionDepth.TRANSITIVE,
            ALLOWED_SCOPES,
            false,
            true
        )
        .stream()
        .collect(Collectors.toUnmodifiableList());

    var args = new ArgumentFileBuilder();

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

    requireNonNullElse(plugin.getJvmConfigArgs(), DEFAULT_JVM_CONFIG_ARGS)
        .stream()
        .filter(checkValidJvmConfigArg(plugin))
        .forEach(args::add);

    args.add(determineMainClass(plugin, dependencies.get(0)));

    requireNonNullElse(plugin.getJvmArgs(), DEFAULT_JVM_ARGS)
        .forEach(args::add);

    return args;
  }

  private String determineMainClass(
      MavenProtocPlugin plugin,
      Path pluginPath
  ) throws ResolutionException {
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
  ) throws ResolutionException {
    try (
        var zip = FileUtils.openZipAsFileSystem(pluginPath);
        var manifestStream = Files.newInputStream(zip.getPath("META-INF", "MANIFEST.MF"))
    ) {
      return new Manifest(manifestStream)
          .getMainAttributes()
          .getValue("Main-Class");
    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to determine the main class in the MANIFEST.MF for JAR corresponding to "
              + pluginPath
              + ": an unexpected IO error occurred",
          ex
      );
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

  private List<Path> findJavaModules(List<Path> paths) {
    // ModuleFinder may be an overkill, so we might eventually want to just
    // discard this and use some other method (e.g. reading the manifest
    // for Automatic-Module-Name, and checking for the presence of a
    // module-info.class in the root and in META-INF/versions child directories.
    return ModuleFinder.of(paths.toArray(Path[]::new))
        .findAll()
        .stream()
        .map(ModuleReference::location)
        .flatMap(Optional::stream)
        .map(Path::of)
        .map(FileUtils::normalize)
        .peek(modulePath -> log.trace("Looks like {} is a JPMS module!", modulePath))
        // Sort as the order of output is arbitrary, and this ensures reproducible builds.
        .sorted(Comparator.comparing(Path::toString))
        .collect(Collectors.toUnmodifiableList());
  }

  private Predicate<String> checkValidJvmConfigArg(MavenProtocPlugin plugin) {
    return arg -> {
      // JVM args must begin with a hyphen, otherwise Java may interpret
      // them as being the application entrypoint class and accidentally
      // clobber the invocation.
      if (arg.startsWith("-") && arg.length() > 1) {
        return true;
      }

      log.warn(
          "Dropping illegal JVM argument '{}' for Maven plugin {}",
          arg,
          plugin.getArtifactId()
      );
      return false;
    };
  }

  private Path writePosixScripts(
      Path javaExecutable,
      Path scratchDir,
      ArgumentFileBuilder argFileBuilder
  ) throws ResolutionException {
    var sh = pathResolver.resolve("sh").orElseThrow();
    var argumentFile = writeArgumentFile(StandardCharsets.UTF_8, scratchDir, argFileBuilder);

    var script = scratchDir.resolve("invoke.sh");
    try {
      try (var writer = Files.newBufferedWriter(script, StandardCharsets.UTF_8)) {
        writer.append("#!").append(sh.toString()).append('\n')
            .append("set -o errexit\n");
        quoteShellArg(writer, javaExecutable.toString());
        writer.append(' ');
        quoteShellArg(writer, "@" + argumentFile);
        writer.append('\n');
      }

      FileUtils.makeExecutable(script);
    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to create plugin invocation shell script at " + script + ": an IO error occurred",
          ex
      );
    }
    return script;
  }

  private Path writeWindowsScripts(
      Path javaExecutable,
      Path scratchDir,
      ArgumentFileBuilder argFileBuilder
  ) throws ResolutionException {
    var argumentFile = writeArgumentFile(StandardCharsets.ISO_8859_1, scratchDir, argFileBuilder);

    var script = scratchDir.resolve("invoke.bat");
    try (var writer = Files.newBufferedWriter(script, StandardCharsets.ISO_8859_1)) {
      writer.append("@echo off\r\n");
      quoteBatchArg(writer, javaExecutable.toString());
      writer.append(" ");
      quoteBatchArg(writer, "@" + argumentFile);
      writer.append("\r\n");
    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to create plugin invocation batch script at " + script + ": an IO error occurred",
          ex
      );
    }

    return script;
  }

  private Path writeArgumentFile(
      Charset charset,
      Path scratchDir,
      ArgumentFileBuilder argumentFileBuilder
  ) throws ResolutionException {
    var argumentFile = scratchDir.resolve("args.txt");

    try (var writer = Files.newBufferedWriter(argumentFile, charset)) {
      argumentFileBuilder.writeTo(writer);
    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to write argument file at " + argumentFile + ": an IO error occurred",
          ex
      );
    }
    return argumentFile;
  }

  private void quoteShellArg(Appendable appendable, String arg) throws IOException {
    // POSIX file names can be a bit more complicated and may need escaping
    // in certain edge cases to remain valid.
    appendable.append('\'');
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      switch (c) {
        case '\\':
          appendable.append("\\\\");
          break;
        case '\'':
          appendable.append("'\"'\"'");
          break;
        case '\n':
          appendable.append("'$'\\n''");
          break;
        case '\r':
          appendable.append("'$'\\r''");
          break;
        case '\t':
          appendable.append("'$'\\t''");
          break;
        default:
          appendable.append(c);
          break;
      }
    }
    appendable.append('\'');
  }

  private void quoteBatchArg(Appendable appendable, String arg) throws IOException {
    // All the escapable characters in batch files other than quotes
    // are considered to be illegal characters in Windows file names,
    // so we can make the assumption that we don't need to change much
    // here.
    appendable.append("\"")
        .append(arg.replaceAll("\"", "\"\"\""))
        .append("\"");
  }
}
