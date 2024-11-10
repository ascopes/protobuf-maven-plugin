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

package io.github.ascopes.protobufmavenplugin.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Hashing/digests.
 *
 * @author Ashley Scopes
 */
public final class Digests {

  private Digests() {
    // Static-only class
  }

  public static String sha1(String string) {
    var messageDigest = createMessageDigest("SHA-1");
    var bytes = string.getBytes(StandardCharsets.UTF_8);
    return base64Encode(messageDigest.digest(bytes));
  }

  public static String sha512ForStream(InputStream inputStream) throws IOException {
    var messageDigest = createMessageDigest("SHA-512");
    var buff = new byte[4_096];
    int offset;

    while ((offset = inputStream.read(buff)) != -1) {
      messageDigest.update(buff, 0, offset);
    }

    return base64Encode(messageDigest.digest());
  }

  private static String base64Encode(byte[] digest) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }

  @SuppressWarnings("SameParameterValue")
  private static MessageDigest createMessageDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }
}
