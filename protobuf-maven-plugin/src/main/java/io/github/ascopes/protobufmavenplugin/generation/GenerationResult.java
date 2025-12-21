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
package io.github.ascopes.protobufmavenplugin.generation;

import java.util.Locale;

/**
 * Marker to describe the result of a generation run.
 *
 * @author Ashley Scopes
 * @since 2.13.0
 */
public enum GenerationResult {
  PROTOC_SUCCEEDED(
      true,
      "Protoc invocation succeeded."
  ),
  NOTHING_TO_DO(
      true,
      "There is nothing to do. If this is unexpected, review the above logs for more details."
  ),
  PROTOC_FAILED(
      false,
      "Protoc failed with an error. Check the build logs above to find the root cause."
  ),
  NO_SOURCES(
      false,
      "No valid protobuf sources were found. Check the build logs above for more details."
  ),
  NO_TARGETS(
      false,
      "No output languages were enabled and no protoc plugins were configured."
  );

  private final boolean ok;
  private final String description;

  GenerationResult(boolean ok, String description) {
    this.ok = ok;
    this.description = description;
  }

  public boolean isOk() {
    return ok;
  }

  @Override
  public String toString() {
    return name().replace("_", " ").toUpperCase(Locale.ROOT) + ": " + description;
  }
}
