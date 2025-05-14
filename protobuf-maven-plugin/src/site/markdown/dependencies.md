# Dependencies

<div id="pmp-toc"></div>

Like any other Java project in Maven, your Protobuf project may need to depend on other dependencies
located in a Maven repository or some other location. This has first-class support within this
plugin without you needing to do anything special.

## A simple example

Any Maven dependencies in your project that are a valid ZIP or JAR archive or file tree will be 
scanned during execution to discover any `*.proto` files internally. All proto files will be 
extracted and made visible to `protoc` relative to their file hierarchy.

For example, suppose you are writing a ticketing system, and you are working on the user profiles
functionality. You may have the following project structure:

```plaintext
┣━ pom.xml  (org.example/ticketsystem-users-parent)
┗━ ticketsystem-user-protos/
    ┣━ pom.xml  (org.example/ticketsystem-user-protos)
    ┗━ src/
        ┗━ main/
            ┗━ protobuf/
                ┗━ org/
                    ┗━ example/
                        ┗━ ticketsystem/
                            ┗━ users/
                                ┣━ avatar.proto
                                ┣━ profile.proto
                                ┣━ team.proto
                                ┗━ user.proto      
```

You may have a set of message definitions that depends on proto files from other projects. In our
case, let's say our `team.proto` depends on a `ticket_board.proto` in a totally different Maven
project:

```protobuf
// org/example/ticketsystem/users/team.proto

syntax = "proto3";

package org.example.ticketsystem.users;

option java_multiple_files = true;
option java_package = "org.example.ticketsystem.users";

import "org/example/ticketsystem/board/ticket_board.proto";
import "org/example/ticketsystem/users/avatar.proto";
import "org/example/ticketsystem/users/user.proto";

message Team {
  org.example.ticketsystem.users.Avatar icon = 1;
  string name = 2;
  org.example.ticketsystem.users.User owner = 3;
  org.example.ticketsystem.board.TicketBoard ticket_board = 4;
}
```

Luckily for us, the `ticket_board.proto` is published in our Maven repository in a ZIP file with
the following hierarchy:

```plaintext
┣━ pom.xml  (org.example/ticketsystem-users-parent)
┗━ ticket_board_protos.zip
    ┣━ META_INF/
    ┃   ┗━ .../
    ┗━ org/
        ┗━ example/
            ┗━ ticketsystem/
                ┗━ board/
                    ┗━ ticket_board.proto       
```

We can add a dependency on this in our `pom.xml`, and the protobuf-maven-plugin will automatically
detect it and discover the proto files inside it, enabling our project to build successfully:

```xml
<project>
  ...
  
  <dependencies>
    <dependency>
      <groupId>org.example</groupId>
      <artifactId>ticketsystem-ticket-board</artifactId>
      <version>...</version>
      <classifier>zip</classifier>
      <scope>compile</scope>      
    </dependency>
  </dependencies>
</project>
```

## Maven Dependencies

By default, the plugin will discover all `*.proto` files in all dependencies of the current Maven
project. This occurs transitively, and all are added to the `protoc` import path.

The idea here is that the behaviour for imports should almost exactly mimic how Java dependencies
work and how they are resolved on the classpath.

It is important to note that by default, only dependencies with the `compile`, `provided`, `system`
and `test` (only for test executions) scopes will be inspected.

If you wish to exclude certain transitive dependencies, you can use the existing Maven
exclusion mechanism.

```xml
<dependency>
  <groupId>org.example</groupId>
  <artifactId>ticketsystem-ticket-board</artifactId>
  <version>...</version>
  <classifier>zip</classifier>
  <scope>compile</scope>
  
  <excludes>
    <exclude>
      <groupId>org.example</groupId>
      <artifactId>ticketsystem-beta-user-protos</artifactId>
    </exclude>
  </excludes>
</dependency>
```

This will also respect Maven dependency management.

## Advanced configuration

In addition to scanning Maven dependencies, the plugin also has several advanced settings to enable
you to control the dependencies that are resolved from within the plugin itself.


### Disabling resolution of Maven project dependencies

If you wish to disable discovery of `*.proto` sources from Maven project dependencies, you can
disable this in the plugin configuration:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  
  <configuration>
    <ignoreProjectDependencies>true</ignoreProjectDependencies>
  </configuration>
</plugin>
```

If you do this, only dependencies explicitly configured in the plugin configuration will be
considered.

### Adding additional import paths from the local file system

If you wish to make `*.proto` files from the local file system visible to `protoc`, you can add
their directory roots to the plugin configuration:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  
  <configuration>
    <importPaths>
      <importPath>/path/to/protos/root</importPath>
      ...
    </importPaths>
  </configuration>
</plugin>
```

In this example, `/path/to/protos/root` will be recursively searched for `*.proto` sources
and added to the import path.

You can add as many of these as you like to your project.

### Adding Maven artifact dependencies within the plugin configuration

If you wish to keep your protobuf dependencies out of your main project configuration, you can
configure them within the plugin configuration itself. Ideally you should use the Maven project
dependency configuration for this, but regardless, this feature is provided in case you have any
unique use cases for it.

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>

  <configuration>
    <importDependencies>
      <importDependency>
        <groupId>org.example</groupId>
        <artifactId>ticketsystem-ticket-board</artifactId>
        <version>...</version>
        <classifier>zip</classifier>
        <scope>compile</scope>
      
        <exclusions>
          <exclusion>
            <groupId>org.example</groupId>
            <artifactId>ticketsystem-beta-user-protos</artifactId>
          </exclusion>
        </exclusions>
      </importDependency>
    </importDependencies>
  </configuration>
</plugin>
```

### Compiling dependencies

If you wish to also _compile_ your dependencies, you can specify them in the `sourceDependencies`
and `sourceDirectories` configuration parameters, depending on whether they are Maven artifacts
or paths on the local file system:

```xml
<plugin>
  <groupId>io.github.ascopes</groupId>
  <artifactId>protobuf-maven-plugin</artifactId>
  
  <configuration>
    <sourceDependencies>
      <sourceDependency>
        <groupId>org.example.foobar</groupId>
        <artifactId>core-protos</artifactId>
        <version>1.2.3</version>
        <type>zip</type>
      </sourceDependency>
    </sourceDependencies>
    
    <sourceDirectories>
      <!-- Keep the default path -->
      <sourceDirectory>${project.basedir}/src/protobuf</sourceDirectory>

      <!-- Add something else from the local system -->
      <sourceDirectory>/path/to/something/else/to/include</sourceDirectory>
    </sourceDirectories>
  </configuration>
</plugin>
```

### Dependency management

The `protobuf-maven-plugin` will respect any `dependencyManagement` blocks in the current or 
parent project, allowing you to infer versions from a parent POM across numerous projects in your
team.

This is already valid if you use the `dependencies` block within your pom.xml, but will also apply
to `importDependencies` and `sourceDependencies` blocks as well.

```xml
<project>
  ...
  
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.30.0</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <plugin>
    <groupId>io.github.ascopes</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
  
    <configuration>
      ...
      
      <importDependencies>
        <importDependency>
          <groupId>com.google.protobuf</groupId>
          <artifactId>protobuf-java</artifactId>
          <!-- The version here is inferred from <dependencyManagement/>! -->
        </importDependency>
      </importDependencies>
    </configuration>
  </plugin>
</project>
```

Note that in the event of duplicate dependencies, the newer dependency version will be
taken.
