# Basic Usage

<div id="pmp-toc"></div>

This plugin is designed with simplicity in mind. As a result, you do not need
to do much to get this working.

## A very simple example

The following shows a basic `pom.xml` that will configure and run this plugin
as a part of the build. Replace `%VERSION%` with the version number in the
header of this page.

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  
  <groupId>org.example</groupId>
  <artifactId>hello-world</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <protobuf.version>4.28.0</protobuf.version>
    
    <maven.compiler.release>22</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.google.protobuf</groupId>
      <artifactId>protobuf-java</artifactId>
      <version>${protobuf.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>io.github.ascopes</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>%VERSION%</version>

        <configuration>
          <protocVersion>${protobuf.version}</protocVersion>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

You can place protobuf sources in `src/main/protobuf`. As an example, the following
could be created in `src/main/protobuf/org/example/helloworld.proto`:

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "org.example";

package org.example;

message Greeting {
  string greeting = 1;
}
```

Finally, you can create a Java class to verify this works. In this example, we
create `src/main/java/org/example/Main.java`:

```java
package org.example;

public class Main {
  public static void main(String[] args) {
    var greeting = Greeting.newBuilder()
        .setGreeting("Hello, World!")
        .build();
    System.out.println(greeting);
  }
}
```

When you run `mvn package`, this plugin will first download the required version
of the Protobuf compiler into your local Maven repository, and then
will use this to compile your `*.proto` sources to Java classes. These will
be output in `target/generated-sources/protobuf`. Maven will then run the
`compile` phase which will compile both your handwritten classes and the
generated protobuf classes.

---

## Migration

[GH-692](https://github.com/ascopes/protobuf-maven-plugin/issues/692) introduced the
fallback source directory of `src/main/proto` and `src/test/proto` to be used to
assist users who are migrating their codebases off of other unmaintained Maven plugins.

It is recommended that users default to making use of `src/main/protobuf` and 
`src/test/protobuf` instead, as this follows the standard naming convention of other 
Maven-managed projects which is to use the language name rather than the file extension.

---

## Goals

By default, this plugin runs the `generate` goal to generate source code designed
to be packaged with your final application. The generated Java sources will be
added to the Maven project's `main` source directory list to do this.

If you instead only wish to compile sources for use within tests, you can
invoke the `generate-test` goal instead. This will instead default to reading
Protobuf files from `src/test/protobuf`, and output generated code to
`target/generated-test-sources/protobuf`.

By default, `generate-test` does not run.

The plugin configuration for this would look like the following:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    <protocVersion>${protobuf.version}</protocVersion>
  </configuration>

  <executions>
    <execution>
      <goals>
        <goal>generate-test</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Of course, there is nothing stopping you running both together to generate
Java sources for both the final application and for testing. In this example
any main sources would be in `src/main/protobuf` and any test-only sources
would be in `src/test/protobuf`:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    <protocVersion>${protobuf.version}</protocVersion>
  </configuration>

  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
        <goal>generate-test</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

---

## Changing source paths

If you need to use a different location for your sources, you can override
it in the plugin configuration globally:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    <protocVersion>${protobuf.version}</protocVersion>

    <sourceDirectories>
      <sourceDirectory>path/to/directory</sourceDirectory>
    </sourceDirectories>
  </configuration>
</plugin>
```

Note that you can provide as many paths as you wish. If none are provided, then
the default paths are used.

If you are using both the `generate` and `generate-test` goals, you can
configure their paths individually.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    <protocVersion>${protobuf.version}</protocVersion>
  </configuration>

  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <sourceDirectories>
          <sourceDirectory>path/to/main/directory</sourceDirectory>
        </sourceDirectories>  
      </configuration>
    </execution>

    <execution>
      <goals>
        <goal>generate-test</goal>
      </goals>
      <configuration>
        <sourceDirectories>
          <sourceDirectory>path/to/test/directory</sourceDirectory>
        </sourceDirectories>  
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

For more complex configuration, visit the Goals page or the other guide pages.
