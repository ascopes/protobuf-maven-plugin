# protobuf-maven-plugin

A work-in-progress to build a modern Maven plugin for generating protobuf
Java and Kotlin sources by automatically obtaining and invoking `protoc`.

## Aims

- Multiplatform-compatible.
- Support for invoking `protoc` from the `$PATH`
- Support for downloading a specified version of `protoc`
    - Ability to resolve any desired version
    - Resolution via the Maven dependency resolver to support building behind
    - corporate proxies and Maven proxy repositories.
- Java 11+ support.
- Support for generating Java and Kotlin sources.
