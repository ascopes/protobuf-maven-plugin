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
package io.github.ascopes.protobufmavenplugin.dependencies;

import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;

/**
 * Factory that can produce classifiers for dependencies based on the current platform.
 *
 * @author Ashley Scopes
 */
@Description("Generates classifiers for protoc binaries based on the current platform")
@MojoExecutionScoped
@Named
public final class PlatformClassifierFactory {

  private final HostSystem hostSystem;
  private final Properties platformMapping;

  @Inject
  public PlatformClassifierFactory(HostSystem hostSystem) throws IOException {
    this.hostSystem = hostSystem;

    try (var is = getClass().getResourceAsStream("platforms.properties")) {
      platformMapping = new Properties();
      platformMapping.load(is);
    }
  }

  public String getClassifier(String binaryName) throws ResolutionException {
    String osKey;

    if (hostSystem.isProbablyLinux()) {
      osKey = "linux";
    } else if (hostSystem.isProbablyMacOs()) {
      osKey = "macos";
    } else if (hostSystem.isProbablyWindows()) {
      osKey = "windows";
    } else {
      osKey = "unknown";
    }

    var rawArch = hostSystem.getCpuArchitecture();
    var classifier = platformMapping.getProperty(osKey + "." + rawArch);

    if (classifier != null) {
      return classifier;
    }

    var rawOs = hostSystem.getOperatingSystem();
    throw new ResolutionException(
        "No '" + binaryName + "' binary is available for reported OS '"
            + rawOs + "' and CPU architecture '" + rawArch + "'"
    );
  }
}
