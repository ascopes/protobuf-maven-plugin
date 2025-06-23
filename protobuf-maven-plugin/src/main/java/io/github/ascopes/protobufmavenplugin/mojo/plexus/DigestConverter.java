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
package io.github.ascopes.protobufmavenplugin.mojo.plexus;

import io.github.ascopes.protobufmavenplugin.utils.Digest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;

/**
 * Converter for {@link Digest}s.
 *
 * @author Ashley Scopes
 * @since 3.5.0
 */
final class DigestConverter extends AbstractBasicConverter {

  private static final Pattern PATTERN = Pattern.compile(
      "^(?<algorithm>[-a-z0-9]+):(?<digest>[0-9a-f]+)$",
      Pattern.CASE_INSENSITIVE
  );

  private static final Map<String, String> DIGEST_ALIASES;

  static {
    var digestAliases = new TreeMap<String, String>(String::compareToIgnoreCase);
    digestAliases.put("sha1", "SHA-1");
    digestAliases.put("sha224", "SHA-224");
    digestAliases.put("sha256", "SHA-256");
    digestAliases.put("sha384", "SHA-384");
    digestAliases.put("sha512", "SHA-512");
    DIGEST_ALIASES = Collections.unmodifiableMap(digestAliases);
  }

  @Override
  public boolean canConvert(Class<?> type) {
    return Digest.class.equals(type);
  }

  @Override
  protected Object fromString(String str) throws ComponentConfigurationException {
    // Users may wish to split digests into more than one line
    // so they can satisfy tools like spotless. Support this by
    // yanking any whitespace prior to matching the string.
    str = removeWhitespace(str);
    var matcher = PATTERN.matcher(str);

    if (!matcher.matches()) {
      throw new ComponentConfigurationException(
          "Failed to parse digest '" + str + "'. "
              +  "Ensure that the digest is in a format such as "
              +  "'sha512:1a2b3c4d', where the digest is a hexadecimal-encoded"
              + "string."
      );
    }

    try {
      var algorithm = matcher.group("algorithm");
      algorithm = DIGEST_ALIASES.getOrDefault(algorithm, algorithm)
          .toUpperCase(Locale.ROOT);

      var digest = matcher.group("digest")
          .toLowerCase(Locale.ROOT);

      return Digest.from(algorithm, digest);
    } catch (Exception ex) {
      throw new ComponentConfigurationException(
          "Failed to parse digest '" + str + "': " + ex,
          ex
      );
    }
  }

  private static String removeWhitespace(String string) {
    var sb = new StringBuilder();
    for (var i = 0; i < string.length(); ++i) {
      var c = string.charAt(i);
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
