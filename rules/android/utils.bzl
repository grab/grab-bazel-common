def _to_path(f):
    return f.path

def _inspect(obj, name = None):
    if (name != None):
        print("%s : " % name)
    print("fields: %s" % dir(obj))
    print("values: %s" % obj)

utils = struct(
    to_path = _to_path,
    inspect = _inspect,
)
