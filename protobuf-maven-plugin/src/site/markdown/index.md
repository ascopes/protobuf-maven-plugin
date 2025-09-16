# Introduction

<div id="pmp-toc"></div>

The Protobuf Maven Plugin is a modern Maven plugin that attempts to reduce the hassle needed to
integrate Protobuf compilation into your build process.

Unlike existing Protobuf integrations, this plugin is able to pull the desired version of
`protoc` directly from Google's releases on Maven Central. This means you do not need to update 
the version of this plugin to be able to pull in a newer version of `protoc` when it is released.

If your system is not directly supported by Google in the `protoc` releases they supply, you can
instruct the plugin to instead invoke `protoc` from your system path directly.

For users who are writing gRPC services, this plugin can also support generating the gRPC stubs
for you automatically.

In addition to generating Java sources, this plugin can also generate Kotlin sources.

## Bugs and feature requests

Please raise any bugs or feature requests on 
[the GitHub project for this plugin](https://github.com/ascopes/protobuf-maven-plugin/issues).

## Detailed examples

If you need detailed working examples to use as reference, then the 
[integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it)
are a great place to start, since they are full working Maven projects.

If you wish to contribute additional test cases to show integration with custom plugins or more
complicated use cases, then this is always welcome.

## Stars

[![Star History Chart](https://api.star-history.com/svg?repos=ascopes/protobuf-maven-plugin&type=Timeline)](https://www.star-history.com/#ascopes/protobuf-maven-plugin&Timeline)
