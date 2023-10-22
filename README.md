# protobuf-maven-plugin

A work-in-progress to build a modern Maven plugin for generating protobuf
Java sources by invoking `protoc`.

## Aims

- Maven 4.0 compatibility
- Multiplatform-compatible
- Support for invoking `protoc` from the `$PATH`
- Support for downloading a specified version of `protoc`
    - Ability to resolve any desired version
    - Resolution via the Maven dependency resolver to support building behind
    - corporate proxies and Maven proxy repositories.
- Java 11+ support
