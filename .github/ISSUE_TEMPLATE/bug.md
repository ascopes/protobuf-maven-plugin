---
name: Bug report
about: Report an issue
title: "Bug: <enter a title here>"
labels: bug
assignees: 'ascopes'

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behaviour.

**Expected behaviour**
A clear and concise description of what you expected to happen.

**Test case or reproduction**
Please attach a reproduction or code snippet to help us understand what is being performed.

Ideally this should contain relevant configuration and protobuf sources where applicable so that
we can reproduce the issue in our tests.

**Logs**
When you run Maven, invoke it with these additional flags:

```
-e -Dorg.slf4j.simpleLogger.log.io.github.ascopes.protobufmavenplugin=trace
```

Attach the log contents here as an attachment or inside a code block.

**Additional context**

- Protobuf Maven Plugin version: _Please enter the Maven plugin version you are using_
- Maven version: _Please provide the output of `mvn --version`_
- JDK version: _Please enter the JDK you are using here (e.g. 17.0.1)_
- JDK vendor: _Please enter the JDK vendor being used (e.g. Amazon Corretto, Adoptium)_
- Platform: _Please enter details about your platform (e.g. Fedora Linux ARM64, Windows 11 AMD64).
- [ ] Tick if you wish to provide a fix for this issue.
