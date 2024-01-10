load("@grab_bazel_common//rules/android/lint:lint_aspect.bzl", "lint_aspect")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "AndroidLintInfo")

def _lint_impl(ctx):
    return [
        ctx.attr.target[AndroidLintInfo],  # Forward the provider
    ]

lint = rule(
    implementation = _lint_impl,
    attrs = {
        "target": attr.label(
            doc = "The android_binary or android_library that should be used for implementing linting on",
            aspects = [lint_aspect],
            providers = [
                JavaInfo,
            ],
            mandatory = True,
        ),
    },
)
