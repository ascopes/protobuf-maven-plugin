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
package io.github.ascopes.protobufmavenplugin.protoc.targets;

import io.github.ascopes.protobufmavenplugin.fs.AbstractTemporaryLocationProvider;
import io.github.ascopes.protobufmavenplugin.plugins.ImmutableResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.protoc.ImmutableProtocInvocation;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocInvocation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transformer of {@link ProtocInvocation} requests that moves executables to a
 * sanctioned user-requested location.
 *
 * <p>The use case for this is for users working in overly restrictive corporate
 * environments with various company-mandated facilities that prevent execution
 * of binaries and scripts from outside very specific locations.
 *
 * <p>If no sanctioned location has been specified, then nothing is changed.
 *
 * <p>In the event a sanctioned location is specified, then any targets will be
 * rebuilt with a new executable location, and any respective files will be copied
 * across to that location.
 *
 * @author Ashley Scopes
 * @since 3.9.0
 */
@Description("Moves executable targets to a user-specified location for corporate environments")
@MojoExecutionScoped
@Named
public final class SanctionedExecutableTransformer extends AbstractTemporaryLocationProvider {

  private static final Logger log = LoggerFactory.getLogger(SanctionedExecutableTransformer.class);

  private final MavenProject mavenProject;

  @Inject
  public SanctionedExecutableTransformer(
      MavenProject mavenProject,
      MojoExecution mojoExecution
  ) {
    super(mojoExecution);
    this.mavenProject = mavenProject;
  }

  public ProtocInvocation transform(ProtocInvocation protocInvocation) throws IOException {
    var sanctionedPath = protocInvocation.getSanctionedExecutablePath();

    if (sanctionedPath == null) {
      log.debug(
          "No sanctioned executable location specified; will not intercept the protoc invocation"
      );
      return protocInvocation;
    }

    sanctionedPath = sanctionedPath
        .resolve(mavenProject.getGroupId())
        .resolve(mavenProject.getArtifactId());
    sanctionedPath = resolveAndCreateDirectory(sanctionedPath);

    Files.createDirectories(sanctionedPath);

    log.warn(
        "A user-specified sanctioned execution location of \"{}\" was provided. All executables "
            + "managed by this plugin invocation will be moved to that location. Your "
            + "mileage may vary, and it will be up to you to manage cleaning up this path.",
        sanctionedPath
    );

    return ImmutableProtocInvocation.builder()
        .from(protocInvocation)
        .protocPath(transfer(sanctionedPath, "protoc-", protocInvocation.getProtocPath()))
        .targets(transformTargets(sanctionedPath, protocInvocation))
        .build();
  }

  private SortedSet<ProtocTarget> transformTargets(
      Path sanctionedPath,
      ProtocInvocation invocation
  ) throws IOException {
    var transformedTargets = new TreeSet<ProtocTarget>();

    for (var target : invocation.getTargets()) {
      if (target instanceof PluginProtocTarget) {
        var pluginTarget = (PluginProtocTarget) target;
        var prefix = "plugin-" + transformedTargets.size() + "-";

        target = ImmutablePluginProtocTarget.builder()
            .from(pluginTarget)
            .plugin(ImmutableResolvedProtocPlugin.builder()
                .from(pluginTarget.getPlugin())
                .path(transfer(
                    sanctionedPath,
                    prefix,
                    pluginTarget.getPlugin().getPath()
                ))
                .build())
            .build();
      }

      transformedTargets.add(target);
    }

    return Collections.unmodifiableSortedSet(transformedTargets);
  }

  private Path transfer(
      Path sanctionedPath,
      String prefix,
      Path existingFile
  ) throws IOException {
    var newFile = sanctionedPath.resolve(prefix + existingFile.getFileName().toString());

    log.debug("Copying \"{}\" to \"{}\"", existingFile, newFile);

    return Files.copy(
        existingFile,
        newFile,
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING
    );
  }
}
