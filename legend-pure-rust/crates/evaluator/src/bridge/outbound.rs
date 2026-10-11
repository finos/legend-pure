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

//! Values on their way out to a host: a result flattens into leaves, and
//! each leaf is classified into the one thing the host builds for it. A
//! transport materialises every [`Leaf`] arm through its host and knows
//! nothing of the value behind it.

use rust_decimal::Decimal;
use smol_str::SmolStr;

use crate::bridge::failure::{boundary_error, unsupported};
use crate::bridge::host::{HostShared, host_node_id};
use crate::date::{DatePrecision, TimePrecision};
use crate::error::PureException;
use crate::heap::RuntimeHeap;
use crate::model_utils::build_element_path;
use crate::value::{Value, ValueKey};

/// The values `value` stands for once collections are flattened and
/// `Unit` dropped, in order: one host object each.
pub fn leaves<'v>(value: &'v Value, out: &mut Vec<&'v Value>) {
    match value {
        Value::Unit => {}
        Value::Collection(items) => {
            for item in items.iter() {
                leaves(item, out);
            }
        }
        other => out.push(other),
    }
}

/// A date as a host builds one: components, a subsecond string and the
/// precision code of a [`Description`](crate::bridge::description::Description).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DateParts {
    /// Year.
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
    /// Subsecond digits, empty below subsecond precision.
    pub subsecond: String,
    /// 0 year, 1 month, 2 day, 3 hour, 4 minute, 5 second, 6 subsecond.
    pub precision: i32,
}

/// A strict time as a host builds one.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TimeParts {
    /// Hour.
    pub hour: i32,
    /// Minute.
    pub minute: i32,
    /// Second.
    pub second: i32,
    /// Subsecond digits, empty at second precision.
    pub subsecond: String,
    /// 5 second, 6 subsecond.
    pub precision: i32,
}

/// One value that is neither a collection nor `Unit` (see [`leaves`]),
/// as the host builds it.
#[derive(Debug)]
pub enum Leaf<'v> {
    /// `Boolean`.
    Boolean(bool),
    /// `Integer`.
    Integer(i64),
    /// `Float`, finite.
    Float(f64),
    /// `Decimal`.
    Decimal(&'v Decimal),
    /// `String`.
    String(&'v str),
    /// `%latest`.
    LatestDate,
    /// A date of any precision.
    Date(DateParts),
    /// `StrictTime`.
    StrictTime(TimeParts),
    /// A model element, by user path, which the host resolves in its
    /// own graph.
    Element(String),
    /// A value of an enumeration, by the enumeration's user path and
    /// the value's name.
    EnumValue {
        /// The enumeration's user path.
        enumeration: String,
        /// The value's name.
        name: &'v str,
    },
    /// A node the host handed in, by its id.
    HostNode(i64),
    /// A Rust object the host holds from now on, by its integer and its
    /// classifier's user path.
    RustObject {
        /// The integer registered for the object.
        handle: i64,
        /// Classifier user path.
        classifier: SmolStr,
    },
    /// A map, as its entries; each key and value holds exactly one leaf.
    Map(Vec<(Value, Value)>),
}

/// A map key as the value it was made from. A key built from an object's
/// `<<equality.Key>>` fields no longer holds the object, so it cannot
/// cross.
fn key_value(key: &ValueKey) -> Result<Value, PureException> {
    Ok(match key {
        ValueKey::Boolean(b) => Value::Boolean(*b),
        ValueKey::Integer(i) => Value::Integer(*i),
        ValueKey::Decimal(d) => Value::Decimal(*d),
        ValueKey::String(s) => Value::String(s.clone()),
        ValueKey::Date(d) => Value::Date(*d),
        ValueKey::StrictTime(t) => Value::StrictTime(*t),
        ValueKey::Object(handle) => Value::Object(handle.clone()),
        ValueKey::EnumValue { enum_id, member } => Value::EnumValue {
            enum_id: *enum_id,
            member: member.clone(),
        },
        ValueKey::ObjectByEqualityKeys { .. } => {
            return Err(unsupported(
                "a Map keyed by objects with equality keys across the boundary (the key holds their key fields, not the objects)",
            ));
        }
    })
}

/// The one value a map key or map value holds.
fn single(value: &Value) -> Result<Value, PureException> {
    let mut found = Vec::new();
    leaves(value, &mut found);
    match found.as_slice() {
        [one] => Ok((*one).clone()),
        _ => Err(unsupported(format!(
            "a Map entry holding {} values across the boundary",
            found.len()
        ))),
    }
}

/// What the host builds for one leaf. A Rust object that is neither a
/// host node nor a model element is registered for the host here.
///
/// # Errors
/// Value kinds that cannot cross yet.
pub fn leaf<'v>(shared: &HostShared, value: &'v Value) -> Result<Leaf<'v>, PureException> {
    let element_path = |id| build_element_path(&shared.model(), id, "::", false);
    Ok(match value {
        Value::Unit | Value::Collection(_) => {
            return Err(boundary_error(
                "a collection is flattened before it crosses",
            ));
        }
        Value::Boolean(b) => Leaf::Boolean(*b),
        Value::Integer(i) => Leaf::Integer(*i),
        Value::Float(f) => {
            if !f.is_finite() {
                return Err(unsupported("a non-finite Float across the boundary"));
            }
            Leaf::Float(*f)
        }
        Value::Decimal(d) => Leaf::Decimal(d),
        Value::String(s) => Leaf::String(s.as_str()),
        Value::Latest => Leaf::LatestDate,
        Value::Date(d) => {
            let (precision, subsecond): (i32, String) = match d.precision() {
                DatePrecision::Year => (0, String::new()),
                DatePrecision::Month => (1, String::new()),
                DatePrecision::Day => (2, String::new()),
                DatePrecision::Time(TimePrecision::Hour) => (3, String::new()),
                DatePrecision::Time(TimePrecision::Minute) => (4, String::new()),
                DatePrecision::Time(TimePrecision::Second) => (5, String::new()),
                DatePrecision::Time(TimePrecision::Subsecond(digits)) => {
                    let nanos = d.get_subsec_nanosecond().unwrap_or(0);
                    let s = format!("{nanos:09}");
                    (6, s[..usize::from(digits).min(9)].to_string())
                }
            };
            Leaf::Date(DateParts {
                year: i32::from(d.get_year()),
                month: i32::from(d.get_month().unwrap_or(0)),
                day: i32::from(d.get_day().unwrap_or(0)),
                hour: i32::from(d.get_hour().unwrap_or(0)),
                minute: i32::from(d.get_minute().unwrap_or(0)),
                second: i32::from(d.get_second().unwrap_or(0)),
                subsecond,
                precision,
            })
        }
        Value::StrictTime(t) => {
            let nanos = t.nanosecond();
            let (precision, subsecond) = if nanos == 0 {
                (5, String::new())
            } else {
                let s = format!("{nanos:09}");
                (6, s.trim_end_matches('0').to_string())
            };
            Leaf::StrictTime(TimeParts {
                hour: i32::from(t.hour()),
                minute: i32::from(t.minute()),
                second: i32::from(t.second()),
                subsecond,
                precision,
            })
        }
        Value::Element(id) => Leaf::Element(element_path(*id)),
        Value::EnumValue { enum_id, member } => Leaf::EnumValue {
            enumeration: element_path(*enum_id),
            name: member.as_str(),
        },
        Value::Object(handle) => {
            if let Some(id) = host_node_id(handle) {
                Leaf::HostNode(id)
            } else if let Some(eid) = RuntimeHeap::element_for_object(handle) {
                Leaf::Element(element_path(eid))
            } else {
                let classifier = handle.borrow().classifier();
                Leaf::RustObject {
                    handle: shared.register_handle(handle.clone()),
                    classifier,
                }
            }
        }
        Value::Function(_) => return Err(unsupported("a function value across the boundary")),
        Value::Map(map) => {
            let entries: Vec<(Value, Value)> = map
                .borrow()
                .entries
                .iter()
                .map(|(k, v)| Ok((key_value(k)?, v.clone())))
                .collect::<Result<_, PureException>>()?;
            let mut singles = Vec::with_capacity(entries.len());
            for (key, value) in &entries {
                singles.push((single(key)?, single(value)?));
            }
            Leaf::Map(singles)
        }
        Value::UnitInstance { .. } => {
            return Err(unsupported("a unit instance across the boundary"));
        }
    })
}
