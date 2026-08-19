package com.github.samblake.yamlrewrite.operation;

/**
 * Represents a single segment in a path, which can be:
 * - A key: "spec"
 * - A wildcard: "*"
 * - An array index: "0"
 * - An array filter: "name=channel"
 * - An empty array filter: "empty"
 */
public class PathSegment {
    public enum Type {
        KEY,           // Regular map key: "spec"
        WILDCARD,      // Wildcard: "*"
        INDEX,         // Array index: "0"
        FILTER,        // Array filter: "name=channel"
        EMPTY          // Empty array filter: "empty"
    }
    private final Type type;
    private final String value;
    private final String filterProperty;  // For FILTER type: "name"
    private final String filterValue;     // For FILTER type: "channel"

    /**
     * Create a KEY segment
     */
    public static PathSegment key(String name) {
        return new PathSegment(Type.KEY, name, null, null);
    }

    /**
     * Create a WILDCARD segment
     */
    public static PathSegment wildcard() {
        return new PathSegment(Type.WILDCARD, "*", null, null);
    }

    /**
     * Create an INDEX segment
     */
    public static PathSegment index(int idx) {
        return new PathSegment(Type.INDEX, String.valueOf(idx), null, null);
    }

    /**
     * Create a FILTER segment
     */
    public static PathSegment filter(String property, String value) {
        return new PathSegment(Type.FILTER, null, property, value);
    }

    /**
     * Create an EMPTY array filter segment
     */
    public static PathSegment empty() {
        return new PathSegment(Type.EMPTY, "[]", null, null);
    }

    /**
     * Parse a path segment from a string like "spec", "*", "0", "name=channel", or "[]"
     */
    public static PathSegment parse(String segment) {
        if (segment == null || segment.isEmpty()) {
            throw new IllegalArgumentException("Empty path segment");
        }

        if ("*".equals(segment)) {
            return wildcard();
        }

        if ("[]".equals(segment)) {
            return empty();
        }

        // Check if it's a filter (contains =)
        if (segment.contains("=")) {
            String[] parts = segment.split("=", 2);
            if (parts.length == 2) {
                return filter(parts[0].trim(), parts[1].trim());
            }
        }

        // Check if it's a numeric index
        try {
            int idx = Integer.parseInt(segment);
            return index(idx);
        } catch (NumberFormatException e) {
            // Not a number, treat as key
        }

        // Default to key
        return key(segment);
    }

    private PathSegment(Type type, String value, String filterProperty, String filterValue) {
        this.type = type;
        this.value = value;
        this.filterProperty = filterProperty;
        this.filterValue = filterValue;
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getFilterProperty() {
        return filterProperty;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public boolean isWildcard() {
        return type == Type.WILDCARD;
    }

    public boolean isFilter() {
        return type == Type.FILTER;
    }

    public boolean isIndex() {
        return type == Type.INDEX;
    }

    public boolean isKey() {
        return type == Type.KEY;
    }

    public boolean isEmpty() {
        return type == Type.EMPTY;
    }

    @Override
    public String toString() {
        switch (type) {
            case WILDCARD:
                return "*";
            case INDEX:
                return value;
            case FILTER:
                return filterProperty + "=" + filterValue;
            case EMPTY:
                return "[]";
            case KEY:
                return value;
            default:
                return value;
        }
    }
}






