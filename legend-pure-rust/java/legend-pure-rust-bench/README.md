# legend-pure-rust-bench

The benchmarks that run both transports of the Rust Pure evaluator side by side (Step 2c of
`legend-pure-rust/docs/rust-evaluator-mvp.md`; the report is
`legend-pure-rust/docs/benchmarks-2026-10-11.md`). Test code only: no main sources, an empty
jar, nothing any other module depends on. It depends, at test scope, on
`legend-pure-rust-api` (and its test-jar: `PctExpressionsBenchmark`, `BenchmarkNativeExpressions`
and `bench/expressions.pure`), `legend-pure-rust-jni` and `legend-pure-rust-ffm` (and their
test-jars, which carry the Java generated for `platform_rust_jni` and `platform_rust_ffm` that the
compiled engine's `Rust_JNI` and `Rust_FFM` modes load). Neither transport depends on the other;
deleting JNI removes its modes here and nothing else.

| Class | Role |
|---|---|
| `bench.BenchmarkPctExpressions` | `PctExpressionsBenchmark` under six modes: each engine with its own adapter, with `Rust_JNI` and with `Rust_FFM`, so one run reports both transports with the Rust side's split per call. Writes `target/benchmarks/pct-expressions.{csv,md}`. |
| `bench.BenchmarkTransportExpressions` | The twelve expressions of `-api`'s `BenchmarkNativeExpressions` under every Java mode and both transports in one JVM: the interpreted engine, the compiled engine through `start` and through the generated method, then each expression's function node through `PureRustEvaluator.evaluate` and `PureRustFfmEvaluator.evaluate` over the interpreted engine's graph (`rust-jni`, `rust-ffm`) and over the compiled engine's (`…/compiled-host`), batched, result checked, with the Rust split per call. Writes `target/benchmarks/expressions-java.csv`. |

The single-transport benchmarks stay with their module: `-jni`'s `BenchmarkPctExpressions` (four
modes, JDK 17) and `-api`'s `BenchmarkNativeExpressions` (the Java engines alone).

## Run

Neither class is in the default surefire run. The module compiles with release 25 and joins the
reactor through the profile `rust-evaluator-jdk25` in `legend-pure-rust/java/pom.xml`. Run it
with the three other modules in one reactor, so their jars and test-jars of this checkout are the
ones used, with an optimised Rust side:

```bash
cd legend-pure-rust && cargo build --release --workspace
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 25) \
  mvn test -pl legend-pure-rust/java/legend-pure-rust-api,legend-pure-rust/java/legend-pure-rust-jni,legend-pure-rust/java/legend-pure-rust-ffm,legend-pure-rust/java/legend-pure-rust-bench \
  -Dtest=org.finos.legend.pure.rust.bench.BenchmarkPctExpressions \
  -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false \
  -Dlegend.rust.libdir=$PWD/legend-pure-rust/target/release
# the twelve expressions under every mode: -Dtest=BenchmarkTransportExpressions
```

The class name is qualified because `-jni` has a `BenchmarkPctExpressions` of its own (four
modes), which a bare name would also run. Surefire passes the library directory as
`legend.pure.rust.libdir` and `--enable-native-access=ALL-UNNAMED`; both cdylibs must be in it.
When either is absent from `legend-pure-rust/target/debug` the module's tests are skipped
(profiles `rust-cdylib-missing-{jni,ffi}-*`); override with `-DskipTests=false`.
