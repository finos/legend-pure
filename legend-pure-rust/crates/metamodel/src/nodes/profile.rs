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

//! Compiled Profile node.

use crate::source_info::SourceInfo;
use crate::source_info::SpannedString;
use smol_str::SmolStr;

/// A compiled profile definition.
///
/// A profile declares stereotypes and tags that can be applied to
/// elements, properties, and enum values via `<<profile.stereo>>`
/// and `{profile.tag = 'value'}`.
///
/// Each declared name carries its own [`SpannedString::source_info`]
/// so IDE goto-def can navigate from a use-site (`<<P.stereo>>`) to
/// the declaration line. m3-bootstrap and loaded profiles
/// use a synthetic source — see [`BOOTSTRAP_SOURCE`] / the
/// [`bootstrap_spanned_name`] constructor. The reference index
/// recognises that sentinel and skips emitting entries for it.
#[derive(Debug, Clone, PartialEq)]
pub struct Profile {
    /// Stereotype declarations, each carrying its name + source span.
    pub stereotypes: Vec<SpannedString>,
    /// Tag declarations, each carrying its name + source span.
    pub tags: Vec<SpannedString>,
}

/// Sentinel source name used by [`bootstrap_spanned_name`] for
/// declarations whose span isn't tracked (m3 bootstrap, loaded
/// graphs without span data).
pub const BOOTSTRAP_SOURCE: &str = "<bootstrap>";

/// Build a [`SpannedString`] with the bootstrap sentinel source.
/// Callers constructing Profiles outside the parser path use this
/// to fill the `stereotypes` / `tags` lists with name-only entries.
#[must_use]
pub fn bootstrap_spanned_name(name: SmolStr) -> SpannedString {
    SpannedString {
        value: name,
        source_info: SourceInfo::new(BOOTSTRAP_SOURCE, 0, 0, 0, 0),
    }
}
