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

//! Compiled Association node.

use crate::annotations::{StereotypeRef, TaggedValueRef};
use crate::nodes::class::{Property, QualifiedProperty};

/// A compiled association definition linking two classes.
///
/// An association declares properties that are injected into the connected
/// classes. These injected properties are NOT stored on the Class node —
/// they are computed as a derived index on the frozen model.
#[derive(Debug, Clone, PartialEq)]
pub struct Association {
    /// Properties (typically exactly two — one for each end).
    pub properties: Vec<Property>,
    /// Qualified (derived) properties.
    pub qualified_properties: Vec<QualifiedProperty>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValueRef>,
    /// Properties moved aside by milestoning post-processing because their
    /// target class carries a temporal stereotype.
    pub original_milestoned_properties: Vec<Property>,
}
