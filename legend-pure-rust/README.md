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

Two crates: `crates/metamodel` and `crates/evaluator`. The earlier Rust
workspace (parser, compiler, tooling) is on tag
`archive/pre-evaluator-extract-2026-10`.
