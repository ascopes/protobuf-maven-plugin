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
package io.github.ascopes.protobufmavenplugin.fs;

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to provide access to temporary spaces on the file system to use during builds.
 *
 * @author Ashley Scopes
 */
@Description("Manages build-scoped reusable temporary directories for processing")
@MojoExecutionScoped
@Named
public final class TemporarySpace {

  private static final String FRAG = "protobuf-maven-plugin";
  private static final Logger log = LoggerFactory.getLogger(TemporarySpace.class);

  private final MavenProject mavenProject;
  private final MojoExecution mojoExecution;

  @Inject
  public TemporarySpace(MavenProject mavenProject, MojoExecution mojoExecution) {
    this.mavenProject = mavenProject;
    this.mojoExecution = mojoExecution;
  }

  public Path createTemporarySpace(String... bits) {
    // GH-488: Execution ID and goal can potentially be null, e.g. in Quarkus dev mode, so
    // default to a semi-sensible value to prevent a NullPointerException.
    var goal = Objects.requireNonNullElse(
        mojoExecution.getGoal(),
        "unknown-goal"
    );
    var executionId = Objects.requireNonNullElse(
        mojoExecution.getExecutionId(),
        "unknown-execution-id"
    );

    // Use a digest of the components to make bits from, rather than subdirectories.
    // This avoids https://bugs.openjdk.org/browse/JDK-8315405 and
    // https://bugs.openjdk.org/browse/JDK-8315405 on Windows, which effectively limit the
    // fully qualified path of a Windows executable passed to ProcessBuilder to 260
    // characters in length... something we easily hit during integration testing, amongst
    // other things.
    //
    // Eventually if OpenJDK fix this, we should revert this change to keep the code easier
    // to debug for bug reports. For now, it gets us out of a mess though.
    var subDirectoryBits = Stream
        .concat(Stream.of(goal, executionId), Stream.of(bits))
        .toArray(String[]::new);
    var subDirectoryName = Digest.compute("SHA-256", String.join("\0", subDirectoryBits))
        .toHexString();

    var dir = Path.of(mavenProject.getBuild().getDirectory())
        .resolve(FRAG)
        .resolve(subDirectoryName);

    log.trace(
        "Creating temporary directory at '{}' for fragment <{}> if it does not already exist...",
        dir,
        String.join(", ", subDirectoryBits)
    );

    // This should be concurrent-safe as it will not break if the directory already exists unless
    // the directory is instead a regular file.
    try {
      return Files.createDirectories(dir);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to create temporary directory!", ex);
    }
  }
}
