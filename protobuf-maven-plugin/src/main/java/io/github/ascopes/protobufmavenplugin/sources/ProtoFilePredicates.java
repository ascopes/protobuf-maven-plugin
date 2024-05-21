/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.sources;

import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common predicates for proto files.
 *
 * @author Ashley Scopes
 */
public final class ProtoFilePredicates {

  private ProtoFilePredicates() {
    // Static-only class
  }

  public static boolean isProtoFile(Path file) {
    return Files.isRegularFile(file) && FileUtils.getFileExtension(file)
        .filter(".proto"::equalsIgnoreCase)
        .isPresent();
  }
}
