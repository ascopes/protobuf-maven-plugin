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
package io.github.ascopes.protobufmavenplugin.platform;

import java.util.function.BiConsumer;

/**
 * Shell/batch file quoting.
 *
 * <p>Losely based on Python's {@code shlex} module.
 *
 * <p>This is far from perfect but should work in the majority of use cases
 * to ensure scripts do not interpret special characters in paths in strange
 * and unexpected ways.
 *
 * @author Ashley Scopes
 */
public final class Shlex {

  private Shlex() {
    // Static-only class
  }

  public static String quoteShellArgs(Iterable<String> args) {
    return quote(args, Shlex::quoteShellArg);
  }

  public static String quoteBatchArgs(Iterable<String> args) {
    return quote(args, Shlex::quoteBatchArg);
  }

  private static String quote(Iterable<String> args, BiConsumer<StringBuilder, String> quoter) {
    var iter = args.iterator();

    if (!iter.hasNext()) {
      // Probably won't ever happen.
      return "";
    }

    var sb = new StringBuilder();
    quoter.accept(sb, iter.next());

    while (iter.hasNext()) {
      sb.append(' ');
      quoter.accept(sb, iter.next());
    }

    return sb.toString();
  }

  private static void quoteShellArg(StringBuilder sb, String arg) {
    if (isSafe(arg)) {
      sb.append(arg);
      return;
    }

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
  }

  private static void quoteBatchArg(StringBuilder sb, String arg) {
    if (isSafe(arg)) {
      sb.append(arg);
      return;
    }

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
