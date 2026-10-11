# Developer Getting Started Guide

## 1. Prerequisites

Install the following tools before cloning the project:

| Tool | Required version | Notes |
|------|-----------------|-------|
| **JDK** | 11 or 17 | Temurin/OpenJDK recommended. The enforcer rejects 8, 12–16, and 18+. |
| **Maven** | 3.6+ (3.9.x preferred) | `mvn -version` to verify. |
| **Git** | Any recent version | |
| **IntelliJ IDEA** | 2023.x+ (Community or Ultimate) | Strongly recommended. Eclipse works but is less supported. |
| **Docker** *(optional)* | Any recent version | Only needed if you want to run H2 in server mode via `h2Start.sh`. |

### Verifying Your Java Version

```bash
java -version      # must print 11.x.x or 17.x.x
mvn -version       # must print 3.6.x or higher
```

The Maven enforcer will **fail the build** if you use any other JDK version.

---

## 2. Clone and First Build

```bash
# 1. Clone the repository
git clone https://github.com/finos/legend-pure.git
cd legend-pure

# 2. (Optional) Pre-fetch all dependencies while offline-capable
mvn de.qaware.maven:go-offline-maven-plugin:resolve-dependencies

# 3. Build without tests (fastest first build)
mvn -T 4 install -DskipTests

# 4. Full build including all tests
mvn -T 4 install
```

> **Expected first-build time:** 15–30 minutes depending on hardware and whether the
> Maven local repository is warm. Subsequent incremental builds are significantly faster.

### What Happens During `mvn install`

1. **`initialize`** — `build-helper-maven-plugin` registers `target/generated-sources/`
   as a source root.
2. **`generate-sources`** — ANTLR4 generates parser/lexer Java classes from `.g4`
   grammars; `legend-pure-maven-generation-platform-java` generates M3 `CoreInstance`
   Java accessor files.
3. **`compile`** — `maven-compiler-plugin` compiles Java source; `legend-pure-maven-compiler`
   compiles Pure source to binary elements.
4. **`test-compile`** — Test sources compiled; `maven-dependency-plugin` validates
   declared vs used dependencies.
5. **`test`** — `maven-surefire-plugin` runs JUnit 4 tests; JaCoCo collects coverage.
6. **`verify`** — `maven-checkstyle-plugin` enforces code style.
7. **`install`** — JARs and plugins are installed to `~/.m2/repository`.

---

## 3. Running Specific Parts of the Build

### Run Only Unit Tests for a Single Module

```bash
mvn test -pl legend-pure-core/legend-pure-m3-core
```

### Skip Tests

```bash
mvn install -DskipTests
```

### Skip Checkstyle

```bash
mvn install -Dcheckstyle.skip=true
```

### Build a Specific Module and Its Dependencies

```bash
mvn install -pl legend-pure-store/legend-pure-store-relational -am
```

### Clean Everything and Rebuild

```bash
mvn clean install
```

---

## 4. Running in Different Modes / Profiles

### Default (No Profile)

Runs the full build + tests without Sonar integration. This is what you use locally.

```bash
mvn install
```

### Sonar Profile

Used automatically by CI on the `master` branch. Runs SonarCloud analysis.
Requires the `SONAR_TOKEN` environment variable (available in GitHub Secrets only).

```bash
mvn install -Psonar   # requires SONAR_TOKEN env var
```

### Parallel Build (Faster CI)

Mirrors how CI runs the build. Use with caution locally — some modules have
implicit ordering requirements.

```bash
mvn install -DforkCount=3 -DreuseForks=true
```

---

## 5. IDE Setup (IntelliJ IDEA)

1. Open IntelliJ → **File → Open** → select the `legend-pure` directory.
2. IntelliJ detects the root `pom.xml` and imports all Maven modules automatically.
3. Accept the Maven import dialog. IntelliJ will index all modules.
4. Set **Project SDK** to JDK 11 or 17:
   **File → Project Structure → Project → SDK**.
5. Enable annotation processing:
   **Settings → Build → Compiler → Annotation Processors → Enable annotation processing**.
6. Run a full Maven build from the terminal (step 2 above) **before** running tests
   inside IntelliJ so that all generated sources exist on disk.
7. Reload Maven: right-click the root `pom.xml` → **Maven → Reload project**.

### Useful IntelliJ Features for This Project

- **Module diagram:** Right-click a module in the Project view → **Diagrams →
  Show Diagram** to visualise module dependencies.
- **Find Usages (Alt+F7):** Trace where a `CoreInstance` type or interface is used.
- **Call Hierarchy (Ctrl+Alt+H):** Understand call chains in the compiler passes.
- **Search Structurally (Edit → Find → Search Structurally):** Find ANTLR4 visitor
  method implementations.

---

## 6. Common Environment Variables

| Variable | Used where | Purpose |
|----------|-----------|---------|
| `MAVEN_OPTS` | Shell / `.profile` | JVM flags for the Maven process. CI uses `-XX:MaxRAMPercentage=25.0`. Locally, `-Xmx4g` is a reasonable starting point. |
| `CI_DEPLOY_USERNAME` | GitHub Actions secret | Maven Central publishing username. |
| `CI_DEPLOY_PASSWORD` | GitHub Actions secret | Maven Central publishing password. |
| `SONAR_TOKEN` | GitHub Actions secret | SonarCloud authentication. |

---

## 7. Working with H2 (Local Relational Tests)

The project ships a helper script for starting H2 in server mode:

```bash
bash h2Start.sh   # starts H2 server on default port 9092
```

Most relational integration tests use H2 in **embedded mode** and need no running
server. Server mode is only needed if you want to inspect the in-memory database
with an external SQL client during a debugging session.

---

## 8. Troubleshooting Common Setup Issues

### Build Fails with "Unsupported class file major version"

Your JDK version is not 11 or 17. Switch JDK and rerun.

```bash
java -version    # confirm the active version
update-alternatives --config java   # Linux: switch version
```

### `[ERROR] Failed to execute goal org.apache.maven.plugins:maven-enforcer-plugin ... requireJavaVersion`

Same issue — wrong JDK. See above.

### Generated Sources Not Found (`cannot find symbol: class CoreInstance...`)

The `legend-pure-maven-generation-platform-java` plugin generates Java files that
`maven-compiler-plugin` then compiles. If you run `mvn test` without a prior
`mvn generate-sources`, these files won't exist. Fix:

```bash
mvn generate-sources -pl legend-pure-core/legend-pure-m3-core
# or just:
mvn install -DskipTests
```

### OutOfMemoryError / GC Overhead Limit Exceeded

The compiler builds large in-memory graphs. Increase Maven heap:

```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxRAMPercentage=60.0"
mvn install
```

### Checkstyle Failure: "File does not contain a valid Copyright header"

Every Java, XML, and properties file must contain a copyright header. Add:

```java
// Copyright <YEAR> Goldman Sachs (or your organisation)
//
// Licensed under the Apache License, Version 2.0 (the "License");
// ...
```

### `maven-dependency-plugin` Warns About Unused / Undeclared Dependencies

Add or remove `<dependency>` entries in the relevant module's `pom.xml`. The
plugin runs at `test-compile` phase. Use `mvn dependency:analyze` to get a
full report.

### IntelliJ Shows Red Errors After Maven Reload

Ensure you have run `mvn generate-sources` (or a full `mvn install -DskipTests`)
so that ANTLR4-generated and platform-generated Java files are present in
`target/generated-sources/`. Then **File → Invalidate Caches → Restart**.

---

*Back: [Architecture Overview](../architecture/overview.md) · Next: [Build & CI Guide](build-and-ci.md)*
