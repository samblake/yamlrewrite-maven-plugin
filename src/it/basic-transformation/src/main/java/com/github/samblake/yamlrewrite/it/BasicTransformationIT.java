package com.github.samblake.yamlrewrite.it;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * Integration test that verifies the yamlrewrite-maven-plugin
 * correctly transforms a YAML file through Maven.
 *
 * This test compares the actual transformed output with an expected output file
 * and verifies both unconditional and conditional transformations.
 */
public class BasicTransformationIT {

    public static void main(String[] args) throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/config-transformed.yaml");
        File expectedFile = new File(basedir, "src/config-expected.yaml");

        if (!outputFile.exists()) {
            System.err.println("ERROR: Transformed output file does not exist at: " + outputFile.getAbsolutePath());
            System.exit(1);
        }
        if (!expectedFile.exists()) {
            System.err.println("ERROR: Expected output file does not exist at: " + expectedFile.getAbsolutePath());
            System.exit(1);
        }

        // Read file contents
        String actualContent = new String(Files.readAllBytes(outputFile.toPath()));
        String expectedContent = new String(Files.readAllBytes(expectedFile.toPath()));

        // Parse YAML
        Yaml yaml = new Yaml();
        Map<String, Object> actualYaml = yaml.load(actualContent);
        Map<String, Object> expectedYaml = yaml.load(expectedContent);

        System.out.println("=== Actual Transformed Output ===");
        System.out.println(actualContent);
        System.out.println("=================================\n");

        boolean allPass = true;

        // Test 1: Full YAML comparison
        System.out.println("--- Full YAML Comparison ---");
        if (actualYaml.equals(expectedYaml)) {
            System.out.println("✓ PASS: Actual YAML matches expected YAML");
        } else {
            System.out.println("✗ FAIL: Actual YAML does not match expected output");
            allPass = false;
        }

        // Test 2: Unconditional transformations
        System.out.println("\n--- Unconditional Transformations ---");

        // Verify metadata version was set to 2.0
        Map<String, Object> metadata = (Map<String, Object>) actualYaml.get("metadata");
        if (metadata != null && metadata.get("version") != null && metadata.get("version").equals(2.0)) {
            System.out.println("✓ PASS: Set metadata version to 2.0");
        } else {
            System.out.println("✗ FAIL: Metadata version should be 2.0");
            allPass = false;
        }

        // Verify deprecated field was deleted
        Map<String, Object> data = (Map<String, Object>) actualYaml.get("data");
        Map<String, Object> app = (Map<String, Object>) data.get("app");
        if (app.get("deprecated_feature") == null) {
            System.out.println("✓ PASS: Deleted deprecated_feature field");
        } else {
            System.out.println("✗ FAIL: deprecated_feature should have been deleted");
            allPass = false;
        }

        // Verify database merge
        Map<String, Object> database = (Map<String, Object>) app.get("database");
        boolean dbOk = database.get("pool_size") != null && database.get("pool_size").equals(20)
                && database.get("ssl_enabled") != null && database.get("ssl_enabled").equals(true)
                && database.get("replica_set") != null && database.get("replica_set").equals("enabled")
                && database.get("backup_enabled") != null && database.get("backup_enabled").equals(true)
                && database.get("connection_timeout") != null && database.get("connection_timeout").equals(60);
        if (dbOk) {
            System.out.println("✓ PASS: Database configuration merged correctly");
        } else {
            System.out.println("✗ FAIL: Database configuration not merged correctly");
            allPass = false;
        }

        // Verify image was updated
        if (app.get("image") != null && app.get("image").equals("myapp:2.0-prod")) {
            System.out.println("✓ PASS: Image updated to 2.0-prod");
        } else {
            System.out.println("✗ FAIL: Image should be 2.0-prod");
            allPass = false;
        }

        // Test 3: Conditional transformations (Production)
        System.out.println("\n--- Conditional Transformations (Production Environment) ---");

        if (app.get("replicas") != null && app.get("replicas").equals(5)) {
            System.out.println("✓ PASS: Production replicas set to 5");
        } else {
            System.out.println("✗ FAIL: Production replicas should be 5");
            allPass = false;
        }

        Map<String, Object> labels = (Map<String, Object>) metadata.get("labels");
        if (labels != null && labels.get("environment") != null && labels.get("environment").equals("production")) {
            System.out.println("✓ PASS: Environment label set to production");
        } else {
            System.out.println("✗ FAIL: Environment label should be production");
            allPass = false;
        }

        boolean monitoringOk = labels != null
                && labels.get("monitoring") != null && labels.get("monitoring").equals("enabled")
                && labels.get("tier") != null && labels.get("tier").equals("critical")
                && labels.get("sla") != null && labels.get("sla").equals("gold");
        if (monitoringOk) {
            System.out.println("✓ PASS: Production monitoring labels set correctly");
        } else {
            System.out.println("✗ FAIL: Production monitoring labels not correct");
            allPass = false;
        }

        if (app.get("permanent_config") != null && app.get("temp_config") == null) {
            System.out.println("✓ PASS: temp_config renamed to permanent_config");
        } else {
            System.out.println("✗ FAIL: temp_config should have been renamed");
            allPass = false;
        }

        // Test 4: Field preservation
        System.out.println("\n--- Field Preservation ---");
        if (actualContent.contains("localhost") && actualContent.contains("timeout: 30")) {
            System.out.println("✓ PASS: Original fields preserved");
        } else {
            System.out.println("✗ FAIL: Original fields should be preserved");
            allPass = false;
        }

        // Test 5: Environment field
        System.out.println("\n--- Environment Field ---");
        if (actualYaml.get("environment") != null && actualYaml.get("environment").equals("production")) {
            System.out.println("✓ PASS: Environment field set to production");
        } else {
            System.out.println("✗ FAIL: Environment field should be production");
            allPass = false;
        }

        System.out.println();
        if (allPass) {
            System.out.println("✓ All integration test checks PASSED");
            System.exit(0);
        } else {
            System.out.println("✗ Some integration test checks FAILED");
            System.exit(1);
        }
    }
}

