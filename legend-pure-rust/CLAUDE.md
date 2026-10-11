# CLAUDE.md — legend-pure-rust

Guide for Claude Code when working inside this Cargo workspace.

**The plan is `docs/rust-evaluator-mvp.md`.** It is the single source of truth
for the Rust direction: what exists, what comes next, what is deliberately left
out. Read its §0 before doing anything here. If this file and the plan disagree,
the plan wins and this file gets fixed.

## What is here

Three crates, nothing else:

| Crate | Package | Holds |
|---|---|---|
| `crates/metamodel` | `legend-pure-metamodel` | Element tables (`PureModel`, `Element`, `nodes/`), the expression tree (`types::ValueSpec`, `types::ExprKind`), the M3 bootstrap (`bootstrap`, embeds `m3.pure` at compile time), function-name mangling (`fqn`). |
| `crates/evaluator` | `legend-pure-evaluator` | Values, heap, variable context, dispatch (`eval`), the native registry (`native`, fished one at a time), hooks, builder, the `builders` test helpers. |
| `crates/jni` | `legend-pure-jni` (cdylib `libpure_rust_jni`) | The JNI boundary behind the Java class `org.finos.legend.pure.rust.PureRustEvaluator` (Step 2): context, value marshalling, handle table, typed exceptions. Unsafe by nature; the only crate without `forbid(unsafe_code)`. |

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

CI (`.github/workflows/rust.yml` at the repository root) runs these plus
`cargo build --workspace` and a separate doctest pass.

## PCT through the Java harness (Step 2)

The platform PCT corpus runs through the Rust evaluator as the `Rust_JNI`
adapter of the interpreted Java engine: the test's lambda crosses the boundary
as a `CoreInstance`, nothing else changes in the harness. The number each
suite prints is the progress metric.

```bash
cargo build --workspace                                   # the cdylib the Java module loads
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator clean verify
#   -> Rust_JNI essential: 320 / 327, Rust_JNI grammar: 132 / 136 (interpreted engine)
#      Rust_JNI essential (compiled): 321 / 327, grammar (compiled): 132 / 136 (plus 22 boundary tests)
# after a change that moves tests: regenerate the manifests, review the diff, verify again
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator \
  -Dtest=RebaseRustJniManifests -DfailIfNoTests=false
```

The manifests under that module's `src/test/resources/platform/` are generated,
never edited by hand; they rank what is left by kind: `FunctionNotFound: <key>`
(nothing registered), `NotImplemented: <key>` (registered under its key through
`native::NotYet`, body not fished), `Unsupported: …` (a lowering or value-shape
gap), then bare Pure error texts (a semantic difference). A lambda reaching
`JniContext::start` is lowered by `crates/evaluator/src/lower/` (Phase A, model
read-only) and run after the model grew (Phase B). Natives are fished from the
archive one `impl NativeFunction` at a time (the natives round of 2026-10-11
did it one module at a time on disjoint files). `tests/registry.rs` pins the
exact key set by hand: every `register` call must have its key listed there,
and the test fails otherwise. Root Maven installs need `clean` (the PAR
plugin trips on a stale descriptor otherwise). The Java module compiles with
release 17 and joins the reactor only on JDK 17+ (profile
`rust-evaluator-jdk17` in `legend-pure-runtime/pom.xml`): run its Maven
commands with JDK 17; a JDK 11 build leaves it out.

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
`tracing`, and for the JNI crate only `jni`, `slotmap`. `cargo tree --workspace
-e normal --depth 1` must show nothing else, and
`grep -rnE 'purem|pelt|legend_pure_parser|legend_pure_runtime|serde|linkme|postcard|toml' crates/`
must return nothing.

## Conventions

- Copyright header on every `.rs`, `.toml` and `.sh` file (the script checks).
- `#![deny(missing_docs)]` on all three crates, `#![forbid(unsafe_code)]` on
  `metamodel` and `evaluator`; doc comments must not link into
  archived modules.
- Commit messages record every cut or deviation from the plan's extraction map.
