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

//! How a host describes one of its nodes: twenty-two fields a transport
//! fills, by name, from whatever its host hands over (an object's fields,
//! a flat record).

use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

/// Kind tags of a [`Description`]; the host's description type declares
/// the same numbers.
pub mod kind {
    /// `Boolean` primitive.
    pub const BOOLEAN: i32 = 0;
    /// `Integer` primitive: [`integer`](super::Description::integer);
    /// the decimal digits in `payload` instead when it does not fit 64
    /// bits.
    pub const INTEGER: i32 = 1;
    /// `Float` primitive: [`real`](super::Description::real).
    pub const FLOAT: i32 = 2;
    /// `Decimal` primitive (plain decimal text payload).
    pub const DECIMAL: i32 = 3;
    /// `String` primitive.
    pub const STRING: i32 = 4;
    /// `Date`, `StrictDate` or `DateTime`, as components.
    pub const DATE: i32 = 5;
    /// `%latest`.
    pub const LATEST_DATE: i32 = 6;
    /// `StrictTime`, as components.
    pub const STRICT_TIME: i32 = 7;
    /// `Byte` primitive.
    pub const BYTE: i32 = 8;
    /// A value of an `Enumeration`.
    pub const ENUM_VALUE: i32 = 10;
    /// A `PackageableElement` with a user path.
    pub const PACKAGEABLE_ELEMENT: i32 = 11;
    /// Any other node.
    pub const NODE: i32 = 12;
    /// A Rust object previously handed to the host.
    pub const RUST_OBJECT: i32 = 13;
}

/// One node of the host's graph, described. [`Default`] is a description
/// with no definition (`-1`), no strings and every number zero.
#[derive(Debug, Clone, PartialEq)]
pub struct Description {
    /// One of the [`kind`] constants.
    pub kind: i32,
    /// The host's id for the node (`NODE`, `ENUM_VALUE`,
    /// `PACKAGEABLE_ELEMENT`) or a Rust handle (`RUST_OBJECT`).
    pub id: i64,
    /// The host's id of the lambda definition a per-evaluation lambda
    /// value stands for; -1 when none.
    pub definition: i64,
    /// Classifier user path.
    pub classifier: Option<SmolStr>,
    /// The node's own name, when it has one.
    pub name: Option<SmolStr>,
    /// User path of a packageable element.
    pub user_path: Option<SmolStr>,
    /// The value of an `Integer` that fits 64 bits.
    pub integer: i64,
    /// The value of a `Float`.
    pub real: f64,
    /// Text payload of a primitive: a `Boolean`'s `true` or `false`, a
    /// `Decimal`'s plain digits, a `String`, and the decimal digits of an
    /// `Integer` that does not fit 64 bits.
    pub payload: Option<String>,
    /// Year of a date.
    pub year: i32,
    /// Month, 0 when absent.
    pub month: i32,
    /// Day, 0 when absent.
    pub day: i32,
    /// Hour, 0 when absent.
    pub hour: i32,
    /// Minute, 0 when absent.
    pub minute: i32,
    /// Second, 0 when absent.
    pub second: i32,
    /// 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond.
    pub precision: i32,
    /// Subsecond digits, when `precision == 6`.
    pub subsecond: Option<String>,
    /// Source id of the node's span; `None` when the host knows none.
    pub source_id: Option<String>,
    /// Start line of the span, the host's anchor.
    pub line: i32,
    /// Start column of the span, the host's anchor.
    pub column: i32,
    /// End line of the span.
    pub end_line: i32,
    /// End column of the span.
    pub end_column: i32,
}

impl Default for Description {
    fn default() -> Self {
        Self {
            kind: 0,
            id: 0,
            definition: -1,
            classifier: None,
            name: None,
            user_path: None,
            integer: 0,
            real: 0.0,
            payload: None,
            year: 0,
            month: 0,
            day: 0,
            hour: 0,
            minute: 0,
            second: 0,
            precision: 0,
            subsecond: None,
            source_id: None,
            line: 0,
            column: 0,
            end_line: 0,
            end_column: 0,
        }
    }
}

impl Description {
    /// The source span, when the host gave one. The host's anchor is the
    /// span's start on this side: the evaluator and `assertError` both
    /// read the anchor.
    #[must_use]
    pub fn source_info(&self) -> Option<SourceInfo> {
        let unsigned = |v: i32| u32::try_from(v).unwrap_or(0);
        self.source_id.as_ref().map(|source_id| {
            SourceInfo::new(
                source_id.as_str(),
                unsigned(self.line),
                unsigned(self.column),
                unsigned(self.end_line),
                unsigned(self.end_column),
            )
        })
    }
}
