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
    var sb = new StringBuilder("mvn");
    appendFragment(sb, getGroupId());
    appendFragment(sb, getArtifactId());
    appendFragment(sb, getVersion());
    appendFragment(sb, getType());
    appendFragment(sb, getClassifier());

    // Remove trailing colons with nothing after them.
    int lastIndex;
    while ((lastIndex = sb.lastIndexOf(":")) == sb.length() - 1) {
      sb.deleteCharAt(lastIndex);
    }

    return sb.toString();
  }

  private static void appendFragment(StringBuilder sb, @Nullable String fragment) {
    sb.append(":").append(requireNonNullElse(fragment, ""));
  }
}
