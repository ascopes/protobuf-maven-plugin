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
package io.github.ascopes.protobufmavenplugin;

import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import java.nio.file.Path;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Plugin goal that generates test code from protobuf sources.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate-test",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    threadSafe = true
)
public class TestGenerateMojo extends AbstractGenerateMojo {

  @Override
  protected SourceRootRegistrar sourceRootRegistrar() {
    return SourceRootRegistrar.MAIN;
  }

  @Override
  protected Path defaultOutputDirectory(MavenSession session) {
    return Path.of(session.getCurrentProject().getBuild().getDirectory())
        .resolve("generated-test-sources")
        .resolve("protobuf-maven-plugin");
  }
}
