# Coding Standards & Style Guide

## 1. Code Formatting — Checkstyle

The project uses **Checkstyle 8.25** with a custom rule set based on the
[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
The configuration lives at [`checkstyle.xml`](../../checkstyle.xml) in the project root.

Checkstyle runs automatically at the `verify` phase:

```bash
mvn checkstyle:check          # check only (fast)
mvn verify -DskipTests        # full verify phase including Checkstyle
```

Violations at `warning` severity **fail the build** (`<failOnViolation>true</failOnViolation>`).

### Key Checkstyle Rules

| Rule | Requirement |
|------|------------|
| **Copyright header** | Every `.java`, `.properties`, and `.xml` file must contain a copyright header with a valid Apache 2.0 license reference |
| **No tab characters** | Use **spaces** throughout; tabs anywhere in the file fail the build |
| `OneTopLevelClass` | Each Java file must contain exactly one top-level type |
| `NoLineWrap` | Import statements must not be line-wrapped |
| `NeedBraces` | All `if`, `else`, `for`, `while`, `do` bodies must have braces |
| `LeftCurly` | Opening brace on a **new line** (`nl` option) |
| `RightCurly` | Closing brace on its **own line** (`alone` option) |
| `OneStatementPerLine` | One statement per line |
| `MultipleVariableDeclarations` | Declare each variable on its own line |
| `EmptyBlock` | Empty blocks must contain text (a comment), not be truly empty |
| `EmptyCatchBlock` | Allowed only when the exception variable is named `expected` or `ignored` |
| `FallThrough` | Switch fall-through must have a comment |
| `ModifierOrder` | Modifiers in the order: `public protected private abstract default static final transient volatile synchronized native strictfp` |

### Naming Conventions (Enforced by Checkstyle)

| Element | Pattern | Example |
|---------|---------|---------|
| Package | `[a-zA-Z]+(\.[_a-zA-Z][_a-zA-Z0-9]*)*` | `org.finos.legend.pure.m3` |
| Class / Interface / Enum | `[_a-zA-Z][_a-zA-Z0-9]*` | `CoreInstance`, `M3Compiler` |
| Method | `[_a-zA-Z][a-zA-Z0-9_$]*` | `getValueForMetaPropertyToOne` |
| Member variable | `[_a-zA-Z][_a-zA-Z0-9]*` | `sourceInformation`, `_name` |
| Parameter | `[_a-zA-Z][_a-zA-Z0-9]*?` | `propertyName`, `instance` |
| Local variable | `[_a-zA-Z][_a-zA-Z0-9]*?` | `result`, `currentNode` |
| Type parameter | `(^[A-Z][0-9]?)\|([A-Z][a-zA-Z0-9]*)` | `T`, `V`, `CoreType` |

> The patterns are intentionally **permissive** compared to strict Google style (they
> allow underscores and capital-first members) to accommodate the Legacy codebase.

### Import Order

`CustomImportOrder` is enabled with `separateLineBetweenGroups=true`. IntelliJ can
be configured to match: **Settings → Editor → Code Style → Java → Imports →
"Use single class imports"**, layout per Google style.

---

## 2. Copyright Header Template

Every new file must start with comment similar to the below:

```java
// Copyright <YEAR> Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
```

For XML and properties files the comment style must be adapted accordingly.
Checkstyle validates the presence of `Copyright` and the Apache license URL
via `RegexpMultiline`.

---

## 3. General Code Conventions

### Collections

- **Prefer Eclipse Collections** over `java.util` collections for all new code.
- Use `Lists.mutable.empty()` / `Lists.immutable.with(...)` instead of
  `new ArrayList<>()` / `List.of(...)`.
- Return `RichIterable` or `ListIterable` from APIs, not `java.util.List`, unless
  you need `java.util` compatibility.

### Null Handling

- Avoid returning `null` from public methods. Use `Optional` (sparingly) or throw a
  meaningful exception.
- Eclipse Collections methods like `getIfAbsent`, `getIfAbsentPut`, and `detect`
  remove common null-check patterns.

### Method Visibility

- Make methods as package-private as possible for testability without exposing them
  as public API.
- When widening visibility for testing, add a comment: `// visible for testing`
  (do **not** add a Guava `@VisibleForTesting` annotation dependency solely for this).

### Logging

```java
// Correct
private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);
LOGGER.info("Compiling repository: {}", repositoryName);

// Wrong — uses raw System.out or System.err
System.out.println("Compiling...");
```

- Use `{}` placeholders instead of string concatenation in log calls.
- Log level guidelines:
  - `TRACE` — very detailed, loop-level diagnostics (disabled in production).
  - `DEBUG` — developer diagnostics; individual step outcomes.
  - `INFO` — major milestones (start/end of compilation, plugin execution).
  - `WARN` — unexpected but recoverable state; deprecated API usage.
  - `ERROR` — unrecoverable failures; exceptions that propagate to the user.
- **Never log** credentials, personal data, raw SQL with user values, or security tokens.

### Exception Handling

```java
// Correct — named 'expected' or 'ignored'
try {
    parseOptionalElement();
}
catch (Exception expected)
{
    // intentionally ignored: element is optional
}

// Wrong — empty catch without a name or comment
try { ... } catch (Exception e) {}
```

- Do not swallow `PureCompilationException` or `PureExecutionException` without
  re-throwing or explicitly logging.
- Do not call `System.exit()` anywhere in library code.
- Prefer specific exception types over catching `Exception` or `Throwable`.

---

## 4. Git Workflow & Branching Strategy

The project follows a **feature-branch / GitHub Flow** model:

```text
main (master)
  └── feature/<short-description>
  └── fix/<short-description>
  └── docs/<short-description>
```

### Branch Naming

| Prefix | Purpose |
|--------|---------|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `docs/` | Documentation-only changes |
| `refactor/` | Internal restructuring without behaviour change |
| `test/` | Test additions or corrections |
| `chore/` | Dependency upgrades, build configuration |

### Commit Message Convention

```text
<type>(<scope>): <short summary in present tense, ≤72 chars>

[Optional longer body explaining WHY, not WHAT]

[Optional: Closes #<issue-number>]
```

**Types:** `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`

**Example:**

```text
feat(compiler): add write-if-changed guard to PureCompilerBinaryGenerator

Prevents downstream Maven plugins from re-compiling unchanged generated files,
significantly improving incremental build times.

Closes #1234
```

---

## 5. Pull Request Checklist

Before opening a PR, verify every item:

- [ ] `mvn install` passes locally (no test failures, no Checkstyle violations).
- [ ] All new Java files have the copyright header.
- [ ] New or changed behaviour has tests (see [Testing Strategy](../testing/testing-strategy.md)).
- [ ] No new `System.out.println` or `e.printStackTrace()` calls.
- [ ] No new `null` returns from public methods without documentation.
- [ ] Dependency changes in `pom.xml` use the managed version from the root POM.
- [ ] If a module's behaviour changed, the module's `README.md` is updated.
- [ ] If a new library was added, the [Technology Stack](../architecture/tech-stack.md) doc is updated.
- [ ] PR description explains **what** changed and **why**.
- [ ] PR is linked to the relevant GitHub issue (if any).

### Code Review Expectations

- Reviewers should check: correctness, test coverage, style compliance, and impact on
  the downstream Legend platform.
- Authors should respond to all review comments before merging.
- At least **one approving review** is required before merging to `master`.
- Do not merge your own PR unless it is a trivial documentation fix.

---

## 6. API Design Conventions

Legend Pure is a library, not a web service. There are no REST endpoints in this
repository. API conventions apply to Java public APIs:

- **Method naming:** `getX()` for simple accessors, `findX()` for nullable lookups,
  `resolveX()` for operations that compute a result, `buildX()` for factory/builder
  patterns.
- **Immutability:** Prefer returning `ImmutableList` / `ImmutableSet` from public APIs
  to prevent callers from mutating internal state.
- **Backwards compatibility:** Public interfaces and classes in `legend-pure-m4` and
  `legend-pure-m3-core` are consumed by downstream Legend projects. Adding methods to
  interfaces is a **breaking change** unless a default implementation is provided.
- **Deprecation:** Annotate with `@Deprecated` and add a Javadoc `@deprecated` tag
  explaining the replacement. Do not remove deprecated methods in the same release
  that deprecates them.

---

*Back: [Build & CI Guide](../guides/build-and-ci.md) · Next: [Testing Strategy](../testing/testing-strategy.md)*
