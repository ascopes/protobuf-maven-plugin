# Faster builds

When you have large numbers of proto files, builds can become slow and hinder development. To work
around this, there are a few things you can do.

## Incremental compilation

By default, this plugin will regenerate all protobuf sources each time it runs.
This is usually fine, but may slow builds down if you are rebuilding a lot or
have a large number of `*.proto` files.

As of v2.7.0, an experimental incremental compilation feature has been added that
enables the plugin to detect whether sources and dependencies have changed since
the last build. If sources and dependencies have not changed, then `protoc` will
not be re-invoked. Likewise, if only a subset of sources have changed, then only
those sources will be generated again.

To enable this feature, use the following plugin configuration:

```xml
<configuration>
  <incrementalCompilation>true</incrementalCompilation>
</configuration>
```

You can alternatively set the `protobuf.compiler.incremental` property to `true`
on the commandline or via the Maven properties.

Remember that this feature is experimental and subject to change in future
releases. It may not work correctly with custom `protoc` plugins that expect
all sources to be passed to `protoc` on every single build.

## Including/excluding file patterns

For a way to quickly include or exclude sources based upon a glob, check out the
[includes](https://ascopes.github.io/protobuf-maven-plugin/generate-mojo.html#includes)
and
[excludes](https://ascopes.github.io/protobuf-maven-plugin/generate-mojo.html#excludes)
parameters in the goal documentation. You can utilise this to reduce the number of files
you are passing to `protoc` on clean builds if you are not using incremental compilation.

## Compile using ECJ rather than javac

Another way of improving build speeds is to switch out `javac` with the Eclipse Java Compiler
backend for `maven-compiler-plugin`. This appears to reduce compilation times by around 25%
for massive projects:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>

  <configuration>
    <compilerId>eclipse</compilerId>
    <!-- ECJ raises warnings we do not care about in the generated code. -->
    <failOnWarning>false</failOnWarning>
    <showWarnings>false</showWarnings>
  </configuration>

  <dependencies>
    <dependency>
      <groupId>org.codehaus.plexus</groupId>
      <artifactId>plexus-compiler-eclipse</artifactId>
      <version>2.15.0</version>
    </dependency>
  </dependencies>
</plugin>
```

## Move proto files into a separate module

If you are pairing your proto files with your application logic, you may find your project has a
large number of files in it, which will hinder build speeds. You can always consider moving your
proto files into a separate Maven project that you only rebuild when the proto files change. By
doing this, you can just reference the generated code as a dependency.
