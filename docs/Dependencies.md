## Updating dependencies

Whenever any maven dependencies are updated, the pinned artifacts information stored in
`bazel_common_maven_install.json` need to be refreshed. This can be done by running
`bazelisk run -- //tools/maven:maven_pinner --maven_repo=bazel_common_maven` or by running
`Pin Artifacts` IDE configuration that is imported after syncing the project in IntelliJ.