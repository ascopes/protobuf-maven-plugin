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
package io.github.ascopes.protobufmavenplugin.java;

import java.nio.file.Path;
import java.util.List;
import org.immutables.value.Value.Immutable;
import org.jspecify.annotations.Nullable;

/**
 * Descriptor for a Java application.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Immutable
public interface JavaApp {
  String getUniqueName();

  List<Path> getDependencies();

  @Nullable List<String> getJvmArgs();

  @Nullable List<String> getJvmConfigArgs();

  // Null if unset or inferred from MANIFEST.MF.
  @Nullable String getMainClass();
}
