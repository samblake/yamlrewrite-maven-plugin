package com.github.samblake.yamlrewrite.operation;

import java.util.Map;

/**
 * Set operation - sets a value at a path in the YAML structure.
 */
public class SetOperation implements TransformationOperation {

    private final String path;
    private final Object value;

    public SetOperation(String path, Object value) {
        this.path = path;
        this.value = value;
    }

    @Override
    public void apply(Map<String, Object> data) {
        YamlPathNavigator.setValueAtPath(data, path, value);
    }

    @Override
    public String getOperationType() {
        return "set";
    }

    public String getPath() {
        return path;
    }

    public Object getValue() {
        return value;
    }
}

