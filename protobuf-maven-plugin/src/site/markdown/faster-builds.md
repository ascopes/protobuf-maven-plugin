# Faster builds

When you have large numbers of proto files, builds can become slow and hinder development. To work
around this, there are a few things you can do.

## Incremental compilation

As of v2.7.0, an incremental compilation feature has been added that enables the
plugin to detect whether sources and dependencies have changed since the last build.
If sources and dependencies have not changed, then `protoc` will not be re-invoked.

As of v2.8.0, this feature is enabled by default.

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

Note that you may see new warnings, as ECJ tends to be much more vocal by default than javac.

## Move proto files into a separate module

If you are pairing your proto files with your application logic, you may find your project has a
large number of files in it, which will hinder build speeds. You can always consider moving your
proto files into a separate Maven project that you only rebuild when the proto files change. By
doing this, you can just reference the generated code as a dependency.

## Skipping plugin invocation entirely

If you do not want the plugin to run at all, you can invoke Maven with the `-Dprotobuf.skip` flag. This will
skip the invocation of this plugin entirely.
