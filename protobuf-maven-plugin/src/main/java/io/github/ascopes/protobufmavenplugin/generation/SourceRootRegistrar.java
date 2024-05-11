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

package io.github.ascopes.protobufmavenplugin.generation;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registrar for source roots.
 *
 * @author Ashley Scopes
 */
@FunctionalInterface
public interface SourceRootRegistrar {

  MavenRegistrar MAIN = new MavenRegistrar("main", MavenProject::addCompileSourceRoot);
  MavenRegistrar TEST = new MavenRegistrar("test", MavenProject::addTestCompileSourceRoot);

  void registerSourceRoot(MavenSession session, Path path);

  /**
   * A basic registrar for {@link MavenProject} sources.
   */
  final class MavenRegistrar implements SourceRootRegistrar {

    static final Logger log = LoggerFactory.getLogger(SourceRootRegistrar.class);

    private final String name;
    private final BiConsumer<MavenProject, String> delegate;

    private MavenRegistrar(String name, BiConsumer<MavenProject, String> delegate) {
      this.name = name;
      this.delegate = delegate;
    }

    @Override
    public void registerSourceRoot(MavenSession session, Path path) {
      log.info("Registering {} as a {} source root", path, this);
      delegate.accept(session.getCurrentProject(), path.toString());
    }

    @Override
    public String toString() {
      return "Maven " + name;
    }
  }
}
