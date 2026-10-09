// Copyright 2026 Goldman Sachs
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

//! The Pure language metamodel the evaluator needs.
//!
//! This crate holds the element tables (`PureModel`, `Element`, the
//! `nodes` module), the expression tree (`types::ValueSpec` and
//! `types::ExprKind`) and the M3 bootstrap (`bootstrap`), which embeds
//! `m3.pure` at compile time and is the only Pure source this workspace
//! depends on.
//!
//! The plan this crate serves is `docs/rust-evaluator-mvp.md` at the
//! workspace root; Appendix A there records where each module came from.

#![forbid(unsafe_code)]
#![deny(missing_docs)]

pub mod annotations;
pub mod arena;
pub mod bootstrap;
pub mod fqn;
pub mod ids;
pub mod m3_parser;
pub mod model;
pub mod nodes;
pub mod source_info;
pub mod types;
