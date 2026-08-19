package com.github.samblake.yamlrewrite.operation;

import com.github.samblake.yamlrewrite.condition.Condition;
import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class for transformation operations that support optional conditions.
 * Handles the common logic for evaluating conditions before applying operations.
 */
public abstract class ConditionalTransformationOperation implements TransformationOperation {

    protected final String path;
    protected final Optional<Condition> condition;

    protected ConditionalTransformationOperation() {
        this.path = null;
        this.condition = Optional.empty();
    }

    protected ConditionalTransformationOperation(String path) {
        this.path = path;
        this.condition = Optional.empty();
    }

    protected ConditionalTransformationOperation(String path, Condition condition) {
        this.path = path;
        this.condition = Optional.of(condition);
    }

    protected ConditionalTransformationOperation(Condition condition) {
        this.path = null;
        this.condition = Optional.of(condition);
    }

    @Override
    public final void apply(Map<String, Object> data) {
        // Check if condition exists and is not satisfied - if so, skip operation
        if (condition.isPresent() && !condition.get().isSatisfied(data, path)) {
            return;
        }
        // Condition is satisfied or doesn't exist - execute the operation
        executeOperation(data);
    }

    /**
     * Executes the actual operation logic.
     * Subclasses must implement this to define their specific behavior.
     *
     * @param data the YAML data structure
     */
    protected abstract void executeOperation(Map<String, Object> data);


    @Override
    public Optional<Condition> getCondition() {
        return condition;
    }
}

