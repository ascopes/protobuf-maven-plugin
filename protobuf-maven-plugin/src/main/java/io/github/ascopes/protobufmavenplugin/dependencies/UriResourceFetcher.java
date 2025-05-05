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
package io.github.ascopes.protobufmavenplugin.dependencies;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.Maven;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that consumes URIs to obtain resources from remote locations.
 *
 * @author Ashley Scopes
 * @since 0.4.0
 */
@Description("Fetches and downloads resources from URIs")
@MojoExecutionScoped
@Named
public final class UriResourceFetcher {

  private static final int TIMEOUT = 30_000;
  private static final String USER_AGENT_HEADER = "User-Agent";
  private static final String USER_AGENT_VALUE = String.format(
      "io.github.ascopes.protobuf-maven-plugin/%s org.apache.maven/%s (Java %s)",
      requireNonNullElse(
          // May not be set if we are running within an IDE.
          UriResourceFetcher.class.getPackage().getImplementationVersion(),
          "SNAPSHOT"
      ),
      Maven.class.getPackage().getImplementationVersion(),
      Runtime.version().toString()
  );

  private static final Logger log = LoggerFactory.getLogger(UriResourceFetcher.class);

  private final TemporarySpace temporarySpace;

  @Inject
  public UriResourceFetcher(TemporarySpace temporarySpace) {
    this.temporarySpace = temporarySpace;
  }

  /**
   * Fetch a file from the given URL, possibly downloading it to the
   * local file system in a temporary location if it is not on the
   * root file system.
   *
   * @param uri the URI of the resource to fetch.
   * @param extension a hint pointing to the potential file extension to use for the resource.
   *     This may be ignored if the URL points to a resource that is already
   *     on the root file system.
   * @return the URL, or an empty optional if it points to a non-existent
   *     resource.
   * @throws ResolutionException if resolution fails for any other reason.
   */
  public Optional<Path> fetchFileFromUri(URI uri, String extension) throws ResolutionException {
    // This will die if the URI points to a file on the non default file system...
    // probably don't care enough to fix this bug as users should not ever want to do this I guess.
    return uri.getScheme().equalsIgnoreCase("file")
        ? handleFileSystemUri(uri)
        : handleOtherUri(uri, extension);
  }

  private Optional<Path> handleFileSystemUri(URI uri) throws ResolutionException {
    try {
      return Optional.of(uri)
          .map(Path::of)
          .filter(Files::exists);
    } catch (Exception ex) {
      throw new ResolutionException("Failed to discover file at '" + uri + "': " + ex, ex);
    }
  }

  private Optional<Path> handleOtherUri(URI uri, String extension) throws ResolutionException {
    var url = parseUrlWithAnyHandler(uri);
    // We have to pass a URL in here, since URIs do not parse the !/ fragments at the ends of
    // strings correctly...
    var targetFile = targetFile(url, extension);

    try {
      var conn = url.openConnection();
      conn.setConnectTimeout(TIMEOUT);
      conn.setReadTimeout(TIMEOUT);
      conn.setAllowUserInteraction(false);
      conn.setRequestProperty(USER_AGENT_HEADER, USER_AGENT_VALUE);

      log.debug("Connecting to '{}' to copy resources to '{}'", uri, targetFile);
      conn.connect();

      try (
          var responseStream = new BufferedInputStream(conn.getInputStream());
          var fileStream = new SizeAwareBufferedOutputStream(Files.newOutputStream(targetFile))
      ) {
        responseStream.transferTo(fileStream);
        log.info("Downloaded '{}' to '{}' ({} bytes)", uri, targetFile, fileStream.size);
        return Optional.of(targetFile);

      } catch (FileNotFoundException ex) {
        log.warn("No resource at '{}' appears to exist!", uri);
        return Optional.empty();
      }

    } catch (IOException ex) {
      log.debug("Failed to download '{}' to '{}'", uri, targetFile, ex);

      throw new ResolutionException("Failed to download '" + uri + "' to '" + targetFile + "'", ex);
    }
  }

  private Path targetFile(URL url, String extension) {
    var digest = Digests.sha1(url.toExternalForm());
    var path = url.getPath();
    var lastSlash = path.lastIndexOf('/');
    var fileName = lastSlash < 0
        ? digest
        : path.substring(lastSlash + 1) + "-" + digest;

    return temporarySpace
        .createTemporarySpace("url", url.getProtocol())
        .resolve(fileName + extension);
  }

  private URL parseUrlWithAnyHandler(URI uri) throws ResolutionException {
    var customHandler = ServiceLoader
        .load(URLStreamHandlerProvider.class, getClass().getClassLoader())
        .stream()
        .map(Provider::get)
        .map(provider -> provider.createURLStreamHandler(uri.getScheme()))
        .findFirst()
        .orElse(null);

    log.debug("Parsing URI '{}' into URL using custom handler '{}'", uri, customHandler);

    try {
      return new URL(null, uri.toString(), customHandler);
    } catch (MalformedURLException ex) {
      throw new ResolutionException("Syntax for URI '" + uri + "' is invalid", ex);
    }
  }

  /**
   * Buffers an output stream, and keeps track of how many bytes
   * were written.
   */
  private static final class SizeAwareBufferedOutputStream extends OutputStream {
    private final OutputStream delegate;
    private long size;

    private SizeAwareBufferedOutputStream(OutputStream delegate) {
      this.delegate = new BufferedOutputStream(delegate);
      size = 0;
    }

    @Override
    public void close() throws IOException {
      flush();
      delegate.close();
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void write(int nextByte) throws IOException {
      ++size;
      delegate.write(nextByte);
    }
  }
}
