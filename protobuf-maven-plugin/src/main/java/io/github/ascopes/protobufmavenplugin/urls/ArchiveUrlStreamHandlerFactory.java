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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    extends AbstractRecursiveUrlStreamHandlerFactory {

  private static final Logger log = LoggerFactory.getLogger(ArchiveUrlStreamHandlerFactory.class);

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
  protected InputStream decorate(
      InputStream inputStream,
      @Nullable String file
  ) throws IOException {
    requireNonNull(file);
    file = normalizeEntryName(file);

    try (
        // Important that we close the original input stream here.
        // The URLConnection API is arguably poorly designed because it
        // provides no simple mechanism for closing any associated 
        // resources. Instead, we have to close the associated input
        // streams manually to avoid leaking resources.
        inputStream;
        // The archive input stream is only kept alive for as long as
        // we need it to read from. Everything else is rebuffered in
        // memory so we can avoid resource leaks that the garbage collector
        // cannot deal with itself.
        var archiveInputStream = decorator.decorate(inputStream)
    ) {
      ArchiveEntry entry;
      String name;

      while ((entry = archiveInputStream.getNextEntry()) != null) {
        name = normalizeEntryName(entry.getName());

        log.trace(
            "Discovered entry '{}' (original name was '{}') in {} (wrapping {})",
            name,
            entry.getName(),
            archiveInputStream,
            inputStream
        );

        if (file.equals(name)) {
          break;
        }
      }

      if (entry == null) {
        throw new FileNotFoundException(
            "Could not find '" + file + "' within "
                + archiveInputStream.getClass().getSimpleName()
        );
      }

      // Transfer the file contents out so we can close the input streams
      // to avoid leaking resources.
      var baos = new ByteArrayOutputStream();
      log.trace("Loading '{}' into new buffer", file);
      archiveInputStream.transferTo(baos);
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }

  private static String normalizeEntryName(String name) {
    // Tarballs seem to do this sometimes. I'm not sure if there are other
    // edge cases to worry about, so we may find this needs further expansion
    // in the future.
    if (name.startsWith("./")) {
      name = name.substring(2);
    }
    return name;
  }
}
