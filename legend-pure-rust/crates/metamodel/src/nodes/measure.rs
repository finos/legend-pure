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

//! Compiled Measure node.
//!
//! Units are promoted to standalone Elements with their own `ElementId`
//! (see `nodes/unit.rs`). The Measure stores references to its units
//! by `ElementId` rather than embedding them.

use crate::ids::ElementId;

/// A compiled measure definition.
///
/// A measure defines a system of units with a canonical unit and
/// zero or more non-canonical units, each with conversion functions.
#[derive(Debug, Clone, PartialEq)]
pub struct Measure {
    /// The canonical unit (`*` marked), referenced by `ElementId`.
    pub canonical_unit: Option<ElementId>,
    /// Non-canonical units, each referenced by `ElementId`.
    pub non_canonical_units: Vec<ElementId>,
}
