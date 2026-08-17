package com.github.samblake.yamlrewrite;

import com.github.samblake.yamlrewrite.operation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parser for transformation specifications defined in YAML format.
 *
 * Expected YAML format:
 * transformations:
 *   - operation: delete
 *     path: spec.oldField
 *   - operation: set
 *     path: spec.replicas
 *     value: 3
 *   - operation: rename
 *     from: spec.oldName
 *     to: spec.newName
 */
public class TransformationParser {

    /**
     * Parse a transformation specification from the given map.
     *
     * @param spec the transformation specification (root map from YAML)
     * @return a list of transformation operations
     * @throws IllegalArgumentException if the specification format is invalid
     */
    public static List<TransformationOperation> parse(Map<String, Object> spec) {
        List<TransformationOperation> operations = new ArrayList<>();

        Object transformationsObj = spec.get("transformations");
        if (transformationsObj == null) {
            return operations;
        }

        if (!(transformationsObj instanceof List)) {
            throw new IllegalArgumentException("'transformations' must be a list");
        }

        List<?> transformations = (List<?>) transformationsObj;

        for (Object item : transformations) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Each transformation must be a map");
            }

            Map<String, Object> transform = (Map<String, Object>) item;
            String operation = (String) transform.get("operation");

            if (operation == null) {
                throw new IllegalArgumentException("Each transformation must have an 'operation' field");
            }

            TransformationOperation op = createOperation(operation, transform);
            operations.add(op);
        }

        return operations;
    }

    private static TransformationOperation createOperation(String type, Map<String, Object> config) {
        switch (type) {
            case "delete":
                return createDeleteOperation(config);
            case "set":
                return createSetOperation(config);
            case "rename":
                return createRenameOperation(config);
            case "merge":
                return createMergeOperation(config);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }

    private static TransformationOperation createDeleteOperation(Map<String, Object> config) {
        String path = (String) config.get("path");
        if (path == null) {
            throw new IllegalArgumentException("Delete operation requires 'path' field");
        }
        return new DeleteOperation(path);
    }

    private static TransformationOperation createSetOperation(Map<String, Object> config) {
        String path = (String) config.get("path");
        Object value = config.get("value");

        if (path == null) {
            throw new IllegalArgumentException("Set operation requires 'path' field");
        }

        return new SetOperation(path, value);
    }

    private static TransformationOperation createRenameOperation(Map<String, Object> config) {
        String from = (String) config.get("from");
        String to = (String) config.get("to");

        if (from == null) {
            throw new IllegalArgumentException("Rename operation requires 'from' field");
        }
        if (to == null) {
            throw new IllegalArgumentException("Rename operation requires 'to' field");
        }

        return new RenameOperation(from, to);
    }

    private static TransformationOperation createMergeOperation(Map<String, Object> config) {
        String path = (String) config.get("path");
        Object value = config.get("value");

        if (path == null) {
            throw new IllegalArgumentException("Merge operation requires 'path' field");
        }
        if (value == null) {
            throw new IllegalArgumentException("Merge operation requires 'value' field");
        }

        return new MergeOperation(path, value);
    }
}

