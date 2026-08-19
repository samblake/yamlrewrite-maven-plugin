package com.github.samblake.yamlrewrite.operation;

import java.util.*;

import static java.lang.String.join;
import static java.util.Arrays.copyOf;

/**
 * Utility class for navigating YAML structures using dot notation paths.
 * Supports paths like "spec.replicas" to access nested keys.
 * Also supports wildcard patterns like "paths.*.get" to match all keys at a level.
 */
public class YamlPathNavigator {

    /**
     * Navigate to a value in the YAML structure using dot notation.
     * Returns null if the path doesn't exist.
     *
     * @param data the root map
     * @param path dot-separated path (e.g., "spec.replicas.count")
     * @return the value at the path, or null if not found
     */
    public static Object getValueAtPath(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(key);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Set a value at the specified path, creating intermediate maps as needed.
     *
     * @param data the root map
     * @param path dot-separated path (e.g., "spec.replicas")
     * @param value the value to set
     */
    public static void setValueAtPath(Map<String, Object> data, String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            Object next = current.get(key);

            if (!(next instanceof Map)) {
                next = new java.util.LinkedHashMap<>();
                current.put(key, next);
            }

            current = (Map<String, Object>) next;
        }

        current.put(keys[keys.length - 1], value);
    }

    /**
     * Delete the value at the specified path.
     *
     * @param data the root map
     * @param path dot-separated path
     * @return true if something was deleted, false if path didn't exist
     */
    public static boolean deleteAtPath(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];
            Object next = current.get(key);

            if (!(next instanceof Map)) {
                return false;
            }

            current = (Map<String, Object>) next;
        }

        return current.remove(keys[keys.length - 1]) != null;
    }

    /**
     * Get the parent map and the final key for a path.
     * Useful for operations that need to access both parent and key.
     *
     * @param data the root map
     * @param path dot-separated path
     * @return array of [parentMap, finalKey], or null if parent doesn't exist
     */
    public static Object[] getParentAndKey(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        if (keys.length == 0) {
            return null;
        }

        if (keys.length == 1) {
            return new Object[]{data, keys[0]};
        }

        String parentPath = join(".", copyOf(keys, keys.length - 1));
        Object parent = getValueAtPath(data, parentPath);

        if (!(parent instanceof Map)) {
            return null;
        }

        return new Object[]{parent, keys[keys.length - 1]};
    }

    /**
     * Check if a path contains a wildcard pattern (*).
     *
     * @param path the dot-separated path to check
     * @return true if the path contains wildcards
     */
    public static boolean isWildcardPath(String path) {
        return path != null && path.contains("*");
    }

    /**
     * Find all paths matching a wildcard pattern.
     * For example, "paths.*.get" would match "paths.a.get" and "paths.b.get".
     *
     * @param data the root map
     * @param pattern dot-separated pattern with wildcards (e.g., "paths.*.get")
     * @return list of matching paths (without wildcards)
     */
    public static List<String> findMatchingPaths(Map<String, Object> data, String pattern) {
        List<String> matches = new ArrayList<>();
        findMatchingPathsRecursive(data, pattern.split("\\."), 0, "", matches);
        return matches;
    }

    /**
     * Recursively find all paths matching a pattern.
     *
     * @param current current object being traversed
     * @param patternParts the pattern split by dots
     * @param partIndex current index in pattern parts
     * @param currentPath the path built so far
     * @param matches list to accumulate matching paths
     */
    private static void findMatchingPathsRecursive(Object current, String[] patternParts, int partIndex,
                                                   String currentPath, List<String> matches) {
        if (partIndex >= patternParts.length) {
            if (!currentPath.isEmpty()) {
                matches.add(currentPath);
            }
            return;
        }

        if (!(current instanceof Map)) {
            return;
        }

        String patternPart = patternParts[partIndex];
        Map<String, Object> currentMap = (Map<String, Object>) current;

        if ("*".equals(patternPart)) {
            // Wildcard: match all keys
            for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
                String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                findMatchingPathsRecursive(entry.getValue(), patternParts, partIndex + 1, newPath, matches);
            }
        } else {
            // Exact match
            Object next = currentMap.get(patternPart);
            if (next != null) {
                String newPath = currentPath.isEmpty() ? patternPart : currentPath + "." + patternPart;
                findMatchingPathsRecursive(next, patternParts, partIndex + 1, newPath, matches);
            }
        }
    }

    /**
     * Apply an operation to all paths matching a wildcard pattern.
     * This is useful for operations that should apply to multiple paths.
     *
     * @param data the root map
     * @param pattern the path pattern with optional wildcards
     * @param operation the operation to apply to each matching path
     */
    public static void applyToMatchingPaths(Map<String, Object> data, String pattern,
                                           PathOperation operation) {
        if (!isWildcardPath(pattern)) {
            // No wildcards, apply to single path
            operation.apply(data, pattern);
        } else {
            // Has wildcards, find all matching paths and apply to each
            List<String> matchingPaths = findMatchingPaths(data, pattern);
            for (String path : matchingPaths) {
                operation.apply(data, path);
            }
        }
    }

    /**
     * Functional interface for operations that apply to paths.
     */
    public interface PathOperation {
        void apply(Map<String, Object> data, String path);
    }
}

