package com.github.samblake.yamlrewrite.operation;

import com.github.samblake.yamlrewrite.condition.Condition;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Merge operation - recursively merges a value into an existing structure at a path.
 * If the path doesn't exist, it will be created.
 * For map values, performs a deep merge (nested maps are merged recursively).
 * For other values, the new value overwrites the existing one.
 */
public class MergeOperation extends ConditionalTransformationOperation {

    private final Object value;

    public MergeOperation(String path, Object value) {
        super(path);
        this.value = value;
    }

    public MergeOperation(String path, Object value, Condition condition) {
        super(path, condition);
        this.value = value;
    }

    @Override
    protected void executeOperation(Map<String, Object> data) {
        if (!(value instanceof Map)) {
            // If value is not a map, just set it (no merge possible)
            YamlPathNavigator.setValueAtPath(data, path, value);
            return;
        }

        Object existing = YamlPathNavigator.getValueAtPath(data, path);

        if (existing == null) {
            // Path doesn't exist, just set the value
            YamlPathNavigator.setValueAtPath(data, path, deepCopyMap((Map<String, Object>) value));
        }
        else if (existing instanceof Map) {
            // Both are maps, perform deep merge
            Map<String, Object> existingMap = (Map<String, Object>) existing;
            Map<String, Object> mergeValue = (Map<String, Object>) value;
            deepMerge(existingMap, mergeValue);
        }
        else {
            // Existing value is not a map, overwrite it
            YamlPathNavigator.setValueAtPath(data, path, deepCopyMap((Map<String, Object>) value));
        }
    }

    @Override
    public String getOperationType() {
        return "merge";
    }


    public String getPath() {
        return path;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Deep merge source into target.
     * For each key in source:
     * - If target doesn't have the key, add it
     * - If both have the key and value is a map, recursively merge
     * - Otherwise, overwrite target's value with source's value
     *
     * @param target the map to merge into
     * @param source the map to merge from
     */
    private void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();

            if (target.containsKey(key)) {
                Object targetValue = target.get(key);

                // If both are maps, recursively merge
                if (targetValue instanceof Map && sourceValue instanceof Map) {
                    deepMerge((Map<String, Object>) targetValue, (Map<String, Object>) sourceValue);
                } else {
                    // Otherwise, overwrite with source value (deep copy if it's a map)
                    target.put(key, deepCopyValue(sourceValue));
                }
            } else {
                // Key doesn't exist in target, add it (deep copy if it's a map)
                target.put(key, deepCopyValue(sourceValue));
            }
        }
    }

    /**
     * Deep copy a value. If it's a map, recursively copy it.
     * Otherwise, return the value as-is.
     *
     * @param value the value to copy
     * @return a deep copy of the value
     */
    private Object deepCopyValue(Object value) {
        if (value instanceof Map) {
            return deepCopyMap((Map<String, Object>) value);
        }
        // For other types (strings, numbers, booleans, lists, etc.),
        // return as-is (YAML operations typically don't mutate these)
        return value;
    }

    /**
     * Deep copy a map recursively.
     *
     * @param map the map to copy
     * @return a deep copy of the map
     */
    private Map<String, Object> deepCopyMap(Map<String, Object> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            copy.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }
}

