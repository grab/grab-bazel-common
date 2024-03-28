load("@grab_bazel_common//rules/android/lint:providers.bzl", "AarInfo", "AarNodeInfo")

def _collect_aar_aspect(_, ctx):
    deps = getattr(ctx.rule.attr, "deps", [])
    exports = getattr(ctx.rule.attr, "exports", [])
    associates = getattr(ctx.rule.attr, "associates", [])
    transitive_aar_depsets = []
    for dep in deps + exports + associates:
        if AarNodeInfo in dep:
            transitive_aar_depsets.append(dep[AarNodeInfo].aars)

    current_info = AarNodeInfo(
        aar = None,
        aar_dir = None,
    )

    if hasattr(ctx.rule.attr, "aar"):
        aar = ctx.rule.attr.aar.files.to_list()[0]
        aar_extract = ctx.actions.declare_directory("lint/" + ctx.label.name + "_extracted_aar")

        ctx.actions.run_shell(
            inputs = [aar],
            outputs = [aar_extract],
            mnemonic = "ExtractAar",
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
            transitive = depset(transitive_aar_depsets),
        ),
    ]

collect_aar_aspect = aspect(
    implementation = _collect_aar_aspect,
    attr_aspects = ["aar", "deps", "exports", "associates"],
)
