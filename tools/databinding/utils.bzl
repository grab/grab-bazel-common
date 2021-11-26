def copy_file_action(ctx, in_file, out_file, file_type = ""):
    """
    Registers simple action that copies in_file to out_file

    Args:
        ctx: The rule context
        in_file: Source to be copied
        out_file: Destination where the file will be copied to.
        file_type: Metadata about the file that will be printed in progress message.
    """
    ctx.actions.run_shell(
        inputs = [in_file],
        outputs = [out_file],
        progress_message = "Copying %s %s" % (file_type, in_file.short_path),
        command = "cp %s %s" % (in_file.path, out_file.path),
    )

def extract_binding_classes(ctx, class_info_zip_file, binding_classes_json_file):
    """
    Registers action to extract binding_class json files from class_info_zip_file.
    """
    ctx.actions.run_shell(
        inputs = [class_info_zip_file],
        outputs = [binding_classes_json_file],
        progress_message = "Extracing binding classes %s" % binding_classes_json_file.basename,
        command = """
        unzip -q -d . {class_info_zip_file} {binding_class_json_name}
        cp {binding_class_json_name} {binding_class_json_file}
        """.format(
            class_info_zip_file = class_info_zip_file.path,
            binding_class_json_name = binding_classes_json_file.basename,
            binding_class_json_file = binding_classes_json_file.path,
        ),
    )
