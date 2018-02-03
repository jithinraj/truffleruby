# Docker Test Images

These are Dockerfiles to test TruffleRuby builds on different Linux
distributions. They could also serve as sources on which to base a deployment
image, but we'd recommend the official GraalVM Dockerfile for that.

https://github.com/oracle/docker-images/tree/master/GraalVM

These Dockerfiles run the tests as part of the image build, so if the image
successfully builds then the tests have all passed.

You need to put the built GraalVM binary tarball into the same directory as the
Dockerfile, and update the Dockerfile for the version.

```
$ docker build -t truffleruby-test-ubuntu . --build-arg GRAALVM_VERSION=n.n
```

Also set `--build-arg TEST_BRANCH=master` to pick a branch to test against
if you are testing a pre-release.

You need to specify `GRAALVM_VERSION` as the version of GraalVM that you are
using.

Docker will need to run the container with at least 8 GB of RAM if you are using
virtualisation, to give enough space for the native image to build.

## Ruby Managers

There are also tested by Dockerfiles. These all run on Ubuntu. We test rbenv,
chruby and RVM.

```
$ docker build -t truffleruby-test-rbenv . --build-arg GRAALVM_VERSION=n.n
```
