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
package io.github.ascopes.protobufmavenplugin.generate;

import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;

/**
 * Registrar for source roots.
 *
 * @author Ashley Scopes
 */
@FunctionalInterface
public interface SourceRootRegistrar {
  SourceRootRegistrar MAIN = of("main", (session, path) -> session.getCurrentProject()
      .addCompileSourceRoot(path.toString()));
  SourceRootRegistrar TEST = of("test", (session, path) -> session.getCurrentProject()
      .addTestCompileSourceRoot(path.toString()));

  void registerSourceRoot(MavenSession session, Path path);

  static SourceRootRegistrar of(String name, SourceRootRegistrar registrar) {
    return new SourceRootRegistrar() {
      @Override
      public void registerSourceRoot(MavenSession session, Path path) {
        registrar.registerSourceRoot(session, path);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }
}
