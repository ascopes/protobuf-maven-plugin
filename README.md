![Java 11+](https://img.shields.io/badge/Java-11+-blue?logo=openjdk&logoColor=white)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
<!--[![codecov](https://codecov.io/gh/ascopes/protobuf-maven-plugin/graph/badge.svg?token=bW37uc04cL)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)-->

# protobuf-maven-plugin

A simple and modern Maven plugin to generate Java/Kotlin sources from Protobuf definitions.

## Features

- Support for downloading a specified version of `protoc` from Maven repositories.
  - Ability to resolve any desired version
  - Will work automatically with corporate proxies and self-hosted Maven repositories like Nexus,
    Maven, GitLab, GitHub Packages.
- Support for invoking `protoc` from the `$PATH` if you have an obscure system that does not provide
  an official `protoc` binary release from Google.
- Java 11+ support.
- Ability to generate sources for Protobuf message payloads.
- Ability to optionally generate source stubs for GRPC services.
- Option to also generate Kotlin stubs that wrap the Java sources.

## Usage

Full documentation with usage examples can be found [on the plugin site](https://ascopes.github.io/protobuf-maven-plugin).

To give a quick idea of what this looks like, see the following example:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>3.25.0</protocVersion>
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
