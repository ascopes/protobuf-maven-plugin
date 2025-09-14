# Writing Integration Tests

This directory contains Maven Invoker integration tests that will be executed as a nested Maven
project for each directory within this one. JaCoCo will also be hooked up automatically for you
to capture code coverage.

## Parent POM

All test POMs inherit the `setup/pom.xml` project which acts as the base for defining common 
versions across all projects. This keeps various concerns simple when updating dependencies.

Note that Windows has very strict file name length limits, and the invoker configuration can result
in paths that breach this limit, causing builds to spuriously fail. To avoid this, on Windows,
we run all invoker tests within the user's temporary directory. Be mindful of this when naming
the test cases.

## Invocation

To run all integration tests, run:

```shell
$ ./mvnw integration-test
```

If you want to run specific tests only, pass the `-Dinvoker.test` flag with a comma-separated
name of each project in this directory you want to run.

```shell
$ ./mvnw integration-test -Dinvoker.test=setup,help,java-simple
```

This should always be called with `setup` as the first project to ensure that the aggregator
test parent POM is installed into the test environment first. Failing to do this will result
in test failures.

## Configuration

Each directory should contain a `pom.xml` and an `invoker.properties`. The `invoker.properties`
can configure how Invoker will run. By default, you want to include the following property:

```properties
invoker.goals = clean package
```

The `invoker.goals` can be changed to use any combination of Maven goals.

The `invoker.properties` within this directory is inherited by all projects implicitly for global
configuration.

## Naming

The project directory name should summarise what the test is for, starting with a GitHub issue
number if relevant.

The `groupId` of the project should be the directory name. This ensures that each test is isolated.

The local repository can be found in the `target/it-repo` directory.

## Skipping tests conditionally

If there are certain conditions where the test should not run, you can create a script within the test
directory named `selector.groovy`. In this script, you can write logic and return `true` or `false`, where
the former allows the test to run, and the latter results in it being skipped.

Omitting this script will result in the test always being run.

## Assertions

A script can be placed within each directory called `test.groovy`. This will be run within a Groovy
interpreter after the Maven goals.

You can use any of the test dependencies or compile dependencies in the Protobuf plugin dependencies
from this script, including AssertJ, JUnit, etc.

In addition, the following identifiers will be made available:

| Identifier            | Type           | Description                                        |
|-----------------------|----------------|----------------------------------------------------|
| `basedir`             | `java.io.File` | The directory the IT project is being called from. |
| `localRepositoryPath` | `java.io.File` | The absolute path to the local Maven repository.   |
| `mavenVersion`        | `String`       | The version of Maven being used.                   |

## Debugging

Enable the `invoker-debug` Maven profile with `./mvnw -Pinvoker-debug ...`. This will enable the
use of the `invoker-debug.properties` in this directory rather than `invoker.properties`.

For example, to debug the plugin while running the `path-protoc` IT case, you could run

```console
$ ./mvnw verify -DskipTests -Dinvoker.test=setup,path-protoc -Pinvoker-debug
```

This debugger will suspend the invoked IT Maven process until a debugger client connects to the
debugger server. This can be done by using an IDE such as IntelliJ and setting up a
"Remote Debugger" run configuration to connect to port `5005`.

Any breakpoints in the project source code will then be able to be hit and stepped through
individually.

If you wish to enable verbose output from Maven, edit the `invoker-debug.properties` to set
the `invoker.debug` property to `true`.

## Using `protoc` from the system $PATH

Activate the `-Pinvoker-path-protoc` profile to force all tests to run using `protoc` on the system `$PATH`.

Note that this is incompatible with the debug profile, so you may need to add the debug flags to the properties
manually.
