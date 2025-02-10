![Java 11+](https://img.shields.io/badge/Java-11+-red?logo=openjdk&logoColor=white)
![Maven 3.8](https://img.shields.io/badge/maven-3.8,%204.x-blue?logo=apache-maven)
![GitHub License](https://img.shields.io/github/license/ascopes/protobuf-maven-plugin)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin)](https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin)
[![Documentation](https://img.shields.io/badge/-documentation-purple?logo=apache-maven)](https://ascopes.github.io/protobuf-maven-plugin)
![Libraries.io dependency status for GitHub repo](https://img.shields.io/librariesio/github/ascopes/protobuf-maven-plugin)
![GitHub Release Date](https://img.shields.io/github/release-date/ascopes/protobuf-maven-plugin)

![logo](protobuf-maven-plugin/src/site/resources/images/banner.jpg)

# protobuf-maven-plugin

A scratch-built, modern Maven plugin for seamless protoc integration. Provides support for native
and JVM-based protoc plugins, as well as automatic dependency resolution and incremental code
generation.

> [!NOTE]
> Full documentation with usage examples can be found [within the plugin documentation](https://ascopes.github.io/protobuf-maven-plugin),
> and examples are present [in the integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it).

## Features

- Maven 4 support.
- Pulls `protoc` from Maven Central directly, given a valid version, meaning the plugin is always up-to-date for your use cases.
- Can alternatively invoke `protoc` from the system PATH if you are using an unsupported platform.
- Supports Java and JVM Kotlin sources out of the box.
- Plugin support. Need reactive support, Scala support, or gRPC? Just add the plugin and away you go.
  - Ability to use plugins implemented for the JVM (JAR plugins and classpath plugins) without needing them to be bundled as
    native binaries first.
  - Ability to use regular `protoc` plugins (native binaries).
  - Plugins can be resolved from Maven repositories, URLs, or the system path.
- Generation of main and test sources.
- Importing of `*.proto` sources from classpath dependencies.
- Additional support for generating sources targeting C++, C#, Objective C, Python (including optional static typechecking stubs),
  PHP, Ruby, and Rust.
- Aims to keep builds reproducible and easily debuggable where possible.
- Incremental compilation.

## Quick examples

### Basic generation

Getting started is very simple. The following will compile any sources that are found in
`src/main/protobuf` to Java classes and pop them in `target/generated-sources` where Maven
will automatically discover them and compile them to Java bytecode.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protocVersion>${protobuf-java.version}</protocVersion>
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

> [!TIP]
> Any `*.proto` files that are discovered in project dependencies will be made available to `protoc`,
> so you can import them in exactly the same way you would with Java classes!

### Other language support

Other language generation targets are also available. This plugin provides support for generating all
the languages that protoc supports out of the box, including Kotlin, Python, Python typeshed stubs,
Ruby, PHP, C#, C++, and Rust.

The following will generate Java classes and corresponding Kotlin wrappers:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <kotlinEnabled>true</kotlinEnabled>
    <protocVersion>${protobuf-java.version}</protocVersion>
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

### Plugins

The following snippet will compile any protobuf sources in `src/main/protobuf` to Java source code,
and then proceed to generate gRPC wrappers and Reactor gRPC wrappers.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protocVersion>${protobuf-java.version}</protocVersion>

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

## Why do we need _another_ plugin?

At the time of writing, the existing Maven plugins that provide Protobuf support are not kept up to date. This poses a risk for any applications depending on these plugins as they
are either constrained to outdated versions of Protoc, or are not guaranteed to work in the future.

Many of these existing plugins will not be compatible with the Maven v4.0 APIs.

Some plugins are highly specific to certain CPU architectures as well, which produces issues when using Apple Silicon devices.

All of these issues lingered over the projects I work on that make use of Protobuf. In an attempt to mediate those issues, I have created this Maven
plugin with the following requirements:

- It must support for arbitrary versions of Protoc, including those on the system path.
- It must support invoking binary Protobuf plugins from dependencies, URLs, or the system path.
- It must support invoking Protobuf plugins that are packaged as JARs, without the need to compile native binaries first.
- It must support compiling Protobuf sources from archives and from the local filesystem tree.
- It must be able to allow Protoc to import Protobuf files from other file trees, archives, and JARs transparently.
- It must be aware of the Maven project dependencies.
- It must be compatible with Maven 4.0 once a stable version is released.
