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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.maven.execution.scope.MojoExecutionScoped;

/**
 * Workaround for loading URLs with custom protocols defined in this ClassWorlds realm.
 *
 * <p>URL, by default, will query the service loader for the default class loader only
 * when converting itself into a {@link java.net.URLConnection}. This is problematic for us as we
 * cannot see any implementations we've put on the classpath.
 *
 * <p>Any implementations are expected to be injected via CDI.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@MojoExecutionScoped
@Named
public final class UrlFactory {

  private final List<URLStreamHandlerFactory> urlStreamHandlerFactories;

  UrlFactory() {
    urlStreamHandlerFactories = new ArrayList<>();
  }

  @PostConstruct
  void init() {
    urlStreamHandlerFactories.addAll(List.of(
        new TransformingUrlStreamHandlerFactory(
            this,
            BZip2CompressorInputStream::new,
            "bz", "bzip2"
        ),
        new TransformingUrlStreamHandlerFactory(
            this,
            GZIPInputStream::new,
            "gz", "gzip"
        ),
        new ArchiveUrlStreamHandlerFactory(
            this,
            JarArchiveInputStream::new,
            "jar", "ear", "war"
        ),
        new ArchiveUrlStreamHandlerFactory(
            this,
            ZipArchiveInputStream::new,
            "zip"
        ),
        new ArchiveUrlStreamHandlerFactory(
            this,
            TarArchiveInputStream::new,
            "tar"
        )
    ));
  }

  public URL create(URI uri) throws IOException {
    var protocol = uri.getScheme();
    var handler = urlStreamHandlerFactories.stream()
        .map(factory -> factory.createURLStreamHandler(protocol))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);

    // If the handler is null, the regular JDK providers will be used as normal.
    return new URL(null, uri.toString(), handler);
  }
}
