# Repository Guidelines

## Project Structure & Module Organization
- Root is a multi-module Maven project (`pom.xml`). Core modules live under:
  - `legend-pure-core`, `legend-pure-dsl`, `legend-pure-maven`, `legend-pure-runtime`, `legend-pure-store`.
- Typical layout per module:
  - `src/main/java`, `src/test/java`, `src/main/resources`, and where applicable `src/main/antlr4`.
- Build outputs go to each module’s `target/`. Use `-pl <module> -am` to build a subset.

## Build, Test, and Development Commands
- Build all: `mvn clean install` — compiles, runs tests, and packages all modules.
- Faster build: `mvn -T 1C -DskipTests install` — parallel, skip tests.
- Verify + quality gates: `mvn verify` — includes Checkstyle and dependency analysis.
- Single module: `mvn -pl legend-pure-core -am install` — builds module and its deps.
- Single test: `mvn -Dtest=ClassNameTest test` (or `ClassNameTest#method`).
- Optional: `./h2Start.sh` starts a local H2 instance used by some tests/utilities.

## Coding Style & Naming Conventions
- Java code style is enforced by Checkstyle (see `checkstyle.xml`), based on Google style.
- Use spaces (no tabs). Keep lines readable; braces and whitespace must follow the rules enforced by Checkstyle.
- Package/type/member names should follow the patterns encoded in `checkstyle.xml` and match existing packages (e.g., `org.finos.legend.pure...`).

## Testing Guidelines
- Framework: JUnit 4.x. Place tests under `src/test/java` mirroring package structure.
- Naming: use `*Test.java` and test method names that clearly state intent.
- Run all tests with `mvn test`; run module tests with `mvn -pl <module> test`.
- Coverage: JaCoCo is available; modules that bind it will write reports under `target/site/jacoco`.

## Commit & Pull Request Guidelines
- Commit messages: imperative mood, concise subject; reference issues/PRs when relevant (e.g., "Fix namespace conflicts (#123)").
- PRs must:
  - Describe the change, rationale, and impact.
  - Link related issues and include screenshots/logs when UI or behavior changes.
  - Include tests for new behavior and updates to docs when needed.
  - Pass CI (build, tests, Checkstyle, dependency checks). Keep diffs focused per module.

## Security & Configuration Tips
- Build requires JDK 11 or 17 and Maven 3.6+ (see `README.md` and Maven Enforcer rules).
- Certain dependencies are banned via Enforcer; avoid adding alternative logging stacks or restricted libs.
- Use UTF-8 source encoding; avoid adding files with tabs or non-standard headers.
