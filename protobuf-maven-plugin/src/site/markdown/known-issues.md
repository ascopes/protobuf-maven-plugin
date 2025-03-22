# Known Issues

<div id="pmp-toc"></div>

While every attempt is made to ensure this plugin is as functional and complete as possible,
some known issues exist.

## Plexus and Maven

Prior to Maven 3.9, a component named `plexus-tools` was included by Apache
Maven at runtime. The component is used by Maven plugin internals. In v3.9.0 of
Maven, this was removed. Some older Maven plugins may still need this dependency
to work. The protobuf-maven-plugin does not require this to operate in normal cases.

Under some conditions, it appears that the use of certain other plugins or
extensions triggers an issue within Maven where `plexus-utils` becomes a requirement.
A current known case occurs when users make use of the Maven Cache Extension. The
following error may be observed if this issue is encountered:

```
[ERROR] org.apache.maven.plugin.PluginContainerException: Unable to load the mojo 'generate' (or one of its required components) from the plugin 'io.github.ascopes:protobuf-maven-plugin:2.7.0': com.google.inject.ProvisionException: Unable to provision, see the following errors:
[ERROR] 
[ERROR] 1) [Guice/ErrorInCustomProvider]: IllegalStateException
[ERROR]   at MojoExecutionScopeModule.configure(MojoExecutionScopeModule.java:50)
[ERROR]       \_ installed by: WireModule -> MojoExecutionScopeModule
[ERROR]   at TemporarySpace.<init>(TemporarySpace.java:45)
[ERROR]       \_ for 2nd parameter
[ERROR]   at UrlResourceFetcher.<init>(UrlResourceFetcher.java:65)
[ERROR]       \_ for 1st parameter
[ERROR]   at ProtocResolver.<init>(ProtocResolver.java:63)
[ERROR]       \_ for 5th parameter
[ERROR]   at ProtobufBuildOrchestrator.<init>(ProtobufBuildOrchestrator.java:71)
[ERROR]       \_ for 2nd parameter
[ERROR]   at AbstractGenerateMojo.sourceCodeGenerator(AbstractGenerateMojo.java:71)
[ERROR]       \_ for field sourceCodeGenerator
[ERROR]   while locating MainGenerateMojo
[ERROR]   at ClassRealm[plugin>io.github.ascopes:protobuf-maven-plugin:2.7.0, parent: ClassLoaders$AppClassLoader@531d72ca]
[ERROR]       \_ installed by: WireModule -> PlexusBindingModule
[ERROR]   while locating Mojo annotated with @Named("io.github.ascopes:protobuf-maven-plugin:2.7.0:generate")
```

Note that other errors like this may also occur.

The workaround for now appears to be to include `plexus-utils` explicitly as a dependency of this plugin:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>...</version>

  <configuration>...</configuration>
  <executions>...</executions>

  <dependencies>
    <dependency>
      <groupId>org.codehaus.plexus</groupId>
      <artifactId>plexus-utils</artifactId>
      <version>4.0.2</version>
    </dependency>
  </dependencies>
</plugin>
```

- See https://github.com/ascopes/protobuf-maven-plugin/issues/472 for tracking this issue.

## Incremental compilation with descriptor files

Using incremental compilation when creating descriptor files is not supported due to limitations
with `protoc`. The entire descriptor must be rebuilt on each build to remain valid.

As of 2.10.0, incremental compilation will be disabled automatically if this condition is
detected.
