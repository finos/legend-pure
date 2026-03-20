# Codebase Exploration & Discovery Guide

This guide helps new engineers systematically understand the Legend Pure codebase.
Follow the phases in order; each phase builds on the previous one.

---

## Phase 1 — Orientation (Day 1)

### 1.1 Read the Architecture Docs First

Before touching code:

1. [Architecture Overview](../architecture/overview.md) — understand what the project does and its component map.
2. [Module Reference](../architecture/modules.md) — understand every module's purpose.
3. [Technology Stack](../architecture/tech-stack.md) — note the key libraries and why they were chosen.
4. [Domain & Key Concepts](../architecture/domain-concepts.md) — learn the M4/M3/M2/M1 layer model and the glossary.

### 1.2 Build the Project

```bash
mvn install -DskipTests
```

Confirm all modules build cleanly. Any build error here is a setup issue — see
[Troubleshooting](getting-started.md#8-troubleshooting-common-setup-issues).

### 1.3 Understand the Reactor Order

```bash
mvn install -DskipTests --no-transfer-progress 2>&1 | grep "Building Legend"
```

This prints every module in the order Maven builds it. The order reflects the
dependency graph.

---

## Phase 2 — Dependency Mapping (Days 1–2)

### 2.1 Dependency Tree

Generate the full tree for the root module:

```bash
mvn dependency:tree > /tmp/dep-tree.txt
cat /tmp/dep-tree.txt | grep "legend-pure" | sort -u
```

Per-module tree:

```bash
mvn dependency:tree -pl legend-pure-core/legend-pure-m3-core
```

### 2.2 Dependency Analysis

Find unused/undeclared dependencies in a module:

```bash
mvn dependency:analyze -pl legend-pure-core/legend-pure-m3-core
```

Run for all modules at once:

```bash
mvn dependency:analyze-report
```

### 2.3 Detect Version Conflicts

```bash
mvn dependency:tree -Dverbose 2>&1 | grep "omitted for conflict" | sort -u
```

All version conflicts should be resolved — the enforcer's `<dependencyConvergence/>`
rule means the build will fail if any exist in the declared dependency graph.

---

## Phase 3 — Code Structure Exploration (Days 2–5)

### 3.1 Start at the Entry Points

The two most important entry-point classes for understanding the system:

| Class | Module | What it does |
|-------|--------|-------------|
| `org.finos.legend.pure.m3.compiler.Compiler` | `legend-pure-m3-core` | The top-level Pure compiler entry point |
| `org.finos.legend.pure.runtime.java.compiled.CompiledExecutionSupport` | `legend-pure-runtime-java-engine-compiled` | The compiled-mode execution entry point |
| `org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport` | `legend-pure-runtime-java-engine-interpreted` | The interpreted-mode execution entry point |

Use IntelliJ **Call Hierarchy (Ctrl+Alt+H)** on these classes to trace inward.

### 3.2 Trace the Compilation Pipeline

Follow a single Pure source file through the pipeline:

1. Find an ANTLR4 grammar file (e.g. `M3.g4` in `legend-pure-m3-core/src/main/antlr4`).
2. Find the corresponding Java visitor class that implements it.
3. Find where the visitor is invoked (search for `new M3AntlrParser()` or similar).
4. Follow the output (a `CoreInstance` graph node) into the compiler passes.

```bash
# Find all ANTLR4 grammar files
find . -name "*.g4" | sort
```

### 3.3 Understand the CoreInstance Graph

`CoreInstance` is the universal node type. Understand its key methods:

```java
CoreInstance.getValueForMetaPropertyToOne(String propertyName)
CoreInstance.getValueForMetaPropertyToMany(String propertyName)
CoreInstance.getName()
CoreInstance.getClassifier()
```

Search for usages with **Find Usages (Alt+F7)** on `CoreInstance`.

### 3.4 Explore the Test Suite

Tests are the best executable documentation:

```bash
# Find all test classes
find . -path "*/test/java/**/*Test*.java" | sort

# Count tests per module
find . -path "*/test/java/**/*Test*.java" | \
  sed 's|/src/test/java.*||' | sort | uniq -c | sort -rn
```

Run a single test class to understand its coverage area:

```bash
mvn test -pl legend-pure-core/legend-pure-m3-core \
  -Dtest=TestM3Compiler -DfailIfNoTests=false
```

### 3.5 PCT (Platform Compatibility Tests)

PCT tests verify that both the compiled and interpreted engines produce identical
results for a standard set of Pure functions. They are the integration tests between
the language layer and the runtime.

```bash
# Find all PCT test classes
find . -name "*_PCT*.java" | sort
```

---

## Phase 4 — Maven Plugin Exploration (Days 3–4)

### 4.1 Read the Plugin Review Document

The [Maven Plugins Reference](../reference/maven-plugins-reference.md)
is an authoritative guide to all five custom Maven plugins. Read sections 1 and 2 first.

### 4.2 Trace a Plugin Execution

Pick one plugin and trace its execution:

1. Open the `@Mojo`-annotated class (e.g. `PureCompilerMojo`).
2. Find the `execute()` method.
3. Follow the call to the shared library utility (`ProjectDependencyResolution`).
4. Follow the delegate call into the core/runtime module.

### 4.3 Inspect What the Plugins Produce

```bash
# After a build, look at what compile-pure generates
find legend-pure-core -path "*/target/classes/*.json" -o -path "*/target/classes/*.dat" | head -20

# What build-pure-jar generates
find . -name "*.par" | sort

# What build-pure-compiled-jar generates
find . -path "*/target/generated-sources/**/*.java" | head -20
```

---

## Phase 5 — Architectural Analysis Tools (Week 2)

### 5.1 ArchUnit (Recommended)

ArchUnit can be added as a test dependency to write architectural rules as tests.
Example rules that would be valuable for this project:

```java
// No module below runtime should depend on the compiled engine
noClasses()
    .that().resideInAPackage("..m3..")
    .should().dependOnClassesThat()
    .resideInAPackage("..compiled..");

// CoreInstance must not depend on DSL classes
noClasses()
    .that().resideInAPackage("..m4..")
    .should().dependOnClassesThat()
    .resideInAPackage("..dsl..");
```

### 5.2 IntelliJ Module Diagram

1. In IntelliJ, right-click the project root → **Diagrams → Show Diagram**.
2. Select **Maven Module Dependencies**.
3. Export as SVG for the docs.

### 5.3 Structure101 / JDepend

For deeper package-level dependency analysis:

```bash
# JDepend (if installed)
jdepend legend-pure-core/legend-pure-m3-core/target/classes

# Or use the Maven JDepend report plugin
mvn jdepend:generate
```

---

## Phase 6 — Tribal Knowledge Capture (Ongoing)

### Suggested Interview Questions for Existing Team Members

Use these as a guide when pairing with senior engineers:

1. **Why does the build have two `maven-compiler-plugin` executions in `legend-pure-m3-core`?**
   *(Bootstrap / generated sources ordering.)*
2. **What is the difference between a PAR file and a binary element file?**
   *(PAR = legacy text/binary snapshots; binary elements = new binary format for `PureCompilerLoader`.)*
3. **When should I add code to the compiled extension vs the shared extension?**
   *(Compiled extensions are only loaded in AOT mode; shared extensions run in both.)*
4. **What triggers a full re-compile of Pure source vs. loading from PAR cache?**
5. **What is the significance of the `definition.json` file in a module?**
6. **Why is JUnit 4 used instead of JUnit 5?**
   *(Project convention; migration is out of scope for current work.)*
7. **What is PCT and why does it matter?**
   *(Platform Compatibility Testing — guarantees behavioral parity between compiled and interpreted modes.)*

### Capturing Knowledge

Document each answer as a new section in the relevant `docs/` page or as an FAQ entry.

---

## Module README Standardization

Every module should have its own `README.md` following the
[Module README Template](../templates/module-readme-template.md).
When exploring a module, check whether a README exists; if not, create one as part
of your exploration.

---

*Back: [Getting Started Guide](getting-started.md) · Next: [Coding Standards](../standards/coding-standards.md)*
