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

//! Source locations.
//!
//! Every expression node and every element carries a [`SourceInfo`]. The
//! evaluator uses it for error messages; the builders synthesise one for
//! hand-built nodes. Line and column numbers are 1-indexed, matching the
//! Java `SourceInformation` format.

use smol_str::SmolStr;

/// The source location of a node within the original source text.
///
/// # Examples
///
/// ```
/// use legend_pure_metamodel::source_info::SourceInfo;
///
/// let info = SourceInfo::new("test.pure", 1, 1, 1, 10);
/// assert_eq!(info.start_line, 1);
/// assert_eq!(info.to_string(), "test.pure:1:1-1:10");
/// ```
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct SourceInfo {
    /// Source identifier (file path or URI).
    pub source: SmolStr,
    /// Starting line (1-indexed).
    pub start_line: u32,
    /// Starting column (1-indexed).
    pub start_column: u32,
    /// Ending line (1-indexed).
    pub end_line: u32,
    /// Ending column (1-indexed).
    pub end_column: u32,
}

impl SourceInfo {
    /// Creates a new `SourceInfo` from a source identifier and position.
    #[must_use]
    pub fn new(
        source: impl Into<SmolStr>,
        start_line: u32,
        start_column: u32,
        end_line: u32,
        end_column: u32,
    ) -> Self {
        Self {
            source: source.into(),
            start_line,
            start_column,
            end_line,
            end_column,
        }
    }

    /// Merges two source spans into one that covers both.
    #[must_use]
    pub fn merge(&self, other: &Self) -> Self {
        let (start_line, start_column) = if self.start_line < other.start_line
            || (self.start_line == other.start_line && self.start_column <= other.start_column)
        {
            (self.start_line, self.start_column)
        } else {
            (other.start_line, other.start_column)
        };

        let (end_line, end_column) = if self.end_line > other.end_line
            || (self.end_line == other.end_line && self.end_column >= other.end_column)
        {
            (self.end_line, self.end_column)
        } else {
            (other.end_line, other.end_column)
        };

        Self {
            source: self.source.clone(),
            start_line,
            start_column,
            end_line,
            end_column,
        }
    }
}

impl std::fmt::Display for SourceInfo {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(
            f,
            "{}:{}:{}-{}:{}",
            self.source, self.start_line, self.start_column, self.end_line, self.end_column
        )
    }
}

/// A plain string that carries its own source location.
///
/// Used by profile definitions, where stereotype and tag names are plain
/// strings that still need source tracking.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SpannedString {
    /// The string value.
    pub value: SmolStr,
    /// Source location.
    pub source_info: SourceInfo,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display() {
        let info = SourceInfo::new("test.pure", 1, 5, 3, 10);
        assert_eq!(info.to_string(), "test.pure:1:5-3:10");
    }

    #[test]
    fn merge_covers_both_spans() {
        let a = SourceInfo::new("test.pure", 1, 5, 2, 10);
        let b = SourceInfo::new("test.pure", 2, 3, 4, 15);
        let merged = a.merge(&b);
        assert_eq!((merged.start_line, merged.start_column), (1, 5));
        assert_eq!((merged.end_line, merged.end_column), (4, 15));
    }
}
