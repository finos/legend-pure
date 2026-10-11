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

//! A tree-walking evaluator for Pure.
//!
//! The evaluator executes the expression tree of the metamodel crate
//! (`legend_pure_metamodel::types::ValueSpec`) against a `PureModel`,
//! producing [`Value`](value::Value)s. Its model starts as the bootstrap
//! alone; further elements arrive from the test builders or, later, from a
//! loader. Natives are fished one at a time into [`native`].
//!
//! The plan this crate serves is `docs/rust-evaluator-mvp.md` at the
//! workspace root; Appendix A there records where each module came from.

#![forbid(unsafe_code)]
#![deny(missing_docs)]
// PureException carries a call stack (Vec) so its size is inherently large.
// Boxing it at every call site would require rewriting the entire native API.
#![allow(clippy::result_large_err)]

pub mod builder;
pub mod builders;
pub mod context;
pub mod date;
pub mod error;
pub mod eval;
pub mod heap;
pub mod hooks;
pub mod lower;
pub mod m3_paths;
pub mod model_utils;
pub mod native;
pub mod scope_guard;
pub mod value;
