# Corporate environments

<div id="pmp-toc"></div>

Some users may be utilising this plugin within a locked-down corporate environments. This can make
running resolved executables and scripts into a challenge, especially if system administrators
install certain pieces of software that automatically kill or delete unsanctioned executables.

Many users may also have to utilise private package registries, or store components on in-house
servers that are not publically accessible.

The following documents some techniques and general guidance for these situations.

## Limited executable locations

Corporate developers may be required to develop on workstations that only allown sanctioned
executables to be executed outside a specific set of paths.

While it may be tempting (and much easier for me) to suggest that developers work with their IT
administrators to gain access to development environments with reasonable constraints, this Maven
plugin provides an additional potential workaround that is less invasive and can attempt to
satisfy these requirements rather than directly avoid them.

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

- Configure a Maven profile in your organization's parent POM that activates based on
  the development environment. In the following example, we assume our developers work on Windows
  workstations outside a CI environment, so only activate the profile when we detect these
  prerequisites. We assume our CI enviromment sets the `CI` environment variable
  (which is true for GitLab CI, GitHub Actions, and Jenkins, amongst others).

  ```xml
  <project>
    ...
    <profiles>
      <profile>
        <id>corporate-workstation</id>
        <activation>
          <property>
            <name>!env.CI</name>
          </property>
          <os>
            <family>Windows</family>
          </os>
        </activation>

        <!-- Option 1: configure via a property. Quick and simple. -->
        <properties>
          <protobuf.sanctioned-executable-path>C:\dev\protobuf-maven-plugin</protobuf.sanctioned-executable-path>
        </properties>

        <!-- Option 2: configure via additional plugin management. Verbose but clear. -->
        <build>
          <pluginManagement>
            <plugins>
              <plugin>
                <groupId>io.github.ascopes</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>${protobuf-maven-plugin.version}</version>

                <configuration>
                  <sanctionedExecutablePath>C:\dev\protobuf-maven-plugin</sanctionedExecutablePath>
                </configuration>
              </plugin>
            </plugins>
          </pluginManagement>
        </build>
      </profile>
    </profiles>
  </project>
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

Users should discuss this with their IT administrator if this is problematic, or raise

## Using package mirrors

Corporate environments often utilise mirrors of Maven Central for package management.

This usually uses a system such as:

- Sonatype Nexus
- JFrog Artifactory
- GitLab Packages
- AWS CodeArtifact

This plugin supports the use of mirrors out of the box, as dependency resolution is peformed using
the Maven subsystem for artifact resolution. Users must only ensure that their `settings.xml` and
corresponding security credentials up correctly.

## Using authenticated HTTP/FTP endpoints for direct downloads

At this time, the use of authenticated HTTP or FTP endpoints for direct downloads of resources
specified by URLs is not supported outside encoding credentials within the authority section of the
URL itself.

Users should consider placing such resources in a Maven package registry, or keeping them on their
local machine.

### Deploying required executables to your package registry

A good way of keeping resources in a Maven package registry is to have a separate project or
repository that downloads the required resource, and then uses `maven-deploy-plugin` via the
command line to upload it as an artifact to the required package registry.

An example GitLab pipeline for this could look like the following:

```yaml
stages:
  - deploy

Deploy to package registry:
  stage: deploy
  image: container-registry.example.org/maven:latest
  rules:
    - if: CI_COMMIT_TAG
  parallel:
    matrix:
      MAVEN_CLASSIFIER:
        - linux-x86_64      
  before_script:
    # Do whatever you need to in order to set up your settings.xml
    - ...
  script:
    - curl --fail --silent "https://github.com/scalapb/ScalaPB/releases/download/v${CI_COMMIT_TAG}/protoc-gen-scala-${CI_COMMIT_TAG}-${MAVEN_CLASSIFIER}.zip"
    - 'unzip *.zip'
    - >-
      mvn deploy:deploy-file
        -DgroupId=io.github.scalapb
        -DartifactId=protoc-gen-scalapb
        -Dversion="${CI_COMMIT_TAG}"
        -Dclassifier="${MAVEN_CLASSIFIER}"
        -Dpackaging=exe
        -Dfile=protoc-gen-scala
```

## Dependency scanning

Scanning of dependencies is not covered by this plugin, and is considered out of scope.
