# protobuf-maven-plugin

A work-in-progress to build a modern Maven plugin for generating protobuf
Java and Kotlin sources by automatically obtaining and invoking `protoc`.

## Aims

- Support for downloading a specified version of `protoc` from Maven repositories.
  - Ability to resolve any desired version
  - Will work automatically with corporate proxies and self-hosted Maven repositories like Nexus,
    Maven, GitLab, GitHub Packages.
- Support for invoking `protoc` from the `$PATH` if you have an obscure system that does not provide
  an official `protoc` binary release from Google.
- Java 11+ support.
- Support for generating Java and Kotlin sources.
