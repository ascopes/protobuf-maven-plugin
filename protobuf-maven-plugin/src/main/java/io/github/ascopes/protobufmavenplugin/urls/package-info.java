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
 * URL provider implementations and URL fetching facilities
 *
 * <p>An unfortunate side effect of how ClassWorlds and the URL handler
 * implementations work is that we have to handle custom provider implementations
 * in a bespoke factory pattern. This is due to the fact URL's SPI only respects
 * the default classloader, and we are actively operating in a shared environment
 * that is pre-configured at runtime.
 *
 * <p>As a result, much of this machinary could be mistaken for reinventing the
 * wheel, but this is totally intentional by design to avoid edge cases or
 * mutating the Maven enviromment outside the bounds of this specific plugin.
 */
package io.github.ascopes.protobufmavenplugin.urls;

