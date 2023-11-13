# protobuf-maven-plugin

**A work-in-progress** to build a modern Maven plugin for generating protobuf
Java and Kotlin sources by automatically obtaining and invoking `protoc`.

**This is not yet ready for consumption in a Maven project.** Please watch this space as development is
ongoing!

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
