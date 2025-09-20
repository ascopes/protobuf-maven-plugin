# URL support

<div id="pmp-toc"></div>

URLs can be used in a few different places within this plugin.

To make life easier for users, a number of standard and non-standard protocols
are included out of the box. This enables fetching of resources, extraction of
resources from within archives, and decompression of archives.

## Protocol nesting

Protocols within URLs are stackable and nestable. When being evaluated, the innermost
URL fragment is evaluated first, and then fed into the next handler until all have
been evaluated. Take the following example:

```
tar:gz:https://some-website.com/protoc-gen-bang.tar.gz!/bin/protoc-gen-bang
```

A lot is going on here.

1. We fetch `https://some-website.com/protoc-gen-bang.tar.gz`
2. We decompress the fetched resource using the `gz` protocol (gzip).
3. We treat the decompressed resource as a tarball, and extract an entry named
  `bin/protoc-gen-bang` from this archive.
4. The resulting binary is fed into the plugin.

The syntax for this will be discussed further down this page, but the main point to remember
is that it is designed to be fairly close to the way `java.net.URL` is used... just with
a number of non-standard protocols out of the box.

## Digests

When performing digest verification against a URL resource, the digest is always
used to verify the final output artifact within the URL. For example, if you specify
the following URL:

```
zip:ftp://some-server.net/archives/data.zip!/data/some-file.txt
```

...then the digest will be compared against the binary content for `data/some-file.txt`.
It will **not** be computed for the outer ZIP archive.

## Supported protocols

Protocol support covers three main use cases:

- Fetching a resource from a local or remote file system.
- Decompressing/transforming a fetched resource.
- Parsing a fetched resource as some kind of archive, extracting some content from it.

### "Fetching" protocols

- `file:///foo/bar/baz/file.zip` - read `/foo/bar/baz/file.zip` as an absolute path from
  the local file system.
    - Note that on Windows, you must still use forward slashes. E.g. `C:\Foo\Bar Baz\File.zip`
      should be specified as `file:///c/Foo/Bar Baz/File.zip`.
- `file://foo/bar/baz/file.zip` - read `foo/bar/baz/file.zip` as a path relative to the
  current Maven project directory on the local file system.
- `ftp://server.net/path/to/file.zip` - fetch `path/to/file.zip` from the FTP server
  hosted at `server.net` (provided by the Java standard library).
- `sftp://server.net/path/to/file.zip` - fetch `path/to/file.zip` from the SFTP server
  hosted at `server.net` (provided by the Java standard library).
- `http://server.net/path/to/file.zip` - fetch `path/to/file.zip` from the HTTP server
  hosted at `server.net` (provided by the Java standard library).
- `http://server.net/path/to/file.zip` - fetch `path/to/file.zip` from the HTTPS server
  hosted at `server.net` (provided by the Java standard library).

**Note:**

- When Maven is run in offline mode, all fetching protocols other than `file:` will be
  unavailable.
- Authentication support is not provided by this plugin at this time.

### "Archiving" protocols

All archiving protocols take the following format:

```
archiving_protocol ::= PROTOCOL ':' url '!/' PATH
```

...where `url` is a nested URL.

- `zip:...!/path/to/file` - treats `...` as a ZIP archive, fetching `path/to/file` from
  inside it (provided by Apache Commons Compress).
- `jar:...!/path/to/file` - treats `...` as a JAR archive, fetching `path/to/file` from
  inside it (provided by Apache Commons Compress).
- `war:...!/path/to/file` - treats `...` as a WAR archive, fetching `path/to/file` from
  inside it (provided by Apache Commons Compress).
- `ear:...!/path/to/file` - treats `...` as a EAR archive, fetching `path/to/file` from
  inside it (provided by Apache Commons Compress).
- `tar:...!/path/to/file` - treats `...` as a decompressed TAR archive, fetching
  `path/to/file` from inside it (provided by Apache Commons Compress).

For more archive formats supported by Apache Commons Compress out of the box, please raise
an issue with appropriate details.

### "Decompressing" protocols

All decompressing protocols take the following format:

```
archiving_protocol ::= PROTOCOL ':' url
```

...where `url` is a nested URL.

- `gzip:...`, `gz:` - treats `...` as a GZIP archive, decompressing it (provided by the
  Java standard library).
- `bz2:...`, `bzip:...` - treats `...` as a BZip2 archive, decompressing it (provided by
  Apache Commons Compress).

For more decompressing formats supported by Apache Commons Compress out of the box, please
raise an issue with appropriate details.

## Support

Each underlying implementation is subject to any caveats documented by the developers
of that implementation. Please raise any issues with them as appropriate.
