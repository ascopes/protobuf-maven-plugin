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
package io.github.ascopes.protobufmavenplugin.urls;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.jspecify.annotations.Nullable;

/**
 * Standard type for wrapping variants of an Apache Commons Compress archivers into
 * {@link java.net.URLConnection}s.
 *
 * <p>When used, it will extract some kind of file entry if one exists, buffering it into memory
 * before closing the original input streams and returning a new in-memory stream of the buffered
 * data.
 *
 * @author Ashley Scopes
 * @since 3.10.0
 */
final class ArchiveUrlStreamHandlerFactory
    extends AbstractDecoratingUrlStreamHandlerFactory {

  private final InputStreamDecorator<ArchiveInputStream<?>> decorator;

  ArchiveUrlStreamHandlerFactory(
      UrlFactory urlFactory,
      InputStreamDecorator<ArchiveInputStream<?>> decorator,
      String... protocols
  ) {
    super(true, urlFactory, protocols);
    this.decorator = decorator;
  }

  @Override
  protected InputStream decorateInputStream(
      InputStream inputStream,
      @Nullable String file
  ) throws IOException {
    requireNonNull(file);

    try (
        inputStream;
        var archiveInputStream = decorator.decorate(inputStream)
    ) {
      ArchiveEntry entry;

      while ((entry = archiveInputStream.getNextEntry()) != null) {
        if (file.equals(entry.getName())) {
          break;
        }
      }

      if (entry == null) {
        throw new FileNotFoundException(
            "Could not find '" + file + "' within "
                + archiveInputStream.getClass().getSimpleName()
        );
      }

      // Transfer the file contents out so we can close the actual stream.
      var baos = new ByteArrayOutputStream();
      archiveInputStream.transferTo(baos);
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }
}
