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
package io.github.ascopes.protobufmavenplugin.sources.incremental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import net.minidev.json.JSONObject;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("IncrementalCacheSerializer test")
class IncrementalCacheSerializerTest {

  static @TempDir Path someDir;

  @DisplayName("I can serialize caches into JSON")
  @MethodSource("cacheExamples")
  @ParameterizedTest(name = "for {argumentSetName}")
  void canSerializeCachesIntoJson(IncrementalCache cache, JSONObject object) throws IOException {
    // Given
    var serializer = new IncrementalCacheSerializer();

    // When
    var writer = new StringWriter();
    serializer.serialize(cache, writer);

    // Then
    assertEquals(object.toJSONString(), writer.toString(), true);
  }

  @DisplayName("serialization rethrows any errors")
  @Test
  void serializationRethrowsAnyErrors() {
    // Given
    var serializer = new IncrementalCacheSerializer();

    // Then
    assertThatThrownBy(() -> serializer.serialize(null, null))
        .isInstanceOf(IOException.class)
        .hasMessage("Failed to write JSON file");
  }

  @DisplayName("I can deserialize JSON into caches")
  @MethodSource("cacheExamples")
  @ParameterizedTest(name = "for {argumentSetName}")
  void canDeserializeJsonIntoCaches(IncrementalCache cache, JSONObject object) throws IOException {
    // Given
    var serializer = new IncrementalCacheSerializer();
    var reader = new StringReader(object.toJSONString());

    // When
    var actualObj = serializer.deserialize(reader);

    // Then
    assertThat(actualObj).isEqualTo(cache);
  }

  @DisplayName("deserialization rethrows any errors")
  @Test
  void deserializationRethrowsAnyErrors() {
    // Given
    var serializer = new IncrementalCacheSerializer();
    var input = "{";

    // Then
    assertThatThrownBy(() -> serializer.deserialize(new StringReader(input)))
        .isInstanceOf(IOException.class)
        .hasMessage("Failed to read JSON file")
        .hasCauseInstanceOf(JSONException.class);
  }


  static Stream<Arguments> cacheExamples() {
    return Stream.of(
        argumentSet(
            "empty object",
            ImmutableIncrementalCache.builder().build(),
            new JSONObject()
                .appendField("dependencies", new JSONObject())
                .appendField("sources", new JSONObject())
        ),
        argumentSet(
            "only dependencies",
            ImmutableIncrementalCache.builder()
                .dependencies(Map.of(
                    path(someDir, "foo", "bar"), "1a2b3c4d",
                    path(someDir, "eh", "nah"), "eh-nah-na!"
                ))
                .build(),
            new JSONObject()
                .appendField("dependencies", new JSONObject()
                    .appendField(uri(someDir, "foo", "bar"), "1a2b3c4d")
                    .appendField(uri(someDir, "eh", "nah"), "eh-nah-na!"))
                .appendField("sources", new JSONObject())
        ),
        argumentSet(
            "only sources",
            ImmutableIncrementalCache.builder()
                .sources(Map.of(
                    path(someDir, "foo", "bar"), "1a2b3c4d",
                    path(someDir, "eh", "nah"), "eh-nah-na!"
                ))
                .build(),
            new JSONObject()
                .appendField("dependencies", new JSONObject())
                .appendField("sources", new JSONObject()
                    .appendField(uri(someDir, "foo", "bar"), "1a2b3c4d")
                    .appendField(uri(someDir, "eh", "nah"), "eh-nah-na!"))
        ),
        argumentSet(
            "dependencies and sources",
            ImmutableIncrementalCache.builder()
                .dependencies(Map.of(
                    path(someDir, "kimi", "wa"), "sudeni shinde iru",
                    path(someDir, "watashi", "wa"), "taikutsudesu"
                ))
                .sources(Map.of(
                    path(someDir, "foo", "bar"), "1a2b3c4d",
                    path(someDir, "eh", "nah"), "eh-nah-na!"
                ))
                .build(),
            new JSONObject()
                .appendField("dependencies", new JSONObject()
                    .appendField(uri(someDir, "kimi", "wa"), "sudeni shinde iru")
                    .appendField(uri(someDir, "watashi", "wa"), "taikutsudesu"))
                .appendField("sources", new JSONObject()
                    .appendField(uri(someDir, "foo", "bar"), "1a2b3c4d")
                    .appendField(uri(someDir, "eh", "nah"), "eh-nah-na!"))
        )
    );
  }

  static Path path(Path base, String... frags) {
    var path = base;
    for (var frag : frags) {
      path = path.resolve(frag);
    }
    return path;
  }

  static String uri(Path base, String... frags) {
    return path(base, frags).toUri().toASCIIString();
  }
}
