# Legend Pure — Maven Plugin Review & Modernisation Recommendations

## Table of Contents

1. [Overview](#1-overview)
2. [Plugin Inventory](#2-plugin-inventory)
3. [Recommendations to Improve Build Times](#3-recommendations-to-improve-build-times)
4. [Recommendations to Improve Consistency](#4-recommendations-to-improve-consistency)
5. [Recommendations for Testing](#5-recommendations-for-testing)
6. [Summary & Prioritised Action Items](#6-summary--prioritised-action-items)

---

## 1. Overview

The `legend-pure-maven` directory contains five Maven plugins and one shared library that
together drive the Legend Pure build pipeline. These plugins compile Pure source code,
generate Java source and class files, produce PAR (Pure Archive) files, and generate
PCT (Platform Compatibility Test) reports.

All five plugins follow the same high-level pattern:

1. Resolve the project's Maven dependencies into a set of URLs.
2. Construct a `URLClassLoader` that includes those dependencies.
3. Set the context classloader of the current thread to the new classloader.
4. Delegate to a library method in the core/runtime modules that performs the actual work.
5. Restore the original classloader.

The shared library (`legend-pure-maven-shared`) centralises the dependency-resolution
and classloader-construction logic so that it does not need to be duplicated in each
plugin. However, not every plugin uses it yet.

---

## 2. Plugin Inventory

### 2.1 `legend-pure-maven-compiler` — `compile-pure`

| Attribute | Value |
|---|---|
| **Goal** | `compile-pure` |
| **Default phase** | `COMPILE` |
| **`requiresDependencyResolution`** | `TEST` |
| **Thread-safe** | Yes |
| **Uses shared library** | Yes |
| **Has `skip` parameter** | No |

**Purpose.** Compiles Pure source code and serialises the resulting binary compiler
artefacts (element files, module manifests, source metadata, external-reference
metadata, back-reference metadata, function-name metadata) to the project output
directory. This is the "new" compiler plugin that produces the binary format consumed
by `PureCompilerLoader`.

The plugin discovers which code repositories to compile either from an explicit
`<repositories>` configuration, or by scanning the project output directory for
`*.definition.json` files. Repositories can be compiled individually (in topological
dependency order) or all together.

Delegates to: `PureCompilerBinaryGenerator.serializeModules()`

---

### 2.2 `legend-pure-maven-generation-java` — `build-pure-compiled-jar`

| Attribute | Value |
|---|---|
| **Goal** | `build-pure-compiled-jar` |
| **Default phase** | *(none — must be bound explicitly)* |
| **`requiresDependencyResolution`** | *(none declared)* |
| **Thread-safe** | Yes |
| **Uses shared library** | No |
| **Has `skip` parameter** | Yes |

**Purpose.** Generates Java source code from the compiled Pure model, optionally
compiles it into `.class` files, and writes distributed binary metadata. This is the
primary plugin that turns a Pure code base into a Java library. It supports both
`monolithic` and `modular` generation strategies.

The classloader is constructed directly from `MavenProject.getCompileClasspathElements()`
using Eclipse Collections. It does **not** use the shared `ProjectDependencyResolution`
utility.

Delegates to: `JavaCodeGeneration.doIt()`

---

### 2.3 `legend-pure-maven-generation-par` — `build-pure-jar`

| Attribute | Value |
|---|---|
| **Goal** | `build-pure-jar` |
| **Default phase** | *(none — must be bound explicitly)* |
| **`requiresDependencyResolution`** | `TEST` |
| **Thread-safe** | Yes |
| **Uses shared library** | Yes |
| **Has `skip` parameter** | No |

**Purpose.** Produces Pure Archive (PAR) files — serialised snapshots of compiled Pure
repositories. PAR files are the primary cache format used to avoid re-parsing and
re-compiling Pure source at runtime. Each repository is serialised as a separate
`pure-<name>.par` file.

Delegates to: `PureJarGenerator.doGeneratePAR()`

---

### 2.4 `legend-pure-maven-generation-platform-java` — `generate-m3-core-instances`

| Attribute | Value |
|---|---|
| **Goal** | `generate-m3-core-instances` |
| **Default phase** | `COMPILE` |
| **`requiresDependencyResolution`** | `TEST` |
| **Thread-safe** | Yes |
| **Uses shared library** | No (has its own copy of the logic) |
| **Has `skip` parameter** | Yes |

**Purpose.** Generates strongly-typed Java accessor interfaces, implementation classes,
wrapper classes, and lazy-loading classes from the M3 Pure metamodel. These are the
foundational `CoreInstance` Java types that the rest of the platform depends on.

The generated output directory is automatically added as a compile source root (by
default) so that `maven-compiler-plugin` will compile the generated files in the same
build.

Delegates to: `M3CoreInstanceGenerator.generate()`

---

### 2.5 `legend-pure-maven-generation-pct` — `generate-pct-functions` / `generate-pct-report`

| Attribute | Value |
|---|---|
| **Goals** | `generate-pct-functions`, `generate-pct-report` |
| **Default phase** | *(none — must be bound explicitly)* |
| **`requiresDependencyResolution`** | *(none declared)* |
| **Thread-safe** | Yes |
| **Uses shared library** | No (has its own `Shared.buildClassLoader()`) |
| **Has `skip` parameter** | No |

**Purpose.** Two goals for the Platform Compatibility Testing (PCT) framework:

- **`generate-pct-functions`** — introspects the compiled Pure model for PCT-annotated
  functions and writes a JSON index file (`FUNCTIONS_<module>.json`).
- **`generate-pct-report`** — runs PCT test suites (in compiled or interpreted mode)
  and writes a JSON results report.

Delegates to: `FunctionsGeneration.generateFunctions()`, `PCTReportGenerator.generateCompiled()` / `generateInterpreted()`

---

### 2.6 `legend-pure-maven-shared`

| Attribute | Value |
|---|---|
| **Packaging** | `jar` (not a plugin) |

**Purpose.** A shared utility library containing:

- **`DependencyResolutionScope`** — an enum modelling the five dependency scopes
  (`compile`, `compile+runtime`, `runtime`, `runtime+system`, `test`) with their
  corresponding `ScopeDependencyFilter` instances.
- **`ProjectDependencyResolution`** — static helpers for resolving dependency URLs,
  determining the appropriate scope from the current lifecycle phase, and a reusable
  `inTestPhase()` check.

This library was introduced to eliminate the need for each plugin to implement its own
dependency resolution. However, only `compile-pure` and `build-pure-jar` currently use
it.

---

## 3. Recommendations to Improve Build Times

### 3.1 Unconditional file overwriting invalidates downstream build caching

**This is the single highest-impact issue affecting build times.**

Every plugin in the chain writes its output files unconditionally — that is, the file
is always created and written to, even if the content is byte-for-byte identical to
the file that already exists on disk. This updates the file's modification timestamp,
which causes every downstream Maven plugin that relies on timestamp comparison (such as
`maven-compiler-plugin`, `maven-jar-plugin`, `maven-resources-plugin`) to re-process
those files. The net effect is that **a full re-build occurs on every invocation, even
when nothing has changed.**

The affected write paths are:

| Plugin / Generator | Write call | File type |
|---|---|---|
| `M3CoreInstanceGenerator` → `M3ToJavaGenerator` | `Files.write(path, bytes)` | Generated `.java` source files |
| `M3CoreInstanceGenerator` → `M3LazyCoreInstanceGenerator` | `Files.write(filePath, code.getBytes())` | Generated `.java` source files |
| `JavaCodeGeneration` → `MemoryFileManager.writeClassJavaSources()` | `Files.write(path, source.getBytes())` | Compiled `.class` files |
| `JavaCodeGeneration` → `DistributedBinaryGraphSerializer` | `FileWriters.fromDirectory(directory)` | Binary metadata files |
| `PureJarSerializer.writePureRepositoryJars()` | `Files.newOutputStream(outputFile)` | `.par` archive files |
| `PureCompilerBinaryGenerator` → `FileSerializer.serializeElement()` | `Files.newOutputStream(filePath)` | Binary element files |
| `PureCompilerBinaryGenerator` → `FileSerializer.serializeModule*()` | `Files.newOutputStream(...)` | Module metadata files |
| `FunctionsGeneration` → `Shared.writeStringToTarget()` | `Files.write(path, bytes, CREATE, TRUNCATE_EXISTING)` | PCT JSON files |

**Recommendation.** Introduce a `write-if-changed` utility in a shared location. Before
writing, read the existing file (if it exists) and compare the content. Only perform the
write if the content differs. A minimal implementation:

```java
public static void writeIfChanged(Path path, byte[] newContent) throws IOException {
    if (Files.exists(path)) {
        byte[] existing = Files.readAllBytes(path);
        if (Arrays.equals(existing, newContent)) {
            return; // Content unchanged — preserve timestamp
        }
    }
    Files.createDirectories(path.getParent());
    Files.write(path, newContent);
}
```

This preserves the file's modification timestamp when nothing has changed, which allows
`maven-compiler-plugin` and `maven-jar-plugin` to skip unchanged outputs.

For output streams (e.g. binary serialisers that write to an `OutputStream`), the
approach is to write to a `ByteArrayOutputStream` first, then compare the resulting
bytes with the existing file before flushing to disk.

**Note:** These write paths are in the core/runtime modules, not the Maven plugin modules
themselves. The fix should be applied in the generators. The Maven plugins do not need
to change for this to take effect, which maintains full compatibility.

### 3.2 No incremental / up-to-date checking

None of the five plugins implement any form of up-to-date checking. Every execution
always performs the full compilation + generation cycle, regardless of whether any
inputs have changed since the last successful run.

Maven supports several patterns for incremental builds:

- **Simple marker-file approach.** Before doing work, hash all input files (Pure
  sources, dependency JARs). Write the hash to a marker file (e.g.
  `.pure-compile.sha256`) in the output directory. On the next execution, if the
  hash matches, skip the goal entirely and log an info message. This is the lowest
  effort to implement and provides the biggest benefit for `mvn install` → `mvn install`
  repeated invocations.

- **Plexus Build API.** Injecting `org.sonatype.plexus.build.incremental.BuildContext`
  allows plugins to participate in IDE incremental builds (Eclipse, IntelliJ). This
  is more effort but pays off in developer workflows.

**Recommendation (short term).** Add a marker-file based up-to-date check to the two
most expensive plugins: `compile-pure` and `build-pure-compiled-jar`. The marker
should be invalidated by:

- Any change to the set of input Pure source files (file count, paths, content hashes).
- Any change to the dependency classpath (dependency coordinates + file hashes).
- Any change to the plugin configuration parameters.

When the marker is valid, the plugin should log `"Output is up-to-date — skipping."`
and return immediately.

### 3.3 PAR files may be non-deterministic

`PureJarSerializer` writes PAR files using `BinaryModelRepositorySerializer` to a plain
`OutputStream`. JAR/ZIP-based archive formats embed entry timestamps. If the
serialisation library sets `ZipEntry.setTime()` to the current time, then the binary
content of the PAR file will differ between runs even when the logical content is
unchanged. This defeats both the `write-if-changed` optimisation (§3.1) and any external
build caching (e.g. Maven build cache extension, Gradle remote cache, Bazel).

**Recommendation.** Ensure all `ZipEntry` timestamps are fixed to a constant (e.g.
epoch `0` or `1980-01-01T00:00:00Z`, the minimum ZIP timestamp). This makes PAR
output reproducible and allows downstream tools to detect no-change correctly.

---

## 4. Recommendations to Improve Consistency

### 4.1 All plugins should have a `skip` parameter

| Plugin | Has `skip` | System property |
|---|---|---|
| `compile-pure` | **No** | — |
| `build-pure-compiled-jar` | Yes | — (not exposed as a property) |
| `build-pure-jar` | **No** | — |
| `generate-m3-core-instances` | Yes | — (not exposed as a property) |
| `generate-pct-functions` | **No** | — |
| `generate-pct-report` | **No** | — |

Maven convention is that every goal should have a `skip` parameter, ideally also
exposed as a system property so it can be toggled from the command line:

```java
@Parameter(property = "legend.pure.compile.skip", defaultValue = "false")
private boolean skip;
```

**Recommendation.** Add `skip` to all four plugins that lack it. Expose each as a
system property following a consistent naming convention (`legend.pure.<goal>.skip`).
This is fully backward-compatible — the default is `false`.

### 4.2 All plugins should declare `defaultPhase`

| Plugin | Has `defaultPhase` |
|---|---|
| `compile-pure` | Yes (`COMPILE`) |
| `build-pure-compiled-jar` | **No** |
| `build-pure-jar` | **No** |
| `generate-m3-core-instances` | Yes (`COMPILE`) |
| `generate-pct-functions` | **No** |
| `generate-pct-report` | **No** |

Plugins without a `defaultPhase` require every consumer to explicitly bind the goal to
a lifecycle phase in their POM. Declaring a default phase makes the plugin easier to use
and is overridden if the consumer specifies an explicit `<phase>`.

**Recommendation.** Add default phases:

| Plugin | Recommended `defaultPhase` |
|---|---|
| `build-pure-compiled-jar` | `COMPILE` |
| `build-pure-jar` | `COMPILE` |
| `generate-pct-functions` | `GENERATE_RESOURCES` |
| `generate-pct-report` | `TEST` |

### 4.3 All plugins should declare `requiresDependencyResolution`

| Plugin | Has `requiresDependencyResolution` |
|---|---|
| `compile-pure` | Yes (`TEST`) |
| `build-pure-compiled-jar` | **No** |
| `build-pure-jar` | Yes (`TEST`) |
| `generate-m3-core-instances` | Yes (`TEST`) |
| `generate-pct-functions` | **No** |
| `generate-pct-report` | **No** |

`build-pure-compiled-jar` calls `project.getCompileClasspathElements()` to build its
classloader. `generate-pct-functions` and `generate-pct-report` call both
`project.getCompileClasspathElements()` and `project.getTestClasspathElements()`. Without
`requiresDependencyResolution`, Maven does not guarantee that the classpath has been
resolved before the plugin executes. In practice this usually works because an earlier
lifecycle phase forces resolution, but it violates the Maven contract and may fail in
edge cases (e.g. when invoked directly via `mvn <plugin>:<goal>`).

**Recommendation.** Add `requiresDependencyResolution = ResolutionScope.TEST` to all
three affected mojos. This matches the convention used by the other plugins and ensures
correctness.

### 4.4 `M3CoreInstanceGeneratorMojo` should use the shared dependency resolution library

`M3CoreInstanceGeneratorMojo` contains its own copies of:

- Dependency scope constants (`COMPILE_RESOLUTION_SCOPE`, etc.)
- `getDependencyURLs()` method
- `getDependencyFilter()` method (a `switch` statement over scope strings)
- `resolveDependencyScope()` method
- `inTestPhase()` method
- `toURL()` method

All of this logic already exists in `DependencyResolutionScope` and
`ProjectDependencyResolution` in `legend-pure-maven-shared`. The duplication means:

- Bug fixes or scope additions in the shared library are not picked up by this mojo.
- The two implementations may diverge over time.
- It is 70+ lines of code that can be replaced by two method calls.

**Recommendation.** Refactor `M3CoreInstanceGeneratorMojo` to use the shared library.
The `execute()` method should call `ProjectDependencyResolution.determineDependencyResolutionScope()`
and `ProjectDependencyResolution.getDependencyURLs()`. Add `legend-pure-maven-shared`
as a dependency to `legend-pure-maven-generation-platform-java/pom.xml`.

### 4.5 `PureCompiledJarMojo` should use the shared dependency resolution library

`PureCompiledJarMojo` builds its classloader via a custom `buildClassLoader()` method
that uses Eclipse Collections `ListIterate.collect()` on
`project.getCompileClasspathElements()`. This differs from the shared library approach
in two ways:

1. It only includes compile-scope classpath elements (not test, even when that may be
   needed).
2. It does not use the filtered dependency resolution approach (via
   `ProjectDependenciesResolver`), so it relies on the legacy
   `MavenProject.getCompileClasspathElements()` API.

**Recommendation.** Refactor `PureCompiledJarMojo` to use `ProjectDependencyResolution`,
add `requiresDependencyResolution = ResolutionScope.TEST` and a `dependencyScope`
parameter, consistent with the other plugins. Add `legend-pure-maven-shared` as a
dependency to `legend-pure-maven-generation-java/pom.xml`.

### 4.6 PCT `Shared.buildClassLoader()` should use the shared library or declare dependency scope

The PCT module has its own `Shared.buildClassLoader()` that combines both
`project.getCompileClasspathElements()` and `project.getTestClasspathElements()`. This
duplicates the URL-construction logic and does not use the dependency-scope filtering
available in the shared library.

**Recommendation.** Either:

- Refactor both PCT mojos to use `ProjectDependencyResolution` from the shared library, or
- At minimum, add `requiresDependencyResolution = ResolutionScope.TEST` to both mojos to
  guarantee that the classpath is resolved.

### 4.7 Error handling: `RuntimeException` instead of `MojoExecutionException`

Two mojos catch exceptions and re-throw as `RuntimeException`:

```java
// GeneratePCTFunctions.java, line 57
throw new RuntimeException(e);

// GeneratePCTReport.java, line 81
throw new RuntimeException(e);
```

Maven expects plugins to throw `MojoExecutionException` (for unexpected errors) or
`MojoFailureException` (for expected failures like compilation errors). Throwing a raw
`RuntimeException` causes Maven to print an "Internal error" banner with a full stack
trace instead of a clean `BUILD FAILURE` message.

**Recommendation.** Change both to throw `MojoExecutionException`. Note that
`GeneratePCTReport` already has `throws MojoExecutionException` in its method signature
but the catch block throws `RuntimeException` instead — this is almost certainly a bug.

### 4.8 `maven-core` dependency scope

| Plugin POM | `maven-core` scope |
|---|---|
| `legend-pure-maven-compiler` | `provided` ✓ |
| `legend-pure-maven-generation-par` | `provided` ✓ |
| `legend-pure-maven-generation-platform-java` | `provided` ✓ |
| `legend-pure-maven-generation-java` | *(not specified — defaults to `compile`)* ✗ |
| `legend-pure-maven-generation-pct` | *(not specified — defaults to `compile`)* ✗ |

`maven-core` and `maven-plugin-api` are provided by the Maven runtime and should always
be declared with `<scope>provided</scope>` in plugin POMs. Including them at compile
scope bloats the plugin JAR and can cause classloader conflicts when Maven's own version
differs from the bundled version.

**Recommendation.** Add `<scope>provided</scope>` to `maven-core` and `maven-plugin-api`
in `legend-pure-maven-generation-java/pom.xml` and `legend-pure-maven-generation-pct/pom.xml`.

### 4.9 Obsolete `skipErrorNoDescriptorsFound` configuration

Four of the five plugin POMs configure:

```xml
<skipErrorNoDescriptorsFound>true</skipErrorNoDescriptorsFound>
```

This was a workaround for [MNG-5346](https://issues.apache.org/jira/browse/MNG-5346),
which has been resolved since Maven 3.x. With the current `maven-plugin-plugin` version
(3.10.2+) and annotation-based mojo discovery, this option has no effect.

**Recommendation.** Remove `skipErrorNoDescriptorsFound` from all plugin POMs. This is
a trivial cleanup with no functional impact.

### 4.10 Inconsistent `maven-plugin-plugin` descriptor configuration

`legend-pure-maven-generation-platform-java` uses the `helpmojo` goal (which generates
a `HelpMojo` class that documents all parameters), while the other four plugins use
only the `descriptor` goal.

**Recommendation.** Either add `helpmojo` to all five plugin POMs (for a better user
experience when running `mvn <plugin>:help`), or remove it from the one that has it,
for consistency.

### 4.11 Log adapter duplication

Both `PureCompiledJarMojo` and `PureJarMojo` contain inline anonymous-class
implementations of their respective `Log` interfaces to bridge Maven's logging to
the library's logging. These are essentially identical in structure.

**Recommendation.** Consider adding a shared log adapter factory to
`legend-pure-maven-shared` that takes a Maven `org.apache.maven.plugin.logging.Log` and
returns the appropriate library `Log` wrapper. This eliminates ~30 lines of duplicated
boilerplate from each mojo.

---

## 5. Recommendations for Testing

### 5.1 Current state: zero tests

None of the six modules under `legend-pure-maven/` contain any test code. There are no
`src/test/` directories anywhere.

### 5.2 Unit tests for the shared library

`legend-pure-maven-shared` contains pure logic with no Maven runtime dependencies in its
public API (other than `MojoExecution`, which is easy to mock). The following should be
unit-tested:

| Class | Method | Test cases |
|---|---|---|
| `DependencyResolutionScope` | `fromName(String)` | Each valid name (case-insensitive), invalid name (expect `IllegalArgumentException`) |
| `DependencyResolutionScope` | `getScopeDependencyFilter()` | Non-null for all scopes except `test`, null for `test` |
| `DependencyResolutionScope` | `isTestScope()` | `true` only for `TEST_RESOLUTION_SCOPE` |
| `ProjectDependencyResolution` | `inTestPhase(MojoExecution)` | `"test-compile"` → true, `"process-test-classes"` → true, `"test"` → true, `"compile"` → false, `"package"` → false |
| `ProjectDependencyResolution` | `determineDependencyResolutionScope(String, MojoExecution)` | Non-null override returns corresponding scope; null override in test phase returns `TEST`; null override in non-test phase returns `COMPILE` |

### 5.3 Unit tests for mojo parameter resolution

Each mojo contains logic that resolves configuration parameters into effective values.
These methods are private but could be made package-private (or extracted to a helper)
for testing:

| Mojo | Method to test |
|---|---|
| `PureCompilerMojo` | `resolveOutputDirectory()`, `resolveRepositoriesToSerialize()`, `shouldSerializeIndividually()` |
| `M3CoreInstanceGeneratorMojo` | `resolveOutputDirectory()`, `resolveDependencyScope()`, `getDependencyFilter()`, `inTestPhase()` |

### 5.4 Integration tests with `maven-invoker-plugin`

Maven provides `maven-invoker-plugin` for end-to-end testing of plugins. Each plugin
should have at least one `src/it/` project that:

1. Invokes the plugin goal on a minimal set of inputs.
2. Verifies that the expected output files exist.
3. Verifies that a second invocation with unchanged inputs is faster (or skipped, once
   incremental build support is added).

This is the gold standard for Maven plugin testing and would catch configuration,
classloading, and lifecycle binding regressions.

---

## 6. Summary & Prioritised Action Items

### Tier 0 — Establish a testing foundation (prerequisite for safe changes)

All subsequent tiers involve behavioural changes to the plugins. Without tests, there is
no safety net to verify that those changes preserve compatibility. Tier 0 focuses on
getting the minimum viable test coverage in place **before** making functional changes.

| # | Action | Modules affected | Rationale |
|---|---|---|---|
| ✅ 0a | Add unit tests for `DependencyResolutionScope` | maven-shared | Validates scope resolution (`fromName`, `isTestScope`, filter correctness) — the shared logic all other plugins depend on |
| ✅ 0b | Add unit tests for `ProjectDependencyResolution` | maven-shared | Validates `inTestPhase()` lifecycle mapping and `determineDependencyResolutionScope()` — used by every mojo that consumes the shared library |
| ✅ 0c | Add unit tests for `PureCompilerMojo` parameter resolution | compiler | Validates `resolveOutputDirectory()`, `resolveRepositoriesToSerialize()`, `shouldSerializeIndividually()` — the most complex mojo-level logic |
| ✅ 0d | Add unit tests for `Shared.assertPresentOrNotEmpty()` | generation-pct | Trivial to test (null, empty collection, valid values); catches regressions before PCT mojos are refactored |
| ✅ 0e | Add a `pom.xml` test-dependency on JUnit 5 + Mockito to the shared and plugin modules | maven-shared, compiler, generation-pct | Establishes the test infrastructure so subsequent tiers can add tests alongside their changes |

**Why Tier 0 comes first.** The recommendations in Tiers 1–4 (adding `skip` parameters,
refactoring mojos to the shared library, changing error handling, etc.) all change
runtime behaviour. Unit tests written *before* those changes serve as regression baselines:
they document the current behaviour, and any future refactoring that accidentally breaks
the contract will be caught immediately. This is especially important given the project's
emphasis on maintaining full compatibility.

**Additional work completed on this branch (beyond original Tier 0 scope):**

| Area | Tests added | Notes |
|---|---|---|
| `execute()`-level tests for all five mojos | `TestPureCompiledJarMojo` (10), `TestPureJarMojo` (17), `TestPureCompilerMojo` (22), `TestM3CoreInstanceGeneratorMojo` (23), `TestGeneratePCTReport` (9), `TestGeneratePCTFunctions` (5) | End-to-end mojo execution with real field injection |
| Core generator tests (`legend-pure-m3-core`) | `TestPureCompilerBinaryGenerator` (+6), `TestPureJarGenerator` (+5), `TestM3CoreInstanceGenerator` (5 new) | Covers `PureJarSerializer`, `DirectoryPureCompilerLoader`, `M3CoreInstanceGenerator.generate()` end-to-end with production file list |
| `TestJavaCodeGeneration` (`legend-pure-runtime-java-engine-compiled`) | +4 tests | `generateSources`, `generateTestSources`, error wrapping paths |
| Production code: `M3ToJavaGenerator` | Added `getFactoryNamePrefix()` accessor | Enables direct assertion of prefix passthrough without relying on output side-effects |

### Tier 1 — High impact, low effort (do first)

| # | Action | Modules affected | Rationale |
|---|---|---|---|
| 1 | Add `write-if-changed` logic to core generators | Core/runtime generators (not maven modules directly) | Eliminates the #1 cause of unnecessary rebuilds |
| ✅ 2 | Add `skip` parameter (with system property) to all mojos | compiler, par, pct (×2) | Maven convention; enables CI flexibility |
| ✅ 3 | Fix `RuntimeException` → `MojoExecutionException` in PCT mojos | generation-pct | Corrects Maven error reporting |
| ✅ 4 | Add `requiresDependencyResolution = TEST` to mojos missing it | generation-java, generation-pct (×2) | Correctness |
| ✅ 5 | Fix `maven-core` scope to `provided` | generation-java, generation-pct | Prevents classloader conflicts |

### Tier 2 — High impact, moderate effort

| # | Action | Modules affected | Rationale |
|---|---|---|---|
| 6 | Refactor `M3CoreInstanceGeneratorMojo` to use shared library | generation-platform-java | Eliminates 70+ lines of duplicated code |
| 7 | Refactor `PureCompiledJarMojo` to use shared library | generation-java | Consistency, correctness |
| 8 | Refactor PCT mojos to use shared library | generation-pct | Consistency |
| ✅ 9 | Add unit tests for `legend-pure-maven-shared` | maven-shared | Prevents regressions in shared logic |
| 10 | Add `defaultPhase` to mojos missing it | generation-java, par, pct (×2) | User experience |

### Tier 3 — High impact, high effort

| # | Action | Modules affected | Rationale |
|---|---|---|---|
| 11 | Implement marker-file up-to-date checking | compiler, generation-java | Skips entire goals when inputs unchanged |
| 12 | Ensure deterministic PAR output (fixed ZIP timestamps) | par (core generator) | Reproducible builds, enables build caching |
| 13 | Add `maven-invoker-plugin` integration tests | All five plugins | End-to-end regression prevention |

### Tier 4 — Low impact, low effort (cleanup)

| # | Action | Modules affected | Rationale |
|---|---|---|---|
| 14 | Remove obsolete `skipErrorNoDescriptorsFound` | compiler, generation-java, par, pct | Cleanup |
| 15 | Standardise `maven-plugin-plugin` configuration (`helpmojo`) | All five plugins | Consistency |
| 16 | Extract shared log adapter | maven-shared | Reduces boilerplate |

---

*Document generated: 2026-03-18 — Last updated: 2026-03-26 (marked completed items from branch `feature-ao_260318-mavenPlugins`)*

