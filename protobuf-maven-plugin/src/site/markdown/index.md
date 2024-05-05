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

# Detailed examples

If you need detailed working examples to use as reference, then the 
[integration tests](https://github.com/ascopes/protobuf-maven-plugin/tree/main/protobuf-maven-plugin/src/it)
are a great place to start, since they are full working Maven projects.

If you wish to contribute additional test cases to show integration with custom plugins or more
complicated use cases, then this is always welcome.

# Usage reference

The following sections document basic usage of this plugin for several of the most popular use cases.

If you need the full reference for a parameters that are allowed, visit the [goals page](plugin-info.html).

## Minimum requirements

You will need to be using Maven 3.8.2 and Java 11 at a minimum.

## Basic configuration

At the core, this plugin is designed to be fairly simple to use, and will
attempt to resolve everything that you need automatically. All you need to
do is provide the version of `protoc` to use.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>${protobuf-maven-plugin.version}</version>

  <configuration>
    <protocVersion>4.26.1</protocVersion>
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

Currently, the newest supported version of `protoc` is
![newest-protoc-version](https://img.shields.io/maven-central/v/com.google.protobuf/protoc?versionPrefix=4).

To generate test sources, use the `generate-test` goal instead.

Inputs will be read from `src/main/protobuf` (or `src/test/protobuf` for tests)
and will be output to `target/generated-sources/protobuf`
(or `target/generated-test-sources/protobuf` for tests). This can be overridden
in the `configuration` block if needed.

The `protoc` version itself can be set via the `<protocVersion>` configuration parameter
as shown above, or can be set via the `protoc.version` property in your POM. It may
optionally be totally overridden on the command line by passing `-Dprotoc.version=xxx`,
in which case the `<protocVersion>` and `protoc.version` will be ignored. This is done
to allow users who may have an incompatible system to be able to request a build using
the `$PATH`-based `protoc` binary on their system (documented later in this page).

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
    <protobuf.version>4.26.1</protobuf.version>
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

### v4.x versus v3.x

The v4.x versions of the protobuf libraries have breaking changes compared to the v3.x
versions. This means that using the `protoc` binary corresponding to v4.x will not be
compatible with libraries using v3.x of the protobuf libraries.

Ensure you are using the correct version for your project and requirements!

## Importing protobuf definitions from other places

By default, this plugin will index all JARs that are dependencies for the current Maven project,
just like you would expect when using Java code. This plugin considers any dependency that is marked with the `compile`,
`provided`, or `system` scope (or additionally `test` if the `generate-test` goal is used).

If there are additional paths on the file system that you wish to add to the import path, then
you can specify these using the `importPaths` parameter. Note that these will not be
compiled, only made visible to the protobuf compiler.

## Generating other language sources

The following languages are available. You can turn any combination on and off. If all
are disabled then at least one plugin must be provided.

<table>
  <thead>
    <tr>
      <th>Language</th>
      <th>Parameter</th>
      <th>Default value</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>C++</td>
      <td><code>cppEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>C#</td>
      <td><code>csharpEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>Java</td>
      <td><code>javaEnabled</code></td>
      <td><code>true</code></td>
      <td/>
    </tr>
    <tr>
      <td>Kotlin</td>
      <td><code>kotlinEnabled</code></td>
      <td><code>false</code></td>
      <td>Generates JVM Kotlin descriptors. You should also ensure <code>javaEnabled</code> is true.</td>
    </tr>
    <tr>
      <td>Objective-C</td>
      <td><code>objcEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>PHP</td>
      <td><code>phpEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>Python</td>
      <td><code>pythonEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>Python typeshed stubs</td>
      <td><code>pyiEnabled</code></td>
      <td><code>false</code></td>
      <td>Enable this alongside <code>pythonEnabled</code> to generate MyPy-compatible typehint stubs.</td>
    </tr>
    <tr>
      <td>Ruby</td>
      <td><code>rubyEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>Rust</td>
      <td><code>rustEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
  </tbody>
</table>

Other languages (such as Scala, Clojure, etc) can be configured by using
third-party plugins (see further down this page).

Note that all generated sources will be written to the same output directory, so you
may want to configure multiple executions for each language to override the individual
output directories.

It is also important to note that you need to provide a valid compiler or tooling to
make use of the generared sources (other than Java). For example, Kotlin generation
would require you to also configure the `kotlin-maven-plugin`.

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

## Customising the source for `protoc`

### Using protoc from your system path

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

### Using protoc from a specific path

You may wish to run `protoc` from a specific path on your file system. If you need to do this,
you can provide a URL with the `file` scheme to reference it:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protocVersion>file:///opt/protoc/protoc.exe</protocVersion>
  </configuration>
</plugin>
```

### Using protoc from a remote server

If you have a `protoc` binary on a remote FTP or HTTP(S) server, you can provide the URL to download
directly:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protocVersion>ftp://company-server.internal/protoc/protoc.exe</protocVersion>
    <!-- or -->
    <protocVersion>http://company-server.internal/protoc/protoc.exe</protocVersion>
    <!-- or -->
    <protocVersion>https://company-server.internal/protoc/protoc.exe</protocVersion>
  </configuration>
</plugin>
```

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.

## Additional plugins

If you wish to generate GRPC stubs, or outputs for other languages like Scala that are not already
covered by the protoc executable, you can add custom plugins to your build.

### Binary plugins

Binary plugins are OS-specific executables that are passed to `protoc` directly, and are the 
standard way of handling plugins with `protoc`.

#### Binary plugins from Maven Central

If the plugin you wish to use is on Maven Central or any other Maven repository, you can reference
that plugin directly via the group ID, artifact ID, and version (like any other Maven artifact).

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>
  </configuration>

  ...
</plugin>
```

Each `binaryMavenPlugin` can take an optional `options` parameter which will
be passed as an option to the plugin if specified.

#### Binary plugins from the system path

If you instead wish to read the executable from the system `$PATH`, then you can specify an
executable name instead:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <binaryPathPlugins>
      <binaryPathPlugin>
        <name>protoc-gen-grpc-java</name>
      </binaryPathPlugin>
    </binaryPathPlugins>
  </configuration>

  ...
</plugin>
```

Each `binarPathPlugin` can take an optional `options` parameter which will
be passed as an option to the plugin if specified.

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved on the system path. This is useful for specific cases where resources may only be available 
during CI builds but do not prevent the application being built locally.

#### Binary plugins from specific locations

In some situations, you may wish to download plugins directly from a URL or run them from a 
specific file system path:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <binaryUrlPlugins>
      <binaryUrlPlugin>
        <url>file:///opt/protoc/protoc-gen-grpc-java</url>
      </binaryUrlPlugin>
      <binaryUrlPlugin>
        <url>ftp://company-server.internal/some-other-plugin.exe
      </binaryUrlPlugin>
      <binaryUrlPlugin>
        <url>https://some-website.net/downloads/my-cool-protoc-plugin.exe</url>
        <options>some-option=some-value</options>
      </binaryUrlPlugin>
    </binaryUrlPlugins>
  </configuration>

  ...
</plugin>
```

Any protocols supported by your JRE should be able to be used here, including:

- `file://`
- `http://`
- `https://`
- `ftp://`
- `jar://`

Each `binaryUrlPlugin` can take an optional `options` parameter which will
be passed as an option to the plugin if specified.

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved. This is useful for specific cases where resources may only be available during CI builds but do not
prevent the application being built locally. If set to optional, then any "not found" response provided by
the underlying URL protocol will be ignored.

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.

### Pure-Java plugins

If a `protoc` plugin is distributed as a platform-independent JAR archive rather than a native
executable, you can instruct this Maven plugin to invoke the artifact as part of compilation. To
do this, simply specify the `jvmMavenPlugins` configuration property, passing in a list of
dependencies to execute.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <jvmMavenPlugins>
      <jvmMavenPlugin>
        <!-- Use the JAR that Salesforce distributes -->
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </jvmMavenPlugin>
    </jvmMavenPlugins>
  </configuration>

  ...
</plugin>
```

Each `jvmMavenPlugin` can take an optional `options` parameter which will
be passed as an option to the plugin if specified.

Currently, you are required to be able to execute `*.bat` files on Windows, or have 
`sh` available on the system `$PATH` for any other platform.

### Mixing plugins

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
    <binaryMavenPlugins>
      <binaryMavenPlugin>
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </binaryMavenPlugin>
      <binaryMavenPlugin>
        <!-- Use the native *.exe that Salesforce distributes -->
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </binaryMavenPlugin>
    </binaryMavenPlugins>
  </configuration>

  ...
</plugin>
```

### Inheriting plugins in multi-module projects

A lesser-known feature of Maven is that you can control how child projects override plugin configurations
that are provided in the parent POM.

If you want to force children to always use a base set of `protoc` plugins, whilst being able to add
additional plugins per project, you can change the overriding mechanism directly.

To do this, configure the plugin like so in your parent.

```xml
<configuration>
  ...
  <binaryMavenPlugins combine.children="append">
    ...
  </binaryMavenPlugins>
</configuration>
```

Note that this is just an example. The `combine.children` attribute can be used anywhere and is documented
in the [Apache Maven POM Reference](https://maven.apache.org/pom.html#Build).
