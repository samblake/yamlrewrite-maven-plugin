package com.github.samblake.yamlrewrite.io;

import java.io.IOException;
import java.util.Map;

/**
 * Abstraction for filesystem operations to enable testing without real files.
 */
public interface FileSystem {

    /**
     * Load a YAML file and return its contents as a map.
     *
     * @param filePath the path to the file to load
     * @return the parsed YAML as a Map
     * @throws IOException if the file cannot be read
     * @throws IllegalArgumentException if the file doesn't exist or contains invalid YAML
     */
    Map<String, Object> loadYaml(String filePath) throws IOException;

    /**
     * Write YAML data to a file.
     *
     * @param filePath the path to the file to write to
     * @param data the data to write
     * @throws IOException if the file cannot be written
     */
    void writeYaml(String filePath, Map<String, Object> data) throws IOException;

    /**
     * Check if a file exists.
     *
     * @param filePath the path to check
     * @return true if the file exists, false otherwise
     */
    boolean fileExists(String filePath);
}

