# Examples

<div id="pmp-toc"></div>

Examples of how to use the protobuf-maven-plugin for different use cases and integrations.

Note that any third party libraries, `protoc` plugins, or Maven plugins are not associated with
the protobuf-maven-plugin project. If you spot any bugs or have any queries, please contact the
authors of that tooling. Likewise, these examples are best effort only. While effort has been made
to ensure they work, updates in third party components may result in issues. If you spot such
an issue, please raise a GitHub Issue or Pull Request to adjust these examples as appropriate!

## gRPC with Java

For generating gRPC stubs for Java, you can use the official gRPC Java plugin:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protoc>${protobuf.version}</protoc>
    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>
  </configuration>
</plugin>
```

Note that for gRPC versions prior to 1.74.0, you should also pass the following options
to avoid dependencies on `javax.annotation-api`.

```xml
<options>@generated=omit</options>
```

## gRPC and Reactor

Salesforce maintains a library to allow you to generate gRPC service stubs that use reactive
streams via the Reactor framework.

To configure protobuf-maven-plugin to use this `protoc` plugin, use the following configuration:

```xml

<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protoc>${protobuf.version}</protoc>

    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>

    <jvmMavenPlugins>
      <jvmMavenPlugin>
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </jvmMavenPlugin>
    </jvmMavenPlugins>
  </configuration>
</plugin>
```

## gRPC and Vert.x

For integrating with Vert.x, you can use the official Vert.x gRPC plugin:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <protoc>${protobuf.version}</protoc>
    <jvmMavenPlugins>
      <jvmMavenPlugin>
        <groupId>io.vertx</groupId>
        <artifactId>vertx-grpc-protoc-plugin2</artifactId>
        <version>${vertx.version}</version>
        <mainClass>io.vertx.grpc.plugin.VertxGrpcGenerator</mainClass>
        <jvmArgs>
          <jvmArg>--grpc-client</jvmArg>
          <jvmArg>--grpc-service</jvmArg>
          <jvmArg>--service-prefix=Vertx</jvmArg>
        </jvmArgs>
      </jvmMavenPlugin>
    </jvmMavenPlugins>
  </configuration>
</plugin>
```

For more information, see the [Vert.x gRPC plugin documentation](https://vertx.io/docs/vertx-grpc/java/#vertx-grpc-protoc-plugin).

## Scala

Scala support for Protobuf and gRPC can be enabled using the ScalaPB Protoc plugin.

This is distributed as an OS-dependent binary within a ZIP archive on their GitHub releases.

A suitable Maven plugin for providing Scala compilation is also required.

```xml
<plugins>
  <plugin>
    <groupId>io.github.ascopes</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>

    <configuration>
      <javaEnabled>false</javaEnabled>

      <binaryUrlPlugins>
        <binaryUrlPlugin>
          <url>zip:https://github.com/scalapb/ScalaPB/releases/download/v${scalapb.version}/protoc-gen-scala-${scalapb.version}-linux-x86_64.zip!/protoc-gen-scala</url>
          <options>flat_package,grpc,scala3_sources</options>
        </binaryUrlPlugin>
      </binaryUrlPlugins>
    </configuration>

    <executions>
      <execution>
        <goals>
          <goal>generate</goal>
        </goals>
      </execution>
    </executions>
  </plugin>


  <plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>

    <configuration>
      <failOnMultipleScalaVersions>true</failOnMultipleScalaVersions>
    </configuration>
  </plugin>
</plugins>
```

An example service handler may look like the following:

`src/main/protobuf/helloworld.proto`:
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "org.example.helloworld";

package org.example.helloworld;

message GreetingRequest {
  string name = 1;
}

message GreetingResponse {
  string text = 1;
}

service GreetingService {
  rpc Greet(GreetingRequest) returns (GreetingResponse);
}
```

`src/main/scala/org/example/helloworld/HelloWorld.scala`:
```scala
package org.example.helloworld

import scala.concurrent.Future

class GreetingServiceImpl extends GreetingServiceGrpc.GreetingService:
  override def greet(request: GreetingRequest) =
    val response = GreetingResponse(text = "Hello, " + request.name + "!")
    Future.successful(response)
end GreetingServiceImpl
```


## JavaScript and gRPC-Web

While JavaScript generation is not natively supported by this plugin (as it's not part of core protoc), you can easily integrate JavaScript and gRPC-Web code generation using binary URL plugins.

### Complete Example with Platform-Specific Profiles

This example shows how to generate JavaScript/TypeScript client code for a gRPC service:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.ascopes</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>

      <configuration>
        <protoc>${protobuf.version}</protoc>
        <javaEnabled>false</javaEnabled>

        <binaryUrlPlugins>
          <binaryUrlPlugin>
            <!-- Generate JavaScript protobuf messages -->
            <url>${protoc.gen.js.url}</url>
            <options>import_style=commonjs</options>
            <outputDirectory>${project.basedir}/target/js/protobuf</outputDirectory>
          </binaryUrlPlugin>
          <binaryUrlPlugin>
            <!-- Generate JavaScript gRPC stubs -->
            <url>${protoc.gen.grpc.web.url}</url>
            <options>import_style=typescript,mode=grpcwebtext</options>
            <outputDirectory>${project.basedir}/target/js/grpc</outputDirectory>
          </binaryUrlPlugin>
        </binaryUrlPlugins>
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
</build>

<!-- Platform-specific profiles for JavaScript/gRPC-Web plugins -->
<profiles>
  <profile>
    <id>windows</id>
    <activation>
      <os>
        <family>windows</family>
      </os>
    </activation>
    <properties>
      <protoc.gen.js.url>zip:https://github.com/protocolbuffers/protobuf-javascript/releases/download/v3.21.4/protobuf-javascript-3.21.4-win64.zip!/protobuf-javascript-3.21.4-win64/bin/protoc-gen-js.exe</protoc.gen.js.url>
      <protoc.gen.grpc.web.url>https://github.com/grpc/grpc-web/releases/download/1.5.0/protoc-gen-grpc-web-1.5.0-windows-x86_64.exe</protoc.gen.grpc.web.url>
    </properties>
  </profile>
  
  <profile>
    <id>unix</id>
    <activation>
      <os>
        <family>unix</family>
      </os>
    </activation>
    <properties>
      <protoc.gen.js.url>zip:https://github.com/protocolbuffers/protobuf-javascript/releases/download/v3.21.4/protobuf-javascript-3.21.4-linux-x86_64.zip!/bin/protoc-gen-js</protoc.gen.js.url>
      <protoc.gen.grpc.web.url>https://github.com/grpc/grpc-web/releases/download/1.5.0/protoc-gen-grpc-web-1.5.0-linux-x86_64</protoc.gen.grpc.web.url>
    </properties>
  </profile>
  
  <profile>
    <id>mac</id>
    <activation>
      <os>
        <family>mac</family>
      </os>
    </activation>
    <properties>
      <protoc.gen.js.url>zip:https://github.com/protocolbuffers/protobuf-javascript/releases/download/v3.21.4/protobuf-javascript-3.21.4-osx-x86_64.zip!/bin/protoc-gen-js</protoc.gen.js.url>
      <protoc.gen.grpc.web.url>https://github.com/grpc/grpc-web/releases/download/1.5.0/protoc-gen-grpc-web-1.5.0-darwin-x86_64</protoc.gen.grpc.web.url>
    </properties>
  </profile>
</profiles>
```

You should edit the URLs to have the correct version of the plugin for your platform.

### Options for JavaScript Generation

The `protoc-gen-js` plugin supports several options:

- `import_style=commonjs` - Use CommonJS imports (default)
- `import_style=commonjs+dts` - CommonJS with TypeScript definitions
- `import_style=closure` - Use goog.require() style imports
- `binary` - Include binary serialization/deserialization support

For more information, see the [JavaScript plugin documentation](https://github.com/protocolbuffers/protobuf-javascript).

### Options for gRPC-Web Generation

The `protoc-gen-grpc-web` plugin supports:

- `import_style=typescript` - Generate TypeScript code
- `import_style=commonjs` - Generate CommonJS JavaScript
- `import_style=commonjs+dts` - CommonJS with TypeScript definitions
- `mode=grpcwebtext` - Use grpc-web-text format (default)
- `mode=grpcweb` - Use binary gRPC-Web format

For more information, see the [gRPC-Web plugin documentation](https://github.com/grpc/grpc-web).

## Multiple Output Directories

If you need to generate code to different directories based on the target language:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  
  <executions>
    <!-- Java sources -->
    <execution>
      <id>java</id>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <protoc>${protobuf.version}</protoc>
        <outputDirectory>${project.basedir}/target/java</outputDirectory>
      </configuration>
    </execution>
    
    <!-- Python sources -->
    <execution>
      <id>python</id>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <javaEnabled>false</javaEnabled>
        <outputDirectory>${project.basedir}/target/python</outputDirectory>
        <protoc>${protobuf.version}</protoc>
        <pythonEnabled>true</pythonEnabled>
      </configuration>
    </execution>
  </executions>
</plugin>
```
