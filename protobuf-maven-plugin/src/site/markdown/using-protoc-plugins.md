# Using `protoc` Plugins

<div id="pmp-toc"></div>

If you wish to generate gRPC stubs, or outputs for other languages like Scala that are not already
covered by the protoc executable, you can add custom plugins to your build.

---

## Variants

There are a few different kinds of plugin, so you will need to instruct `protobuf-maven-plugin`
on what type of plugin you are describing. This is achieved by specifying the `kind` attribute
on each plugin like so:

```xml
<plugins>
  <plugin kind="binary-maven">
    ...
  </plugin>
  <plugin kind="jvm-maven">
    ...
  </plugin>
  <plugin kind="path">
    ...
  </plugin>
  <plugin kind="url">
    ...
  </plugin>
</plugins>
```

---

## Binary plugins

Binary plugins are OS-specific executables that are passed to `protoc` directly, and are the 
standard way of handling plugins with `protoc`.

### Binary plugins from Maven Central

If the plugin you wish to use is on Maven Central or any other Maven repository, you can reference
that plugin directly via the group ID, artifact ID, and version (like any other Maven artifact).

Binary Maven plugins should have the `kind` attribute set to `binary-maven`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <plugins>
      <plugin kind="binary-maven">
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
      </plugin>
    </plugins>
  </configuration>

  ...
</plugin>
```

| Property                    | Type      | Default           | Description             |
|:----------------------------|:----------|:------------------|:------------------------|
| `groupId`                   | `String`  |                   | The group ID to use.    |
| `artifactId`                | `String`  |                   | The artifact ID to use. |
| `version`                   | `String`  |                   | The version to use.     |
| `classifier`                | `String`  | platform-specific | The classifier to use. Defaults to an OS and CPU-specific string matching the conventions used by `protoc` |
| `type`                      | `String`  | `exe`             | The artifact type.      |
| `options`                   | `String`  | unspecified       | Options to pass to the plugin via `protoc`'s options API. |
| `order`                     | `int`     | `0`               | Relative order to run the plugin. |
| `outputDirectory`           | `Path`    | unspecified       | The location to output generated sources. Defaults to the default for the Maven goal if unspecified. |
| `registerAsCompilationRoot` | `boolean` | `true`            | If `true`, Maven will consider the output sources as compilable sources for `maven-compiler-plugin`. |
| `skip`                      | `boolean` | `false`           | Set to `true` to skip resolution and invocation. |

### Binary plugins from the system path

If you instead wish to read the executable from the system `$PATH`, then you can specify an
executable name to find instead.

Path plugins should have the `kind` attribute set to `path`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <plugins>
      <plugin kind="path">
        <name>protoc-gen-grpc-java</name>
      </plugin>
    </plugins>
  </configuration>

  ...
</plugin>
```

| Property                    | Type           | Default           | Description             |
|:----------------------------|:---------------|:------------------|:------------------------|
| `name`                      | `String`       |                   | The name of the executable on the system path. On Windows, this must not include the file extension. |
| `optional`                  | `boolean`      | `false`           | If `true`, then any failure to fetch the resource will not halt the build. Use this if some platforms lack support for the plugin. |
| `options`                   | `String`       | unspecified       | Options to pass to the plugin via `protoc`'s options API. |
| `order`                     | `int`          | `0`               | Relative order to run the plugin. |
| `outputDirectory`           | `Path`         | unspecified       | The location to output generated sources. Defaults to the default for the Maven goal if unspecified. |
| `registerAsCompilationRoot` | `boolean`      | `true`            | If `true`, Maven will consider the output sources as compilable sources for `maven-compiler-plugin`. |
| `skip`                      | `boolean`      | `false`           | Set to `true` to skip resolution and invocation. |


#### Skipping if unavailable

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved on the system path. This is useful for specific cases where resources may only be available 
during CI builds but do not prevent the application being built locally.

#### How path lookup works

On Linux, macOS, and other POSIX-like operating systems, this will read the `$PATH` environment
variable and search for a binary named the given name case-sensitively. The executable **MUST** be
executable by the current user (i.e. `chmod +x /path/to/binary`), otherwise it will be ignored.

On Windows, this will respect the `%PATH%` environment variable (case-insensitive). The path will
be searched for files where their name matches the binary case-insensitively, ignoring the file
extension. The file extension must match one of the extensions specified in the `%PATHEXT%`
environment variable. The above example would therefore match `protoc-gen-grpc-java.EXE` on Windows,
as an example.

### Binary plugins from specific locations

In some situations, you may wish to download plugins directly from a URL or run them from a 
specific file system path. URL plugins help you achieve this goal.

URL Maven plugins should have the `kind` attribute set to `url`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <plugins>
      <plugin kind="url">
        <url>file:///opt/protoc/protoc-gen-grpc-java</url>
      </plugin>
      <plugin kind="url">
        <url>ftp://company-server.internal/some-other-plugin.exe</url>
      </plugin>
      <plugin kind="url">
        <url>https://some-website.net/downloads/my-cool-protoc-plugin.exe</url>
        <options>some-option=some-value</options>
      </plugin>
    </plugins>
  </configuration>

  ...
</plugin>
```

| Property                    | Type           | Default           | Description             |
|:----------------------------|:---------------|:------------------|:------------------------|
| `url`                       | `URI`          |                   | The URI to fetch. |
| `digest`                    | `String`       | unspecified       | If specified, the contents of the plugin binary will be validated against the digest. E.g. `sha1:9478159bef3d3c6fe5c2fe084a74ce5e92b6c070` |
| `optional`                  | `boolean`      | `false`           | If `true`, then any failure to fetch the resource will not halt the build. Use this if some platforms lack support for the plugin. |
| `options`                   | `String`       | unspecified       | Options to pass to the plugin via `protoc`'s options API. |
| `order`                     | `int`          | `0`               | Relative order to run the plugin. |
| `outputDirectory`           | `Path`         | unspecified       | The location to output generated sources. Defaults to the default for the Maven goal if unspecified. |
| `registerAsCompilationRoot` | `boolean`      | `true`            | If `true`, Maven will consider the output sources as compilable sources for `maven-compiler-plugin`. |
| `skip`                      | `boolean`      | `false`           | Set to `true` to skip resolution and invocation. |

#### Skipping if unavailable

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved. This is useful for specific cases where resources may only be available during CI builds but do not
prevent the application being built locally. If set to optional, then any "not found" response provided by
the underlying URL protocol will be ignored.

#### Digests

If you wish to verify that the content of a URL's resource matches the expected digest, you can
provide the `digest` attribute to verify the integrity. This takes the format
`<digest>md5:6c224d84618c71e2ebb46dd9c4459aa6</digest>`, and supports `md5`, `sha1`, `sha256`,
`sha512`, and any other JDK-provided message digest types.

#### Caveats

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.

---

## Pure-Java plugins

If a `protoc` plugin is distributed as a platform-independent JAR archive rather than a native
executable, you can instruct this Maven plugin to invoke the artifact as part of compilation. To
do this, simply specify the `jvmMavenPlugins` configuration property, passing in a list of
dependencies to execute.

JVM Maven plugins should have the `kind` attribute set to `jvm-maven`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <plugins>
      <plugin kind="jvm-maven">
        <!-- Use the JAR that Salesforce distributes -->
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
      </plugin>
    </plugins>
  </configuration>

  ...
</plugin>
```

| Property                    | Type           | Default           | Description             |
|:----------------------------|:---------------|:------------------|:------------------------|
| `groupId`                   | `String`       |                   | The group ID to use.    |
| `artifactId`                | `String`       |                   | The artifact ID to use. |
| `version`                   | `String`       |                   | The version to use.     |
| `classifier`                | `String`       | unspecified       | The classifier to use. Defaults to an OS and CPU-specific string matching the conventions used by `protoc` |
| `type`                      | `String`       | `jar`             | The artifact type.      |
| `jvmArgs`                   | `List<String>` | empty             | Additional command line arguments to pass to the plugin. |
| `jvmConfigArgs`             | `List<String>` | \*             | JVM arguments to pass to Java. |
| `mainClass`                 | `String`       | unspecified       | Lets you override the Java entrypoint for cases where no `Main-Class` manifest attribute is set. |
| `options`                   | `String`       | unspecified       | Options to pass to the plugin via `protoc`'s options API. |
| `order`                     | `int`          | `0`               | Relative order to run the plugin. |
| `outputDirectory`           | `Path`         | unspecified       | The location to output generated sources. Defaults to the default for the Maven goal if unspecified. |
| `registerAsCompilationRoot` | `boolean`      | `true`            | If `true`, Maven will consider the output sources as compilable sources for `maven-compiler-plugin`. |
| `skip`                      | `boolean`      | `false`           | Set to `true` to skip resolution and invocation. |

<small>* The default JVM config arguments are implementation-specific, but designed to provide
optimal performance during builds. Overriding this property will disable those defaults.</small>

### Caveats

Currently, you are required to be able to execute `*.bat` files on Windows, or have 
`sh` available on the system `$PATH` for any other platform in order for this feature to work.

### Overriding the entrypoint class

If you are supplying the JVM plugin from the same Maven project where it is not 
assembled yet, or the plugin does not specify a `Main-Class` attribute in the 
manifest, then you can specify (or override) the main class via the `mainClass` 
attribute.

```xml
<plugin kind="jvm-maven">
  <groupId>org.example</groupId>
  <artifactId>my-super-awesome-plugin</artifactId>
  <version>${my-super-awesone-plugin.version}</version>
  <mainClass>org.example.protocplugin.MySuperAwesomePluginMainClass</mainClass>
</plugin>
```

This can be useful in a small set of cases, such as when developing a Java-based plugin.

### Command line arguments

Since JVM plugins work by internally bootstrapping a Java process per invocation, you can pass some extra
options here.

If you wish to provide some command line arguments, you can provide the `<jvmArgs>` parameter in each
`<plugin kind="jvm-maven">` block. This is a list of string arguments. Of course, support for this relies entirely
on the plugin supporting the use of command line arguments in the first place.

For example:

```xml
<plugin kind="jvm-maven">
  ...
  <jvmArgs>
    <jvmArg>--logger.level=DEBUG</jvmArg>
    <jvmArg>--include-comments</jvmArg>
  </jvmArgs>
</plugin>
```

### JVM configuration arguments

You can also override the JVM configuration arguments itself if you need to tune how the JVM is running. By
default, JVM configuration arguments are passed that disable the full server JIT, and enable class sharing
where supported. This optimises build times for short-lived JVM processes. Overriding this argument will
**replace** these default arguments as well.

An example of providing custom arguments would be:

```xml
<plugin kind="jvm-maven">
  ...
  <jvmConfigArgs>
    <jvmConfigArg>-Xshare:off</jvmConfigArg>
    <jvmConfigArg>-Xms100m</jvmConfigArg>
    <jvmConfigArg>-Xmx500m</jvmConfigArg>
    <jvmConfigArg>-Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG</jvmConfigArg>
  </jvmConfigArgs>
</plugin>
```

### Developing JVM-based plugins

JVM-based plugins are fairly simple to create. This section will not cover the details of the
standard plugin API itself, but a few considerations are needed to ensure that it works with this
Maven Plugin correctly.

At a minimum:

- You must depend on `com.google.protobuf:protobuf-java`.
- You must read a `com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest` from `stdin`
  (i.e. `System.in`).

  ```java
  CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in);
  ```

- You must write out a `com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse` to
  `stdout` (i.e. `System.out`).

  ```java
  CodeGeneratorResponse response = CodeGeneratorResponse.newBuilder()
      ...
      .build();

  response.writeTo(System.out);
  ```
- If logging, any logs must be written to `stderr` (i.e. `System.err`) rather than `stdout` to
  prevent corrupting the response that `protoc` will read.

  - If using `java.logging`, logs will stream to `stderr` by default.
  - If using the SLF4J simple logger backend, logs will write to `stderr` by default.
  - If using Log4J2 or Logback, ensure your console appender writes to `stderr` explicitly.
  - Avoid the use of libraries that abuse writing to `System.out` where possible.
    You may be able to reassign `System.out` via `System#setOut` and write to a
    `FileOutputStream` wrapping a [`FileDescriptor`](https://github.com/openjdk/jdk/blob/36d2c277c47767ba22208e2e49c46001642bd4f5/src/java.base/share/classes/java/io/FileDescriptor.java#L49)
    with value `0` if this proves to be problematic.
- Your JAR should have a `Main-Class` manifest entry pointing to your `main` method.

  ```xml
  <!-- pom.xml plugins section -->
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
      <archive>
        <manifest>
          <mainClass>org.example.protocplugin.ProtocPlugin</mainClass>
        </manifest>
      </archive>
    </configuration>
  </plugin>
  ```

This Maven plugin will resolve any dependencies for you, so there is no need to shade
your dependencies if you only wish to work with this plugin.

#### Compatibility with other build systems and toolchains

If you only plan to use this Maven plugin to orchestrate your Protobuf builds, you can simply
distribute your JAR on your Maven package registry. Everything will work out of the box without the
need for further platform-specific configuration.

If you need to support `protoc` calling your plugin directly (e.g. for users who do not use this
Maven plugin in their builds), you will need to provide a binary distribution of your plugin for
each CPU architecture and operating system you wish to support. Some starting points for this
include:

- Building a GraalNative executable.
- Using the `jpackage` tool distributed with your JDK.
- Using JNI to invoke your Java entrypoint from a C/C++ application.
- Using WinRun4J.
- Distributing batch files and shell scripts that bootstrap the JVM application
  (similar to those generated by this Maven plugin internally).

Remember your obligations for any open source licenses used for the JVM distribution and
any dependencies if you go down this route, as you will be distributing an opaque
fat binary or fat JAR.

---

## Mixing plugin kinds

Multiple plugins can be provided if needed. For example, if you are using the 
[Salesforce Reactor gRPC libraries](https://github.com/salesforce/reactive-grpc/tree/master),
then you can provide the following:

```xml
<plugins>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    ...
    <plugins>
      <plugin kind="binary-maven">
        <groupId>io.grpc</groupId>
        <artifactId>protoc-gen-grpc-java</artifactId>
        <version>${grpc.version}</version>
        <order>1</order>
      </plugin>
      <plugin kind="jvm-maven">
        <!-- Use the JAR that Salesforce distributes -->
        <groupId>com.salesforce.servicelibs</groupId>
        <artifactId>reactor-grpc</artifactId>
        <version>${reactor-grpc.version}</version>
        <order>2</order>
      </plugin>
    </plugins>
  </configuration>

  ...
</plugin>
```

It would also be valid to use a binary plugin for Salesforce here if you prefer.

---

## Plugin ordering

Plugins are inherently orderable and can be ordered relative to each-other, as well as
relative to the built-in generators in `protoc` and the generation of descriptors.

By default, plugins are applied with the same precedence as descriptors and languages,
which have an order of `0`, but this can be overridden to a negative integer to run the
plugin first, or to a positive integer to run the plugin last.

The order can be specified with the `order` attribute on any plugin block. For example:

```xml
<plugin kind="jvm-maven">
  ...
  <!-- Always run before generating Java sources. -->
  <order>-999</order>
</plugin>
```

The order that generation is performed in for equally-ordered plugins is undefined, but
guaranteed to be stable between builds on the same system in the same location.

---

## Running plugins without default Java code generation

You can turn the default Java code generation step off by setting `<javaEnabled>false</javaEnabled>` on the
protobuf-maven-plugin configuration. This will then only attempt to run the plugins you provide.

---

## Defining plugins in the parent POM

You can define plugins in a parent POM and child projects will inherit it.

By default, if you define the plugin in the parent and the child POM, Maven will merge the
definitions together.

If you wish for plugins in the child POM to be appended to the list in the parent POM, you can
add the `combine.children="append"` XML attribute to the parent POM elements:

```xml
<plugins combine.children="append">
  ...
</plugins>
```

You can alternatively use `combine.self="override"` if you want child POMs to totally replace
anything defined in the parent.
