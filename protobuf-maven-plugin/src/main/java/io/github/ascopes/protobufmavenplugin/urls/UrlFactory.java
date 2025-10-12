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

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
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
import org.jspecify.annotations.Nullable;

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
 * @since 3.10.0
 */
@MojoExecutionScoped
@Named
public final class UrlFactory {

  // late-initialised to avoid circular dependency problems.
  private @Nullable List<URLStreamHandlerFactory> urlStreamHandlerFactories;

  @PostConstruct
  void init() {
    urlStreamHandlerFactories = List.of(
        new NestingUrlStreamHandlerFactory(
            this,
            BZip2CompressorInputStream::new,
            "bz", "bz2", "bzip", "bzip2"
        ),
        new NestingUrlStreamHandlerFactory(
            this,
            GZIPInputStream::new,
            "gz", "gzip"
        ),
        new ApacheArchiveUrlStreamHandlerFactory(
            this,
            JarArchiveInputStream::new,
            "jar", "ear", "war"
        ),
        new ApacheArchiveUrlStreamHandlerFactory(
            this,
            ZipArchiveInputStream::new,
            "kar", "zip"
        ),
        new ApacheArchiveUrlStreamHandlerFactory(
            this,
            TarArchiveInputStream::new,
            "tar"
        ),
        new HttpUrlStreamHandlerFactory()
    );
  }

  public URL create(URI uri) throws IOException {
    var protocol = uri.getScheme();
    var handler = requireNonNull(urlStreamHandlerFactories)
        .stream()
        .map(factory -> factory.createURLStreamHandler(protocol))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);

    // If the handler is null, the regular JDK providers will be used as normal.
    // In that case, we do not support further recursion, but that is probably
    // fine.
    return new URL(null, uri.toString(), handler);
  }
}
