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

import org.jspecify.annotations.Nullable;

/**
 * Various common string helpers.
 *
 * @author Ashley Scopes
 */
public final class StringUtils {

  @DeadCodeGenerated(reason = "static-only class")
  private StringUtils() {
    throw new UnsupportedOperationException();
  }

  public static String pluralize(long quantity, String singular) {
    return pluralize(quantity, singular, singular + "s");
  }

  public static String pluralize(long quantity, String singular, String plural) {
    return quantity == 1
        ? quantity + " " + singular
        : quantity + " " + plural;
  }

  public static String quoted(@Nullable String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }
}
