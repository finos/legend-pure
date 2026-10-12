# legend-pure-rust-api

The Java side of the Rust Pure evaluator in `legend-pure-rust/` that no transport owns
(Step 2c of `legend-pure-rust/docs/rust-evaluator-mvp.md`): the evaluator as a
`FunctionExecution`, what a Java engine gives the bridge, what a Rust object looks like from
Java, how a graph node is described for the Rust side, the typed failures, and the tooling the
PCT suites and benchmarks of every transport are built from. A transport module
(`legend-pure-rust-jni`, `legend-pure-rust-ffm`) depends on this one and supplies the native
calls of `RustEvaluator`, nothing else.

## Surface

The only value currency across the boundary is `org.finos.legend.pure.m4.coreinstance.CoreInstance`,
in both directions.

| Class | Role |
|---|---|
| `RustEvaluator` | The evaluator, whichever transport carries the calls: a `FunctionExecution` bound to the interpreted engine's `PureRuntime` by `init(runtime, message)` or to any engine's `RustEngineHost` by `attach(host)` (a transport's static `over(host)`); `evaluate(function, arguments)` runs a graph node and returns the raw results as the host engine represents them, a typed failure carrying the Pure exception the engine rethrows as its cause (`PureAssertFailException` for `ASSERTION_FAILED`, else a `PureExecutionException` whose info is the bare Pure text for the Pure-level kinds and the kind-prefixed message for the Rust-side diagnostics); `start(function, arguments)` is the interpreted engine's form (arguments unwrapped from their `InstanceValue`s, result wrapped in one); `create(classifierPath, names, values)` allocates a Rust object (test support); `liveHandleCount()`; `close()` releases every proxy and frees the native context, once, every later call refused. It is the `RustHeap` of its proxies, every method `synchronized`. A transport implements nine protected methods, each one native call: `open` (the bridge and the context), `run`, `allocate`, `objectPropertyValues`, `objectPropertyNames`, `releaseHandle`, `liveHandles`, `crossingOf` (test support: which `Crossing` a node becomes) and `free`. |
| `RustEvaluators` | The evaluators a native keeps, each bound to a scope and created on the first call under it, the scope held weakly and found by identity: `latest(open)` keeps one, for the scope of the latest call, closing it when a call comes under another (the interpreted natives: one per engine, its current `PureRuntime`); `perScope(open)` keeps one per scope the garbage collector has not reclaimed, closing the evaluator of a reclaimed scope on the next call (the compiled natives: one per `ExecutionSupport`, which an evaluator over a `CompiledHost` does not reach). `call(scope, f)` runs `f` on the scope's evaluator and discards it (closed, removed) when `f` fails with an untyped `PureRustException` (a panic); a typed failure leaves it in place. Evaluators are closed before the one that replaces them is created. |
| `interpreted.InterpretedEvalRust` | The interpreted engine's body of a transport's `evalRust` native: the lambda node crosses as is and the evaluator's `start(lambda, [])` runs it, on the engine's evaluator from `RustEvaluators.latest`, created on the first call (the extension loader drops an extension whose constructor throws). A transport's native is its constructor, naming its evaluator class. |
| `compiled.CompiledEvalRustNative` | The compiled engine's code generation for a transport's `evalRust`: a call compiles to the static `evaluate(es, f)` of the entry class the transport names. |
| `compiled.CompiledEvaluators` | The run-time side that entry delegates to: one evaluator per `CompiledExecutionSupport` over a `CompiledHost` (`RustEvaluators.perScope`), the function value crossing as is, the results as compiled values (`CompiledObjects`), a typed failure rethrown as the Pure exception it carries. |
| `RustEngineHost` | What the bridge needs from the engine it serves: the `ProcessorSupport`, a node's property names, a lambda's captured values, the definition a lambda value stands for (the closure cache's key), and the engine's own representation of each primitive and of a map. `interpreted.InterpretedHost` (keys from the node, captures from `LambdaWithContext`, a lambda's repository synthetic id, primitives from the `ModelRepository`, maps as `MapCoreInstance`) and `compiled.CompiledHost` (keys from the class's properties, captures from the generated lambda's open variables, no lambda definition, primitives as `ValCoreInstance`, maps as a `PureMap` carried in a `CompiledValue`). |
| `NodeBridge` | The host side of the node bridge: the Java graph nodes the Rust side holds by id (ids never reused; `forget` drops the ones Rust released), their description in one `NodeDescription` (kind, classifier path, primitive payload, a date's or strict time's components, a lambda's definition id, the source span), their reads on demand (property values, keys, captured values), the elements and enum values Rust names by path, the host's primitives, maps, dates and strict times (built from components), the proxy of a Rust object (`rustObject`, one per handle, `RustProxies`), and `failureOf`, how a Java failure is reported to Rust (its kind and text). A transport's bridge class extends it: the JNI one only for the class name its crate resolves method ids on, the FFM one with the encoding of the C ABI. |
| `NodeDescription` | The record `NodeBridge.describe` fills; its kind constants are mirrored in `crates/evaluator/src/bridge/description.rs`. |
| `RustHeap` | One evaluator's Rust heap as the proxies read it: property values and names by handle, the graph the classifier paths resolve in, release. `RustEvaluator` implements it. |
| `RustCoreInstance` | A Rust heap object seen from Java as a read-only `CoreInstance` over a `RustHeap`; its classifier is the Java graph node for the Rust classifier path, its name the `name` of a model element or else an anonymous one, as a Java instance's. One proxy per Rust handle; `close()` releases it. |
| `RustProxies` | The bridge's proxy table: one `RustCoreInstance` per handle while Java reaches it, held weakly; the handle of a proxy the garbage collector reclaimed is released at the evaluator's next call, on its own thread (the interpreted engine never closes the results it reads). |
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
own, which is what surefire runs. It is test code (`src/test/java`, package `pct`, and the
boundary cases in `org.finos.legend.pure.rust`, beside the package-private hooks they read): the module's
test-jar carries it, which `-jni`, `-ffm` and `-bench` take at test scope; the main jar holds
none of it, and neither JUnit nor Jackson is on its compile classpath.

| Class | Role |
|---|---|
| `pct.PctSuites` (test) | `interpreted(adapter, scope, manifestPath)` and `compiled(…)` build the surveyor suite of a scope on that engine and print `<adapter> <scope>[ (compiled)]: <passes> / <total>`. |
| `pct.PctManifestRebase` (test) | `interpreted(adapter, scope, manifestPath)` and `compiled(…)` regenerate one manifest from a surveyor run under the committed one (a FAIL or ERROR becomes an exclusion holding the first line of its message; a PASS reported as an expected failure keeps its committed entry; an excluded test that now passes is dropped) and write it under the directory the system property `legend.pct.rust.manifestDir` names. A second run reproduces the file byte for byte. |
| `pct.PctExpressionsBenchmark` (test) | Times every PCT test under a list of modes, each an engine with an adapter: a host mode (the engine's own adapter) or a Rust mode (a transport's adapter plus where its split of the last call is read: lowering, run of the lowered closure, bridge reads during each). Per test and mode, in nanoseconds: first run, median, p90 and minimum, and the medians of the Rust split, to `target/benchmarks/pct-expressions.csv`; a summary per mode over the tests every mode completed and the ten tests where the Rust modes lose the most against their host modes, to `pct-expressions.md`. Properties `legend.bench.warmup`, `legend.bench.runs`, `legend.bench.batch` (calls per timed sample), `legend.bench.filter`. |
| `pct.RustPlatformGraph` (test) | The platform graph loaded once per JVM from the `.par` archives on the classpath, for tests that bind an evaluator to it. |
| `RustEvaluatorBoundaryCases` (test) | The 26 boundary cases every transport passes over the bootstrap-only Rust model (primitives, numbers, floats, strict times and collections round-tripped; elements, host nodes and primitives crossing; Rust objects, their proxies, names, maps and release; lowering of functions, lambdas, classes, enumerations and Pure-bodied calls; typed failures with their kind, info, span and Pure cause; a Java failure inside a host read; ten thousand round trips leaving no handle; a closed evaluator), sharing one evaluator per class. A transport's boundary test extends it with three methods (`newEvaluator()`, `over(host)`, `lastTimings()`) and adds the cases only it has; its name matches no surefire pattern, and this module runs none of it. |
| `pct.BenchmarkNativeExpressions` (test) | The Java half of the apples-to-apples benchmark, with no Rust involved: the twelve expressions of `src/test/resources/bench/expressions.pure`, compiled into each engine at run time and timed there (the compiled engine both through its `start` entry and by invoking the generated method directly); the Rust half is `crates/evaluator/examples/expressions.rs`, the same expressions built with the builders and timed under `cargo run --release --example expressions`. Both check the same results. The transports' modes are added through the `transportModes` hook, so every mode runs in one JVM (`-bench`'s `BenchmarkTransportExpressions`); the module's test-jar carries the class and `bench/expressions.pure` to it. Results in `target/benchmarks/expressions-java.csv` (median, p90, minimum ns per call, and the Rust split for a mode that crosses). Not in the default run; `mvn test -pl legend-pure-rust/java/legend-pure-rust-api -Dtest=BenchmarkNativeExpressions -DfailIfNoTests=false`. |

## Build

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-rust/java/legend-pure-rust-api clean verify
```

The module compiles with release 17 (the rest of the build stays at release 8) and needs
JDK 17: it joins the reactor through the profile `rust-evaluator-jdk17` in
`legend-pure-rust/java/pom.xml`, active on JDK 17 and later, so a JDK 11 build neither builds
nor installs it and `-pl` cannot select it there. Nothing here loads a Rust library; its
default test run is `TestRustEvaluators`, the evaluator holder over evaluators with no native
context (an execution support nothing else reaches is reclaimed and its evaluator closed, a new
runtime replaces the interpreted evaluator, a panic discards one).
