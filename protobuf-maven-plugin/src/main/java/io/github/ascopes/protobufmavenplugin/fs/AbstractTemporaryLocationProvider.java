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
package io.github.ascopes.protobufmavenplugin.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.maven.plugin.MojoExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base for implementing a temporary location somewhere that has a unique path per goal
 * invocation.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractTemporaryLocationProvider {
  private static final String FRAG = "protobuf-maven-plugin";
  private static final Logger log = LoggerFactory.getLogger(
      AbstractTemporaryLocationProvider.class
  );

  private final MojoExecution mojoExecution;

  protected AbstractTemporaryLocationProvider(MojoExecution mojoExecution) {
    this.mojoExecution = mojoExecution;
  }

  protected Path resolveAndCreateDirectory(Path basePath, String... bits) throws IOException {
    // GH-488: Execution ID and goal can potentially be null, e.g. in Quarkus dev mode, so
    // default to a semi-sensible value to prevent a NullPointerException.
    var goal = Objects.requireNonNullElse(
        mojoExecution.getGoal(),
        "unknown-goal"
    );
    var executionId = Objects.requireNonNullElse(
        mojoExecution.getExecutionId(),
        "unknown-execution-id"
    );

    var dir = basePath.resolve(FRAG)
        // GH-421: Include the execution ID and goal to keep file paths unique
        // between invocations in multiple goals.
        .resolve(goal)
        .resolve(executionId);

    for (var bit : bits) {
      dir = dir.resolve(bit);
    }

    log.trace("Creating temporary location at '{}' if it does not already exist...", dir);

    // This should be concurrent-safe as it will not break if the directory already exists unless
    // the directory is instead a regular file.
    return Files.createDirectories(dir);
  }
}
