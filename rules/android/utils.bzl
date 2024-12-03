def _to_path(f):
    return f.path

def _inspect(obj, name = None):
    if (name != None):
        print("%s : " % name)
    print("type: %s" % type(obj))
    print("fields: %s" % dir(obj))
    print("values: %s" % obj)

def _collect_providers(provider_type, *all_deps):
    providers = []
    for deps in all_deps:
        for dep in deps:
            if provider_type in dep:
                providers.append(dep[provider_type])
    return providers

def _to_depset(list):
    return depset(list)

def _to_set(items):
    value_dict = {}
    for item in items:
        value_dict[item] = item
    return list(value_dict.values())

utils = struct(
    to_path = _to_path,
    inspect = _inspect,
    collect_providers = _collect_providers,
    to_depset = _to_depset,
    to_set = _to_set,
)
