package com.github.samblake.yamlrewrite.operation;

import com.github.samblake.yamlrewrite.condition.Condition;
import java.util.Map;

/**
 * Delete operation - removes a key from the YAML structure.
 */
public class DeleteOperation extends ConditionalTransformationOperation {

    public DeleteOperation(String path) {
        super(path);
    }

    public DeleteOperation(String path, Condition condition) {
        super(path, condition);
    }

    @Override
    protected void executeOperation(Map<String, Object> data) {
        YamlPathNavigator.deleteAtPath(data, path);
    }


    @Override
    public String getOperationType() {
        return "delete";
    }


    public String getPath() {
        return path;
    }
}

