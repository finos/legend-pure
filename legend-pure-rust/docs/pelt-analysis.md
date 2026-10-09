# PELT vs `.purem` — can the Rust runtime use (or mimic) Java's PELT format?

> **Status: analysis, 2026-10-06 (reframed the same day for incremental
> delivery, see "Goal of this exploration").** No code change. Companion to
> `lazy_loading_binary_format.md` (the original Tier-1/Tier-2 `.purem`
> design) and `crates/core-platform-pure/docs/PUREM_FORMAT.md` (the v0
> that shipped). Written to answer: *should the Rust interpreter adopt
> the Java PELT serialization instead of its own `.purem`, and is the
> PELT strategy something Rust can mimic?*

## Goal of this exploration (reframed 2026-10-06)

The Rust port stays an independent compiler + runtime, but delivery should be
**incremental**: Java came first and has already compiled every repository in
legend-pure and legend-engine into PELT. If the Rust runtime can *consume* PELT,
it can execute graphs from DSLs the Rust compiler does not parse yet, test its
runtime against Java-resolved graphs, and ship value before grammar coverage is
complete. Producing PELT *from* Rust is a possible later step (Java engine
consuming Rust-compiled graphs; two-way parity).

Independence is preserved because PELT is a contract over the **M3 metamodel**
(classifier + property bag per node), not over Java internals. Depending on M3
is depending on the language definition the port already implements.

## Verdict

1. **PELT bytes and the Rust `PureModel` live at different layers.** PELT is
   the resolved M3 *instance graph*; `.purem` is the Rust *typed, lowered IR*.
   Reading PELT is a bounded wire-format port (~2 k lines) **plus an M3-graph →
   `Element`/`ExprKind` lowering** (a second, resolution-free front-end). It is
   never "load and run" for Rust the way it is for the Java interpreter, but the
   lowering can run lazily per element, so laziness survives.
2. **PELT is the right incremental-delivery vehicle.** Every `*-pure` module in
   legend-pure and legend-engine ships it (engine #4699 made it the production
   metadata path). A Rust reader + lowering turns the entire engine graph into
   something the Rust runtime can load today — as property bags for DSLs Rust
   has no typed element for, as typed `Element`s where it does.
3. **PELT doubles as a compiler-parity oracle.** For the same source it carries
   Java's dispatch resolution, inferred types and milestoning rewrites. Diffing
   the Rust-compiled model against the PELT-lowered model element by element
   catches compiler divergence without going through the runtime.
4. **PELT already solves lazy loading.** Its manifest, function-name index and
   back-reference side tables are the pieces the deferred `.purem` Tier-2 design
   was missing. With PELT as the Tier-2 store and per-element lowering on
   demand, `.purem` becomes an optional cache of lowered IR rather than the
   primary artifact.

Recommendation (detail in §6): **build the PELT reader + lowering as the
primary track** (§6 option (c), delivered in the milestones of §6.1); keep the
Rust compiler and `.purem` for Rust-authored code; treat a Rust PELT *writer* as
a later milestone, not a prerequisite. Do not make raw PELT bytes the in-memory
model (§6 option (a)).

---

## 1. What PELT is (as of legend-pure `master`, Oct 2026)

**PELT = "Pure ELemenT".** Added 2025-09-10 (#1084 "Add PELT (Pure
Element) serialization"), compressed 2026-03-16 (#1217), back-reference
index 2026-03-31 (#1237), atomic writes (#1235), logging (#1256, April).
Actively maintained upstream.

Source: `legend-pure-core/legend-pure-m3-core/src/main/java/org/finos/legend/pure/m3/serialization/compiler/` (~21 k lines, ~90 files).
Full byte-level reference: `docs/architecture/pelt-serialization.md` at the
repository root (finos/legend-pure PR #1360).

### 1.1 File layout — one file per packageable element + per-module side tables

| File | Path | Content |
|---|---|---|
| `<pkg path>/<name>.pelt` | mirrors the Pure package path; functions use the mangled signature name (`at_T_MANY__Integer_1__T_1_.pelt`); packages and primitives get their own (`Root.pelt`, `meta.pelt`, `Integer.pelt`) | the element's self-contained sub-graph |
| `org/finos/legend/pure/module/<module>.pmf` | manifest | module name, **dependencies**, list of `(element path, classifier path, source span)` — the Tier-1 directory |
| `…/<module>.psr` | source metadata | per source file: sections `(parser name, element paths)` — rebuilds `SourceRegistry` without `.pure` text |
| `…/<module>.pxr` | external references | per element: the reference ids it points at outside itself |
| `…/<module>.pbr` + `…/<module>/<pkg path>/<name>.pbr` | back references | index of which elements have back-refs, then per *target* element: `(instance ref id → [BackReference])` |
| `…/<module>.pfn` | function names | `simpleName → [function element paths]` — the overload index |

"Module" == code repository (`platform`, `core`, …; `root` for the welcome
file). Written to a directory or straight into a jar via `ZipOutputStream`.

Measured on this checkout's `legend-pure-m3-core/target/classes`
(platform, tests included):

| | count | bytes |
|---|---|---|
| `.pelt` files | 1 894 | 3.55 MB |
| largest element | `testEqualNonPrimitive_…pelt` | 40 KB |
| `platform.pmf` / `.pxr` / `.pfn` / `.psr` | 1 each | 39 / 42 / 28 / 26 KB |
| per-element `.pbr` (largest: `eval_Function_1__T_n__V_m_.pbr`) | hundreds | 14 KB max |

For comparison `.purem` v0: 3 MB full platform, 603 KB production slice.
Not size-comparable — PELT is per-element and carries the full M3 graph
including every `GenericType`/`Multiplicity` instance.

### 1.2 Five independently versioned layers

Each layer is a `ServiceLoader` extension with `version()`; every file
header records which versions wrote it, so readers dispatch per file.

| Layer | Versions | Current default |
|---|---|---|
| `FilePathProvider` (naming) | 1 | 1 |
| `StringIndexer` (string table) | 0, 1, 2, 3 | 3 |
| `ConcreteElementSerializer` (element payload) | 1, 2 (= 1 + deflate level 7) | 2 |
| `ModuleMetadataSerializer` | 1, 2, 3 (= 2 + deflate) | 3 |
| `ReferenceId` (cross-element ids) | 1 | 1 |

### 1.3 Element payload (`.pelt`)

Envelope, written with the m4 `BinaryWriter` (**big-endian**, Java
`ByteBuffer` default; strings are `int` length + UTF-8 bytes):

```
i64  signature       = Long.parseLong("PureElement", 36)
i32  serializer version   (2 today)
i32  reference-id version (1 today)
---- v2: everything below is one DEFLATE stream ----
     string index  (StringIndexerV3, see 1.5)
str  element path
str  source id
u8   compileState bitset width
i32  node count
     node[0] = the element itself, then BFS order
```

Each node:

```
bool has name   [str name]            -- anonymous instances omit the name
str  classifier path                   -- e.g. meta::pure::metamodel::function::ConcreteFunctionDefinition
u8   source-info code  [6 ints of packed width: startLine/startCol/line/col/endLine/endCol]
u8   ref-id code       [str reference id]
int  compileState bitset
i32  property count
  per property:
    str  property name
    str  property *source type*  (the class that declares the property — resolves inheritance/overrides)
    i32  value count
    per value: u8 code, then
      INTERNAL_REFERENCE  → index into this file's node list (width = f(node count))
      EXTERNAL_REFERENCE  → str reference id (see 1.4)
      BOOLEAN/BYTE/DATE*/NUMBER*/STRICT_TIME/STRING → compact primitive
```

Rules that define the sub-graph:

- A node is **internal** iff its `SourceInformation` is lexically
  subsumed by the element's span. Packages (no source info) are always
  external. So a `.pelt` is exactly "everything the parser produced
  inside this declaration", with every edge out of it as a reference id.
- **Back-reference properties are stripped**: `applications`,
  `modelElements`, `propertiesFromAssociations`,
  `qualifiedPropertiesFromAssociations`, `referenceUsages`,
  `specializations` (`M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS`)
  plus `children`. Only `referenceUsages` whose owner is *internal* are
  kept inline. The cross-element ones go to `.pbr` (see 1.6).
- Stubs (`ImportStub`, `PropertyStub`, `EnumStub`, `GrammarInfoStub`)
  must be resolved or serialization fails; the stub node is written with
  its `resolvedNode` as an external reference.
- The serializer is **schema-agnostic**: it walks
  `class_getSimplePropertiesByName(classifier)` and never special-cases
  `Class`, `FunctionExpression`, etc. Any M3 class, including DSL
  metamodels (`Mapping`, `Database`, `Service`), serializes the same way.
  This is the "packageable instance" idea from
  `lazy_loading_binary_format.md`, applied to *every* element.

### 1.4 Reference ids = navigation paths from a packageable element

`ReferenceIdGenerator` BFS-walks each element and assigns every
source-bearing, non-stub internal node the shortest (then
lexicographically smallest) `GraphPath` from the element:

```
meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_          -- the element itself
<elt>.expressionSequence[0]                                             -- to-many at index
<elt>.expressionSequence[0].parametersValues[1].genericType.rawType     -- to-one chain
<elt>.qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0] -- keyed by a String property
<elt>.values['VAL1']                                                    -- shorthand keyed form (name/id/value)
<elt>.parameters['l']
```

Keyed edges are used whenever a to-many property's values have a unique
`name` / `id` / `value` string (`QualifiedProperty.id`,
`Stereotype.value`, `Tag.value`, `Enum.name`, …), so ids survive
reordering. Resolution (`ReferenceIdResolverV1`) = parse the path, load
the head element by package path, walk the edges. Properties on skip
list (`_package`, `children`, all back-ref props) are never traversed.
Ids are build-independent: no arena index, no counter.

### 1.5 String table (`StringIndexerV3`)

Per file: collect every string, assign ids. ~100 "special strings"
(`::`, `/`, M3 class paths, common property names, `Pure`, `import`, …)
have fixed **negative** ids and are never written. Other strings are
written once in a table, classified as simple / `::`-delimited package
path / `/`-delimited source path / `.`-delimited / import-group /
bracket-indexed (`prefix[value]`, `prefix['value']`, `prefix[key='value']`)
so that `meta::pure::functions::collection` is stored as references to
its segments. Id width is 1–4 bytes chosen from the table size.
Pure compression trick; equivalent to generic string interning + deflate.

### 1.6 Module metadata and back references

`ModuleMetadataGenerator` walks every element of a module once and emits:

- `ConcreteElementMetadata(path, classifierPath, sourceInfo)` → `.pmf`.
- `ElementExternalReferenceMetadata(path, [ref ids])` → `.pxr`.
- For each external node an element points at, a `BackReference` is
  recorded against the **containing element of the target**, keyed by
  the target instance's ref id:
  `Application(functionExpression id)`, `ModelElement(element path)`,
  `PropertyFromAssociation(property id)`,
  `QualifiedPropertyFromAssociation(qp id)`,
  `ReferenceUsage(owner id, property, offset, sourceInfo)`,
  `Specialization(generalization id)` → `.pbr`.
- `functionName → [paths]` → `.pfn`.
- `SourceMetadata(sourceId, [SectionMetadata(parser, [paths])])` → `.psr`.

This is what makes lazy loading sound: `Class.specializations`,
`Property.applications`, `Association` inverse properties can be
answered for one element **without hydrating the rest of the module**.

### 1.7 Loading and laziness

```
MetadataIndex   ← .pmf of requested modules + transitive dependencies (+ .pbr index)
ElementLoader   ← per-path ConcurrentMap<String, AtomicReference<CoreInstance>>; load-once, thread-safe
ElementBuilder  ← builds CoreInstances from InstanceData
```

Generated classes (`M3LazyCoreInstanceGenerator` → `AbstractLazyConcreteElement`)
hold `volatile Init`; the **first property access** inflates the `.pelt`,
builds the component nodes, and resolves external refs through the
resolver, which loads other elements on demand. Laziness is
**per-element-file with inflate-on-demand**, not mmap zero-copy — coarser
than the FlatBuffers Tier-2 sketch, but the directory (`.pmf`) is tiny
and the per-element cost is paid only for touched elements.

Two consumers, same files:

- **Compiled runtime** — `MetadataPelt` (replaces the old
  `DistributedBinaryGraph` "MetadataLazy").
- **Interpreted runtime** — `PureCompilerLoader` +
  `FSPeltPureGraphCache` / `FSPeltGraphLoaderHybridPureGraphCache`
  load modules into a `ModelRepository` of lazy `CoreInstance`s. The
  Java interpreter executes M3 `FunctionExpression` instances directly,
  so for Java "load PELT" **is** "ready to run". This is the property
  Rust cannot inherit (§3).

### 1.8 Producers

- `legend-pure-maven-compiler:compile-pure` (`PureCompilerBinaryGenerator`)
  bound in the root POM `pluginManagement` and used by every `*-pure`
  module in this repo (`m3-core`, `precisePrimitives`, all `legend-pure-dsl-*`).
- **legend-engine**: upstream finos commit `9e244c851f2` (2026-04-30,
  "Replace modular distributed metadata with pelt metadata", #4699)
  switched `PureModel.METADATA_LAZY` and `InterpretedMetadata.METADATA_LAZY`
  to `MetadataPelt.fromClassLoader(...)`; it is reachable from
  `upstream/master`, not fork-local. In the local checkout
  (`~/Projects/legend-engine`, 2026-10-06) 145 POM/Java files reference
  the `compile-pure` mojo. PELT is the engine's production metadata path
  today.

---

## 2. What `.purem` v0 is (for contrast)

One blob per repo: 22-byte header (`PUREM`, format version, FNV schema
hash of the Rust type list, payload length) + Postcard of
`PureModelSlice { chunks: Vec<ModelChunk>, external_refs: Vec<FqnPath>,
element_packages }`. `ModelChunk` = `Arena<ElementNode>` +
`Arena<Element>` (typed Rust structs, bodies already lowered to
`ValueSpec`/`ExprKind` with inferred `type_info`). Cross-repo refs are
element-level FQN sentinels resolved by `merge_slice`. Eager load; strict
refusal on any version/schema mismatch; derived indexes
(`specializations`, overload tables, association inverses) **recomputed
at merge**. DSL elements ride along as `Element::DSLInstance { dsl_name,
classifier_fqn, data: postcard bytes }`.

---

## 3. The discriminator: instance graph vs lowered IR

| | PELT node | Rust `.purem` |
|---|---|---|
| unit | `(classifier FQN, sourceInfo, refId, [prop → values])` for **every** M3 node, including `GenericType`, `Multiplicity`, `InstanceValue`, `VariableExpression`, stubs | `Element` enum (9 typed variants) with `ExprKind` trees |
| schema | the M3 class definitions (`class_getSimplePropertiesByName`) at *load* time | the Rust struct definitions at *compile* time (schema hash) |
| refs | GraphPath ids reaching **inside** elements | element-level `ElementId`/FQN; sub-element things are inline and by name (`EnumValue { enum_element, value }`, `StereotypeRef { profile, value }`, `PropertyCall { function_name }`) |
| types on expressions | `genericType`/`multiplicity` instances per node (Java inference) | `type_info: Option<ResolvedType>` per `ValueSpec` (Rust Pass 2.5) |
| what the evaluator runs | the M3 graph itself (Java interpreted) | `ExprKind` |

Consequences:

- The Rust runtime's `HeapEntry::Dynamic(RuntimeObject { classifier,
  properties })` is literally PELT's node shape, so **holding** PELT data
  in Rust is trivial. **Executing** it is not: `Evaluator` dispatches on
  `ExprKind`, so every function body would still need lowering
  M3 `FunctionExpression` → `ExprKind` before first call. That lowering
  is cheaper than today's AST lowering (no import resolution, no
  overload dispatch, no inference — `func`, `genericType`,
  `multiplicity` are all already resolved by javac-side Pure) but it is
  a new front-end: roughly the surface of `crates/pure/src/lower/`
  (3.4 k lines) plus signature hydration, minus resolution.
- Rust's `ElementId` is an arena index assigned at load; PELT's identity
  is the element path. A PELT-shaped `.purem` needs the manifest to
  pre-assign ids (manifest order == arena order), which the deferred doc
  already assumed.
- Rust needs no `.pbr`-style sub-element back references today because
  it recomputes `specializations` and association inverses — but that
  recomputation is exactly what forces **eager** loading of every
  element. Any lazy Tier-2 needs the precomputed index; PELT shows what
  to put in it.

---

## 4. Concept-by-concept mapping

| PELT concept | Rust today | Gap / transfer |
|---|---|---|
| `.pmf` manifest (name, deps, element directory) | `RepoMeta { name, pattern, dependencies }` + classpath TOML + `topo_sort_repos`; element directory only *inside* the Postcard payload | Move the directory out of the payload into a Tier-1 table: `(path, classifier, span, record offset/len)`. Gives `hasElement`/`getClassifierInstances` without hydration. |
| per-element record | one Postcard blob per repo | Per-element records in **one container file per repo** (not 1 894 files): index + offset table + optional deflate per record. Keeps `include_bytes!`/mmap friendly. |
| GraphPath reference ids | element FQN sentinels; sub-element by name | Not needed for forward refs (Rust IR only points at elements + names). Needed only if `.purem` ever carries back references into expression nodes (e.g. `applications`). Adopt the *grammar* if so; it is small (`path.prop`, `.prop[i]`, `.prop['k']`, `.prop[key='k']`). |
| `.pbr` back references | `derived().specializations`, association inverse walk, overload tables — recomputed on load | **The missing piece for lazy Tier-2.** Precompute at snapshot time: `specializations`, `properties_from_associations`, `functions_by_name`, and (if the LSP/IDE wants it) `applications`. |
| `.pfn` functions-by-name | `PureModel::overloads(name)` built at merge | Emit as a Tier-1 table; same data. |
| `.psr` source/section metadata | `Repo::Purem.manifests` (static JSON side table), `sources()` empty | Already equivalent in spirit; PELT shows sections should be in the manifest too (LSP "which file declared X"). |
| `.pxr` external refs | `PureModelSlice.external_refs` (flat) | Already equivalent (per-element grouping would allow partial-load dependency closure). |
| string index v3 | Postcard varints, no interning | Low value; deflate/zstd per record gets most of it. Skip. |
| five versioned layers, per-file headers | one `format_version` + schema hash, hard refusal | Adopt: version the container, the record payload, and the index tables independently; keep schema hash for the *payload* only. |
| lazy element (`volatile Init`, load-once map) | `Arena<Element>` fully hydrated | `Arena<OnceLock<Element>>` (or `LazyElement` from the deferred doc). `PureModel` is `Send + Sync` behind `Arc`; `OnceLock` keeps that. |
| schema-agnostic nodes for DSL elements | `Element::DSLInstance { dsl_name, classifier_fqn, data }` | Same *idea*, different payload: `data` is Postcard of a Rust-specific per-DSL struct, not a classifier + property bag. Loading engine DSL graphs from PELT needs either a generic `Element::Instance { classifier_fqn, properties }` variant or per-DSL hydration from property bags — a sizing line item for (c), not a free win. |
| `compileStateBitSet`, stub validation, `referenceUsages` | n/a | Ignore on read. |

---

## 5. Can Rust read the actual `.pelt` bytes?

Yes, and the wire-format part is bounded:

- m4 `BinaryWriter`: big-endian `i32`/`i64`, `int`-length-prefixed UTF-8
  strings, arrays as `int` count + items.
- Element envelope + v1 node grammar (§1.3) + v2 deflate (`flate2`).
- `StringIndexerV3` reader incl. the fixed special-string table (must be
  copied verbatim; ids are negative indexes into that array).
- `ModuleMetadataSerializerV2` grammars for `.pmf/.psr/.pxr/.pbr/.pfn` +
  v3 deflate.
- `GraphPath` parser for reference ids.
- Primitive decoders: packed ints, digit-string floats/decimals/big
  integers, date with width code (year…subsecond), strict time.

Estimate: ~2 k lines of Rust, no M3 knowledge needed to *parse*. What
the parse yields is a `Vec<InstanceData>` per element, which is a
`RuntimeObject`-shaped graph. The expensive part is §3's lowering.

Things a reader must handle that `.purem` never sees:

- `Root.pelt`, `meta.pelt`, … (packages) and `Integer.pelt`, … (primitives)
  are elements; Rust has a global package arena and bootstrap chunk 0 —
  skip and map onto existing ids.
- `ImportStub` nodes with `resolvedNode` external refs; `ImportGroup`
  references in source metadata.
- Milestoning: PELT carries Java's *rewritten* graph; Rust runs its own
  synthesis in Pass 2a'. Loading PELT must skip Rust synthesis for those
  elements (and is arguably more faithful to Java).
- Java inference results on every `ValueSpecification` → Rust can skip
  Pass 2.5 for loaded elements but must translate `GenericType` instances
  into `ResolvedType`.
- Per-element laziness is at the file/record level only. Within an
  element everything inflates at once.

---

## 6. Options

### (a) Make PELT the Rust native format

Rust reads (and eventually writes) `.pelt` + module metadata; `PureModel`
is built by lowering PELT nodes.

- Cost: ~2 k lines reader + M3-graph → `Element`/`ExprKind` lowering
  (new front-end, ~3–5 k lines) + writer side needs Rust to materialize
  a full M3 graph per element incl. reference-id generation (today the
  runtime only reflects elements lazily into `RuntimeObject`s, not
  expression trees). Writer is the expensive half.
- Pros: one artifact for both stacks; Java inference/dispatch results
  for free; engine jars already ship it; no Rust compiler needed for
  engine DSLs to be *loaded*.
- Cons: couples Rust's IR to Java's graph quirks (stubs,
  `classifierGenericType`, milestoning rewrite) — against the standing
  "Rust replaces Java, don't chase Java quirks" rule; Rust-compiled user
  code (LSP, REPL, `legend compile`) would still need the `.purem` path,
  so two formats would coexist anyway; writer is a large project.

### (b) Make `.purem` v1 PELT-shaped (keep the typed IR payload)

Container per repo with: Tier-1 manifest (`name`, `dependencies`,
element directory `(path, classifier, span, offset, len)`), Tier-1
indexes (`functions_by_name`, `specializations`,
`properties_from_associations`, per-element external refs, source
sections), Tier-2 per-element records (Postcard `Element`, optionally
deflated), independent version numbers per layer, `OnceLock` hydration.

- Cost: contained inside `crates/pure/src/purem/` (writer/reader are
  private), plus `Arena<OnceLock<Element>>` in `ModelChunk` and
  derived-index loading in `repo::load`. Medium.
- Pros: delivers the deferred Tier-2 laziness with the back-reference
  problem solved the way Java solved it; no coupling to Java bytes;
  schema evolution handled by versioned layers rather than a global
  schema-hash refusal.
- Cons: still no cross-stack artifact.

### (c) Bridge: PELT importer feeding `.purem`

Keep (b) as native. Add a `pelt` reader crate that turns engine-built
`.pelt` modules into `Element`s via the §3 lowering, so `legend snapshot`
(or `cargo build`'s snapshot-builder) can produce `<repo>.purem` from
Java-compiled jars. This is "generate from Java later" with the lowering
as the only new piece; the writer half of (a) is never needed.

- Cost: the reader (~2 k) + lowering (~3–5 k). The lowering is shared
  with (a) and is the thing to size first.
- Pros: engine DSL graphs become loadable by the Rust runtime through
  the normal `.purem` path; Java stays the canonical compiler for
  anything Rust doesn't compile yet; `.purem` stays decoupled.

### Recommendation

**(c) is the primary track. (b) is secondary and may become unnecessary. (a)
is rejected.**

Under the incremental-delivery goal, the bridge is not a sizing spike but the
deliverable: it is what lets the Rust runtime consume Java-compiled graphs
before the Rust compiler covers every DSL. The PELT-shaped `.purem` v1 (b) is
still a good design, but if PELT files themselves serve as the Tier-2 store
(manifest-driven `ElementId` allocation, per-element lowering on demand), the
lazy-loading motivation for (b) disappears and `.purem` is reduced to a cache of
already-lowered IR for Rust-authored code. Decide (b) after milestone 5 below,
with measurements.

### 6.1 Milestones (each shippable on its own)

| # | Deliverable | Scope | Proves |
|---|---|---|---|
| 1 | `legend-pure-pelt` reader crate + `legend pelt inspect` | parse `.pelt`, `.pmf`, `.psr`, `.pxr`, `.pbr`, `.pfn` into Rust structs (`InstanceData`-shaped); golden tests against the platform's existing `target/classes` output; GraphPath parser | wire format understood end to end |
| 2 | Declarative lowering | `Class`, `Enumeration`, `Profile`, `Association`, `PrimitiveType`, `Measure`/`Unit`, function *signatures* → `Element`; packages/primitives mapped onto the global package arena and bootstrap chunk 0; a generic property-bag `Element` variant so every element is loadable | type graph + LSP navigation over engine graphs |
| 3 | Body lowering + parity | M3 `ValueSpecification` trees → `ExprKind` (`SimpleFunctionExpression` → `FunctionCall`/`PropertyCall`, `InstanceValue`, `VariableExpression`, `LambdaFunction`, `KeyExpression`, `GenericType` → `ResolvedType`, …); run the platform PCT on Java-compiled platform PELT with the Rust runtime; element-by-element diff of Rust-compiled vs PELT-lowered platform | runtime parity on Java graphs; compiler-parity oracle |
| 4 | Engine graphs | typed hydration of Mapping and Relational from property bags; everything else stays property-bag | engine repositories executable from the Rust runtime |
| 5 | Lazy loading | `Arena<OnceLock<Element>>` sized from `.pmf`; lowering on first access; `.pfn` as overload index; `.pbr` for specializations / association inverses instead of recomputation | startup on the full engine graph without hydrating it |
| 6 | PELT writer (later, see §6.4) | full M3 view of Rust IR incl. expression nodes, reference-id generation (`GraphPath` BFS), module metadata generation | Java engine consumes Rust-compiled graphs; two-way parity |

### 6.2 Risks

- **The Rust IR is lossy relative to M3.** `ValueSpec.type_info` is optional,
  there is no `KeyExpression` or multi-value `InstanceValue` today, stubs and
  `ImportGroup`s have no counterpart, `classifierGenericType` is dropped on
  reflection. The lowering will force the IR to grow; the property-bag fallback
  keeps any gap from blocking a load.
- **Java-shaped graph quirks** (resolved stubs, milestoning already rewritten,
  Java inference results) are *data* in this direction, not behaviour to chase:
  the lowering reads them; the Rust compiler is not changed to emulate them.
  The parity diff in milestone 3 is where genuine divergences get triaged.
- **Reference ids reach inside elements** (`…expressionSequence[0]…`). Forward
  refs from element bodies are to top-level elements plus `name`/`id` keys, which
  Rust already represents; sub-element ids matter only for `.pbr` consumption
  in milestone 5.
- **Per-element laziness only.** A touched element inflates fully; large test
  functions and `Class.pelt` pay that on first access.

### 6.3 Executing from PELT: is the `.pure` source still needed?

**No.** PELT carries the compiled function bodies, not just signatures.
Inflating `meta/pure/functions/collection/tests/add/testAddWithOffset_Function_1__Boolean_1_.pelt`
from the platform build (2.7 KB on disk, 8.3 KB inflated) shows
`SimpleFunctionExpression`, `InstanceValue`, `VariableExpression` and
`parametersValues` nodes: the whole `expressionSequence` as an M3 instance graph,
with every call already resolved to its target function and every expression
carrying its inferred `genericType` and `multiplicity`.

What each consumer needs on top of the PELT files:

| Consumer | Needs | Does not need |
|---|---|---|
| Java interpreted engine | nothing for *evaluation* — it executes the M3 graph. Caveat: today's `PureCompilerLoader` rebuilds the source registry via `loadSourceIfLoadable`, which reads the `.pure` text from code storage and throws `Unknown source` if the file is absent. Bookkeeping for the IDE / incremental compiler, never consulted while running; engine jars ship both anyway. | source text while executing |
| Java compiled engine | the compiled jar — bodies run as generated Java bytecode; PELT is metadata only | `.pure` text |
| Rust runtime | the M3 → `ExprKind` lowering (milestone 3) + the native registry | `.pure` text; source ids and spans are already on every node and suffice for diagnostics |

What PELT never contains, for any engine: native function implementations (a
`native function` has no body in any format — the runtime's native registry
supplies it), the `.pure` text (only source ids and spans), and the
bidirectional links (those live in the optional `.pbr` side tables).

### 6.4 Producing PELT from Rust (milestone 6): feasibility

**Feasible, with one hard requirement.**

Why it is feasible:

- PELT is schema-agnostic in both directions. The Java loader never checks
  "is this a well-formed `Class`" — it instantiates the lazy class for the
  classifier path and fills properties by name from the property bag. Whatever
  Rust writes with the right classifier and property names, Java loads.
- Every file type has a small, fully specified grammar (§8 and
  `docs/architecture/pelt-serialization.md`). The writer mirrors the reader,
  roughly the same ~2 k lines.
- The Rust runtime already projects elements into an M3 shape for reflection
  (`RuntimeObject { classifier, properties }`) — exactly a PELT node. The
  writer is "that projection, for every node of every element, serialized".

The hard requirement — **reference-id parity.** Cross-element links are
strings (`meta::pure::metamodel::type::Class.properties['name']`,
`…expressionSequence[0].parametersValues[1].genericType`). For a Rust-produced
module to reference elements of a Java-produced module, or vice versa, Rust
must generate exactly the ids Java would: the same BFS, skip list, keyed-edge
rules, tie-breaks and to-many value ordering (§1.4). The algorithm is
deterministic and small, so this is testable rather than fragile. **Acceptance
gate for the writer:** compile the platform with Rust, emit PELT, decode both
outputs with the reader, diff node by node. Same parity oracle as the reader
direction, run backwards.

Where the Rust side has to grow:

- **Complete M3 shapes for expression trees.** Java's interpreter reads
  `func`, `parametersValues`, `genericType`, `multiplicity`, `usageContext`,
  lambda `openVariables` and `classifierGenericType`, `KeyExpression`s for `^`
  / `->copy`, resolved `ImportStub`s. The Rust IR keeps a subset (and
  `type_info` is optional). The M3-projection layer must synthesize the rest
  in the shape Java expects, which means the IR must carry enough to do so.
- **Back references and module metadata.** `.pbr` needs `applications`,
  `referenceUsages`, `specializations` and association-contributed properties
  expressed as reference ids; Rust computes equivalents already but must emit
  them in PELT's categories. `.psr` sections and `.pfn` fall out of what the
  compiler has.
- **Fidelity details.** Compile-state bits must be `PROCESSED | VALIDATED` or
  Java may treat elements as unprocessed; floats and decimals are stored as
  literal text, so preserve the literal rather than re-rendering an `f64`;
  synthesized nodes (milestoning properties, …) need source information
  consistent with Java's, or they get no reference id and cannot be pointed at
  from other elements.
- **Java-side quirk.** The interpreted loader still expects the `.pure` files
  on the code-storage path to register sources (§6.3). Fine for PELT produced
  from Rust-authored sources; a problem only for synthetic graphs with no files
  behind them.

Two tiers of ambition, in order:

1. **Java can load and execute it.** Needs the complete M3 shapes above. Also
   unlocks the Java compiled engine generating bytecode from a Rust-compiled
   graph, because `JavaCodeGeneration` runs over a loaded `ModelRepository`.
2. **Id-for-id interoperability with Java-produced modules.** Needs the
   reference-id parity gate. Without it Rust-produced modules can only
   reference each other, not Java-built dependencies — which defeats the
   purpose.

Sequencing: reader + lowering first (milestones 1–3), because they also reveal
exactly which M3 shapes the IR is missing; then the writer, with
"Rust-emitted platform PELT decodes identically to Java-emitted platform PELT"
as the definition of done.

### 6.5 Static code from PELT + dynamic code from users: compiling engine grammars into the same graph

The mechanism already exists in the workspace as "compile a repo against a
running model"; PELT just becomes another source of that running model.

**One model, two kinds of chunks.** `PureModel` is a list of chunks. Today
`repo::load` topo-sorts repos, merges `.purem` slices into chunks, then
compiles source repos on top with `pipeline::compile_repo_slice`, whose
resolver consults the package tree that already contains earlier repos'
elements. A PELT-loaded repo is the same thing: the milestone-2 lowering
allocates chunks from the manifest (one `ElementId` per `.pmf` entry, FQN
index, overload tables from `.pfn`, specializations and association inverses
from `.pbr`). User code is parsed by the Rust parser and compiled as a *new*
chunk against that model with the pipeline unchanged. Static code from PELT,
dynamic code from the user, one `ElementId` space, one resolver.

**What the user chunk needs from the static graph, and when.**

| Pass | Needs from static elements | Source in PELT |
|---|---|---|
| 1 declaration, 2a signatures, 2b body lowering + dispatch | class properties, function parameter / return types, enum values, overload candidates | manifest + declarative lowering (Tier 1), `.pfn` |
| 2.5 inference | property and return types | same |
| execution | function bodies | milestone-3 lowering, lazily per called function |

Compiling a user mapping therefore hydrates only the static elements it
references, never the whole engine graph.

**Engine grammars: a DSL instance is an instance of a static class.** The
metamodel for `###Mapping`, `###Relational`, `###Service`, `###Runtime`, … is
M2 Pure code (`meta::pure::mapping::Mapping`,
`meta::relational::metamodel::Database`, …) and comes from PELT. The user's
DSL block must become an instance of that class. Two lowering strategies, both
already in the architecture:

- **Typed**, for DSLs Rust owns: a `CompilerExtension` lowers the AST into a
  Rust struct carried as `Element::DSLInstance` (dsl-mapping, dsl-diagram
  today).
- **Generic property bag**, for everything else: AST node →
  `(classifier_fqn, property → values)` with the *schema* read from the static
  class in PELT — its properties, types and multiplicities validate the bag;
  element references go through the normal resolver. This is how
  legend-engine's Java `toPureGraph` builds the graph and exactly PELT's own
  node shape. One extension per grammar shrinks to "AST → property bag"; the
  per-DSL part is the semantic validators.

Pure expressions inside DSL blocks (mapping transforms, service queries,
filters) are lowered by the core expression lowering like any other lambda.

**Execution: static Pure functions consume dynamic instances.** The engine's
execution logic — routing, SQL generation, plan generation — is Pure code and
therefore lives in PELT; the user's instances are its arguments. The runtime
already has the bridge: `bootstrap_metamodel` pre-allocates a heap object per
element, and `DSLPopulator::populate` projects each `DSLInstance` payload into
`RuntimeObject` property rows that static Pure code navigates with ordinary
property access (`$mapping.classMappings`). A property-bag element needs one
generic populator instead of one per DSL. The remaining gap on this path is
not the format but the engine natives declared in Java (`core_functions_*`),
which need Rust implementations.

**Two design points to settle early.**

- **Overlay, not copy.** A server compiles many user models over one static
  graph. `PureModel.chunks` is an owned `Vec` today, so each user model would
  clone the static chunks. The static base should be shared (`Arc<ModelChunk>`
  or a layered model) with the user chunk as an overlay; this also matches the
  LSP's incremental recompile, which already replaces chunks.
- **Protocol JSON as a second front door.** User code from Studio arrives as
  Legend Protocol JSON; the `protocol` crate maps it to the same AST. Grammar
  text and JSON converge before lowering — something Java's engine, with its
  separate grammar → protocol → graph steps, does not have.

### 6.6 Exposing PELT to the Rust workspace

Expose it the way `.purem` is exposed today: one layer down for the bytes, one
layer up for the tooling.

**Crate layout (dependency rule: lower layers never depend on higher).**

| Crate / module | Depends on | Contents |
|---|---|---|
| `crates/pelt` (`legend-pure-pelt`), leaf | `flate2`, a zip reader | reader (later writer) for all six file types, `GraphPath` parser, decoded structs (`InstanceData`-shaped nodes, manifest, back refs). No knowledge of `PureModel`, so tooling and tests use it without the compiler. |
| `pure::pelt_lower` | `pelt`, the resolver | M3 nodes → `Element` / `ExprKind` (milestones 2–3) |
| `core-platform-pure::repo::Repo::Pelt` | both | fourth `Repo` variant next to `Embedded` / `Filesystem` / `Purem`; `repo::load` treats it like `Purem` — merge into the running model in topo order, then compile source repos on top (§6.5) |

**A byte-source abstraction mirroring Java's directory-or-classloader dual.**

```rust
trait PeltStore {
    fn manifest(&self, module: &str) -> Option<Bytes>;
    fn element(&self, path: &str) -> Option<Bytes>;
    fn module_file(&self, module: &str, kind: ModuleFile) -> Option<Bytes>;   // psr / pxr / pfn / pbr index
    fn back_refs(&self, module: &str, path: &str) -> Option<Bytes>;
}
```

Implementations: a directory (`target/classes`), a jar or an ordered list of
jars (a Maven classpath, first hit wins like a classloader), an in-memory map
for tests, and a JNI-backed store that calls `ClassLoader.getResource` on
demand. The `FilePathProvider` naming rules (path → file, 126-char truncation)
live in exactly one place, inside the store; callers never reconstruct names.

**Classpath TOML and CLI surface.** `legend-pure-classpath.toml` already has
`kind = "purem"`, `kind = "filesystem"` and a reserved `kind = "maven"`. Add
`kind = "pelt"` with `path` (directory or jar) or `jars = [...]`, and let
`maven = "org.finos.legend.pure:legend-pure-m3-core:<version>"` resolve from
`~/.m2` into the jar form. Module name and dependencies come from the `.pmf`,
so `RepoMeta.dependencies` is populated automatically and the topo sort needs
no extra configuration. The per-repo filesystem shadow override keeps working:
a `--live` source repo of the same name wins over its PELT copy — the dev loop.

**Load semantics the rest of the workspace never sees.** `Repo::Pelt` reads
manifests eagerly (Tier-1 directory, overloads from `.pfn`, specializations
and association inverses from `.pbr`) and hydrates elements lazily once
milestone 5 lands — eagerly before that. Either way the result is a
`PureModel`, so runtime, LSP, MCP and DAP neither know nor care where an
element came from. Worth adding: a provenance tag per chunk so diagnostics can
say "declared in `platform.pmf`" instead of pointing at a source file that is
not there.

**Tooling.**

- `legend pelt inspect <file>` — dump one element's nodes;
  `legend pelt ls <dir|jar>` — modules and elements from manifests.
- `legend pelt diff <dir|jar> --sources <repo>` — the parity oracle:
  Rust-compiled model vs PELT-lowered model, element by element.
- `legend run` / `test` / `check` / `repl` accept the PELT classpath like any
  other kind.

**JNI.** `nativeInitContextWithClasspath` already exists. For the Rust runtime
embedded in a JVM the right store is the JNI one: the JVM already holds every
jar on its classpath, so Rust asks the classloader for
`meta/pure/functions/collection/map_….pelt` bytes on first use and never
copies or re-ships anything. That gives the JNI path lazy startup from the
same artifacts legend-engine uses — one of the original motivations for
`.purem`.

### 6.7 JNI: implementing Java's `CoreInstance`-derived interfaces over the Rust environment

Yes — two routes, and one of them falls out of the PELT work almost for free.

**Route 1 — data bridge through PELT's in-memory contract (recommended for
metamodel objects).** Java already has a generated implementation class for
*every* M3/M2 class in both engines, built to be fed from outside:

| Engine | Generator | Generated classes |
|---|---|---|
| interpreted | `M3LazyCoreInstanceGenerator` | over `AbstractLazyConcreteElement` / `AbstractLazyComponentInstance` |
| compiled | `ClassPeltImplProcessor` | `*_LazyConcrete`, `*_LazyComponent`, `*_LazyVirtual` over `AbstractCompiledLazyConcreteElement`, implementing the full generated interfaces (`Class<T>` alone: 88 methods; `CoreInstance`: 65) |

All are constructed from manifest metadata plus two suppliers — one returning
a `DeserializedConcreteElement` (the element's `InstanceData` nodes), one
returning back references — with `ElementBuilder` as the factory and
`ElementLoader` as the per-path cache. Rust therefore implements **no Java
interface**: it supplies `InstanceData` per element on demand (classifier
path, name, source span, reference id, compile states, property → values with
internal indexes, external reference-id strings and primitives). That producer
is the milestone-6 writer minus the bytes, so Route 1 is a by-product of work
already on the plan.

Two integration options on the Java side:

- *Zero Java change:* Rust emits PELT bytes in memory; Java calls
  `ConcreteElementDeserializer.deserialize(InputStream)` exactly as for a jar
  entry.
- *Small upstream change:* a Rust-backed `ElementLoader` / `PureCompilerLoader`.
  Both are abstract with private file-backed subclasses today; a pluggable
  `deserializeConcreteElement` hook is a one-line widening.

Laziness survives (Java asks Rust for an element on first access; external
refs resolve through `ReferenceIdResolver` back into the same loader).
Java-side mutation of the lazy objects diverges from Rust, which is fine for
compiled metadata — immutable after compilation. This route puts Rust-compiled
graphs under `MetadataPelt`, under legend-engine's `PureModel`, and under the
Java interpreter.

**Route 2 — live proxies, extending what `java-bindings` already does.**
`PureProxyFactory` + `PureInvocationHandler` wrap a `PureRustInstance` handle
in a `java.lang.reflect.Proxy` implementing a generated interface; every
method call becomes `nativeGetProperty`, with the slotmap handle table and
`nativeFreeInstance` managing lifetime. Doing the same for
`CoreInstance`-derived interfaces is possible but costs more than it looks:

- `CoreInstance` carries mutation (`setKeyValues`, `addKeyValue`,
  `_propertiesAdd`, …), repository access, synthetic ids, compile states,
  `print`, `commit`, `validate`. Each needs a route; mutators need new
  write-through JNI entry points.
- Generated compiled code casts to the generated *interfaces* (a proxy can
  satisfy those), but some support paths test for the abstract base classes or
  rely on generated `Impl` behaviour such as equality keys — those break under
  a proxy.
- Every property read is a JNI transition. The interpreter and the type
  checker walk the metamodel constantly; hot paths would pay hundreds of
  nanoseconds per hop against field reads today.

Route 2 stays the right tool for what it does now: runtime values and user
instances crossing the boundary during execution, where the graph is small and
live.

**The split:** metamodel and compiled graphs cross as *data* through the PELT
contract (Route 1); execution-time values cross as *proxies* (Route 2). The
`_rustInstance()` back door on `Any` bridges the two when a Java caller holds a
proxy and needs its `CoreInstance` view. The reverse direction — Rust consuming
Java `CoreInstance`s — is the PELT reader (milestones 1–3), or, for live
engine-built graphs, the JNI node bridge of §6.8.

### 6.8 JNI: the Rust interpreter consuming engine-built Java objects (interpreter without a compiler)

**Question.** legend-engine's own compiler (`toPureGraph`) handcrafts compiled
`_Impl` objects — `new Root_meta_pure_metamodel_type_Class_Impl(...)` in
`PureModel`, `Milestoning`, `ValueSpecificationBuilder`, … Could the Rust
interpreter operate on those directly, so a Rust *interpreter* can be added to
the engine without a Rust *compiler*?

**Answer: yes.** The seam exists on both sides.

Why it works regardless of how the Java object was made:
`Root_meta_pure_metamodel_type_Class_Impl extends ReflectiveCoreInstance`, so
besides its typed accessors it answers the generic `CoreInstance` protocol —
`getClassifier()`, `getKeys()`, `getValueForMetaPropertyToOne/ToMany(name)`,
`getSourceInformation()`, primitives wrapped as `ValCoreInstance`. Engine
handcrafted `_Impl`s, `MetadataPelt`'s `_LazyConcrete` objects and the Java
interpreter's `SimpleCoreInstance`s all speak it, and it is exactly PELT's node
shape: classifier + property bag.

**Design: one lowering, two node sources.**

- The runtime heap already has `HeapEntry::Typed(Box<dyn TypedObject>)`
  (`classifier_path`, `get_property`, `set_property`). A `JavaBackedObject:
  TypedObject` holds a JNI global reference and reads properties through the
  `CoreInstance` protocol; static Pure code navigates `$class.properties` on an
  engine-built class exactly as on a PELT-loaded one.
- The milestone-3 M3 → `ExprKind` lowering is written against a small
  `M3Node` trait (classifier, name, span, to-one, to-many) with two impls:
  PELT `InstanceData` and the JNI-backed node. An engine-compiled
  `LambdaFunction_Impl` tree then lowers like a PELT body. Lowering is cached
  per Java object identity (once per request).
- One JNI transition per *object*, not per read: a small Java helper in
  `legend-pure-runtime-rust-evaluator` returns a whole node in one call
  (classifier, keys, values as primitives or handles — the mirror of the
  existing `nativeNew` arrays). Rust snapshots the node on first touch; nested
  objects stay lazy handles.
- Lifetime: the existing handle table in reverse — Java objects held by Rust
  are JNI global refs released on heap drop, as `nativeFreeInstance` releases
  Rust objects held by Java.
- Writes, if ever needed: `setKeyValues` / `addKeyValue` on the Java side;
  Rust-created values materialised Java-side by classifier (the compiled
  runtime's instance builders already do this for `^Class(...)`). First
  increments treat engine graphs as read-only.

**Worked example.**

1. The engine compiles a user model; `PureModel` holds `my::Person` as a
   `Root_meta_pure_metamodel_type_Class_Impl`.
2. Java calls
   `rustEvaluator.evaluate("meta::analytics::class::getClassInfo_Class_1__ClassInfo_1_", personImpl)`.
3. Rust wraps `personImpl` as a `JavaBackedObject`, loads the analytics
   function body from PELT (static), evaluates it; every `$c.properties` /
   `$p.genericType.rawType` read fetches one node across JNI and caches it.
4. The result returns as a Rust object behind a proxy, or as Java values if
   primitive.

No Rust compiler is involved: static code from PELT, dynamic objects from the
engine's own compiler.

**Does the whole `PureModel` (the compilation unit) have to appear in Rust?
No.** What must be in Rust splits four ways:

| Category | When it crosses | Mechanism |
|---|---|---|
| Dynamic objects reachable from the argument | on demand, as navigated | each hop is a Java reference → JNI node on first touch; the rest of the unit never leaves Java |
| References from engine objects into the platform (`rawType = String`, `Any`, profiles, stereotypes) | **before the first call** — the one up-front requirement | identity unification: a crossing `PackageableElement` reports its path, Rust looks it up in its PELT-loaded model and substitutes the static `ElementId`; only unknown paths become foreign nodes. Without this `instanceOf`, type `==` and dispatch break |
| Definitions the evaluator reasons about *by type* (user class of a value: `match`, subtype tests, property typing) | lazily, on first classifier miss | fetch the class node, run the milestone-2 declarative lowering (properties, supertypes), register in an overlay chunk — the §6.5 overlay filled from Java instead of the Rust compiler. Instances need no lowering; `specializations` etc. are already populated on the Java `_Impl`s |
| Whole-model queries (`Class.all()`, "every mapping in the model") | only if the function asks | a bulk call into the engine's `PureModel` indexes, or switch strategy: export the unit once per compile as a PELT snapshot and load it through the reader |

Rule of thumb: argument-reachable work → lazy proxies; whole-model analytics
(lineage, search) → snapshot. A point in favour of the JNI route for engine
graphs: PELT serialization requires source information on every element and
engine-built elements often lack it; the node bridge does not care (spans are
optional on nodes).

**Incremental wins this unlocks without a Rust compiler.**

- **A third PCT adapter** — "Rust interpreted" beside "Java interpreted" /
  "Java compiled" for `legend-engine-pure-code-functions-*`: PELT in, JNI
  values in/out, parity measured by the existing PCT contract. Lowest risk,
  most measurable.
- **Graph analytics written in Pure** —
  `legend-engine-xts-analytics-{class,mapping,lineage,search,quality}-pure`
  are Pure functions over the compiled user graph: static code from PELT,
  dynamic graph via JNI nodes, results marshalled back (the example above).
- **In-memory relation / TDS functions** — `meta::pure::functions::relation::*`
  over in-memory data, consuming the engine's compiled lambda objects; the Rust
  natives already exist.
- **Dev tooling** — REPL / LSP evaluation against an engine `PureModel`
  without round-tripping through grammar text.

**Natives: aim for 100 %, reach it incrementally through PCT.** Every
`native function` the engine declares in Java (`core_functions_*`) needs a Rust
implementation; nothing in PELT or the JNI bridge changes that. What changes is
that the gap becomes *measured*:

- Running the PCT suites over Java-compiled PELT in the Rust interpreter sorts
  every failure into one of two buckets — missing native or lowering gap. The
  surveyor's missing-natives harvester already produces that histogram for the
  platform; the same tooling yields a ranked list of natives to write next,
  weighted by how many PCT functions each unblocks.
- Each `legend-engine-pure-code-functions-*` module brings its own PCT
  functions and the natives they declare, so coverage is driven module by
  module, with the "Rust interpreted" adapter reporting a percentage per module
  rather than one global number.

**Where it does not help yet.** Plan generation and routing are Pure code with
a heavy Java-native and extension surface, so they sit at the end of that
native backlog rather than its start; store execution that lives in Java stays
in Java; and first-touch JNI latency per node means graphs of hundreds of
thousands of nodes want the snapshot route, not live proxies.

---

## 7. Concrete design notes for `.purem` v1 (option b)

Container (one file per repo, still `include_bytes!`-able):

```
header      magic "PUREM", container_version, index_version, record_version, schema_hash(record payload)
manifest    repo name, dependencies, element directory [(path, classifier_fqn, span, record_off, record_len)]
indexes     functions_by_name, specializations, properties_from_associations,
            external_refs_by_element, source_sections
records     per-element Postcard(Element) [+ deflate]  — Tier 2
```

- `ElementId` = `(chunk, directory index)`; allocate `Arena<OnceLock<Element>>`
  of directory length at load. `ElementNode` (name, spans, package) is
  filled from the manifest — that is Tier 1.
- Cross-element refs inside a record stay FQN sentinels (today's
  `EXTERNAL_REF_SENTINEL`) resolved at *record* hydration against the
  directory, not at repo load.
- Back references are tables keyed by `ElementId`, emitted by
  `snapshot-builder` from the fully compiled model (the same walk that
  builds `derived()` today) — `merge_slice` loads them instead of
  recomputing.
- Version each layer; on mismatch of `record_version`/`schema_hash` fall
  back to "parse from source" when sources are available (`Repo::Embedded`
  shadow), hard-fail only for `Repo::Purem` without sources.
- Determinism gate unchanged: directory sorted by FQN, records in
  directory order, byte-identical re-serialization.

---

## 8. Reader cheat sheet (input to milestone 1)

- Byte order: **big-endian** everywhere. PELT writes through
  `StreamBinaryWriter extends AbstractSimpleBinaryWriter`, which packs
  `short`/`int`/`long` via a default-order (`BIG_ENDIAN`) `java.nio.ByteBuffer`.
- Strings outside the string index: `i32 len` + UTF-8. Inside an
  indexed section: `writeString` emits a string-table id of 1/2/3/4
  bytes (width chosen by table size); the on-wire byte is
  `id + SPECIAL_STRINGS.length + Byte.MIN_VALUE` so the negative
  special-string ids fit in the same width.
- Element file: `i64 Long.parseLong("PureElement", 36)`, `i32 ser_ver`,
  `i32 refid_ver`; if `ser_ver == 2` the remainder is a **raw** DEFLATE
  stream (`CompressorPool.borrowDeflater(7, nowrap = true)`: no zlib
  header/trailer, no gzip header → `flate2::read::DeflateDecoder`, not
  `ZlibDecoder`). Same for metadata v3.
- Node value code byte (`BaseV1`): top 3 bits select
  `INTERNAL 000 / EXTERNAL 100 / BOOLEAN 010 / BYTE 001 / DATE 110 /
  NUMBER 101 / STRICT_TIME 011 / STRING 111`; low bits carry width,
  sign, number subtype (`INTEGER/BIG_INTEGER/FLOAT/DECIMAL`) or date
  subtype (`DATE/STRICT_DATE/DATE_TIME/LATEST_DATE`) and date width
  (`YEAR…SUBSECOND`).
- Source info code: `0x80 | int_width`; then six ints of that width.
- Module files: each starts with `i64` signature + `i32` metadata
  version written by `ModuleMetadataSerializer`
  (`PureManifest` / `PureSource` / `PureExtRefs` / `PureBackRefs` /
  `PureBRIndex` / `PureFuncName`, all `Long.parseLong(name, 36)`); at
  v3 the body is a DEFLATE stream whose grammar is
  `ModuleMetadataSerializerV2` (string index first, then the record).
- Reference-id grammar (`GraphPath`): `<element path>` followed by zero
  or more of `.prop`, `.prop[<int>]`, `.prop['<string>']`,
  `.prop[<keyProp>='<string>']`.
