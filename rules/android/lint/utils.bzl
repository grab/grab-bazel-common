def baseline(lint_baseline):
    """
    Return None if no baseline exists at the provided path and return path if it exists
    """
    if lint_baseline != None:
        results = native.glob(include = [lint_baseline])
        if results and len(results) != 0:
            return lint_baseline
        else:
            return None
    return None
