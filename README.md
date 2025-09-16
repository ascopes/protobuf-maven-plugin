<h1 align="center">Protobuf Maven Plugin</h1>

<img align="center" alt="logo" src="protobuf-maven-plugin/src/site/resources/images/banner.jpg">

<p align="center">  
  <!-- Note: do not put inner tags on newlines within <a/>, it messes up the rendering of text decorations leaving blue underlines between badges. -->
  <img alt="Java 11+" src="https://img.shields.io/badge/Java-11+-red?logo=openjdk&logoColor=white">
  <img alt="Maven 3.8" src="https://img.shields.io/badge/maven-3.8,%204.x-blue?logo=apache-maven">
  <img alt="GitHub License" src="https://img.shields.io/github/license/ascopes/protobuf-maven-plugin">
  <a href="https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml"><img alt="Build Status" src="https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main"></a>
  <a href="https://codecov.io/gh/ascopes/protobuf-maven-plugin"><img alt="Coverage" src="https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main"></a>
  <a href="https://search.maven.org/search?q=g:%22io.github.ascopes%22%20AND%20a:%22protobuf-maven-plugin%22"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin.svg?label=Maven%20Central"></a>
  <a href="https://ascopes.github.io/protobuf-maven-plugin"><img alt="Documentation" src="https://img.shields.io/badge/documentation-latest-purple?logo=apache-maven"></a>
  <img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/ascopes/protobuf-maven-plugin">
</p>


A scratch-built, modern Maven plugin for seamless `protoc` integration. Provides support for native
and JVM-based `protoc` plugins, as well as automatic dependency resolution and incremental code
generation.

> [!NOTE]
> Full documentation with usage examples can be
> found [within the plugin documentation](https://ascopes.github.io/protobuf-maven-plugin),
> and examples are
> present [in the integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it).

## Quick start

### Basic code generation

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
> Any `*.proto` files that are discovered in project dependencies will be made available
> to `protoc`,
> so you can import them in exactly the same way you would with Java classes 

### Other language support

Other language generation targets are also available. This plugin provides support for generating
all
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

### Dependencies

Native Maven dependency management is supported out of the box, allowing you to use Maven as an
artifact registry for bundles of Proto files seamlessly.

```xml

<project>
  ...

  <dependencies>
    <dependency>
      <groupId>org.example.protos</groupId>
      <artifactId>user-protos</artifactId>
      <version>1.2.3</version>
      <type>zip</type>
    </dependency>
  </dependencies>

  <plugins>
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
  </plugins>
</project>
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

    <!-- Vanilla protoc plugins - these are platform specific executables
         just like protoc is. -->
    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>

    <!-- JVM plugins are distributed as JARs rather than native system
         executables. The protobuf-maven-plugin will automatically bootstrap
         them for you, without the need for additional
         tooling for your platform. It should "just work". -->
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

## Stars

[![Star History Chart](https://api.star-history.com/svg?repos=ascopes/protobuf-maven-plugin&type=Timeline)](https://www.star-history.com/#ascopes/protobuf-maven-plugin&Timeline)
