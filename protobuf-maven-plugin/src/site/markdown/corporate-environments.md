# Corporate environments

<div id="pmp-toc"></div>

Some users may be utilising this plugin within a locked-down corporate environments.

The following documents some usage patterns that may be useful to corporate users.

## Limited executable locations

Some development environments will have corporate-mandated locations that any custom executables
must be run from. In this case, failing to run executables from said locations often will result in 
builds being forcefully aborted and failing.

To work around this, a "sanctioned executable path" directory can be configured within this plugin.
When specified, any executables will first be copied to a unique path within this directory. Any
calls to the original executables will be changed to invoke the executables within the sanctioned
directory.

This setting is designed to be able to be set within profiles and within parent POMs if
desired, so a path that is unique to each project will be generated during the build process.

**Note**: it is up to users to periodically clear out such a directory if specified.

To configure this, there are a few options:

- Configure the plugin directly within the project or plugin management of the parent project.

  ```xml
  <configuration>
    <sanctionedExecutablePath>C:\dev\protobuf-maven-plugin</sanctionedExecutablePath>
  </configuration>
  ```

- Configure a property within the project or parent plugin management.

  ```xml
  <properties>
    <protobuf.sanctioned-executable-path>C:\dev\protobuf-maven-plugin</protobuf.sanctioned-executable-path>
  </properties>
  ```

- Configure Maven properties in the root of your repository.

  ```
  # .mvn/maven.config
  -Dprotobuf.sanctioned-executable-path=C:\dev\protobuf-maven-plugin
  ```

- Configure a global environment variable to propagate this configuration to all
  invocations of Maven on the current machine.

  ```
  MAVEN_ARGS=-Dprotobuf.sanctioned-executable-path=C:\dev\protobuf-maven-plugin
  # or
  MAVEN_OPTS=-Dprotobuf.sanctioned-executable-path=C:\dev\protobuf-maven-plugin
  ```

If this solution is not desirable, the other option is to ensure your `.m2` and
project you are building are located within the sanctioned path.

## No permission to run executables

Corporate users may be forbidden from running downloaded executables.

In order to use this plugin, the ability to run executables is required. This will need to be
discussed with the user's IT administrator if it is problematic.

Users may also consider having `protoc` and any binary executable plugins shipped on their
system `$PATH`. Configuring this plugin to read `protoc` and associated plugins from the system
`$PATH` is supported and documented in the goal documentation.

## No permission to run scripts

Corporate users may be forbidden from running scripts.

To utilise JVM plugins, this plugin needs the ability to run batch scripts on Windows, or shell
scripts on other operating systems. This is required as `protoc` lacks the ability to run JARs
directly.

Historically, other protobuf plugins have instead required the presence of a C compiler to generate
a JNI-integrated entrypoint. In this plugin, host-specific scripts are used instesd due to the
improved simplicity and lack of additional dependencies.

Users should discuss this with their IT administrator if this is problematic.

## Using package mirrors

Corporate environments often utilise mirrors of Maven Central for package management.

This usually uses a system such as:

- Sonatype Nexus
- JFrog Artifactory
- GitLab Packages
- AWS CodeArtifact

This plugin supports the use of mirrors out of the box, as dependency resolution is peformed using
the Maven subsystem for artifact resolution. Users must only ensure that their `settings.xml` and
`settings-security.xml` (for Maven 3) or keychain (for Maven 4) are configured
appropriately.

## Using authenticated HTTP/FTP endpoints for direct downloads

At this time, the use of authenticated HTTP or FTP endpoints for direct downloads
of resources specified by URLs is not supported outside encoding credentials within the authority
section of the URL itself.

Users should consider placing such resources in a Maven package registry, or keeping them on their
local machine.

## Dependency scanning

Scanning of dependencies is not covered by this plugin, and is considered out of scope.
