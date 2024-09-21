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

import java.io.IOException;
import java.io.Writer;
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

  public void write(Writer writer) throws IOException {
    for (var argument : arguments) {
      if (argument.chars().noneMatch(c -> " \n\r\t'\"".indexOf(c) >= 0)) {
        writer.append(argument).append("\n");
        continue;
      }

      writer.append('"');
      for (var i = 0; i < argument.length(); ++i) {
        var nextChar = argument.charAt(i);
        switch (nextChar) {
          case '"':
            writer.append("\\\"");
            break;
          case '\'':
            writer.append("\\'");
            break;
          case '\\':
            writer.append("\\\\");
            break;
          case '\n':
            writer.append("\\n");
            break;
          case '\r':
            writer.append("\\r");
            break;
          case '\t':
            writer.append("\\t");
            break;
          default:
            writer.append(nextChar);
            break;
        }
      }
      writer.append("\"\n");
    }
  }
}
