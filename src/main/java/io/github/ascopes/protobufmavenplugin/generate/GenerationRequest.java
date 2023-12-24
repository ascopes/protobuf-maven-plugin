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
package io.github.ascopes.protobufmavenplugin.generate;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;
import org.jspecify.annotations.Nullable;

public final class GenerationRequest {
  private final MavenSession mavenSession;
  private final String protocVersion;

  private final Set<Path> sourceDirectories;
  private @Nullable Path outputDirectory;
  private boolean liteOnly;
  private boolean fatalWarnings;
  private boolean kotlinIncluded;

  public GenerationRequest(MavenSession mavenSession, String protocVersion) {
    this.mavenSession = mavenSession;
    this.protocVersion = protocVersion;
    sourceDirectories = new HashSet<>();

    outputDirectory = null;
    liteOnly = false;
    fatalWarnings = false;
    kotlinIncluded = false;
  }

  public MavenSession getMavenSession() {
    return mavenSession;
  }

  public String getProtocVersion() {
    return protocVersion;
  }

  public Set<Path> getSourceDirectories() {
    return sourceDirectories;
  }

  public Optional<Path> getOutputDirectory() {
    return Optional.ofNullable(outputDirectory);
  }

  public boolean isLiteOnly() {
    return liteOnly;
  }

  public boolean isFatalWarnings() {
    return fatalWarnings;
  }

  public boolean isKotlinIncluded() {
    return kotlinIncluded;
  }
}
