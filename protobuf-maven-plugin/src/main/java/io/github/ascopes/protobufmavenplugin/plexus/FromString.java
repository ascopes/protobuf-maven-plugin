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
package io.github.ascopes.protobufmavenplugin.plexus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker to advise that a method on a sealed interface base can be used to parse the base
 * as a string.
 *
 * <p>Methods must:
 *
 * <ul>
 *   <li>be public</li>
 *   <li>be static</li>
 *   <li>return an instance of the interface defining the method</li>
 *   <li>take a single {@link String} as a parameter.</li>
 * </ul>
 *
 * @author Ashley Scopes
 * @see KindHint
 * @see SealedTypePlexusConverter
 * @since TBC
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FromString {
}
