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

//! Compiled Enumeration node.

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::annotations::{StereotypeRef, TaggedValueRef};

/// A compiled enumeration definition.
#[derive(Debug, Clone, PartialEq)]
pub struct Enumeration {
    /// Enum values (members).
    pub values: Vec<EnumValue>,
    /// Stereotypes.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values.
    pub tagged_values: Vec<TaggedValueRef>,
}

/// A single value (member) in an enumeration.
#[derive(Debug, Clone, PartialEq)]
pub struct EnumValue {
    /// The value name.
    pub name: SmolStr,
    /// Source location.
    pub source_info: SourceInfo,
    /// Stereotypes on this enum value.
    pub stereotypes: Vec<StereotypeRef>,
    /// Tagged values on this enum value.
    pub tagged_values: Vec<TaggedValueRef>,
}
