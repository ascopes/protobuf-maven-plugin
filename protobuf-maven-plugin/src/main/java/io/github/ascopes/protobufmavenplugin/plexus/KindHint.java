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
package io.github.ascopes.protobufmavenplugin.plexus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;

/**
 * Marker to advise the "kind" of the implementation when used with a sealed-type hierarchy.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KindHint {

  /**
   * The kind of the implementation.
   *
   * @return the kind.
   */
  String kind();

  /**
   * The implementation that the kind should point to.
   *
   * <p>This is needed until
   * <a href="https://github.com/ascopes/protobuf-maven-plugin/pull/880">GH-880</a>
   * can be merged, as we cannot easily infer the immutable implementation class from the base
   * when working with the Immutables library. GH-880 will enable better integration with that
   * library based on compile-time metadata that avoids this issue. We may just need to include
   * this annotation in {@link Style#passAnnotations()} to achieve this correctly closer to the
   * time.
   *
   * @return the implementation type.
   */
  Class<?> implementation();
}
