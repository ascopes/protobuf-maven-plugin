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
package io.github.ascopes.protobufmavenplugin.plexus;

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import java.security.Security;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.eclipse.sisu.Description;

/**
 * Converter for {@link Digest}s.
 *
 * @author Ashley Scopes
 * @since 3.5.0
 */
@Description("Plexus converter for parsing Digest objects")
@Named
@Singleton
final class DigestPlexusConverter extends AbstractBasicConverter implements PlexusConverter {

  private static final Pattern PATTERN = Pattern.compile(
      "^(?<algorithm>[-a-z0-9]+):(?<digest>[0-9a-f]+)$",
      Pattern.CASE_INSENSITIVE
  );

  // This is a special sorted map that maps all MessageDogest algorithms back to
  // themselves. At a glance, this might seem totally pointless, but we provide a
  // key comparator that compares all keys ignoring case and without any hyphen
  // separator characters if present. This enables us to programmatically support
  // all aliases for all available digest algorithms dynamically without hardcoding
  // anything. Effectively this enables us to map things like "sha256" back to
  // "SHA-256" internally.
  private static final Map<String, String> DIGEST_ALIASES = Security.getAlgorithms("MessageDigest")
      .stream()
      .collect(Collectors.collectingAndThen(
          Collectors.toMap(
              Function.identity(),
              Function.identity(),
              (existingAlgorithm, newAlgorithm) -> newAlgorithm,
              () -> new TreeMap<>(Comparator.comparing(alias -> alias.replace("-", "")
                  .toLowerCase(Locale.ROOT)))
          ),
          Collections::unmodifiableMap
      ));

  @Override
  public int getOrder() {
    return 0;
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
              + "Ensure that the digest is in a format such as "
              + "'sha512:1a2b3c4d', where the digest is a hexadecimal-encoded "
              + "string."
      );
    }

    try {
      var algorithm = matcher.group("algorithm");
      algorithm = DIGEST_ALIASES.getOrDefault(algorithm, algorithm);
      var digest = matcher.group("digest");
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
