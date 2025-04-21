![Java 11+](https://img.shields.io/badge/Java-11+-red?logo=openjdk&logoColor=white)
![Maven 3.8](https://img.shields.io/badge/maven-3.8,%204.x-blue?logo=apache-maven)
![GitHub License](https://img.shields.io/github/license/ascopes/protobuf-maven-plugin)
[![Build Status](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ascopes/protobuf-maven-plugin/actions/workflows/build.yml)
[![Coverage](https://img.shields.io/codecov/c/github/ascopes/protobuf-maven-plugin/main)](https://codecov.io/gh/ascopes/protobuf-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ascopes/protobuf-maven-plugin)](https://central.sonatype.com/artifact/io.github.ascopes/protobuf-maven-plugin)
[![Documentation](https://img.shields.io/badge/-documentation-purple?logo=apache-maven)](https://ascopes.github.io/protobuf-maven-plugin)
![GitHub Release Date](https://img.shields.io/github/release-date/ascopes/protobuf-maven-plugin)

![logo](protobuf-maven-plugin/src/site/resources/images/banner.jpg)

# protobuf-maven-plugin

A scratch-built, modern Maven plugin for seamless protoc integration. Provides support for native
and JVM-based protoc plugins, as well as automatic dependency resolution and incremental code
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
         them for you. -->
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

---

## Feature matrix and project roadmap

- :white_check_mark: - actively supported/available
- :thought_balloon: - not supported, but open for discussion
- :x: - not supported, nor planned

### Supported platform versions

| Feature           | Support                                     |
|:------------------|:--------------------------------------------|
| Maven 3.8 Support | :white_check_mark:                          |
| Maven 3.9 Support | :white_check_mark:                          |
| Maven 4.x Support | :white_check_mark:                          |
| Java 11 Support   | :white_check_mark:                          |
| Java 17 Support   | :white_check_mark:                          |
| Java 21 Support   | :white_check_mark:                          |
| Java 24 Support   | :white_check_mark:                          |
| Linux x86_64      | :white_check_mark:                          |
| Linux ARM64       | :white_check_mark:                          |
| macOS x86_64      | :white_check_mark:                          |
| macOS ARM64       | :white_check_mark:                          |
| Windows x86_64    | :white_check_mark:                          |
| Android Termux    | :white_check_mark:\*                        |

> [!NOTE]
> Maven integration is tested with the newest bugfix of each minor version that is listed (e.g. 3.9.9).

> [!TIP]
> \* Only supports using `protoc` from the system `$PATH` at this time.

### Languages and frameworks

| Feature                        | Support                                                                                     |
|:-------------------------------|:--------------------------------------------------------------------------------------------|
| Java                           | :white_check_mark:                                                                          |
| Kotlin                         | :white_check_mark: (JVM shims out of the box, multiplatform via third-party protoc plugins) |
| C#                             | :white_check_mark:                                                                          |
| C++                            | :white_check_mark:                                                                          |
| Objective C                    | :white_check_mark:                                                                          |
| PHP                            | :white_check_mark:                                                                          |
| Ruby                           | :white_check_mark:                                                                          |
| Rust                           | :white_check_mark:                                                                          |
| Python and PYI stubs           | :white_check_mark:                                                                          |
| Other languages and frameworks | :white_check_mark: (via third-party protoc plugins)                                         |

### Protoc support

| Feature                 | Support                                                       |
|:------------------------|:--------------------------------------------------------------|
| From Maven repositories | :white_check_mark: (any Google-released version)              |
| From system path        | :white_check_mark: (as long as it is installed first)         |
| From a URL              | :white_check_mark: (no SOCKS/HTTP proxy support at this time) |

### Protoc features

| Feature                                                 | Support                                              | 
|:--------------------------------------------------------|:-----------------------------------------------------|
| Proto2                                                  | :white_check_mark:                                   |
| Proto3                                                  | :white_check_mark:                                   |
| Editions                                                | :white_check_mark:                                   |
| Failing on warnings                                     | :white_check_mark: (opt-in via plugin configuration) |
| Lite builds                                             | :white_check_mark: (opt-in via plugin configuration) |                 
| Generation of binary descriptors                        | :white_check_mark: (opt-in via plugin configuration) |
| Controlling import inclusion in binary descriptors      | :white_check_mark: (opt-in via plugin configuration) |
| Controlling source info inclusion in binary descriptors | :white_check_mark: (opt-in via plugin configuration) |
| Building from FileDescriptorSet descriptors             | :white_check_mark:                                   |

### Maven integrations

| Feature                                                          | Support                     | 
|:-----------------------------------------------------------------|:----------------------------|
| Incremental compilation                                          | :white_check_mark:          |
| Attaching protobuf sources to generated JARs                     | :white_check_mark:          |
| Attaching generated binary descriptors to builds                 | :white_check_mark:          |
| Detecting importable dependencies from project `<dependencies/>` | :white_check_mark:          |
| Detecting importable dependencies from provided filesystem paths | :white_check_mark:          |
| Respecting project `<dependencyManagement/>`                     | :white_check_mark:          |
| Compiling `proto` files from dependencies                        | :white_check_mark: (opt-in) |
| Controlling binary descriptor type                               | :white_check_mark:          |
| Controlling binary descriptor classifier                         | :white_check_mark:          |
| Controlling binary descriptor attachment                         | :white_check_mark:          |
| Generation of main sources                                       | :white_check_mark:          | 
| Generation of test sources                                       | :white_check_mark:          | 
| Controlling the dependencies that are resolved by Maven scope    | :white_check_mark:          |
| Marking generates sources as compilation candidates              | :white_check_mark:          |
| Creating protobuf ZIP archives\*                                 | :thought_balloon:           |
| Creating protobuf TAR archives\*                                 | :thought_balloon:           |
| Generating JAR-based `protoc` plugins\*\*                        | :thought_balloon:           |
| Consuming TAR archives for imports and sources                   | :thought_balloon:           |

> [!TIP]
> \* This can currently be performed using `maven-assembly-plugin`.
>
> \*\* Set the `Main-Class` attribute in the MANIFEST.MF to do this. Use `maven-shade-plugin` if you need a fat JAR
> (not required for use with `protobuf-maven-plugin`, but may be useful for other use
> cases with third party tooling and build systems).

### Protoc plugin integrations

| Feature                                 | Support                                                                                                      | 
|:----------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| Calling binary protoc plugins           | :white_check_mark: (from Maven repositories, URLs, system path)                                              |
| Calling JVM-based protoc plugins        | :white_check_mark: (from Maven repositories, URLs, system path -- generates the shims automatically for you) |
| Calling plugins distributed in tarballs | :thought_balloon:

### Additional dependency management integrations

| Feature                                                                   | Support                                                                   | 
|:--------------------------------------------------------------------------|:--------------------------------------------------------------------------|
| Importing paths without adding to the Maven project                       | :white_check_mark: (via plugin configuration)                             |
| Generating code from additional paths without adding to the Maven project | :white_check_mark: (via plugin configuration)                             |
| Controlling includes and excludes by path                                 | :white_check_mark: (via Glob expressions in plugin configuration)         |
| Failing on invalid dependencies                                           | :white_check_mark: (opt-in via plugin configuration)                      |
| Failing on missing sources                                                | :white_check_mark: (opt-out via plugin configuration)                     |
| Controlling whether transitive dependencies are included                  | :white_check_mark: (globally, or per dependency via plugin configuration) |
