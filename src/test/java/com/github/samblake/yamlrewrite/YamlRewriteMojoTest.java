package com.github.samblake.yamlrewrite;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;

/**
 * Unit tests for YAML transformation using an in-memory filesystem.
 * No integration tests or real file I/O - everything is in-memory for speed and isolation.
 */
public class YamlRewriteMojoTest {

    private InMemoryFileSystem fileSystem;
    private YamlTransformer transformer;

    @Before
    public void setUp() {
        fileSystem = new InMemoryFileSystem();
        transformer = new YamlTransformer(fileSystem);
    }

    /**
     * Test basic delete, set, and rename operations with simple YAML structures.
     */
    @Test
    public void testBasicOperations() throws Exception {
        // Setup: Create source YAML
        fileSystem.writeRawYaml("source.yaml",
                "spec:\n" +
                "  replicas: 1\n" +
                "  oldField: remove-me\n" +
                "  name: myapp");

        // Setup: Create transformation spec
        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: delete\n" +
                "    path: spec.oldField\n" +
                "  - operation: set\n" +
                "    path: spec.replicas\n" +
                "    value: 3");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify output was written
        assertTrue("Output file should exist", fileSystem.fileExists("output.yaml"));

        // Verify transformations were applied
        String output = fileSystem.readRawYaml("output.yaml");
        assertTrue("Output should contain replicas: 3", output.contains("replicas: 3"));
        assertFalse("Output should not contain oldField", output.contains("oldField"));
    }

    /**
     * Test rename operation.
     */
    @Test
    public void testRenameOperation() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml", "spec:\n  oldName: myvalue\n  other: stay");
        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: rename\n" +
                "    from: spec.oldName\n" +
                "    to: spec.newName");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");
        assertTrue("Output should contain newName", output.contains("newName"));
        assertFalse("Output should not contain oldName", output.contains("oldName"));
        assertTrue("Output should preserve other field", output.contains("other: stay"));
    }

    /**
     * Test in-place modification (when output file path is null).
     */
    @Test
    public void testInPlaceModification() throws Exception {
        // Setup
        fileSystem.writeRawYaml("config.yaml", "version: 1\nstatus: old");
        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: set\n" +
                "    path: status\n" +
                "    value: new");

        // Execute with null output file (in-place modification)
        transformer.transform("config.yaml", "transform.yaml", null);

        // Verify source file was modified
        assertTrue("Source file should still exist", fileSystem.fileExists("config.yaml"));
        String result = fileSystem.readRawYaml("config.yaml");
        assertTrue("Modified file should contain new status", result.contains("status: new"));
    }

    /**
     * Test multiple operations in sequence.
     */
    @Test
    public void testMultipleOperations() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml",
                "app:\n" +
                "  name: myapp\n" +
                "  version: 1.0\n" +
                "  deprecated: true\n" +
                "database:\n" +
                "  host: localhost\n" +
                "  oldConfig: remove");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: delete\n" +
                "    path: app.deprecated\n" +
                "  - operation: set\n" +
                "    path: app.version\n" +
                "    value: 2.0\n" +
                "  - operation: delete\n" +
                "    path: database.oldConfig\n" +
                "  - operation: set\n" +
                "    path: database.port\n" +
                "    value: 5432");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        assertTrue("Should contain version 2.0", output.contains("version: 2.0"));
        assertTrue("Should contain port 5432", output.contains("port: 5432"));
        assertFalse("Should not contain deprecated", output.contains("deprecated"));
        assertFalse("Should not contain oldConfig", output.contains("oldConfig"));
    }

    /**
     * Test deeply nested structures.
     */
    @Test
    public void testDeeplyNestedStructures() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml",
                "spec:\n" +
                "  template:\n" +
                "    spec:\n" +
                "      containers:\n" +
                "        image: old-image");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: set\n" +
                "    path: spec.template.spec.containers.image\n" +
                "    value: new-image:2.0");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        String output = fileSystem.readRawYaml("output.yaml");
        assertTrue("Should contain new image", output.contains("new-image:2.0"));
    }

    /**
     * Test creating new nested paths with set operation.
     */
    @Test
    public void testCreatingNewNestedPaths() throws Exception {
        // Setup - source has minimal structure
        fileSystem.writeRawYaml("source.yaml", "name: myapp");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: set\n" +
                "    path: config.database.host\n" +
                "    value: localhost\n" +
                "  - operation: set\n" +
                "    path: config.database.port\n" +
                "    value: 5432");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify entire nested structure was created
        String output = fileSystem.readRawYaml("output.yaml");
        assertTrue("Should contain host localhost", output.contains("localhost"));
        assertTrue("Should contain port 5432", output.contains("5432"));
    }

    /**
     * Test error handling for missing source file.
     */
    @Test(expected = IOException.class)
    public void testMissingSourceFile() throws Exception {
        fileSystem.writeRawYaml("transform.yaml", "transformations: []");

        // Should throw IOException
        transformer.transform("nonexistent.yaml", "transform.yaml", "output.yaml");
    }

    /**
     * Test error handling for missing transformation file.
     */
    @Test(expected = IOException.class)
    public void testMissingTransformationFile() throws Exception {
        fileSystem.writeRawYaml("source.yaml", "name: test");

        // Should throw IOException
        transformer.transform("source.yaml", "nonexistent.yaml", "output.yaml");
    }

    /**
     * Test error handling for invalid transformation specification.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTransformationSpec() throws Exception {
        fileSystem.writeRawYaml("source.yaml", "name: test");
        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: unknown\n" +
                "    path: name");

        // Should throw IllegalArgumentException for unknown operation
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");
    }

    /**
     * Test idempotent delete (deleting non-existent path should not fail).
     */
    @Test
    public void testIdempotentDelete() throws Exception {
        fileSystem.writeRawYaml("source.yaml", "name: myapp");
        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: delete\n" +
                "    path: nonexistent.path.here");

        // Should succeed without error
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify file was still processed
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
    }

    /**
     * Test complex real-world transformation scenario.
     */
    @Test
    public void testComplexRealWorldScenario() throws Exception {
        // Simulate transforming a Kubernetes deployment config
        fileSystem.writeRawYaml("deployment.yaml",
                "apiVersion: apps/v1\n" +
                "kind: Deployment\n" +
                "metadata:\n" +
                "  name: web-app\n" +
                "  namespace: dev\n" +
                "  labels:\n" +
                "    version: v1.0\n" +
                "spec:\n" +
                "  replicas: 1\n" +
                "  template:\n" +
                "    spec:\n" +
                "      containers:\n" +
                "        - name: web\n" +
                "          image: myimage:1.0\n" +
                "          env:\n" +
                "            DEBUG: 'true'");

        fileSystem.writeRawYaml("prod-transform.yaml",
                "transformations:\n" +
                "  - operation: set\n" +
                "    path: metadata.namespace\n" +
                "    value: prod\n" +
                "  - operation: set\n" +
                "    path: metadata.labels.version\n" +
                "    value: v2.0\n" +
                "  - operation: set\n" +
                "    path: spec.replicas\n" +
                "    value: 3\n" +
                "  - operation: delete\n" +
                "    path: spec.template.spec.containers.0.env.DEBUG");

        // Execute
        transformer.transform("deployment.yaml", "prod-transform.yaml", "deployment-prod.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("deployment-prod.yaml"));
        String output = fileSystem.readRawYaml("deployment-prod.yaml");

        assertTrue("Should have prod namespace", output.contains("namespace: prod"));
        assertTrue("Should have v2.0 version", output.contains("version: v2.0"));
        assertTrue("Should have 3 replicas", output.contains("replicas: 3"));
    }

    /**
     * Test merge operation with simple values.
     */
    @Test
    public void testMergeOperationSimple() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml",
                "config:\n" +
                "  database:\n" +
                "    host: localhost\n" +
                "    port: 5432");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: config.database\n" +
                "    value:\n" +
                "      timeout: 30\n" +
                "      pool_size: 10");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        assertTrue("Should preserve host", output.contains("host: localhost"));
        assertTrue("Should preserve port", output.contains("port: 5432"));
        assertTrue("Should add timeout", output.contains("timeout: 30"));
        assertTrue("Should add pool_size", output.contains("pool_size: 10"));
    }

    /**
     * Test merge operation with deeply nested structures.
     */
    @Test
    public void testMergeOperationDeepNesting() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml",
                "app:\n" +
                "  settings:\n" +
                "    database:\n" +
                "      host: localhost\n" +
                "      credentials:\n" +
                "        user: admin");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: app.settings.database\n" +
                "    value:\n" +
                "      port: 5432\n" +
                "      credentials:\n" +
                "        password: secret");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        // Original values should be preserved
        assertTrue("Should preserve host", output.contains("host: localhost"));
        assertTrue("Should preserve user", output.contains("user: admin"));

        // New values should be added
        assertTrue("Should add port", output.contains("port: 5432"));
        assertTrue("Should add password", output.contains("password: secret"));
    }

    /**
     * Test merge operation creating new path.
     */
    @Test
    public void testMergeOperationNewPath() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml", "name: myapp\nversion: 1.0");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: config\n" +
                "    value:\n" +
                "      timeout: 30\n" +
                "      retries: 3");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        assertTrue("Should create config section", output.contains("config:"));
        assertTrue("Should have timeout", output.contains("timeout: 30"));
        assertTrue("Should have retries", output.contains("retries: 3"));
        assertTrue("Should preserve original fields", output.contains("name: myapp"));
    }

    /**
     * Test merge operation overwriting non-map values.
     */
    @Test
    public void testMergeOperationOverwriteNonMap() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml", "config: old_value");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: config\n" +
                "    value:\n" +
                "      timeout: 30");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify - non-map value should be replaced with map
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        assertFalse("Should replace old_value", output.contains("old_value"));
        assertTrue("Should have new config", output.contains("timeout: 30"));
    }

    /**
     * Test multiple merge operations in sequence.
     */
    @Test
    public void testMultipleMergeOperations() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml",
                "app:\n" +
                "  version: 1.0\n" +
                "config:\n" +
                "  log_level: info");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: app\n" +
                "    value:\n" +
                "      environment: production\n" +
                "      features:\n" +
                "        cache: enabled\n" +
                "  - operation: merge\n" +
                "    path: config\n" +
                "    value:\n" +
                "      timeout: 30\n" +
                "      features:\n" +
                "        compression: gzip");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        // First merge
        assertTrue("Should preserve app.version", output.contains("version: 1.0"));
        assertTrue("Should add environment", output.contains("environment: production"));
        assertTrue("Should add cache", output.contains("cache: enabled"));

        // Second merge
        assertTrue("Should preserve log_level", output.contains("log_level: info"));
        assertTrue("Should add timeout", output.contains("timeout: 30"));
        assertTrue("Should add compression", output.contains("compression: gzip"));
    }

    /**
     * Test merge with empty source map.
     */
    @Test
    public void testMergeOperationEmptySource() throws Exception {
        // Setup
        fileSystem.writeRawYaml("source.yaml", "app: {}");

        fileSystem.writeRawYaml("transform.yaml",
                "transformations:\n" +
                "  - operation: merge\n" +
                "    path: app\n" +
                "    value:\n" +
                "      version: 2.0\n" +
                "      name: myapp");

        // Execute
        transformer.transform("source.yaml", "transform.yaml", "output.yaml");

        // Verify
        assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
        String output = fileSystem.readRawYaml("output.yaml");

        assertTrue("Should have version", output.contains("version: 2.0"));
        assertTrue("Should have name", output.contains("name: myapp"));
    }

     /**
      * Test merge preserving complex nested structures.
      */
     @Test
     public void testMergeOperationComplexStructures() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "services:\n" +
                 "  api:\n" +
                 "    port: 8080\n" +
                 "    routes:\n" +
                 "      - path: /health\n" +
                 "        method: GET");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: merge\n" +
                 "    path: services.api\n" +
                 "    value:\n" +
                 "      timeout: 30\n" +
                 "      authentication:\n" +
                 "        enabled: true\n" +
                 "        type: oauth2");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");

         // Original structure preserved
         assertTrue("Should preserve port", output.contains("port: 8080"));
         assertTrue("Should preserve routes", output.contains("routes:"));
         assertTrue("Should preserve route path", output.contains("path: /health"));

         // New values added
         assertTrue("Should add timeout", output.contains("timeout: 30"));
         assertTrue("Should add authentication", output.contains("authentication:"));
         assertTrue("Should add enabled", output.contains("enabled: true"));
         assertTrue("Should add type oauth2", output.contains("type: oauth2"));
     }

     /**
      * Test set operation with equals condition that is satisfied.
      */
     @Test
     public void testSetOperationWithConditionSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: dev\n" +
                 "config:\n" +
                 "  replicas: 1\n" +
                 "  debug: false");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: config.debug\n" +
                 "    value: true\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is satisfied, so operation should apply
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should set debug to true", output.contains("debug: true"));
     }

     /**
      * Test set operation with equals condition that is NOT satisfied.
      */
     @Test
     public void testSetOperationWithConditionNotSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: prod\n" +
                 "config:\n" +
                 "  replicas: 1\n" +
                 "  debug: false");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: config.debug\n" +
                 "    value: true\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is NOT satisfied, so operation should NOT apply
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should preserve debug as false", output.contains("debug: false"));
     }

     /**
      * Test delete operation with equals condition that is satisfied.
      */
     @Test
     public void testDeleteOperationWithConditionSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: dev\n" +
                 "config:\n" +
                 "  replicas: 1\n" +
                 "  debug: true");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: delete\n" +
                 "    path: config.debug\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is satisfied, so debug field should be deleted
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertFalse("Should not contain debug", output.contains("debug"));
         assertTrue("Should preserve replicas", output.contains("replicas"));
     }

     /**
      * Test delete operation with equals condition that is NOT satisfied.
      */
     @Test
     public void testDeleteOperationWithConditionNotSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: prod\n" +
                 "config:\n" +
                 "  replicas: 1\n" +
                 "  debug: true");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: delete\n" +
                 "    path: config.debug\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is NOT satisfied, so debug field should remain
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should preserve debug", output.contains("debug: true"));
     }

     /**
      * Test rename operation with equals condition that is satisfied.
      */
     @Test
     public void testRenameOperationWithConditionSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: dev\n" +
                 "config:\n" +
                 "  old_setting: value");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: rename\n" +
                 "    from: config.old_setting\n" +
                 "    to: config.new_setting\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is satisfied, so rename should apply
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should have new_setting", output.contains("new_setting"));
         assertFalse("Should not have old_setting", output.contains("old_setting"));
     }

     /**
      * Test merge operation with equals condition that is satisfied.
      */
     @Test
     public void testMergeOperationWithConditionSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: staging\n" +
                 "config:\n" +
                 "  timeout: 10");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: merge\n" +
                 "    path: config\n" +
                 "    value:\n" +
                 "      retries: 3\n" +
                 "      cache_enabled: true\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: staging");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition is satisfied, so merge should apply
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should preserve timeout", output.contains("timeout: 10"));
         assertTrue("Should add retries", output.contains("retries: 3"));
         assertTrue("Should add cache_enabled", output.contains("cache_enabled: true"));
     }

     /**
      * Test multiple operations with different conditions.
      */
     @Test
     public void testMultipleOperationsWithMixedConditions() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "environment: dev\n" +
                 "app:\n" +
                 "  name: myapp\n" +
                 "  version: 1.0\n" +
                 "  debug: false");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: app.debug\n" +
                 "    value: true\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: dev\n" +
                 "  - operation: set\n" +
                 "    path: app.version\n" +
                 "    value: 2.0\n" +
                 "  - operation: delete\n" +
                 "    path: app.deprecated\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: environment\n" +
                 "      value: prod");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");

         // First condition is satisfied (dev environment)
         assertTrue("Should set debug to true", output.contains("debug: true"));
         // Second operation has no condition
         assertTrue("Should set version to 2.0", output.contains("version: 2.0"));
         // Third condition is NOT satisfied (environment is dev, not prod)
         assertTrue("Should preserve the app section", output.contains("name: myapp"));
     }

     /**
      * Test condition with nested path.
      */
     @Test
     public void testConditionWithNestedPath() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "metadata:\n" +
                 "  env:\n" +
                 "    type: production\n" +
                 "config:\n" +
                 "  cache: false");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: config.cache\n" +
                 "    value: true\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      path: metadata.env.type\n" +
                 "      value: production");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition checks nested path and is satisfied
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should set cache to true", output.contains("cache: true"));
     }

     /**
      * Test condition with relative path (empty condition path uses operation's path).
      */
     @Test
     public void testConditionWithRelativePath() throws Exception {
         // Setup - Check the value at the operation's target path
         fileSystem.writeRawYaml("source.yaml",
                 "config:\n" +
                 "  env: production\n" +
                 "  setting: old");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: config.env\n" +
                 "    value: dev\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      value: production");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition checks value at the operation's path (config.env)
         // Since config.env is 'production' and condition matches, operation should apply
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should change env to dev", output.contains("env: dev"));
     }

     /**
      * Test relative path condition not satisfied.
      */
     @Test
     public void testRelativePathConditionNotSatisfied() throws Exception {
         // Setup
         fileSystem.writeRawYaml("source.yaml",
                 "config:\n" +
                 "  env: staging\n" +
                 "  setting: old");

         fileSystem.writeRawYaml("transform.yaml",
                 "transformations:\n" +
                 "  - operation: set\n" +
                 "    path: config.env\n" +
                 "    value: dev\n" +
                 "    when:\n" +
                 "      type: equals\n" +
                 "      value: production");

         // Execute
         transformer.transform("source.yaml", "transform.yaml", "output.yaml");

         // Verify - condition checks value at operation's path (config.env = staging)
         // Condition doesn't match, so operation should be skipped
         assertTrue("Output should exist", fileSystem.fileExists("output.yaml"));
         String output = fileSystem.readRawYaml("output.yaml");
         assertTrue("Should preserve env as staging", output.contains("env: staging"));
     }

 }
