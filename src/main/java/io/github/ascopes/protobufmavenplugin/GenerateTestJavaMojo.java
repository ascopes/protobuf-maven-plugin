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

import java.nio.file.Path;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

/**
 * Mojo to generate Java code from protobuf test sources.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate-test-java",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    requiresOnline = true,
    threadSafe = true
)
public final class GenerateTestJavaMojo extends AbstractGenerateMojo {

  @Override
  protected String getSourceOutputType() {
    return "java";
  }

  @Override
  protected void registerSource(MavenProject project, Path path) {
    project.addTestCompileSourceRoot(path.toString());
  }
}
