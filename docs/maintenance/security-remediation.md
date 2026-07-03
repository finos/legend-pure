# Dependency Vulnerability Remediation — finos/legend-pure

Tracking document for burning down known-vulnerable dependency versions. All findings
derive from public security advisories (GHSA/CVE) cross-referenced against this repo's
`pom.xml` and actual code usage — the same analysis anyone can reproduce from the
public advisory databases. Contributions welcome: pick an unclaimed unit, follow its
*How*/*Verify* sections, and update the status tracker in the same PR.

- **Created:** 2026-07-03 (all version/alert facts verified as of this date)
- **Scope:** all open advisories of severity **high + medium** against managed
  dependency versions (24 findings: 10 high, 14 medium; no critical). Low severity is
  out of scope.
- **Workflow:** one unit per PR, branched off `master`. Make the edit, run the unit's
  *Verify* commands, tick the tracker checkbox in the same PR. Findings only clear for
  downstream consumers when a **release** ships the fix; a unit is "done" here when
  its PR is merged.

## Status tracker

| Unit | What | Alerts | Status |
|---|---|---|---|
| 1 | Delete dead deps (bouncycastle ×2, commons-httpclient) | #86 (H), #64 #71 #74 #76 #77 #85 (M) | ☐ |
| 2 | commons-io 2.7 → 2.16.1 | #80 (H) | ☐ |
| 3 | classgraph 4.8.25 → 4.8.184 | #78 (M) | ☐ |
| 4 | commons-lang3 3.5 → 3.18.0 | #83 (M) | ☐ |
| 5 | checkstyle 8.25 → 9.3 | #57 (M) | ☐ |
| 6 | jackson family 2.10.5 → 2.18.8 | #22 #52 #54 #82 #87 #89 #90 (H), #81 #84 #88 #91 (M) | ☐ |
| 7 | h2 2.1.214 → 2.2.224 (test scope) | #79 (H) | ☐ |
| — | **BLOCKED**: jackson #92 (CVE-2026-54515) — no released 2.x fix | #92 (M) | ⏳ watch |

Order = ascending blast radius. Units 1–5 are independent of each other; do them in any
order if needed, but 6 before 7 keeps the riskiest change last.

## Ground rules (read before starting any unit)

1. **Re-pull the alert list first** — alerts may have been fixed, dismissed, or added
   since 2026-07-03 (requires Dependabot-alert read access on the repo; contributors
   without it can work from this doc and the public GHSA/CVE advisories, or ask a
   maintainer to refresh the table):
   ```bash
   gh api 'repos/finos/legend-pure/dependabot/alerts?state=open&severity=critical,high,medium&per_page=100' --paginate \
     | jq -r '.[] | [.number,.security_advisory.severity,.dependency.package.name,.security_advisory.cve_id,(.security_vulnerability.first_patched_version.identifier // "NONE")] | @tsv'
   ```
2. **Re-verify the target version is still the right one** on Maven Central
   (`https://repo1.maven.org/maven2/<group-path>/<artifact>/maven-metadata.xml`).
   In particular check whether **jackson-databind 2.18.9** has shipped — it unblocks
   alert #92 (see Blocked log).
3. All edits are in the **root `pom.xml`** unless stated otherwise. Line numbers below
   are as of commit `04477b4e6`; re-locate by content if the file has drifted.
4. Default verification recipe is `mvn clean install` (JDK 11 or 17,
   `MAVEN_OPTS=-Xmx4g`). PCT runs on both engines during a full build — that is the
   cross-engine regression net for all runtime-visible changes.

## Downstream / transitive context (applies to every unit)

- **legend-engine pins the exact same vulnerable versions** (jackson 2.10.5 / 2.10.5.1,
  h2 2.1.214, commons-io 2.7 — verified in finos/legend-engine master pom on
  2026-07-03). Engine's own `dependencyManagement` overrides legend-pure's transitive
  pins at engine build time, so bumping here does **not** break engine builds. The one
  hard constraint: **legend-pure source must keep compiling against jackson-2.10-era
  APIs** (it currently does — `JsonMapper`, `StreamReadFeature`, `StreamWriteFeature`,
  `ObjectReader/Writer` all exist in 2.10) until engine bumps too, because engine will
  run legend-pure's classes against its own older jackson.
- All 24 alerts point at the **root pom** — there are no vulnerable versions introduced
  by module poms; every module inherits via `dependencyManagement`, so a single
  property/entry edit fixes all transitive pinning at once per unit.
- **Exploitability reality-check:** none of these is an actively exploitable hole in
  legend-pure itself. Jackson only deserializes legend-pure's own classpath PCT
  manifests/reports (not attacker input); h2 is test-scope only; checkstyle is
  build-time only; the bouncycastle/httpclient entries are dead. The drivers are
  hygiene, alert burn-down, and not shipping vulnerable version pins downstream.

---

## Unit 1 — Delete dead dependencyManagement entries (bouncycastle, commons-httpclient)

**Why.** 7 alerts, none fixable by upgrade on the current coordinates:
- `org.bouncycastle:bcpg-jdk15on` 1.67 — #86 (high, CVE-2026-3505, uncontrolled
  resource consumption). The `-jdk15on` line ended at 1.70 with no patch; fixes only
  exist on `-jdk18on`.
- `org.bouncycastle:bcprov-jdk15on` 1.67 — #64 (CVE-2023-33201), #74 (CVE-2024-29857),
  #76 (CVE-2024-30171), #77 (CVE-2023-33202), #85 (CVE-2024-34447), all medium; fixes
  are at 1.70/1.78 which on this artifact line means migrating to `-jdk18on`.
- `commons-httpclient:commons-httpclient` 3.1 — #71 (medium, CVE-2012-5783, no SSL
  hostname verification). 3.x is EOL with no fix; the successor
  `org.apache.httpcomponents:httpclient` 4.5.13 is *already* managed in this pom.

**Blast radius: ZERO.** All three exist **only** in root `dependencyManagement` — no
module `pom.xml` declares any of them, and there is not a single
`org.bouncycastle.*` or `org.apache.commons.httpclient.*` import in the repo (verified
2026-07-03). `dependencyManagement`-only entries never reach any classpath. The
bouncycastle block even sits under a `<!-- To Review -->` comment. Deletion, not
migration, is the fix.

**How.** In root `pom.xml`:
1. Delete the bouncycastle block (lines ~869–880: the `bcpg-jdk15on` and
   `bcprov-jdk15on` dependencies plus the surrounding `<!-- To Review -->` comments).
2. Delete the `<bouncycastle.version>1.67</bouncycastle.version>` property (line ~64).
3. Delete the `commons-httpclient` dependency entry incl. its commons-logging
   exclusion (lines ~789–799).

**Verify.**
```bash
mvn clean install -DskipTests   # compile proof nothing referenced them
mvn clean install               # full build + tests
grep -rn 'bouncycastle\|commons-httpclient' --include=pom.xml . | grep -v target  # expect empty
```

---

## Unit 2 — commons-io 2.7 → 2.16.1

**Why.** #80 (high, CVE-2024-47554, CVSS 7.5): `XmlStreamReader` DoS on untrusted
input. Fixed ≥ 2.14.0. legend-pure doesn't use `XmlStreamReader`, so real exposure is
nil — this is hygiene + downstream pin.

**Blast radius.** Compile-scope in `legend-pure-m3-core` only. Exactly two usage sites:
- `legend-pure-core/legend-pure-m3-core/src/main/java/org/finos/legend/pure/m3/execution/XLSXOutputWriter.java` — `IOUtils` (stable API).
- `legend-pure-core/legend-pure-m3-core/src/test/java/org/finos/legend/pure/m3/tests/AbstractPureTestWithCoreCompiled.java` — `NullPrintStream` (constructor deprecated since 2.12 in favor of `NullPrintStream.INSTANCE`, but still present — compiles with a deprecation warning; optionally switch to the constant).

Target 2.16.1 (Java 8 bytecode, well-soaked). Latest is 2.22.0 — fine too, but confirm
Java 8 compatibility before choosing it (`maven.compiler.release=8` and downstream
consumers may run Java 8).

**How.** Root `pom.xml` line ~67: `<commons-io.version>2.7</commons-io.version>` → `2.16.1`.

**Verify.**
```bash
mvn clean install -pl legend-pure-core/legend-pure-m3-core -am
mvn clean install
```

---

## Unit 3 — classgraph 4.8.25 → 4.8.184

**Why.** #78 (medium, CVE-2021-47621, XXE in classgraph's module/classpath XML
handling). Fixed ≥ 4.8.112.

**Blast radius.** One consumer:
`legend-pure-runtime/legend-pure-runtime-java-engine-compiled/src/main/java/org/finos/legend/pure/runtime/java/compiled/compiler/PureJavaCompiler.java`.
Same 4.8.x line (latest 4.8.184 as of 2026-07-03); classgraph keeps API stability
within 4.8.x. Low risk.

**How.** Root `pom.xml` line ~921: classgraph `<version>4.8.25</version>` → `4.8.184`
(inline version, no property).

**Verify.**
```bash
mvn clean install -pl legend-pure-runtime/legend-pure-runtime-java-engine-compiled -am
mvn clean install
```

---

## Unit 4 — commons-lang3 3.5 → 3.18.0

**Why.** #83 (medium, CVE-2025-48924, `ClassUtils.getClass(...)` StackOverflowError on
crafted very-long input). Fixed ≥ 3.18.0. Note `ClassUtils` IS imported in one file, so
this one is at least theoretically on a code path.

**Blast radius.** 13 files across 4 modules (m4, m3-core, engine-compiled,
engine-interpreted). Imports in play: `StringUtils` (6×), `StringEscapeUtils` (3×),
`text.translate.*` (7 imports — `AggregateTranslator`, `LookupTranslator`,
`EntityArrays`, `JavaUnicodeEscaper`, `OctalUnescaper`, `UnicodeUnescaper`,
`CharSequenceTranslator`), `DurationFormatUtils`, `ExceptionUtils`, `ClassUtils`.

Key compatibility fact: `StringEscapeUtils` and the whole `lang3.text.translate`
package were **deprecated in 3.6 but are still present in 3.18** (removal is slated for
4.0 only). The bump is compile-safe; expect deprecation warnings, which the build does
not treat as errors. Optional follow-up (separate PR, not this unit): migrate those
call sites to `org.apache.commons:commons-text`, which is already managed at 1.10.0 in
this pom.

3.18.0 is the CVE floor and keeps the delta minimal. 3.20.0 is latest — verify its Java
baseline is still 8 before preferring it.

**How.** Root `pom.xml` line ~68: `<commons-lang.version>3.5</commons-lang.version>` → `3.18.0`.

**Verify.**
```bash
mvn clean install    # touches 4 modules — just run the full build
```

---

## Unit 5 — checkstyle 8.25 → 9.3 (build-time only)

**Why.** #57 (medium, CVE-2019-10782, XXE in checkstyle's config/import handling).
Fixed ≥ 8.29. Checkstyle runs only inside the Maven build JVM and is never shipped in
any artifact — zero runtime exposure; this closes the alert and modernizes the linter.

**Blast radius.** Build-only. The risk is behavioral: checkstyle 8.25 → 9.3 adds/changes
checks, and **checkstyle failures fail the build at `verify`**. Expect possibly a
handful of new violations to fix in code or to adjust in the checkstyle config. 9.3 is
the last 9.x (runs fine on the build JDKs 11/17) and is the default bundled line for
maven-checkstyle-plugin 3.6.0 (already in use here, line ~92), which minimizes plugin
compatibility risk vs jumping to 10.x/11.x.

**How.** Root `pom.xml` line ~288 (inside the `maven-checkstyle-plugin` block):
`com.puppycrawl.tools:checkstyle` `<version>8.25</version>` → `9.3`.

**Verify.**
```bash
mvn checkstyle:check          # fast signal on new violations
mvn verify -DskipTests        # full checkstyle gate
mvn clean install             # final
```

---

## Unit 6 — jackson family 2.10.5 / 2.10.5.1 → 2.18.8 (lockstep)

**Why.** 11 alerts on jackson-core + jackson-databind; the max fix floor across the
released ones is **2.18.8**:

| Alert | Sev | CVE | Nature | Fixed |
|---|---|---|---|---|
| #22 | H | CVE-2020-36518 | deeply nested JSON DoS | 2.12.6.1 |
| #52 | H | CVE-2021-46877 | JsonNode JDK-serialization DoS | 2.12.6 |
| #54 | H | CVE-2022-42003 | resource exhaustion (UNWRAP_SINGLE_VALUE_ARRAYS) | 2.12.7.1 |
| #87 | H | CVE-2022-42004 | resource exhaustion (BeanDeserializer) | 2.12.7.1 |
| #82 | H | CVE-2025-52999 | jackson-core deep-nesting StackOverflow | 2.15.0 |
| #89 | H | CVE-2026-54512 | PolymorphicTypeValidator bypass via generics (8.1) | 2.18.8 |
| #90 | H | CVE-2026-54513 | PTV `allowIfSubTypeIsArray` bypass (8.1) | 2.18.8 |
| #81 | M | CVE-2025-49128 | jackson-core memory disclosure | 2.13.0 |
| #84 | M | GHSA-72hv-8253-57qq | async parser number-length bypass | 2.18.6 |
| #88 | M | CVE-2026-50193 | nested JsonNode.toString StackOverflow | 2.14.0 |
| #91 | M | CVE-2026-54514 | InetSocketAddress eager DNS (SSRF) | 2.18.8 |

Real exposure is low: legend-pure uses jackson only for its **own** PCT
manifests/report JSON on the classpath (not attacker-controlled), and does not use
`PolymorphicTypeValidator`/default typing at all — the one polymorphic spot is a
closed `@JsonTypeInfo(use = Id.NAME)` + `@JsonSubTypes` set in
`m3-core .../pct/reports/model/Adapter.java`, which is the safe pattern.

**Blast radius.** 8 files, all internal serialization plumbing:
- m3-core: `pct/functions/generation/FunctionsGeneration.java`,
  `pct/reports/generation/ReportGeneration.java`, `pct/reports/model/Adapter.java`,
  `pct/reports/model/TestInfo.java`, `pct/shared/PCTManifestLoader.java`,
  `pct/shared/provider/PCTReportProviderTool.java`,
  `serialization/runtime/SourceCoordinates.java`
- engine-compiled: `serialization/binary/DistributedMetadataSpecification.java`

Transitive coupling — this is the one unit where artifacts MUST move together:
`jackson-databind 2.18.8` requires `jackson-core`/`jackson-annotations` 2.18.x, and
`jackson-dataformat-xml`/`-yaml` are managed off the same `${jackson.version}`
property (no module currently consumes the dataformats — they're inert but should stay
version-aligned). All five artifacts verified to exist at 2.18.8 on Maven Central.
Jackson 2.18 is still Java 8 bytecode ✓.

Downstream constraint (see context section): don't introduce any post-2.10 jackson API
into legend-pure source in this unit — engine runs these classes against 2.10.5 until
it upgrades.

Behavioral watch-items 2.10 → 2.18 (all low-probability for plain POJO round-trips but
check the PCT reports): stricter coercion defaults, `Nulls`/absent handling tweaks,
map/property ordering is unchanged for `@JsonProperty`-annotated POJOs.

**How.** Root `pom.xml`:
- line ~76: `<jackson.version>2.10.5</jackson.version>` → `2.18.8`
- line ~77: `<jackson.databind.version>2.10.5.1</jackson.databind.version>` → `2.18.8`
  (optionally collapse to one property since they're now equal — cosmetic).

**Verify.**
```bash
mvn clean install     # PCT on both engines = the serialization regression test
```
Then spot-check: diff a generated PCT report JSON (under target/ of a PCT-running
module) against one generated on master — should be byte-identical or
whitespace-equivalent.

---

## Unit 7 — h2 2.1.214 → 2.2.224 (test scope; do LAST)

**Why.** #79 (high, CVE-2022-45868, CVSS 7.8): H2 web-console password exposure via
process listing — requires **local access** to the machine running H2. Fixed ≥ 2.2.220.
H2 here is test-only, so real exposure is ~nil; this is alert burn-down.

**Blast radius.** `test` scope only, in:
- `legend-pure-store/legend-pure-store-relational/legend-pure-runtime-java-extension-compiled-store-relational`
- `legend-pure-store/legend-pure-store-relational/legend-pure-runtime-java-extension-interpreted-store-relational`

(`TestDatabaseConnect.java` in the shared module only does
`Class.forName("org.h2.Driver")` — a string, no compile dependency.) Because it's test
scope, **nothing propagates downstream** — legend-engine's h2 2.1.214 pin is entirely
its own.

Why riskiest anyway: H2 2.1 → 2.2 changed SQL semantics (stricter type handling,
changed metadata/result behaviors), and the relational PCT suites assert exact results
on BOTH engines. Expect possible test failures that need fixture/expected-value
updates. Target 2.2.224 (last 2.2.x, Java 8 baseline) — **not** 2.3.x, which requires a
Java 11 runtime and would break the Java-8 compatibility posture even for tests run by
downstream forks on 8.

**How.** Root `pom.xml` line ~74: `<h2.version>2.1.214</h2.version>` → `2.2.224`.

**Verify.**
```bash
mvn clean install -pl legend-pure-store/legend-pure-store-relational -am -Dsurefire.failIfNoSpecifiedTests=false
mvn clean install
```
**Fallback:** if H2 2.2 semantics break relational tests broadly (not a handful of
fixable fixtures), STOP: revert the bump, and record the failure details in the
Deferred log below with the rationale — test-only scope + local-access-only CVE =
acceptable residual risk until a coordinated Legend-stack H2 upgrade.

---

## Deferred / blocked log

| Item | Status | Rationale / unblock condition |
|---|---|---|
| **jackson #92** — CVE-2026-54515 (medium): case-insensitive deserialization bypasses per-property `@JsonIgnoreProperties` | ⏳ **BLOCKED upstream** as of 2026-07-03 | No released fix on ANY `com.fasterxml` 2.x line (fix boundaries 2.18.9 / 2.21.5 / 2.22.1 are all unreleased; only jackson 3.x `tools.jackson` 3.1.4 has one). Not exploitable here: repo uses no `@JsonIgnoreProperties` and deserializes only its own classpath resources. **Unblock:** jackson-databind 2.18.9 on Maven Central → bump both jackson properties 2.18.8 → 2.18.9 (trivial follow-up to Unit 6). Check before starting any unit. |
| bcpg-jdk15on / bcprov-jdk15on / commons-httpclient | ✅ resolved by design in Unit 1 | No patched version exists on these artifact lines (jdk15on abandoned at 1.70; httpclient 3.x EOL). Upgrade is impossible; removal is correct because they are provably unused (no module declaration, no imports). |
| h2 (only if Unit 7 fallback triggers) | — | (fill in failure details if the H2 2.2 upgrade is reverted) |

## Alert cross-reference (all 24, as of 2026-07-03)

Unit 1: #86, #64, #71, #74, #76, #77, #85 · Unit 2: #80 · Unit 3: #78 · Unit 4: #83 ·
Unit 5: #57 · Unit 6: #22, #52, #54, #82, #87, #89, #90, #81, #84, #88, #91 ·
Unit 7: #79 · Blocked: #92
