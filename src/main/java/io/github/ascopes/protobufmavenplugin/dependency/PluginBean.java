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
package io.github.ascopes.protobufmavenplugin.dependency;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.jspecify.annotations.Nullable;

/**
 * Declaration of a plugin.
 *
 * @author Ashley Scopes
 */
public final class PluginBean {

  /**
   * The plugin ID that is passed to the {@code protoc} commandline.
   */
  @Parameter(readonly = true)
  private @Nullable String id;

  @Parameter
  private @Nullable DependableCoordinate dependableCoordinate;

  @Parameter
  private @Nullable String executableName;

  public PluginBean() {
    // Used by Maven reflectively only.
    this(null, null, null);
  }

  public PluginBean(
      @Nullable String id,
      @Nullable DependableCoordinate dependableCoordinate,
      @Nullable String executableName
  ) {
    this.id = id;
    this.dependableCoordinate = dependableCoordinate;
    this.executableName = executableName;
  }

  public String getId() {
    return requireNonNull(id, "id");
  }

  public Optional<DependableCoordinate> getDependableCoordinate() {
    return Optional.ofNullable(dependableCoordinate);
  }

  public Optional<String> getExecutableName() {
    return Optional.ofNullable(executableName);
  }
}
