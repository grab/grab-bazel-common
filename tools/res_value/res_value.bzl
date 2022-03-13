def _res_statement(items):
    statements = ""
    for variable_name, value in items.items():
        statements += """
        <string name=\\"{}\\">{}</string>""".format(variable_name, value)
    return statements

def _generate_xml(items):
    statements = """<?xml version=\\"1.0\\" encoding=\\"utf-8\\"?>
<resources>{}
</resources>
    """
    return statements.format(items)

def res_value(
        name,
        strings = {}):
    """Generates an resources xml with the given strings.

    Usage:
    Provide the string variables in the string dictionary and add this target in resource_files of a target.

    Args:
        name: name for this target
        strings: string values
    """

    resources = "src/main/res/values/gen_strings.xml"
    statements = _res_statement(strings)
    xml = _generate_xml(statements)

    cmd = """echo "{xml}" > $@""".format(xml = xml)

    native.genrule(
        name = "_" + name,
        outs = [resources],
        message = "Generating %s's resources" % (native.package_name()),
        cmd = cmd,
    )
    return [resources]
