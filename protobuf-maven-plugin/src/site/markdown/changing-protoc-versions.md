# Changing `protoc` versions

<div id="pmp-toc"></div>

Sometimes, it may not be possible to use the version of `protoc` that is available on Maven.
If you are in this situation, a few alternatives exist.

## Using protoc from your system path

If you need to use the version of `protoc` that is installed on your system, specify the version
as `PATH`.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <version>...</version>

  <configuration>
    <protoc>PATH</protoc>
  </configuration>

  ...
</plugin>
```

On Linux, MacOS, and other POSIX-like operating systems, this will read the `$PATH` environment
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
    <protoc>ftp://company-server.internal/protoc/protoc.exe</protoc>
    <!-- or -->
    <protoc>http://company-server.internal/protoc/protoc.exe</protoc>
    <!-- or -->
    <protoc>https://company-server.internal/protoc/protoc.exe</protoc>
  </configuration>
</plugin>
```

This is not recommended outside specific use cases, and care should be taken to ensure the
legitimacy and security of any URLs being provided prior to adding them.

Providing authentication details or proxy details is not supported at this time.
