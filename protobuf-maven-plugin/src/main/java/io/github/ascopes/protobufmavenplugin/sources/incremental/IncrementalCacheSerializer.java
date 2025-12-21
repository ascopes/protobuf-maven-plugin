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
package io.github.ascopes.protobufmavenplugin.sources.incremental;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Serializer for {@link IncrementalCache} objects.
 *
 * @author Ashley Scopes
 * @since 2.10.5
 */
@Description("Serializes and deserializes incremental cache files")
@MojoExecutionScoped
@Named
final class IncrementalCacheSerializer {
  private static final String PROTO_DEPENDENCIES = "proto_dependencies";
  private static final String PROTO_SOURCES = "proto_sources";
  private static final String DESCRIPTOR_FILES = "descriptor_files";

  void serialize(IncrementalCache cache, Writer writer) throws IOException {
    try {
      new JSONObject()
          .put(PROTO_DEPENDENCIES, pathMappingToJson(cache.getProtoDependencies()))
          .put(PROTO_SOURCES, pathMappingToJson(cache.getProtoSources()))
          .put(DESCRIPTOR_FILES, pathMappingToJson(cache.getDescriptorFiles()))
          .write(writer, 2, 2);
    } catch (Exception ex) {
      throw new IOException("Failed to write JSON file", ex);
    }
  }

  IncrementalCache deserialize(Reader reader) throws IOException {
    try {
      var object = new JSONObject(new JSONTokener(reader));
      return ImmutableIncrementalCache.builder()
          .protoDependencies(jsonToPathMapping(object.getJSONObject(PROTO_DEPENDENCIES)))
          .protoSources(jsonToPathMapping(object.getJSONObject(PROTO_SOURCES)))
          .descriptorFiles(jsonToPathMapping(object.getJSONObject(DESCRIPTOR_FILES)))
          .build();
    } catch (Exception ex) {
      throw new IOException("Failed to read JSON file", ex);
    }
  }

  private JSONObject pathMappingToJson(Map<Path, String> mapping) {
    var object = new JSONObject();
    mapping.forEach((path, hash) -> object.put(path.toUri().toASCIIString(), hash));
    return object;
  }

  private Map<Path, String> jsonToPathMapping(JSONObject object) {
    var mapping = new LinkedHashMap<Path, String>();
    object.keys().forEachRemaining(path -> {
      var hash = object.getString(path);
      mapping.put(Path.of(URI.create(path)), hash);
    });
    return Collections.unmodifiableMap(mapping);
  }
}
