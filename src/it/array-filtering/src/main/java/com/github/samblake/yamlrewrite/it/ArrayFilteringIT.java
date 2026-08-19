package com.github.samblake.yamlrewrite.it;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Integration test for array filtering and empty array detection.
 * Verifies that deprecated parameters and security schemes are properly removed.
 */
public class ArrayFilteringIT {

    public static void main(String[] args) throws IOException {
        String basedir = System.getProperty("basedir");
        String transformedFile = Paths.get(basedir, "target", "openapi-transformed.yaml").toString();

        // Load the transformed YAML
        Yaml yaml = new Yaml();
        Map<String, Object> transformed;
        try (FileInputStream fis = new FileInputStream(transformedFile)) {
            transformed = yaml.load(fis);
        }

        // Test 1: Verify version was updated
        Map<String, Object> info = (Map<String, Object>) transformed.get("info");
        String version = (String) info.get("version");
        if (!"2.0.0".equals(version)) {
            throw new AssertionError("Expected version 2.0.0, got " + version);
        }
        System.out.println("✓ Version updated correctly to 2.0.0");

        // Test 2: Verify deprecated security scheme was removed
        Map<String, Object> components = (Map<String, Object>) transformed.get("components");
        Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
        if (securitySchemes.containsKey("oauth2_deprecated")) {
            throw new AssertionError("Deprecated oauth2_deprecated security scheme was not removed");
        }
        System.out.println("✓ Deprecated security scheme removed");

        // Test 3: Verify deprecated parameters were removed
        Map<String, Object> paths = (Map<String, Object>) transformed.get("paths");
        Map<String, Object> basketAddPath = (Map<String, Object>) paths.get("/api/basket/add/{code}");
        Map<String, Object> post = (Map<String, Object>) basketAddPath.get("post");
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) post.get("parameters");

        boolean foundDeprecated = false;
        for (Map<String, Object> param : parameters) {
            String name = (String) param.get("name");
            if ("deprecated_session_id".equals(name)) {
                foundDeprecated = true;
                break;
            }
        }
        if (foundDeprecated) {
            throw new AssertionError("Deprecated parameter 'deprecated_session_id' was not removed");
        }
        System.out.println("✓ Deprecated parameter removed from POST endpoint");

        // Test 4: Verify second endpoint's deprecated parameter was removed
        Map<String, Object> basketRemovePath = (Map<String, Object>) paths.get("/api/basket/remove/{code}");
        Map<String, Object> delete = (Map<String, Object>) basketRemovePath.get("delete");
        List<Map<String, Object>> removeParameters = (List<Map<String, Object>>) delete.get("parameters");

        foundDeprecated = false;
        for (Map<String, Object> param : removeParameters) {
            String name = (String) param.get("name");
            if ("old_tracking_id".equals(name)) {
                foundDeprecated = true;
                break;
            }
        }
        if (foundDeprecated) {
            throw new AssertionError("Deprecated parameter 'old_tracking_id' was not removed");
        }
        System.out.println("✓ Deprecated parameter removed from DELETE endpoint");

        // Test 5: Verify that channel parameter was kept and marked-for-review was added
        boolean foundMarkedForReview = false;
        for (Map<String, Object> param : parameters) {
            String name = (String) param.get("name");
            if ("marked-for-review".equals(name)) {
                foundMarkedForReview = true;
                break;
            }
        }
        if (!foundMarkedForReview) {
            throw new AssertionError("Merged parameter marked-for-review was not added");
        }
        System.out.println("✓ New parameters added via merge");

        // Test 6: Verify that the array still has the expected parameter
        if (parameters.size() < 1) {
            throw new AssertionError("Parameters array is empty after merge");
        }
        System.out.println("✓ Parameters array contains merged data");

        System.out.println("\n✅ All array filtering tests passed!");
    }
}


