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
package io.github.ascopes.protobufmavenplugin.plugins;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import java.util.List;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.Nullable;


/**
 * Implementation independent descriptor for a protoc plugin that can be resolved from a Maven
 * repository.
 *
 * @author Ashley Scopes
 * @since 2.0.0
 */
@Immutable
@Modifiable
public interface MavenProtocPlugin extends MavenArtifact, ProtocPlugin {

  /**
   * Get the command line arguments to pass to the JVM.
   *
   * <p>This defaults to an empty list.
   *
   * @return the list of command line arguments to pass to the JVM.
   * @since 2.6.0
   */
  @Nullable List<String> getJvmArgs();

  /**
   * The dependency resolution depth.
   *
   * <p>This cannot be changed for this type of plugin.
   *
   * @return {@code null}, always.
   */
  @Derived
  @Override
  default @Nullable DependencyResolutionDepth getDependencyResolutionDepth() {
    // We never allow this to be specified for protoc plugins.
    return null;
  }

  /**
   * Get the arguments to pass to the JVM to configure it.
   *
   * <p>Users can use this to control concerns such as heap memory controls,
   * GC and JIT settings, and specifying additional JVM options.
   *
   * @return the list of command line arguments to pass to the JVM.
   * @since 2.6.0
   */
  @Nullable List<String> getJvmConfigArgs();

  /**
   * The main class entrypoint to use if the plugin is not an assembled JAR.
   *
   * <p>Ignored in all other cases.
   *
   * @return the main class name.
   * @since 2.5.0
   */
  @Nullable String getMainClass();
}
