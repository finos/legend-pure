# Plan: Remove Distributed Metadata (Modular and Monolithic)

**Status:** agreed - all open decisions resolved (see [section 7](#7-decisions-resolved))
**Scope:** `legend-pure` only (with a text-only survey of `legend-engine` recorded below)
**Superseded by:** PELT metadata (`PureCompilerSerializer` / `FileSerializer` write side, `MetadataPelt` read side)

---

## 1. What is being removed

"Distributed metadata" is the compiled-mode binary metadata format that predates PELT.
It has two flavours, which share the whole serializer/deserializer stack and differ only
in whether a metadata *name* is present:

| Flavour | Serializer | Metadata name | On-disk layout |
|---|---|---|---|
| **Monolithic** | `DistributedBinaryFullGraphSerializer` | none (`null`) | `metadata/classifiers/**.idx`, `metadata/bin/*.bin`, `metadata/strings/*.idx` |
| **Modular** | `DistributedBinaryRepositorySerializer` | repository name, plus a `DistributedMetadataSpecification` | same, namespaced per repo: `metadata/specs/<name>.json`, `metadata/classifiers/<name>/**`, `metadata/bin/<name>/*.bin`, `metadata/strings/<name>/*.idx` |

The full runtime chain being retired is:

```
CoreInstance graph
  -> GraphSerializer.buildObj (Obj / ObjRef / EnumRef / Primitive model)
  -> DistributedBinaryGraphSerializer (+ DistributedStringCache, BinaryObjSerializer*)
  -> metadata/** files
  -> DistributedBinaryGraphDeserializer (+ LazyStringIndex, BinaryObjDeserializer*)
  -> MetadataLazy.LegacyMetadataLazy
  -> generated `*_LazyImpl` / `PureEnum_LazyImpl` CoreInstance classes
```

The PELT replacement, already in place, is:

```
CoreInstance graph
  -> PureCompilerSerializer / FileSerializer / ModuleMetadataSerializer
  -> module + element binary files
  -> MetadataPelt (MetadataIndex + ElementLoader + ReferenceIdResolver)
  -> generated `*_LazyConcrete` / `*_LazyComponent` / `*_LazyVirtual` / `PureEnum_LazyComponent` classes
```

---

## 2. Findings from the code survey

These facts drive the phasing below. They were established by static search only; no
`legend-engine` build was attempted.

1. **All distributed-metadata code lives in one module:**
   `legend-pure-runtime/legend-pure-runtime-java-engine-compiled`, in
   `.../compiled/serialization/binary` (29 main classes), `.../compiled/serialization/tool`
   (1 class), plus edits to five classes elsewhere. Nothing in `legend-pure-core`,
   `legend-pure-m4`, `legend-pure-dsl/*`, `legend-pure-store/*` or `legend-pure-maven/*`
   references it.

2. **Nothing in production writes distributed metadata any more.**
   `JavaStandaloneLibraryGenerator.serializeAndWriteDistributedMetadata(...)` (4 overloads)
   and `compileSerializeAndWriteClassesAndMetadata(...)` are called **only** from
   `TestJavaStandaloneLibraryGenerator`. The Maven path
   (`PureCompiledJarMojo` -> `JavaCodeGeneration.doIt`) already serializes PELT for *both*
   generation types.

3. **`GenerationType.monolithic` / `modular` is out of scope.** These names *used to* select the
   metadata flavour, which in turn dictated how generation was grouped. Both branches now
   serialize PELT via `PureCompilerSerializer`, so the grouping no longer makes any difference
   to Java or metadata generation - though it may still have memory implications for very large
   graphs. The two cases are expected to be merged eventually, but that is a separate piece of
   work: **do not touch `GenerationType` here.** Note that although all 142 `legend-engine`
   modules that bind `build-pure-compiled-jar` set `<generationType>modular</generationType>`,
   `monolithic` is still used elsewhere, so neither branch may be dropped.

4. **`legend-engine` has no source dependency on any of it.** Text search found:
   - zero references to `DistributedBinaryGraphSerializer`, `DistributedBinaryGraphDeserializer`,
     `DistributedMetadataSpecification`, `DistributedMetadataHelper`, `DistributedMetadataTool`,
     `DistributedStringCache`, `serializeAndWriteDistributedMetadata`,
     `compileSerializeAndWriteClassesAndMetadata`;
   - zero references to `MetadataLazy` in Java source (one prose mention in
     `docs/engineering/architecture/alloy-compiler.md`);
   - zero references to `org.finos.legend.pure.runtime.java.compiled.serialization.model`
     (`Obj`, `RValue`, ...) or to `_LazyImpl`;
   - **it does** use `IdBuilder.sourceToId(...)` (`PureTestHelperFramework`, `MFTTestSuitBuilder`),
     `IdBuilder.newIdBuilder(processorSupport)` and `IdBuilder.newIdBuilder("$core$", processorSupport)`
     (`TestIdBuilderCore`), and `MetadataPelt`. These must all keep working, and the
     `$<name>$` id-prefix format must be preserved (see decision **D2**).
   - it does **not** use `JavaStandaloneLibraryGenerator` or `JavaSourceCodeGenerator` directly;
     it does use `new ProcessorContext(processorSupport, false)`.

5. `legend-sdlc` and `legend-shared` contain no source references (matches were only inside
   prebuilt shaded jars under `target/`).

6. **Build gotcha - `maven-dependency-plugin:analyze-only` runs at `test-compile` with
   `failOnWarning=true`** (root `pom.xml`). `DistributedMetadataSpecification` is the *only*
   consumer of Jackson in `legend-pure-runtime-java-engine-compiled`; deleting it without
   also dropping `jackson-annotations`, `jackson-core` and `jackson-databind` from that
   module's `pom.xml` will fail the build with "Unused declared dependencies".

7. **`MetadataLazy` is already a two-mode compatibility shim**: `LegacyMetadataLazy`
   (distributed) and `PeltMetadataLazy` (delegates to `MetadataPelt`). Only the legacy half
   is being removed; the class stays, per the stated requirement to preserve downstream
   references.

8. **`IdBuilder` is likewise two-mode**: `ReferenceIdV1IdBuilder` (PELT-compatible, keep) and
   `LegacyIdBuilder` (distributed-only). `IdBuilder.legacyBuilder(...)` has exactly one main-code
   caller: `DistributedBinaryGraphSerializer.newIdBuilder_internal` when the metadata name is
   `null`. That method is *also* how Java code generation gets its `IdBuilder`
   (`JavaStandaloneLibraryGenerator.getSourceCodeGenerator`), which is the one real coupling
   between the distributed stack and code generation - see decisions **D2** and **D3**.

9. **`_LazyImpl` code generation is distributed-only.** `ClassLazyImplProcessor` and
   `EnumProcessor.ENUM_LAZY_CLASS_NAME` produce classes whose only constructor is
   `(Obj, MetadataLazy)`, instantiated exclusively by `LegacyMetadataLazy`. `PeltMetadataLazy.valueToObject`
   throws `UnsupportedOperationException`, so these classes cannot function against PELT metadata.
   They become dead code the moment `LegacyMetadataLazy` is deleted - see decision **D4**.

---

## 3. Inventory

### 3.1 Main sources - delete outright

| Path (under `legend-pure-runtime/legend-pure-runtime-java-engine-compiled/src/main/java/org/finos/legend/pure/runtime/java/compiled/`) | Notes |
|---|---|
| `serialization/binary/` - **all 29 files** | `AbstractBinaryObj{Ser,Deser}ializer`, `BinaryGraphSerializationTypes`, `BinaryObjDeserializer{,WithStringIndex,WithStringIndexAndImplicitIdentifiers}`, `BinaryObjSerializer{,WithStringCache,WithStringCacheAndImplicitIdentifiers}`, `DistributedBinaryFullGraphSerializer`, `DistributedBinaryGraphDeserializer`, `DistributedBinaryGraphSerializer`, `DistributedBinaryRepositorySerializer`, `DistributedMetadataHelper`, `DistributedMetadataSpecification`, `DistributedStringCache`, `EagerStringIndex`, `FileReader`, `FileReaders`, `FileWriter`, `FileWriters`, `LazyStringIndex`, `SimpleBinaryObj{Ser,Deser}ializer`, `SimpleStringCache`, `SourceCoordinateMapProvider`, `StringCache`, `StringCacheOrIndex`, `StringIndex` (~4,500 lines) |
| `serialization/tool/DistributedMetadataTool.java` | CLI inspection tool (214 lines); the whole `tool` package goes with it |
| `generation/processors/LegacyIdBuilder.java` | 194 lines (**D3**) |
| `serialization/model/` - **all 13 files** | `Obj`, `ObjRef`, `Enum`, `EnumRef`, `Primitive`, `PropertyValue{,Consumer,Many,One,Visitor}`, `RValue{,Consumer,Visitor}` (~834 lines) (**D4**) |
| `generation/processors/type/_class/ClassLazyImplProcessor.java` | 278 lines (**D4**) |
| `generation/processors/support/coreinstance/AbstractLazyReflectiveCoreInstance.java` | 160 lines (**D4**) |
| `generation/processors/support/coreinstance/PersistentReflectiveCoreInstance.java` | Becomes unreferenced once the line above goes - `AbstractLazyReflectiveCoreInstance` is its only subclass. Public API in a package that generated code imports by wildcard, so deleting it is optional; flag it in review. |

### 3.2 Main sources - edit

| File | Change |
|---|---|
| `metadata/MetadataLazy.java` | Delete inner class `LegacyMetadataLazy`, `newMetadata(ClassLoader, DistributedBinaryGraphDeserializer)`, `fromClassLoader(ClassLoader)`, and - with **D4** - the `valueToObject` / `valuesToObjects` members. Keep `PeltMetadataLazy` and the `fromClassLoader(ClassLoader, ...names)` overloads. Mark the whole class `@Deprecated` (**D6**). |
| `testHelper/PureTestBuilderCompiled.java` | Switch `MetadataLazy.fromClassLoader(classLoader, repoNames)` (line ~256) to `MetadataPelt.fromClassLoader(...)` so no `legend-pure` code depends on the deprecated shim (**D6**). |
| `generation/JavaStandaloneLibraryGenerator.java` | Delete the 4 `serializeAndWriteDistributedMetadata` overloads and `compileSerializeAndWriteClassesAndMetadata`; drop the `useLegacyMetadataForExternalAPI` field/ctor param (keep a no-op `@Deprecated` overload of `newGenerator`); replace `DistributedBinaryGraphSerializer.newIdBuilder(compileGroup, ps)` in `getSourceCodeGenerator` with a **private** helper in this class (**D2**, **D3**). Remove the now-unused `java.io.IOException` / `JarOutputStream` imports as applicable. |
| `generation/ExternalClassBuilder.java` | Drop the `useLegacyMetadata` parameter and the `MetadataLazy.fromClassLoader(classLoader)` branch; emit `MetadataPelt` unconditionally; move `MetadataPelt` into `BASE_EXTERNALIZABLE_IMPORTS`. |
| `generation/JavaSourceCodeGenerator.java` | Drop the `useLegacyMetadataForExternalAPI` field and ctor param; keep `@Deprecated` pass-through ctors. (The adjacent `generateCompilerExtensionCode` param is already dead - removing it is optional cleanup, flag in review.) |
| `execution/JavaCompilerEventHandler.java`, `generation/orchestrator/JavaModelFactoryGenerator.java` | Update to the trimmed `JavaSourceCodeGenerator` ctor. |
| `generation/processors/IdBuilder.java` | Remove `LegacyBuilder` + `legacyBuilder(...)` (**D3**). |
| `serialization/GraphSerializer.java` | Delete `buildObj` (both overloads), `collectProperties`, `buildRValue`, and the `ClassifierCaches` inner class (**D4**). **Keep** `valueSpecToJavaObject`, `valueSpecValueToJavaObject`, `processPrimitiveTypeJava` - `FunctionExecutionCompiled` depends on them. |
| `generation/JavaPackageAndImportBuilder.java`, `generation/processors/type/EnumProcessor.java`, `generation/processors/type/_class/ClassProcessor.java` | Remove `buildLazyImplClass*` helpers, `ENUM_LAZY_CLASS_NAME` + its builder, and the `isLazy(_class)` -> `ClassLazyImplProcessor.buildImplementation` call in `processClass` (**D4**). `ClassProcessor.isLazy` itself then becomes unused. |
| `generation/JavaSourceCodeGenerator.java` (import block, ~line 131) | **Required for D4:** the fixed import block emitted into *every* generated source file contains `import org.finos.legend.pure.runtime.java.compiled.serialization.model.*;`. Importing a package that no longer exists is a compile error, so this line must be removed in the same commit that deletes `serialization/model`. |
| `legend-pure-runtime/legend-pure-runtime-java-engine-compiled/pom.xml` | Remove `jackson-annotations`, `jackson-core`, `jackson-databind` (compile scope). Leave the test-scoped `jersey-media-json-jackson`. |

### 3.3 Tests - delete

Under `.../src/test/java/org/finos/legend/pure/runtime/java/compiled/`:

- `runtime/serialization/binary/` - all 13 files (`TestBinaryGraphSerializationTypes`,
  `TestClassLoaderDistributedBinaryGraphSerialization`,
  `TestDirectoryClassLoaderDistributedBinaryGraphSerialization`,
  `TestDirectoryDistributedBinaryGraphSerialization`, `TestDistributedBinaryGraphSerialization`,
  `TestDistributedMetadataHelper`, `TestDistributedMetadataSpecification`,
  `TestDistributedStringCaching`, `TestInMemoryDistributedBinaryGraphSerialization`,
  `TestJarDistributedBinaryGraphSerialization`, `TestSimpleStringCaching`,
  `TestStringCacheOrIndex`, `TestStringCaching`)
- `runtime/serialization/TestLazyImplClassifiers.java`
- `runtime/serialization/model/TestObj.java` - deleted in Phase 7 with its subject, `Obj` (**D4**)

### 3.4 Tests - edit

| File | Change |
|---|---|
| `runtime/TestJavaStandaloneLibraryGenerator.java` | Delete `testStandaloneLibraryNoExternalDistributedMetadata` and `testStandaloneLibraryExternalExecutionDistributedMetadata`; keep the `...PeltMetadata` twins and rename them to drop the now-redundant suffix (**D7**). In Phase 7, update the generated-file regexes in `testGenerateOnly_allRepos` / `testGenerateOnly_oneRepo`, which currently allow `PureEnum_LazyImpl` and `Package_LazyImpl` (**D4**). |
| `generation/TestJavaCodeGeneration.java` | Remove the two vacuous `metadata-distributed` directory assertions (~lines 119-121, 168-170) - no code ever wrote a directory of that name under either scheme. **Keep** the `new File(classesDir, "metadata")` assertion in `testMain` (~line 501): `metadata/` *is* the distributed output directory, so that one is a real guard until Phase 2 removes the write path. Drop it then, and switch the `MetadataLazy.fromClassLoader(cl, "platform")` call in `testModularGeneration` to `MetadataPelt` in Phase 4. |
| `runtime/generation/TestJavaPackageAndImportBuilder.java` | Delete the `buildLazyImplClassNameFromUserPath` / `...FromType` / `...ReferenceFrom*` assertions (~lines 134-150, 276-292) (**D4**). |

### 3.5 Documentation

| File | Line(s) | Change |
|---|---|---|
| `docs/architecture/domain-concepts.md` | 240 | `compiled.serialization` row still names `DistributedBinaryGraphSerializer` as "binary metadata writer" - repoint at the PELT writer or drop the row |
| `docs/architecture/modules.md` | 223 | Key-classes list for `legend-pure-runtime-java-engine-compiled` names `DistributedBinaryGraphSerializer` |
| `docs/architecture/compiler-pipeline.md` | 202 | "`MetadataEager` / `MetadataLazy` - two strategies" - update to `MetadataEager` / `MetadataPelt` |
| `docs/reference/maven-plugins-usage-patterns.md` | 188-190 | Claims metadata is written to `metadata/classifiers/<repoName>/` and `metadata/bin/<repoName>/` - already wrong post-PELT |
| `docs/maintenance/maven-plugin-review-and-modernisation.md` | 75, 194 | "writes distributed binary metadata"; write-call table row for `DistributedBinaryGraphSerializer` |
| `docs/README.md` | Documentation Map | Add a row for this plan if it is kept as a durable record |

Out of repo (not part of this change, note for the `legend-engine` PR later):
`legend-engine/docs/engineering/architecture/alloy-compiler.md` lines 507 and 824 describe
startup as `MetadataLazy.fromClassLoader(...)` loading pre-compiled metadata.

---

## 4. Phased execution

Each phase is intended to be one commit that compiles and passes tests on its own.

### Phase 0 - baseline
Confirm a clean starting point.
```
mvn.cmd -T 4 clean install -DskipTests
```
(JDK 25 at `C:/Users/kmkni/.jdks/temurin-25.0.4`, Maven at
`C:/Users/kmkni/AppData/Local/Apache/apache-maven-3.9.6`.)

### Phase 1 - delete the distributed tests and the CLI tool - **DONE**
Delete everything in **3.3** except `TestObj`, plus `serialization/tool/DistributedMetadataTool.java`,
plus the `TestJavaStandaloneLibraryGenerator` / `TestJavaCodeGeneration` edits from **3.4**.
Main sources are untouched apart from the tool, so this cannot break compilation.

While renaming per **D7**, the surviving test methods collided with the private helpers they
call (`testStandaloneLibraryNoExternal`, `testStandaloneLibraryExternalExecution`); the helpers
were renamed to `assertStandaloneLibrary*` rather than leaving a confusing overload pair. The
surviving external-execution test was also moved off the 7-arg `newGenerator` overload onto the
equivalent 6-arg one, since the 7-arg form becomes a deprecated no-op in Phase 2.

Verified: module `verify -DskipTests` (0 Checkstyle violations, `dependency:analyze` clean) and
the full module suite - 1,994 tests, 0 failures, 0 errors, 13 skipped.

*Note:* this deliberately leaves `serialization/binary` untested for the two commits until
Phase 5 deletes it. The alternative - deleting each test with its subject - is equally valid
and only changes commit boundaries.

```
mvn.cmd clean install -pl legend-pure-runtime/legend-pure-runtime-java-engine-compiled -am -DskipTests
mvn.cmd surefire:test -pl legend-pure-runtime/legend-pure-runtime-java-engine-compiled
```

### Phase 2 - remove the write path - **DONE**
`JavaStandaloneLibraryGenerator` (the 5 methods + the `useLegacyMetadataForExternalAPI` field),
`ExternalClassBuilder`, `JavaSourceCodeGenerator`, `JavaCompilerEventHandler`,
`JavaModelFactoryGenerator`. Retain `@Deprecated` no-op overloads per **D5**.

The dead `generateCompilerExtensionCode` parameter was removed at the same time: three
`JavaSourceCodeGenerator` constructors accepted it and none ever stored or read it, and removing
`useLegacyMetadataForExternalAPI` was already restructuring exactly those signatures. All three
prior signatures survive as `@Deprecated` pass-throughs.

Also removed here (deferred from Phase 1): the `new File(classesDir, "metadata")` assertion in
`TestJavaCodeGeneration.testMain`, whose subject - the distributed write path - no longer exists.

After this phase the only main-code references to `serialization/binary` are
`getSourceCodeGenerator`'s `IdBuilder` factory call (Phase 3) and `MetadataLazy`'s import
(Phase 4).

Verified: module `verify -DskipTests` (0 Checkstyle violations, `dependency:analyze` clean) and
the full module suite - 1,994 tests, 0 failures, 0 errors, 13 skipped.

### Phase 3 - relocate the code-generation `IdBuilder` factory - **DONE**
`getSourceCodeGenerator` no longer mentions `DistributedBinaryGraphSerializer`. Per **D2**,
the replacement is a private static helper in `JavaStandaloneLibraryGenerator` - no new class,
no new public API - along these lines:

```java
private IdBuilder newIdBuilder(String compileGroup)
{
    ProcessorSupport processorSupport = this.runtime.getProcessorSupport();
    return (compileGroup == null) ?
           IdBuilder.newIdBuilder(processorSupport) :
           IdBuilder.newIdBuilder("$" + compileGroup + "$", processorSupport);
}
```

Per **D3** the `compileGroup == null` branch now yields a `ReferenceIdV1IdBuilder` instead of a
`LegacyIdBuilder`. This is the highest-risk phase: it changes the ids embedded in generated
Java (`getMetadata(classifier, id)`, `PureCompiledLambda` ids, `getUnit` / `getEnumeration`
lookups) for the non-modular generation path. The Maven path always supplies a compile group,
so plugin behaviour is unchanged. Run the full module test suite here, and re-run
`TestJavaCodeGeneration` and `TestJavaStandaloneLibraryGenerator` specifically.

The `$<name>$` prefix format is load-bearing downstream (`legend-engine`'s `TestIdBuilderCore`
asserts on `"$core$"`) and must not change. The `DistributedMetadataHelper.validateMetadataName`
check that the old factory performed on the compile-group name is dropped; compile groups are
repository names, already validated upstream.

In the event the behavioural delta was narrower than feared. The modular branch is byte-for-byte
equivalent - `getMetadataIdPrefix` only ever built that same `$name$` string - so the sole change
is the `compileGroup == null` branch. That branch is reached by the `modularMetadataIds=false`
overloads (tests only) **and by the externalizable-API generator in both branches** (lines ~219
and ~261), so any build with `addExternalAPI` set exercises it. Everything else in the compiled
engine reaches `JavaSourceCodeGenerator` through `JavaCompilerEventHandler` with a null
`IdBuilder`, which already defaulted to `IdBuilder.newIdBuilder(ps, false)`.

Verified: full reactor `-T 4 clean install -DskipTests` (BUILD SUCCESS, 2:50 - exercises code
generation in every module), module `verify -DskipTests` (0 Checkstyle violations,
`dependency:analyze` clean), and the full module suite - 1,994 tests, 0 failures, 0 errors,
13 skipped.

After this phase `MetadataLazy` is the only consumer of `serialization/binary` left.

### Phase 4 - remove the read path - **DONE**
`MetadataLazy`: delete `LegacyMetadataLazy`, `newMetadata(...)`, `fromClassLoader(ClassLoader)`,
and mark the class `@Deprecated` (**D6**). Repoint `PureTestBuilderCompiled` and
`TestJavaCodeGeneration.testModularGeneration` at `MetadataPelt` so that no `legend-pure` code
depends on the deprecated shim. `PureTestBuilderCompiled.buildMetadata` is `private static
Metadata`, so that repoint is internal and changes no signature.

`MetadataLazy` is now 367 lines lighter and delegates entirely to `MetadataPelt`.
`serialization/binary` has **no consumers at all** - Phase 5 can delete it wholesale. The only
remaining `MetadataLazy` references are the `_LazyImpl` machinery removed in Phase 7:
`AbstractLazyReflectiveCoreInstance`, and the generated-code strings in `EnumProcessor` and
`ClassLazyImplProcessor`.

Verified: module `verify -DskipTests` (0 Checkstyle violations, `dependency:analyze` clean) and
the full module suite - 1,994 tests, 0 failures, 0 errors, 13 skipped.

### Phase 5 - delete `serialization/binary` and the Jackson dependencies - **DONE**
Delete all 29 files; remove the three Jackson dependencies from the module `pom.xml`
(finding 6). `mvn verify` here specifically to exercise `dependency:analyze-only` and Checkstyle.

No `Distributed` identifier remains anywhere in `legend-pure` Java source after this phase.

Verified: module `verify -DskipTests` (0 Checkstyle violations, and `dependency:analyze-only`
green with `failOnWarning=true`, which is what confirms the Jackson removal was both necessary
and complete); full reactor `-T 4 clean install -DskipTests` (BUILD SUCCESS, 2:56 - confirms no
other module was picking up Jackson transitively through this one); and the full module suite -
1,994 tests, 0 failures, 0 errors, 13 skipped.

### Phase 6 - retire `LegacyIdBuilder` (per **D3**) - **DONE**
Delete `LegacyIdBuilder`, `IdBuilder.LegacyBuilder` and `IdBuilder.legacyBuilder`. Nothing
references them after Phase 3.

Two things surfaced that were **not** in the original inventory:

1. **`CompiledExtension.getExtraIdBuilders` is now a dead extension point.** `LegacyIdBuilder`
   was its only consumer (`LegacyIdBuilder:181`). The default method on `CompiledExtension` and
   the one override, `PathExtensionCompiled.getExtraIdBuilders` (which registered an id function
   for `Path`), were never called again. Resolved by **D8**: the default method is deprecated and
   the override deleted.
2. `IdBuilder.AbstractBuilder` is now a redundant layer with a single subclass, `Builder`. It is
   package-private, so collapsing it is a free but purely cosmetic refactor, out of scope here.

Verified: module `verify -DskipTests` (0 Checkstyle violations, `dependency:analyze` clean),
full reactor `-T 4 clean install -DskipTests` (BUILD SUCCESS, 2:14), and the full module suite.

### Phase 7 - retire `_LazyImpl` code generation (per **D4**)
`ClassLazyImplProcessor`, `AbstractLazyReflectiveCoreInstance` (and, optionally, the
now-orphaned `PersistentReflectiveCoreInstance`), `serialization/model/`,
`GraphSerializer.buildObj` + `ClassifierCaches`, `EnumProcessor.ENUM_LAZY_CLASS_NAME`,
`JavaPackageAndImportBuilder.buildLazyImpl*`, the `ClassProcessor.processClass` call site,
`MetadataLazy.valueToObject` / `valuesToObjects`, the generated import block in
`JavaSourceCodeGenerator`, `TestObj`, and the `TestJavaPackageAndImportBuilder` /
`TestJavaStandaloneLibraryGenerator` regex updates.

Sequencing within the phase matters: remove the *generation* of `_LazyImpl` sources and the
`serialization.model.*` line from the generated import block **before or in the same commit as**
the deletion of `serialization/model`, or every generated file will fail to compile.

This removes one generated class per platform class, so the generated-source footprint of every
downstream build shrinks - worth calling out in the PR description. Verify by regenerating and
diffing the file list under `target/generated-*-sources/`.

### Phase 8 - documentation
Apply **3.5**.

### Phase 9 - full verification
```
mvn.cmd -T 4 clean install
```
Then, as a separate later exercise (explicitly deferred): build `legend-engine` against the
new snapshot.

---

## 5. Verification notes

- **Always `clean`** for `install`/`verify`; `surefire:test` may be run without cleaning.
- Up to 4 threads (`-T 4`); a no-test clean install takes 2-3 minutes.
- The two build gates that will bite are **Checkstyle** (unused imports, header, brace style)
  and **`dependency:analyze-only` with `failOnWarning=true`** at `test-compile`.
- Focused suites for the risky phases:
  `TestJavaCodeGeneration`, `TestJavaStandaloneLibraryGenerator`, `TestMetadataPelt`,
  `TestGraphIsSerialized`, `TestJavaPackageAndImportBuilder`, `TestPureCompiledJarMojo`.
  Remember `-DfailIfNoTests=false` when using `-Dtest=` across modules.
- `legend-engine` is validated by **text search only** in this plan; no `legend-engine` build.

---

## 6. Explicitly out of scope

- `JavaCodeGeneration.GenerationType` (`monolithic` / `modular`) and the
  `PureCompiledJarMojo` `generationType` parameter - both branches already serialize PELT, and
  merging them is separate future work (finding 3).
- `PureCompiledJarMojo.generateMetadata` and the already-`@Deprecated` `useSingleDir` parameter.
- `MetadataEager` / `MetadataBuilder` / `MetadataEagerCompilerEventHandler` - already migrated
  off distributed metadata; their `IdBuilder`-taking overloads are already `@Deprecated`.
- `PreCompiledPureGraphCache` (in the `serialization` package but about PAR caches).
- PAR generation (`legend-pure-maven-generation-par`) and the M4 binary serialization
  primitives in `legend-pure-m4`.
- Any change to `legend-engine`.

---

## 7. Decisions (resolved)

| # | Question | Resolution |
|---|---|---|
| **D1** | Keep `MetadataLazy`? | **Keep**, as a PELT-only compatibility shim for downstream consumers, but fully deprecated (**D6**). Its `valueToObject` / `valuesToObjects` members go with **D4**. |
| **D2** | Where does the code-generation id prefix live once `DistributedBinaryGraphSerializer.newIdBuilder` is gone? | **No new class, and not public API.** Keep it private and as close to the single use site as possible: a private helper on `JavaStandaloneLibraryGenerator` that builds `"$" + compileGroup + "$"` and calls the existing `IdBuilder.newIdBuilder(prefix, processorSupport)`. The `$<name>$` format itself must not change. The old `validateMetadataName` check on the compile-group name is dropped. |
| **D3** | `getSourceCodeGenerator(null, ...)` currently yields a `LegacyIdBuilder`. What replaces it? | **Switch to `IdBuilder.newIdBuilder(processorSupport)` (ReferenceIdV1) and delete `LegacyIdBuilder`.** There is no longer any reason to generate Java code with legacy ids. |
| **D4** | Delete `_LazyImpl` code generation, `serialization/model`, `AbstractLazyReflectiveCoreInstance`, `GraphSerializer.buildObj` / `ClassifierCaches`, and `MetadataLazy.valueToObject`? | **Yes, in this change** (Phase 7), not deferred to a follow-up PR. |
| **D5** | Retain no-op `@Deprecated` overloads for the removed boolean flags? | **Yes** - `newGenerator(..., useLegacyMetadataForExternalAPI, ...)` and the wide `JavaSourceCodeGenerator` constructors stay as deprecated pass-throughs. Javadoc should say they are retained temporarily, without naming a removal release. |
| **D6** | Mark the whole of `MetadataLazy` `@Deprecated`, or only the removed members? | **Whole class** - it is entirely replaceable by `MetadataPelt`. Internal call sites move to `MetadataPelt` so no `legend-pure` code triggers the deprecation warning. |
| **D7** | Rename the surviving `...PeltMetadata` test methods? | **Yes** - drop the now-redundant suffix. |

| **D8** | `CompiledExtension.getExtraIdBuilders` became a dead extension point in Phase 6 - `LegacyIdBuilder` was its only consumer. Its sole override, `PathExtensionCompiled.getExtraIdBuilders`, registers an id function for `Path` that is now never invoked. What should happen to it? | **Deprecate `CompiledExtension.getExtraIdBuilders`** (javadoc: no longer consulted, implementing it has no effect, retained temporarily) **and delete `PathExtensionCompiled.getExtraIdBuilders` along with its `buildIdForPath` helper.** This is dead code, not a behaviour gap: the extension point was only consulted when a `LegacyIdBuilder` was constructed, which happened solely for the null-metadata-name case, so every modular build - including all 142 `legend-engine` modules - already ignored it. Reference id v1 assigns ids by generic graph-path traversal, so `Path` needs no special-casing. |

---

## 8. Relationship to the earlier partial attempt

Branch `backup/remove_compiled_metadata` (5 commits on top of `master`) already covers,
in substance, **Phase 1** and **Phase 2** of this plan, and takes the `@Deprecated`-shim
approach agreed in **D5**/**D6**. It does **not** cover the `IdBuilder` relocation
(Phase 3), the deletion of `serialization/binary` and the Jackson dependencies (Phases 4-5),
`_LazyImpl` (Phase 7), or the documentation updates (Phase 8). It also usefully removed the
already-dead `generateCompilerExtensionCode` parameter from `JavaSourceCodeGenerator`.
