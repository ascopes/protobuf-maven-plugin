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
package io.github.ascopes.protobufmavenplugin.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import org.jspecify.annotations.Nullable;


/**
 * Helper type for computing and comparing various types of digest.
 *
 * @author Ashley Scopes
 * @since 3.5.0
 */
public final class Digest {
  private final String algorithm;
  private final byte[] digest;

  private Digest(String algorithm, byte[] digest) {
    this.algorithm = algorithm;
    this.digest = digest;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof Digest)) {
      return false;
    }

    var that = (Digest) other;

    return algorithm == that.algorithm && Arrays.equals(digest, that.digest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(algorithm, digest);
  }

  @Override
  public String toString() {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(digest);
  }

  public boolean matchesDigestOf(InputStream inputStream) {
    return compute(algorithm, inputStream).equals(this);
  }

  public static Digest from(String algorithm, String b64Data) {
    // Validate the algorithm exists in this JVM, and
    // de-alias it.
    var messageDigest = getMessageDigest(algorithm);
    var data = Base64.getDecoder().decode(b64Data);
    return new Digest(messageDigest.getAlgorithm(), data);
  }

  public static Digest compute(String algorithm, String text) {
    return compute(algorithm, text.getBytes(StandardCharsets.UTF_16LE));
  }

  public static Digest compute(String algorithm, byte[] data) {
    return compute(algorithm, new ByteArrayInputStream(data));
  }

  public static Digest compute(String algorithm, InputStream inputStream) {
    try {
      var messageDigest = getMessageDigest(algorithm);
      var buff = new byte[4_096];
      int offset;

      while ((offset = inputStream.read(buff)) != -1) {
        messageDigest.update(buff, 0, offset);
      }

      return new Digest(messageDigest.getAlgorithm(), messageDigest.digest());
    } catch (IOException ex) {
      throw new UncheckedIOException(
          "Failed to read data while computing " + algorithm + " digest",
          ex
      );
    }
  }

  private static MessageDigest getMessageDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalArgumentException(
          "No digest named " + algorithm + " is supported on this system",
          ex
      );
    }
  }
}
