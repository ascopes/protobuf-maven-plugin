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

import io.github.ascopes.protobufmavenplugin.protoc.targets.DescriptorFileProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.LanguageProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.PluginProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.ProtocTarget;
import io.github.ascopes.protobufmavenplugin.utils.ArgumentFileBuilder;
import io.github.ascopes.protobufmavenplugin.utils.TeeWriter;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor for {@code protoc} commands.
 *
 * @author Ashley Scopes
 */
@Description("Executes protoc in a subprocess, intercepting any outputs")
@MojoExecutionScoped
@Named
public final class ProtocExecutor {

  private static final Logger log = LoggerFactory.getLogger(ProtocExecutor.class);
  private final TemporarySpace temporarySpace;

  @Inject
  public ProtocExecutor(TemporarySpace temporarySpace) {
    this.temporarySpace = temporarySpace;
  }

  public boolean invoke(ProtocInvocation invocation) throws IOException {
    var argumentFileBuilder = createArgumentFileBuilder(invocation);
    var argumentFile = writeArgumentFile(argumentFileBuilder);

    log.info("Invoking protoc (enable debug logs for more details)");
    log.debug("Protoc binary is located at {}", invocation.getProtocPath());
    log.debug("Protoc argument file:\n{},", argumentFileBuilder);

    var procBuilder = new ProcessBuilder(invocation.getProtocPath().toString(), "@" + argumentFile);
    procBuilder.environment().putAll(System.getenv());

    try {
      return runProcess(procBuilder);

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      var newEx = new InterruptedIOException("Execution was interrupted");
      newEx.initCause(ex);
      throw newEx;
    }
  }

  private ArgumentFileBuilder createArgumentFileBuilder(ProtocInvocation invocation) {
    return new ArgumentFileBuilder()
        .addIfTrue(invocation.isFatalWarnings(), "--fatal_warnings")
        .applyForEach(invocation.getTargets(), this::applyProtocTargetArguments)
        .applyForEach(invocation.getSourcePaths(), this::applySourcePathArgument)
        .applyForEach(invocation.getImportPaths(), this::applyImportPathArgument);
  }

  private void applyProtocTargetArguments(ArgumentFileBuilder builder, ProtocTarget target) {
    if (target instanceof DescriptorFileProtocTarget) {
      applyDescriptorFileProtocTargetArguments(builder, (DescriptorFileProtocTarget) target);
    } else if (target instanceof LanguageProtocTarget) {
      applyLanguageProtocTargetArguments(builder, (LanguageProtocTarget) target);
    } else if (target instanceof PluginProtocTarget) {
      applyPluginProtocTargetArguments(builder, (PluginProtocTarget) target);
    } else {
      throw new IllegalStateException("Unknown target " + target);
    }
  }

  private void applyDescriptorFileProtocTargetArguments(
      ArgumentFileBuilder builder, 
      DescriptorFileProtocTarget target
  ) {
    builder.add("--descriptor_set_out=" + target.getOutputFile())
        .addIfTrue(target.isIncludeImports(), "--include_imports")
        .addIfTrue(target.isIncludeSourceInfo(), "--include_source_info")
        .addIfTrue(target.isRetainOptions(), "--retain_options");
  }

  private void applyLanguageProtocTargetArguments(
      ArgumentFileBuilder builder, 
      LanguageProtocTarget target
  ) {
    var flag = "--" + target.getLanguage().getFlagName() + "_out="
        + (target.isLite() ? "lite:" : "")
        + target.getOutputPath();
    builder.add(flag);
  }

  private void applyPluginProtocTargetArguments(
      ArgumentFileBuilder builder, 
      PluginProtocTarget target
  ) {
    var plugin = target.getPlugin();
    builder
        .add("--plugin=protoc-gen-" + plugin.getId() + "=" + plugin.getPath())
        .add("--" + plugin.getId() + "_out=" + target.getOutputPath());
    plugin.getOptions()
        .map(options -> "--" + plugin.getId() + "_opt=" + options)
        .ifPresent(builder::add);
  }

  private void applyImportPathArgument(ArgumentFileBuilder builder, Path importPath) {
    builder.add("--proto_path=" + importPath);
  }

  private void applySourcePathArgument(ArgumentFileBuilder builder, Path sourcePath) {
    builder.add(sourcePath);
  }

  private Path writeArgumentFile(ArgumentFileBuilder argumentFileBuilder) throws IOException {
    var file = temporarySpace.createTemporarySpace("protoc").resolve("args.txt");
    log.debug("Writing to protoc argument file at {}", file);

    var writer = new TeeWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
    try (writer) {
      argumentFileBuilder.writeToProtocArgumentFile(writer);
    }

    log.debug("Written arguments were:\n{}", writer);
    return file;
  }

  private boolean runProcess(ProcessBuilder procBuilder) throws InterruptedException, IOException {
    final var startTimeNs = System.nanoTime();

    log.trace("Starting protoc subprocess");
    final var proc = procBuilder.start();

    final var stdoutRedirector = new OutputRedirectorDaemon(
        "protoc - stdout",
        proc.pid(),
        proc.getInputStream(),
        log::info
    );
    final var stderrRedirector = new OutputRedirectorDaemon(
        "protoc - stderr",
        proc.pid(),
        proc.getErrorStream(),
        log::warn
    );

    log.trace("Waiting for protoc to exit...");
    final var exitCode = proc.waitFor();
    final var elapsedTimeMs = (System.nanoTime() - startTimeNs) / 1_000_000L;

    // Ensure we've flushed the logs through before continuing.
    log.trace("Waiting for stdout and stderr redirectors to terminate...");
    stdoutRedirector.await();
    stderrRedirector.await();
    log.trace("Stdout and stderr redirectors terminated");

    if (exitCode == 0) {
      log.info(
          "protoc (pid {}) returned exit code 0 (success) after {}ms",
          proc.pid(),
          elapsedTimeMs
      );
      return true;

    } else {
      log.error(
          "protoc (pid {}) returned exit code {} (error) after {}ms",
          proc.pid(),
          exitCode,
          elapsedTimeMs
      );
      return false;
    }
  }
}
