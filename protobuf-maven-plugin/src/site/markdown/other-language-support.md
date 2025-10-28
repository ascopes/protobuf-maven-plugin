# Other language support

<div id="pmp-toc"></div>

While Maven does not provide official plugins for most other programming
languages, the protobuf-maven-plugin allows you to generate source code for
a number of other programming languages.

## Languages directly supported by `protoc`

By default, only Java generation is enabled. You can turn this off if desired.

To enable other languages, use the following attributes within your `configuration` block;

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
      <td>Python</td>
      <td><code>pythonEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
    <tr>
      <td>Python typeshed stubs</td>
      <td><code>pythonStubsEnabled</code></td>
      <td><code>false</code></td>
      <td>Enable this alongside <code>pythonEnabled</code> to generate MyPy-compatible typehint stubs.</td>
    </tr>
    <tr>
      <td>Ruby</td>
      <td><code>rubyEnabled</code></td>
      <td><code>false</code></td>
      <td/>
    </tr>
  </tbody>
</table>

### Further support

Any other languages natively available in `protoc` can be enabled by passing `<arguments/>`
in the plugin configuration. For example:

```xml
<configuration>
  <arguments>
    <argument>--cpp_out=target/generated-sources/cxx</argument>
  </arguments>
  <protoc>...</protoc>
</configuration>
```

## Languages and tooling without first-class support

Any other language integrations can be provided to this plugin in the shape of
third-party custom `protoc` plugins.

See "[using protoc plugins](./using-protoc-plugins.md)" for full details.

### Caveats

It is also important to note that you need to provide a valid compiler or tooling to
make use of the generared sources (other than Java). For example, Kotlin generation
would require you to also configure the `kotlin-maven-plugin`. See 
[the Kotlin Maven Plugin documentation](https://kotlinlang.org/docs/maven.html) for details on
how to configure your builds for Kotlin.

Kotlin generation only supports the generation of JVM descriptors that compliment
the Java outputs. This means you will need both Java and Kotlin compilation in your
build. For pure Kotlin support, a third-party `protoc` plugin is required.
