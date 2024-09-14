# Writing Integration Tests

This directory contains Maven Invoker integration tests that will be executed as a nested Maven
project for each directory within this one. JaCoCo will also be hooked up automatically for you
to capture code coverage.

## Invocation

To run all integration tests, run:

```shell
$ ./mvnw integration-test
```

If you want to run specific tests only, pass the `-Dinvoker.test` flag with a comma-separated
name of each project in this directory you want to run.

```shell
$ ./mvnw integration-test -Dinvoker.test=help,java-simple
```

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
$ ./mvnw verify -DskipTests -Dinvoker.test=path-protoc -Pinvoker-debug
```

This debugger will suspend the invoked IT Maven process until a debugger client connects to the
debugger server. This can be done by using an IDE such as IntelliJ and setting up a
"Remote Debugger" run configuration to connect to port `5005`.

Any breakpoints in the project source code will then be able to be hit and stepped through
individually.

If you wish to enable verbose output from Maven, edit the `invoker-debug.properties` to set
the `invoker.debug` property to `true`.

## Tests that run web servers

Since 2.5.0, all ITs run in parallel in an undefined order, so if you are exposing a web server
or socket anywhere, you need to make sure that it is a unique port. These tests start with port
10000 and increment by 1 each time a new test is added so that ports are kept unique.
