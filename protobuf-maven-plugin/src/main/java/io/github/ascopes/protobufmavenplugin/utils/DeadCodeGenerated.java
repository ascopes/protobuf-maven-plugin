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
package io.github.ascopes.protobufmavenplugin.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instruct JaCoCo to not include the annotated element in any code coverage.
 *
 * <p>Use for things like private constructors in static-only classes, and overridden
 * methods that only exist to instruct {@code immutables} on how to derive an
 * implementation class, such as generated attributes and {@code toString} overrides.
 *
 * <p>This annotation must have {@code Generated} in the name for JaCoCo to detect it.
 *
 * @author Ashley Scopes
 * @since 5.1.3
 */
@Documented
@Inherited
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE})
public @interface DeadCodeGenerated {
  String reason();
}
