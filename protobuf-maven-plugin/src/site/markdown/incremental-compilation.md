# Incremental compilation

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

For a way to quickly include or exclude sources based upon a glob, check out the
[includes](https://ascopes.github.io/protobuf-maven-plugin/generate-mojo.html#includes)
and
[excludes](https://ascopes.github.io/protobuf-maven-plugin/generate-mojo.html#excludes)
parameters in the goal documentation.
