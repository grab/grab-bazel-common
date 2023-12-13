def baseline(lint_baseline):
    """
    Return None if no baseline exists at the provided path and return path if it exists
    """
    if lint_baseline != None and len(native.glob([lint_baseline])) == 0:
        return None
    return lint_baseline
