![Java 11+](https://img.shields.io/badge/Java-11+-blue?logo=openjdk&logoColor=white)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
[![Coverage](https://codecov.io/gh/ascopes/protobuf-maven-plugin/graph/badge.svg?token=bW37uc04cL)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin)](https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin)
[![Documentation](https://img.shields.io/badge/Documentation-latest-blue?logo=apache-maven)](https://ascopes.github.io/protobuf-maven-plugin)

# protobuf-maven-plugin

A simple and modern Maven plugin to generate source code from protobuf definitions.

```xml

<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>3.25.1</protocVersion>

    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>

    <jvmMavenPlugins>
      <jvmMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
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

## Usage

Full documentation with usage examples can be found [within the plugin documentation](https://ascopes.github.io/protobuf-maven-plugin),
and  examples are present [in the integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/src/it).

## Contributing

Since this is a new project, any contributions are always welcome! This includes contributing integration test cases or reporting issues.
Any input is greatly appreciated 😊.
