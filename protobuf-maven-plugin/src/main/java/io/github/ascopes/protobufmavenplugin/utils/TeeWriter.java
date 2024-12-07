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

/**
 * A writer that also writes to an in-memory buffer to enable the content to be replayed.
 *
 * @author Ashley Scopes
 * @since 2.7.2
 */
public final class TeeWriter extends Writer {

  private final StringBuilder stringBuilder;
  private final Writer writer;

  public TeeWriter(Writer writer) {
    stringBuilder = new StringBuilder();
    this.writer = writer;
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  public String toString() {
    return stringBuilder.toString();
  }

  @Override
  public void write(char[] buffer, int offset, int length) throws IOException {
    writer.write(buffer, offset, length);
    stringBuilder.append(buffer, offset, length);
  }
}
