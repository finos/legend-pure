# Technology Stack & Key Dependencies

## 1. Java & Maven Version Requirements

| Toolchain | Required version |
|-----------|-----------------|
| JDK | **11** or **17** (enforced by `maven-enforcer-plugin`; `java.version.range=[11,12),[17,18)`) |
| Maven | **3.6+** (3.9.x recommended; `apache.maven.version=3.9.11` in parent POM) |
| Target bytecode | Java 8 (`maven.compiler.release=8`) — output JARs run on JRE 8+ |

> The CI pipeline (GitHub Actions) uses **JDK 17 (Temurin)**.

---

## 2. Dependency Version Management

All third-party library versions are declared **once** in the root `pom.xml`
`<properties>` block (e.g. `<eclipsecollections.version>10.2.0</eclipsecollections.version>`)
and pinned in the root `<dependencyManagement>` section.

Leaf modules reference dependencies **without version numbers** — they inherit the
managed version from the root POM. This approach:

- Guarantees convergence across all modules (enforced by `<dependencyConvergence/>` rule).
- Provides a single place to upgrade a library for the entire project.
- Avoids the need for a separate BOM artifact (the root POM acts as the BOM).

### How to Upgrade a Dependency

1. Change the version property in the root `pom.xml` `<properties>` block.
2. Run `mvn versions:display-dependency-updates` to verify no further outdated
   transitive versions remain.
3. Build and test: `mvn install`.

---

## 3. Major Third-Party Libraries

### Language & Collections

| Library | Version | Usage |
|---------|---------|-------|
| **Eclipse Collections** | 10.2.0 | Primary collection library throughout the codebase. Preferred over `java.util` collections for performance (`MutableList`, `MutableMap`, `ImmutableList`, `FastList`, etc.). |
| **Google Guava** | 33.4.6-jre | Utility methods where Eclipse Collections is insufficient; `ImmutableSet`, `Stopwatch`, etc. |

### Parsing

| Library | Version | Usage |
|---------|---------|-------|
| **ANTLR4** | 4.8-1 | Grammar-driven parser generator. Every DSL and the M3/M4 core grammar is expressed as an ANTLR4 `.g4` file. The `antlr4-maven-plugin` generates lexer/parser Java classes at build time. |

### JSON / Data Formats

| Library | Version | Usage |
|---------|---------|-------|
| **Jackson** (core, databind, annotations) | 2.10.5 / 2.10.5.1 | JSON serialization for configuration, PCT reports, and inter-service protocol. |
| **Jackson XML / YAML** | 2.10.5 | Supplementary data-format support (XML mappings, YAML config). |
| **JSON Simple** | 1.1.1 | Lightweight JSON reading in legacy areas; prefer Jackson for new code. |

### HTTP & Networking

| Library | Version | Usage |
|---------|---------|-------|
| **Apache HttpComponents** (client + core) | 4.5.13 / 4.4.9 | HTTP client used by the runtime engine shared module. |
| **commons-httpclient** | 3.1 | Legacy HTTP client; avoid in new code. |

### Databases (Test / Embedded)

| Library | Version | Usage |
|---------|---------|-------|
| **H2 Database** | 2.1.214 | Embedded JDBC database used in relational store integration tests. `h2Start.sh` in the root starts a server mode H2 instance for manual testing. |
| **DuckDB JDBC** | 1.0.0 | In-process analytical database used in relational store tests and TDS functions. |
| **Apache Tomcat DBCP** | 10.0.4 | JDBC connection pool used by the relational store runtime. |

### Observability

| Library | Version | Usage |
|---------|---------|-------|
| **SLF4J API** | 1.7.36 | Logging facade. All log statements must use SLF4J. |
| **jcl-over-slf4j** | 1.7.36 | Routes Apache Commons Logging to SLF4J. |
| **OpenTracing** (api, util, noop, jaxrs2) | 0.32.0 | Distributed tracing instrumentation. |

> **Logging constraint:** `log4j`, raw `org.slf4j` implementations, and
> `commons-logging` are banned as **direct** dependencies by the enforcer plugin.
> Only `slf4j-api` and `jcl-over-slf4j` are permitted. Runtime SLF4J backends are
> the responsibility of the consuming application.

### Serialization & Classpath Scanning

| Library | Version | Usage |
|---------|---------|-------|
| **ClassGraph** | 4.8.25 | Fast classpath/module-path scanning; used to discover `.pure` resource files and `*.definition.json` repository descriptors at runtime and build time. |
| **Deephaven CSV** | 0.18.0 | High-performance CSV parsing used by TDS functions. |
| **Apache Commons IO / Lang3 / Text** | 2.7 / 3.5 / 1.10.0 | Standard utility methods; file I/O, string operations. |
| **Commons Codec** | 1.15 | Base64 and hash utilities. |
| **Commons CSV** | 1.5 | CSV reading utilities. |

### Web / REST

| Library | Version | Usage |
|---------|---------|-------|
| **JAX-RS API** | 2.0.1 | REST endpoint annotations. |
| **Jersey** (test framework + Grizzly2) | 2.25.1 | Used **in tests only** to spin up a lightweight JAX-RS server for integration tests. |
| **Javax Servlet API** | 3.1.0 | Servlet container integration. |

### Security

| Library | Version | Usage |
|---------|---------|-------|
| **BouncyCastle** (bcpg, bcprov) | 1.67 | Cryptographic primitives; PGP signing support. |

### Maven Plugin Development

| Library | Version | Usage |
|---------|---------|-------|
| **Apache Maven Core / Model / Plugin API** | 3.9.11 | Base classes and annotations for the five custom Maven Mojos. |
| **Maven Plugin Annotations** | 3.15.2 | `@Mojo`, `@Parameter`, `@Component` annotations. |
| **Maven Resolver** (api, util) | 1.9.10 | Dependency resolution from within Mojo code. |
| **Eclipse Sisu Plexus** | 0.3.5 | Dependency injection container used by Maven internals. |

### Testing

| Library | Version | Usage |
|---------|---------|-------|
| **JUnit 4** | 4.13.1 | **Sole test framework**. All tests use JUnit 4 conventions (`@Test`, `Assert.*`, `@Rule`, `TemporaryFolder`). JUnit 5 is not used. |
| **Eclipse Collections Test Utils** | 10.2.0 | `Verify.*` assertion helpers for Eclipse Collections. |
| **Jersey Test Framework** | 2.25.1 | REST integration test container. |

---

## 4. External System Integrations

| System | How integrated | Notes |
|--------|---------------|-------|
| **H2 Database** (embedded) | JDBC (`com.h2database:h2`) | Used in relational store tests; `h2Start.sh` for server mode |
| **DuckDB** (embedded) | JDBC (`org.duckdb:duckdb_jdbc`) | Used in TDS and analytical query tests |
| **SonarCloud** | GitHub Actions (`-Psonar` profile on master) | Static analysis and code quality metrics; token in GitHub Secrets |
| **Maven Central** | `central-publishing-maven-plugin` | Artifact publishing via the Sonatype Central portal |
| **GitHub Actions** | `.github/workflows/build.yml` | CI/CD; builds, tests, uploads Surefire XML, optional Sonar scan |

---

## 5. Technology Rationale

| Choice | Rationale |
|--------|-----------|
| **Eclipse Collections over java.util** | Significantly richer API (primitive maps, immutable factories, lazy evaluation); better performance for the large in-memory graph structures the compiler builds. |
| **ANTLR4 for parsing** | Industry-standard grammar tool; visitor/listener pattern integrates cleanly with the M3 compiler architecture; `treatWarningsAsErrors=true` ensures grammar quality. |
| **JUnit 4 (not 5)** | Existing investment in JUnit 4 across the entire codebase. Migrating to JUnit 5 is a future concern; new tests must match existing conventions. |
| **Java 8 bytecode target** | Ensures the compiled JARs can be consumed by downstream Legend components that may still run on JRE 8. |
| **No mocking framework** | Established project convention: prefer real objects or hand-written stubs to avoid Mockito as a dependency and keep test setup explicit. |
| **PAR archive format** | Avoids full re-parse on startup; binary snapshots of compiled repositories load 10–100× faster than re-parsing source. |

---

*Back: [Module Reference](modules.md) · Next: [Domain & Key Concepts](domain-concepts.md)*
