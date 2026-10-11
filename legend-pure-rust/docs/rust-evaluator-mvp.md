# A Pure Evaluator in Rust — MVP Proposal

> **Status: in execution, October 2026 (revised 2026-10-11). Steps 0–2 done
> 2026-10-09; the Java-lambda lowering (Step 2 "Next") done 2026-10-10;
> natives round 1, Step 2b (the evaluator's own cost), natives round 2 and
> the bridge's cost done 2026-10-11 (`Rust_JNI` 320 / 327 and 132 / 136,
> compiled 321 / 327 and 132 / 136; the bridge within 1.15x of the
> interpreted engine's own adapter). Next: Step 3, read PELT, with the
> `Directory` trait of Step 4 (Appendix C item 6). Archive tag
> `archive/pre-evaluator-extract-2026-10`.**
> This document is
> self-contained. It assumes the reader knows Legend Pure and legend-engine as
> they exist in Java today and nothing else. It describes what a minimum viable
> Rust evaluator is, which existing technologies it plugs into, and a sequence
> of steps where each step ships something usable and proves something
> specific.

---

## 0. Orientation for a fresh session

Read this first; everything below assumes it.

**Rules for this effort (set by the author, 2026-10-08).**

1. **Only the evaluator and the language metamodel it needs move.** Nothing
   else from earlier Rust work is brought along: no parser, no compiler, no
   LSP, no DSL crates. They stay on an archive branch.
2. **No other crates.** The new workspace is two crates — `metamodel` and
   `evaluator` — plus whatever a later step explicitly adds (a PELT reader, a
   JNI boundary). Each addition is named in its step.
3. **The evaluator depends on nothing Pure-shaped except the bootstrap.** No
   binary graph format, no `.pure` repository, no platform load. The
   bootstrap is the M3 metamodel itself (primitives, `Any`, `Nil`, and the
   classes of `m3.pure`), which the evaluator needs to answer `type()`,
   `instanceOf` and dispatch questions.
4. **Natives are fished one at a time, when a test or a PCT failure asks for
   one.** No native file moves wholesale.
5. **Tests build the evaluator's inputs directly** — literals, collections,
   function expressions, lambdas, classes, functions — and hand them to the
   evaluator. No source text, no loader. The PELT import comes *after* the
   evaluator is proven this way **and after the Java boundary and the PCT
   adapter exist**, so every PELT increment is measured from its first commit.
6. **Much of this already exists.** Appendix A says, file by file, what to
   take from the archive and what to strip. Extraction is a copy with listed
   edits, not a rewrite.

| What | Where |
|---|---|
| This document | `legend-pure-rust/docs/rust-evaluator-mvp.md` on branch `legend-pure-rust` of the `legend-pure` fork (`rafaelbey/legend-pure`). It is the single source of truth for the Rust direction. |
| The Rust workspace **after Step 0** | `legend-pure-rust/` (Cargo workspace, edition 2024) holding `crates/metamodel` and `crates/evaluator`, plus `crates/jni` since Step 2. Build and test: `cargo build --workspace`, `cargo test --workspace`. `legend-pure-rust/CLAUDE.md` is rewritten in Step 0 to match. |
| The Rust workspace **before Step 0** (the archive) | Tag `archive/pre-evaluator-extract-2026-10` and branch `legend-pure-rust-archive`, both at the last commit before extraction. Every path in Appendix A is an archive path. |
| The Java stack (upstream FINOS code) | Sibling directories at the repository root: `legend-pure-core/`, `legend-pure-runtime/`, `legend-pure-maven/`, … Requires JDK 11 or 17 and Maven. |
| The bootstrap source | `legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/grammar/m3.pure`, embedded at compile time by the metamodel crate. This is the one `.pure` file the evaluator may depend on. |
| PELT files for the platform, locally (read by Rust from Step 3; the Java-side PCT discovery of Step 2 uses the `.par` jars from the same Maven build, which is already a Step 0 precondition) | `legend-pure-core/legend-pure-m3-core/target/classes/**/*.pelt` plus `org/finos/legend/pure/module/platform.{pmf,psr,pxr,pbr,pfn}`. Produced by `mvn -T 4 install -DskipTests -pl legend-pure-core/legend-pure-m3-core -am` (15–30 min first time). ~1,900 files, 3.5 MB. |
| PELT byte-level reference | `docs/architecture/pelt-serialization.md` at the repository root — on branch `docs/pelt-format`, submitted upstream as finos/legend-pure PR #1360. **Not on `legend-pure-rust` yet**: before Step 3, `git cherry-pick -n c989b50953b`, keep `docs/architecture/pelt-serialization.md`, and redo by hand its three link edits (`docs/README.md`, `docs/architecture/compiler-pipeline.md`, `docs/reference/maven-plugins-reference.md`; the first two already differ between the branches). Do **not** merge the branch — it tracks upstream `master` and differs from `legend-pure-rust` by ~1,500 files. |
| Background analysis (why PELT, what it contains, how it maps) | `legend-pure-rust/docs/pelt-analysis.md` (moved from `docs/deferred/` in Step 0). Sections 6.3–6.8 are the source of Steps 2–7 here. |
| legend-engine | A sibling checkout (`upstream/master` is the FINOS main line — see Appendix C for which branch is the target; it matters from Step 5, when the engine suite class is added). Its `*-pure` modules emit PELT into their jars (e.g. 414 `.pelt` in `functions-standard`); its PCT suite classes are the `Test_*_PCT.java` classes (42 on `upstream/master`); the engine compiler is `legend-engine-language-pure-compiler` (`toPureGraph`); the Pure function libraries are `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-{standard,relation,json,planExecution,…}`. |
| PCT in the platform | Profile `meta::pure::test::pct::PCT` in `legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/essential/tests/pct_core.pure`, which since Step 2 also declares the native `evalRust` and the `Rust_JNI` adapter `testAdapterForRustExecution`; 463 `<<PCT.test>>` functions in the platform sources (327 essential, 136 grammar); Java report providers `Essential_Functions_PCTReportProvider`, `Grammar_Functions_PCTReportProvider`, `Core_{Compiled,Interpreted}_PCTReportProvider`, and `Core_RustJni_PCTReportProvider` in the fork's `legend-pure-runtime-rust-evaluator`; shared helpers in `m3/pct/shared/PCTTools.java`. |
| What to extract, and what to leave | Appendix A (extraction map). Read it before Step 0 and before fishing anything later. |
| Step 0 mechanics | Appendix B: archive commands, workspace skeleton, gates. |
| Decisions still owned by the author | Appendix C. Do not guess these; ask. |

**Where we are, and what comes next (2026-10-11).** Steps 0–2 are done,
the Java-lambda lowering, natives rounds 1 and 2, Step 2b and the bridge's
cost with them, and both Java engines run the PCT corpus through
`Rust_JNI` (interpreted 320 / 327 and 132 / 136, compiled 321 / 327 and
132 / 136; what stays red is listed under Step 2, "Natives round 2 and the
bridge's cost", each line a representation choice rather than a missing
native). Two measurement tools exist: `BenchmarkPctExpressions` (every PCT
test under four modes, with the Rust side's own split per call) and the
apples-to-apples pair (`examples/expressions.rs`, `examples/hot_path.rs`,
`BenchmarkNativeExpressions`). They say the evaluator alone is within 3x
of the compiled engine's generated method on the twelve expressions (Step
2b's table) and that a PCT test through `Rust_JNI` takes 1.15x the
interpreted engine's own adapter (median 94 µs against 82 µs), the
lowering cached per lambda definition. The order from here, each phase
with its gate:

1. **Step 3 and Step 4** (next) — read PELT, the `Directory` trait, the
   static graph: the per-call lowering disappears for everything static.
   Before it, two decisions the author owns: the Java 17 floor for the
   `legend-pure-runtime-rust-evaluator` module and whether the bridge
   moves from JNI to the Foreign Function & Memory API (assessed
   2026-10-11; the spike and its numbers are on the author's branch).
2. **Steps 5–7** as written.

Done phases and their closing paragraphs: Step 2b (its own section, the
final table), natives round 2 and the bridge's cost (Step 2, after
"Natives round 1"). The bridge's menu left unpicked: columnar
description, string interning, a thread-local `JNIEnv`, subtree prefetch,
element resolution cache; the gate did not need them, and Step 3 changes
what crosses.

Before any push: the review's decision list (files from the archived
direction, the uncommitted `pom.xml` release change, the archive tag and
branch on `origin`, the merge from `master` and the manifest rebase after
it) is the author's.

**Definition of done for the MVP.** The `Rust_JNI` PCT adapter (built in
Step 2, red) reports pass rates for the platform and at least
one `legend-engine-pure-code-functions-*` module from PELT-loaded code with a
published ranked list of missing natives (Step 5's gate), and a static Pure
function runs against an engine-compiled model with Java-equal results
(Step 6's gate). Step 7 is the first increment after the MVP.

---

## 1. Thesis

Legend already has a complete Pure **compiler** in Java, and legend-engine
already has its own compiler that turns user models (protocol JSON) into a Pure
graph. What a Rust effort can add first, with the least risk, is an
**evaluator**: a component that executes Pure functions.

Three facts make an evaluator-first approach possible:

1. **An evaluator can be proven on its own.** Its inputs are a handful of
   node shapes — literals, function expressions, variables, lambdas — plus
   class and function declarations. Tests can build those shapes directly and
   check the result, with no source text and no loader. The only thing the
   evaluator must know up front is the M3 metamodel, which is small and fixed.
2. **The Java build already writes the compiled graph to disk in an open,
   element-granular binary format called PELT.** Every `*-pure` module in
   legend-pure and legend-engine emits it. It contains function bodies, not just
   signatures, so once the evaluator exists, real programs can be executed from
   it without any `.pure` source and without re-compiling. PELT, and every Java
   Pure object, share one shape: a classifier plus a bag of named property
   values. An evaluator that understands that shape can consume graphs built by
   the Java compiler, by legend-engine's compiler, or by anything else.
3. **PCT (Platform Compatibility Testing) is an existing, executable
   definition of "behaves like Java".** Pure functions tagged `<<PCT.function>>`
   and their `<<PCT.test>>` tests run on both Java engines on every build. A
   third engine that runs the same tests gets a per-function parity score for
   free.

So the MVP is: **stand the evaluator up over the bootstrap alone; connect it
to Java and register it with PCT while it still fails every test; then read
PELT and watch the pass rate climb.** No Rust parser or compiler is needed for any step below.
Natives (functions declared `native` in Pure and implemented in Java) must be
re-implemented in Rust; the target is 100 %, and PCT turns that into a ranked,
incremental backlog rather than a precondition.

---

## 2. The landscape this plugs into

```mermaid
flowchart LR
    subgraph Build time (Java, exists today)
        SRC[.pure sources\nplatform + engine repos] --> JC[Java Pure compiler]
        JC --> PELT[(PELT files in jars)]
        JC --> BYTECODE[generated Java\ncompiled engine]
    end
    subgraph Request time (Java, exists today)
        PJ[protocol JSON\nuser models + queries] --> EC[legend-engine compiler\ntoPureGraph]
        EC --> UG[user graph:\nJava CoreInstance objects]
        PELT --> MP[MetadataPelt\nlazy platform graph]
        MP --> UG
    end
    subgraph Proposed (Rust)
        M3[m3.pure\nbootstrap] --> RE[Rust evaluator]
        TB[hand-built nodes\ntests] --> RE
        PELT --> RE
        UG -. JNI node bridge .-> RE
        RE -. JNI values / proxies .-> JAVA[Java callers]
        PCT[PCT suites] --> RE
    end
```

| Technology | What it is | How the evaluator uses it |
|---|---|---|
| **M3 bootstrap** | `m3.pure`: the metamodel's own classes (`Class`, `Property`, `Function`, `ValueSpecification`, …) in an instance-graph syntax, plus the primitive types. | The only Pure input the evaluator has before Step 3. Gives `type()`, `instanceOf`, property typing and dispatch something to stand on. |
| **PELT** | Per-element binary files (`<path>.pelt`) plus per-module metadata (`.pmf` manifest, `.psr` sources, `.pxr` external refs, `.pbr` back references, `.pfn` function names), produced by the `compile-pure` Maven goal into `target/classes` and therefore into every jar. Byte-level reference: `docs/architecture/pelt-serialization.md` in the legend-pure repository (on branch `docs/pelt-format`, cherry-picked before Step 3 — see §0). | The source of all *static* code: platform functions, engine core functions, DSL metamodels. Read-only input, from Step 3. |
| **Java Pure compiler** | `legend-pure` M3 compiler. | Stays the compiler for all repository code. Not replaced. |
| **legend-engine compiler** | `toPureGraph`: protocol JSON → Java `CoreInstance` graph (handcrafted `Root_..._Impl` objects). | Stays the compiler for user models, mappings, queries. Its output is consumed live through JNI. |
| **Java engines** | compiled (generated bytecode) and interpreted (tree-walker over `CoreInstance`s). | Remain the reference implementations; PCT compares against them. |
| **PCT** | `<<PCT.function>>` / `<<PCT.test>>` functions, run per engine through *adapters*. | A second platform adapter, `Rust_JNI` (`testAdapterForRustExecution`, body `$f->evalRust()`), runs every test lambda in the Rust evaluator under the interpreted engine's own discovery, assertion and reporting, and gives parity and native-coverage numbers. Later the Rust surveyor runs the corpus under cargo and reports as `Rust_Native`. |
| **JNI** | The Java ↔ native boundary. | `CoreInstance` is the only currency both ways: `PureRustEvaluator.start(function, args)`, the node bridge that lets Rust read Java objects lazily, handles for Rust objects seen from Java, typed failures. |

---

## 3. Vocabulary

- **Element.** A packageable Pure element: class, enumeration, profile,
  association, function, measure, package. One PELT file per element.
- **Node.** Any instance in the graph: an element, a property, a type reference,
  an expression inside a function body. A node is `(classifier path, name?,
  source span?, property → values)`. Values are primitives, references to other
  nodes in the same element (by index), or references to nodes in other elements
  (by **reference id**).
- **Bootstrap.** The model the evaluator starts with: the primitive types,
  `Any`, `Nil`, and the M3 classes parsed from `m3.pure`. Fixed, small, built
  once per process.
- **Executable form (IR).** The shapes the evaluator actually consumes: an
  *expression tree* (`ValueSpec` carrying an `ExprKind`) and *element tables*
  (`Element::Class`, `Element::Function`, …). Section 5, Step 1 gives the
  M3 ↔ IR correspondence.
- **Building block.** One IR node built directly in Rust — by a test, or
  later by a lowering — rather than parsed from text.
- **Reference id.** A string identity for a node across elements: the element's
  path for an element, otherwise the element path plus a navigation path, e.g.
  `meta::pure::metamodel::type::Class.properties['name']` or
  `my::f__String_1_.expressionSequence[0].parametersValues[1]`. Stable across
  builds.
- **Static vs dynamic.** *Static* is everything compiled at build time and
  shipped as PELT. *Dynamic* is what legend-engine builds per request from user
  input: classes, mappings, queries, lambdas, as live Java objects.
- **Lowering.** Turning the generic node shape into the executable form. For
  declarations this is building type and property tables; for function bodies
  this is building an expression tree.
- **Directory.** The index over a node source: path → node, classifier →
  its instances, simple name → overloads, reference id → node, element →
  module. It is what Java's `MetadataPelt` is (`MetadataIndex` +
  `ElementLoader` + `ReferenceIdResolver`) and the one place where a PELT
  store and a live Java graph genuinely differ. Nodes are read through it;
  the executable form is *not* behind it (Step 4).
- **Native.** A Pure function declared without a body. Each needs a Rust
  implementation.
- **Chunk.** One slice of `PureModel` (`ModelChunk`): chunk 0 is the
  bootstrap, each later chunk is one source of elements (a test's builders,
  the bridge, later one PELT module).
- **Surveyor.** The interpreted engine's PCT discovery-and-run harness
  (`PCTTools` and the `*_PCT` suite base classes): it finds every
  `<<PCT.test>>`, runs each through the adapter, applies the manifest and
  prints `<adapter> <scope>: <passes> / <total>`.
- **Fishing.** Copying one existing implementation (typically one native, or
  one module) from the archive branch into the workspace because a test or a
  PCT failure needs it now. The unit of fishing is the smallest thing that
  compiles.

---

## 4. The MVP in one picture

```mermaid
flowchart TD
    subgraph Inputs
        BOOT[Bootstrap\nm3.pure + primitives]
        TB[Hand-built nodes\ntests, Step 1]
        STORE[PELT store\ndirectory · jar list · classloader]
        JOBJ[Java objects\nengine-built graph]
    end
    subgraph metamodel crate
        TAB[Element tables\nclasses · functions · packages]
        IR[Expression tree\nValueSpec / ExprKind]
    end
    subgraph evaluator crate
        EVAL[Evaluator\nvalues · heap · variables · lambdas · dispatch]
        NAT[Native registry\nfished one at a time]
    end
    subgraph Added in Step 2
        JNI[JNI boundary\nstart() · CoreInstance both ways · handles · typed failures]
        PCTA[PCT adapter Rust_JNI\nevalRust on the interpreted engine · red first]
    end
    subgraph Added in Steps 3–5
        DIR[Element directory + lowering\nPELT nodes → tables + trees]
    end
    BOOT --> TAB
    TB --> TAB
    TB --> IR
    STORE --> DIR --> TAB
    JOBJ --> DIR
    DIR --> IR
    TAB --> EVAL
    IR --> EVAL
    NAT --> EVAL
    EVAL <--> JNI
    PCTA --> JNI
```

Design rules that hold for every step:

- **Two core crates, plus what a step names.** `metamodel` (element tables,
  expression tree, bootstrap) and `evaluator` (values, heap, variables,
  dispatch, native registry). Later steps add a crate only when the step says
  so: `jni` (Step 2) and `pelt` (Step 3) are the named additions.
- **Bootstrap only.** The evaluator's model starts as chunk 0 — the bootstrap —
  and nothing else. Every further element arrives either from a test's
  builders (Step 1) or from a lowering (Steps 4 onward).
- **Natives on demand.** The registry starts empty. A native is fished when a
  test or a PCT failure names it; the registry's contents are always
  explainable by a test.
- **Red before green.** The PCT adapter exists (Step 2) before any
  loader exists, with failures typed by kind. Its pass rate is the progress
  metric; a step that does not move it needs a reason.
- **Lazy by element, eager per call.** The directory (manifests) is read eagerly; an element's
  nodes are read and lowered the first time something touches it. Platform plus
  engine is tens of thousands of elements; a request touches hundreds. *Touch*
  means a `start` call: each call lowers the transitive closure of what its
  lambda references before it runs, and the model does not grow while it runs
  (decided 2026-10-11, Step 4 "The directory").
- **One node interface.** Everything downstream of "give me this node's
  classifier and properties" is identical whether the node came from PELT bytes
  or from a live Java object. This is what lets the engine's compiler output be
  consumed without a Rust compiler.
- **One directory, two sources.** Above the node interface sits one
  `Directory` trait (path → node, classifier → instances, name → overloads,
  reference id → node) with a JNI implementation and a PELT implementation.
  Below it, the executable form (`PureModel` and its chunks) is read directly
  by the evaluator and is never behind a trait: the bootstrap and test-built
  elements are born lowered and need no directory at all.
- **Identity by path.** Elements are identified by their Pure path everywhere.
  A Java object that is a packageable element with a path the evaluator already
  knows *is* that element, not a copy of it.
- **Java stays the oracle.** Any divergence is a bug on the Rust side until
  proven otherwise; PCT is the arbiter.

---

## 5. Steps

Each step has a goal, what gets built, how it integrates, what it proves, and
the gate that says it is done. Steps are ordered so that every one ships a
usable artifact.

### Step 0 — Extract the evaluator and its metamodel into a clean workspace — done 2026-10-09

**Goal.** A workspace that contains exactly the evaluator, the metamodel it
needs, and this document. Anyone opening it finds one direction and no code
arguing for another. Everything else is recoverable from the archive.

**Decision (Appendix C item 5, 2026-10-09: replace).** The live branch's
`legend-pure-rust/` directory is *replaced* by the new two-crate workspace.
The previous workspace — parser, compiler, LSP, DSL crates, binary snapshot
format, CLI — lives on the archive tag and branch only. Same path, so the
repository-level `CLAUDE.md` and tooling pointers stay valid.

**Build.**

1. *Freeze.* Tag the current head and create the archive branch (commands in
   Appendix B). Nothing is lost.
2. *Empty the directory.* Remove everything under `legend-pure-rust/` except
   `docs/rust-evaluator-mvp.md` and `docs/deferred/pelt_vs_purem.md` (renamed
   to `docs/pelt-analysis.md`).
3. *Skeleton.* Root `Cargo.toml` with two members, `crates/metamodel` and
   `crates/evaluator` (Appendix B has the skeleton). Dependencies allowed:
   `smol_str`, `thiserror`, `rust_decimal`, `im-rc`, `jiff`, `tracing`. No
   `serde`, no `postcard`, no `linkme`, no `toml`.
4. *Copy the metamodel crate* per Appendix A, table 1, applying the listed
   edits: strip `Serialize`/`Deserialize` derives, copy the two leaf types from
   the old AST crate instead of depending on it, drop the compiler-only fields
   and methods from the model type, leave the least-upper-bound functions
   behind (Appendix A, table 3: they come with the meta natives that need
   them). Keep the `include_str!`
   of `m3.pure`; that *is* the permitted bootstrap, and the relative path is
   unchanged because the crate sits at the same depth as before.
5. *Copy the evaluator crate* per Appendix A, table 2: values, heap, variable
   context, dates, errors, hooks, the native registry (without its plugin
   slice), the evaluator itself with the listed arms stubbed as "unsupported",
   and the 14 in-crate unit tests that already run over a bootstrap-only model.
   Natives: none. The two seed tests that call `plus` and `times` fail until
   Step 1 fishes them; that is expected and is the first thing Step 1 does.
6. *Docs.* New `legend-pure-rust/CLAUDE.md` (build, test, the six rules, a
   pointer here), `README.md` (one paragraph), nothing else. No backlog, no
   architecture doc; this document is the plan. **Also rewrite the
   repository-level `CLAUDE.md`** (its "Repository Shape" and "Current State"
   sections describe the archived workspace: parser, compiler, 17 CLI
   subcommands, `BACKLOG.md`, `docs/deferred/`); every session loads it first,
   so it is the one file that must not contradict this plan.
7. *Build plumbing outside the workspace.* `.github/workflows/rust.yml` runs
   `cargo fmt --check`, the `cargo lint-lib` / `cargo lint` aliases from the
   archived `.cargo/config.toml`, and `cargo llvm-cov nextest --workspace
   --exclude legend-pure-parser-stress --exclude legend-pure-parser-jni
   --fail-under-lines 85`, then doctests, `cargo bench`,
   `./scripts/check-copyright.sh` and a `cargo doc` warning gate. Replace with
   `cargo fmt --check`, `cargo clippy --workspace --all-targets -- -D warnings`,
   `cargo test --workspace --doc`, `cargo test --workspace`,
   `./scripts/check-copyright.sh`, `RUSTDOCFLAGS=-Dwarnings cargo doc
   --workspace --no-deps`; drop the lint aliases, the excludes (those crates no
   longer exist), the bench, and the coverage gate until Step 1 has a test base
   worth measuring. Keep `scripts/check-copyright.sh` and `rustfmt.toml` in
   the new workspace (Appendix B layout). The Maven reactor lists two Java
   modules for the old JNI crate (`legend-pure-runtime/pom.xml`:
   `legend-pure-runtime-rust-evaluator-bindings-ap`,
   `legend-pure-runtime-rust-evaluator`); the bindings annotation processor
   loads `legend-pure-rust/target/debug/libpure_rust_jni*` at `javac` time
   (`PureBindingsProcessor`, a compile error if absent, so `-DskipTests` does
   not help), and Step 0 deletes that library.
   Remove both `<module>` lines in Step 0 (the directories stay; Step 2
   re-added `legend-pure-runtime-rust-evaluator` only — the bindings
   processor depends on the archived `java-codegen` crate and returns, if at
   all, with typed proxies) so `mvn install` on the fork stays green.
8. *Agent memory.* Project memory entries that describe the old workspace's
   decisions are deleted or rewritten so future sessions start from this
   document. (`.claude/agent-memory/` and `.claude/agents/` are tracked on
   the branch; not part of the gate.)

**Gate.** `cargo build --workspace` succeeds and `cargo tree -e normal
--depth 1` lists only the six dependencies above; the Appendix B grep returns
nothing (the `m3.pure` include path matches none of its patterns);
`cargo test --workspace` compiles and passes except the two seed tests that
call `plus` and `times`, which `rust.yml` skips by name until Step 1 fishes
them (done in Step 1 milestone 1, where the skips are removed); both `CLAUDE.md` files
(repository root and `legend-pure-rust/`) name `docs/rust-evaluator-mvp.md` as
the single source of truth and describe nothing from the archive as current;
`rust.yml` runs green on the new workspace; `mvn -T 4 install -DskipTests`
still succeeds at the repository root.

**Start here.** Appendix B, then Appendix A table 1 top to bottom, then table
2. Rough size: metamodel ≈ 6 k lines copied, evaluator ≈ 8 k lines copied;
one to two days including the edits.

### Step 1 — The evaluator over the bootstrap alone — done 2026-10-09

**Goal.** Prove the evaluator executes every expression shape and consults
every element shape it knows, with inputs built in Rust and nothing loaded.

**M3 ↔ executable form.** Tests speak the evaluator's language; this table is
the dictionary from the M3 names a Pure reader knows to the shapes the tests
build. It is also the specification the Step 5 PELT body lowering must
satisfy.

| M3 node (what Java and PELT hold) | Executable form (what tests build) |
|---|---|
| `InstanceValue` with one primitive value | `ExprKind::{Integer,Float,Decimal,String,Boolean,Date}Literal` |
| `InstanceValue` with several values | `ExprKind::Collection { elements }` |
| `InstanceValue` whose value is an enum value | `ExprKind::EnumValue` |
| `InstanceValue` whose value is a packageable element / type | `ExprKind::PackageableElementRef`, `ExprKind::TypeReference`, `ExprKind::MultiplicityReference` |
| `SimpleFunctionExpression` (resolved `func`, `parametersValues`) | `ExprKind::FunctionCall(FunctionCallData { function: Some(id) \| None, function_name, arguments })` |
| `SimpleFunctionExpression` whose `func` is a `Property` | `ExprKind::PropertyCall` (receiver is `arguments[0]`) |
| `SimpleFunctionExpression` whose `func` is a `QualifiedProperty` | `ExprKind::QualifiedPropertyCall` (receiver is `arguments[0]`, then the arguments) |
| `VariableExpression` | `ExprKind::Variable { name }` |
| `LambdaFunction` (`InstanceValue` wrapping it) | `ExprKind::Lambda { parameters, body, … }` |
| `let` / `if` / `new` / `copy` desugared by the compiler | `FunctionCall` to the natives `letFunction_…`, `if_…`, `new_…`, `copy_…` |
| `Class` element (properties, generalizations, qualified properties) | `Element::Class` in a chunk ≥ 1, registered in its package |
| `ConcreteFunctionDefinition` (`expressionSequence`) | `Element::Function { body, is_native: false }` |
| `NativeFunction` | `Element::Function { is_native: true }` plus a registry entry under the mangled name |
| `Enumeration`, `Profile`, `Association`, `PrimitiveType`, `Package` | the matching `Element::*` variant |
| `genericType` / `multiplicity` on any `ValueSpecification` | `ValueSpec.type_info: Option<Box<ResolvedType>>` (may be `None`; the evaluator does not require it) |

**Build.**
- A public `builders` module in the evaluator crate, promoted from the test
  helpers that already exist (`lit_*`, `make_expr`, `test_model`): expression
  builders for every row above (`lit_int`, `collection`, `var`, `call`,
  `call_resolved`, `prop`, `qp`, `lambda`, `enum_value`, `type_ref`, …) and a
  `ModelBuilder` that starts from the bootstrap and allocates `Element::Class`
  / `Element::Function` / `Element::Enumeration` into chunk 1 (a `ModelChunk`
  pushed onto `model.chunks`), registers them in their package, and rebuilds
  the derived indexes (specializations, association properties) before the
  evaluator is created. Every `ValueSpec` carries a mandatory `SourceInfo`;
  the builders synthesize one (`<test>`, as the old `synth_src()` did).
- **One hidden contract the builders must honour.** Dispatch reads the
  *mangled* function name (`plus_Number_MANY__Number_1_`) from the element
  node's `name` field; the old compiler wrote it there. `ModelBuilder::function`
  therefore allocates the element, computes the mangled name with the
  metamodel crate's `compute_function_fqn` (it needs an allocated element),
  and writes it back into the node's `name`. A function
  built with a plain name never dispatches. The Step 5 lowering hits the same
  wall; write it down once, here.
- Natives, fished in this order because the tests need them: `plus` and
  `times` (the two seed tests), then `letFunction`, `if`, `new` (five
  registry keys: one each for `let` and `if`, three signatures of `new`),
  `copy` as the builders reach `let`,
  `if`, instantiation and copy; then `minus`, `eq` / `equal` (in
  `comparison.rs`), `and` / `or` / `not`. Each fished native arrives with its
  own unit test. Nothing else.
- Error paths: variable not found, function not found (the mangled key is in
  the message), property not found, multiplicity violation.

**Integrates with.** Nothing external. This step is pure Rust.

**Proves.** The evaluator is a usable library on its own. Gate: one test per
`ExprKind` arm that is not stubbed (the stubbed arms — relation literals,
column specs, path literals — have one test each asserting the "unsupported"
error); one test per `Element` kind the evaluator reads; one test per dispatch
path (native by exact mangled name, user function body, qualified property,
lambda application, property access; the prefix fallback that Step 1 tested
was removed on 2026-10-10); `grep -rE 'purem|pelt|legend_pure_parser|toml' crates/`
still returns nothing (the `pelt` token leaves the gate when Step 3 adds
`crates/pelt`).

**Start here.** Move the 14 unit tests out of `eval.rs` into
`crates/evaluator/tests/building_blocks.rs`, make them pass by fishing `plus`
and `times`, then add rows from the table one at a time. Rough size: builders ≈ 500 lines;
each fished native is a copy plus one test.

### Step 2 — Values across JNI, and a PCT run that starts red — done 2026-10-09

**Goal.** Connect the evaluator to Java and run the platform PCT corpus
through it *before it can run anything real*. Every test is red on day one;
from here on, each later step is measured by how many turn green.

**What was built** (commits `11c63c83b65`, `51f546f3e96`, `37d1c0e52d1`,
`d20e9a8539a`, `d8f1b69ab4f`).

- **`crates/jni`** (cdylib `libpure_rust_jni`; dependencies `jni`,
  `slotmap`): the third crate. The only value currency across the boundary is
  the Java `CoreInstance`, in both directions. Crossing in, a node is described
  once (kind, classifier path, primitive payload, property names) and becomes
  a Rust value: primitives, dates and decimals decode; a packageable element
  whose path the Rust model knows *is* that element (`Value::Element`); enum
  values of known enumerations become enum values; every other node is a
  lazily read Java-backed heap object (`JavaNode: TypedObject`, properties
  fetched on first touch through `RustNodeBridge.propertyValues`). Crossing
  out, primitives go through the `ModelRepository` factories, elements and
  enum values by path, Java-backed nodes as their original object, other Rust
  objects as `RustCoreInstance` proxies over a handle table. Failures reach
  Java as `PureRustEvaluationException` with a kind (`FUNCTION_NOT_FOUND`,
  `UNSUPPORTED`, `EXECUTION_ERROR`, `ASSERTION_FAILED`, `CONSTRAINT_VIOLATION`;
  `NOT_IMPLEMENTED` added 2026-10-11) that also prefixes the message, because manifests match `expectedError` by
  substring; `ASSERTION_FAILED` carries a `PureAssertFailException` cause so
  the harness's fail / error split applies.
- **Java module `legend-pure-runtime/legend-pure-runtime-rust-evaluator`**
  (back in the reactor; `…-bindings-ap` stays out): `PureRustEvaluator
  implements FunctionExecution` (`init(runtime, message)`, `start(function,
  args)`), `RustNodeBridge`, `RustCoreInstance`, and `TestRustEvaluatorBoundary`
  (nine tests at the end of the step — primitives, collections, element
  identity, Rust objects, `FunctionNotFound` with the user path, assertion
  cause, lambda `Unsupported`, ten thousand round trips leave no live handle,
  a closed evaluator refuses calls — sixteen today).
- **The PCT seam is the adapter, not a platform.** Every PCT test takes its
  adapter function as argument 0 and calls `$f->eval(|…)`; the adapter decides
  which engine runs the lambda. The platform (`pct_core.pure`) gained a native
  `meta::pure::functions::lang::evalRust<V|m>(f:Function<{->V[m]}>[1]):V[m]`
  and a second adapter `<<PCT.adapter>> {PCT.adapterName='Rust_JNI'}
  meta::pure::test::pct::testAdapterForRustExecution` whose body is
  `$f->evalRust()`; `PlatformCodeRepositoryProvider.rustJniAdapter` names it
  for reports. The interpreted engine implements `evalRust` through `EvalRust`
  (registered by `RustJniInterpretedExtension` via `META-INF/services`; the
  evaluator is created on the first call, because an extension whose
  constructor throws is dropped silently). The compiled engine generates Java
  for the whole platform at build time, so its `evalRust` compiles to a call
  into an `EvalRustExecutor` hook found through `META-INF/services` at run
  time (`compiled.CompiledEvalRust` in the module, since 2026-10-11; before
  that a stub that threw). Discovery, assertion and reporting stay the Java
  engines' own.
- **Suites, manifests, reports** in the Java module:
  `Test_RustJni_{Essential,Grammar}Functions_PCT` are the interpreted suites
  with the `Rust_JNI` adapter and the manifests
  `src/test/resources/platform/pure/essential/pct_essential_interpreted_rust_jni.json`
  and `…/grammar/functions/pct_grammar_interpreted_rust_jni.json` (inside a
  repository tree, because the surveyor reads them through code storage and
  the report through the classpath). The manifests are generated, never
  hand-written: `RebaseRustJniManifests` runs the surveyor under the committed
  manifest and derives the new one from its report (FAIL/ERROR → first line of
  the message; PASS as expected failure → carried forward; an excluded test
  that now passes → dropped; a second run reproduces the files byte for
  byte). `PCTReportGenerator` writes
  `ADAPTER_{essential,grammar}_interpreted_Rust_JNI.json` at
  `process-test-classes` from the manifests alone, published by
  `Core_RustJni_PCTReportProvider`, so the report step needs no Rust library.
  Without the cdylib the module's tests are skipped and the build stays green.

**State at the end of the step.** `Rust_JNI essential: 0 / 327`,
`Rust_JNI grammar: 0 / 136`. 451 tests record
`Unsupported: evaluating a Java LambdaFunction through the bridge`: the lambda
reaches Rust as a Java node classified `LambdaFunction`, and nothing lowers
it yet. The 12 tests that assert on an error message record
`Execution error message mismatch.`, because their assertion compares that
same `Unsupported` message with the expected error text. The line each suite
prints (`Rust_JNI <scope>: <passes> / <total>`) is the progress metric of the
whole effort from here on.

**Next (decided by the author, 2026-10-09): lower Java lambdas through the
bridge — done 2026-10-10** (commits `80e4235c470`, `eae28b3fb89`,
`287c424bca4`, `8d584acd57e`). A test turns green when the lambda's body
becomes an expression tree the evaluator can run: the body lowering of
Step 5 over the declarative lowering of Step 4, both applied to Java nodes
reached through the bridge, before any PELT is read. What landed:

- `crates/evaluator/src/lower/` reads nodes through the heap's `TypedObject`
  view (`JavaNode` today, PELT nodes later: the one node interface) and
  lowers `InstanceValue`, `VariableExpression`, `SimpleFunctionExpression`,
  `LambdaFunction` and `KeyExpression` into specs, and `NativeFunction`,
  `ConcreteFunctionDefinition` (bodies, transitively), `Class` (properties,
  qualified properties, supertypes; constraints not yet) and `Enumeration`
  into elements. Elements are collected in a `ModelExtension` with ids
  assigned up front (mutual recursion resolves to pending ids) and appended
  to a bridge chunk — whatever index follows the model's chunks — in one
  step. `JniContext::start` runs Phase A (lower, model read-only) and
  Phase B (if elements were added: drop the evaluator, apply, rebuild),
  then applies the closure. Captured variables come through a new bridge
  method, `RustNodeBridge.capturedValue`, from the `LambdaWithContext` the
  interpreted engine hands over.
- Every `func` becomes a `Function` element whose node name is Java's
  mangled name verbatim; dispatch reads it as the registry key, so a fished
  native hits exactly and an unfished one reports
  `FunctionNotFound: <mangled>` at its call site. The simple-name prefix
  fallback is gone from dispatch: it could route `plus_String` to
  arithmetic `plus`. `^Class(k = v)` reaches the platform's KeyExpression
  `new` through its Java key via a new `ExprKind::KeyExpression`; nothing
  is retargeted by name. Types are advisory (dropped when unlowerable)
  except while an element is half-built, where they fail loudly.
- Source spans cross both ways: `NodeDescription` carries the Java
  `SourceInformation` (anchor line/column as the span start), every lowered
  spec and element carries it, and a failure raised in Rust is anchored at
  its innermost call site and rethrown by the interpreted engine with that
  anchor (`PureRustEvaluator.start` attaches a source-bearing cause for
  every kind).
- Two defects found on the way, fixed at the source: the bootstrap never
  registered the primitive types under the root package (a bare `Integer`
  path never resolved); the Java graph has cycles and `JavaNode` caches
  hold strong handles, so `BridgeShared::end_execution` drops every node
  cache when a start ends. Findings left for natives: `plus`/`times` wrap
  on i64 overflow where Java promotes; `copy` with key expressions must
  learn the `KeyExpression` decode.

**State after the lowering.** `Rust_JNI essential: 2 / 327`,
`Rust_JNI grammar: 59 / 136`. No `Unsupported` line about lambdas,
functions, captures, classes, enumerations or properties remains; the
manifests are the natives backlog, ranked by tests unblocked: `toString` 17,
`adjust` 17, `round` 13, `format` 13, `take` 11, `match` 11, `fold` 11,
`drop` 11, `lessThan` 11, `zip` 9, `dateDiff` 9, …; plus 12 message
mismatches (the `*Error` tests need the exact Java message), 2 primitive
types declared outside the bootstrap, 2 integer overflows. Natives are
fished one per commit from that ranking, as Step 5 says; the bridge cost
list above is profiled once the run spends its time in lowered bodies.

**Natives round 1 (2026-10-11).** Every key in that ranking had an
implementation on the archive, so the backlog was fished in one round, one
native module at a time on disjoint files (`collection` 37 keys, `string`
21, `math`/`arithmetic`/`comparison` 37, `datetime` 23, the eight `eval`
arities in `lang`, `meta` 6; 158 keys in all after 426ed3a99fa dropped two
keys the platform does not declare native), each copying the smallest
compiling unit and its tests; the per-commit PCT numbers rule gave way to
one measurement per merged round. Two things changed around
the natives: `PureRuntimeError::NotImplemented(key)` with the
`native::NotYet` native (a key registered before its body is fished, JNI
kind `NOT_IMPLEMENTED`, manifest prefix `NotImplemented:`), used for
`genericType` and `deactivate`, which need the compiler's least upper
bound and the `ValueSpecification` heap shapes; and the Pure cause attached
to a Rust failure now carries Pure's bare info for the Pure-level kinds
(what `assertError` compares) and the kind prefix only for the Rust-side
diagnostics. State: `Rust_JNI essential: 303 / 327`, `Rust_JNI grammar:
126 / 136`. The 34 lines left: `Map` values across the boundary (6, the
interpreted engine's `MapCoreInstance`), a Java null key on `zip` results
compared by `EqualityUtilities` (6), `copy` with key expressions (3),
`adjust` beyond jiff's date range (5), `NotImplemented` (3), `getAll` (1),
structural equality over Java-backed instances (2), user-defined primitive
types (2), 64-bit overflow (3), Decimal/Float rendering (2), mixed-precision
date compare (1).

**Natives round 2 (2026-10-11).** The residue was worked on two disjoint
file sets. The evaluator side: `copy` over key expressions
(`decode_key_expressions` feeds the same `copy_with_triples` as the flat
form), `genericType` out of `NotYet` (a `GenericType` object with its
`rawType`), `getAll` fished from the archive (metamodel rows by classifier,
then the objects reachable through the variable context), `parseDecimal`
rounding half away from zero and keeping its scale, and three lowering gaps:
a property's stereotypes (each resolved to its `Profile` element, lowered
from the Java node as its stereotype and tag names, so `<<equality.Key>>`
reaches `equality_key_properties`), a `PrimitiveType` declared outside the
bootstrap (its one generalization as supertype), and the boundary test for
an unlowerable node moved to a unit of a measure. The JNI side (the bridge
branch): exception discipline and local frames, one mutable entry per call
with every unsafe block justified, read failures keeping their kind and
anchor, Rust objects with a name and reaching the compiled engine's
generated code as generated classes, a Rust `Map` crossing out as the host
engine's own map, `start` classifying by element id, and the bridge
forgetting the nodes Rust released. Recorded as out of scope for the MVP, each a
representation choice rather than a missing native: `adjust` beyond jiff's
date range (5), 64-bit integer overflow where Java promotes to `BigInteger`
(3), `Float` as `f64` where Java's is a `BigDecimal` (`testBigFloatAbs`),
and `deactivate` (needs the `ValueSpecification` heap shapes and the
compiler's least upper bound).

**Natives round 2 and the bridge's cost (2026-10-11).** Both halves of
round 2 merged, and the bridge measured and cut. Before: interpreted
`Rust_JNI essential: 303 / 327`, `Rust_JNI grammar: 126 / 136`, compiled
300 / 327 and 125 / 136. After: `Rust_JNI essential: 320 / 327`,
`Rust_JNI grammar: 132 / 136`, compiled 321 / 327 and 132 / 136. What
moved, by kind: `Map` out as the host engine's own map (`MapCoreInstance`,
`PureMap` in a carrier; 6 per engine); the `zip` results, whose cause was
not equality but `RustCoreInstance.getName()` returning null where every
Java instance has a name (`assertEquals` builds its message eagerly through
`id()`; 6 interpreted); a Rust object reaching the compiled engine as an
instance of its generated `_Impl` class, copied when the call ends (the five
compiled-only holes and the six compiled `zip` tests); `copy` over key
expressions (3), `genericType` (2), `getAll` (1), stereotypes and user
primitive types in the lowering (4), `parseDecimal` scale (1). Along the
way the review's correctness items: JNI exception discipline and local
frames, one mutable entry per call (`Session`) with every `unsafe` block
justified and `undocumented_unsafe_blocks` denied, read failures keeping
their kind and anchor (typed side-channel), `start` classifying by element
id and lowering a `ConcreteFunctionDefinition` and its arguments, the
bridge forgetting the node ids Rust released. What stays red, one line
each: `adjust` beyond jiff's range (5 per engine) and `%10999` in
`testDateCompare` (`PureDate` is a jiff civil date-time, years ±9999, and
its API, i16 years and `inner_datetime()`, is consumed by the literal
evaluation and `format`; Java's years are `int`); 64-bit overflow (3:
`Value::Integer` is `i64`, Java promotes to `BigInteger`; `testLargeMinus`
fails already at the crossing of its literal); `testBigFloatAbs`
(interpreted only: `Float` is `f64`, the interpreted engine's a
`BigDecimal`); `deactivate` (the `ValueSpecification` heap shapes). The
three compiled `fold` tests went with the merge: `copy` kept a synthetic
`sourceInformation` slot on the copy, an archive-era workaround no test
needs, which the compiled conversion refused as an undeclared property.
The bridge, `BenchmarkPctExpressions`
with the release cdylib, interpreted host: before, median 658 µs per PCT
test against 82 µs for `interpreted/In-Memory` (geomean 8.65x), of which
the lowering 246 µs and the run under 1 µs, first run 839 µs; after,
median 94 µs (1.15x, geomean 1.10x), lowering 1 µs, run under 1 µs (431
tests read nothing during the run), first run 391 µs. Three changes did
it, each measured: the lowered closure cached per lambda definition (the
interpreted engine's `LambdaWithContext` is new per evaluation; the key is
the wrapped lambda's synthetic id), with captured lambdas and lambda
arguments through the same cache (lowering 246 → 1 µs); the description
read through field ids resolved once instead of a class and field lookup
by name per field (one `Integer` crossing 7.7 → 1.9 µs); property keys
read when asked instead of with every description (an expression node
5.4 → 2.4 µs; the compiled host built them from the class per node). The
compiled host has no closure cache (its lambda values expose no stable
definition), so it still lowers per call: 709 → 203 µs median (geomean
9.32x → 2.45x of the interpreted engine's adapter). A cached call now costs
about 5 µs of crossing (the lambda's description, its captures, the end
of the execution, materialising the result, Java's wrapping); the rest of
the menu below (columnar description, interning, the thread-local
`JNIEnv`, subtree prefetch, element resolution) was not needed for the
gate and waits for a profile that asks for it. An incremental recompile
of the Java runtime leaves both the model and the closure cache stale
until the evaluator is replaced (`EvalRust` replaces it when the
engine's runtime object changes).

**Later: `Rust_Native`.** Once the surveyor's own functions execute in Rust,
the same corpus runs under cargo through the Rust surveyor, with its own
manifests, and reports as adapter `Rust_Native`; `Rust_JNI` stays the measure
of the bridge.

**Recipe.**

```bash
cd legend-pure-rust && cargo build --workspace            # builds the cdylib
cd .. && JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator clean verify
# after a change that moves tests: regenerate, review the diff, verify again
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn test -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator \
  -Dtest=RebaseRustJniManifests -DfailIfNoTests=false
```

**Deferred: the cost of the bridge (profile first, decided 2026-10-10).**
Property fetches on a `JavaNode` are already lazy; what is eager is the
per-value `NodeDescription`, read through about fifteen JNI field calls plus
a string copy per text field, and the `getKeys()` array built for every
node. Numbers cross as decimal text both ways, `RustNodeBridge.nodes` pins
every described node until `close`, and every lazy read re-attaches the
thread. Lowering now walks whole bodies (2026-10-10); the measuring tool is
the module's `BenchmarkPctExpressions` (2026-10-11: every PCT test timed
under each engine with its own adapter and with `Rust_JNI`, first run apart
from the warm median, and the Rust side's own split of every call:
lowering, run, JNI reads during the run). First measurement, release
cdylib, median per PCT test: the whole test 77 µs on the interpreted
engine and 614 µs through `Rust_JNI`, of which the evaluator's run of the
lowered closure is under 1 µs (p90 2 µs, max 15 µs), the lowering 229 µs,
and the rest, about 300 µs, the crossing itself (describe, materialise,
cache clearing, the JNI entry). So the evaluator is not the cost; the
per-call re-lowering and the crossing are. The apples-to-apples pair
(`crates/evaluator/examples/expressions.rs` and the module's
`BenchmarkNativeExpressions`, the same twelve expressions built on each
side, no JNI, 2026-10-11) puts the evaluator itself between the two Java
engines: 10 to 20 times faster than the interpreted engine's entry, 2 to
13 times slower than the compiled engine's generated method, the gap
widest where lambdas are applied per element (`filter` over a hundred
integers: 37 µs against 2.8 µs, about 350 ns per lambda application).
The lambda application path is therefore the evaluator's own hot spot.
The list below is the bridge's menu, roughly in order of payoff per
line:

- *Columnar description*: `describe` / `propertyValues` return one `long[]`
  (kind, id, classifier id, date components) plus a `String[]` of the
  payloads that exist — two array reads per call instead of fifteen field
  reads per value.
- *Intern repeated strings*: classifier paths, property keys and element
  paths get a small integer on the Java side, sent once; Rust mirrors the
  table.
- *Drop the eager keys array*: fetch `property_names` on demand.
- *Reuse the live `JNIEnv`*: the evaluator runs inside the `nativeStart`
  frame; a thread-local env removes the attach per read.
- *Subtree prefetch for code, laziness for data*: lowering touches every
  node of a body, so `describeSubtree(id)` returning the whole body in one
  columnar payload (or a `byte[]` in the PELT element encoding, once Step 3
  can decode it) beats per-value crossing; instances of user classes
  (Steps 6–7) keep per-property laziness because a request touches few of
  their properties.
- *Cache element resolution*: `resolve_function_by_path` walks the model on
  every packageable-element crossing.

Not worth doing: deferring the classifier itself (dispatch asks for it at
once).

**JNI or the Foreign Function & Memory API (assessed 2026-10-11).** A
spike on JDK 17's incubator FFM (`jdk.incubator.foreign`, JEP 412; branch
`worktree-agent-a90035275085e3b67`, `legend-pure-rust/spike-ffm/`, kept
off the workspace) measured the bridge's call shapes against JNI, median
ns per operation on Temurin 17 aarch64: a `long` downcall 2.1 against 1.9;
a 40-byte string in and its length out 315 against 142 (36–45 with a
reused native segment); a `long` upcall from Rust 117 against 159; a
`long` plus short string upcall pair 554 against 394; binding a method
209 against a symbol lookup of 140 plus 2.6–3.1 µs per downcall handle
and 6 µs per upcall stub. Downcalls are equal, primitive upcalls are
slower, strings are the one gain, and the bridge's cost is the number of
crossings and the text encoding, which batching pays the same under
either mechanism. The incubator module must be enabled on every JVM that
loads the evaluator (`--add-modules jdk.incubator.foreign
--enable-native-access=ALL-UNNAMED`: surefire, the engine's launchers,
IDE runners), the 17 API is rewritten for the final one of JDK 22
(`Linker`, `Arena`, zero-length `MemorySegment` for pointers), no
exception may cross an upcall, and confined scopes are single-thread
while `start` runs on arbitrary threads. Decision: stay on JNI; revisit
when the JDK floor reaches 22. The string cost, if it matters, is reachable
within JNI (`GetStringUTFRegion` instead of `GetStringUTFChars`; not
measured). The module itself moved to release 17 the same day (profile
`rust-evaluator-jdk17` in `legend-pure-runtime/pom.xml`); Java 17 language
features in its sources wait on the root's checkstyle 8.25 pin, which
predates records, switch expressions and pattern `instanceof`.

**Deviations from the plan as first written** (recorded, as rule 6 asks).
The archived JNI crate's tagged-union marshalling was not fished:
`CoreInstance` crosses instead, so every Pure type is covered without a second
value model. No `PureTestBuilderRust`, no `rust-interpreted` platform, no
`target/pct-rust` report: the harness's adapter seam made all three redundant,
and the surveyor path is used after all, since discovery runs in the Java
engine, not in Rust. Lambdas across the bridge reported `Unsupported` (not
`FunctionNotFound(testPath)`) until the lowering of 2026-10-10.

### Step 2b — The evaluator's own cost (next, decided 2026-10-11)

**Why now.** The apples-to-apples pair (Step 2, "Deferred: the cost of
the bridge") put the evaluator between the two Java engines: 10 to 20
times faster than the interpreted engine, 2 to 13 times slower than the
compiled engine's generated method. The bridge is an accepted trade-off;
the evaluator is the product, and it must be as fast as a tree-walker can
be before any further step builds on it. This phase is the evaluator
alone: no JNI, no bridge, no natives backlog.

**Where the time goes.** Measured on the hot path in isolation
(`crates/evaluator/examples/hot_path.rs`, release build, median of 3,000
runs after 300 warm-up, 2026-10-11):

| Operation | Today | Same, with a structural clone and no custom drop |
|---|---|---|
| literal `1` through `eval` | 42 ns | 41 ns |
| `1 + 2` (one native, a two-element collection argument) | 667 ns | 209 ns |
| `range(0, 100, 1)->size()` | 2.6 µs | 0.8 µs |
| `range(…)->filter(x \| true)->size()`, per lambda application | 166 ns | 104 ns |
| `… ->filter(x \| $x->mod(2) == 0)`, the body on top | 186 ns | 100 ns |
| `… ->map(x \| $x * 2)`, the body on top | 335 ns | 165 ns |
| `Value::Integer` clone | 27 ns | 2 ns |
| `Value::Collection` of 100 clone | 2,361 ns | 16 ns |
| `push_scope` + one `set` + `pop_scope` | 44 ns | 45 ns |

The compiled Java engine runs the whole filter at about 28 ns per element.
Reading the code behind the numbers:

- `Value::clone` is an iterative deep clone that allocates two work
  vectors for every clone, an `Integer` included, and the custom `Drop`
  walks every element of a collection on every drop, which defeats the
  structural sharing `im_rc::Vector` provides. Every variable read,
  argument binding, numeric promotion and `filter` result pays both.
  Pure collections are flat and a unit instance nests one level, so a
  plain clone never recurses meaningfully; the iterative versions guard
  against a shape Pure cannot produce.
- An arithmetic native receives its operands as one `Number[*]`
  collection: `a + b` evaluates a `Collection` literal into an
  `im_rc::Vector` behind a `Box`, the native copies it into a `Vec`,
  `promote_pair` clones both operands again, and the fold allocates
  once more. Four allocations and six clones for one addition.
- A lambda application pushes a scope (a `Vec` for its undo entries),
  binds every capture and parameter into a `HashMap<SmolStr, Value>`
  keyed by name with SipHash, and reads each variable back through the
  same map. Names are resolved at run time on every application although
  the lowering and the builders know every binding statically.
- A resolved native call still hashes its mangled name to find the
  registry entry, on every call.
- `force_all` allocates a `Vec` per native call; `equal` and `eq`
  allocate a work stack and clone both operands even for two integers.
- `Result<Value, PureException>` is 144 bytes against 32 for `Value`,
  moved through every return of the recursive `eval`.

**Build, in this order; each item is one commit carrying the before and
after of `examples/expressions.rs` and `examples/hot_path.rs`.**

1. *Clone and drop.* A structural `Clone` (leaf variants by value,
   `Collection` by sharing the vector, `UnitInstance` one level deep) and
   no custom `Drop`. Keep one test that a collection of ten thousand
   elements clones in constant time and drops without growing the stack.
   Measured above: `1 + 2` 667 → 209 ns, the filter 37.8 → 21.3 µs.
2. *Operands without a collection.* The evaluator exposes the elements
   of a `Collection` literal to a native without materialising the
   vector (an iterator over the evaluated elements, or the natives read
   the literal's elements directly), and the arithmetic natives fold
   over it; `promote_pair` works on borrowed numbers and clones nothing.
   Expected: `1 + 2` under 100 ns, `$x * 2` per application under 60 ns.
3. *Variables as slots.* The lowering and the builders resolve every
   `$x` to a frame slot; a frame is a `Vec<Value>` and `VariableContext`
   a stack of frames with a flat undo log, no map and no hashing. `let`
   appends to the current frame; captures are copied into the frame on
   application; the natives that read the context by name
   (`letFunction`, `getAll`, the `KeyExpression` evaluation) go through
   the resolved slot. Expected: a lambda application under 40 ns.
   If the lowering change is too large for one commit, the interim step
   is a flat undo log and a cheap hasher written in the crate (no new
   dependency), worth roughly a third of the gain.
4. *Dispatch by element.* A resolved call caches its `&dyn
   NativeFunction` per `ElementId` (a vector indexed by chunk and local
   index) so no string is hashed; a call without an element (the
   builders) keeps the map.
5. *Arguments without a heap vector.* Fixed-arity helpers (`force1`,
   `force2`, `force3`) returning tuples replace `force_all` in every
   native of known arity.
6. *Equality fast path.* `equal`, `eq` and `values_equal` compare two
   primitives without a work stack, a set or a clone; only objects and
   collections take the general path.
7. *Boxed error.* `Result<Value, Box<PureException>>` (or a boxed
   payload inside `PureException`) through the evaluator, removing the
   crate-wide `result_large_err` allowance. Mechanical; measure before
   keeping.
8. *Collection representation.* Only if the profile still shows
   `im_rc::Vector` after 1–7: a small inline vector for collections of
   up to eight elements, or `im_rc::Vector::unit` for one. Measure
   first; the representation is shared with the heap and the bridge.

**Rules.** No new dependency. No semantic change: all Rust tests and all
four `Rust_JNI` numbers unchanged after every item. No name-based
special case anywhere. Items 1 and 2 before anything else: they are the
cheapest and the largest.

**Gate.** The twelve expressions of `expressions.pure` within three
times the compiled engine's generated method (from 2 to 13 times today);
`1 + 2` under 100 ns; a lambda application under 50 ns; the PCT corpus
through `Rust_JNI` unchanged in count. Record the two tables in this
section when the phase closes.

**Not in this phase.** Matching generated Java needs compilation of the
expression tree into closures or direct-threaded code, which is a
different evaluator, not a faster walk; it is listed under "Later,
outside the MVP". The bridge's own cost (the per-call re-lowering and the
crossing, Step 2 "Deferred") is a separate phase after this one.

**Progress (2026-10-11, items 1, 2, 3a, 4, 5, 6, 7 and the natives' own
cost landed, each with the four `Rust_JNI` numbers unchanged).** Two
corrections first. The clock on this machine ticks every 41.7 ns, so
every sub-microsecond reading in the table above and in the first commit
messages was quantised to that step; both harnesses now time batches of
32 calls, and the figures below are per call at that resolution. And the
compiled engine's generated method is faster than first read: 36 to 220
ns on the twelve, not 167 to 292. Where the twelve stand, release build,
median ns per call, Rust evaluator against the generated method:

| Expression | Before | Now | Compiled Java |
|---|---|---|---|
| `1 + 2` | 792 | 121 | 167 |
| `(3 * 4) + (10 - 2)` | 1,916 | 244 | 220 |
| `let x = 5; $x * $x` | 1,042 | 213 | 96 |
| `if(1 < 2, ...)` | 417 | 207 | 69 |
| `map` then `fold` over five | 9,542 | 2,230 | 587 |
| `range` of a hundred, `filter`, `size` | 36,875 | 14,736 | 2,346 |
| `sort` then `at` | 959 | 380 | 50 |
| `format` | 667 | 238 | 50 |
| `adjust` a date | 84 | 96 | 113 |
| `match` | 1,208 | 763 | 36 |
| `removeDuplicates` then `size` | 750 | 483 | 96 |
| `joinStrings` | 375 | 260 | 66 |

Arithmetic and dates are at or below the generated method; `let`, `if`,
`joinStrings` and the collection pipelines sit at 2 to 6x; `sort`,
`format` and `match` at 5 to 21x, where the cost is the native's own
work over a persistent vector plus the dispatch layers around it.

**Closed (2026-10-11).** Items 8 and 3b landed after the above. Item 8
was decided by measurement: against the persistent vector, a
reference-counted vector builds a two-element collection in 18 ns
instead of 68 and a hundred-element one in 176 instead of 865, iterates
a hundred in 27 instead of 146, indexes in 38 instead of 206, clones at
the same cost, and loses only on appending to a large shared collection
(489 against 194 at a hundred), which Pure copies anyway; `PVector` is
now the crate's own `Rc<Vec<T>>` with copy on write, `im-rc` stays for
the map type. Item 3b did not need slots: with batched timing a
variable read was 9 ns but binding one parameter 25 ns, so the context
became a stack of bindings scanned from the top (a scope is a position,
popping truncates), which costs 5 ns per binding and 7 ns per read over
the handful of bindings a lambda body sees; the registry hashes names
the same way. The last item was the closures themselves: creating one
copied the lambda's parameters and body into fresh Arcs and walked the
body for its free variables on every evaluation of the node (every
`if`, every `match` branch); the node now shares its body with its
closures and carries its free variables, found once when it is built.

Where the twelve stand at the close, release build, median ns per call,
batched timing (about 10 % run-to-run noise), against the compiled
engine's generated method:

| Expression | Start of phase | Close | Compiled Java | Ratio |
|---|---|---|---|---|
| `1 + 2` | 792 | 66 | 167 | 0.4x |
| `(3 * 4) + (10 - 2)` | 1,916 | 166 | 220 | 0.8x |
| `let x = 5; $x * $x` | 1,042 | 136 | 96 | 1.4x |
| `if(1 < 2, ...)` | 417 | 122 | 69 | 1.8x |
| `map` then `fold` over five | 9,542 | 1,235 | 587 | 2.1x |
| `range` of a hundred, `filter`, `size` | 36,875 | 12,149 | 2,346 | 5.2x |
| `sort` then `at` | 959 | 311 | 50 | 6.2x |
| `format` | 667 | 204 | 50 | 4.1x |
| `adjust` a date | 84 | 96 | 113 | 0.8x |
| `match` | 1,208 | 334 | 36 | 9.3x |
| `removeDuplicates` then `size` | 750 | 479 | 96 | 5.0x |
| `joinStrings` | 375 | 250 | 66 | 3.8x |

Six of the twelve meet the 3x gate; the collection and string natives
sit at 4 to 6x and `match` at 9x. The per-element cost of a pipeline is
now the evaluator's layers around a native call (about 30 ns for a
dispatch, 45 for `equal`), a lambda application (16 ns with one
parameter) and the result collection, no longer any single allocation:
what is left is the shape of a tree-walker, and the natives' own
algorithms (`match` walks every branch's type against the value,
`removeDuplicates` compares pairwise) where Java's generated code has
the same algorithm with the JIT behind it. The four `Rust_JNI` numbers
were unchanged after every item.

### Step 3 — Read PELT

**Goal.** Decode every PELT file type into in-memory structures, with no
knowledge of Pure semantics.

**Build.**
- A reader for the element file: 16-byte envelope (signature, serializer
  version, reference-id version), raw-DEFLATE body, string table, then nodes
  in breadth-first order with their property bags.
- Readers for the module files — manifest, sources, external references,
  functions by name, the back-reference index, and the **per-element**
  back-reference files (one per referenced element; 616 for the platform) —
  each with its own signature and version.
- The reference-id parser (`path`, `.prop`, `.prop[i]`, `.prop['key']`,
  `.prop[key='v']`).
- A store abstraction over *where bytes come from*: a directory
  (`target/classes`), an ordered list of jars (a classpath), or a Java
  classloader reached over JNI. File naming lives only here.
- A small binary (`pelt-inspect`) that dumps one element and lists a module's
  elements from its manifest.

**Integrates with.** The jars Maven already builds. Nothing in the Java build
changes; the only Java code is the classloader callback below. The readers
and the reference-id parser are what the PELT `Directory` of Step 4 is built
on; this crate itself knows no Pure semantics and no `metamodel` type.

**Proves.** The format is fully understood. Gate: golden tests decoding every
file in the platform's `target/classes` without error and round-tripping the
string table; `inspect` output reviewed against the Java `print` of the same
element.

**Start here.** New leaf crate `crates/pelt` — the fourth crate, and the only
one this step adds (dependencies: `flate2`, a zip reader only; it does not
depend on `metamodel` or `evaluator`). The classloader-backed store uses the
Java module from Step 2: a `ClassLoader.getResource` callback exposed to
native code (~100 lines Java). Fixtures: copy a dozen representative
files from the platform `target/classes` (a class, an enumeration, a profile,
an association, a native function, a test function with a lambda, `Root.pelt`,
`Integer.pelt`, the five `platform.*` module files) into
`crates/pelt/tests/fixtures/`. The Java `print` of an element comes from a
throwaway JUnit test in `legend-pure-m3-core` using `PureCompilerLoader` on
`target/classes`. Rough size: ~2 k lines.

### Step 4 — A static graph in memory

**Goal.** Load modules by name, resolve dependencies, and answer "what is `X`"
for every element without executing anything.

**Build.**
- Element directory from manifests: path → classifier, source span, module;
  follow manifest dependencies transitively (as `MetadataPelt` does).
- Declarative lowering: classes (properties, supertypes, type parameters),
  enumerations, profiles, associations, primitive types, function *signatures*.
  Produces the metamodel crate's element tables, which the evaluator already
  uses for `instanceOf`, property typing and dispatch. **Identity rule:** an
  element the bootstrap already defines (`String`, `Any`, `Class`, …) is the
  bootstrap's element; the PELT copy is skipped, not duplicated.
- Overload index from `.pfn` (simple name → functions); subtype and
  association-inverse indexes from `.pbr`, so no element has to be loaded to
  answer "who extends `X`".
- Packages: real ones from their PELT files, virtual ones implied by element
  paths.
- A generic "property-bag element" for any classifier the lowering does not
  know (DSL metamodels, engine extensions), so *every* element is loadable.
- The `Directory` trait and its PELT implementation (below); the lowering
  takes a `&dyn Directory` and resolves references through it.

**Integrates with.** Module dependencies and repository boundaries exactly as
Java sees them; a store configuration pointing at a directory, jar list or
Maven coordinate.

**Proves.** Type-level questions match Java. Gate: for every element in the
platform manifest, classifier, supertypes, property names and types and
function signatures agree with what the Java `CoreInstance` API reports for the
same element.

**Already there (2026-10-10).** The declarative lowering of classes
(properties, qualified properties, supertypes, type parameters),
enumerations and function signatures exists in
`crates/evaluator/src/lower/elements.rs`, over the heap's `TypedObject`
node view, with `ModelExtension` as the growth path into a chunk. This
step adds the PELT node source behind that view, the directory and the
indexes; constraints, associations, profiles and the property-bag element
are still to lower.

**The directory (decided 2026-10-11).** Java's `Metadata` hands out
`CoreInstance`s, which are at once the node you read and the thing you run.
Rust keeps those apart, and only the first half still lacks an abstraction:

- *Node.* `TypedObject` already is the node interface (`JavaNode` today, a
  PELT-inflated node in this step). Nothing changes there.
- *Model.* `PureModel` and its chunks are the sink, never a backend. The
  evaluator reads them directly (dozens of sites, most in `eval.rs`) and the
  PELT analysis rejected M3 instances or raw bytes as the in-memory model.
  Do not put them behind a trait.
- *Directory.* The missing piece, one trait in the evaluator crate
  (`lower/directory.rs`), with these operations and nothing more until a
  step asks: `node(path) -> Option<Node>` (an element by user path),
  `resolve(reference_id) -> Option<Node>` (a node inside an element; the
  Step 3 parser reads the id, the directory walks it, the lowering never
  sees the string form), `overloads(simple_name) -> Vec<Node>`,
  `instances(classifier) -> Vec<Node>`, `module_of(path) -> ModuleId`.
  Two implementations: the PELT one in this step, over the Step 3 readers
  (manifests, `.pfn`, `.pbr`, the element loader); the JNI one in Step 6, in
  the `jni` crate, as bulk calls into the engine's indexes. The bridge today
  needs none of it because Java hands the lowering every referenced object
  already; it grows the JNI directory when Step 6 asks "who are the
  instances of `X`".

What this changes in the existing lowering, and it is small: `Lowering`
takes a `&dyn Directory`, and the three places that follow a reference
(`func`, `rawType`, `general`) accept a reference id in place of an object
in hand. Everything after that point is the same code.

*Routing.* The directory answers for definitions and instances alike, but
they go to different places: definitions the evaluator reasons about by
type (classes, enumerations, functions, associations, profiles) are lowered
into `PureModel`; instances (a Mapping, a DSL node, a user object) stay heap
`TypedObject`s and are never lowered. Step 6's definitions overlay and
Step 7's property-bag element are this rule, not exceptions to it.

*Chunks.* One chunk per manifest module; the directory is what knows module
→ chunk. The bridge's single `bridge_chunk` stays as the chunk of the
unnamed "live Java" module.

*Eager per call, lazy later.* The directory is consulted while a `start`
call lowers the transitive closure of its lambda (Phase A); it is not
consulted while the lambda runs. `Evaluator<'model>` borrows the whole
`PureModel`, so growing a chunk mid-execution is impossible with the
current shapes; it would need an append-only chunk store with stable
addresses or interior mutability, and that plumbing is not justified until
profiling or a run-time query forces it. The queries that would force it are
known: `getAll` on a static class, `Class.all()`, `match` by a classifier
not yet in the model, `extractEnumValue` by string path. Until then they are
answered from what the closure walk brought in, and a miss is a typed
`Unsupported`, not a silent empty result.

**Start here.** Build the oracle first: a JUnit test in `legend-pure-m3-core`
that loads `target/classes` through `PureCompilerLoader` and dumps, per
element, `(path, classifier, supertypes, properties[(name, type, multiplicity)],
function signature)` as JSON into `crates/pelt/tests/fixtures/platform-oracle.json`.
The Rust gate is then a diff against that file. The lowering lives in the
evaluator crate (module `lower/`, which already exists), targets the
metamodel crate's types, and
invents nothing new except the property-bag element: repurpose
`Element::DSLInstance`, replacing its opaque byte payload with a classifier
plus a property map. Rough size: 2–3 k lines.

**Keeping fixtures honest.** Step 3 fixtures and the oracle both derive from
`target/classes`, which only exists after a Maven build. Step 3 adds
`legend-pure-rust/scripts/regen-pelt-fixtures.sh` (Maven module build, copy
the chosen files into `crates/pelt/tests/fixtures/`); this step extends it to
run the oracle test. Commit the fixtures and the script, and record the
legend-pure commit they were generated from (`git rev-parse HEAD`, one line
`source-commit: <sha>`) in a `FIXTURES.md` next to
them. The oracle JUnit is committed on the fork branch under
`legend-pure-core/legend-pure-m3-core/src/test/java/.../pelt/` (a test, not
product code); whether it goes upstream is an open question (Appendix C).

### Step 5 — Execute function bodies, measure with PCT

**Goal.** Run Pure functions whose bodies come from PELT.

**Build.**
- Body lowering: M3 expression nodes (`SimpleFunctionExpression`,
  `InstanceValue`, `VariableExpression`, `LambdaFunction`, `KeyExpression`,
  type references) into the expression tree, row by row from the Step 1 table.
  Calls are already resolved to their target function by the Java compiler;
  types and multiplicities are already inferred on every expression. The
  lowering reads them, it does not recompute them. Function nodes get the
  mangled name (Step 1's hidden contract).
- Lazy per element: a function body is lowered the first time a `start`
  call's closure walk reaches it (Step 4 "The directory": eager per call,
  nothing lowered while the lambda runs).
- Measurement is the Step 2 runner: its report already splits failures by
  kind, so *missing native* (`FunctionNotFound` on a native declaration) and
  *lowering gap* (`Unsupported`) are visible per module without new tooling.
  Optionally, fish the archive's Rust-side PCT runner (Appendix A, table 3)
  for local iteration without a JVM; the published numbers are the Step 2 runner's.
- Natives: fished per histogram row, highest-leverage first. The `assert*` and
  `executePCTTest` support natives come first because every PCT test goes
  through them.

**Integrates with.** The PCT corpus as it exists in the platform `.pure`
sources, compiled by Java and read from PELT. No test is rewritten.

**Proves.** Semantic parity, function by function. Gate: the Step 2 runner's
pass rate on the platform suite and on one engine functions module, with the
ranked list of missing natives; the expected-failures manifest shrinks with
every commit and the rate only goes up.

**How PCT works, in one paragraph.** A PCT function is a platform function
carrying `<<PCT.function>>`; its tests are functions carrying `<<PCT.test>>`
(463 in the platform today: 327 essential, 136 grammar) that call the function and assert on the result.
An *adapter* is a Pure function that decides how a test's expression is
executed on a given engine; each engine registers its adapter and a report
provider. A test passes when evaluating it raises no assertion. Per-engine
exclusions (known gaps) are declared in a manifest so the pass rate is honest
about what is skipped.

**Already there (2026-10-10, natives 2026-10-11).** The body lowering exists in
`crates/evaluator/src/lower/body.rs` and runs over Java nodes through the
bridge (Step 2 "Next"); the natives backlog it exposed was fished from the
archive in one round (Step 2, "Natives round 1": 158 keys, 303 / 327 and
126 / 136), so the histogram this step fishes from is now the residue listed
there. What this step adds is the PELT-fed run and the engine suite class.

**Start here.** The body lowering is the new code; natives are fished.
First target: `testAdapterForInMemoryExecution` loaded from PELT, lambda
application and `eval`, then the first PCT test in the platform's essential
functions that needs only `assert`, `eq` and a literal. Once the platform
number moves, add the legend-engine suite class (`Test_RustJni_*_PCT`
for one functions module, modelled on its `Test_Interpreted_*_PCT` sibling)
with a dependency on the fork's Java module and the cdylib on
`java.library.path` (Appendix C items 1 and 3). Rough size of the body lowering:
3–5 k lines.

**How natives become incremental.** The histogram is the backlog. Each native
is weighted by how many PCT functions it unblocks, so the order of work is
data-driven, and "100 % natives" is reached one measured step at a time
rather than demanded up front.

### Step 6 — Consume legend-engine's graph live

**Goal.** Execute static Pure code against user models compiled by
legend-engine, with no Rust compiler.

**Build.**
- A JNI-backed node source: a small Java helper that, given any `CoreInstance`
  (handcrafted `Root_..._Impl`, PELT-lazy, or interpreted), returns its
  classifier, keys and values in one call, primitives inline and other objects
  as handles. On the Rust side the same node interface as PELT, so the same
  lowering applies. *(Done 2026-10-10 as `RustNodeBridge` / `JavaNode`;
  proven on compiled objects 2026-10-11: the compiled engine runs the same
  corpus through `RustEngineHost` at 300 / 327 and 125 / 136, the difference
  being Rust-built objects handed back to generated code.)*
- The JNI `Directory` (Step 4): `node(path)` through `_Package.getByUserPath`,
  `instances(classifier)` and `overloads(name)` as one bulk call each into the
  engine's own indexes, `resolve(reference_id)` through the Java
  `ReferenceIdResolver`. Whole-model queries never walk the graph node by
  node across the boundary.
- **Identity unification.** When a crossing object is a packageable element
  whose path the evaluator already knows (`String`, `Any`, a platform class, a
  profile), substitute the known element. Without this, `instanceOf`, type
  equality and dispatch break. This is the one thing required before the first
  call.
- **Lazy definitions overlay.** When a value's class is unknown (a user class
  such as `my::Person`), fetch the class node, run the Step 4 declarative
  lowering, and register it in an overlay layer on top of the static graph.
  Instances never need lowering; only definitions the evaluator reasons about by
  type do.
- Lowering of engine-built lambdas (`LambdaFunction_Impl` trees) through the
  same body lowering, cached per Java object.
- Read-only in the first increment: no writes back into Java objects.

**Integrates with.** `PureModel` in legend-engine: pass its objects straight
into `evaluate`. Whole-model queries (`Class.all()`) use a bulk call into the
engine's indexes rather than walking.

**Proves.** The engine's compiler output is directly executable by Rust.
Gate: a worked example such as a class-analytics function from
`legend-engine-xts-analytics/legend-engine-xts-analytics-class/legend-engine-xt-analytics-class-pure`
run on an engine-compiled model with
results equal to the Java compiled engine's; the Step 2 runner extended to
engine modules whose tests need a compiled user model.

**Start here.** The Java node helper lives next to
`PureRustEvaluator`; it needs only the `CoreInstance` interface
(`legend-pure-m4`), so it works for handcrafted, PELT-lazy and interpreted
objects alike. First target: a function whose inputs are classes and
properties only, before anything that touches mappings. Rough size: ~2 k
lines Rust (node bridge, identity unification, overlay), ~300 lines Java
(node helper).

### Step 7 — DSL instances and property bags end to end

**Goal.** Mappings, runtimes, connections, services and other DSL instances
from the engine flow through the evaluator into the static Pure code that
consumes them (routing, analytics, in-memory execution).

**Build.**
- Generic hydration of property-bag elements into evaluator instances, with
  the schema read from the DSL's metamodel class (itself static, from PELT):
  property names, types and multiplicities validate the bag. The archive's
  DSL-instance hydration module is the starting point (Appendix A, table 3).
- Typed fast paths only where profiling says so.

**Integrates with.** Every DSL legend-engine already compiles, without the
evaluator knowing any DSL grammar.

**Proves.** The evaluator is DSL-agnostic. Gate: a mapping-analytics or
in-memory relation function run on an engine-compiled mapping, parity against
Java. Rough size: ~1 k lines.

### Later, outside the MVP

- **Writing PELT from Rust.** Needed only when something other than the Java
  compiler produces graphs. Requires producing complete M3 node shapes and the
  same reference ids Java would generate; the gate is "Rust-written platform
  PELT decodes identically to Java-written platform PELT".
- **Compiling the expression tree.** Closure compilation or direct-threaded
  code over the lowered tree, the route to the compiled Java engine's speed
  once Step 2b has taken the tree-walker as far as it goes.
- **A Rust parser and compiler.** Independent of everything above; when it
  exists, it produces the same node shape and plugs into the same lowering.
  The archive branch holds a complete one; whether it is revived or rewritten
  is a decision for that day, not this document.

---

## 6. Integration matrix

| Existing technology | Step | Direction | Mechanism |
|---|---|---|---|
| `m3.pure` (M3 bootstrap) | 0 onward | in | embedded at compile time; chunk 0 of every model |
| Maven-built jars with PELT | 3–4 | in | PELT store over directory / jars / classloader |
| Java Pure compiler | all | upstream | unchanged; its PELT output is the contract |
| PCT corpus and adapters | 2 onward | test | `Rust_JNI` adapter: `evalRust` on the interpreted engine, suites and generated manifests in the fork's `legend-pure-runtime-rust-evaluator` from Step 2 (red), engine suite class from Step 5; failures typed by kind; `Rust_Native` through the Rust surveyor later |
| JNI | 2, 3, 6 | both | `start()`, `CoreInstance` both ways, node bridge, handles, typed failures |
| legend-engine compiler (`toPureGraph`) | 6, 7 | in | live `CoreInstance` objects consumed through the node bridge; identity unification by path |
| `MetadataPelt` (class in `legend-pure-runtime-java-engine-compiled`, used by legend-engine's `PureModel`) | 6 | in | its lazy objects are also `CoreInstance`s; unified with the evaluator's own PELT-loaded elements by path |
| Java natives (`core_functions_*`) | 1 onward | re-implement | Rust native registry, fished per test (Step 1) then per the runner's `FunctionNotFound` ranking (Step 5 onward) |

---

## 7. What this deliberately does not do

- It does not compile Pure. Repository code is compiled by Java; user code by
  legend-engine. A Rust compiler is a separate, later project that benefits from
  everything here but is not required by it.
- It does not carry the previous Rust workspace forward. Parser, compiler,
  LSP, DAP, MCP, DSL crates, CLI and the binary snapshot format stay on the
  archive branch. Anything needed later is fished, not merged.
- It does not replace either Java engine. They remain the oracle.
- It does not write to engine-built Java objects in the MVP.
- It does not attempt plan generation or store execution first. Those are Pure
  code with the largest native and extension surface; they sit at the end of the
  PCT-ranked native backlog, not its start.

---

## 8. Risks and how each step contains them

| Risk | Where it bites | Containment |
|---|---|---|
| Extraction leaves a hidden coupling behind (a field the evaluator reads that the old compiler filled, such as the mangled name on function nodes or the metatype lookup) | Steps 0–1 | Step 1's per-arm, per-element, per-dispatch-path tests over a bootstrap-only model surface every such contract before any loader exists |
| The executable form is lossy relative to M3 (missing node kinds, optional types) | Step 5 | property-bag fallback means nothing is unloadable; PCT histogram shows exactly which shapes are missing |
| Java-specific graph details (resolved import stubs, milestoning rewrites, inference results) | Step 5 | they are *data* the lowering reads, not behaviour to emulate; the Java compiler is upstream |
| Reference ids reach inside elements | Steps 4, 6 | forward references from bodies target elements plus `name`/`id` keys; sub-element ids only matter for back references, which are optional |
| JNI latency per node | Step 6 | one transition per object, cached; whole-model work uses bulk calls or a PELT snapshot of the model |
| Engine-built elements lack source spans | Step 6 | the node bridge tolerates missing spans; PELT snapshots would not, which is why live objects go through the bridge |
| A red-first run is noise | Step 2 | failures cross JNI with a kind that prefixes the message (`Unsupported: …`); the generated manifests record that first line per test, so "not lowered yet" never masquerades as a divergence, and they keep CI green while the number climbs |
| Native coverage | Step 1 onward | measured per test, then per module by the PCT runner; target 100 %, reached incrementally |

---

## 9. Glossary

| Term | Meaning |
|---|---|
| M3 | The Pure metamodel: `Class`, `Function`, `Property`, `ValueSpecification`, … Every graph node is an instance of an M3 class. |
| Bootstrap | Chunk 0 of every model: primitive types, `Any`, `Nil`, and the M3 classes parsed from `m3.pure`. |
| `CoreInstance` | The Java interface every Pure graph node implements: classifier, name, source information, property access by name. |
| PELT | Pure Element serialization: one binary file per element plus per-module metadata, written by `compile-pure`. |
| Reference id | String path identifying a node across elements, stable across builds. |
| PCT | Platform Compatibility Testing: Pure functions and tests run on every engine; results must agree. |
| Adapter | The per-engine runner PCT uses to execute a test function. |
| Native | A Pure function declared without a body, implemented in the host language. |
| Mangled name | A function's identity string: simple name plus parameter and return types and multiplicities, e.g. `plus_Number_MANY__Number_1_`. The native registry and dispatch are keyed by it. |
| Lowering | Converting generic graph nodes into evaluator-specific structures. |
| Directory | The index over a node source (path, classifier, simple name, reference id → nodes); one trait, a PELT and a JNI implementation. Java: `MetadataPelt`. |
| Overlay | A per-request layer of definitions on top of the shared static graph. |
| Chunk | One slice of `PureModel`: chunk 0 the bootstrap, each later chunk one source of elements. |
| Surveyor | The interpreted engine's PCT discovery-and-run harness; prints `<adapter> <scope>: <passes> / <total>`. |
| Fishing | Copying one existing implementation from the archive into the workspace because a test or PCT failure needs it. |

---

## Appendix A — Extraction map (archive → new workspace)

All source paths are on the archive tag `archive/pre-evaluator-extract-2026-10`
(the old workspace root is `legend-pure-rust/`, crate names carried a
`legend-pure-parser-` / `legend-pure-` prefix). Copy, then apply the edit in
the third column. Nothing in these tables is rewritten from scratch. Two
edits apply to every copied file: rewrite every `legend_pure_parser_pure::`,
`legend_pure_parser_ast::` and `legend_pure_runtime::` path (about 80
occurrences in code and doc comments) to `legend_pure_metamodel::` /
`legend_pure_evaluator::`; and scrub doc comments that mention the snapshot
format, serialisation or serde (about 40 lines plus some 60 derive and
`#[serde(...)]` attribute lines), so the grep gates stay honest. Drop the
`serde`-related cargo features the old crates enabled on `smol_str` and
`rust_decimal`.

**Table 1 — `crates/metamodel` (package `legend-pure-metamodel`).** Depends on
`smol_str`, `thiserror`, `rust_decimal`. About 6 k lines.

| Archive path | Destination | Edit on copy |
|---|---|---|
| `crates/ast/src/source_info.rs` (`SourceInfo` only) and `crates/ast/src/annotation.rs` (`SpannedString` only) | `src/source_info.rs` | Copy the two leaf types; do not depend on the AST crate. Keep `SourceInfo::new`. |
| `crates/pure/src/ids.rs` | `src/ids.rs` | Strip `Serialize`/`Deserialize`. Keep `ElementId`, `PackageId`, `RelationId`. |
| `crates/pure/src/arena.rs` | `src/arena.rs` | Strip serde (`use`, derive, `#[serde(transparent)]`); fix the doc-test import path. |
| `crates/pure/src/types.rs` | `src/types.rs` | Strip serde. Keep **everything**: `TypeExpr`, `Multiplicity`, `Parameter`, `ResolvedType`, `ValueSpec`, `FunctionCallData`, `ExprKind` (all 20 arms, including the relation / column / path ones the evaluator stubs), `ConstValue`, `DateValue`, `PrimitiveType`, `RelationColumn*`, `PathStepLowered`, `ColSpecLiteralKind`. These are the language. |
| `crates/pure/src/annotations.rs` | `src/annotations.rs` | Strip serde. |
| `crates/pure/src/nodes/{class,function,enumeration,profile,association,measure,unit,mod}.rs` | `src/nodes/` | Strip serde. `profile.rs` uses `SpannedString` from `source_info.rs`. |
| `crates/pure/src/model.rs` | `src/model.rs` | Strip serde. Keep `ElementNode`, `Element` (all variants, `DSLInstance` included — Step 4 turns it into the property-bag element by replacing its opaque byte payload with a property map), `DSLInstance`, `Package`, `ModelChunk`, `DerivedIndexes`, and on `PureModel` the fields `global_packages`, `root_package`, `chunks`, `package_elements`, `derived`. **Drop** `extension_arenas`, `repo_visibility`, `repo_patterns`, `compile_scope` and the methods taking the AST `Package` type (`resolve_package`, `resolve_in_package`, `resolve_functions_by_name_in_package`). Keep the methods the evaluator calls (`get_node`, `get_element`, `try_get_element`, `element_name`, `get_package`, `resolve_fqn_str`, `resolve_by_path`, `resolve_function_by_path`, `association_properties`) and the ones the builders and the Step 4 lowering need (`new`, `get_or_create_package`, `register_element`, `rebuild_derived_indexes`, `specializations`, `derived`, `resolve_by_fqn`, `resolve_functions_by_simple_name_globally`). |
| `crates/pure/src/bootstrap.rs` | `src/bootstrap.rs` | As is (fix the doc-test import path, as in `arena.rs`). Keep the `include_str!` of `m3.pure` (relative path unchanged: the crate is at the same depth). Keep `create_bootstrap_chunk`, `metatype_of`, `register_m3_packages`, the `*_ID` constants. Add a `PureModel::bootstrapped()` convenience: `new()` + push the bootstrap chunk + `register_m3_packages` (one step more than the old `test_model()` in `eval.rs`, which skipped the package registration). |
| `crates/pure/src/m3_parser.rs` | `src/m3_parser.rs` | As is. |
| `crates/pure/src/fqn.rs` | `src/fqn.rs` | Keep `compute_function_fqn` (the mangling the builders and the Step 5 lowering need); drop its test module, which compiles source through the archived pipeline. The Step 1 builders become its tests. |
| `crates/pure/src/milestoning/mod.rs` | — | **Not in Step 0.** Fished in Step 5 together with the evaluator's milestoning dispatch if a PCT row needs it. |
| `crates/pure/src/{error,pipeline,infer,inference/,lower/,validate,extension,visibility,access,locate,island_lower,purem/}` | — | **Never.** Compiler, validator, binary snapshot format. `error.rs` is `CompilationError`; the evaluator's error type is in table 2. |

**Table 2 — `crates/evaluator` (package `legend-pure-evaluator`).** Depends on
`metamodel`, `smol_str`, `thiserror`, `rust_decimal`, `im-rc`, `jiff`,
`tracing`. About 8 k lines.

| Archive path | Destination | Edit on copy |
|---|---|---|
| `crates/runtime/src/error.rs` | `src/error.rs` | As is (`PureException`, `PureRuntimeError`). Add one variant, `Unsupported(String)` (first `&'static str`; widened on 2026-10-10 so the lowering can name the classifier), for the stubbed arms. |
| `crates/runtime/src/value.rs` | `src/value.rs` | As is. `FunctionValue::Path` stays as a type; nothing produces it until path literals are fished. |
| `crates/runtime/src/heap.rs` | `src/heap.rs` | As is (`HeapEntry::{Dynamic,Typed}`, `TypedObject`, `RuntimeHeap`). |
| `crates/runtime/src/context.rs`, `scope_guard.rs` | `src/context.rs`, `src/scope_guard.rs` | As is. |
| `crates/runtime/src/date.rs` | `src/date.rs` | As is. |
| `crates/runtime/src/m3_paths.rs` | `src/m3_paths.rs` | As is (classifier FQN constants the heap uses). |
| `crates/runtime/src/model_utils.rs` | `src/model_utils.rs` | As is (element path helpers; `heap.rs` calls them). |
| `crates/runtime/src/hooks.rs` | `src/hooks.rs` | Keep; the evaluator is generic over it. Drop the one hook method that takes a display tree (it is the only use of `display.rs`). |
| `crates/runtime/src/native.rs` | `src/native.rs` | Keep `Evaluated`, `EvalContextTrait`, `NativeFunction`, `NativeRegistry` (`new`, `register`, `get`, `get_or_err`; `find_by_prefix` was copied and then removed with the prefix fallback on 2026-10-10), `expect_args`, `expect_min_args`, `force_all`, `force_thunk`, and the `lit_*` / `MockCtx` test helpers (they become the seed of the `builders` module). **Drop** `RuntimeExtension`, `RUNTIME_EXTENSIONS`, `with_extensions`, `discovered`, `merge_extensions`, every `pub mod` of a native file, the `extensions()` / `config_for()` trait methods and `MockCtx`'s impls of them. `NativeRegistry::standard()` registers nothing until Step 1 fishes natives. |
| `crates/runtime/src/eval.rs` | `src/eval.rs` | Keep `Evaluator`, `EvalContext`, `eval`, `eval_body`, `call`, `call_user_function*`, `eval_lambda_value`, `apply_callable`, `FoundQp`, `find_qp_with_generalization`, the free helpers at the bottom (`collect_free_variables`, `multiplicity_constant_bounds`). **Drop** the constructors `new_default`, `new_default_with_extensions*`, `new_default_with_dsl_populators` (keep `new`, `with_hooks`, `builder`), the fields `extensions` and `extension_configs` with `set_extension_configs`, `config_for`, `extensions` and the matching `EvalContext` trait impls, the `toml` import, and the display snapshot in `dispatch_snapshot_pause` (keep the function name; it is a hook call site). **Stub** with `Unsupported`: the `RelationLiteral`, `ColSpecArrayLiteral`, `ColSpecLiteral` and `PathLiteral` arms; the milestoning qualified-property hook. Move the 14 unit tests at the bottom to `tests/building_blocks.rs`. |
| `crates/runtime/src/builder.rs` | `src/builder.rs` | Keep `new()`, `registry()` and `build()`; drop `populators`, `extension_configs` and the two `dsl::*populators` calls inside `build()`. |
| `crates/runtime/src/native/lang.rs` — `letFunction`, `if`, `new` (three keys), `copy` | `src/native/lang.rs` | **Step 1, one `impl NativeFunction` at a time.** `getAll*` and `dynamicNew*` stay behind until a test needs them. |
| `crates/runtime/src/native/{arithmetic,comparison,boolean}.rs` — `plus`, `minus`, `times`, `eq`, `equal`, `and`, `or`, `not` | `src/native/…` | **Step 1**, as the building-block tests reach them. |
| `crates/runtime/src/native/{collection,string,math,datetime,meta,equality}.rs` | — | **Step 5 onward**, per runner `FunctionNotFound` row — done 2026-10-11 (natives round 1); `equality.rs` registers nothing yet. |

**Table 3 — stays on the archive until the step that fishes it.**

| Archive path | Fished in | Note |
|---|---|---|
| `crates/runtime/src/{pct.rs,runner/}`, `crates/runtime/resources/pct_grammar_rust_native.json`, `crates/runtime/src/native/testing.rs` | Step 5, optional | Local Rust-side runner, exclusion manifest, report; `assert*` / `executePCTTest` natives (`testing.rs`: `assert` fished 2026-10-09; the runner and manifest are still on the archive). The published numbers come from the Step 2 Java adapter. `testing.rs` reads the manifest through the old repository crate; replace that with a path. |
| `crates/runtime/src/relation.rs`, `src/native/relation/`, `crates/dsl-tds/src/csv.rs` | Step 5, when a PCT row needs relations | The CSV parser is copied as a module, not as a crate. |
| `crates/runtime/src/milestoning.rs` + `crates/pure/src/milestoning/mod.rs` | Step 5, when a PCT row needs milestoning | |
| `crates/runtime/src/dsl.rs` | Step 7 | DSL-instance hydration SPI. |
| `crates/runtime/src/{display,debug,extensions}.rs` | Not in the MVP | IDE debugger rendering; per-evaluator extension state. |
| `crates/jni/` (1.8 k lines: `lib`, `context`, `conversion`, `exception`, `codegen`), `legend-pure-runtime/legend-pure-runtime-rust-evaluator{,-bindings-ap}/` (Java, left in place but removed from the Maven reactor in Step 0), `crates/java-codegen/` | Step 2 | Handle table, value conversion, `PureRustEvaluator`, proxies. Adds `jni` and `slotmap` to the allowed dependencies; cut the crate's dependencies on `ast`, `lexer`, `parser`, `protocol`, `core-platform-pure`, `java-codegen`, `toml`, `tracing-subscriber`; re-add the `legend-pure-runtime-rust-evaluator` `<module>` line to `legend-pure-runtime/pom.xml`. |
| `crates/pure/src/resolve.rs` — only `least_upper_bound`, `least_upper_bound_ids`, `collect_ancestors`, `is_subtype`, `get_first_supertype` (≈ 100 lines) | Natives round 2: the trigger is `genericType` / `deactivate` leaving `NotYet` | Their only callers are the meta natives; extract the functions into `metamodel/src/subtype.rs` when those are fished. The rest of `resolve.rs` is the compiler. |
| `crates/runtime/tests/*.rs` (16 files) | Never as they are | Eleven compile `.pure` source through the old platform loader; `discovery_smoke.rs` and the three `extension_*_smoke.rs` exercise the dropped extension SPI; `platform_invariants.rs` asserts on the registry alone. Their *assertions* are a source of Step 1 and Step 5 test ideas. |
| `crates/{lexer,parser,ast,ast-derive,compose,protocol,ide,lsp,dap,mcp,cli,build,core-platform-pure,snapshot-builder,dsl-*,store-relational-runtime,stress,xtask}` | Never, by rule 1 | The Rust parser and compiler, tooling, DSLs, binary snapshot format. |

Rule for implementers: if a step's "Build" list names something in these tables,
the task is to *fish* it (copy the smallest compiling unit and its test).
Writing a parallel implementation is a review failure; so is copying a whole
file when one function was asked for.

## Appendix B — Step 0 mechanics

**Archive first.**

```bash
# `legend-pure-rust` is both a branch and a directory; qualify the ref.
git tag archive/pre-evaluator-extract-2026-10 refs/heads/legend-pure-rust
git branch legend-pure-rust-archive refs/heads/legend-pure-rust
git push origin archive/pre-evaluator-extract-2026-10 legend-pure-rust-archive
```

**Reading from the archive without checking it out.**

```bash
git show archive/pre-evaluator-extract-2026-10:legend-pure-rust/crates/runtime/src/eval.rs > /tmp/eval.rs
git ls-tree -r --name-only archive/pre-evaluator-extract-2026-10 -- legend-pure-rust/crates/runtime/src/
```

**Workspace skeleton.**

```toml
# legend-pure-rust/Cargo.toml
[workspace]
members = ["crates/metamodel", "crates/evaluator"]
resolver = "2"

[workspace.package]
edition = "2024"
license = "Apache-2.0"

[workspace.dependencies]
smol_str = "0.3"
thiserror = "2"
rust_decimal = "1"
im-rc = "15"
jiff = { version = "0.2", features = ["std"] }
tracing = "0.1"
```

`crates/metamodel/Cargo.toml` takes `smol_str`, `thiserror`, `rust_decimal`.
`crates/evaluator/Cargo.toml` takes `legend-pure-metamodel = { path = "../metamodel" }`
and the remaining four. The versions above are the archive's, so copied code
compiles unchanged.

**Layout after Step 0.**

```
legend-pure-rust/
  Cargo.toml
  CLAUDE.md                 # build, test, the six rules, pointer to the plan
  README.md
  docs/rust-evaluator-mvp.md
  docs/pelt-analysis.md
  rustfmt.toml
  scripts/check-copyright.sh
  crates/metamodel/src/{lib,source_info,ids,arena,types,annotations,model,bootstrap,m3_parser,fqn}.rs
  crates/metamodel/src/nodes/
  crates/evaluator/src/{lib,error,value,heap,context,scope_guard,date,m3_paths,model_utils,hooks,native,eval,builder,builders}.rs
  crates/evaluator/src/native/            # empty at the end of Step 0
  crates/evaluator/tests/building_blocks.rs
```

**Files outside the workspace that Step 0 touches.** `CLAUDE.md` at the
repository root (rewrite two sections), `.github/workflows/rust.yml` (new
command list), `legend-pure-runtime/pom.xml` (two `<module>` lines removed),
`docs/architecture/modules.md` and `docs/guides/getting-started.md` (their
Rust sections), a note at the top of the two
`legend-pure-runtime-rust-evaluator*` READMEs, and the deletion of what
described the archived workspace as current: `CONTRIBUTING.md` back to
master's pointer, `docs/compiler/binary_format.md`,
`legend-pure-classpath.toml`. Nothing else in the Java tree changes.

**Gates, as commands.**

```bash
cargo build --workspace
cargo tree --workspace -e normal --depth 1   # expect: the three crates and the eight dependencies (six plus jni, slotmap for crates/jni), nothing else
grep -rnE 'purem|pelt|legend_pure_parser|legend_pure_runtime|serde|linkme|postcard|toml' crates/   # expect: nothing (drop the pelt token when Step 3 adds crates/pelt)
cargo test --workspace
```

## Appendix C — Open questions owned by the author

Implementers should not resolve these on their own. Each has a **working
default** so a session is never blocked; use it, record that you did, and
flag it in the commit message.

1. **legend-engine target.** Which branch / commit of legend-engine is the
   target from Step 5 (engine suite class) onward — the fork's `rust-parser`
   branch or FINOS `upstream/master`? PELT contents and the set of
   `Test_*_PCT.java` classes differ between them. *Default: `upstream/master`.*
2. **Where Java-side helpers land.** The Step 2 `evalRust` native, suites
   and rebase tool, the Step 3 classloader callback, the Step 4 oracle JUnit and
   the Step 6 node helper are Java code. Decision per item: fork branch only,
   or upstream PR to finos/legend-pure (the `legend-pure-runtime-rust-evaluator`
   module already lives in this fork). *Default: fork branch only.*
3. **The engine PCT suite class.** Upstream PR to finos/legend-engine, or
   kept in the fork? This decides whether `Rust_JNI` results appear in
   FINOS CI or only locally. *Default: fork only.*
4. **Fixture refresh cadence.** Who re-runs `regen-pelt-fixtures.sh` when the
   Java platform changes, and is a stale oracle a build failure or a warning?
   *Default: whoever bumps the platform; a warning.*
5. **Extraction destination.** Step 0 assumes the new workspace *replaces*
   `legend-pure-rust/` on the live branch (archive keeps the old one). The
   alternative is a sibling directory with the old workspace left in place;
   that keeps two Rust directions visible, which rule 1 argues against.
   Default is replace unless the author says otherwise. *Decided 2026-10-09:
   replace.*
6. **Route to the first green.** The Step 4 / Step 5 lowerings over Java
   nodes reached through the bridge (no PELT needed; moves the `Rust_JNI`
   count directly), or over PELT first (Step 3, then 4 and 5, as the step
   order reads). *Decided
   2026-10-09: the bridge first; Step 3 follows. Done 2026-10-10 (Step 2
   "Next"): the Step 4 declarative lowering (classes, enumerations,
   function signatures) and the Step 5 body lowering exist in
   `crates/evaluator/src/lower/`, written against the heap's node view;
   Steps 3 and 4 now add the PELT node source and the directory, not the
   lowering.*
7. **The JDK floor of the Java module and the bridge mechanism.** The
   rust-evaluator module compiles with release 17 (decided 2026-10-11, the
   author's edit) and joins the reactor only on JDK 17+; the bridge stays
   on JNI until the floor reaches 22 (Step 2, "JNI or the Foreign Function
   & Memory API"). *Default: as decided.*
