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

//! Values on their way out to a host.

use crate::value::Value;

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
