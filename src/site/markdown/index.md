Introduction
============

The Protobuf Maven Plugin is a modern Maven plugin that attempts to reduce the hassle needed to
integrate Protobuf compilation into your build process.

Unlike existing Protobuf integrations, this plugin is able to pull the desired version of
`protoc` directly from Google's releases on Maven Central. This means you do not need to update 
the version of this plugin to be able to pull in a newer version of `protoc` when it is released.

If your system is not directly supported by Google in the `protoc` releases they supply, you can
instruct the plugin to instead invoke `protoc` from your system path directly.

In addition to generating Java sources, this plugin supports generating Kotlin sources as well!

Bugs and feature requests
=========================

Please raise any bugs or feature requests on 
[the GitHub project for this plugin](https://github.com/ascopes/protobuf-maven-plugin/issues).

Usage
=====

Detailed usage can be found on the [plugin info (goals) page](plugin-info.html).

Basic Configuration
-------------------

A simple project that makes use of this plugin to generate Java sources would place their
`*.proto` protobuf sources in `src/main/protobuf`, and use the following structure:

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
              <goal>generate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

This will output generated sources in `target/generated-sources/protobuf` by default.

Test sources can be generated with the `generate-test` goal. Test sources will be output to
the `target/generated-test-sources/protobuf` directory, and will be read from
`src/test/protobuf` by default.

Kotlin
------

Protoc support for Kotlin currently takes the shape of producing additional Kotlin API wrapper
calls that can decorate the existing generated Java code.

To opt in to also generating these sources, set the `generateKotlinWrappers` plugin property to
`true`:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>
  
  <configuration>
    <generateKotlinWrappers>true</generateKotlinWrappers>
    ...
  </configuration>
    
  ...
</plugin>
```

Sources will be emitted in the same location as the Java sources.

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

Lightweight 'lite' sources
--------------------------

If you are in a situation where you need lightweight and fast protobuf generated sources, you
can opt in to generating "lite" sources only. These will omit all the metadata usually included
within generated protobuf sources, at the cost of flexibility.

Refer to the protobuf documentation for more details on the pros and cons of doing this.

To enable this in the plugin, set the `liteOnly` parameter to `true`. By default this is disabled
as you usually do not need to worry about this.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <liteOnly>true</liteOnly>
    ...
  </configuration>

  ...
</plugin>
```

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
    <version>PATH</version>
  </configuration>

  ...
</plugin>
```
