def mock_android_jar(name = "mock_android_jar"):
    """
    Create an mockable version of Android SDK Jar.

    This is done to mimic Android Gradle Plugin's mocked Android Jar feature for tests
    https://developer.android.com/training/testing/unit-testing/local-unit-tests#mocking-dependencies
    All android framework methods will return default and null values when invoked at test runtime.

    Usage:
    Call mock_android_jar() in desired BUILD file and add a dependency on
    @grab_bazel_common//tools/test:mockable-android-jar to use android classes in unit tests

    """
    mock_jar_generator = "@grab_bazel_common//tools/android_mock:mocked_android_jar_generator"
    android_jar = "@androidsdk//:platforms/android-36/android.jar"
    android_mock_jar = "android_mock.jar"

    native.genrule(
        name = "mock_android_jar_generator",
        srcs = [android_jar],
        outs = [
            android_mock_jar,
        ],
        tools = [mock_jar_generator],
        message = "Generating mocked android.jar",
        toolchains = ["@bazel_tools//tools/jdk:current_java_runtime"],
        cmd = """
            $(location {mock_jar_generator}) \
            --input-jar $< \
            --output-jar $@
            """.format(
            android_jar = android_jar,
            mock_jar_generator = mock_jar_generator,
            android_mock_jar = android_mock_jar,
        ),
    )
    native.java_import(
        name = name,
        visibility = [
            "//visibility:public",
        ],
        jars = [
            android_mock_jar,
        ],
    )
