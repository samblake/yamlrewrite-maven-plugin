package com.github.samblake.yamlrewrite.operation;

import com.github.samblake.yamlrewrite.condition.Condition;
import java.util.Map;

/**
 * Rename operation - renames a key in the YAML structure.
 */
public class RenameOperation extends ConditionalTransformationOperation {

    private final String newPath;

    public RenameOperation(String oldPath, String newPath) {
        super(oldPath);
        this.newPath = newPath;
    }

    public RenameOperation(String oldPath, String newPath, Condition condition) {
        super(oldPath, condition);
        this.newPath = newPath;
    }

    @Override
    protected void executeOperation(Map<String, Object> data) {
        Object value = YamlPathNavigator.getValueAtPath(data, path);
        if (value != null) {
            YamlPathNavigator.setValueAtPath(data, newPath, value);
            YamlPathNavigator.deleteAtPath(data, path);
        }
    }

    @Override
    public String getOperationType() {
        return "rename";
    }

}

