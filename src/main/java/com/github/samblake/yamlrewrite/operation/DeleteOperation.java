package com.github.samblake.yamlrewrite.operation;

import java.util.Map;

/**
 * Delete operation - removes a key from the YAML structure.
 */
public class DeleteOperation implements TransformationOperation {

    private final String path;

    public DeleteOperation(String path) {
        this.path = path;
    }

    @Override
    public void apply(Map<String, Object> data) {
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

