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
