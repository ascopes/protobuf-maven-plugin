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
package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import java.util.Map;
import org.eclipse.aether.artifact.ArtifactType;

/**
 * Default artifact type for Eclipse artifacts.
 *
 * @author Ashley Scopes, Tamas Cservenak (for the original implementation in 2.10.1).
 * @since 2.10.2
 */
final class FallbackEclipseArtifactType implements ArtifactType {

  private static final Map<String, String> NO_PROPERTIES = Map.of();
  private static final String NO_CLASSIFIER = "";

  private final String dependencyExtension;

  FallbackEclipseArtifactType(String dependencyExtension) {
    this.dependencyExtension = dependencyExtension;
  }

  @Override
  public String getId() {
    return dependencyExtension;
  }

  @Override
  public String getExtension() {
    return dependencyExtension;
  }

  @Override
  public String getClassifier() {
    return NO_CLASSIFIER;
  }

  @Override
  public Map<String, String> getProperties() {
    return NO_PROPERTIES;
  }
}
