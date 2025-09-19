# Security Policy

This plugin is designed to be used from trusted environments where inputs
and dependencies are known to not be malicious. External dependencies
including `protoc` itself are not covered by the policy in this file.

## Supported Versions

Security updates for this plugin will be applied on top of the
latest released version unless an issue arises such that it explicitly
needs to be backported to older versions. If this is a requirement, 
please raise an issue, along with details of why you are unable to use
the newer versions.

### Maven 4 support

Maven 3 and 4 are supported by this plugin. Many of the underlying Maven libraries
that this project depends on will be declared with runtime linkage. This means
that security updates in terms of Apache Maven are out of scope for this project
unless they are explicitly included with `compile` scope, and are able to be
updated without sacrificing Maven 3 support.

Note that some dependencies must be kept in lockstep with Apache Maven. In the event that this
is a problem, please reach out via an issue.

## Reporting a Vulnerability

Please raise an issue with the details and reproduction if appropriate.

If the issue is with `protoc` or another dependency, then any issues need
to be raised with the associated projects. This project is not affiliated
with any of the projects it depends upon, unless explicitly stated via relevant license documentation.
