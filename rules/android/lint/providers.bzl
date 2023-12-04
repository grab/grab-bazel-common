AndroidLintNodeInfo = provider(
    doc = "Provider containing data about Android Lint",
    fields = dict(
        name = "Name of the target",
        android = "True for android library or binary",
        library = "True for android library targets",
        enabled = "True if linting was run on this target",
        partial_results_dir = "Lint partial results directory",
        lint_result_xml = "The lint results XML file",
    ),
)

AndroidLintInfo = provider(
    doc = "Provider containing info about lint on this target and it's dependencies",
    fields = dict(
        info = "AndroidLintNodeInfo containing data about Android Lint",
        transitive_nodes = "Depset of AndroidLintNodeInfo containing data about dependencies' lint",
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
