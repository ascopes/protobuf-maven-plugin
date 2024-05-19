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

import io.github.ascopes.protobufmavenplugin.sources.ProtoFileListing;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy for registration of sources with the Maven project.
 *
 * <p>This cannot be extended outside of the predefined strategies that already exist in this class.
 *
 * @author Ashley Scopes
 */
public final class SourceRootRegistrar {

  public static final SourceRootRegistrar MAIN =
      new SourceRootRegistrar(
          "main", MavenProject::addCompileSourceRoot, Build::getOutputDirectory);

  public static final SourceRootRegistrar TEST =
      new SourceRootRegistrar(
          "test", MavenProject::addTestCompileSourceRoot, Build::getTestOutputDirectory);

  private static final Logger log = LoggerFactory.getLogger(SourceRootRegistrar.class);

  private final String name;
  private final BiConsumer<MavenProject, String> sourceRootRegistrar;
  private final Function<Build, String> classOutputDirectoryGetter;

  private SourceRootRegistrar(
      String name,
      BiConsumer<MavenProject, String> sourceRootRegistrar,
      Function<Build, String> classOutputDirectoryGetter) {
    this.name = name;
    this.sourceRootRegistrar = sourceRootRegistrar;
    this.classOutputDirectoryGetter = classOutputDirectoryGetter;
  }

  public void registerSourceRoot(MavenSession session, Path path) {
    log.info("Registering {} as a {} source root", path, this);
    sourceRootRegistrar.accept(session.getCurrentProject(), path.toString());
  }

  public void embedListing(MavenSession session, ProtoFileListing listing) throws IOException {
    log.info("Embedding sources from {} in {} class outputs", listing.getProtoFilesRoot(), this);
    var targetDirectory =
        classOutputDirectoryGetter.andThen(Path::of).apply(session.getCurrentProject().getBuild());

    // TODO: extract this logic out to FileUtils and use both here and in the archive extractor
    // as it can be refactored into common code.
    for (var sourceFile : listing.getProtoFiles()) {
      var targetFile =
          FileUtils.changeRelativePath(targetDirectory, listing.getProtoFilesRoot(), sourceFile);
      Files.createDirectories(targetFile.getParent());
      Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Override
  public String toString() {
    return "Maven " + name;
  }
}
