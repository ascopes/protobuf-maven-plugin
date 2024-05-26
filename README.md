![Java 11+](https://img.shields.io/badge/Java-11+-blue?logo=openjdk&logoColor=white)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin)](https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin)
[![Documentation](https://img.shields.io/badge/Documentation-latest-blue?logo=apache-maven)](https://ascopes.github.io/protobuf-maven-plugin)

# protobuf-maven-plugin

A scratch-built and modern Maven plugin for seamless protoc integration, with support for GRPC and custom plugins.

```xml

<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>4.27.0</protocVersion>

    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>

    <jvmMavenPlugins>
      <jvmMavenPlugin>
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </jvmMavenPlugin>
    </jvmMavenPlugins>
  </configuration>

  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Features

- Pulls `protoc` from Maven Central directly, given a valid version, meaning the plugin is always up-to-date for your use cases.
- Can alternatively invoke `protoc` from the system PATH if you are using an unsupported platform.
- Supports Java and JVM Kotlin sources out of the box.
- Plugin support. Need reactive support, Scala support, or GRPC? Just add the plugin and away you go.
  - Ability to use plugins implemented for the JVM (JAR plugins).
  - Ability to use regular `protoc` plugins (native binaries).
- Generation of main and test sources.
- Importing of `*.proto` sources from classpath dependencies.
- Ready to implement Maven 4 support once Maven 4 is stable, meaning your projects will not be blocked by unmaintained plugins using
  unsupported Maven 2.x APIs.
- Additional support for generating sources targeting C++, C#, Objective C, Python (with and without static typechecking stubs),
  PHP, Ruby, and Rust.

## Usage

Full documentation with usage examples can be found [within the plugin documentation](https://ascopes.github.io/protobuf-maven-plugin),
and  examples are present [in the integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it).

## Why do we need _another_ plugin?

At the time of starting this project, the two most-used Maven plugins had not seen regular 
updates/releases for the best part of 3 years. This presented me with an issue in the team I work in,
as Maven 4.x is slowly approaching, and the existing plugins are not Maven 4.0 compatible. Without
the guarantee of work being done on the existing projects, there was a big risk that we'd be unable
to use Maven 4.0 when it is finally released.

Another issue was that some of the existing plugins did not handle Apple Silicon on Apple Macs
correctly, leading to annoying issues when building.

Finally, the plugin we made use of relied on regular updates to pull in new versions of 
`protoc`. Since this was not happening, we were losing the benefits from any of the newer versions
of `protoc` that were released. We were also seeing an increasing number of build warnings due to
deprecated method calls being made by the older generated code.

Unfortunately, Google appear to mostly use Gradle and Bazel. This means that Maven support has been
left behind in any innovations and improvements. Right now, the best bet is to use Maven Exec Plugin
to generate these sources, but then you have to worry about how you make the `protoc` binary available.

Rather than allowing us to be blocked indefinitely by these issues, I decided to write a new plugin
from scratch with a focus on simplicity and the ability to work across as many platforms and
use-cases as possible. 

This plugin can download the required version of `protoc` for your platform automatically from your
Maven repository. Use Nexus or Artifactory rather than Maven Central? No problem. It will work
automatically with any Maven proxy.

If you are using an obscure architecture, or an environment like Termux on Android (yes, people do
exist who do this), then you are able to make use of a user-installed version of `protoc` from your
system PATH by passing a single flag to Maven. This means you can still build your application.

Additionally, once Maven 4.0 is released, I will be able to update this to a
Maven 4.0-compatible release as soon as possible.

## Contributing

Any input is greatly appreciated and welcome. Be it raising issues for bugs or confusing behaviour,
or raising PRs!
