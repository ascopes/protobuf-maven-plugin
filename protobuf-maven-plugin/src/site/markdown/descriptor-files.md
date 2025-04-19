# Descriptor files

<div id="pmp-toc"></div>

Descriptors are used to encapsulate the information found in one or more `.proto` files into a 
single binary blob that can be passed as an input into `protoc`. They can also be exposed via APIs
such as those used with gRPC to allow clients to use reflection to generate dynamic payloads without
the original proto sources present.

## Building from proto descriptor files

If you have one or more `FileDescriptorSet` payloads (a protocol buffer, defined in
[descriptor.proto](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto), often marked with a `*.binpb` file extension), then you can use this
to generate sources.

### Passing a descriptor file from the local file system

To pass a descriptor file from the local file system, reference it in the `sourceDescriptorPaths`
parameter:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    ...
    <sourceDescriptorPaths>
      <sourceDescriptorPath>${project.basedir}/src/main/resources/some-descriptor.binpb</sourceDescriptorPath>
    </sourceDescriptorPaths>
  </configuration>
</plugin>
```

You can pass as many of these as you like. All sources within these will be compiled. You can
exclude specific sources by using the `includes` and `excludes` parameters (see the usage
documentation).

### Using a descriptor file published in a Maven repository

If you have a descriptor file published in a remote or local Maven repository as an artifact, you
can reference it directly in your build:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    ...
    <sourceDescriptorDependencies>
      <sourceDescriptorDependency>
        <groupId>org.example</groupId>
        <artifactId>grpc-descriptors</artifactId>
        <version>6.9.420</version>
        <classifier>prod</classifier>
        <type>binpb</type>
      </sourceDescriptorDependency>
    </sourceDescriptorDependencies>
  </configuration>
</plugin>
```

The classifier and type are optional and can be any value. Just like any other dependencies, this
will respect the Maven project `<dependencyManagement/>` if covered.

Note that since descriptor files do **not** have a well-defined magic header, referencing them in
any other inputs, such as `<sourceDirectories/>`, will result in them being ignored. This is due to
a limitation of how the file format is defined.

## Generating proto descriptor files

If you need to generate a `FileDescriptorSet`, you can provide the `outputDescriptorFile`
configuration option. This will output a binary blob containing all the inputs you provided.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
    ...
    <outputDescriptorFile>${project.basedir}/target/protos.desc</outputDescriptorFile>
  </configuration>
</plugin>
```

You can also specify the following boolean options:

- `outputDescriptorIncludeImports` - passes the `--include_imports` flag to `protoc`.
- `outputDescriptorIncludeSourceInfo` - passes the `--include_source_info` flag to `protoc`.
- `outputDescriptorRetainOptions` - passes the `--retain_options` flag to `protoc`.

For more information see [descriptor production](https://protobuf.com/docs/descriptors#descriptor-production).

## Attachment and deploying to a repository

Generated descriptor files can optionally be attached to the Maven build as artifacts. The default 
artifact type is `protobin`. For the `generate-test` goal, the default artifact type is 
`test-protobin`, and the default classifier is `test`.

The following options configure descriptor attachment:

- `outputDescriptorAttached` - attaches the descriptor file to the build.
- `outputDescriptorAttachmentClassifier` - artifact classifier for attached descriptor.
- `outputDescriptorAttachmentType` - artifact type for attached descriptor.
