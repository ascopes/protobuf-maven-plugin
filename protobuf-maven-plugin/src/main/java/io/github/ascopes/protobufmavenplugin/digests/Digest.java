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
package io.github.ascopes.protobufmavenplugin.digests;

import io.github.ascopes.protobufmavenplugin.utils.Unchecked;
import io.github.ascopes.protobufmavenplugin.utils.VisibleForTestingOnly;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Helper type for computing and comparing various types of digest.
 *
 * @author Ashley Scopes
 * @since 3.5.0
 */
public final class Digest {
  private static final HexFormat HEX = HexFormat.of();

  private final String algorithm;
  private final byte[] digest;

  @VisibleForTestingOnly
  Digest(String algorithm, byte[] digest) {
    this.algorithm = algorithm;
    this.digest = digest;
  }

  public byte[] getDigest() {
    return digest;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other instanceof Digest that) {
      return Objects.equals(algorithm, that.algorithm) && Arrays.equals(digest, that.digest);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return algorithm.hashCode() ^ Arrays.hashCode(digest);
  }

  @Override
  public String toString() {
    return algorithm + ":" + HEX.formatHex(digest);
  }

  public String toHexString() {
    return HEX.formatHex(digest);
  }

  public void verify(InputStream inputStream) throws IOException {
    var actualDigest = compute(algorithm, inputStream);
    if (!actualDigest.equals(this)) {
      throw new DigestException(
          "Actual digest '"
              + actualDigest
              + "' does not match expected digest '"
              + this
              + "'"
      );
    }
  }

  public static Digest from(String algorithm, String hex) {
    // Validate the algorithm exists in this JVM, and
    // de-alias it. We could possibly optimize this in the future
    // to check the length/2 before decoding.
    var messageDigest = getMessageDigest(algorithm);

    byte[] data;
    try {
      data = HEX.parseHex(hex);
    } catch (IllegalArgumentException ex) {
      throw invalidDigest(algorithm, hex, ex.getMessage(), ex);
    }

    if (data.length != messageDigest.getDigestLength()) {
      throw invalidDigest(
          algorithm,
          hex,
          "illegal length " + data.length + ", expected " + messageDigest.getDigestLength()
              + " bytes.",
          null
      );
    }

    return new Digest(messageDigest.getAlgorithm(), data);
  }

  public static Digest compute(String algorithm, String text) {
    return compute(algorithm, text.getBytes(StandardCharsets.UTF_8));
  }

  public static Digest compute(String algorithm, byte[] data) {
    return Unchecked.call(() -> compute(algorithm, new ByteArrayInputStream(data)));
  }

  public static Digest compute(String algorithm, InputStream inputStream) throws IOException {
    var messageDigest = getMessageDigest(algorithm);
    var buff = new byte[8_192];
    int offset;

    while ((offset = inputStream.read(buff)) != -1) {
      messageDigest.update(buff, 0, offset);
    }

    return new Digest(messageDigest.getAlgorithm(), messageDigest.digest());
  }

  private static MessageDigest getMessageDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException ex) {
      throw new DigestException(
          "Digest '" + algorithm + "' is not supported by this JVM",
          ex
      );
    }
  }

  private static DigestException invalidDigest(
      String inputAlgorithm,
      String inputDigest,
      @Nullable String message,
      @Nullable Exception cause
  ) {
    var fullMessage = "Failed to parse digest \"" + inputAlgorithm + ":" + inputDigest + "\", " 
        + message;
    return cause == null
        ? new DigestException(fullMessage)
        : new DigestException(fullMessage, cause);
  }
}
