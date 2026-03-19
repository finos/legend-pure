# Module README Template

Copy this template to `<module-directory>/README.md` and fill in each section.
Delete sections that are not applicable (e.g. if the module has no ANTLR4 grammars).

---

## `<artifact-id>`

> **Maven coordinates:** `org.finos.legend.pure:<artifact-id>:<version>`
> **Parent module:** `<parent-artifact-id>`
> **Packaging:** `jar` | `maven-plugin` | `pom`

## Purpose

*One to three sentences describing what this module does and why it exists.*

**Key responsibility:** *Single sentence.*

---

## Dependencies on Other Legend Pure Modules

| Dependency | Scope | Why |
|-----------|-------|-----|
| `legend-pure-m4` | compile | *reason* |
| `legend-pure-m3-core` | compile | *reason* |
| *(add rows as needed)* | | |

---

## Key Third-Party Dependencies

| Library | Version | Usage in this module |
|---------|---------|---------------------|
| `eclipse-collections` | `${eclipsecollections.version}` | *e.g. MutableList for internal graphs* |
| *(add rows as needed)* | | |

---

## Build Customizations

*Describe any non-standard Maven build steps in this module's `pom.xml`.*

| Plugin | Phase | Goal | Purpose |
|--------|-------|------|---------|
| `antlr4-maven-plugin` | `generate-sources` | `antlr4` | *e.g. Generate M3 parser from M3.g4* |
| *(add rows as needed)* | | | |

If the module has a multi-pass compilation (e.g. two `maven-compiler-plugin`
executions), explain the ordering here:

> *e.g. First compile runs at `compile` phase to produce base classes. The
> `legend-pure-maven-generation-platform-java` plugin then generates `CoreInstance`
> accessors. The second compile runs at `process-classes` to compile those.*

---

## Key Classes / Entry Points

| Class | Package | Role |
|-------|---------|------|
| `MyMainClass` | `org.finos.legend.pure.xxx` | *e.g. Top-level compiler entry point* |
| *(add rows as needed)* | | |

---

## ANTLR4 Grammars *(if applicable)*

| Grammar file | Location | Generated classes |
|-------------|----------|------------------|
| `MyGrammar.g4` | `src/main/antlr4/...` | `MyGrammarLexer`, `MyGrammarParser`, `MyGrammarVisitor` |

---

## Pure Source / Definition Files *(if applicable)*

| File / directory | Purpose |
|-----------------|---------|
| `src/main/resources/platform/...` | *e.g. Pure standard library source files* |
| `*.definition.json` | *Repository descriptor — lists repositories and their dependencies* |

---

## Running Tests

```bash
# All tests in this module
mvn test -pl <relative-path-from-root>

# A single test class
mvn test -pl <relative-path-from-root> -Dtest=<TestClassName> -DfailIfNoTests=false
```

### Test Coverage Notes

*Describe what is and is not covered by tests, any known gaps, and any tricky
setup required (e.g. "Integration tests require H2 in embedded mode — no setup
needed").*

---

## Known Issues / Gotchas

*List any non-obvious behaviour, known limitations, or common mistakes.*

- *e.g. "The `process-classes` phase re-runs the compiler; do not add the
  `default-compile` execution to the `compile` phase or you will get duplicate class errors."*

---

## Related Documentation

- [Architecture Overview](../../docs/architecture/overview.md)
- [Module Reference](../../docs/architecture/modules.md)
- *Link to any design docs or ADRs relevant to this module.*

---

*Template version: 1.0 — March 2026*
*See [Module README Template guidance](../../docs/templates/module-readme-template.md)*
