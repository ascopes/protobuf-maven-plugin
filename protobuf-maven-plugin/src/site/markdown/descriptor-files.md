# Descriptor files

Descriptors essentially contain exactly the information found in one or more `.proto` files.

## Generating proto descriptor files

If you need to generate a `FileDescriptorSet` (a protocol buffer, defined in 
[descriptor.proto](https://github.com/protocolbuffers/protobuf/blob/main/src/google/protobuf/descriptor.proto))
containing all the input files you can provide an `outputDescriptorFile` configuration option.

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
