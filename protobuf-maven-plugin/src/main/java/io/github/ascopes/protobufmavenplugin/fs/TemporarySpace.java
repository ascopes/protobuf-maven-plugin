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
package io.github.ascopes.protobufmavenplugin.fs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Description;

/**
 * Helper to provide access to temporary spaces on the file system to use during builds.
 *
 * <p>These temporary spaces reside within the project {@code target/} directory.
 *
 * @author Ashley Scopes
 */
@Description("Manages build-scoped reusable temporary directories for processing")
@MojoExecutionScoped
@Named
public final class TemporarySpace extends AbstractTemporaryLocationProvider {

  private final MavenProject mavenProject;

  @Inject
  public TemporarySpace(MavenProject mavenProject, MojoExecution mojoExecution) {
    super(mojoExecution);
    this.mavenProject = mavenProject;
  }

  public Path createTemporarySpace(String... bits) {
    try {
      var baseDir = Path.of(mavenProject.getBuild().getDirectory());
      return resolveAndCreateDirectory(baseDir, bits);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to create temporary location!", ex);
    }
  }
}
