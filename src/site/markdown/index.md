# Introduction

The Protobuf Maven Plugin is a modern Maven plugin that attempts to reduce the hassle needed to
integrate Protobuf compilation into your build process.

Unlike existing Protobuf integrations, this plugin is able to pull the desired version of
`protoc` directly from Google's releases on Maven Central. This means you do not need to update 
the version of this plugin to be able to pull in a newer version of `protoc` when it is released.

If your system is not directly supported by Google in the `protoc` releases they supply, you can
instruct the plugin to instead invoke `protoc` from your system path directly.

For users who are writing GRPC services, this plugin can also support generating the GRPC stubs
for you automatically.

In addition to generating Java sources, this plugin can also generate Kotlin sources.

# Bugs and feature requests

Please raise any bugs or feature requests on 
[the GitHub project for this plugin](https://github.com/ascopes/protobuf-maven-plugin/issues).

# Usage

Detailed usage can be found on the [plugin info (goals) page](plugin-info.html).

## The most basic configuration

At the core, this plugin is designed to be fairly simple to use, and will
attempt to resolve everything that you need automatically. All you need to
do is provide the version of `protoc` to use.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>3.25.0</protocVersion>
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

To generate test sources, use the `generate-test` goal instead.

Inputs will be read from `src/main/protobuf` (or `src/test/protobuf` for tests)
and will be output to `target/generated-sources/protobuf`
(or `target/generated-test-sources/protobuf` for tests). This can be overridden
in the `configuration` block if needed.

## Dependencies

It is worth noting that you will need to include the `protobuf-java` dependency
for the generated Java code to actually compile. 

Ideally, you should use the same version for `protobuf-java` as you do for the 
`protocVersion` parameter. Doing this with a shared property will also allow tools
like Dependabot to keep the compiler version up-to-date automatically.

```xml
<project>

  ...

  <properties>
    <protobuf.version>3.25.0</protobuf.version>
    <protobuf-maven-plugin.version>...</protobuf-maven-plugin.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.google.protobuf</groupId>
      <artifactId>protobuf-java</artifactId>
      <version>${protobuf.version}</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>io.github.ascopes</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>${protobuf-maven-plugin.version}</version>

        <configuration>
          <protocVersion>${protobuf.version}</protocVersion>
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
</project>
```

If you are using other types of output, you'll need different dependencies.
The following table documents the most common ones that you'll run across.

Dependencies are listed as `groupId:artifactId` for brevity. Naming is not
100% consistent, so be sure to use exactly what is written below.

<table>
  <thead>
    <tr>
      <th>Configuration type</th>
      <th>Dependencies</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Java protobuf messages</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-java</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>Java "lite" protobuf messages</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-javalite</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>Kotlin protobuf messages</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-kotlin</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>Kotlin "lite" protobuf messages</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-kotlin-lite</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>Java GRPC services</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-java</code></li>
          <li><code>io.grpc:grpc-protobuf</code></li>
          <li><code>io.grpc:grpc-stub</code></li>
          <li><code>javax.annotation:javax.annotation-api</code></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td>Kotlin GRPC services</td>
      <td>
        <ul>
          <li><code>com.google.protobuf:protobuf-kotlin</code></li>
          <li><code>io.grpc:grpc-protobuf-kotlin</code></li>
          <li><code>io.grpc:grpc-stub</code></li>
          <li><code>javax.annotation:javax.annotation-api</code></li>
        </ul>
      </td>
    </tr>
  </tbody>
</table>

## Importing protobuf definitions from other places

By default, this plugin will index all JARs that are dependencies for the current Maven project,
just like you would expect when using Java code. This plugin considers any dependency that is marked with the `compile`,
`provided`, or `system` scope (or additionally `test` if the `generate-test` goal is used).

If there are additional paths on the file system that you wish to add to the import path, then
you can specify these using the `additionalImportPaths` parameter. Note that these will not be
compiled, only made visible to the protobuf compiler.

## Kotlin generation

Protoc support for Kotlin currently takes the shape of producing additional Kotlin API wrapper
calls that can decorate the existing generated Java code.

To opt in to also generating these sources, set the `kotlinEnabled` plugin property to
`true`:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>
  
  <configuration>
    <kotlinEnabled>true</kotlinEnabled>
    ...
  </configuration>
    
  ...
</plugin>
```

Sources will be emitted in the same location as the Java sources.

## Changing the input directories

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
    ...
  </configuration>
  
  ...
</plugin>
```

Multiple source directories can be specified if required.

## Generating lightweight sources

If you are in a situation where you need lightweight and fast protobuf generated sources, you
can opt in to generating "lite" sources only. These will omit all the metadata usually included
within generated protobuf sources, at the cost of flexibility.

Refer to the protobuf documentation for more details on the pros and cons of doing this.

To enable this in the plugin, set the `liteOnly` parameter to `true`. By default, this is disabled
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

## Using protoc from your system path

If you need to use the version of `protoc` that is installed on your system, specify the version
as `PATH`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protocVersion>PATH</protocVersion>
  </configuration>

  ...
</plugin>
```

## Additional plugins

If you wish to generate GRPC stubs, or outputs for other languages like Scala that are not already
covered by the protoc executable, you can add custom plugins to your build.

If the plugin you wish to use is on Maven Central or any other Maven repository, you can reference
that plugin directly via the group ID, artifact ID, and version.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <additionalPlugins>
      <additionalPlugin>
        <artifact>
          <groupId>io.grpc</groupId>
          <artifactId>protoc-gen-grpc-java</artifactId>
          <version>${grpc.version}</version>
        </artifact>
      </additionalPlugin>
    </additionalPlugins>
  </configuration>

  ...
</plugin>
```

If you instead wish to read the executable from the system `$PATH`, then you can specify an
executable name instead:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <additionalPlugins>
      <additionalPlugin>
        <executableName>protoc-gen-grpc-java</executableName>
      </additionalPlugin>
    </additionalPlugins>
  </configuration>

  ...
</plugin>
```

Multiple plugins can be provided if needed. For example, if you are using the 
[Salesforce Reactor GRPC libraries](https://github.com/salesforce/reactive-grpc/tree/master),
then you can provide the following:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <additionalPlugins>
      <additionalPlugin>
        <artifact>
          <groupId>io.grpc</groupId>
          <artifactId>protoc-gen-grpc-java</artifactId>
          <version>${grpc.version}</version>
        </artifact>
      </additionalPlugin>
      <additionalPlugin>
        <artifact>
          <groupId>com.salesforce.servicelibs</groupId>
          <artifactId>reactor-grpc</artifactId>
          <version>${reactor-grpc.version}</version>
        </artifact>
      </additionalPlugin>
    </additionalPlugins>
  </configuration>

  ...
</plugin>
```

## Further examples

All [integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/src/it) in this 
plugin take the shape of a working Maven project, so feel free to check them out for more examples.

If you wish to contribute additional test cases to show integration with custom plugins or more
complicated use cases, then this is always welcome.
