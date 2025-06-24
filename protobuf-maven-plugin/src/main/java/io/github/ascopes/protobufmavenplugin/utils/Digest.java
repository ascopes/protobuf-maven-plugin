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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

  // Visible for testing only.
  Digest(String algorithm, byte[] digest) {
    this.algorithm = algorithm;
    this.digest = digest;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public byte[] getDigest() {
    return digest;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof Digest)) {
      return false;
    }

    var that = (Digest) other;

    return Objects.equals(algorithm, that.algorithm) && Arrays.equals(digest, that.digest);
  }

  @Override
  public int hashCode() {
    return algorithm.hashCode() ^ Arrays.hashCode(digest);
  }

  @Override
  public String toString() {
    return algorithm + ":" + encodeHex(digest);
  }

  public String toHexString() {
    return encodeHex(digest);
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
    // de-alias it. We could possibly optimise this in the future
    // to check the length/2 before decoding.
    var messageDigest = getMessageDigest(algorithm);
    var data = decodeHex(hex);

    if (data.length != messageDigest.getDigestLength()) {
      throw new DigestException(
          "Illegal length "
              + data.length
              + " for decoded digest '"
              + hex
              + "' with algorithm '"
              + messageDigest.getAlgorithm()
              + "', expected "
              + messageDigest.getDigestLength()
      );
    }

    return new Digest(messageDigest.getAlgorithm(), data);
  }

  public static Digest compute(String algorithm, String text) {
    return compute(algorithm, text.getBytes(StandardCharsets.UTF_8));
  }

  public static Digest compute(String algorithm, byte[] data) {
    try {
      return compute(algorithm, new ByteArrayInputStream(data));
    } catch (IOException ex) {
      throw new IllegalStateException("unreachable", ex);
    }
  }

  public static Digest compute(String algorithm, InputStream inputStream) throws IOException {
    var messageDigest = getMessageDigest(algorithm);
    var buff = new byte[4_096];
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

  private static byte[] decodeHex(String hex) {
    // Hex contains two digits per byte, and we disallow denoting
    // sign here as it makes little sense. If we have an invalid length
    // string, then let it raise an IndexOutOfBoundsException to simplify
    // error handling.
    var decoded = new byte[hex.length() / 2];
    for (var i = 0; i < hex.length(); i += 2) {
      try {
        var hexByte = hex.substring(i, i + 2);
        decoded[i / 2] = (byte) Integer.parseUnsignedInt(hexByte, 16);
      } catch (NumberFormatException | IndexOutOfBoundsException ex) {
        throw new DigestException(
            "Invalid hex byte at position "
                + (i + 1)
                + " in hexadecimal string '"
                + hex
                + "'",
            ex
        );
      }
    }
    return decoded;
  }

  private static String encodeHex(byte[] data) {
    var sb = new StringBuilder();
    for (var b : data) {
      sb.append(Integer.toHexString(Byte.toUnsignedInt(b)));
    }
    return sb.toString();
  }
}
