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

//! Resolved annotation references (stereotypes and tagged values).
//!
//! At the Pure semantic level, annotation references point to resolved
//! `ElementId`s (the Profile element) rather than string paths.

use crate::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::ids::ElementId;

/// A resolved stereotype reference: `<<profile.stereotypeName>>`.
///
/// Unlike `ast::StereotypePtr` which uses string paths, this points to the
/// resolved Profile element via `ElementId`.
#[derive(Debug, Clone, PartialEq)]
pub struct StereotypeRef {
    /// The resolved Profile element this stereotype belongs to.
    pub profile: ElementId,
    /// The stereotype name within the profile.
    pub value: SmolStr,
    /// Source range covering the entire `profile.value` reference inside
    /// the surrounding `<<...>>`.
    /// `None` for synthetic refs constructed by tests or
    /// loaded from a graph that does not carry source info.
    pub source_info: Option<SourceInfo>,
}

/// A resolved tagged value reference: `{profile.tagName = 'value'}`.
///
/// Unlike `ast::TaggedValue` which uses string paths, this points to the
/// resolved Profile element via `ElementId`.
#[derive(Debug, Clone, PartialEq)]
pub struct TaggedValueRef {
    /// The resolved Profile element this tag belongs to.
    pub profile: ElementId,
    /// The tag name within the profile.
    pub tag: SmolStr,
    /// The string value assigned to this tag.
    pub value: String,
    /// Source range covering the entire `profile.tag` reference inside
    /// the surrounding `{...}`.
    pub source_info: Option<SourceInfo>,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn stereotype_ref_construction() {
        let s = StereotypeRef {
            profile: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 10,
            },
            value: SmolStr::new("deprecated"),
            source_info: None,
        };
        assert_eq!(s.value, "deprecated");
    }

    #[test]
    fn tagged_value_ref_construction() {
        let tv = TaggedValueRef {
            profile: ElementId::InstanceId {
                chunk_id: 0,
                local_idx: 10,
            },
            tag: SmolStr::new("description"),
            value: "A person".to_string(),
            source_info: None,
        };
        assert_eq!(tv.tag, "description");
        assert_eq!(tv.value, "A person");
    }
}
