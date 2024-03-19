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
import io.github.ascopes.protobufmavenplugin.platform.Digests;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
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

  private final HttpClient httpClient;
  private final TemporarySpace temporarySpace;

  @Inject
  public UrlResourceFetcher(TemporarySpace temporarySpace) {
    httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .followRedirects(Redirect.NORMAL)
        .build();
    this.temporarySpace = temporarySpace;
  }

  public Path fetchFileFromUrl(URL url, String defaultExtension) throws ResolutionException {
    switch (url.getProtocol().toLowerCase()) {
      case "file":
        return handleFileSystemUrl(url);
      case "http":
      case "https":
        return handleHttpRequest(url, defaultExtension);
      default:
        return handleOtherUrl(url, defaultExtension);
    }
  }

  private Path handleFileSystemUrl(URL url) throws ResolutionException {
    try {
      return Path.of(url.toURI());
    } catch (URISyntaxException ex) {
      throw new ResolutionException("Failed to resolve '" + url + "' due to malformed syntax", ex);
    }
  }

  private Path handleHttpRequest(URL url, String extension) throws ResolutionException {
    // Use the HTTP Client API for this rather than the old HttpUrlConnection API as this
    // is easier to manipulate and fetch responses from without lots of odd error handling.
    var targetFile = targetFile(url, extension);

    try {
      var uri = url.toURI();

      var req = HttpRequest
          .newBuilder()
          .GET()
          .uri(uri)
          .header(USER_AGENT, userAgent())
          .build();

      log.info("Performing HTTP request to {} to download resources into {}", url, targetFile);

      var resp = httpClient
          .send(req, BodyHandlers.ofInputStream());

      handleResponse(resp, targetFile);

    } catch (URISyntaxException | IOException | InterruptedException ex) {
      throw failedToCopy(url, targetFile, ex);
    }

    return targetFile;
  }

  private void handleResponse(
      HttpResponse<InputStream> response,
      Path targetFile
  ) throws IOException, ResolutionException {
    var status = response.statusCode();

    try (var responseBody = response.body()) {
      // Successful response (200 OK), stream it to a file.
      if (status == 200) {
        log.info(
            "{} {} returned {}, streaming response...",
            response.request().method(),
            response.request().uri(),
            response.statusCode()
        );

        try (var fileStream = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
          responseBody.transferTo(fileStream);
        }

        return;
      }

      // Handle errors instead.
      try (var baos = new ByteArrayOutputStream()) {
        responseBody.transferTo(baos);

        // Use 8-bit ASCII to avoid dodgy data causing encoding errors. This is best-effort only.
        var body = baos.toString(StandardCharsets.ISO_8859_1);

        throw new ResolutionException(
            "Failed to request '" + response.uri() + "', received a " + status
                + " status with body: " + body
        );
      }
    }
  }

  private Path handleOtherUrl(URL url, String extension) throws ResolutionException {
    // For all other purposes, we fall back to the legacy URLConnection, which can handle things
    // like JAR references, FTP server paths, etc.

    var targetFile = targetFile(url, extension);

    try {
      var conn = url.openConnection();
      conn.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
      conn.setReadTimeout((int) CONNECT_TIMEOUT.toMillis());
      conn.setAllowUserInteraction(false);
      conn.setDoOutput(false);
      conn.setRequestProperty(USER_AGENT, userAgent());
      conn.setUseCaches(true);

      log.info("Connecting to '{}' (URLConnection) to copy resourcces to '{}'", url, targetFile);

      conn.connect();

      try (var responseBody = conn.getInputStream()) {
        copyToFile(responseBody, targetFile);
      }

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
