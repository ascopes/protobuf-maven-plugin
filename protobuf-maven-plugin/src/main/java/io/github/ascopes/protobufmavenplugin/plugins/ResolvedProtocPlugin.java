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

import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value.Immutable;

/**
 * Model that holds details about a resolved protoc plugin.
 *
 * <p>Only used internally, never exposed via the plugin API to users.
 *
 * @author Ashley Scopes
 */
@Immutable
public interface ResolvedProtocPlugin {

  String getId();

  Optional<String> getOptions();

  int getOrder();

  Path getOutputDirectory();

  Optional<Boolean> getRegisterAsCompilationRoot();

  Path getPath();
}
