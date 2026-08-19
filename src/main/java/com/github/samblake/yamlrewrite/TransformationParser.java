package com.github.samblake.yamlrewrite;

import com.github.samblake.yamlrewrite.condition.Condition;
import com.github.samblake.yamlrewrite.condition.StringMatchCondition;
import com.github.samblake.yamlrewrite.operation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 *     when:
 *       type: equals
 *       path: spec.environment
 *       value: dev
 *   - operation: set
 *     path: spec.environment
 *     value: production
 *     when:
 *       type: equals
 *       value: dev
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

            Optional<Condition> condition = parseCondition(transform);
            TransformationOperation op = createOperation(operation, transform, condition);
            operations.add(op);
        }

        return operations;
    }

    /**
     * Parse a condition from the transformation map if present.
     *
     * @param transform the transformation map
     * @return Optional containing the condition, or empty if no condition
     */
    private static Optional<Condition> parseCondition(Map<String, Object> transform) {
        Object whenObj = transform.get("when");
        if (whenObj == null) {
            return Optional.empty();
        }

        if (!(whenObj instanceof Map)) {
            throw new IllegalArgumentException("'when' field must be a map");
        }

        Map<String, Object> whenMap = (Map<String, Object>) whenObj;
        String type = (String) whenMap.get("type");

        if (type == null) {
            throw new IllegalArgumentException("'when' condition must have a 'type' field");
        }

        switch (type) {
            case "equals":
                return parseStringMatchCondition(whenMap);
            default:
                throw new IllegalArgumentException("Unknown condition type: " + type);
        }
    }

    /**
     * Parse an equals condition.
     *
     * @param conditionMap the condition map
     * @return Optional containing the StringMatchCondition
     */
    private static Optional<Condition> parseStringMatchCondition(Map<String, Object> conditionMap) {
        String path = (String) conditionMap.get("path");
        Object valueObj = conditionMap.get("value");

        if (valueObj == null) {
            throw new IllegalArgumentException("equals condition requires 'value' field");
        }

        String value = String.valueOf(valueObj);
        // path can be null/empty to use relative path (operation's path as context)
        return Optional.of(new StringMatchCondition(path, value));
    }

    private static TransformationOperation createOperation(String type, Map<String, Object> config,
                                                           Optional<Condition> condition) {
        switch (type) {
            case "delete":
                return createDeleteOperation(config, condition);
            case "set":
                return createSetOperation(config, condition);
            case "rename":
                return createRenameOperation(config, condition);
            case "merge":
                return createMergeOperation(config, condition);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }

    private static TransformationOperation createDeleteOperation(Map<String, Object> config,
                                                                  Optional<Condition> condition) {
        String path = (String) config.get("path");
        if (path == null) {
            throw new IllegalArgumentException("Delete operation requires 'path' field");
        }
        if (condition.isPresent()) {
            return new DeleteOperation(path, condition.get());
        }
        return new DeleteOperation(path);
    }

    private static TransformationOperation createSetOperation(Map<String, Object> config,
                                                              Optional<Condition> condition) {
        String path = (String) config.get("path");
        Object value = config.get("value");

        if (path == null) {
            throw new IllegalArgumentException("Set operation requires 'path' field");
        }

        if (condition.isPresent()) {
            return new SetOperation(path, value, condition.get());
        }
        return new SetOperation(path, value);
    }

    private static TransformationOperation createRenameOperation(Map<String, Object> config,
                                                                 Optional<Condition> condition) {
        String from = (String) config.get("from");
        String to = (String) config.get("to");

        if (from == null) {
            throw new IllegalArgumentException("Rename operation requires 'from' field");
        }
        if (to == null) {
            throw new IllegalArgumentException("Rename operation requires 'to' field");
        }

        if (condition.isPresent()) {
            return new RenameOperation(from, to, condition.get());
        }
        return new RenameOperation(from, to);
    }

    private static TransformationOperation createMergeOperation(Map<String, Object> config,
                                                                Optional<Condition> condition) {
        String path = (String) config.get("path");
        Object value = config.get("value");

        if (path == null) {
            throw new IllegalArgumentException("Merge operation requires 'path' field");
        }
        if (value == null) {
            throw new IllegalArgumentException("Merge operation requires 'value' field");
        }

        if (condition.isPresent()) {
            return new MergeOperation(path, value, condition.get());
        }
        return new MergeOperation(path, value);
    }
}

