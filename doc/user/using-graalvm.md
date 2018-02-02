# Using TruffleRuby with GraalVM

TruffleRuby is designed to be run with a JVM that has the Graal compiler.

The easiest way to get a JVM with Graal is via the GraalVM, available from the
Oracle Technology Network. This includes the JVM, the Graal compiler, and
TruffleRuby, all in one package with compatible versions. You normally want
the version that includes the Labs JDK.

http://www.oracle.com/technetwork/oracle-labs/program-languages/

Inside the GraalVM is a `jre/languages/ruby` directory which has the usual
structure of a Ruby implementation. It is recommended to add this directory to
a Ruby manager, see [Configuring Ruby managers](ruby-managers.md) for more
information.

You can use `gem` to install Gems as normal. We recommend that Bundler `1.14.x`
is best to use for now.

By default, GraalVM runs TruffleRuby on a JVM, but it can also run it on top
of the [svm.md](SubstrateVM) for faster startup-time but slower peak-performance.
This is enabled with the `--native` option.

You can also use GraalVM to run a different version of TruffleRuby than the one
it packages, but this not advised for end-users.
