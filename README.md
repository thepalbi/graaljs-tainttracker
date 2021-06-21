# Simple Tool
A simple demonstration code coverage tool built using Truffle for GraalVM.

The source code is documented to explain the how and why of writing a Truffle
tool. A good way to find out more is to read the source with comments. We also
like to encourage people to clone the repository and start hacking.

This repository is licensed under the permissive UPL licence. Fork it to begin
your own Truffle tool.

For instructions on how to get started please refer to [our website](https://www.graalvm.org/docs/graalvm-as-a-platform/implement-instrument/)

## Development notes
- [JVM being used: Graal JVM CE Build 21.1, Java 11 based](https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.1.0/graalvm-ce-java11-darwin-amd64-21.1.0.tar.gz)

## Integration Tests
- Simple additional sink discovery
- Simple additional sink discovery with two new sinks, and taint merging in the middle
