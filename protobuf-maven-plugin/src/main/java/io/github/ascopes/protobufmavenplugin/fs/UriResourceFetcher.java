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
package io.github.ascopes.protobufmavenplugin.fs;

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
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

  // Protocols that we allow in offline mode.
  private static final List<String> OFFLINE_PROTOCOLS = List.of(
      "file:",
      "jar:file:",
      "zip:file:",
      "jrt:"
  );

  private static final int TIMEOUT = 30_000;

  private static final Logger log = LoggerFactory.getLogger(UriResourceFetcher.class);

  private final MavenSession mavenSession;
  private final TemporarySpace temporarySpace;

  @Inject
  public UriResourceFetcher(
      MavenSession mavenSession,
      TemporarySpace temporarySpace
  ) {
    this.mavenSession = mavenSession;
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
    if (mavenSession.isOffline()) {
      var isInvalidOfflineProtocol = OFFLINE_PROTOCOLS.stream()
          .noneMatch(uri.toString()::startsWith);

      if (isInvalidOfflineProtocol) {
        throw new ResolutionException(
            "Cannot resolve URI \""
                + uri
                + "\". Only a limited number of URL protocols are supported in offline mode."
        );
      }
    }

    // This will die if the URI points to a file on the non default file system...
    // probably don't care enough to fix this bug as users should not ever want to do this I guess.
    return "file".equals(uri.getScheme())
        ? handleFileSystemUri(uri)
        : handleOtherUri(uri, extension);
  }

  private Optional<Path> handleFileSystemUri(URI uri) throws ResolutionException {
    try {
      var result = Optional.of(uri)
          .map(Path::of)
          .filter(Files::exists);

      result.ifPresentOrElse(
          path -> log.debug("Resolved \"{}\" to \"{}\"", uri, path),
          () -> log.warn("No resource was found at \"{}\" ", uri)
      );

      return result;
    } catch (Exception ex) {
      throw new ResolutionException("Failed to discover file at \"" + uri + "\": " + ex, ex);
    }
  }

  private Optional<Path> handleOtherUri(URI uri, String extension) throws ResolutionException {
    var url = parseUrlWithAnyHandler(uri);
    // We have to pass a URL in here, since URIs do not parse the !/ fragments at the ends of
    // strings correctly...
    var targetFile = targetFile(url, extension);

    try {
      var conn = url.openConnection();
      // Important! Without this JarURLConnection may leave the underlying connection
      // open after we close conn.getInputStream(). On Windows this can prevent the deletion
      // of these files as part of operations like mvn clean, and also in our unit tests.
      // On Windows, we can evem crash JUnit because of this!
      // See https://github.com/junit-team/junit5/issues/4567
      conn.setUseCaches(false);

      conn.setConnectTimeout(TIMEOUT);
      conn.setReadTimeout(TIMEOUT);
      conn.setAllowUserInteraction(false);

      log.debug("Connecting to \"{}\", will transfer contents to \"{}\"", uri, targetFile);
      conn.connect();

      try (
          var responseInputStream = new BufferedInputStream(conn.getInputStream());
          var fileOutputStream = FileUtils.newBufferedOutputStream(targetFile)
      ) {
        responseInputStream.transferTo(fileOutputStream);
      }

      var fileSize = Files.size(targetFile);
      log.info("Transferred \"{}\" to \"{}\" ({})", uri, StringUtils.pluralize(fileSize, "byte"));

      return Optional.of(targetFile);

    } catch (IOException ex) {
      log.debug("Failed to transfer \"{}\" to \"{}\"", uri, targetFile, ex);

      if (ex instanceof FileNotFoundException) {
        // May be raised during the call to .getInputStream(), or the call to .connect(),
        // depending on the implementation.
        log.warn("No resource at \"{}\" exists", uri);
        return Optional.empty();
      } else {
        throw new ResolutionException(
            "Failed to transfer \"" + uri + "\" to \"" + targetFile + "\"", ex
        );
      }
    }
  }

  private Path targetFile(URL url, String extension) {
    var digest = Digest.compute("SHA-1", url.toExternalForm()).toHexString();
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
    // We use ServiceLoader directly for this so that we can load handlers from
    // other non-system classloaders. URL's internals only consider the default
    // system/boot classloader, which differs to our runtime classloader that
    // runs on top of ClassWorlds in Maven, meaning we cannot load custom schemes
    // via normal mechanisms.
    var handler = ServiceLoader
        .load(URLStreamHandlerProvider.class, getClass().getClassLoader())
        .stream()
        .map(ServiceLoader.Provider::get)
        .map(provider -> provider.createURLStreamHandler(uri.getScheme()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);

    log.debug("Parsing URI \"{}\" into URL using handler \"{}\"", uri, handler);

    try {
      return new URL(null, uri.toString(), handler);
    } catch (MalformedURLException ex) {
      throw new ResolutionException("URI \"" + uri + "\" is invalid: " + ex, ex);
    }
  }
}
