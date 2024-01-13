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

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.system.Digests;
import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import io.github.ascopes.protobufmavenplugin.system.HostSystem;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;

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
public class JvmPluginResolver {

  private static final Set<String> SCOPES = Set.of("compile", "runtime", "system");

  private final MavenProject mavenProject;
  private final HostSystem hostSystem;
  private final MavenDependencyPathResolver dependencyPathResolver;

  @Inject
  public JvmPluginResolver(
      MavenProject mavenProject,
      HostSystem hostSystem,
      MavenDependencyPathResolver dependencyPathResolver
  ) {
    this.mavenProject = mavenProject;
    this.hostSystem = hostSystem;
    this.dependencyPathResolver = dependencyPathResolver;
  }

  public Collection<ResolvedPlugin> resolveAll(
      MavenSession session,
      Collection<DependableCoordinate> plugins
  ) throws IOException, ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedPlugin>();
    for (var plugin : plugins) {
      resolvedPlugins.add(resolve(session, plugin));
    }
    return resolvedPlugins;
  }

  private ResolvedPlugin resolve(
      MavenSession session,
      DependableCoordinate plugin
  ) throws IOException, ResolutionException {
    var scriptNamePrefix = pluginIdDigest(plugin);
    var argLine = resolveAndBuildArgLine(session, plugin);

    Path scriptPath;
    if (hostSystem.isProbablyWindows()) {
      scriptPath = writeWindowsBatchScript(scriptNamePrefix, argLine);
    } else {
      scriptPath = writeShellScript(scriptNamePrefix, argLine);
    }

    return ImmutableResolvedPlugin
        .builder()
        .id(UUID.randomUUID().toString())
        .path(scriptPath)
        .build();
  }

  private List<String> resolveAndBuildArgLine(
      MavenSession session,
      DependableCoordinate pluginDependencyCoordinate
  ) throws ResolutionException {

    // Resolve dependencies first.
    var dependencyIterator = dependencyPathResolver
        .resolveDependencyTreePaths(session, SCOPES, pluginDependencyCoordinate)
        .iterator();

    // First dependency is always the thing we actually want to execute.
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

  private String pluginIdDigest(DependableCoordinate dependableCoordinate) {
    var digestableString = String.join(
        "$",
        requireNonNullElse(dependableCoordinate.getGroupId(), "@"),
        requireNonNullElse(dependableCoordinate.getArtifactId(), "@"),
        requireNonNullElse(dependableCoordinate.getVersion(), "@"),
        requireNonNullElse(dependableCoordinate.getType(), "@"),
        requireNonNullElse(dependableCoordinate.getClassifier(), "@")
    );
    return Digests.sha1(digestableString);
  }

  private Path resolvePluginScriptPath() throws IOException {
    var path = Path.of(mavenProject.getBuild().getDirectory())
        .resolve("protobuf-maven-plugin")
        .resolve("jvm-plugins");
    return Files.createDirectories(path);
  }

  private Path writeWindowsBatchScript(
      String scriptNamePrefix,
      List<String> argLine
  ) throws IOException {
    var fullScriptPath = resolvePluginScriptPath().resolve(scriptNamePrefix + ".bat");

    var script = new StringBuilder()
        .append("@echo off\r\n");
    for (var arg : argLine) {
      script.append(quoteBatchArg(arg)).append(' ');
    }
    script.append("\r\n");

    Files.writeString(fullScriptPath, script, Charset.defaultCharset());
    return fullScriptPath;
  }

  private String quoteBatchArg(String arg) {
    var sb = new StringBuilder();
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      switch (c) {
        case '\\':
        case '"':
        case '\'':
        case ' ':
        case '\r':
        case '\t':
        case '^':
        case '&':
        case '<':
        case '>':
        case '|':
          sb.append('^');
      }

      sb.append(c);
    }

    return sb.toString();
  }

  private Path writeShellScript(
      String scriptNamePrefix,
      List<String> argLine
  ) throws IOException {
    var fullScriptPath = resolvePluginScriptPath().resolve(scriptNamePrefix + ".sh");

    var script = new StringBuilder()
        .append("#!/usr/bin/env sh\n")
        .append("set -eux\n");
    for (var arg : argLine) {
      script.append(quoteShellArg(arg)).append(' ');
    }
    script.append('\n');

    Files.writeString(fullScriptPath, script, Charset.defaultCharset());
    FileUtils.makeExecutable(fullScriptPath);
    return fullScriptPath;
  }

  private String quoteShellArg(String arg) {
    var sb = new StringBuilder("'");
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      if (c == '\'') {
        sb.append("'\"'\"'");
      } else {
        sb.append(c);
      }
    }
    return sb.append('\'').toString();
  }
}