package com.github.samblake.yamlrewrite.it;
import java.io.File;
import java.nio.file.Files;
/**
 * Integration test that verifies the yamlrewrite-maven-plugin
 * correctly transforms a real YAML file through Maven.
 */
public class BasicTransformationIT {
    public static void main(String[] args) throws Exception {
        String basedir = System.getProperty("basedir", ".");
        File outputFile = new File(basedir, "target/config-transformed.yaml");
        if (!outputFile.exists()) {
            System.err.println("ERROR: Output file does not exist at: " + outputFile.getAbsolutePath());
            System.exit(1);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        System.out.println("=== Output File Content ===");
        System.out.println(content);
        System.out.println("============================");
        boolean allPass = true;
        // Verify all transformations were applied
        String[][] checks = {
            {"replicas: 3", "Set replicas to 3"},
            {"myapp:2.0", "Update image to 2.0"},
            {"environment: production", "Update environment to production"},
            {"pool_size: 20", "Merge in pool_size"},
            {"ssl_enabled: true", "Merge in ssl_enabled"}
        };
        for (String[] check : checks) {
            if (content.contains(check[0])) {
                System.out.println("✓ PASS: " + check[1]);
            } else {
                System.out.println("✗ FAIL: " + check[1]);
                System.out.println("  Expected to find: " + check[0]);
                allPass = false;
            }
        }
        // Verify deprecated field was deleted
        if (!content.contains("deprecated_feature")) {
            System.out.println("✓ PASS: Deleted deprecated_feature field");
        } else {
            System.out.println("✗ FAIL: deprecated_feature should have been deleted");
            allPass = false;
        }
        // Verify original fields are preserved
        if (content.contains("myapp") && content.contains("localhost") && content.contains("timeout: 30")) {
            System.out.println("✓ PASS: Original fields preserved");
        } else {
            System.out.println("✗ FAIL: Original fields should be preserved");
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
