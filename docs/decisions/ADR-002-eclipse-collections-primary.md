# ADR-002: Eclipse Collections as the Primary Collection Library

**Status:** Accepted
**Date:** _Historic_
**Deciders:** Legend Pure core team (inherited from Goldman Sachs origin)

## Context

Java's standard library (`java.util`) provides a basic collection framework.
The project must build and traverse large in-memory object graphs (the `CoreInstance`
graph of a compiled Pure model can contain hundreds of thousands of nodes).
The choice of collection library affects both API ergonomics and runtime performance.

## Decision

**Eclipse Collections** (`org.eclipse.collections:eclipse-collections') is the
**primary collection library** throughout the codebase.

- Use `MutableList`, `ImmutableList`, `MutableMap`, `ImmutableMap`, etc. from Eclipse
  Collections in preference to `java.util.ArrayList`, `java.util.HashMap`, etc.
- Use factory methods: `Lists.mutable.empty()`, `Maps.mutable.of(k, v)`,
  `Sets.immutable.with(...)`.
- Return `RichIterable` or `ListIterable` from public APIs where possible.

Google Guava (`com.google.guava:guava:33.4.6-jre`) is available as a secondary
utility library for cases not well served by Eclipse Collections.

## Consequences

**Positive:**

- Rich API: `collect`, `select`, `detect`, `groupBy`, `zip`, `flatCollect` eliminate
  many `for`-loop patterns.
- `getIfAbsent`, `getIfAbsentPut`, `withKeyValue` remove null-check boilerplate.
- Primitive collections (`IntList`, `LongObjectMap`) avoid boxing overhead in
  performance-critical paths.
- Immutable collection factories make defensive copying the path of least resistance.

**Negative:**

- New engineers unfamiliar with Eclipse Collections face a short learning curve.
- Some integration points with Java standard library APIs require conversion
  (`asList()`, `castToList()`).
- Library is less widely known than Guava or vanilla `java.util`.

---

_See also: [Technology Stack](../architecture/tech-stack.md#3-major-third-party-libraries)_
