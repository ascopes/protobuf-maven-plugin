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

package io.github.ascopes.protobufmavenplugin.resolve.grpc;

import io.github.ascopes.protobufmavenplugin.resolve.AbstractMavenResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

/**
 * Resolver for the Kotlin GRPC plugin for {@code protoc} which queries Maven Central.
 *
 * @author Ashley Scopes
 */
public final class MavenGrpcKotlinPluginResolver extends AbstractMavenResolver {

  /**
   * Initialise this resolver.
   *
   * @param version          the version/version range to resolve.
   * @param artifactResolver the Maven artifact resolver to use.
   * @param session          the current Maven session.
   */
  public MavenGrpcKotlinPluginResolver(
      String version,
      ArtifactResolver artifactResolver,
      MavenSession session
  ) {
    super(version, artifactResolver, session, new MavenGrpcKotlinPluginCoordinateFactory());
  }
}
