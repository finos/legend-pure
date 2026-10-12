# legend-pure-rust-api

The Java side of the Rust Pure evaluator in `legend-pure-rust/` that no transport owns
(Step 2c of `legend-pure-rust/docs/rust-evaluator-mvp.md`): what a Java engine gives the
bridge, what a Rust object looks like from Java, how a graph node is described for the
Rust side, the typed failures, and the tooling the PCT suites and benchmarks of every
transport are built from. A transport module (`legend-pure-rust-jni`
today, `-ffm` in phase 2) depends on this one and adds the evaluator that crosses.

## Surface

The only value currency across the boundary is `org.finos.legend.pure.m4.coreinstance.CoreInstance`,
in both directions.

| Class | Role |
|---|---|
| `RustEngineHost` | What the bridge needs from the engine it serves: the `ProcessorSupport`, a node's property names, a lambda's captured values, the definition a lambda value stands for (the closure cache's key), and the engine's own representation of each primitive and of a map. `interpreted.InterpretedHost` (keys from the node, captures from `LambdaWithContext`, a lambda's repository synthetic id, primitives from the `ModelRepository`, maps as `MapCoreInstance`) and `compiled.CompiledHost` (keys from the class's properties, captures from the generated lambda's open variables, no lambda definition, primitives as `ValCoreInstance`, maps as a `PureMap` carried in a `CompiledValue`). |
| `NodeBridge` | The host side of the node bridge: the Java graph nodes the Rust side holds by id (ids never reused; `forget` drops the ones Rust released), their description in one `NodeDescription` (kind, classifier path, primitive payload, a date's or strict time's components, a lambda's definition id, the source span), their reads on demand (property values, keys, captured values), the elements and enum values Rust names by path, dates and strict times built from components through the host, and `failureOf`, how a Java failure is reported to Rust (its kind and text). A transport's bridge class extends it with what its crate calls. |
| `NodeDescription` | The record `NodeBridge.describe` fills; its kind constants are mirrored in `crates/jni/src/java_node.rs`. |
| `RustHeap` | One evaluator's Rust heap as the proxies read it: property values and names by handle, the graph the classifier paths resolve in, release. Each transport's evaluator implements it. |
| `RustCoreInstance` | A Rust heap object seen from Java as a read-only `CoreInstance` over a `RustHeap`; its classifier is the Java graph node for the Rust classifier path, its name the `name` of a model element or else an anonymous one, as a Java instance's. One proxy per Rust handle; `close()` releases it. |
| `RustProxies` | A transport bridge's proxy table: one `RustCoreInstance` per handle while Java reaches it, held weakly; the handle of a proxy the garbage collector reclaimed is released at the evaluator's next call, on its own thread (the interpreted engine never closes the results it reads). |
| `compiled.CompiledObjects` | Rust objects as the compiled engine's generated code holds them: copies into the generated `_Impl` class of their classifier, keeping identity and cycles, releasing the proxies read; `CompiledValue` carries a compiled value that is not a `CoreInstance` (a `PureMap`) across the boundary. |
| `PureRustEvaluationException` | Typed failure: `getKind()` is one of `EXECUTION_ERROR`, `ASSERTION_FAILED`, `CONSTRAINT_VIOLATION`, `FUNCTION_NOT_FOUND`, `UNSUPPORTED`, `NOT_IMPLEMENTED` (a native wired under its key whose implementation is not fished yet); `getMessage()` starts with the kind's display name (`FunctionNotFound: …`), `getInfo()` is the bare text. An `ASSERTION_FAILED` carries a `PureAssertFailException` as its cause. `PureRustException` is the untyped failure (a panic, a wiring error). |

## PCT and benchmark tooling

Every PCT test takes its adapter function as argument 0 and calls `$f->eval(|…)`; a Rust
transport declares an adapter in its own Pure repository (`Rust_JNI` in `platform_rust_jni`,
`Rust_FFM` in `platform_rust_ffm`) and runs the platform corpus through each Java engine with it.
This module ships the repository the transports share, `platform_rust`
(`src/main/resources/platform_rust.definition.json`, pattern `meta::pure::rust::standalone`,
depending on `platform`; `pct.RustCodeRepositoryProvider`; a PAR from
`legend-pure-maven-generation-par` and PELT from `legend-pure-maven-compiler`), which declares
the `Rust_Native` adapter `meta::pure::rust::standalone::testAdapterForRustNativeExecution`
(body `$f->eval()`, run from `legend-pure-rust/crates/evaluator/src/pct.rs`, never by a Java engine). The tooling here is parametrised by that adapter and
by the transport's manifests, and each transport module invokes it from test classes of its
own, which is what surefire runs.

| Class | Role |
|---|---|
| `pct.PctSuites` | `interpreted(adapter, scope, manifestPath)` and `compiled(…)` build the surveyor suite of a scope on that engine and print `<adapter> <scope>[ (compiled)]: <passes> / <total>`. |
| `pct.PctManifestRebase` | `interpreted(adapter, scope, manifestPath)` and `compiled(…)` regenerate one manifest from a surveyor run under the committed one (a FAIL or ERROR becomes an exclusion holding the first line of its message; a PASS reported as an expected failure keeps its committed entry; an excluded test that now passes is dropped) and write it under the directory the system property `legend.pct.rust.manifestDir` names. A second run reproduces the file byte for byte. |
| `pct.PctExpressionsBenchmark` | Times every PCT test under a list of modes, each an engine with an adapter: a host mode (the engine's own adapter) or a Rust mode (a transport's adapter plus where its split of the last call is read: lowering, run of the lowered closure, bridge reads during each). Per test and mode, in nanoseconds: first run, median, p90 and minimum, and the medians of the Rust split, to `target/benchmarks/pct-expressions.csv`; a summary per mode over the tests every mode completed and the ten tests where the Rust modes lose the most against their host modes, to `pct-expressions.md`. Properties `legend.bench.warmup`, `legend.bench.runs`, `legend.bench.batch` (calls per timed sample), `legend.bench.filter`. |
| `pct.RustPlatformGraph` | The platform graph loaded once per JVM from the `.par` archives on the classpath, for tests that bind an evaluator to it. |
| `pct.BenchmarkNativeExpressions` (test) | The Java half of the apples-to-apples benchmark, with no Rust involved: the twelve expressions of `src/test/resources/bench/expressions.pure`, compiled into each engine at run time and timed there (the compiled engine both through its `start` entry and by invoking the generated method directly); the Rust half is `crates/evaluator/examples/expressions.rs`, the same expressions built with the builders and timed under `cargo run --release --example expressions`. Both check the same results. A transport module adds its modes through the `transportModes` hook, so every mode runs in one JVM (`-ffm`'s `BenchmarkTransportExpressions`); the module's test-jar carries the class and `bench/expressions.pure` to it. Results in `target/benchmarks/expressions-java.csv` (median, p90, minimum ns per call, and the Rust split for a mode that crosses). Not in the default run; `mvn test -pl legend-pure-rust/java/legend-pure-rust-api -Dtest=BenchmarkNativeExpressions -DfailIfNoTests=false`. |

## Build

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-rust/java/legend-pure-rust-api clean verify
```

The module compiles with release 17 (the rest of the build stays at release 8) and needs
JDK 17: it joins the reactor through the profile `rust-evaluator-jdk17` in
`legend-pure-rust/java/pom.xml`, active on JDK 17 and later, so a JDK 11 build neither builds
nor installs it and `-pl` cannot select it there. Nothing here loads a Rust library; its
default test run is empty.
