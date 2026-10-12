# legend-pure-rust-ffm

The FFM transport of the Java boundary to the Rust Pure evaluator in `legend-pure-rust/`
(Step 2c phase 2 of `legend-pure-rust/docs/rust-evaluator-mvp.md`): a client of the C ABI
of `libpure_rust_ffi`, written from its header `legend-pure-rust/crates/ffi/include/pure_rust.h`
alone, calling it through the Foreign Function and Memory API of JDK 25. Everything
transport-free (the hosts, the node bridge's host side, the proxies, the exception kinds, the
PCT and benchmark tooling) is in `legend-pure-rust-api`, which this module
depends on (the tooling through its test-jar, at test scope); `legend-pure-rust-jni` is the other transport, over JNI on
JDK 17, with which this one is measured side by side in `legend-pure-rust-bench`. This module
does not depend on `-jni`.

## The header's rules, as this client keeps them

| Rule | Here |
|---|---|
| Only C data crosses: integers, UTF-8 bytes with their count, opaque handles, the flat structs. | `PureAbi` declares every struct as a `StructLayout` in the header's field order (8-byte fields first, `int32_t` fields after, `pure_failure` with its four bytes of tail padding) and asserts every size once, when the class initialises: 16, 176, 16, 32, 96, 16, 32, 48, 136, 32, 40. A string is a pointer plus a byte count, never NUL-terminated; an absent string is a NULL pointer, the empty string a non-NULL pointer with a count of zero. A Rust-owned string is read through `reinterpret(len)`. |
| Each side owns what it allocates. | What the host writes for a call (the function's description, the argument slots, an object's properties, every string) lives in a confined `Arena` opened for that call and closed when it returns; Rust reads it in place and keeps no pointer. What a callback pushes lives in a confined arena opened inside the callback. A `pure_result` is read into host objects and released with `pure_result_free` before the call returns; the `pure_failure` buffer is read before any other call. |
| Failures are a status plus a payload. | Every downcall that returns a `pure_status` other than OK is followed, on the same thread and before any other call, by `pure_last_failure`, raised as a `PureRustEvaluationException` carrying the kind, info, source, call stack and the three constraint fields; `PURE_FAILURE_PANIC` is raised as the untyped `PureRustException`, and the natives discard the evaluator on it, as they do for the JNI transport. |
| No unwind crosses in either direction. | Every upcall stub catches `Throwable`, records it through `pure_reply_fail` with the kind `NodeBridge.failureOf` classifies (`UNSUPPORTED`, `NOT_IMPLEMENTED` and `FUNCTION_NOT_FOUND` keep their kind, anything else is an execution error naming the read) and returns `PURE_STATUS_FAILED`. |
| One thread in a context at a time. | Every method of the evaluator is `synchronized`; the per-call arenas are confined to the calling thread, which is also the thread the upcalls run on. |
| Node ids are stable until `forget`. | `NodeBridge` assigns them; the `forget` callback drops them. |
| Callbacks only from inside an entered call. | The four stubs and the `pure_host_table` live in one shared `Arena` per evaluator, created in `init`/`over` and closed in `close()` after `pure_context_free` returned OK, so no stub is freed while a context could still call it. |
| One version function. | `pure_abi_version()` is compared with the header's `PURE_ABI_VERSION` when `PureAbi` initialises, before any other call; a mismatch is refused with an `IllegalStateException`. |

The library is loaded from the directory the system property `legend.pure.rust.libdir`
names (the JNI module's property), else through `System.loadLibrary("pure_rust_ffi")`, and its
symbols come from `SymbolLookup.loaderLookup()`; the seventeen downcall handles are resolved
once. `System.load` and `reinterpret` are restricted methods: every JVM that loads this
module runs with `--enable-native-access=ALL-UNNAMED` (surefire does), or prints the JEP 472
warning.

## Surface

| Class | Role |
|---|---|
| `PureAbi` | The ABI: constants, layouts and their size assertions, field offsets, the downcall handles, the descriptors of the four callbacks, and the string helpers (`Text`, `writeString`, `readString`, `readText`). |
| `PureRustFfmEvaluator` | The FFM transport of `-api`'s `RustEvaluator` (whose README has the surface: `init`, `evaluate`, `start`, `create`, `liveHandleCount`, `close`): its nine native calls over one `pure_context` and the shared arena of its stubs (`pure_context_new`, `pure_start`, `pure_test_object_new` with the classifier read back through `pure_object_classifier`, `pure_object_property_values`, `pure_object_property_names`, `pure_object_release`, `pure_live_handle_count`, `pure_test_crossing`, `pure_context_free` then the arena closed); `over(host)` binds an evaluator to any engine, `lastTimings()` is the Rust side's split of the last call as a `Timings` record. A failure kind or crossing code the header does not define is a protocol error, raised as the untyped `PureRustException`. |
| `FfmNodeBridge` | `NodeBridge` plus the transport: a `NodeDescription` written into a `pure_description` segment, slots and properties; a `pure_leaf` decoded into a `Leaf` record and built into a host object through the engine's factories (a Float crosses as a `double` and reaches the host as the text Rust prints for it; a map as its entries; a Rust object as one `RustCoreInstance` per handle); and the four callbacks (`answerPropertyValues`, `answerCapturedValues`, `answerKeys`, `acceptForget`) as upcall stubs bound to the instance. |

Crossing in and out is the JNI module's, over the same bridge core: primitives become Rust
values, a packageable element whose path the Rust model knows becomes that element, enum values
of known enumerations become enum values, every other node becomes a host node read on demand
through the callbacks; results come back by kind. A lambda is lowered once per definition and
cached for the evaluator's lifetime.

## PCT: the `Rust_FFM` adapter

This module's Pure repository, `platform_rust_ffm` (`src/main/resources/platform_rust_ffm.definition.json`,
pattern `meta::pure::rust::ffm`, depending on `platform`; `ffm.pct.RustFfmCodeRepositoryProvider`; a
PAR from `legend-pure-maven-generation-par` and PELT from `legend-pure-maven-compiler`), declares the
adapter `meta::pure::rust::ffm::testAdapterForRustFfmExecution` (`<<PCT.adapter>> {PCT.adapterName='Rust_FFM'}`,
`RustFfmCodeRepositoryProvider.rustFfmAdapter`), whose body is `$f->evalRustFfm()`, and the native
`meta::pure::rust::ffm::evalRustFfm`. This module implements that native for both Java engines and
runs the platform corpus through each, beside `Rust_JNI`.

| Class | Role |
|---|---|
| `ffm.interpreted.EvalRustFfm` | The native: unwraps the lambda node and calls `PureRustFfmEvaluator.start(lambda, [])`. One evaluator per engine, bound to its `PureRuntime`, replaced when the runtime changes, discarded after a panic. |
| `ffm.interpreted.RustFfmInterpretedExtension` | Registers `evalRustFfm_Function_1__V_m_`; discovered through `META-INF/services/…InterpretedExtension`. Its constructor never touches the library. |
| `ffm.compiled.RustFfmCompiledExtension` | Registers the generation native `ffm.compiled.EvalRustFfm` for `platform_rust_ffm`; discovered through `META-INF/services/…CompiledExtension`. The module generates its repository's Java at build time (`JavaCodeGeneration platform_rust_ffm` at `compile`), compiled with the tests. |
| `ffm.compiled.CompiledEvalRustFfm` | The compiled engine's `evalRustFfm`: the generated code calls the static `CompiledEvalRustFfm.evaluate(es, f)`; results cross as compiled values through `CompiledObjects`. One evaluator per `CompiledExecutionSupport`. |
| `ffm.pct.Test_RustFfm_{Essential,Grammar}Functions_PCT` | The interpreted engine's suites with the `Rust_FFM` adapter; each prints `Rust_FFM <scope>: <passes> / <total>`. |
| `ffm.pct.Test_RustFfmCompiled_{Essential,Grammar}Functions_PCT` | The same corpus through the compiled engine; prints `Rust_FFM <scope> (compiled): <passes> / <total>`. |
| `ffm.pct.RebaseRustFfmManifests` | Regenerates the four manifests (`PctManifestRebase`); not in the default surefire run. |
| `ffm.pct.Core_RustFfm_PCTReportProvider` | Publishes `pct-reports/ADAPTER_{essential,grammar}_{interpreted,compiled}_Rust_FFM.json`, written at `process-test-classes` by each engine's `PCTReportGenerator` from the manifests alone (no library loaded, no native-access flag needed there). |

Manifests: `src/test/resources/platform/pure/essential/pct_essential_{interpreted,compiled}_rust_ffm.json`
and `…/grammar/functions/pct_grammar_{interpreted,compiled}_rust_ffm.json`, generated, never
hand-written. Their content equals the `Rust_JNI` manifests line for line except the adapter
name: the same bridge core produces the same failures through either transport, and any
difference is a defect of this module.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  mvn test -pl legend-pure-rust/java/legend-pure-rust-ffm \
  -Dtest=RebaseRustFfmManifests -DfailIfNoTests=false
```

State: `Rust_FFM essential: 339 / 346`, `Rust_FFM grammar: 133 / 137` on the interpreted
engine; `340 / 346` and `133 / 137` on the compiled engine, the `Rust_JNI` numbers.

## Build and test

```bash
cd legend-pure-rust && cargo build --workspace     # target/debug/libpure_rust_ffi.{dylib,so,dll}
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  mvn -pl legend-pure-rust/java/legend-pure-rust-api,legend-pure-rust/java/legend-pure-rust-jni,legend-pure-rust/java/legend-pure-rust-ffm clean verify
```

`verify` runs the 25 boundary tests and the four `Rust_FFM` suites, checkstyle (10.21.4 for
this module, the root `checkstyle.xml`) and dependency analysis.

The module compiles with release 25 and joins the reactor only through the profile
`rust-evaluator-jdk25` in `legend-pure-rust/java/pom.xml`, active on JDK 25 and later; a JDK 17
or 11 build neither builds nor installs it and `-pl` cannot select it there. Surefire passes
the library directory as `legend.pure.rust.libdir` and `--enable-native-access=ALL-UNNAMED`;
when the library is absent the module's tests are skipped (profile `rust-cdylib-missing-*`),
so `mvn install` stays green on hosts and CI without a Rust toolchain. Override with
`-DskipTests=false` to force a run.

The benchmarks that run this transport beside `Rust_JNI` (`BenchmarkPctExpressions` under six
modes, `BenchmarkTransportExpressions`) are in `legend-pure-rust-bench`, whose README has the
commands. The module publishes a test-jar for them: it carries the Java generated for
`platform_rust_ffm`, which the compiled engine's `Rust_FFM` mode loads.
