/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.resolve;

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that can recursively discover `*.proto` files from a given root path.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtoFileResolver {

  private static final Logger log = LoggerFactory.getLogger(ProtoFileResolver.class);

  public Collection<Path> findProtoFiles(Iterable<Path> roots) throws IOException {
    if (roots instanceof Path) {
      // Fix a confusing edge case if we pass a single path in, as paths themselves are iterables
      // of other paths.
      roots = List.of((Path) roots);
    }

    var files = new ArrayList<Path>();

    for (var root : roots) {
      try (var stream = Files.walk(root)) {
        stream
            .filter(this::isProtoFile)
            .peek(file -> log.trace("Found candidate proto source file at '{}'", file))
            .forEach(files::add);
      }
    }

    return files;
  }

  private boolean isProtoFile(Path file) {
    if (!Files.isRegularFile(file)) {
      return false;
    }

    return FileUtils
        .getFileExtension(file)
        .filter(".proto"::equalsIgnoreCase)
        .isPresent();
  }
}
