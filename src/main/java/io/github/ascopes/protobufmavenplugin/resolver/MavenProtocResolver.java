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

package io.github.ascopes.protobufmavenplugin.resolver;

import java.nio.file.Path;
import java.util.Objects;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolver for {@code protoc} which looks up the provided version in the Maven remote repositories
 * for the project and fetches the desired version prior to returning the path to the executable.
 *
 * @author Ashley Scopes
 */
public final class MavenProtocResolver implements ProtocResolver {

  private static final String DEFAULT_PROTOC_VERSION = "LATEST";

  private String version;

  /**
   * Initialise this resolver.
   */
  public MavenProtocResolver() {
    version = DEFAULT_PROTOC_VERSION;
  }

  /**
   * Get the {@code protoc} version to use.
   * 
   * @return the version of {@code protoc} to use.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Set the version of {@code protoc} to use.
   *
   * @param version the version of {@code protoc} to use.
   */
  @Parameter(name = "version", property = "protoc.version", defaultValue = DEFAULT_PROTOC_VERSION)
  public void setVersion(String version) {
    this.version = Objects.requireNonNull(version);
  }

  @Override
  public Path resolveProtoc() throws ProtocResolutionException {
    return null;
  }
}
