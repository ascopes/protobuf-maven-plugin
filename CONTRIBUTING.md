# Contributing

So you've decided to contribute to this project? That's awesome. This page documents some info
to help you.

Any bug reports, feature requests, contributions, or feedback are greatly appreciated.

## Reporting bugs with protoc or protobuf

I am not affiliated with either of these projects. Please raise your bug with Google via their
issue tracker!

## Reporting bugs with the plugin

If you wish to raise a bug, please do this via the "Issues" page.

Make sure you provide full details of what you are trying to do and why. Include details of
what you expect to happen, versus what actually happens. Run `mvn` or `mvnw` in your project
with the `--errors` flag to get full exception stacktraces.

If possible and appropriate, try to show `protoc` being called directly and producing the output you
expect.

Please include the name of your OS (`uname -o`), the CPU architecture (`uname -m`), the version
of Maven in use (`mvn --version` or `./mvnw --version`), and the version of Java in use
(`java -version`).

Where possible and appropriate, please try to include a minimal working reproduction
of the issue you are reporting. This can just be a simple Maven project in a `*.zip` or
`*.tar.gz` file if that is easiest.

If this turns out to be a bug that needs a code change to fix, I may ask if I can use your
reproduction as an integration test rather than writing one myself. If you allow this, be aware that
the reproduction will be included in this project with the current project license.

## Requesting new features

New features can be requested via the "Issues" page.

Please include the following details:

- What you want to do
- How you expect it to do it
- What have you tried as an alternative
- What impact does this feature have
- Examples of what you'd expect to see in the feature.

If this relates to invocation semantics of certain Protoc plugins, please try to include info about
them so I have something to test with.

## Requesting help or providing general feedback

The best place to request help is in the "Discussions" page. Remember to try and [phrase your question
in a clear way](https://www.freecodecamp.org/news/how-to-ask-good-questions-as-a-developer-9f71ff809b63/).

## Contributing changes to the codebase

If you have decided to contribute changes, then that is great and always welcome.

For stuff like documentation changes and typo fixes, you can just raise a PR directly.

If the change is a little more complex, please raise an issue first and request that you are assigned
to the issue.

### Working with this project

#### Maven Wrapper

This project uses the Maven Wrapper, which automatically installs and uses the correct version of Maven
for this project. This means you should use `./mvnw` or `.\mvnw.cmd` instead of calling `mvn` directly.

#### License headers

All files in this project need license headers. You can add these by running
`./mvnw license:format`. If you do not add these to new files, the build will fail.

#### Checkstyle

Code style is enforced via Checkstyle. You can validate this using `./mvnw validate`. Builds will fail
if any issues are discovered.

#### Versions

This project uses semantic versioning!

If you need to add a new user-facing feature (i.e. something that is accessible via the interface used in
a `pom.xml` when using this plugin), you should bump the minor version of this plugin.

If you are making a breaking API change, you need to bump the major version.

I'll advise about this during PR reviews as needed, but you can use the `scripts/bump-versions.sh` script
to do this for you. It will not change anything if the versions are already set up correctly.

#### Making sure it builds

Before raising a PR, please make sure your fork works by running `./mvnw clean verify` locally.

#### Commit hygiene

Please avoid creating merge commits from the main branch to your branch. If you need to pull in upstream
changes, please use `git rebase` instead. It keeps the project history linear and readable.

#### Raising the PR

In the PR description, please document what you changed and what impact it has.

If it fixes a bug, please write `Fixes GH-xxx.` as the last line of the PR description, where XXX is
the issue number for this repo. This will make GitHub link the issue and automatically close it.

Once I see the PR, I can approve it to run the CI pipeline to verify it builds on various platforms.

