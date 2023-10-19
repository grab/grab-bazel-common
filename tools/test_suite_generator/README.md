# Test suite generator

## Intro

This annotation processor is used to auto-generate the TestSuite class, a test suite containing all test classes from the input source set.
The TestSuite class is used as the entry point to run all unit tests with a single Bazel target.

### In detail

For example, you have a bunch of test classes

```kotlin
class DummyTestClassA {
    @Test
    fun testFeatureA() {}
}

class DummyTestClassB {
    @Test
    fun testFeatureB() {}
}
```

The generated TestSuite class will look like

```java
@RunWith(Suite.class)
@Suite.SuiteClasses({
    DummyTestClassA.class,
    DummyTestClassB.class
})
public class TestSuite {
}
```

### How to use:

You simply add the annotation processor as a dependency for your target. Below is an example

```kotlin
kt_jvm_test(
    name = "your-test-target",
    srcs = glob([
        "..."
    ]),
    test_class = "com.grazel.generated.TestSuite", // Be aware that the package name is hardcoded
    deps = [
        "@grab_bazel_common//tools/test-info-processor:test-suite-generator",
        "@com_github_jetbrains_kotlin//:kotlin-test",
        "@maven//:junit_junit",
    ],
)
```
