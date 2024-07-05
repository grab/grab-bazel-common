load("@rules_detekt//detekt:defs.bzl", _detekt_create_baseline = "detekt_create_baseline", _detekt_test = "detekt_test")
load("@grab_bazel_common//rules/android/lint:utils.bzl", _baseline_validator = "baseline")

def detekt(
        name,
        baseline,
        cfgs,
        srcs,
        parallel,
        all_rules,
        build_upon_default_config,
        disable_default_rule_sets,
        auto_correct,
        detekt_checks):
    #    TODO: Add more documentation
    """Runs detekt on the target

    Run bazelisk test <name>.detekt_test to run Detekt lint on the target and produce the result XML
    Run baselisk run <name>.detekt_update_baseline to update the baseline for use with detekt

    Args:
      name: The name of the detekt target
      baseline: The baseline XML file to compare results against
      cfgs: The config XML file to configure detekt
      srcs: input sources to run detekt on
      plugins: The plugins to run detekt with/ custom detekt rules
      parallel: [https://detekt.dev/docs/gettingstarted/cli/]
      all_rules: [https://detekt.dev/docs/gettingstarted/cli/]
      build_upon_default_config: [https://detekt.dev/docs/gettingstarted/cli/]
      disable_default_rule_sets: [https://detekt.dev/docs/gettingstarted/cli/]
      auto_correct: [https://detekt.dev/docs/gettingstarted/cli/]
      //todo enable class path for detekt
    """

    _detekt_test(
        name = name + ".detekt_test",
        baseline = baseline,
        cfgs = cfgs,
        srcs = srcs,
        parallel = parallel,
        all_rules = all_rules,
        build_upon_default_config = build_upon_default_config,
        disable_default_rule_sets = disable_default_rule_sets,
        auto_correct = auto_correct,
        plugins = detekt_checks,
    )

    _detekt_create_baseline(
        name = name + ".detekt_update_baseline",
        baseline = baseline,
        cfgs = cfgs,
        srcs = srcs,
        parallel = parallel,
        all_rules = all_rules,
        build_upon_default_config = build_upon_default_config,
        disable_default_rule_sets = disable_default_rule_sets,
        auto_correct = auto_correct,
        plugins = detekt_checks,
    )

def detekt_options(
        enabled,
        baseline = None,
        cfgs = [],
        parallel = False,
        all_rules = False,
        build_upon_default_config = False,
        disable_default_rule_sets = False,
        auto_correct = False,
        detekt_checks = []):
    return {
        "enabled": enabled,
        "baseline": baseline,
        "cfgs": cfgs,
        "parallel": parallel,
        "all_rules": all_rules,
        "build_upon_default_config": build_upon_default_config,
        "disable_default_rule_sets": disable_default_rule_sets,
        "auto_correct": auto_correct,
        "detekt_checks": detekt_checks,
    }
