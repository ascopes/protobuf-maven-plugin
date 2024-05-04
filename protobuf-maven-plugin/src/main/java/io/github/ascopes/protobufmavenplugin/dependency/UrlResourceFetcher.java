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

package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.generate.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that consumes URLs to obtain resources from remote locations.
 *
 * @author Ashley Scopes
 * @since 0.4.0
 */
@Named
public final class UrlResourceFetcher {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  private static final String USER_AGENT = "User-Agent";
  private static final Logger log = LoggerFactory.getLogger(UrlResourceFetcher.class);

  private final TemporarySpace temporarySpace;

  @Inject
  public UrlResourceFetcher(TemporarySpace temporarySpace) {
    this.temporarySpace = temporarySpace;
  }

  public Path fetchFileFromUrl(URL url, String defaultExtension) throws ResolutionException {
    return url.getProtocol().equalsIgnoreCase("file")
        ? handleFileSystemUrl(url)
        : handleOtherUrl(url, defaultExtension);
  }

  private Path handleFileSystemUrl(URL url) throws ResolutionException {
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException ex) {
      throw new ResolutionException("Failed to resolve '" + url + "' due to malformed syntax", ex);
    }
  }

  private Path handleOtherUrl(URL url, String extension) throws ResolutionException {
    var targetFile = targetFile(url, extension);

    try {
      var conn = url.openConnection();
      conn.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
      conn.setReadTimeout((int) CONNECT_TIMEOUT.toMillis());
      conn.setAllowUserInteraction(false);
      conn.setDoOutput(false);
      conn.setRequestProperty(USER_AGENT, userAgent());
      conn.setUseCaches(true);

      log.debug("Connecting to '{}' to copy resources to '{}'", url, targetFile);

      conn.connect();

      try (var responseBody = conn.getInputStream()) {
        copyToFile(responseBody, targetFile);
      }

      log.info("Copied {} to {}", url, targetFile);

    } catch (IOException ex) {
      throw failedToCopy(url, targetFile, ex);
    }

    return targetFile;
  }

  private ResolutionException failedToCopy(URL source, Path destination, Exception cause) {
    return new ResolutionException(
        "Failed to copy '" + source + "' to '" + destination + "'", cause
    );
  }

  private void copyToFile(InputStream inputStream, Path file) throws IOException {
    try (var fileStream = new BufferedOutputStream(Files.newOutputStream(file))) {
      inputStream.transferTo(fileStream);
    }
  }

  private String userAgent() {
    return "protobuf-maven-plugin/" + getClass().getPackage().getImplementationVersion()
        + " Apache-Maven/" + Maven.class.getPackage().getImplementationVersion()
        + " Java/" + Runtime.version().toString();
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
}
