# ADR-001: Use JUnit 4 Rather Than JUnit 5 for the moment

**Status:** Accepted
**Date:** _Historic_
**Deciders:** Legend Pure core team

## Context

The project has an existing, large test suite entirely written with JUnit 4
(`junit:junit:4.13.1`). The question arose whether new tests (particularly for the
Maven plugin tier) should use JUnit 5 (Jupiter) to benefit from its richer extension
model, parameterized test support, and improved assertion API.

## Decision

Tests will continue to use **JUnit 4**, consistent with every other module in the project.

Specifically:

- Use `@Test`, `Assert.*`, `@Rule`, `@ClassRule`, and `TemporaryFolder`.
- Do not introduce `junit-jupiter-api`, `junit-platform-launcher`, or any JUnit 5
  transitives.

There should be a consistent / consolidated effort to modernize the test codebase to JUnit 5,
but for now all new tests will use JUnit 4 to avoid adding a second test framework to the classpath.

## Consequences

**Positive:**

- Zero additional dependencies; no classpath or classloader complexity.
- Every test follows the same pattern — a new engineer only needs to learn one test
  framework.
- The Maven Surefire configuration does not need modification; JUnit 4 tests are
  detected automatically.

**Negative:**

- JUnit 4's `Assert.*` methods are less expressive than JUnit 5's `Assertions.*` +
  `assertAll()`.
- Parameterized tests in JUnit 4 (`@RunWith(Parameterized.class)`) are more verbose
  than JUnit 5's `@ParameterizedTest`.
- JUnit 4 is in maintenance mode; eventually the project will need to migrate.

**Mitigation:**

- Eclipse Collections `Verify.*` assertions partially compensate for JUnit 4's
  weaker collection assertions.
- A future migration to JUnit 5 can be done either incrementally module-by-module or as a holistic upgrade

---

_See also: [Testing Strategy](../testing/testing-strategy.md#2-test-frameworks-in-use)_
