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
package io.github.ascopes.protobufmavenplugin.dependencies;

import static java.util.Objects.requireNonNullElse;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;


/**
 * Base for a parameter that references a deployed Maven artifact
 * somewhere.
 *
 * <p>Implementations should extend this type rather than using it
 * directly.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
public abstract class MavenArtifact {

  public abstract String getGroupId();

  public abstract String getArtifactId();

  public abstract @Nullable String getVersion();

  public abstract @Nullable String getType();

  public abstract @Nullable String getClassifier();

  @Override
  public String toString() {
    return Stream
        .of(
            getGroupId(),
            getArtifactId(),
            getVersion(),
            getType(),
            getClassifier()
        )
        .map(s -> requireNonNullElse(s, ""))
        .collect(Collectors.joining(":", "mvn:", ""))
        .replaceFirst(":+$", "");
  }
}
