# Changing `protoc` versions

<div id="pmp-toc"></div>

Sometimes, it may not be possible to use the version of `protoc` that is available on Maven.
If you are in this situation, a few alternatives exist.

Specifying the version of `protoc` can be done by setting the `<protoc/>` attribute on the plugin
configuration. There are two formats for setting the value of this attribute:

**Modern way**

The modern way has you specify the kind of the distribution as an attribute, and then you provide
nested attributes to describe it:

```xml
<!-- for example -->
<protoc kind="binary-maven">
  <version>4.28.0</version>
</protoc>
```

**Legacy way**

The legacy way of setting these values was to provide a string directly. This is far less flexible.
Note that the `kind` attribute must _not_ be specified in this case.

```xml
<!-- for example -->
<protoc>4.28.0</protoc>
```

It is recommended that users make use of the modern mechanism where possible.

## Using a binary protoc build from Maven repositories

The normal way of adding a dependency on `protoc`, as documented elsewhere on this site, is to
specify the version to pull in. You should already be familiar with this. This will instruct the
plugin to go to the Maven repository and pull the desired version of `protoc` for the current
platform where possible.

```xml
<protoc kind="binary-maven">
  <version>4.28.0</version>
</protoc>

<!-- legacy way of setting this -->
<protoc>4.28.0</protoc>
```

When using the modern format, you can optionally override other attributes if you wish. They default
to pointing to `mvn:com.google.protobuf/protoc/${version}/${classifier}/exe` where `version` is the
required version, and `classifier` is derived from inspecting the platform that Java is running on.

The above is the same as specifying this explicitly, like so:

```xml
<protoc kind="binary-maven">
  <groupId>com.google.protobuf</groupId>
  <artifactId>protoc</artifactId>
  <version>4.28.0</version>
  <classifier>linux-x86_64</classifier>
  <type>exe</type>
</protoc>
```

Generally, users will not need to be this explicit. The plugin is usually smart enough to work out
what you need for you.

## Using protoc from your system path

If you need to use the version of `protoc` that is installed on your system, you can specify what
to run from the system `$PATH`:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protoc kind="path">
      <name>protoc</name>
    </protoc>
    
    <!-- legacy way of setting this. In this case, PATH will always expect it to be called `protoc',
         and you cannot change this. The above mechanism lets you specify whatever you want as the
         name, so is more flexible. -->
    <protoc>PATH</protoc>
  </configuration>
</plugin>
```

On Linux, macOS, and other POSIX-like operating systems, this will read the `$PATH` environment
variable and search for a binary named `protoc` case-sensitively. The executable **MUST** be
executable by the current user (i.e. `chmod +x /path/to/protoc`), otherwise it will be ignored.

On Windows, this will respect the `%PATH%` environment variable (case-insensitive). The path will
be searched for files where their name matches `protoc` case-insensitively, ignoring the file
extension. The file extension must match one of the extensions specified in the `%PATHEXT%`
environment variable. The above example would match `protoc.exe` on Windows, as an example.

## Using protoc from a specific path

You may wish to run `protoc` from a specific path on your file system. If you need to do this,
you can provide a URL with the `file` scheme to reference it:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protoc kind="url">
      <url>file:///opt/protoc/protoc.exe</url>
    </protoc>
    
    <!-- Legacy way of setting this -->
    <protoc>file:///opt/protoc/protoc.exe</protoc>
  </configuration>
</plugin>
```

The syntax for this is `file://$PATH`, where `$PATH` is a relative or absolute path. For Windows, use 
forward-slashes for this syntax rather than backslashes.

Note that paths are resolved relative to the directory that Maven is invoked from.

## Using protoc from a remote server

If you have a `protoc` binary on a remote FTP or HTTP(S) server, you can provide the URL to download
directly:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>
    <protoc kind="url">
      <url>ftp://company-server.internal/protoc/protoc.exe</url>
    </protoc>
    <!-- or -->
    <protoc>
      <url>http://company-server.internal/protoc/protoc.exe</url>
    </protoc>
    <!-- or -->
    <protoc>
      <url>https://company-server.internal/protoc/protoc.exe</url>
    </protoc>
  </configuration>
</plugin>
```

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.
