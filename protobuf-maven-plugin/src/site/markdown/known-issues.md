# Known Issues

While every attempt is made to ensure this plugin is as functional and complete as possible,
some known issues exist.

## Plexus and Maven

Prior to Maven 3.9, a component named `plexus-tools` was included by Apache
Maven at runtime. The component is used by Maven plugin internals. In v3.9.0 of
Maven, this was removed. Some older Maven plugins may still need this dependency
to work. The protobuf-maven-plugin does not require this, but we have observed in
some cases that Maven still attempts to reference this dependency (likely triggered
by the use of Maven 2 plugins that need to be updated by the author). This can
result in the protobuf-maven-plugin crashing with "wiring" errors. See
[this discussion](https://github.com/ascopes/protobuf-maven-plugin/discussions/470)
for details and a potential workaround while this is investigated.

## Incremental compilation edge cases

The experimental incremental compilation feature is known to have some edge cases
where a `mvn clean` is required to rebuild the entire source tree. This is due to
the somewhat coarse way that changes are monitored between builds. If this becomes
overly problematic, please raise an issue on GitHub with details of how to reproduce
the issue you are seeing.

Incremental compilation only currently considers if an entire file has changed. No attempt
is made to parse a dependency graph of all sources to determine whether a change impacts
other files. This may be implemented in the future if the performance overhead is
not too high. For now, it is best to disable incremental compilation, use includes/excludes,
or perform clean builds if you are actively changing protobuf sources.

## Descriptor support

Protobuf descriptors are currently unsupported. Please raise an issue on GitHub if you wish
to request this feature, along with details of your use case.
