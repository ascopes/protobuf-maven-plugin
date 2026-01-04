<h1 align="center">Protobuf Maven Plugin</h1>

<img align="center" alt="logo" src="protobuf-maven-plugin/src/site/resources/images/banner.jpg">

<p align="center">
  <!-- Note: do not put inner tags on newlines within <a/>, it messes up the rendering of text decorations leaving blue underlines between badges. -->
  <img alt="Java 17+" src="https://img.shields.io/badge/Java-17+-red?logo=openjdk&logoColor=white">
  <img alt="Maven 3.9" src="https://img.shields.io/badge/maven-3.9,%204.x-blue?logo=apache-maven">
  <img alt="GitHub License" src="https://img.shields.io/github/license/ascopes/protobuf-maven-plugin">
  <a href="https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml"><img alt="Build Status" src="https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main"></a>
  <a href="https://codecov.io/gh/ascopes/protobuf-maven-plugin"><img alt="Coverage" src="https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main"></a>
  <a href="https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin"><img alt="Maven Central" src="https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin.svg?label=Maven%20Central"></a>
  <a href="https://ascopes.github.io/protobuf-maven-plugin"><img alt="Documentation" src="https://img.shields.io/badge/documentation-latest-purple?logo=apache-maven"></a>
  <img alt="GitHub Release Date" src="https://img.shields.io/github/release-date/ascopes/protobuf-maven-plugin">
</p>

A scratch-built, modern Maven plugin for seamless `protoc` integration. Features include:

- Running `protoc` from Maven Central, the system path, or URLs
- Running `protoc` plugins from binaries on the system path, Maven Central, or from various URLs
    - This includes when packaged within tarballs or ZIP files!
- Running `protoc` plugins that are packaged as regular JARs
- Building from descriptor files
- Building from dependencies
- Generating descriptor files
- Discovering `*.proto` sources from your project dependencies
- Filtering sources based upon globs
- Injecting custom arguments and environment variables into `protoc`
- Incremental analysis to avoid rebuilding large projects when no changes have been made
- Digest verification of `protoc` and any plugins
- Running any executables from an optional sanctioned path, for corporate users in tightly-locked-down build environments
- Ability to dynamically skip plugins if not resolved on certain platforms (e.g. if working with plugins that are only packaged for Linux)

Full documentation with usage examples can be found [within the plugin documentation](https://ascopes.github.io/protobuf-maven-plugin),
and examples are present [in the integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it).

> [!NOTE]
> The v4 release of this plugin requires a minimum of Java 17 and Maven 3.9 to operate. If you
> require the ability to use Maven 3.8 or Java 11, please use the latest v3 release from Maven Central!

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
    <protoc>${protobuf-java.version}</protoc>
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
> so you can import them in exactly the same way you would with Java classes!

### Other language support

Other language generation targets are also available. This plugin provides support for generating
all
the languages that protoc supports out of the box, including Kotlin (JVM), Python, Python typeshed
stubs, and Ruby.

The following will generate Java classes and corresponding Kotlin wrappers:

```xml

<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <kotlinEnabled>true</kotlinEnabled>
    <protoc>${protobuf-java.version}</protoc>
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
        <protoc>${protobuf-java.version}</protoc>
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

<plugins>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protoc>${protobuf-java.version}</protoc>
    
    <plugins>
      <!-- Vanilla protoc plugins - these are platform specific executables
           just like protoc is. -->
      <plugin kind="binary-maven">
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </plugin>

      <!-- JVM plugins are distributed as JARs rather than native system
           executables. The protobuf-maven-plugin will automatically bootstrap
           them for you, without the need for additional
           tooling for your platform. It should "just work". -->
      <plugin kind="jvm-maven">
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </plugin>
    </plugins>
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
