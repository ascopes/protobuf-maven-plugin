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

package io.github.ascopes.protobufmavenplugin.generate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to provide access to temporary spaces on the file system to use during builds.
 *
 * @author Ashley Scopes
 */
@Named
public final class TemporarySpace {

  private static final String FRAG = "protobuf-maven-plugin";

  private static final Logger log = LoggerFactory.getLogger(TemporarySpace.class);

  private final MavenProject mavenProject;

  @Inject
  public TemporarySpace(MavenProject mavenProject) {
    this.mavenProject = mavenProject;
  }

  public Path createTemporarySpace(String... bits) {
    var baseDir = Path.of(mavenProject.getBuild().getDirectory()).resolve(FRAG);

    var dir = reduce(baseDir, bits);
    log.debug("Creating temporary directory at '{}' if it does not already exist...", dir);

    // This should be concurrent-safe as it will not break if the directory already exists unless
    // the directory is instead a regular file.
    try {
      return Files.createDirectories(dir);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to create temporary directory!", ex);
    }
  }

  private Path reduce(Path base, String... bits) {
    var path = base;
    for (var bit : bits) {
      path = path.resolve(bit);
    }
    return path;
  }
}
