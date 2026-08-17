package com.github.samblake.yamlrewrite;

import com.github.samblake.yamlrewrite.io.FileSystem;
import com.github.samblake.yamlrewrite.io.RealFileSystem;
import com.github.samblake.yamlrewrite.operation.TransformationOperation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Main transformer class for applying YAML transformations.
 * Loads a source YAML file, applies transformations defined in a transformation file,
 * and writes the result to an output file.
 *
 * Uses a FileSystem abstraction to allow testing with in-memory filesystems.
 */
public class YamlTransformer {

    private final FileSystem fileSystem;

    /**
     * Create a transformer using the real filesystem.
     */
    public YamlTransformer() {
        this(new RealFileSystem());
    }

    /**
     * Create a transformer with a custom filesystem implementation.
     * Useful for testing with in-memory filesystems.
     *
     * @param fileSystem the filesystem implementation to use
     */
    public YamlTransformer(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * Transform a YAML file using transformations from a specification file.
     *
     * @param sourceFilePath the path to the source YAML file to transform
     * @param transformationFilePath the path to the YAML file containing transformation specifications
     * @param outputFilePath the path to the output file (if null, sourceFilePath is modified in place)
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the transformation specification is invalid
     */
    public void transform(String sourceFilePath, String transformationFilePath, String outputFilePath) throws IOException {
        if (!fileSystem.fileExists(sourceFilePath)) {
            throw new IOException("Source file does not exist: " + sourceFilePath);
        }

        if (!fileSystem.fileExists(transformationFilePath)) {
            throw new IOException("Transformation file does not exist: " + transformationFilePath);
        }

        // Load source YAML
        Map<String, Object> data = fileSystem.loadYaml(sourceFilePath);

        // Load transformation specification
        Map<String, Object> transformSpec = fileSystem.loadYaml(transformationFilePath);

        // Parse and apply transformations
        List<TransformationOperation> operations = TransformationParser.parse(transformSpec);
        for (TransformationOperation operation : operations) {
            operation.apply(data);
        }

        // Write result to output file
        String targetPath = (outputFilePath != null) ? outputFilePath : sourceFilePath;
        fileSystem.writeYaml(targetPath, data);
    }
}


