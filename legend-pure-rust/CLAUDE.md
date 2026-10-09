# CLAUDE.md — legend-pure-rust

Guide for Claude Code when working inside this Cargo workspace.

**The plan is `docs/rust-evaluator-mvp.md`.** It is the single source of truth
for the Rust direction: what exists, what comes next, what is deliberately left
out. Read its §0 before doing anything here. If this file and the plan disagree,
the plan wins and this file gets fixed.

## What is here

Two crates, nothing else:

| Crate | Package | Holds |
|---|---|---|
| `crates/metamodel` | `legend-pure-metamodel` | Element tables (`PureModel`, `Element`, `nodes/`), the expression tree (`types::ValueSpec`, `types::ExprKind`), the M3 bootstrap (`bootstrap`, embeds `m3.pure` at compile time), function-name mangling (`fqn`). |
| `crates/evaluator` | `legend-pure-evaluator` | Values, heap, variable context, dispatch (`eval`), the native registry (`native`, fished one at a time), hooks, builder. |

The previous Rust workspace (parser, compiler, LSP, DSL crates, CLI, binary
snapshot format) is frozen at tag `archive/pre-evaluator-extract-2026-10` and
branch `legend-pure-rust-archive`. Read from it with
`git show archive/pre-evaluator-extract-2026-10:legend-pure-rust/<path>`; never
check it out over this directory.

## Build and test

```bash
cargo build --workspace
cargo test --workspace
cargo clippy --workspace --all-targets -- -D warnings
cargo fmt --check
RUSTDOCFLAGS=-Dwarnings cargo doc --workspace --no-deps
./scripts/check-copyright.sh
```

CI (`.github/workflows/rust.yml` at the repository root) runs exactly these.
Until Step 1 fishes `plus` and `times`, two seed tests in
`crates/evaluator/tests/building_blocks.rs` are red and CI skips them by name.

## The six rules (plan §0)

1. Only the evaluator and the language metamodel it needs live here.
2. No other crates. A later step adds one only when the plan says so.
3. The evaluator depends on nothing Pure-shaped except the bootstrap (`m3.pure`).
4. Natives are fished one at a time, when a test or a PCT failure asks for one.
5. Tests build the evaluator's inputs directly (literals, calls, lambdas,
   classes, functions) and hand them to the evaluator. No source text, no loader.
6. Much already exists on the archive: fish it (copy the smallest compiling
   unit and its test), never rewrite it, never copy a whole file for one
   function. Plan Appendix A says where everything is.

Allowed dependencies: `smol_str`, `thiserror`, `rust_decimal`, `im-rc`, `jiff`,
`tracing`. `cargo tree --workspace -e normal --depth 1` must show nothing else,
and `grep -rnE 'purem|pelt|legend_pure_parser|legend_pure_runtime|serde|linkme|postcard' crates/`
must return nothing.

## Conventions

- Copyright header on every `.rs`, `.toml` and `.sh` file (the script checks).
- `#![deny(missing_docs)]` on both crates; doc comments must not link into
  archived modules.
- Commit messages record every cut or deviation from the plan's extraction map.
