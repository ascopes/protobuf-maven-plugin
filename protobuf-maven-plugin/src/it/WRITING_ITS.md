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

