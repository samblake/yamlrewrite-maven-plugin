package com.github.samblake.yamlrewrite.io;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Real filesystem implementation using actual files.
 */
public class RealFileSystem implements FileSystem {

    private final Yaml yaml;

    public RealFileSystem() {
        this.yaml = new Yaml();
    }

    @Override
    public Map<String, Object> loadYaml(String filePath) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            Object loaded = yaml.load(fis);

            if (loaded == null) {
                return new LinkedHashMap<>();
            }

            if (!(loaded instanceof Map)) {
                throw new IOException("YAML file must contain a map at the root level: " + file.getAbsolutePath());
            }

            return (Map<String, Object>) loaded;
        }
    }

    @Override
    public void writeYaml(String filePath, Map<String, Object> data) throws IOException {
        File file = new File(filePath);

        // Create parent directories if needed
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories: " + parentDir.getAbsolutePath());
            }
        }

        try (FileWriter fw = new FileWriter(file)) {
            yaml.dump(data, fw);
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        return new File(filePath).exists();
    }
}

