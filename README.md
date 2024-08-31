![Java 11+](https://img.shields.io/badge/Java-11+-red?logo=openjdk&logoColor=white)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin)](https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin)
[![Documentation](https://img.shields.io/badge/-Documentation-purple?logo=apache-maven)](https://ascopes.github.io/protobuf-maven-plugin)

# protobuf-maven-plugin

A scratch-built and modern Maven plugin for seamless protoc integration, with support for GRPC and custom plugins.

```xml

<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>4.28.0</protocVersion>

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

At the time of writing, the existing Maven plugins that provide Protobuf support are not kept up to date. This poses a risk for any applications depending on these plugins as they
are either constrained to outdated versions of Protoc, or are not guaranteed to work in the future.

Maven 4.0 will eventually be released, and many of these existing plugins will not be compatible with the v4.0 API.

Some plugins are highly specific to certain CPU architectures as well, which produces issues when using Apple Silicon devices.

All of these issues lingered over the projects I work on that make use of Protobuf. In an attempt to mediate those issues, I have created this Maven
plugin with the following requirements:

- It must support for arbitrary versions of Protoc, including those on the system path.
- It must support invoking binary Protobuf plugins from dependencies, URLs, or the system path.
- It must support invoking Protobuf plugins that are packaged as JARs, without the need to compile native binaries first.
- It must support compiling Protobuf sources from archives and from the local filesystem tree.
- It must be able to allow Protoc to import Protobuf files from other file trees, archives, and JARs transparently.
- It must be aware of the Maven project dependencies.
- It must have the ability to be migrated to Maven 4.0 fairly rapidly when the time comes.
