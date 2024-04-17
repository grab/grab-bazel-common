AndroidLintNodeInfo = provider(
    doc = "Provider containing info about a target's Android Lint data",
    fields = dict(
        name = "Name of the target",
        android = "True for android library or binary",
        library = "True for android library targets",
        enabled = "True if linting was run on this target",
        result_code = "File containing result code of lint run",
        partial_results_dir = "Lint partial results directory",
        models_dir = "Lint models directory",
        updated_baseline = "The updated baseline XML",
        lint_result_xml = "The lint results XML file",
        lint_junit_xml = "Lint result formatted in Junit format",
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
        aar_deps = "direct and transitive aars AarInfo",
        manifest = "Android manifest file",
        baseline = "Lint baseline XML",
        lint_config = "Lint config XML",
        lint_checks = "Custom Lint Targets",
        fail_on_warning = "fail on Lint issues with warning severity",
        fail_on_information = "fail on Lint issues with information severity",
    ),
)

AarNodeInfo = provider(
    "A provider to collect aar info of the current target",
    fields = {
        "aar": "aar path",
        "aar_dir": "aar extracrted path",
    },
)

AarInfo = provider(
    "A provider to collect all aars from transitive dependencies",
    fields = {
        "self": "AarNodeInfo",
        "transitive": "depset(AarNodeInfo)",
    },
)

LINT_ENABLED = "lint_enabled"
