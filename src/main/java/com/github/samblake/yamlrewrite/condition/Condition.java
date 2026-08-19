package com.github.samblake.yamlrewrite.condition;

import java.util.Map;

/**
 * Interface for conditions that determine whether an operation should be applied.
 */
public interface Condition {

    /**
     * Check if the condition is satisfied.
     *
     * @param data the root YAML map to evaluate the condition against
     * @return true if the condition is met, false otherwise
     */
    boolean isSatisfied(Map<String, Object> data);

    /**
     * Check if the condition is satisfied relative to a base path.
     * When the condition path is empty/null, it checks at the base path.
     * When the condition path is specified, it's absolute from the root.
     *
     * @param data the root YAML map to evaluate the condition against
     * @param basePath the base path context (typically the operation's path)
     * @return true if the condition is met, false otherwise
     */
    boolean isSatisfied(Map<String, Object> data, String basePath);

}

