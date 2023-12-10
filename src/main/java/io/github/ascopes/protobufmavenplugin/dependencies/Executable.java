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

package io.github.ascopes.protobufmavenplugin.dependencies;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;

/**
 * Representation of an executable that can be resolved from the
 * {@code $PATH} or from a Maven Central repository.
 *
 * @author Ashley Scopes
 */
public final class Executable {

  // Common executables we care about

  /**
   * The Protobuf Compiler.
   */
  public static final Executable PROTOC = new Executable("com.google.protobuf", "protoc");

  /**
   * The GRPC plugin for the Protobuf Compiler that can generate Java service code.
   */
  public static final Executable PROTOC_GEN_GRPC_JAVA = new Executable("io.grpc", "protoc-gen-grpc-java");

  /**
   * The GRPC plugin for the Protobuf Compiler that can generate Kotlin service code.
   */
  public static final Executable PROTOC_GEN_GRPC_KOTLIN = new Executable("io.grpc", "protoc-gen-grpc-kotlin");

  // Implementation details
  private final String groupId;
  private final String artifactId;

  /**
   * Initialise the executable.
   *
   * @param groupId the group ID.
   * @param artifactId the artifact ID (also treated as the executable name).
   */
  public Executable(String groupId, String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  /**
   * Get the executable name.
   *
   * @return the executable name.
   */
  public String getExecutableName() {
    return artifactId;
  }

  /**
   * Resolve the Maven artifact coordinate for the given version of this
   * executable.
   * 
   * @param version the artifact version.
   * @return the coordinate.
   * @throws DependencyResolutionException if the coordinate cannot be
   *     resolved for the current platform.
   */
  public MavenCoordinate getMavenArtifactCoordinate(String version) throws DependencyResolutionException {
    return new MavenCoordinate(
        groupId,
        artifactId,
        version,
        "exe",
        getMavenClassifier()
    );
  }

  private String getMavenClassifier() throws DependencyResolutionException {
    String classifier;

    if (HostEnvironment.isWindows()) {
      classifier = "windows-" + determineArchitectureForWindows();
    } else if (HostEnvironment.isLinux()) {
      classifier = "linux-" + determineArchitectureForLinux();
    } else if (HostEnvironment.isMacOs()) {
      classifier = "osx-" + determineArchitectureForMacOs();
    } else {
      throw new DependencyResolutionException(
          "No resolvable version of " + artifactId + " for the current OS found");
    }

    return classifier;
  }

  private String determineArchitectureForWindows() throws DependencyResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "amd64":
      case "x86_64":
        return "x86_64";

      case "x86":
      case "x86_32":
        return "x86_32";

      default:
        throw noResolvableExecutableFor("Windows", arch);
    }
  }

  private String determineArchitectureForLinux() throws DependencyResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      // https://bugs.openjdk.org/browse/JDK-8073139
      case "ppc64le":
      case "ppc64":
        return "ppcle_64";

      case "s390":
      case "zarch_64":
        return "s390_64";

      case "aarch64":
        return "aarch_64";

      case "amd64":
        return "x86_64";

      default:
        throw noResolvableExecutableFor("Linux", arch);
    }
  }

  private String determineArchitectureForMacOs() throws DependencyResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "aarch64":
        return "aarch_64";

      case "amd64":
      case "x86_64":
        return "x86_64";

      default:
        throw noResolvableExecutableFor("Mac OS", arch);
    }
  }

  private DependencyResolutionException noResolvableExecutableFor(String os, String arch) {
    var message = "No resolvable " + artifactId + " version for " + os
        + " '" + arch + "' systems found";
    return new DependencyResolutionException(message);
  }
}
