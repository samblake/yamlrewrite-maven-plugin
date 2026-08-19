package com.github.samblake.yamlrewrite.operation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.join;
import static java.util.Arrays.copyOf;

/**
 * Utility class for navigating YAML structures using dot notation paths.
 * Supports paths like "spec.replicas" to access nested keys.
 * Also supports wildcard patterns like "paths.*.get" to match all keys at a level.
 * Also supports array indexing and filtering:
 *   - Array index: "parameters.0"
 *   - Array filter: "parameters[name=channel]"
 *   - Array wildcard: "parameters.*"
 */
public class YamlPathNavigator {

    private static final Pattern ARRAY_SEGMENT_PATTERN = Pattern.compile("^([^\\[]+)(?:\\[([^\\]]+)\\])?$");

    /**
     * Navigate to a value in the YAML structure using dot notation.
     * Returns null if the path doesn't exist.
     * Supports array indices: "parameters.0" accesses the first element of parameters array.
     *
     * @param data the root map
     * @param path dot-separated path (e.g., "spec.replicas.count" or "parameters.0.name")
     * @return the value at the path, or null if not found
     */
    public static Object getValueAtPath(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (String key : keys) {
            if (current == null) {
                return null;
            }

            // Try as array index first
            try {
                int index = Integer.parseInt(key);
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (NumberFormatException e) {
                // Not a number, try as map key
                if (!(current instanceof Map)) {
                    return null;
                }
                current = ((Map<String, Object>) current).get(key);
            }
        }

        return current;
    }

    /**
     * Set a value at the specified path, creating intermediate maps as needed.
     * Supports array indices.
     *
     * @param data the root map
     * @param path dot-separated path (e.g., "spec.replicas" or "parameters.0.name")
     * @param value the value to set
     */
    public static void setValueAtPath(Map<String, Object> data, String path, Object value) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];

            // Try as array index first
            try {
                int index = Integer.parseInt(key);
                if (current instanceof List) {
                    List<Object> list = (List<Object>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return; // Can't set beyond list bounds
                    }
                } else {
                    return; // Not a list, can't use index
                }
            } catch (NumberFormatException e) {
                // Not a number, treat as map key
                if (!(current instanceof Map)) {
                    return;
                }
                Map<String, Object> currentMap = (Map<String, Object>) current;
                Object next = currentMap.get(key);

                if (!(next instanceof Map)) {
                    next = new java.util.LinkedHashMap<>();
                    currentMap.put(key, next);
                }

                current = next;
            }
        }

        // Set the final value
        String lastKey = keys[keys.length - 1];
        try {
            int index = Integer.parseInt(lastKey);
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                if (index >= 0 && index < list.size()) {
                    list.set(index, value);
                }
            }
        } catch (NumberFormatException e) {
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(lastKey, value);
            }
        }
    }

    /**
     * Delete the value at the specified path.
     * Supports array indices.
     *
     * @param data the root map
     * @param path dot-separated path
     * @return true if something was deleted, false if path didn't exist
     */
    public static boolean deleteAtPath(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];

            // Try as array index first
            try {
                int index = Integer.parseInt(key);
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (NumberFormatException e) {
                // Not a number, treat as map key
                if (!(current instanceof Map)) {
                    return false;
                }

                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return false;
                }
            }
        }

        // Delete the final value
        String lastKey = keys[keys.length - 1];
        try {
            int index = Integer.parseInt(lastKey);
            if (current instanceof List) {
                List<?> list = (List<?>) current;
                if (index >= 0 && index < list.size()) {
                    list.remove(index);
                    return true;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            if (current instanceof Map) {
                return ((Map<String, Object>) current).remove(lastKey) != null;
            }
            return false;
        }
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
     * Parse a dot-separated path into PathSegment objects.
     * Handles keys, wildcards, array indices, and array filters.
     *
     * @param path the dot-separated path (e.g., "parameters[name=channel]", "routes.*.methods", "items[]")
     * @return list of PathSegment objects
     */
    public static List<PathSegment> parsePathSegments(String path) {
        List<PathSegment> segments = new ArrayList<>();
        String[] parts = path.split("\\.");

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            // Check if this part contains array bracket notation
            Matcher matcher = ARRAY_SEGMENT_PATTERN.matcher(part);
            if (matcher.matches()) {
                String keyPart = matcher.group(1);
                String bracketPart = matcher.group(2);

                // Add the key/wildcard part
                if (!keyPart.isEmpty()) {
                    segments.add(PathSegment.parse(keyPart));
                }

                // Add the bracket part if it exists (index, filter, or empty array)
                if (bracketPart != null) {
                    if (bracketPart.isEmpty()) {
                        // Empty brackets: []
                        segments.add(PathSegment.empty());
                    } else {
                        // Non-empty brackets: [name=value] or [0]
                        segments.add(PathSegment.parse(bracketPart));
                    }
                }
            } else {
                segments.add(PathSegment.parse(part));
            }
        }

        return segments;
    }

    /**
     * Find all paths matching a pattern that may include array filters.
     * For example, "parameters[name=channel]" would match the path to that specific parameter.
     *
     * @param data the root map
     * @param pattern path pattern with optional wildcards and array filters
     * @return list of matching paths
     */
    public static List<String> findMatchingPathsWithArrays(Map<String, Object> data, String pattern) {
        List<String> matches = new ArrayList<>();
        List<PathSegment> segments = parsePathSegments(pattern);
        findMatchingPathsRecursiveWithArrays(data, segments, 0, "", matches);
        return matches;
    }

    /**
     * Recursively find all paths matching a pattern that includes arrays.
     */
    private static void findMatchingPathsRecursiveWithArrays(Object current, List<PathSegment> segments,
                                                            int segmentIndex, String currentPath,
                                                            List<String> matches) {
        if (segmentIndex >= segments.size()) {
            if (!currentPath.isEmpty()) {
                matches.add(currentPath);
            }
            return;
        }

        PathSegment segment = segments.get(segmentIndex);

        if (segment.isKey()) {
            // Match a specific key in a map
            if (!(current instanceof Map)) {
                return;
            }
            Map<String, Object> currentMap = (Map<String, Object>) current;
            Object next = currentMap.get(segment.getValue());
            if (next != null) {
                String newPath = currentPath.isEmpty() ? segment.getValue() : currentPath + "." + segment.getValue();
                findMatchingPathsRecursiveWithArrays(next, segments, segmentIndex + 1, newPath, matches);
            }
        } else if (segment.isWildcard()) {
            // Match all keys in a map, or all elements in a list
            if (current instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) current;
                for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
                    String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                    findMatchingPathsRecursiveWithArrays(entry.getValue(), segments, segmentIndex + 1, newPath, matches);
                }
            } else if (current instanceof List) {
                List<?> list = (List<?>) current;
                for (int i = 0; i < list.size(); i++) {
                    String newPath = currentPath + "." + i;
                    findMatchingPathsRecursiveWithArrays(list.get(i), segments, segmentIndex + 1, newPath, matches);
                }
            }
        } else if (segment.isIndex()) {
            // Match a specific array index
            if (!(current instanceof List)) {
                return;
            }
            List<?> list = (List<?>) current;
            int idx = Integer.parseInt(segment.getValue());
            if (idx >= 0 && idx < list.size()) {
                String newPath = currentPath + "." + idx;
                findMatchingPathsRecursiveWithArrays(list.get(idx), segments, segmentIndex + 1, newPath, matches);
            }
        } else if (segment.isFilter()) {
            // Match array elements by property value
            if (!(current instanceof List)) {
                return;
            }
            List<?> list = (List<?>) current;
            for (int i = 0; i < list.size(); i++) {
                Object element = list.get(i);
                if (element instanceof Map) {
                    Map<String, Object> elementMap = (Map<String, Object>) element;
                    Object propValue = elementMap.get(segment.getFilterProperty());
                    if (propValue != null && propValue.toString().equals(segment.getFilterValue())) {
                        String newPath = currentPath + "." + i;
                        findMatchingPathsRecursiveWithArrays(element, segments, segmentIndex + 1, newPath, matches);
                    }
                }
            }
        } else if (segment.isEmpty()) {
            // Match empty arrays/lists in the current map
            if (!(current instanceof Map)) {
                return;
            }
            Map<String, Object> currentMap = (Map<String, Object>) current;
            for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List && ((List<?>) value).isEmpty()) {
                    String newPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                    findMatchingPathsRecursiveWithArrays(value, segments, segmentIndex + 1, newPath, matches);
                }
            }
        }
    }

    /**
     * Check if a path contains array filter syntax or other advanced patterns.
     */
    public static boolean hasArrayFilters(String path) {
        return path != null && path.contains("[");
    }

    /**
     * Check if a path is looking for empty arrays.
     */
    public static boolean isEmptyArrayFilter(String path) {
        return path != null && path.contains("[]");
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
     * Apply an operation to all paths matching a wildcard pattern or array filter.
     * This is useful for operations that should apply to multiple paths.
     *
     * @param data the root map
     * @param pattern the path pattern with optional wildcards or array filters
     * @param operation the operation to apply to each matching path
     */
    public static void applyToMatchingPaths(Map<String, Object> data, String pattern,
                                           PathOperation operation) {
        if (hasArrayFilters(pattern)) {
            // Use array-aware path matching
            List<String> matchingPaths = findMatchingPathsWithArrays(data, pattern);
            if (matchingPaths.isEmpty()) {
                // Pattern might be a simple path, try as-is
                operation.apply(data, pattern);
            } else {
                for (String path : matchingPaths) {
                    operation.apply(data, path);
                }
            }
        } else if (!isWildcardPath(pattern)) {
            // No wildcards or array filters, apply to single path
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

    /**
     * Check if a value at a path is an empty array/list.
     *
     * @param data the root map
     * @param path the path to check
     * @return true if the value at path is an empty list
     */
    public static boolean isEmptyArray(Map<String, Object> data, String path) {
        Object value = getValueAtPath(data, path);
        return value instanceof List && ((List<?>) value).isEmpty();
    }

    /**
     * Delete a value at the specified path, with special handling for empty arrays.
     * If the parent is a list and the element at index is in a list, remove it from the list.
     * If the target is an empty array/list, delete it from its parent.
     *
     * @param data the root map
     * @param path dot-separated path
     * @return true if something was deleted, false if path didn't exist
     */
    public static boolean deleteEmptyArrayAtPath(Map<String, Object> data, String path) {
        String[] keys = path.split("\\.");
        if (keys.length == 0) {
            return false;
        }

        // Navigate to the parent of the target
        Object current = data;
        for (int i = 0; i < keys.length - 1; i++) {
            String key = keys[i];

            try {
                int index = Integer.parseInt(key);
                if (current instanceof List) {
                    List<?> list = (List<?>) current;
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (NumberFormatException e) {
                if (!(current instanceof Map)) {
                    return false;
                }
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return false;
                }
            }
        }

        // Delete the target
        String lastKey = keys[keys.length - 1];
        try {
            int index = Integer.parseInt(lastKey);
            if (current instanceof List) {
                List<?> list = (List<?>) current;
                if (index >= 0 && index < list.size()) {
                    Object item = list.get(index);
                    // Only delete if it's an empty array/list
                    if (item instanceof List && ((List<?>) item).isEmpty()) {
                        list.remove(index);
                        return true;
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                Object item = map.get(lastKey);
                // Only delete if it's an empty array/list
                if (item instanceof List && ((List<?>) item).isEmpty()) {
                    return map.remove(lastKey) != null;
                }
            }
            return false;
        }
    }

    /**
     * Clean up all empty arrays/lists from the data structure.
     * Recursively removes empty list/array values from maps.
     *
     * @param data the root map
     * @return true if any empty arrays were removed
     */
    public static boolean cleanupEmptyArrays(Map<String, Object> data) {
        boolean removed = false;
        Iterator<Map.Entry<String, Object>> iterator = data.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (list.isEmpty()) {
                    iterator.remove();
                    removed = true;
                } else {
                    // Recursively clean nested structures
                    for (Object item : list) {
                        if (item instanceof Map) {
                            if (cleanupEmptyArrays((Map<String, Object>) item)) {
                                removed = true;
                            }
                        }
                    }
                }
            } else if (value instanceof Map) {
                if (cleanupEmptyArrays((Map<String, Object>) value)) {
                    removed = true;
                }
            }
        }

        return removed;
    }
}

