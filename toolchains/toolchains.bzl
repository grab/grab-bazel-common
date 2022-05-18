def configure_toolchains():
    native.config_setting(
        name = "non_transitive_r_class",
        values = {"define": "nontransitive_r_class=1"},
    )
