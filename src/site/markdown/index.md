Introduction
============

The Protobuf Maven Plugin is a modern Maven plugin that attempts to reduce the hassle needed to
integrate Protobuf compilation into your build process.

Unlike existing Protobuf plugins, this plugin is able to pull the desired version of
`protoc` directly from Maven Central without needing a new release every time Google create a new
version. This means you can keep the version of `protoc` up-to-date with the version of
`protobuf-java` without needing to update this plugin as well.

If your system is not directly supported by Google in the `protoc` releases they supply, you can
instruct the plugin to instead invoke `protoc` from your system path directly.

In addition to generating Java sources, this plugin supports generating Kotlin sources as well.

Bugs and feature requests
=========================

Please raise any bugs or feature requests on 
[the GitHub project for this plugin](https://ascopes.github.io/protobuf-maven-plugin).

Usage
=====

Detailed usage can be found on the [plugin info (goals) page](plugin-info.html).

Basic Configuration
-------------------

A simple project that makes use of this plugin to generate Java sources would place their
`protobuf` sources in `src/main/protobuf`, and use the following structure:

```xml
<project>
  ...

  <properties>
    <protobuf.version>3.25.0</protobuf.version>
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
        <version>...</version>

        <configuration>
          <version>${protobuf.version}</version>
        </configuration>

        <executions>
          <execution>
            <phase>generate-sources</phase>
            <goals>
              <goal>generate-java</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

To generate Kotlin sources, use the `generate-kotlin` goal instead of the `generate-java` goal:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>
  
  <configuration>
    <version>${protobuf.version}</version>
  </configuration>
  
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>generate-kotlin</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

Overriding the input directories
--------------------------------

If you do not want to use the default directory for your sources, you can override it in the
plugin configuration:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>
  
  <configuration>
    <sourceDirectories>
      <sourceDirectory>path/to/your/directory</sourceDirectory>
    </sourceDirectories>
    <version>${protobuf.version}</version>
  </configuration>
  
  ...
</plugin>
```

Multiple source directories can be specified if required.

Using `protoc` from the system path
-----------------------------------

If you need to use the version of `protoc` that is installed on your system, specify the version
as `PATH`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <version>${protobuf.version}</version>
  </configuration>

  ...
</plugin>
```
