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

/**
 * A modern Maven plugin for generating source code from Protocol Buffers
 * sources.
 */
@NullMarked
@Style(
    beanFriendlyModifiables = true,
    create = "new",
    defaults = @Immutable(copy = false),
    defaultAsDefault = true,
    deferCollectionAllocation = true,
    get = {"get*", "is*"},
    headerComments = true,
    jacksonIntegration = false,
    // Eventually may be true when Maven upgrades.
    jakarta = false,
    jdkOnly = true,
    jdk9Collections = true,
    nullableAnnotation = "org.jspecify.annotations.Nullable",
    passAnnotations = Nullable.class,
    optionalAcceptNullable = true,
    typeModifiable = "*Bean",
    validationMethod = ValidationMethod.MANDATORY_ONLY
)
package io.github.ascopes.protobufmavenplugin;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.ValidationMethod;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
