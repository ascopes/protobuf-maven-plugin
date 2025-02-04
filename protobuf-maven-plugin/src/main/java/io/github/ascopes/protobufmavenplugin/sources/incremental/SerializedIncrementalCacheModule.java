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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Custom serializer/deserializer support for the incremental cache serialization in Jackson.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
final class SerializedIncrementalCacheModule extends SimpleModule {
  SerializedIncrementalCacheModule() {
    addKeyDeserializer(Path.class, new PathKeyDeserializer());
    addKeySerializer(Path.class, new PathKeySerializer());
  }

  private static final class PathKeyDeserializer extends KeyDeserializer {
    @Override
    public Path deserializeKey(String key, DeserializationContext context) {
      return Path.of(URI.create(key));
    }
  }

  private static final class PathKeySerializer extends StdSerializer<Path> {
    private PathKeySerializer() {
      super(Path.class);
    }

    @Override
    public void serialize(
        Path path,
        JsonGenerator generator,
        SerializerProvider provider
    ) throws IOException {
      generator.writeFieldName(path.toUri().toASCIIString());
    }
  }
}
