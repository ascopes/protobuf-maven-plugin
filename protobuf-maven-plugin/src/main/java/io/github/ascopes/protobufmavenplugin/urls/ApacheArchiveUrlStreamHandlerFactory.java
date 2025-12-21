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
package io.github.ascopes.protobufmavenplugin.urls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;

/**
 * URL stream handler factory for URLs that dereference the contents of
 * archives.
 *
 * @author Ashley Scopes
 * @since 3.10.1
 */
final class ApacheArchiveUrlStreamHandlerFactory extends AbstractUrlStreamHandlerFactory {
  private final UrlFactory urlFactory;
  private final InputStreamDecorator<ArchiveInputStream<?>> decorator;

  ApacheArchiveUrlStreamHandlerFactory(
      UrlFactory urlFactory,
      InputStreamDecorator<ArchiveInputStream<?>> decorator,
      String protocol,
      String... protocols
  ) {
    super(protocol, protocols);
    this.urlFactory = urlFactory;
    this.decorator = decorator;
  }

  @Override
  URLConnection createUrlConnection(URL url) throws IOException {
    var rawInnerUri = url.getFile();
    var pathIndex = rawInnerUri.lastIndexOf("!/");
    if (pathIndex == -1) {
      throw new IOException(
          "URI '" + url + "' was missing a nested path fragment (e.g. '"
              + url.getProtocol() + ":http://some-website.com/some-file!/path/within/archive"
              + "')"
      );
    }

    // +2 since prefix "!/" is 2 chars long; we don't want to include the first forwardslash.
    var file = rawInnerUri.substring(pathIndex + 2);
    rawInnerUri = rawInnerUri.substring(0, pathIndex);
    var innerUrl = urlFactory.create(URI.create(rawInnerUri));

    return new AbstractNestingUrlConnection(url, innerUrl) {
      @Override
      InputStream nestInputStream(InputStream inputStream) throws IOException {
        return readFileFromArchive(inputStream, file);
      }
    };
  }

  private InputStream readFileFromArchive(
      InputStream inputStream,
      String file
  ) throws IOException {
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
