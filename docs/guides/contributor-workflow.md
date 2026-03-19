# Contributor Workflow Guide

This guide covers the three most common extension tasks in Legend Pure:

1. [Adding a new DSL extension](#1-adding-a-new-dsl-extension)
2. [Adding a new native Pure function](#2-adding-a-new-native-pure-function)
3. [Adding a new store connector](#3-adding-a-new-store-connector)

Before starting, ensure you have completed the
[Getting Started Guide](getting-started.md) and can build the project cleanly.

---

## 1. Adding a New DSL Extension

A DSL extension adds new syntax and metaclasses to the Pure language. Study the
`legend-pure-dsl-diagram` modules as the simplest reference implementation.

### Step 1 — Create the module structure

Every DSL follows a three-module pattern under `legend-pure-dsl/`:

```text
legend-pure-dsl-<name>/
    pom.xml                                          (aggregator POM)
    legend-pure-m2-dsl-<name>-pure/                  (Pure source — M2 metaclasses)
    legend-pure-m2-dsl-<name>-grammar/               (ANTLR4 grammar + Java visitor)
    legend-pure-runtime-java-extension-compiled-dsl-<name>/  (compiled engine extension)
```

### Step 2 — Define the Pure metamodel (`-pure` module)

1. Create `src/main/resources/platform/pure/` with `.pure` files defining your
   new metaclasses, associations, and functions at the M2 layer.
2. Create a `src/main/resources/<name>.definition.json` repository descriptor:

```json
{
  "name": "platform_dsl_<name>",
  "pattern": "(meta::pure::<name>)(::.*)?",
  "dependencies": [
    "platform"
  ]
}
```

1. In the module's `pom.xml`, bind `legend-pure-maven-compiler` (compile phase) and
   `legend-pure-maven-generation-par` (generate-sources phase):

```xml
<plugin>
    <groupId>org.finos.legend.pure</groupId>
    <artifactId>legend-pure-maven-compiler</artifactId>
</plugin>
<plugin>
    <groupId>org.finos.legend.pure</groupId>
    <artifactId>legend-pure-maven-generation-par</artifactId>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals><goal>build-pure-jar</goal></goals>
        </execution>
    </executions>
</plugin>
```

### Step 3 — Define the grammar (`-grammar` module)

1. Write an ANTLR4 grammar file in
   `src/main/antlr4/org/finos/legend/pure/m2/dsl/<name>/serialization/grammar/`.
2. Implement a Java visitor class that walks the parse tree and produces
   `CoreInstance` nodes. Use `DiagramParser` in `legend-pure-m2-dsl-diagram-grammar`
   as a reference.
3. Add `antlr4-maven-plugin` to the module's `pom.xml` to generate the lexer/parser.

### Step 4 — Implement the compiled extension (`-runtime-*-compiled` module)

1. Create a class implementing `CompiledExtension`:

```java
public class MyDslExtensionCompiled implements CompiledExtension
{
    @Override
    public SetIterable<String> getExtraCorePath()
    {
        // Return the CoreInstance factory paths generated for your DSL's metaclasses
        return MyDslCoreInstanceFactoryRegistry.ALL_PATHS;
    }

    @Override
    public String getRelatedRepository()
    {
        return "platform_dsl_<name>";   // must match definition.json "name"
    }

    public static CompiledExtension extension()
    {
        return new MyDslExtensionCompiled();
    }
}
```

1. Register the extension via Java `ServiceLoader`: create
   `src/main/resources/META-INF/services/org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension`
   containing the fully-qualified class name.

### Step 5 — Register the new DSL in the parent POM

Add the three new artifacts to the root `pom.xml` `<dependencyManagement>` block:

```xml
<dependency>
    <groupId>org.finos.legend.pure</groupId>
    <artifactId>legend-pure-m2-dsl-<name>-pure</artifactId>
    <version>${project.version}</version>
</dependency>
<!-- + grammar and compiled-extension artifacts -->
```

### Step 6 — Write tests

- Unit-test the grammar parser in the `-grammar` module.
- Write a compiled-mode integration test extending the pattern used in existing
  DSL test classes.
- If your DSL introduces new Pure functions, annotate them with `@PCT` and add
  PCT test coverage for both the compiled and interpreted engines.

---

## 2. Adding a New Native Pure Function

A *native function* is a Pure function whose implementation is provided in Java rather
than in Pure. Use this when performance or access to Java APIs makes a Pure
implementation impractical.

### Step 1 — Declare the function in Pure

Add a `.pure` file with the function signature marked `native`:

```pure
// in platform/pure/essential/mypackage/myFunction.pure
native function meta::pure::functions::mypackage::myFunction(input: String[1]): String[1];
```

Place it under the appropriate sub-directory of
`legend-pure-m3-core/src/main/resources/platform/pure/essential/`.

### Step 2 — Implement for the interpreted engine

In `legend-pure-runtime-java-engine-interpreted`, create a class that implements
`NativeFunction` (or extends `AbstractNativeFunction`). Register it in the
`PureRuntimeInterpreted` function-registry map, keyed on the Pure function's
fully-qualified name.

### Step 3 — Implement for the compiled engine

In the appropriate compiled extension module, add a Java method or class that
provides the compiled-mode implementation. The generated Java code will call this
method directly by name.

### Step 4 — Annotate with `@PCT` and add tests

```pure
function <<PCT.function>> meta::pure::functions::mypackage::myFunction(input: String[1]): String[1]
```

Add a PCT test class in both the interpreted and compiled engine test modules.
Both implementations must pass the same assertions.

---

## 3. Adding a New Store Connector

A store connector (like the existing relational store) adds a new persistence
back-end. This is the most involved extension type. Use `legend-pure-store-relational`
as the reference.

### Module structure

```text
legend-pure-store-<name>/
    pom.xml
    legend-pure-m2-store-<name>-pure/         (Pure model: Store, Connection, etc.)
    legend-pure-m2-store-<name>-grammar/      (ANTLR4 grammar for store definition syntax)
    legend-pure-runtime-java-extension-shared-store-<name>/   (engine-agnostic utilities)
    legend-pure-runtime-java-extension-compiled-store-<name>/ (compiled engine integration)
    legend-pure-runtime-java-extension-interpreted-store-<name>/ (interpreted engine)
```

### Key integration points

| Integration | Where to implement |
|------------|-------------------|
| Pure metaclasses for `Store`, `Connection`, `Binding` | `-pure` module `.pure` files |
| Grammar for the store definition syntax | `-grammar` ANTLR4 grammar + visitor |
| SQL / query generation | `-shared` module (engine-agnostic) |
| `CompiledExtension` registration | `-compiled` module |
| `InterpretedExtension` registration | `-interpreted` module |
| JDBC driver dependency | `-shared` or `-compiled` module `pom.xml` |

### Mapping integration

A store connector must integrate with the **Mapping DSL**. Implement the
`ClassMapping` and `PropertyMapping` visitors in the grammar module so that
mapping syntax for your store type is parsed correctly.

---

## Reference Implementations

| Extension type | Reference module |
|---------------|-----------------|
| Minimal DSL | `legend-pure-dsl-diagram` |
| DSL with execution semantics | `legend-pure-dsl-tds` |
| Store connector | `legend-pure-store-relational` |
| Native function (interpreted) | Any class in `legend-pure-runtime-java-engine-interpreted/src/main/java/.../natives/` |

---

*Back: [Getting Started Guide](getting-started.md) · See also: [Module Reference](../architecture/modules.md)*
