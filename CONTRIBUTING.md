# Contributing

## Copyright Requirements

All source files (`.rs`, `.toml`, `.pure`, `.sh`) **must** have a copyright header in the first 5 lines. The check validates the pattern `Copyright <YEAR>` — the entity name is contributor-specific.

**Exempt**: Markdown (`.md`) and JSON (`.json`) files do not require copyright headers.

For `.rs` files:
```rust
// Copyright 2026 <Your Name or Organization>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// ...
```

For `.toml` files:
```toml
# Copyright 2026 <Your Name or Organization>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# ...
```

For `.pure` files:
```pure
// Copyright 2026 <Your Name or Organization>
//
// Licensed under the Apache License, Version 2.0 (the "License");
// ...
```

Run the validation: `./scripts/check-copyright.sh`

## Code Style

- **Format**: `cargo fmt` (config in `rustfmt.toml`)
- **Lint (strict)**: `cargo lint-lib` — no `unwrap()`/`expect()` in library code
- **Lint (standard)**: `cargo lint` — standard clippy on all targets
- **Docs**: All public items need `///` doc comments (`#![deny(missing_docs)]`)
- **Unsafe**: Forbidden in all crates except `jni` (`#![forbid(unsafe_code)]`)

## PR Checklist

- [ ] `cargo fmt --check` passes
- [ ] `cargo lint-lib` passes (strict: no `unwrap()`/`expect()` in library code)
- [ ] `cargo lint` passes (standard clippy on all targets)
- [ ] `cargo test --workspace` passes
- [ ] `cargo llvm-cov --workspace --fail-under-lines 90` passes
- [ ] New public items have doc comments
- [ ] Snapshot tests updated if Protocol JSON output changed (`cargo insta review`)
- [ ] `./scripts/check-copyright.sh` passes (all `.rs`, `.toml`, `.pure`, `.sh` files have copyright headers)

## Adding a Grammar Feature End-to-End

1. **AST** (`crates/ast/`): Add types or variants for the new syntax
2. **Tests** (`tests/`): Write failing tests first (TDD)
3. **Lexer** (`crates/lexer/`): Add tokens if new syntax requires them
4. **Parser** (`crates/parser/`): Implement parsing to produce AST nodes
5. **Protocol** (`crates/protocol/`): Implement AST ↔ Protocol JSON conversion
6. **Snapshots**: Run `cargo insta review` to approve new golden files

## Tracing Guidelines

- `ast` crate: No tracing (pure data)
- `lexer`: `trace` for tokens, `debug` for state transitions
- `parser`: `#[instrument]` on grammar rule methods (`debug` level)
- `protocol`: `debug` per element conversion
- `jni`: `info` for call boundaries, `error` for failures
- **Never** log at `info` or above in hot paths
