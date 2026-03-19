# ADR-003: Use of Mocking Framework

**Status:** Accepted
**Date:** _Historic_
**Deciders:** Legend Pure core team

## Context

When writing tests for classes with complex dependencies,
it is common to introduce a mocking library such as Mockito to create lightweight
substitutes for dependencies. The question is whether to introduce such a library
into the Legend Pure test classpath.

## Decision

If possible, **Avoid mocking framework being introduced.** Instead, tests use:

1. Real, fully-constructed objects wherever reasonably constructible.
2. Hand-written, package-private stub classes for dependencies that are too
   heavyweight to construct in a test.
3. JUnit 4's `TemporaryFolder` rule for filesystem dependencies.
4. Java reflection to inject fields directly.

## Consequences

**Positive:**

- No additional `pom.xml` dependency; keeps the test classpath identical to the
  rest of the project.
- Tests are more brittle to structural changes (they break when the real class
  changes) but more faithful — they catch real integration issues.
- No "magic" proxy classes; test failures are straightforward to diagnose.
- Consistent with the existing test code throughout the project.

**Negative:**

- Some tests require more setup code.
- Stubs must be maintained when the stubbed interface changes.
- Verifying that a method was called (interaction testing) is harder without a
  mocking framework; use state-based assertions instead.

**Guidance:**
When you need to isolate a dependency, prefer:

1. A lightweight real implementation (e.g. use an in-memory H2 instead of mocking JDBC).
2. A simple hand-written stub with `// test stub` comment.
3. A spy-pattern wrapper that delegates and records calls.

If the isolation problem is genuinely intractable without a mock, open a
discussion before adding Mockito.

---

*See also: [Testing Strategy](../testing/testing-strategy.md#2-test-frameworks-in-use),
