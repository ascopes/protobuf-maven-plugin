# Known Issues

<div id="pmp-toc"></div>

While every attempt is made to ensure this plugin is as functional and complete as possible,
some known issues may exist. Any actively known issues will be documented on this page when they are found.

If you have found a problem. Please [raise an issue on GitHub](https://github.com/ascopes/protobuf-maven-plugin/issues).

Note that any issues fixed in other pieces of software will not be documented here. Please ensure
you are using the latest version of dependencies and plugins where possible prior to raising an
issue.

---

## OutOfMemoryException for very large projects

![Status: Fixed in v5.0.1](https://img.shields.io/badge/Fixed--in--v5.0.1-green?style=flat&label=Status)

Prior to v5.0.1, an issue was present as tracked by
[GH-596](https://github.com/ascopes/protobuf-maven-plugin/issues/596) where some users could
observe the JVM exhausting heap memory during dependency resolution.

This issue was previously closed due to being unable to reproduce the conditions that trigger
this behaviour. Changes introduced after this issue was raised appeared to make the issue go away.

As a side effect of certain changes to the artifact resolution lifecycle in
[GH-938](https://github.com/ascopes/protobuf-maven-plugin/issues/938), this issue was re-identified,
and subsequent analysis suggests the trigger is resolution of transitive test artifacts and
fat artifacts that Maven internally attempts to skip.

As part of GH-938, a mitigation has been implemented. Users should update to v5.0.1 or newer of this
plugin if they experience such an issue. If any backport to v4.x is required, please raise an issue
to request the change.

---

## Reactor lifecycle interference

![Status: Partially fixed in v5.0.1](https://img.shields.io/badge/Partially--fixed--in--v5.0.1-yellow?style=flat&label=Status)

Between v2.13.0 and v5.0.0 (inclusive), resolution of project dependencies was handed off to
Apache Maven to manage.

In [GH-938](https://github.com/ascopes/protobuf-maven-plugin/issues/938), users reported that this
can interfere with the lifecycle of Maven when utilising transitive dependencies in a multi-module
project. This manifests as builds failing due to dependency resolution of sibling modules.

Resolution has been refactored in v5.0.1 to mitigate this issue. Users with sibling module
dependencies can instruct the plugin to not resolve project level dependencies by setting
`<ignoreProjectDependencies>true</ignoreProjectDependencies>` and specifying protobuf dependencies
via the `<importDependencies>` block instead.

Users who depend on sibling modules for importable or source dependencies for now should consider
building their project in two steps, performing a `mvn install` of the dependency first.

Please raise any further problems via a new discussion or new issue on GitHub.

---

## Windows path length issues

![Status: JDK issue](https://img.shields.io/badge/JDK--issue-darkgray?style=flat&label=Status)

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

See:

- [(OpenJDK Bug) JDK-8315405 - Can't start process in directory with very long path](https://bugs.openjdk.org/browse/JDK-8315405) 
- [(OpenJDK Bug) JDK-8348664 - Enable long path support in manifest for java.exe and javaw.exe on Windows](https://bugs.openjdk.org/browse/JDK-8348664)
- [(Microsoft) Maximum File Path Limitation](https://learn.microsoft.com/en-us/windows/win32/fileio/maximum-file-path-limitation?tabs=registry)
- [(Microsoft) CreateProcessW function (processthreadsapi.h)](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-createprocessw) 

If this blocks you, please mention it on the first bug to help bring further attention to it.

In the meantime, any further bugs relating to this on the `protobuf-maven-plugin` will not be fixed
unless a suitable workaround is available.

This does **not** affect users on Linux or macOS.
