# legend-pure-rust-jni

The JNI transport of the Java boundary to the Rust Pure evaluator in `legend-pure-rust/`
(Step 2 of `legend-pure-rust/docs/rust-evaluator-mvp.md`; the module split is Step 2c,
phase 1). Everything transport-free (the hosts, the node bridge's host side, the proxies,
the exception kinds, the PCT and benchmark tooling) is in
`legend-pure-rust-api`, which this module depends on (the tooling through its test-jar, at
test scope) and whose README describes the shared surface; this module adds the evaluator that crosses over JNI.

## Surface

| Class | Role |
|---|---|
| `PureRustEvaluator` | A `FunctionExecution` over the Rust evaluator: `init(runtime, message)` binds it to the interpreted engine's `PureRuntime`, `over(host)` to any engine; `evaluate(function, arguments)` runs a graph node and returns the raw results as the host engine represents them; `start(function, arguments)` is the interpreted engine's form (arguments wrapped as `InstanceValue`s, result an `InstanceValue`); `create(classifierPath, names, values)` allocates a Rust object; `liveHandleCount()` reports Rust-side handles still alive; `lastTimings()` the Rust side's split of the last call; `close()` frees everything. Its `native` methods are the exported symbols of `libpure_rust_jni`; it is the `RustHeap` the proxies of its Rust objects read through. |
| `RustNodeBridge` | The JNI-called surface, the class `crates/jni` resolves its method ids on by name: the shared `NodeBridge` (describe, reads, forget, elements, dates, `failureOf`) plus the host's primitive factories and the proxy table (`rustObject`), one `RustCoreInstance` per handle; the ids of nodes Rust released are forgotten when a call ends. |

Crossing in: primitives (Boolean, Integer, Float, Decimal, String, every Date kind, StrictTime) become
Rust values; a packageable element whose path the Rust model knows becomes that element; enum values
of known enumerations become enum values; every other node becomes a lazily read Java-backed heap
object. Crossing out: primitives through the host's factories, elements and enum values by path,
Java-backed nodes as their original object, maps as the host engine's own map, other Rust objects as
`RustCoreInstance`. Functions and unit instances raise `UNSUPPORTED` until a test needs them. A lambda is
lowered once per definition and cached for the evaluator's lifetime; later calls read only its captures.

## PCT: the `Rust_JNI` adapter

This module's Pure repository, `platform_rust_jni` (`src/main/resources/platform_rust_jni.definition.json`,
pattern `meta::pure::rust::jni`, depending on `platform`; `jni.pct.RustJniCodeRepositoryProvider`; a
PAR from `legend-pure-maven-generation-par` and PELT from `legend-pure-maven-compiler`), declares the
adapter `meta::pure::rust::jni::testAdapterForRustExecution` (`<<PCT.adapter>> {PCT.adapterName='Rust_JNI'}`,
`RustJniCodeRepositoryProvider.rustJniAdapter`), whose body is `$f->evalRust()`, and the native
`meta::pure::rust::jni::evalRust`. This module implements that native for both Java engines and runs
the platform corpus through each. (A third adapter, `Rust_Native`, is the corpus on the evaluator
alone with no Java engine in the loop; `-api`'s `platform_rust` declares it and
`legend-pure-rust/crates/evaluator/src/pct.rs` drives it; it never touches this module.)

| Class | Role |
|---|---|
| `jni.interpreted.EvalRust` | The native: unwraps the lambda node and calls `PureRustEvaluator.start(lambda, [])`. One evaluator per engine, created on the first call and bound to the engine's `PureRuntime`; replaced (the old one closed) when the engine's runtime changes. |
| `jni.interpreted.RustJniInterpretedExtension` | Registers `evalRust_Function_1__V_m_`; discovered through `META-INF/services/…InterpretedExtension`. Its constructor must never touch the Rust library: the extension loader drops an extension whose constructor throws, silently. |
| `jni.compiled.RustJniCompiledExtension` | Registers the generation native `jni.compiled.EvalRust` for `platform_rust_jni`; discovered through `META-INF/services/…CompiledExtension`. |
| `jni.compiled.CompiledEvalRust` | The compiled engine's `evalRust`: the generated code calls the static `CompiledEvalRust.evaluate(es, f)`. The function value is a compiled lambda, which is also its own graph node, so it crosses the bridge unchanged; results come back as compiled values (`ValCoreInstance` unwrapped to Java objects, a carried `PureMap` unwrapped, Rust objects copied into their generated `_Impl` classes by `CompiledObjects`, graph objects as they are). One evaluator per `CompiledExecutionSupport`. |
| `jni.pct.Test_RustJni_{Essential,Grammar}Functions_PCT` | The interpreted engine's PCT suites with the `Rust_JNI` adapter (`PctSuites.interpreted`). Each prints `Rust_JNI <scope>: <passes> / <total>`. |
| `jni.pct.Test_RustJniCompiled_{Essential,Grammar}Functions_PCT` | The same corpus through the compiled engine (`PctSuites.compiled`); prints `Rust_JNI <scope> (compiled): <passes> / <total>`. The compiled platform classes come from the engine's test-jar; the repository's own are generated by this module's build (`JavaCodeGeneration platform_rust_jni` at `compile`, after `pre-compile` at `generate-sources` and `compile-pure`), compiled with the tests and published in its test-jar. |
| `jni.pct.RebaseRustJniManifests` | Regenerates the four manifests (`PctManifestRebase`, one call per engine and scope); not in the default surefire run. |
| `jni.pct.BenchmarkPctExpressions` | `PctExpressionsBenchmark` under the four modes (each engine with its own adapter and with `Rust_JNI`, the Rust split read from `PureRustEvaluator.lastTimings()`). Not in the default run; `-Dtest=BenchmarkPctExpressions -DfailIfNoTests=false`, with `-Dlegend.rust.libdir=…/target/release` after `cargo build --release` for an optimised Rust side. |
| `jni.pct.Core_RustJni_PCTReportProvider` | Publishes `pct-reports/ADAPTER_{essential,grammar}_{interpreted,compiled}_Rust_JNI.json`, written at `process-test-classes` by each engine's `PCTReportGenerator` from the manifests alone (no Rust library needed). |

Manifests: `src/test/resources/platform/pure/essential/pct_essential_{interpreted,compiled}_rust_jni.json`
and `…/grammar/functions/pct_grammar_{interpreted,compiled}_rust_jni.json`. They sit inside a repository
tree because the surveyor reads them through the runtime's code storage while the report reads
them from the classpath. They are generated, never hand-written:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn test -pl legend-pure-rust/java/legend-pure-rust-jni \
  -Dtest=RebaseRustJniManifests -DfailIfNoTests=false
```

runs the surveyor under the committed manifest and derives the new one from its report. A second
run reproduces the files byte for byte. Review the diff, then `verify`.

State: `Rust_JNI essential: 339 / 346`, `Rust_JNI grammar: 133 / 137` on the interpreted engine;
`340 / 346` and `133 / 137` on the compiled engine (one more than interpreted: `testBigFloatAbs`
passes there, the compiled engine's `Float` being a `double` as Rust's is). The lambda is
lowered through the bridge (`crates/evaluator/src/lower/`): its body, the functions it calls, the
classes and enumerations it references, and the variables it captured (`NodeBridge.capturedValue`,
read through the host from the interpreted engine's `LambdaWithContext` or the compiled lambda's open
variables); the natives it calls are
fished from the archive (156 keys, pinned by `crates/evaluator/tests/registry.rs`). What remains red is ranked by its first line:
`FunctionNotFound: <mangled key>` (nothing registered), `NotImplemented: <key>` (registered,
body not fished), `Unsupported: …` (a lowering or value-shape gap, e.g. a `Map` crossing the
boundary), then bare Pure texts for semantic differences. A Rust failure reaches the engine as a
`PureExecutionException` cause whose info is Pure's bare text for the Pure-level kinds (what
`assertError` compares) and the kind-prefixed message for the Rust-side diagnostics; the compiled
path rethrows that cause itself. The compiled `evalRust` compiles to a call into `CompiledEvalRust`;
the platform and the Java engines know nothing of it.

## Build and test

```bash
cd legend-pure-rust && cargo build --workspace          # produces target/debug/libpure_rust_jni.{dylib,so,dll}
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-rust/java/legend-pure-rust-api,legend-pure-rust/java/legend-pure-rust-jni clean verify
```

`verify` runs the 22 boundary tests and the four `Rust_JNI` suites (346 + 137 per engine), checkstyle
and dependency analysis on both modules.

The module compiles with release 17 (the rest of the build stays at release 8) and needs JDK 17: it
joins the reactor, after `-api`, through the profile `rust-evaluator-jdk17` in `legend-pure-rust/java/pom.xml`, active
on JDK 17 and later, so a JDK 11 build neither builds nor installs it and `-pl` cannot select it there.
The other modules keep their JDK 11 or 17 range.

Surefire passes the library directory to the tests as the system property
`legend.pure.rust.libdir`; without it `PureRustEvaluator` falls back to
`System.loadLibrary("pure_rust_jni")`. When the library is absent the module's
tests are skipped (profile `rust-cdylib-missing-*`), so `mvn install` stays green on
hosts and CI without a Rust toolchain. Override with `-DskipTests=false` to force a run.
