# legend-pure-runtime-rust-evaluator

The Java side of the JNI boundary to the Rust Pure evaluator in
`legend-pure-rust/` (Step 2 of `legend-pure-rust/docs/rust-evaluator-mvp.md`).

## Surface

The only value currency across the boundary is `org.finos.legend.pure.m4.coreinstance.CoreInstance`,
in both directions.

| Class | Role |
|---|---|
| `PureRustEvaluator` | A `FunctionExecution` over the Rust evaluator: `init(runtime, message)` binds it to a `PureRuntime`; `start(function, arguments)` takes graph nodes (arguments wrapped as `InstanceValue`s, as the interpreted engine expects) and returns an `InstanceValue`; `create(classifierPath, names, values)` allocates a Rust object; `liveHandleCount()` reports Rust-side handles still alive; `close()` frees everything. |
| `RustNodeBridge` | The Java half of the node bridge, called from Rust: describes a node in one call (kind, classifier path, primitive payload, keys), reads property values on demand, and materialises Rust values through the `ModelRepository` factories and `_Package.getByUserPath`. |
| `RustCoreInstance` | A Rust heap object seen from Java as a read-only `CoreInstance`; its classifier is the Java graph node for the Rust classifier path. One proxy per Rust handle; `close()` releases it. |
| `PureRustEvaluationException` | Typed failure: `getKind()` is one of `EXECUTION_ERROR`, `ASSERTION_FAILED`, `CONSTRAINT_VIOLATION`, `FUNCTION_NOT_FOUND`, `UNSUPPORTED`, `NOT_IMPLEMENTED` (a native wired under its key whose implementation is not fished yet); `getMessage()` starts with the kind's display name (`FunctionNotFound: …`). An `ASSERTION_FAILED` carries a `PureAssertFailException` as its cause. |

Crossing in: primitives (Boolean, Integer, Float, Decimal, String, every Date kind, StrictTime) become
Rust values; a packageable element whose path the Rust model knows becomes that element; enum values
of known enumerations become enum values; every other node becomes a lazily read Java-backed heap
object. Crossing out: primitives through the repository factories, elements and enum values by path,
Java-backed nodes as their original object, other Rust objects as `RustCoreInstance`. Functions, maps
and unit instances raise `UNSUPPORTED` until a test needs them.

## PCT: the `Rust_JNI` adapter

Every PCT test takes its adapter function as argument 0 and calls `$f->eval(|…)`.
The platform declares a second adapter, `meta::pure::test::pct::testAdapterForRustExecution`
(`<<PCT.adapter>> {PCT.adapterName='Rust_JNI'}`), whose body is `$f->evalRust()`, and the
native `meta::pure::functions::lang::evalRust`. This module implements that native for the
interpreted engine and runs the platform corpus through it.

| Class | Role |
|---|---|
| `interpreted.EvalRust` | The native: unwraps the lambda node and calls `PureRustEvaluator.start(lambda, [])`. One evaluator per engine, created on the first call and bound to the engine's `PureRuntime`. |
| `interpreted.RustJniInterpretedExtension` | Registers `evalRust_Function_1__V_m_`; discovered through `META-INF/services/…InterpretedExtension`. Its constructor must never touch the Rust library: the extension loader drops an extension whose constructor throws, silently. |
| `pct.Test_RustJni_{Essential,Grammar}Functions_PCT` | The interpreted engine's PCT suites with the `Rust_JNI` adapter (surveyor path). Each prints `Rust_JNI <scope>: <passes> / <total>`. |
| `pct.RebaseRustJniManifests` | Regenerates the manifests from a surveyor run; not in the default surefire run. |
| `pct.Core_RustJni_PCTReportProvider` | Publishes `pct-reports/ADAPTER_{essential,grammar}_interpreted_Rust_JNI.json`, written at `process-test-classes` by `PCTReportGenerator` from the manifests alone (no Rust library needed). |

Manifests: `src/test/resources/platform/pure/essential/pct_essential_interpreted_rust_jni.json`
and `…/grammar/functions/pct_grammar_interpreted_rust_jni.json`. They sit inside a repository
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

State: `Rust_JNI essential: 303 / 327`, `Rust_JNI grammar: 126 / 136`. The lambda is lowered
through the bridge (`crates/evaluator/src/lower/`): its body, the functions it calls, the classes
and enumerations it references, and the variables it captured (`RustNodeBridge.capturedValue`,
read from the `LambdaWithContext` the interpreted engine hands over); the natives it calls are
fished from the archive (160 keys). What remains red is ranked by its first line:
`FunctionNotFound: <mangled key>` (nothing registered), `NotImplemented: <key>` (registered,
body not fished), `Unsupported: …` (a lowering or value-shape gap, e.g. a `Map` crossing the
boundary), then bare Pure texts for semantic differences. A Rust failure reaches the engine as a
`PureExecutionException` cause whose info is Pure's bare text for the Pure-level kinds (what
`assertError` compares) and the kind-prefixed message for the Rust-side diagnostics. The compiled
engine registers `evalRust` as a stub that throws at run time, so the platform compiles on both
engines.

## Build and test

```bash
cd legend-pure-rust && cargo build --workspace          # produces target/debug/libpure_rust_jni.{dylib,so,dll}
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator clean verify
```

`verify` runs the 16 boundary tests and the two `Rust_JNI` suites (327 + 136), checkstyle and
dependency analysis.

Surefire passes the library directory to the tests as the system property
`legend.pure.rust.libdir`; without it `PureRustEvaluator` falls back to
`System.loadLibrary("pure_rust_jni")`. When the library is absent the module's
tests are skipped (profile `rust-cdylib-missing-*`), so `mvn install` stays green on
hosts and CI without a Rust toolchain. Override with `-DskipTests=false` to force a run.

The bindings annotation processor module (`…-bindings-ap`) is out of the reactor:
its generator is the archived Rust `java-codegen` crate. It returns with typed proxies.
