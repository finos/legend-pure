# Phased Documentation Plan

> **Status:** Execution complete (initial set delivered March 2026)
> **Team capacity assumption:** 1–2 engineers, ~20% time (≈ 1 day/week per engineer)

This document records the phased plan used to create this documentation set. It also
serves as the roadmap for future documentation work.

---

## Phase 1 — Foundation (Weeks 1–2) ✅ COMPLETE

**Goal:** Unblock new engineers immediately. Prioritize high-impact, low-effort docs.

**Deliverables:**

| Deliverable | File | Status |
|-------------|------|--------|
| `/docs` directory structure | `docs/` | ✅ |
| Top-level documentation index | `docs/README.md` | ✅ |
| Architecture Overview (module tree, component diagram, build lifecycle) | `docs/architecture/overview.md` | ✅ |
| Getting Started Guide (prerequisites, clone, build, IDE, troubleshooting) | `docs/guides/getting-started.md` | ✅ |
| Module Reference (every module purpose + inter-module deps) | `docs/architecture/modules.md` | ✅ |

**Activities:**

- Review root `pom.xml` (modules, properties, dependencyManagement, plugin config).
- Trace the Maven build lifecycle through each phase.
- Document the module reactor order and naming conventions.
- Validate the Getting Started steps on a clean machine.

**Tools used:**

- `mvn dependency:tree`
- `mvn install -DskipTests` (trace output)
- Read all top-level and per-module `pom.xml` files.

---

## Phase 2 — Technology & Standards (Weeks 3–4) ✅ COMPLETE

**Goal:** Give engineers the context to write correct code from day one.

**Deliverables:**

| Deliverable | File | Status |
|-------------|------|--------|
| Technology stack inventory | `docs/architecture/tech-stack.md` | ✅ |
| Domain & Key Concepts (metamodel layers, glossary, design patterns) | `docs/architecture/domain-concepts.md` | ✅ |
| Coding Standards & Style Guide | `docs/standards/coding-standards.md` | ✅ |
| Testing Strategy | `docs/testing/testing-strategy.md` | ✅ |

**Activities:**

- Inventory all dependencies from `<dependencyManagement>` in root POM.
- Document version management strategy.
- Read `checkstyle.xml` and translate rules into human-readable standards.
- Review existing test classes to document conventions (naming, structure, patterns).
- Document PCT test framework.

---

## Phase 3 — Process & Exploration (Weeks 5–6) ✅ COMPLETE

**Goal:** Enable self-directed exploration and establish governance.

**Deliverables:**

| Deliverable | File | Status |
|-------------|------|--------|
| Build & CI Guide (pipeline, JaCoCo, SonarCloud) | `docs/guides/build-and-ci.md` | ✅ |
| Codebase Exploration Guide | `docs/guides/exploration.md` | ✅ |
| Documentation Maintenance plan | `docs/maintenance/maintenance.md` | ✅ |
| Module README template | `docs/templates/module-readme-template.md` | ✅ |
| This phased plan | `docs/maintenance/documentation-plan.md` | ✅ |
| Contributor Workflow Guide (new DSL, native function, store connector) | `docs/guides/contributor-workflow.md` | ✅ |
| Milestoning concept + Key Java packages map | `docs/architecture/domain-concepts.md` | ✅ |
| Pure Language Reference | `docs/reference/pure-language-reference.md` | ✅ |
| Compiler Pipeline deep-dive | `docs/architecture/compiler-pipeline.md` | ✅ |

**Activities:**

- Read `.github/workflows/build.yml` in detail.
- Document the GitHub Actions pipeline steps and Sonar integration.
- Draft tribal knowledge interview questions.
- Define CODEOWNERS integration approach.

---

## Phase 4 — Per-Module READMEs (Weeks 7–10) 🔲 TODO

**Goal:** Every module has a standardized README that describes its purpose,
build customizations, and key classes.

**Deliverables:**

| Module | README file | Status |
|--------|------------|--------|
| `legend-pure-m4` | `legend-pure-core/legend-pure-m4/README.md` | ✅ |
| `legend-pure-m3-bootstrap-generator` | `legend-pure-core/legend-pure-m3-bootstrap-generator/README.md` | 🔲 |
| `legend-pure-m3-core` | `legend-pure-core/legend-pure-m3-core/README.md` | 🔲 |
| `legend-pure-m3-precisePrimitives` | `legend-pure-core/legend-pure-m3-precisePrimitives/README.md` | 🔲 |
| `legend-pure-dsl-diagram` | `legend-pure-dsl/legend-pure-dsl-diagram/README.md` | 🔲 |
| `legend-pure-dsl-graph` | `legend-pure-dsl/legend-pure-dsl-graph/README.md` | 🔲 |
| `legend-pure-dsl-mapping` | `legend-pure-dsl/legend-pure-dsl-mapping/README.md` | 🔲 |
| `legend-pure-dsl-path` | `legend-pure-dsl/legend-pure-dsl-path/README.md` | 🔲 |
| `legend-pure-dsl-store` | `legend-pure-dsl/legend-pure-dsl-store/README.md` | 🔲 |
| `legend-pure-dsl-tds` | `legend-pure-dsl/legend-pure-dsl-tds/README.md` | 🔲 |
| `legend-pure-maven-*` (all 6) | `legend-pure-maven/<module>/README.md` | 🔲 |
| `legend-pure-runtime-*` (all 3) | `legend-pure-runtime/<module>/README.md` | 🔲 |
| `legend-pure-store-relational` | `legend-pure-store/legend-pure-store-relational/README.md` | 🔲 |

**Approach:**

- Use the [Module README Template](../templates/module-readme-template.md).
- Prioritize modules with the most active development first.
- Each README can be written by the module owner or a new engineer as an
  onboarding exercise.

**Estimated effort:** 0.5–1 hour per module × ~20 modules = 10–20 hours total.

---

## Phase 5 — Architecture Decision Records (Week 10+) 🔲 TODO

**Goal:** Capture the "why" behind key design decisions to prevent churn.

**Suggested initial ADRs:**

| ADR | Key decision | Status |
|-----|-------------|--------|
| ADR-001 | JUnit 4 over JUnit 5 | ✅ |
| ADR-002 | Eclipse Collections as primary collection library | ✅ |
| ADR-003 | No mocking framework (hand-written stubs) | ✅ |
| ADR-004 | Compiled mode vs Interpreted mode — when to use each | 🔲 |
| ADR-005 | PAR format vs binary element format — coexistence strategy | 🔲 |
| ADR-006 | ANTLR4 `treatWarningsAsErrors=true` policy | 🔲 |

---

## Phase 6 — Hosted Documentation Portal (Future / Optional)

**Goal:** Make the docs searchable and navigable outside the IDE.

### Option A: MkDocs + Material

```bash
pip install mkdocs mkdocs-material
mkdocs serve   # live preview at http://localhost:8000
mkdocs build   # generates /site directory
```

Add a `mkdocs.yml` at the project root and a GitHub Actions step to deploy to
GitHub Pages on every `master` merge.

**Option B: Docusaurus** — React-based; better for larger doc sites with versioning.

**Recommendation:** Proceed with MkDocs + Material when the per-module README phase
is complete, so there is enough content to justify the portal.

---

## Effort Summary

| Phase | Weeks | Engineers | Estimated hours |
|-------|-------|-----------|----------------|
| 1 — Foundation | 1–2 | 1–2 | 12–16 h |
| 2 — Technology & Standards | 3–4 | 1–2 | 10–14 h |
| 3 — Process & Exploration | 5–6 | 1 | 8–12 h |
| 4 — Per-module READMEs | 7–10 | 1–2 | 10–20 h |
| 5 — ADRs | 10+ | 1 | 4–8 h |
| 6 — Portal (optional) | 12+ | 1 | 4–8 h |
| **Total** | | | **~50–80 h** |

---

*Back: [Documentation Maintenance](maintenance.md)*
