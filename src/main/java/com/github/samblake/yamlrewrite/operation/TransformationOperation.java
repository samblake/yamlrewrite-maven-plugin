package com.github.samblake.yamlrewrite.operation;

import java.util.Map;

/**
 * Interface for YAML transformation operations.
 */
public interface TransformationOperation {
    /**
     * Apply the transformation to the given YAML data structure.
     *
     * @param data the root YAML map to transform
     */
    void apply(Map<String, Object> data);

    /**
     * Get the operation type (e.g., "delete", "set", "rename", "move").
     *
     * @return the operation type
     */
    String getOperationType();
}

