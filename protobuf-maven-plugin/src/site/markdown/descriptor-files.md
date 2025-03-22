# Descriptor files

<div id="pmp-toc"></div>

Descriptors are used to encapsulate the information found in one or more `.proto` files into a 
single binary blob that can be passed as an input into `protoc`.

## Generating proto descriptor files

If you need to generate a `FileDescriptorSet` (a protocol buffer, defined in 
[descriptor.proto](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto)),
you can provide an `outputDescriptorFile` configuration option. This will output a binary blob 
containing all the inputs you provided.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  <version>%VERSION%</version>

  <configuration>
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
