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
package io.github.ascopes.protobufmavenplugin.platform;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    try {
      var bytes = string.getBytes(StandardCharsets.UTF_8);
      var digest = MessageDigest.getInstance("SHA-1").digest(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception ex) {
      throw new IllegalArgumentException(ex.getMessage(), ex);
    }
  }
}
