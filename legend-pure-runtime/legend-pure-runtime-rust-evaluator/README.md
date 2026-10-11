# legend-pure-runtime-rust-evaluator

The Java side of the JNI boundary to the Rust Pure evaluator in
`legend-pure-rust/` (Step 2 of `legend-pure-rust/docs/rust-evaluator-mvp.md`).

## Surface

The only value currency across the boundary is `org.finos.legend.pure.m4.coreinstance.CoreInstance`,
in both directions.

| Class | Role |
|---|---|
| `PureRustEvaluator` | A `FunctionExecution` over the Rust evaluator: `init(runtime, message)` binds it to the interpreted engine's `PureRuntime`, `over(host)` to any engine; `evaluate(function, arguments)` runs a graph node and returns the raw results as the host engine represents them; `start(function, arguments)` is the interpreted engine's form (arguments wrapped as `InstanceValue`s, result an `InstanceValue`); `create(classifierPath, names, values)` allocates a Rust object; `liveHandleCount()` reports Rust-side handles still alive; `close()` frees everything. |
| `RustEngineHost` | What the bridge needs from the engine it serves: the `ProcessorSupport`, a node's property names, a lambda's captured values, the definition a lambda value stands for (the closure cache's key), and the engine's own representation of each primitive and of a map. `interpreted.InterpretedHost` (keys from the node, captures from `LambdaWithContext`, a lambda's repository synthetic id, primitives from the `ModelRepository`, maps as `MapCoreInstance`) and `compiled.CompiledHost` (keys from the class's properties, captures from the generated lambda's open variables, no lambda definition, primitives as `ValCoreInstance`, maps as a `PureMap` carried in a `CompiledValue`). |
| `RustNodeBridge` | The Java half of the node bridge, called from Rust: describes a node in one call (kind, classifier path, primitive payload, a lambda's definition id), reads property values and keys on demand, materialises Rust values through the host and `_Package.getByUserPath`, and tells Rust's failures apart (`failureOf`). Node ids are never reused; the ids of nodes Rust released are forgotten when a call ends. |
| `RustCoreInstance` | A Rust heap object seen from Java as a read-only `CoreInstance`; its classifier is the Java graph node for the Rust classifier path, its name the `name` of a model element or else an anonymous one, as a Java instance's. One proxy per Rust handle; `close()` releases it. |
| `PureRustEvaluationException` | Typed failure: `getKind()` is one of `EXECUTION_ERROR`, `ASSERTION_FAILED`, `CONSTRAINT_VIOLATION`, `FUNCTION_NOT_FOUND`, `UNSUPPORTED`, `NOT_IMPLEMENTED` (a native wired under its key whose implementation is not fished yet); `getMessage()` starts with the kind's display name (`FunctionNotFound: …`). An `ASSERTION_FAILED` carries a `PureAssertFailException` as its cause. |

Crossing in: primitives (Boolean, Integer, Float, Decimal, String, every Date kind, StrictTime) become
Rust values; a packageable element whose path the Rust model knows becomes that element; enum values
of known enumerations become enum values; every other node becomes a lazily read Java-backed heap
object. Crossing out: primitives through the repository factories, elements and enum values by path,
Java-backed nodes as their original object, maps as the host engine's own map, other Rust objects as
`RustCoreInstance`. Functions and unit instances raise `UNSUPPORTED` until a test needs them. A lambda is
lowered once per definition and cached for the evaluator's lifetime; later calls read only its captures.

## PCT: the `Rust_JNI` adapter

Every PCT test takes its adapter function as argument 0 and calls `$f->eval(|…)`.
The platform declares a second adapter, `meta::pure::test::pct::testAdapterForRustExecution`
(`<<PCT.adapter>> {PCT.adapterName='Rust_JNI'}`), whose body is `$f->evalRust()`, and the
native `meta::pure::functions::lang::evalRust`. This module implements that native for both
Java engines and runs the platform corpus through each. (A third adapter, `Rust_Native`,
is the corpus on the evaluator alone with no Java engine in the loop; it lives in
`legend-pure-rust/crates/evaluator/src/pct.rs` and never touches this module.)

| Class | Role |
|---|---|
| `interpreted.EvalRust` | The native: unwraps the lambda node and calls `PureRustEvaluator.start(lambda, [])`. One evaluator per engine, created on the first call and bound to the engine's `PureRuntime`; replaced (the old one closed) when the engine's runtime changes. |
| `interpreted.RustJniInterpretedExtension` | Registers `evalRust_Function_1__V_m_`; discovered through `META-INF/services/…InterpretedExtension`. Its constructor must never touch the Rust library: the extension loader drops an extension whose constructor throws, silently. |
| `compiled.CompiledEvalRust` | The compiled engine's `evalRust`: the engine's generated code calls its `EvalRust.evaluate(es, f)` hook, which finds this `EvalRustExecutor` through `META-INF/services`. The function value is a compiled lambda, which is also its own graph node, so it crosses the bridge unchanged; results come back as compiled values (`ValCoreInstance` unwrapped to Java objects, a carried `PureMap` unwrapped, Rust objects copied into their generated `_Impl` classes by `CompiledObjects`, graph objects as they are). One evaluator per `CompiledExecutionSupport`. |
| `pct.Test_RustJni_{Essential,Grammar}Functions_PCT` | The interpreted engine's PCT suites with the `Rust_JNI` adapter (surveyor path). Each prints `Rust_JNI <scope>: <passes> / <total>`. |
| `pct.Test_RustJniCompiled_{Essential,Grammar}Functions_PCT` | The same corpus through the compiled engine; prints `Rust_JNI <scope> (compiled): <passes> / <total>`. The compiled platform classes come from the engine's test-jar. |
| `pct.RebaseRustJniManifests` | Regenerates the four manifests from a surveyor run per engine; not in the default surefire run. |
| `pct.BenchmarkNativeExpressions` | The apples-to-apples half on the Java side: the twelve expressions of `src/test/resources/bench/expressions.pure`, compiled into each engine at run time and timed there with no Rust involved (the compiled engine both through its `start` entry and by invoking the generated method directly); the Rust half is `crates/evaluator/examples/expressions.rs`, the same expressions built with the builders and timed under `cargo run --release --example expressions` with no JNI. Both check the same results. Not in the default run; `-Dtest=BenchmarkNativeExpressions -DfailIfNoTests=false`. |
| `pct.BenchmarkPctExpressions` | Times every PCT test under the four modes (each engine with its own adapter and with `Rust_JNI`): first run, median and minimum per test, and for the Rust modes the Rust side's own split (`PureRustEvaluator.lastTimings()`: lowering, run of the lowered closure, JNI reads during the run, JNI reads during the lowering; a run with no read is the evaluator alone, a lowering with none came from the closure cache) to `target/benchmarks/pct-expressions.csv`, a summary per mode to `pct-expressions.md`. Not in the default run; `-Dtest=BenchmarkPctExpressions -DfailIfNoTests=false`, with `-Dlegend.rust.libdir=…/target/release` after `cargo build --release` for an optimised Rust side. |
| `pct.Core_RustJni_PCTReportProvider` | Publishes `pct-reports/ADAPTER_{essential,grammar}_{interpreted,compiled}_Rust_JNI.json`, written at `process-test-classes` by each engine's `PCTReportGenerator` from the manifests alone (no Rust library needed). |

Manifests: `src/test/resources/platform/pure/essential/pct_essential_{interpreted,compiled}_rust_jni.json`
and `…/grammar/functions/pct_grammar_{interpreted,compiled}_rust_jni.json`. They sit inside a repository
tree because the surveyor reads them through the runtime's code storage while the report reads
them from the classpath. They are generated, never hand-written:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator \
  -Dtest=RebaseRustJniManifests -DfailIfNoTests=false
```

runs the surveyor under the committed manifest and derives the new one from its report: a FAIL or
ERROR becomes an exclusion holding the first line of its message; a PASS reported as an expected
failure keeps its committed entry; an excluded test that now passes is dropped. A second run
reproduces the files byte for byte. Review the diff, then `verify`.

State: `Rust_JNI essential: 320 / 327`, `Rust_JNI grammar: 132 / 136` on the interpreted engine;
`321 / 327` and `132 / 136` on the compiled engine (one more than interpreted: `testBigFloatAbs`
passes there, the compiled engine's `Float` being a `double` as Rust's is). The lambda is
lowered through the bridge (`crates/evaluator/src/lower/`): its body, the functions it calls, the
classes and enumerations it references, and the variables it captured (`RustNodeBridge.capturedValue`,
read through the host from the interpreted engine's `LambdaWithContext` or the compiled lambda's open
variables); the natives it calls are
fished from the archive (158 keys, pinned by `crates/evaluator/tests/registry.rs`). What remains red is ranked by its first line:
`FunctionNotFound: <mangled key>` (nothing registered), `NotImplemented: <key>` (registered,
body not fished), `Unsupported: …` (a lowering or value-shape gap, e.g. a `Map` crossing the
boundary), then bare Pure texts for semantic differences. A Rust failure reaches the engine as a
`PureExecutionException` cause whose info is Pure's bare text for the Pure-level kinds (what
`assertError` compares) and the kind-prefixed message for the Rust-side diagnostics; the compiled
path rethrows that cause itself. The compiled engine's `evalRust` compiles to a call into its
`EvalRustExecutor` hook, so the platform builds without this module and fails at run time without it.

## Build and test

```bash
cd legend-pure-rust && cargo build --workspace          # produces target/debug/libpure_rust_jni.{dylib,so,dll}
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator clean verify
```

`verify` runs the 22 boundary tests and the four `Rust_JNI` suites (327 + 136 per engine), checkstyle
and dependency analysis.

The module compiles with release 17 (the rest of the build stays at release 8) and needs JDK 17: it
joins the reactor through the profile `rust-evaluator-jdk17` in `legend-pure-runtime/pom.xml`, active
on JDK 17 and later, so a JDK 11 build neither builds nor installs it and `-pl` cannot select it there.
The other modules keep their JDK 11 or 17 range.

Surefire passes the library directory to the tests as the system property
`legend.pure.rust.libdir`; without it `PureRustEvaluator` falls back to
`System.loadLibrary("pure_rust_jni")`. When the library is absent the module's
tests are skipped (profile `rust-cdylib-missing-*`), so `mvn install` stays green on
hosts and CI without a Rust toolchain. Override with `-DskipTests=false` to force a run.

The bindings annotation processor module (`…-bindings-ap`) is out of the reactor:
its generator is the archived Rust `java-codegen` crate. It is not part of the MVP (plan §7);
if typed Java proxies are ever wanted, it is the place for them.
