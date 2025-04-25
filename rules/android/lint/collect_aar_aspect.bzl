load("@grab_bazel_common//rules/android:utils.bzl", "utils")
load("@grab_bazel_common//rules/android/lint:providers.bzl", "AarInfo", "AarNodeInfo")

def _collect_aar_aspect(_, ctx):
    transitive_aar_node_infos = depset(
        transitive = [
            aar_node_info.transitive
            for aar_node_info in utils.collect_providers(
                AarNodeInfo,
                getattr(ctx.rule.attr, "deps", []),
                getattr(ctx.rule.attr, "exports", []),
                getattr(ctx.rule.attr, "associates", []),
            )
        ],
    )
    current_info = AarNodeInfo(
        aar = None,
        aar_dir = None,
    )

    if hasattr(ctx.rule.attr, "aar"):  # Check for rule name and only extract if it is aar_import
        aar = ctx.rule.attr.aar.files.to_list()[0]
        aar_extract = ctx.actions.declare_directory(ctx.label.name + "/lint/_extracted_aar")

        ctx.actions.run_shell(
            inputs = [aar],
            outputs = [aar_extract],
            mnemonic = "ExtractLintAar",
            progress_message = "Extracting %s's " % (ctx.label.name),
            command = ("unzip -q -o %s -d %s/ " % (aar.path, aar_extract.path)),
        )
        current_info = AarNodeInfo(
            aar = aar,
            aar_dir = aar_extract,
        )

    return [
        AarInfo(
            self = current_info,
            transitive = depset(
                [current_info],
                transitive = [depset(transitive = [transitive_aar_node_infos])],
            ),
        ),
    ]

collect_aar_aspect = aspect(
    implementation = _collect_aar_aspect,
    attr_aspects = ["aar", "deps", "exports", "associates"],
)
