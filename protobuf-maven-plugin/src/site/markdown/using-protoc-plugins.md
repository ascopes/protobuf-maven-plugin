# Using `protoc` Plugins

<div id="pmp-toc"></div>

If you wish to generate gRPC stubs, or outputs for other languages like Scala that are not already
covered by the protoc executable, you can add custom plugins to your build.

## Common options

Each plugin is defined as an XML object with various attributes. Many depend on the type of plugin
you are adding (see the sections below), but all plugins share some common attributes:

- `outputDirectory` - directory to write generated code to. The default is for this to be
  unspecified, which will result in the output directory specified on the Maven plugin to be used
  instead.
- `registerAsCompilationRoot` - overrides the project-global setting for registering output
  code directories as compilation roots for future compiler steps in the Maven build. If 
  unspecified/null, the project global setting will be used. Otherwise, the explicitly provided
  value will be used instead.
- `options` - a string value that can be passed to the plugin as a parameter. Defaults to being
  unspecified.
- `order` - an integer that controls the plugin execution order relative to other plugins.
  Smaller numbers make plugins run before those with larger numbers. Defaults to `100000`, meaning
  you can place plugins before or after those that do not define an order if you wish.
- `skip` - a boolean that, when true, skips the execution of the plugin entirely. Defaults to
  `false`.

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

## Binary plugins

Binary plugins are OS-specific executables that are passed to `protoc` directly, and are the 
standard way of handling plugins with `protoc`.

### Binary plugins from Maven Central

If the plugin you wish to use is on Maven Central or any other Maven repository, you can reference
that plugin directly via the group ID, artifact ID, and version (like any other Maven artifact).

Binary Maven plugins should have the `kind` attribute set to `binary-maven`.

**Note that this is equivalent to passing a list of `<binaryMavenPlugins>` in previous versions. 
This old behaviour will be removed in v5.0.0.**

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

Note that for gRPC versions prior to 1.74.0, you should also pass the following options
to avoid dependencies on `javax.annotation-api`.

```xml
<options>@generated=omit</options>
```

### Binary plugins from the system path

If you instead wish to read the executable from the system `$PATH`, then you can specify an
executable name to find instead.

Path plugins should have the `kind` attribute set to `path`.

**Note that this is equivalent to passing a list of `<binaryPathPlugins>` in previous versions. This
old behaviour will be removed in v5.0.0.**

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

Note that for gRPC versions prior to 1.74.0, you should also pass the following options
to avoid dependencies on `javax.annotation-api`.

```xml
<options>@generated=omit</options>
```

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved on the system path. This is useful for specific cases where resources may only be available 
during CI builds but do not prevent the application being built locally.

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

**Note that this is equivalent to passing a list of `<binaryUrlPlugins>` in previous versions. This
old behaviour will be removed in v5.0.0.**

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

You can also mark these plugins as being optional by setting `<optional>true</optional>` on the
individual plugin objects. This will prevent the Maven plugin from failing the build if the `protoc` plugin
cannot be resolved. This is useful for specific cases where resources may only be available during CI builds but do not
prevent the application being built locally. If set to optional, then any "not found" response provided by
the underlying URL protocol will be ignored.

If you wish to verify that the content of a URL's resource matches the expected digest, you can
provide the `digest` attribute to verify the integrity. This takes the format
`<digest>md5:6c224d84618c71e2ebb46dd9c4459aa6</digest>`, and supports `md5`, `sha1`, `sha256`,
`sha512`, and any other JDK-provided message digest types.

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.

## Pure-Java plugins

If a `protoc` plugin is distributed as a platform-independent JAR archive rather than a native
executable, you can instruct this Maven plugin to invoke the artifact as part of compilation. To
do this, simply specify the `jvmMavenPlugins` configuration property, passing in a list of
dependencies to execute.

JVM Maven plugins should have the `kind` attribute set to `jvm-maven`.

**Note that this is equivalent to passing a list of `<jvmMavenPlugins>` in previous versions. This
old behaviour will be removed in v5.0.0.**

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

Currently, you are required to be able to execute `*.bat` files on Windows, or have 
`sh` available on the system `$PATH` for any other platform.

If you are supplying the JVM plugin from the same Maven project where it is not 
assembled yet, or the plugin does not specify a `Main-Class` attribute in the 
manifest, then you can specify (or override) the main class via the `mainClass` 
attribute.

```xml
<plugin kind="jvm-maven">
  <groupId>${project.parent.groupId}</groupId>
  <artifactId>my-super-awesome-plugin</artifactId>
  <version>${project.parent.version}</version>
  <mainClass>org.example.protocplugin.MySuperAwesomePluginMainClass</mainClass>
</plugin>
```

### Commandline arguments

Since JVM plugins work by internally bootstrapping a Java process per invocation, you can pass some extra
options here.

If you wish to provide some commandline arguments, you can provide the `<jvmArgs>` parameter in each
`<plugin kind="jvm-maven">` block. This is a list of string arguments. Of course, support for this relies entirely
on the plugin supporting the use of commandline arguments in the first place.

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

## Mixing plugins

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
      </plugin>
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

It would also be valid to use a binary plugin for Salesforce here if you prefer.

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

## Running plugins without default Java code generation

You can turn the default Java code generation step off by setting `<javaEnabled>false</javaEnabled>` on the
protobuf-maven-plugin configuration. This will then only attempt to run the plugins you provide.

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
