load("rules_android_ndk//:rules.bzl", "android_ndk_repository")

def android_ndk_setup(name = "androidndk", api_level = None):
    kwargs = {"name" : name}
    if api_level != None:
        kwargs["api_level"] = api_level
    android_ndk_repository(**kwargs)