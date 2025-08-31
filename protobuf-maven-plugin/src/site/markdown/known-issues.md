# Known Issues

<div id="pmp-toc"></div>

While every attempt is made to ensure this plugin is as functional and complete as possible,
some known issues may exist. Any actively known issues will be documented on this page when they are found.

If you have found a problem. Please [raise an issue on GitHub](https://github.com/ascopes/protobuf-maven-plugin/issues).

Note that any issues fixed in newer versions of both this software and other pieces of software
will not be documented here. Please ensure you are using the latest version of dependencies and
plugins where possible prior to raising an issue.

---

## Windows path length issues

Microsoft Windows historically enforces that executables do not have an absolute path that is
more than 260 characters in size. This has been lifted for Windows 11 but requires JDK changes
to be compatible, and at the time of writing, this has not been implemented.

For users, this means that running builds within overly nested hierarchies, or using very long
directory names on Windows may result in builds failing. This generally looks like the following
error:

```text
java.io.IOException: Cannot run program "C:\some\very\long\path\to\a\script-or-executable.exe": CreateProcess error=2, The system cannot find the file specified
```

At the time of writing, the only real workaround for this is to use a shorter absolute path, which
means moving your build nearer to the root directory of your drive you are building from 
(e.g. `C:\`).

For now, a workaround has been implemented that translates internal generated paths to SHA-256
digests with the aim of reducing the length of overly verbose path names, but past this, there
is not much else that can be reasonably done to fix this until OpenJDK actively address this
issue and release a fix in the JDK.

See:

- [(OpenJDK Bug) JDK-8315405 - Can't start process in directory with very long path](https://bugs.openjdk.org/browse/JDK-8315405) 
- [(OpenJDK Bug) JDK-8348664 - Enable long path support in manifest for java.exe and javaw.exe on Windows](https://bugs.openjdk.org/browse/JDK-8348664)
- [(Microsoft) Maximum File Path Limitation](https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation?tabs=registry)
- [(Microsoft) CreateProcessW function (processthreadsapi.h)](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessw) 

If this blocks you, please mention it on the first bug to help bring further attention to it.

In the meantime, any further bugs relating to this on the `protobuf-maven-plugin` will not be fixed
unless a suitable workaround is available.

This does **not** affect users on Linux or macOS.
