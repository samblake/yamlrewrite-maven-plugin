package com.github.samblake.yamlrewrite.operation;

import java.util.Map;

/**
 * Utility class for navigating YAML structures using dot notation paths.
 * Supports paths like "spec.replicas" to access nested keys.
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

        String parentPath = String.join(".", java.util.Arrays.copyOf(keys, keys.length - 1));
        Object parent = getValueAtPath(data, parentPath);

        if (!(parent instanceof Map)) {
            return null;
        }

        return new Object[]{parent, keys[keys.length - 1]};
    }
}

