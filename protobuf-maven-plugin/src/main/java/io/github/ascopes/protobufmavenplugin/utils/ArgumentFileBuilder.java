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
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Java argument files that deals with the quoting and escaping rules Java expects.
 *
 * @author Ashley Scopes
 * @since 2.6.0
 */
public final class ArgumentFileBuilder {

  private final List<String> arguments;

  public ArgumentFileBuilder() {
    arguments = new ArrayList<>();
  }

  public ArgumentFileBuilder add(Object argument) {
    arguments.add(argument.toString());
    return this;
  }

  // See https://github.com/protocolbuffers/protobuf/blob/0361a593/src/google/protobuf/compiler/command_line_interface.cc#L1759
  public void writeToProtocArgumentFile(Appendable appendable) throws IOException {
    for (var argument : arguments) {
      appendable.append(argument).append("\n");
    }
  }

  // See https://github.com/openjdk/jdk/blob/2461263a/src/java.base/share/native/libjli/args.c#L165-L355
  public void writeToJavaArgumentFile(Appendable appendable) throws IOException {
    for (var argument : arguments) {
      if (argument.chars().noneMatch(c -> " \n\r\t'\"".indexOf(c) >= 0)) {
        appendable.append(argument).append("\n");
        continue;
      }

      appendable.append('"');
      for (var i = 0; i < argument.length(); ++i) {
        var nextChar = argument.charAt(i);
        switch (nextChar) {
          case '"':
            appendable.append("\\\"");
            break;
          case '\'':
            appendable.append("\\'");
            break;
          case '\\':
            appendable.append("\\\\");
            break;
          case '\n':
            appendable.append("\\n");
            break;
          case '\r':
            appendable.append("\\r");
            break;
          case '\t':
            appendable.append("\\t");
            break;
          default:
            appendable.append(nextChar);
            break;
        }
      }
      appendable.append("\"\n");
    }
  }
}
