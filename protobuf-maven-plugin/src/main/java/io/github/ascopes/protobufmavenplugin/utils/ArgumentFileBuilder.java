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

package io.github.ascopes.protobufmavenplugin.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Java argument files that deals with the quoting and escaping rules Java expects.
 *
 * <p>See
 * https://github.com/openjdk/jdk/blob/2461263aac35b25e2a48b6fc84da49e4b553dbc3/src/java.base/share/native/libjli/args.c#L165-L355
 * for the Java implementation.
 *
 * @author Ashley Scopes
 * @since 2.6.0
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public final class ArgumentFileBuilder {
  private final List<String> arguments;

  public ArgumentFileBuilder() {
    arguments = new ArrayList<>();
  }

  public ArgumentFileBuilder add(Object argument) {
    arguments.add(argument.toString());
    return this;
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();

    for (var argument : arguments) {
      if (argument.chars().noneMatch(c -> " \n\r\t'\"".indexOf(c) >= 0)) {
        sb.append(argument).append("\n");
        continue;
      }

      sb.append('"');
      for (var i = 0; i < argument.length(); ++i) {
        var nextChar = argument.charAt(i);
        switch (nextChar) {
          case '"':
            sb.append("\\\"");
            break;
          case '\'':
            sb.append("\\'");
            break;
          case '\\':
            sb.append("\\\\");
            break;
          case '\n':
            sb.append("\\n");
            break;
          case '\r':
            sb.append("\\r");
            break;
          case '\t':
            sb.append("\\t");
            break;
          default:
            sb.append(nextChar);
            break;
        }
      }
      sb.append("\"\n");
    }

    return sb.toString();
  }
}
