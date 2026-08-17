package com.github.samblake.yamlrewrite;

import com.github.samblake.yamlrewrite.io.FileSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory filesystem implementation for testing without real files.
 * Stores all file contents in memory.
 */
public class InMemoryFileSystem implements FileSystem {

    private final Map<String, String> files;
    private final Yaml yaml;

    public InMemoryFileSystem() {
        this.files = new HashMap<>();
        this.yaml = new Yaml();
    }

    /**
     * Write a raw YAML string to the virtual filesystem.
     * Useful for setting up test data.
     *
     * @param filePath the path to write to
     * @param yamlContent the YAML content as a string
     */
    public void writeRawYaml(String filePath, String yamlContent) {
        files.put(filePath, yamlContent);
    }

    /**
     * Read the raw YAML string from the virtual filesystem.
     * Useful for verifying test results.
     *
     * @param filePath the path to read from
     * @return the YAML content as a string, or null if file doesn't exist
     */
    public String readRawYaml(String filePath) {
        return files.get(filePath);
    }

    @Override
    public Map<String, Object> loadYaml(String filePath) throws IOException {
        String content = files.get(filePath);

        if (content == null) {
            throw new IOException("File does not exist: " + filePath);
        }

        Object loaded = yaml.load(content);

        if (loaded == null) {
            return new LinkedHashMap<>();
        }

        if (!(loaded instanceof Map)) {
            throw new IOException("YAML file must contain a map at the root level: " + filePath);
        }

        return (Map<String, Object>) loaded;
    }

    @Override
    public void writeYaml(String filePath, Map<String, Object> data) throws IOException {
        String yamlContent = yaml.dump(data);
        files.put(filePath, yamlContent);
    }

    @Override
    public boolean fileExists(String filePath) {
        return files.containsKey(filePath);
    }

    /**
     * Get all files in the virtual filesystem for debugging.
     *
     * @return map of file paths to contents
     */
    public Map<String, String> getAllFiles() {
        return new HashMap<>(files);
    }

    /**
     * Clear all files in the virtual filesystem.
     */
    public void clear() {
        files.clear();
    }
}

