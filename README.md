![Java 11+](https://img.shields.io/badge/Java-11+-blue?logo=openjdk&logoColor=white)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
<!--[![codecov](https://codecov.io/gh/ascopes/protobuf-maven-plugin/graph/badge.svg?token=bW37uc04cL)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)-->

# protobuf-maven-plugin

A simple and modern Maven plugin to generate Java/Kotlin sources from Protobuf definitions.

## Aims

- Support for downloading a specified version of `protoc` from Maven repositories.
  - Ability to resolve any desired version
  - Will work automatically with corporate proxies and self-hosted Maven repositories like Nexus,
    Maven, GitLab, GitHub Packages.
- Support for invoking `protoc` from the `$PATH` if you have an obscure system that does not provide
  an official `protoc` binary release from Google.
- Java 11+ support.
- Support for generating Java and Kotlin sources.

## Usage

The proposed usage at the time of writing will be something along the lines of:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <!-- Version of protoc to use, as defined at
         https://mvnrepository.com/artifact/com.google.protobuf/protoc -->
    <version>3.25.0</version>
    <!-- Fail if protoc raises any warnings -->
    <fatalWarnings>true</fatalWarnings>
  </configuration>
</plugin>
```
