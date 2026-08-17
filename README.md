# YAML Rewrite Maven Plugin

A Maven plugin for transforming YAML files using a declarative specification. 
Read a source YAML file, apply transformations, and write the result to an output file (or modify in place).

## Features

- **Transformation** - Multiple operations (delete, set, rename, merge) on YAML structures
- **Dot Notation Paths** - Use simple dot notation to navigate nested structures (e.g., `spec.replicas.count`)
- **In-place Modification** - Optionally modify the source file directly
- **YAML Specification** - Define transformations using human-readable YAML format

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<plugin>
  <groupId>com.github.samblake.yamlrewrite</groupId>
  <artifactId>yamlrewrite-maven-plugin</artifactId>
  <version>1.0</version>
  <executions>
    <execution>
      <goals>
        <goal>rewrite</goal>
      </goals>
      <configuration>
        <sourceFile>src/main/resources/config.yaml</sourceFile>
        <transformationFile>src/main/resources/transformations.yaml</transformationFile>
        <outputFile>target/config-transformed.yaml</outputFile>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Configuration

| Parameter | Required | Description |
|-----------|----------|-------------|
| `sourceFile` | Yes | Path to the source YAML file to transform |
| `transformationFile` | Yes | Path to the YAML file containing transformation specifications |
| `outputFile` | No | Path to the output file. If not specified, the source file is modified in place |


## Transformation Specification Format

The transformation file is a YAML file containing a list of transformation operations. 
Each operation specifies what action to perform and which path(s) to target.

```yaml
transformations:
  - operation: delete
    path: spec.deprecatedField
  - operation: set
    path: spec.replicas
    value: 3
  - operation: rename
    from: spec.oldName
    to: spec.newName
  - operation: merge
    path: spec
    value:
      timeout: 30
      retries: 3
```

## Operations

### Delete

Removes a key and its value from the YAML structure.

```yaml
- operation: delete
  path: spec.oldField
```

**Example:**
- Source: `{spec: {replicas: 1, deprecated: true}}`
- Transformation: `operation: delete, path: spec.deprecated`
- Result: `{spec: {replicas: 1}}`

### Set

Sets or updates a value at the specified path. Creates intermediate structures as needed.

```yaml
- operation: set
  path: spec.replicas
  value: 3
```

**Example:**
- Source: `{spec: {name: myapp}}`
- Transformation: `operation: set, path: spec.replicas, value: 3`
- Result: `{spec: {name: myapp, replicas: 3}}`

### Rename

Renames a key while preserving its value. The value is moved to the new path and the old path is deleted.

```yaml
- operation: rename
  from: spec.oldName
  to: spec.newName
```

**Example:**
- Source: `{spec: {oldName: myapp}}`
- Transformation: `operation: rename, from: spec.oldName, to: spec.newName`
- Result: `{spec: {newName: myapp}}`

### Merge

Recursively merges a map into the existing structure at a path. For nested maps, performs a deep merge where new keys are added and existing keys are preserved (unless they're also maps, in which case the merge continues recursively).

```yaml
- operation: merge
  path: spec
  value:
    timeout: 30
    retries: 3
```

**Example:**
- Source: `{spec: {replicas: 1, name: myapp}}`
- Transformation: `operation: merge, path: spec, value: {timeout: 30, retries: 3}`
- Result: `{spec: {replicas: 1, name: myapp, timeout: 30, retries: 3}}`

**Deep Merge Example:**
- Source: `{config: {database: {host: localhost, credentials: {user: admin}}}}`
- Transformation: `operation: merge, path: config.database, value: {port: 5432, credentials: {password: secret}}`
- Result: `{config: {database: {host: localhost, port: 5432, credentials: {user: admin, password: secret}}}}`

**Important Notes:**
- If the path doesn't exist, it will be created
- If the path exists but contains a non-map value, it will be replaced with the merged value
- The merge is deep - nested maps are merged recursively
- Original values are preserved during the merge

## Path Syntax

Paths use dot notation to navigate nested YAML structures:

- `spec.replicas` - Access nested key `replicas` under `spec`
- `metadata.labels.app` - Access deeply nested structures
- `root` - Access top-level keys

Paths are case-sensitive and must match exactly.

## Usage Examples

### Example

**Source YAML** (`config.yaml`):
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  labels:
    version: v1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
      - name: app
        image: my-image:1.0
        deprecatedFlag: true
```

**Transformation File** (`transformations.yaml`):
```yaml
transformations:
  - operation: set
    path: metadata.labels.environment
    value: production
  - operation: set
    path: spec.replicas
    value: 3
  - operation: set
    path: spec.template.spec.containers.0.image
    value: my-image:2.0
  - operation: delete
    path: spec.template.spec.containers.0.deprecatedFlag
```

**Result**:
- Replicas changed from 1 to 3
- New environment label added
- Container image updated
- Deprecated flag removed

## Testing

### Unit Tests

Run the unit test suite:

```bash
mvn test
```

### Tests

Run the full test suite includes unit tests)

```bash
mvn -Prun-its verify
```
