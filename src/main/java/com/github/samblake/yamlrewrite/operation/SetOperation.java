package com.github.samblake.yamlrewrite.operation;

import com.github.samblake.yamlrewrite.condition.Condition;
import java.util.Map;

/**
 * Set operation - sets a value at a path in the YAML structure.
 */
public class SetOperation extends ConditionalTransformationOperation {

    private final Object value;

    public SetOperation(String path, Object value) {
        super(path);
        this.value = value;
    }

    public SetOperation(String path, Object value, Condition condition) {
        super(path, condition);
        this.value = value;
    }

    @Override
    protected void executeOperation(Map<String, Object> data) {
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

