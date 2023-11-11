/*
 * Copyright (C) 2023, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin;

import io.github.ascopes.protobufmavenplugin.executor.DefaultProtocExecutor;
import io.github.ascopes.protobufmavenplugin.resolver.MavenProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.PathProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.ProtocResolutionException;
import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

/**
 * Base Mojo to generate protobuf sources.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractCodegenMojo extends AbstractMojo {

  /**
   * The artifact resolver.
   */
  @Component
  private ArtifactResolver artifactResolver;

  /**
   * The Maven session that is in use.
   *
   * <p>This is passed in by Maven automatically, so can be ignored.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  /**
   * The version of protoc to use.
   *
   * <p>This can be a static version, "{@code LATEST}", or a valid Maven version range (such as
   * "{@code [3.5.0,4.0.0)}"). It is recommended to use a static version to ensure your builds are
   * reproducible.
   *
   * <p>Ignored if {@code usePath} is set to {@code true}.
   *
   * <p>If not specified explicitly, then this defaults to searching for the latest version that
   * is available on the Maven remote repository.
   */
  @Parameter(defaultValue = "LATEST")
  private String version;

  /**
   * If set to {@code true}, then instruct the plugin to look on the system {@code $PATH} for a
   * {@code protoc} executable rather than querying the Maven remote repository.
   */
  @Parameter(defaultValue = "false")
  private boolean usePath;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var protocPath = resolveProtocPath();
    var executor = new DefaultProtocExecutor(protocPath);
  }

  private Path resolveProtocPath() throws MojoExecutionException, MojoFailureException {
    try {
      var resolver = usePath
          ? new PathProtocResolver()
          : new MavenProtocResolver(version, artifactResolver, session);

      return resolver.resolveProtoc();

    } catch (ProtocResolutionException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
  }
}
