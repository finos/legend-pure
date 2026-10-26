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

//! Compiled Unit node.
//!
//! Units are promoted to standalone Elements (each gets its own `ElementId`)
//! so they can be referenced in type positions: `prop: Kilogram[1]`.
//!
//! The parent Measure references its units by `ElementId`.

use crate::ids::ElementId;
use crate::types::Expression;

/// A compiled unit definition within a measure.
///
/// Each unit has its own `ElementId` and `ElementNode` (name, source, package).
/// The conversion expression converts from this unit to the canonical unit.
#[derive(Debug, Clone, PartialEq)]
pub struct Unit {
    /// The parent measure this unit belongs to.
    pub measure: ElementId,
    /// Conversion expression to the canonical unit (None for the canonical unit itself).
    pub conversion_expression: Option<Expression>,
}
