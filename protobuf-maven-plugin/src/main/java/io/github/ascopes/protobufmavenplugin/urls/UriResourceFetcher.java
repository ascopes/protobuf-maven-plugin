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

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.Maven;
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
  private static final Pattern OFFLINE_PROTOCOLS = Pattern.compile("^([A-Za-z0-9-]+:)*file:.*");

  // Fetch our version from our JAR when it is available. For unit tests, etc., this will usually
  // be null.
  private static final String USER_AGENT = String.format(
      "protobuf-maven-plugin/%s (io.github.ascopes) Apache-Maven/%s Java/%s (%s, %s, %s)",
      UriResourceFetcher.class.getPackage().getImplementationVersion(),
      Maven.class.getPackage().getImplementationVersion(),
      System.getProperty("java.version"),
      System.getProperty("java.vm.name"),
      System.getProperty("java.vm.version"),
      System.getProperty("java.vm.vendor")
  );

  private static final int TIMEOUT = 30_000;

  private static final Logger log = LoggerFactory.getLogger(UriResourceFetcher.class);

  private final MavenSession mavenSession;
  private final TemporarySpace temporarySpace;
  private final UrlFactory urlFactory;

  @Inject
  public UriResourceFetcher(
      MavenSession mavenSession,
      UrlFactory urlFactory,
      TemporarySpace temporarySpace
  ) {
    this.mavenSession = mavenSession;
    this.temporarySpace = temporarySpace;
    this.urlFactory = urlFactory;
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
  public Optional<Path> fetchFileFromUri(
      URI uri,
      String extension,
      boolean setExecutable
  ) throws ResolutionException {
    if (mavenSession.isOffline()) {
      if (!OFFLINE_PROTOCOLS.matcher(uri.toString()).matches()) {
        throw new ResolutionException(
            "Cannot resolve URI \""
                + uri
                + "\". Only a limited number of URL protocols are supported in offline mode."
        );
      }
    }

    // Prior to GH-782, we handled local files differently, returning the original path to avoid
    // a copy on each invocation. This has been simplified to be treated in the same way as any
    // other URI so we can correctly enforce executable bits on the file if required without
    // modifying files outside the current build.
    return fetchFileFromUriOnline(uri, extension, setExecutable);
  }

  private Optional<Path> fetchFileFromUriOnline(
      URI uri,
      String extension,
      boolean setExecutable
  ) throws ResolutionException {
    URL url;

    try {
      url = urlFactory.create(uri);
    } catch (IOException ex) {
      throw new ResolutionException("URI \"" + uri + "\" is invalid: " + ex, ex);
    }

    // We have to pass a URL in here, since URIs do not parse the !/ fragments at the ends of
    // strings correctly...
    var targetFile = targetFile(url, extension);

    try {
      var conn = openConnection(url);

      log.debug("Connecting to \"{}\", will transfer contents to \"{}\"", uri, targetFile);
      conn.connect();

      try (
          // This should always result in the underlying connections being closed.
          var responseInputStream = new BufferedInputStream(conn.getInputStream());
          var fileOutputStream = FileUtils.newBufferedOutputStream(targetFile)
      ) {
        responseInputStream.transferTo(fileOutputStream);
      }

      var fileSize = Files.size(targetFile);
      log.info(
          "Transferred \"{}\" to \"{}\" ({})",
          uri,
          targetFile,
          StringUtils.pluralize(fileSize, "byte")
      );

      if (setExecutable) {
        FileUtils.makeExecutable(targetFile);
      }

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
            "Failed to transfer \"" + uri + "\" to \"" + targetFile + "\": " + ex,
            ex
        );
      }
    }
  }

  private static URLConnection openConnection(URL url) throws IOException {
    // Important! Without disabling caches, JarURLConnection may leave the underlying connection
    // open after we close conn.getInputStream(). On Windows this can prevent the deletion of these
    // files as part of operations like mvn clean, and also in our unit tests. On Windows, we can
    // even crash JUnit because of this!
    // See https://github.com/junit-team/junit5/issues/4567.
    var conn = url.openConnection();
    conn.addRequestProperty("User-Agent", USER_AGENT);
    conn.setAllowUserInteraction(false);
    conn.setConnectTimeout(TIMEOUT);
    conn.setDoInput(true);
    conn.setDoOutput(false);
    conn.setReadTimeout(TIMEOUT);
    conn.setUseCaches(false);
    return conn;
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
}
