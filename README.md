# YAML Rewrite Maven Plugin

A Maven plugin for transforming YAML files using a declarative specification. 
Read a source YAML file, apply transformations, and write the result to an output file (or modify in place).

## Features

- **Transformation** - Multiple operations (delete, set, rename, merge) on YAML structures
- **Conditional Operations** - Apply operations only when specific conditions are met
- **Dot Notation Paths** - Use simple dot notation to navigate nested structures (e.g., `spec.replicas.count`)
- **In-place Modification** - Optionally modify the source file directly
- **YAML Specification** - Define transformations using human-readable YAML format

## Installation

Add the plugin to your project's `pom.xml`:

```xml
<plugin>
  <groupId>com.github.samblake.yamlrewrite</groupId>
  <artifactId>yamlrewrite-maven-plugin</artifactId>
  <version>1.2</version>
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
    path: spec.*.replicas
    value: 3
  - operation: rename
    from: spec.oldName
    to: spec.newName
    when:
      type: equals
      path: environment
      value: dev
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

### Wildcard Patterns

Paths support wildcard patterns using `*` to match any key at that level.

Wildcard patterns work with all operations (delete, set, merge). When a wildcard matches multiple paths, the operation is applied to each matching path.

## Conditional Operations

All operations support optional conditions that determine whether they should be executed. Use the `when` clause to specify a condition.

### String Match Condition

Check if a value at a given path equals a specific string value.

```yaml
- operation: set
  path: config.debug
  value: true
  when:
    type: equals
    path: environment
    value: dev
```

## Example

**Source YAML** (`deployment.yaml`):
```yaml
environment: production
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  labels:
    version: v1
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: app
        image: my-image:1.0
        debug: false
```

**Transformation File** (`transformations.yaml`):
```yaml
transformations:
  # Always update the version
  - operation: set
    path: metadata.labels.version
    value: v2

   # Only increase replicas in production
   - operation: set
     path: spec.replicas
     value: 5
     when:
       type: equals
       path: environment
       value: production

   # Only enable debug in dev
   - operation: set
     path: spec.template.spec.containers.0.debug
     value: true
     when:
       type: equals
       path: environment
       value: dev

   # Only use production image in production
   - operation: set
     path: spec.template.spec.containers.0.image
     value: my-image:prod
     when:
       type: equals
       path: environment
       value: production
```

**Result** (with production environment):
```yaml
environment: production
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  labels:
    version: v2
spec:
  replicas: 5
  template:
    spec:
      containers:
       - name: app
         image: my-image:prod
         debug: false
```

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
