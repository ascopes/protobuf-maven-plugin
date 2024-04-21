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

import java.util.function.Function;

/**
 * Shell/batch file quoting.
 *
 * <p>Losely based on Python's {@code shlex} module.
 *
 * <p>This is far from perfect but should work in the majority of use cases
 * to ensure scripts do not interpret special characters in paths in strange
 * and unexpected ways.
 *
 * <p>Long lines will be split up with line continuations.
 *
 * @author Ashley Scopes
 */
public final class Shlex {

  private static final int LINE_LENGTH_TARGET = 99;
  private static final String INDENT = " ".repeat(4);

  private Shlex() {
    // Static-only class
  }

  public static String quoteShellArgs(Iterable<String> args) {
    return quote(args, Shlex::quoteShellArg, " \\\n");
  }

  public static String quoteBatchArgs(Iterable<String> args) {
    return quote(args, Shlex::quoteBatchArg, " ^\r\n");
  }

  private static String quote(
      Iterable<String> args,
      Function<String, String> quoter,
      String continuation
  ) {
    var iter = args.iterator();

    if (!iter.hasNext()) {
      // Probably won't ever happen.
      return "";
    }

    var sb = new StringBuilder(quoter.apply(iter.next()));
    var lineLength = sb.length();

    while (iter.hasNext()) {
      var next = quoter.apply(iter.next());

      if (lineLength + next.length() >= LINE_LENGTH_TARGET) {
        sb.append(continuation);
        lineLength = INDENT.length();
        sb.append(INDENT);
      } else {
        sb.append(" ");
        lineLength += 1;
      }

      sb.append(next);
      lineLength += next.length();
    }

    return sb.toString();
  }

  private static String quoteShellArg(String arg) {
    if (isSafe(arg)) {
      return arg;
    }

    var sb = new StringBuilder();
    sb.append('\'');
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      switch (c) {
        case '\'':
          sb.append("'\"'\"'");
          break;
        case '\n':
          sb.append("'$'\\n''");
          break;
        case '\r':
          sb.append("'$'\\r''");
          break;
        case '\t':
          sb.append("'$'\\t''");
          break;
        default:
          sb.append(c);
          break;
      }
    }
    sb.append('\'');

    return sb.toString();
  }

  private static String quoteBatchArg(String arg) {
    if (isSafe(arg)) {
      return arg;
    }

    var sb = new StringBuilder();
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      switch (c) {
        case '%':
          sb.append("%%");
          break;
        case '\\':
        case '"':
        case '\'':
        case ' ':
        case '\r':
        case '\t':
        case '^':
        case '&':
        case '<':
        case '>':
        case '|':
          sb.append('^').append(c);
          break;
        default:
          sb.append(c);
          break;
      }
    }

    return sb.toString();
  }

  private static boolean isSafe(String arg) {
    for (var i = 0; i < arg.length(); ++i) {
      var c = arg.charAt(i);
      var safe = 'A' <= c && c <= 'Z'
          || 'a' <= c && c <= 'z'
          || '0' <= c && c <= '9'
          || c == '-'
          || c == '/'
          || c == '_'
          || c == '.'
          || c == '=';

      if (!safe) {
        return false;
      }
    }

    return true;
  }
}
