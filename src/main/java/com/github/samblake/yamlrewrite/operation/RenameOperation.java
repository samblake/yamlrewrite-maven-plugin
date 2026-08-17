package com.github.samblake.yamlrewrite.operation;

import java.util.Map;

/**
 * Rename operation - renames a key in the YAML structure.
 */
public class RenameOperation implements TransformationOperation {

    private final String oldPath;
    private final String newPath;

    public RenameOperation(String oldPath, String newPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
    }

    @Override
    public void apply(Map<String, Object> data) {
        Object value = YamlPathNavigator.getValueAtPath(data, oldPath);
        if (value != null) {
            YamlPathNavigator.setValueAtPath(data, newPath, value);
            YamlPathNavigator.deleteAtPath(data, oldPath);
        }
    }

    @Override
    public String getOperationType() {
        return "rename";
    }

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }
}

