# Documentation Maintenance

## 1. Docs-as-Code Principle

All documentation lives in the `/docs` directory of the repository, versioned
alongside the code. Documentation changes follow the same PR review process as code
changes.

### Rule: Update Docs in the Same PR as Code

| Type of code change | Required doc update |
|--------------------|---------------------|
| New module added | New module entry in [Module Reference](../architecture/modules.md); new per-module `README.md` using the [template](../templates/module-readme-template.md) |
| Module renamed or removed | Update [Architecture Overview](../architecture/overview.md) and [Module Reference](../architecture/modules.md) |
| New third-party library added | Update [Technology Stack](../architecture/tech-stack.md) |
| New Maven plugin goal | Update [Build & CI Guide](../guides/build-and-ci.md) and [Module Reference](../architecture/modules.md) |
| New test convention established | Update [Testing Strategy](../testing/testing-strategy.md) |
| New Checkstyle rule added | Update [Coding Standards](../standards/coding-standards.md) |
| New domain concept or glossary term | Update [Domain & Key Concepts](../architecture/domain-concepts.md) |
| JDK or Maven version requirement changed | Update [Getting Started Guide](../guides/getting-started.md) and [Technology Stack](../architecture/tech-stack.md) |
| CI pipeline changed | Update [Build & CI Guide](../guides/build-and-ci.md) |

---

## 2. Ownership Model

| Documentation section | Owner | Backup |
|----------------------|-------|--------|
| Architecture Overview & Module Reference | Core platform lead | Any senior engineer |
| Technology Stack | Build / platform lead | Core platform lead |
| Domain & Key Concepts | Domain modelling lead | Core platform lead |
| Getting Started Guide | Developer experience lead | Any engineer who recently onboarded |
| Build & CI Guide | Build / DevOps lead | Core platform lead |
| Coding Standards | Tech lead | Any senior engineer |
| Testing Strategy | QA / test lead | Core platform lead |
| Exploration Guide | Developer experience lead | Any senior engineer |
| Per-module READMEs | Module owner (see `CODEOWNERS`) | Module contributors |
| Documentation Plan & Maintenance | Tech lead | Project manager |

> **`CODEOWNERS` integration:** Add documentation ownership entries to
> [`.github/CODEOWNERS`](../../CODEOWNERS) so that relevant owners are automatically
> requested as reviewers on PRs that touch documentation.

---

## 3. Review Cadence

Ideal / target review cadence for different types of documentation updates:

| Review type | Frequency | Participants |
|-------------|-----------|-------------|
| **Full docs review** | Quarterly (January, April, July, October) | All doc owners |
| **Onboarding feedback session** | After every new engineer joins | New engineer + developer experience lead |
| **Architecture decision record (ADR)** | On-demand when major technical decisions are made | Tech lead + affected module owners |
| **Stale content audit** | As part of quarterly review | Tech lead |

### Periodic Review Checklist

- [ ] Are all module descriptions still accurate?
- [ ] Are all dependency versions in `tech-stack.md` still current?
- [ ] Does the Getting Started Guide still work end-to-end for a clean checkout?
- [ ] Are there any new tribal-knowledge items that should be documented?
- [ ] Are per-module READMEs complete for all modules?
- [ ] Are there any broken links in the docs?
- [ ] Has the CI pipeline changed since the last review?

---

## 4. PR Requirements for Documentation

The PR template (if/when added to `.github/PULL_REQUEST_TEMPLATE.md`) should include:

```markdown
### Documentation

- [ ] Documentation updated in `/docs` if behaviour, API, or setup changed.
- [ ] Per-module README updated if this module's purpose or dependencies changed.
- [ ] No hard-coded version numbers in docs (use references to the root POM property names).
```

---

## 5. Documentation Tooling

The documentation is plain Markdown — no build step required. However, the following
tools are recommended:

| Tool | Purpose |
|------|---------|
| **markdownlint** | Lint Markdown for consistent style. Run: `npx markdownlint docs/**/*.md` |
| **markdown-link-check** | Detect broken links. Run: `npx markdown-link-check docs/**/*.md` |
| **MkDocs + Material theme** *(future)* | Convert the `/docs` folder to a searchable static site if the team wants a hosted docs portal |

To install linting tools:

```bash
npm install -g markdownlint-cli markdown-link-check
markdownlint "docs/**/*.md"
markdown-link-check docs/README.md
```

---

## 6. Architecture Decision Records (ADRs)

For significant technical decisions, create an ADR in `docs/decisions/`:

```text
docs/decisions/
  ADR-001-junit4-over-junit5.md
  ADR-002-eclipse-collections-as-primary.md
  ADR-003-no-mocking-framework.md
```

### ADR Template

```markdown
# ADR-NNN: <Short Title>

**Status:** Accepted / Superseded by ADR-XYZ / Deprecated
**Date:** YYYY-MM-DD
**Deciders:** [names]

## Context
What is the issue we are addressing?

## Decision
What was decided?

## Consequences
What are the positive and negative consequences of this decision?
```

---

*Back: [Testing Strategy](../testing/testing-strategy.md) · Next: [Documentation Plan](documentation-plan.md)*
