AndroidLintInfo = provider(
    doc = "Provider to pass information about AndroidLint",
    fields = dict(
        name = "Name of the target",
        android = "True for android library or binary",
        library = "True for android library targets",
        enabled = "True if linting was run on this target",
        partial_results_dir = "Lint partial results directory",
        transitive_partial_results_dirs = "Depset of transitive partial results",
        lint_result_xml = "The lint results XML file",
    ),
)

AndroidLintSourcesInfo = provider(
    doc = "Provider to pass sources for Android Lint",
    fields = dict(
        name = "Name of target",
        srcs = "Java/Kotlin sources",
        resources = "Android resources",
        manifest = "Android manifest file",
        baseline = "Lint baseline XML",
        lint_config = "Lint config XML",
    ),
)
