# legend-pure-rust

A Pure evaluator in Rust, built in the order laid out in
[`docs/rust-evaluator-mvp.md`](docs/rust-evaluator-mvp.md): the evaluator and
the metamodel it needs first, proven over the M3 bootstrap with hand-built
nodes; then a Java boundary and a PCT run that starts red; then PELT, the
binary graph the Java compiler already writes, so the pass rate can climb.

```bash
cargo build --workspace
cargo test --workspace
```

Four crates: `crates/metamodel`, `crates/evaluator`, `crates/ffi` (the C ABI,
`libpure_rust_ffi`, behind the Java module `java/legend-pure-rust-ffm`) and
`crates/jni` (`libpure_rust_jni`, behind `java/legend-pure-rust-jni`); both
Java modules share `java/legend-pure-rust-api`. The earlier Rust
workspace (parser, compiler, tooling) is on tag
`archive/pre-evaluator-extract-2026-10`.
