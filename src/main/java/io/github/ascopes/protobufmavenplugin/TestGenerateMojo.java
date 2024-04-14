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

package io.github.ascopes.protobufmavenplugin;

import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generate source code from protobuf files for use in tests.
 *
 * <p>Unlike the {@code generate} goal, these sources will only be visible
 * to tests, and will not be included in any final JAR of the project main
 * sources.
 *
 * <p>Any project dependencies using the {@code compile}, {@code provided},
 * {@code system}, or {@code test} scopes will be made available to import
 * from protobuf sources.
 *
 * <p>By default, sources will be read from {@code src/test/protobuf},
 * and generated sources will be written to
 * {@code target/generated-test-sources/protobuf}.
 *
 * <p>Generally, you won't need to use this. It can be useful in some more
 * specific use cases where you are only using the protobuf definitions
 * within the context of a test.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate-test",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    requiresOnline = true,
    threadSafe = true
)
public final class TestGenerateMojo extends AbstractGenerateMojo {

  @Override
  SourceRootRegistrar sourceRootRegistrar() {
    return SourceRootRegistrar.TEST;
  }

  @Override
  Path defaultSourceDirectory(MavenSession session) {
    return session.getCurrentProject().getBasedir().toPath()
        .resolve("src")
        .resolve("test")
        .resolve("protobuf");
  }

  @Override
  Path defaultOutputDirectory(MavenSession session) {
    return Path.of(session.getCurrentProject().getBuild().getDirectory())
        .resolve("generated-test-sources")
        .resolve("protobuf");
  }
}
