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
package io.github.ascopes.protobufmavenplugin.java;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
 * Implementation of an executable factory that converts Java apps to executables by making an
 * OS-specific shell script and argline file.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Description("Converts Java applications into native executable formats.")
@MojoExecutionScoped
@Named
final class JavaAppToExecutableScriptFactory implements JavaAppToExecutableFactory {

  private static final List<String> DEFAULT_ARGS = List.of();
  private static final List<String> DEFAULT_JVM_ARGS = List.of(
      "-Xshare:auto",
      "-XX:+TieredCompilation",
      "-XX:TieredStopAtLevel=1"
  );
  private static final Logger log = LoggerFactory.getLogger(JavaAppToExecutableScriptFactory.class);

  private final HostSystem hostSystem;
  private final TemporarySpace temporarySpace;
  private final SystemPathBinaryResolver pathResolver;

  @Inject
  JavaAppToExecutableScriptFactory(
      HostSystem hostSystem,
      TemporarySpace temporarySpace,
      SystemPathBinaryResolver pathResolver
  ) {
    this.hostSystem = hostSystem;
    this.temporarySpace = temporarySpace;
    this.pathResolver = pathResolver;
  }

  @Override
  public Path toExecutable(JavaApp app) throws ResolutionException {
    var argLine = buildArgLine(app);
    var javaPath = hostSystem.getJavaExecutablePath();
    var scratchDir = temporarySpace.createTemporarySpace("java-apps", app.getUniqueName());

    log.debug("Arguments for JVM app \"{}\" are:\n{}", app, argLine);

    return hostSystem.isProbablyWindows()
        ? writeWindowsScripts(javaPath, scratchDir, argLine)
        : writePosixScripts(javaPath, scratchDir, argLine);
  }

  private ArgumentFileBuilder buildArgLine(JavaApp app) throws ResolutionException {
    var args = new ArgumentFileBuilder();

    // Caveat: we currently ignore the Class-Path JAR manifest entry. Not sure why we would want
    // to be using that here though, so I am leaving it unimplemented until such a time that someone
    // requests it.
    args.add("-classpath");
    args.add(buildJavaPath(app.getDependencies()));

    var modules = findJavaModules(app.getDependencies());

    if (!modules.isEmpty()) {
      args.add("--module-path");
      args.add(buildJavaPath(modules));
    }

    requireNonNullElse(app.getJvmConfigArgs(), DEFAULT_JVM_ARGS)
        .stream()
        .filter(checkValidJvmConfigArg(app))
        .forEach(args::add);

    // First dependency is always the entrypoint due to how Aether resolves
    // dependencies internally.
    args.add(determineMainClass(app));

    requireNonNullElse(app.getJvmArgs(), DEFAULT_ARGS)
        .forEach(args::add);

    return args;
  }

  private Predicate<String> checkValidJvmConfigArg(JavaApp app) {
    return arg -> {
      // JVM args must begin with a hyphen and be greater than zero in size,
      // otherwise Java may interpret them as being the application entrypoint
      // class and accidentally clobber the invocation.
      if (arg.startsWith("-") && arg.length() > 1) {
        return true;
      }

      log.warn(
          "Dropping illegal JVM argument \"{}\" for app \"{}\"",
          arg,
          app
      );
      return false;
    };
  }

  private String determineMainClass(JavaApp app) throws ResolutionException {
    // GH-363: It appears that we have to avoid calling `java -jar` when running JARs as the
    // classpath argument is totally ignored by Java in this case, meaning no dependencies
    // get loaded correctly, and we get NoClassDefFoundErrors being raised for non-shaded JARs.
    // This means we have to explicitly provide the main class entrypoint due to the way we
    // have to invoke the java executable, and this in turn means we have to do some sniffing
    // around to make a best-effort guess at what the main class really is... which is not very
    // fun.

    if (app.getMainClass() != null) {
      // The user provided it explicitly in the configuration, so trust their judgement.
      log.debug("Using user-provided main class for app \"{}\": {}", app, app.getMainClass());
      return app.getMainClass();
    }

    var firstPath = app.getDependencies().iterator().next();

    // If we don't have a JAR, we can't really guess the main class, as Maven will not emit
    // the MANIFEST.MF directly in a place we can see it. I guess we could try and scrape the
    // POM of the project but that is likely to be awkward and at best flaky due to the numerous
    // ways this attribute could be injected into any manifest. Let's just keep it simple for now.
    if (!Files.isDirectory(firstPath)) {
      var mainClass = tryToDetermineMainClassFromJarManifest(firstPath);

      if (mainClass == null) {
        // Not my fault! Please provide a Main-Class attribute on the JAR instead...
        log.warn(
            "No Main-Class manifest attribute found in \"{}\", this is probably a bug with how that"
                + " JAR was built",
            firstPath
        );
      } else {
        log.debug(
            "Determined main class to be \"{}\" from manifest for \"{}\"",
            mainClass,
            firstPath
        );
        return mainClass;
      }
    }

    throw new ResolutionException(
        "No main class was described for \""
            + firstPath
            + "\", please provide an explicit "
            + "'mainClass' attribute when configuring this component."
    );
  }

  private @Nullable String tryToDetermineMainClassFromJarManifest(
      Path pluginPath
  ) throws ResolutionException {
    try (
        var zip = FileUtils.openZipAsFileSystem(pluginPath);
        var manifestInputStream = FileUtils.newBufferedInputStream(
            zip.getPath("META-INF", "MANIFEST.MF"))
    ) {
      return new Manifest(manifestInputStream)
          .getMainAttributes()
          .getValue("Main-Class");
    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to determine the main class in the MANIFEST.MF for JAR corresponding to \""
              + pluginPath
              + "\":" + ex,
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
    // module-info.class in the root and in META-INF/versions child directories).
    return ModuleFinder.of(paths.toArray(Path[]::new))
        .findAll()
        .stream()
        .map(ModuleReference::location)
        .flatMap(Optional::stream)
        .map(Path::of)
        .map(FileUtils::normalize)
        // Sort as the order of output is arbitrary, and this ensures reproducible builds.
        .sorted(Comparator.comparing(Path::toString))
        .toList();
  }

  private Path writePosixScripts(
      Path javaExecutable,
      Path scratchDir,
      ArgumentFileBuilder argFileBuilder
  ) throws ResolutionException {
    var sh = pathResolver.resolve("sh").orElseThrow();
    var argumentFile = writeArgumentFile(StandardCharsets.UTF_8, scratchDir, argFileBuilder);

    var script = scratchDir.resolve("invoke.sh");
    writeAndPropagateExceptions(script, StandardCharsets.UTF_8, true, writer -> {
      writer.append("#!")
          .append(sh.toString())
          .append('\n')
          .append("set -o errexit\n");
      quoteShellArg(writer, javaExecutable.toString());
      writer.append(' ');
      quoteShellArg(writer, "@" + argumentFile);
      writer.append('\n');
    });

    return script;
  }

  private Path writeWindowsScripts(
      Path javaExecutable,
      Path scratchDir,
      ArgumentFileBuilder argFileBuilder
  ) throws ResolutionException {
    var argumentFile = writeArgumentFile(StandardCharsets.ISO_8859_1, scratchDir, argFileBuilder);

    var script = scratchDir.resolve("invoke.bat");
    writeAndPropagateExceptions(script, StandardCharsets.ISO_8859_1, false, writer -> {
      writer.append("@echo off\r\n");
      quoteBatchArg(writer, javaExecutable.toString());
      writer.append(" ");
      quoteBatchArg(writer, "@" + argumentFile);
      writer.append("\r\n");
    });

    return script;
  }

  private Path writeArgumentFile(
      Charset charset,
      Path scratchDir,
      ArgumentFileBuilder argumentFileBuilder
  ) throws ResolutionException {
    var argumentFile = scratchDir.resolve("args.txt");
    writeAndPropagateExceptions(
        argumentFile,
        charset,
        false,
        argumentFileBuilder::writeToJavaArgumentFile
    );
    return argumentFile;
  }

  private void quoteShellArg(Appendable appendable, String arg) throws IOException {
    // POSIX file names can be a bit more complicated and may need escaping
    // in certain edge cases to remain valid.
    appendable.append('\'');
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      switch (c) {
        case '\\' -> appendable.append("\\\\");
        case '\'' -> appendable.append("'\"'\"'");
        case '\n' -> appendable.append("'$'\\n''");
        case '\r' -> appendable.append("'$'\\r''");
        case '\t' -> appendable.append("'$'\\t''");
        default -> appendable.append(c);
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

  private static void writeAndPropagateExceptions(
      Path file,
      Charset charset,
      boolean makeExecutable,
      WriteOperation writeOperation
  ) throws ResolutionException {
    try {
      try (var writer = Files.newBufferedWriter(file, charset)) {
        writeOperation.writeTo(writer);
      }
      if (makeExecutable) {
        FileUtils.makeExecutable(file);
      }
    } catch (IOException ex) {
      throw new ResolutionException("An unexpected IO error occurred while writing to " + file, ex);
    }
  }

  @FunctionalInterface
  private interface WriteOperation {

    void writeTo(BufferedWriter writer) throws IOException;
  }
}
