# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Legend Pure is the language and compiler engine powering the FINOS Legend data-management platform. This repo ships the Pure language, its compiler, two execution engines (compiled + interpreted), DSL extensions, a relational store, and the Maven plugin suite that drives Pure compilation during normal Maven builds.

Group ID: `org.finos.legend.pure`. Current version: `5.81.1-SNAPSHOT` (root `pom.xml`). Downstream consumer: `legend-engine` compiles against these JARs, so public Java API in `legend-pure-m4`, `legend-pure-m3-core`, and runtime engine modules is a **breaking-change surface for the whole Legend stack**.

## Build & test commands

Requires **JDK 11 or 17** (enforcer rejects anything else) and Maven 3.6+.

```bash
# First build, skip tests (15–30 min warm repo, longer cold)
mvn -T 4 clean install -DskipTests

# Full build + tests
mvn clean install

# Single module
mvn test -pl legend-pure-core/legend-pure-m3-core

# Single test class / method (remember -DfailIfNoTests=false — most modules don't
# have a match and will fail without it)
mvn test -pl legend-pure-core/legend-pure-m3-core \
    -Dtest=TestM3Compiler -DfailIfNoTests=false
mvn test -pl legend-pure-core/legend-pure-m3-core \
    -Dtest="TestM3Compiler#testSimpleClass" -DfailIfNoTests=false

# PCT tests only (pattern-matched)
mvn test -Dtest="*_PCT" -DfailIfNoTests=false

# Checkstyle without tests
mvn verify -DskipTests
mvn checkstyle:check
# Skip checkstyle in a hurry
mvn clean install -Dcheckstyle.skip=true

# Build a module and its dependencies
mvn clean install -pl legend-pure-store/legend-pure-store-relational -am
```

If you see `cannot find symbol: class CoreInstance...`, you ran `mvn test` without prior code generation. Run `mvn generate-sources` or `mvn install -DskipTests` first — ANTLR4 parsers and `CoreInstance` accessors live in `target/generated-sources/` and must exist before `maven-compiler-plugin` runs.

Heap: the compiler builds large graphs. For local builds, `export MAVEN_OPTS="-Xmx4g"`.

macOS JDK switch: `export JAVA_HOME=$(/usr/libexec/java_home -v 11) PATH="$JAVA_HOME/bin:$PATH"`. The enforcer rejects 25, 21, 12–16, 18+ — must be 11 or 17.

After editing a `.pure` file, the compiled engine's per-function generated Java under `target/generated-test-sources/` can go stale. Run `mvn clean install -DskipTests -pl <engine-module> -am` (or just delete `target/generated-test-sources/`) to regenerate before `mvn test`.

When passing `-Dtest=...` with `-am`, add `-Dsurefire.failIfNoSpecifiedTests=false` — dependency modules without matching tests otherwise fail the reactor.

## Architecture — the M4 → M3 → M2 → M1 stack

The metamodel-layer split is **the** key concept for navigating this codebase:

- **M4** (`legend-pure-m4`) — meta-metamodel. Defines what a "node" is (`CoreInstance` interface, serialization primitives). Rarely touched.
- **M3** (`legend-pure-m3-core`) — the Pure language itself: `Class`, `Function`, `Association`, parser (ANTLR4 `M3.g4` + `M3AntlrParser`), compiler passes, standard library.
- **M2** (`legend-pure-dsl/*`, `legend-pure-store/*`) — DSL extensions built *in Pure*: mapping, diagram, graph, path, store, tds, relational. Each DSL ships three sub-modules: `*-pure` (Pure source), `*-grammar` (ANTLR4 + Java visitor), `*-runtime-*-extension` (compiled/interpreted runtime hook).
- **M1** — user/business Pure code. Lives in consumer repos, not here.

Rule of thumb: editing `m4` = changing what a node *is*; editing `m3-core` = changing what `Class` or `Function` *means*; editing a DSL module = changing what `Mapping` or `Database` *means*.

### Two execution modes (kept in lockstep by PCT)

Both modes compile from the same `CoreInstance` graph produced by the M3 compiler:

- **Compiled** (`legend-pure-runtime-java-engine-compiled`) — ahead-of-time codegen to Java during Maven build. Production path.
- **Interpreted** (`legend-pure-runtime-java-engine-interpreted`) — tree-walking interpreter at runtime. Dev/IDE path.

**PCT (Platform Compatibility Testing)** is the integration contract that keeps these engines identical: Pure functions carrying the `<<PCT.function>>` stereotype (with `<<PCT.test>>` tests) run on *both* engines every build; divergent results fail CI. Tests follow `Test_<Mode>_<Suite>_PCT` naming. The current branch (`pct-refactor`) is migrating Java-side abstract test classes into native Pure PCT — recent commits named `test(grammar): PCT … migration` are deleting `AbstractTest*.java` under `legend-pure-m3-core/src/test/java/.../function/base/` and replacing them with Pure PCT functions in `legend-pure-m3-core/src/main/resources/platform/pure/`.

### Maven plugin pipeline (driven by `legend-pure-maven/*`)

Pure compilation runs as part of the normal Maven lifecycle, not a separate step:

1. `legend-pure-maven-generation-platform-java` (phase `compile`) — generates M3 `CoreInstance` Java accessors.
2. `legend-pure-maven-compiler` (`compile`) — parses `.pure` files to binary elements.
3. `legend-pure-maven-generation-par` — serializes compiled repos to PAR archives (the build/startup cache).
4. `legend-pure-maven-generation-java` — emits Java source + bytecode for the compiled engine.
5. `legend-pure-maven-generation-pct` — produces PCT function index + reports.

These plugin goals and parameter names are part of the public contract consumed by `legend-engine`.

## Conventions

- **Collections:** Eclipse Collections throughout. Use `Lists.mutable.empty()` / `Lists.immutable.with(...)`; return `RichIterable` / `ListIterable` from APIs, not `java.util.List`. Java collections only when bridging to external APIs.
- **Testing framework:** JUnit 4 only (`junit:junit:4.13.1`). **Do not introduce JUnit 5.** No mocking framework — hand-written stubs, real objects, or `TemporaryFolder`. See `docs/decisions/ADR-003-use-of-mocking-framework.md`.
- **Mojo tests:** no `maven-plugin-testing-harness`. Set `@Parameter` fields via reflection and call `execute()` directly — this is the established pattern.
- **Pure test co-location:** tests live in the same `.pure` file as the function under test, nested under `::tests::<functionName>::`. Use `<<test.Test>>` for vanilla tests and `<<PCT.test>>` for cross-engine tests. If the native function bears `<<PCT.platformOnly>>` (or has no `<<PCT.function>>` stereotype at all), its tests *must* be `<<test.Test>>` — this is enforced by `FunctionsGeneration.java:123-147`. Use `{test.excludePlatform='Java compiled'}` or `'Java interpreted'` to skip one engine.
- **Pure association gotcha:** two associations cannot both declare the same property name on the same class, even with different association names — Pure raises "Property conflict on class X: property 'Y' defined more than once" at parse. Split into distinct class pairs when a test needs separate reverse-multiplicity fixtures.
- **Platform Pure fixture naming:** anything added under `legend-pure-m3-core/src/main/resources/platform/pure/**` joins the global symbol table and leaks into every test. Short generic names (`Car`, `Owner`, `A`, `B`, `func`, `Test<word><digit>`) break unrelated tests that assert on exact error messages (`PureUnresolvedIdentifierException` candidate lists), exact match counts (`TestSearchTools#testFindInAllPackages`), or exact file counts (`TestClassLoaderCodeStorage#testGetUserFiles`). Rules: (1) prefer reusing `LA_*` fixtures from `lang/_testModel.pure`; (2) promote reusable fixtures there with the `LA_*` prefix; (3) name PCT-local fixtures with unique descriptive identifiers (e.g. `ZeroToOneSource`, `MultiplicityParameterizedHolder`, `InstanceOfEnumA`); (4) when adding new `.pure` files under `platform/pure/**`, bump the count in `TestClassLoaderCodeStorage#testGetUserFiles` by the number added; (5) before pushing, sanity-run `TestPureRuntimeProjection`, `TestMilestoning`, `TestPureRuntimeClass_*`, `TestMatching`, `TestSearchTools`, and `TestClassLoaderCodeStorage`.
- **Java test-source hygiene:** JUnit tests that call `compileTestSource("fromString.pure", …)` must define `@After cleanRuntime() { runtime.delete("fromString.pure"); runtime.compile(); }`. Without it, the second `@Test` method errors with `Source id 'fromString.pure' is already in use`.
- **Checkstyle is enforced at `verify` and fails the build on warnings.** Notable rules: Apache 2.0 copyright header on every `.java`/`.xml`/`.properties` file; spaces not tabs; opening brace on a new line (`nl`); empty catch blocks only when the variable is named `expected` or `ignored`.
- **Logging:** SLF4J with `{}` placeholders. Never `System.out`/`System.err`. Never log secrets or raw SQL with user values.
- **API stability:** adding methods to interfaces in `m4` / `m3-core` breaks legend-engine unless a `default` implementation is provided. Deprecate first, remove in a later release.

## Docs

Authoritative developer docs live in `/docs` and are maintained alongside code. When behaviour, dependencies, or build steps change, update the matching doc in the same PR.

- `docs/README.md` — top-level index.
- `docs/architecture/overview.md` — module tree + ecosystem position.
- `docs/architecture/compiler-pipeline.md` — parse → post-process → validate → codegen.
- `docs/reference/maven-plugins-reference.md` — every plugin goal + parameter.
- `docs/testing/testing-strategy.md` — PCT, coverage, how to run things.
- `docs/standards/coding-standards.md` — full Checkstyle rule list.
- `docs/decisions/` — ADRs (JUnit 4, Eclipse Collections, no mocks).
