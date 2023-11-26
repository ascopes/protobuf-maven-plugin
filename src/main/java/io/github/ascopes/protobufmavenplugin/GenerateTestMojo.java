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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Mojo to generate test source code from Protobuf sources.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate-test",
    defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
    requiresOnline = true,
    threadSafe = true
)
public final class GenerateTestMojo extends AbstractGenerateMojo {

  /**
   * Initialise this Mojo.
   */
  public GenerateTestMojo() {
    // Nothing to do.
  }


  @Override
  protected Path getDefaultSourceDirectory(Path baseDir) {
    return baseDir.resolve("src").resolve("test").resolve("protobuf");
  }

  @Override
  protected Path getDefaultOutputDirectory(Path targetDir) {
    return targetDir.resolve("generated-test-sources").resolve("protobuf");
  }

  @Override
  protected SourceRootRegistrar getSourceRootRegistrar() {
    return SourceRootRegistrar.TEST;
  }
}
