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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;

/**
 * Generate Java source code from Protobuf source file definitions.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate-java",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresOnline = true,
    threadSafe = true
)
public class JavaCodegenMojo extends AbstractMojo {

  /**
   * The repository system.
   */
  @Component
  private RepositorySystem repositorySystem;

  /**
   * The Maven session that is in use.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  public JavaCodegenMojo() {
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
  }
}
