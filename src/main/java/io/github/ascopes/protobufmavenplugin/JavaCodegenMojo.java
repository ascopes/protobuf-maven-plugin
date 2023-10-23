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

import io.github.ascopes.protobufmavenplugin.resolver.MavenProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.PathProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolver.ProtocResolver;
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
    threadSafe = true
)
public final class JavaCodegenMojo extends AbstractMojo {

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

  /**
   * The version of protoc to use.
   *
   * <p>Only relevant for the {@code MAVEN} protoc resolver.
   */
  @Parameter(defaultValue = "LATEST")
  private String version;

  /**
   * The version of protoc to use.
   *
   * <p>Only relevant for the {@code PATH} protoc resolver.
   */
  @Parameter(defaultValue = "protoc")
  private String executableName;

  /**
   * How to resolve the protoc binary.
   */
  @Parameter(defaultValue = "MAVEN")
  private ResolverKind resolverKind;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var protocResolver = buildProtocResolver();
  }

  private ProtocResolver buildProtocResolver() {
    switch (resolverKind) {
      case MAVEN:
        return new MavenProtocResolver(version);
      case PATH:
        return new PathProtocResolver(executableName);
      default:
        throw new IllegalStateException("unsupported resolver kind");
    }
  }

  /**
   * Valid protoc resolver kinds.
   */
  public enum ResolverKind {
    /**
     * Resolve protoc from the local/remote Maven repository.
     */
    MAVEN,

    /**
     * Resolve protoc from the {@code PATH} environment variable.
     */
    PATH,
  }
}
