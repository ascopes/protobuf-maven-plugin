# Other OS support

Not every OS has an official Java protocol buffers compiler (`protoc`) binary deployed to Maven Central. This plugin does however support detecting some of these OSes. For these OSes it still tries to download the `protoc` compiler the same way it would do for a supported OS. It is up to you to deploy the `protoc` binary for this unsupported OS to your local Maven repository.

This is a list of OSes currently supported by this plugin which, do not have an official `protoc` binary deployed in Maven Central:

* illumos / Solaris

## Building protoc for illumos / Solaris

Building the `protoc` binary for illumos and Solaris requires access to a machine or VM running illumos or Solaris. The following steps show how to build the `protoc` binary version 35.1 and how to deploy it to your own local Maven repository in such a way this plugin can automatically use it. This example uses a SmartOS (illumos) native zone with pkgsrc, but the instructions should be similar for other illumos distributions or Solaris.

Install the required tooling for building protoc:

```shell
$ pkgin install git cmake gmake gcc13 openjdk21
```

Prepare the source tree for building version 35.1:

```shell
$ git clone https://github.com/protocolbuffers/protobuf.git
$ cd protobuf/
$ git checkout v35.1
$ git submodule update --init --recursive
```

Build the Protobuf compiler:

```shell
$ rm -rf build CMakeCache.txt CMakeFiles
$ cmake . -DCMAKE_CXX_STANDARD=17 \
-DCMAKE_INSTALL_PREFIX=/opt/local \
-Dprotobuf_BUILD_TESTS=OFF \
-Dprotobuf_BUILD_LIBPROTOC=ON \
-Dprotobuf_BUILD_PROTOC_BINARIES=ON \
-Dprotobuf_BUILD_PROTOBUF_BINARIES=ON \
-Dprotobuf_FORCE_FETCH_DEPENDENCIES=ON
$ cmake --build . -j4
```

Remove the debug symbols (using `strip`) from the resulting executable and correct its name:

```shell
$ strip ./protoc
$ mv ./protoc ~/protoc-4.35.1-sunos-x86_64.exe
```

You can now deploy the `protoc-4.35.1-sunos-x86_64.exe` (`protoc`) binary to your local Maven repository. 

## Deploying a protoc binary to a repository

Once you have build a `protoc` binary for an unsupported platform, you can deploy the binary to your Maven repository. This example shows how to deploy a `protoc` binary built for illumos to a local Maven repository in such a way that this plugin will automatically find it:

```shell
$ mvn deploy:deploy-file \
-DrepositoryId=third-party \
-Durl=https://reposilite.example.com/third-party/ \
-Dfile="protoc-4.35.1-sunos-x86_64.exe" \
-DgroupId="com.google.protobuf" \
-DartifactId="protoc" \
-Dversion="4.35.1" \
-Dpackaging=exe \
-Dclassifier="sunos-x86_64" \
-DcreateChecksum=true
```
