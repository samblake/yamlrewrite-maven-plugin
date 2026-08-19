package com.github.samblake.yamlrewrite.condition;

import com.github.samblake.yamlrewrite.operation.YamlPathNavigator;
import java.util.Map;

/**
 * Condition that checks if a value at a given path equals a specific string.
 *
 * The path can be:
 * - Absolute from the root (e.g., "environment", "metadata.tier")
 * - Relative to an operation's path when using isSatisfied(data, basePath)
 * - Empty/null to check at the basePath (operation's path)
 */
public class StringMatchCondition implements Condition {

    private final String path;  // Can be null/empty for relative-to-operation checking
    private final String value;

    /**
     * Create a condition that checks an absolute path.
     *
     * @param path absolute path from root (e.g., "environment")
     * @param value expected string value to match
     */
    public StringMatchCondition(String path, String value) {
        this.path = path;
        this.value = value;
    }

    /**
     * Check if condition is satisfied (absolute path from root).
     * This is typically called without context.
     */
    @Override
    public boolean isSatisfied(Map<String, Object> data) {
        if (path == null || path.isEmpty()) {
            return false;  // Can't check without a path and no context
        }
        Object current = YamlPathNavigator.getValueAtPath(data, path);
        if (current == null) {
            return false;
        }
        return value.equals(String.valueOf(current));
    }

    /**
     * Check if condition is satisfied with a base path context.
     *
     * If this condition's path is empty/null, checks at the basePath.
     * Otherwise, checks at the absolute path from root.
     *
     * @param data the root YAML map
     * @param basePath the base path context (operation's path)
     * @return true if condition is satisfied
     */
    @Override
    public boolean isSatisfied(Map<String, Object> data, String basePath) {
        String checkPath;

        // If condition path is empty/null, use the operation's base path
        if (path == null || path.isEmpty()) {
            checkPath = basePath;
        } else {
            checkPath = path;
        }

        if (checkPath == null || checkPath.isEmpty()) {
            return false;
        }

        Object current = YamlPathNavigator.getValueAtPath(data, checkPath);
        if (current == null) {
            return false;
        }
        return value.equals(String.valueOf(current));
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }
}


