/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.fixture;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Random test data.
 *
 * @author Ashley Scopes
 */
public final class RandomData {
  private static final Random RANDOM = new Random();

  public static <T> T oneOf(Iterable<T> iterable) {
    var choices = new ArrayList<T>();
    iterable.forEach(choices::add);
    var index = RANDOM.nextInt(choices.size());
    return choices.get(index);
  }

  public static String someString() {
    return UUID.randomUUID().toString();
  }

  private RandomData() {
    // Static-only class.
  }
}
