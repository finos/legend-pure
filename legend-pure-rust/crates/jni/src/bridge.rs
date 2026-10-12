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

//! The Java classes the crate talks to, and the method and field ids
//! resolved once per context on them.

use jni::JNIEnv;
use jni::objects::{JFieldID, JMethodID};

/// Java-side method ids, resolved once per context.
#[allow(missing_docs)]
pub struct BridgeMethods {
    pub describe: JMethodID,
    pub property_values: JMethodID,
    pub captured_value: JMethodID,
    pub new_boolean: JMethodID,
    pub new_integer: JMethodID,
    pub new_float: JMethodID,
    pub new_decimal: JMethodID,
    pub new_string: JMethodID,
    pub new_date: JMethodID,
    pub new_latest_date: JMethodID,
    pub new_strict_time: JMethodID,
    pub element: JMethodID,
    pub enum_value: JMethodID,
    pub node: JMethodID,
    pub rust_object: JMethodID,
    pub new_map: JMethodID,
    pub failure_of: JMethodID,
    pub forget: JMethodID,
    pub keys_of: JMethodID,
}

/// Java class names the crate talks to.
pub const BRIDGE_CLASS: &str = "org/finos/legend/pure/rust/RustNodeBridge";
/// The description record the bridge hands back per node.
pub const DESCRIPTION_CLASS: &str = "org/finos/legend/pure/rust/NodeDescription";
/// The only currency across the boundary.
pub const CORE_INSTANCE_CLASS: &str = "org/finos/legend/pure/m4/coreinstance/CoreInstance";

impl BridgeMethods {
    /// Resolve every method id on the bridge class.
    ///
    /// # Errors
    /// Any JNI lookup failure (the Java class is older than this binary).
    pub fn resolve(env: &mut JNIEnv) -> jni::errors::Result<Self> {
        let cls = env.find_class(BRIDGE_CLASS)?;
        let ci = format!("L{CORE_INSTANCE_CLASS};");
        let desc = format!("L{DESCRIPTION_CLASS};");
        Ok(Self {
            describe: env.get_method_id(&cls, "describe", format!("({ci}){desc}"))?,
            property_values: env.get_method_id(
                &cls,
                "propertyValues",
                format!("(JLjava/lang/String;)[{desc}"),
            )?,
            captured_value: env.get_method_id(
                &cls,
                "capturedValue",
                format!("(JLjava/lang/String;)[{desc}"),
            )?,
            new_boolean: env.get_method_id(&cls, "newBoolean", format!("(Z){ci}"))?,
            new_integer: env.get_method_id(&cls, "newInteger", format!("(J){ci}"))?,
            new_float: env.get_method_id(&cls, "newFloat", format!("(Ljava/lang/String;){ci}"))?,
            new_decimal: env.get_method_id(
                &cls,
                "newDecimal",
                format!("(Ljava/lang/String;){ci}"),
            )?,
            new_string: env.get_method_id(
                &cls,
                "newString",
                format!("(Ljava/lang/String;){ci}"),
            )?,
            new_date: env.get_method_id(
                &cls,
                "newDate",
                format!("(IIIIIILjava/lang/String;I){ci}"),
            )?,
            new_latest_date: env.get_method_id(&cls, "newLatestDate", format!("(){ci}"))?,
            new_strict_time: env.get_method_id(
                &cls,
                "newStrictTime",
                format!("(IIILjava/lang/String;I){ci}"),
            )?,
            element: env.get_method_id(&cls, "element", format!("(Ljava/lang/String;){ci}"))?,
            enum_value: env.get_method_id(
                &cls,
                "enumValue",
                format!("(Ljava/lang/String;Ljava/lang/String;){ci}"),
            )?,
            node: env.get_method_id(&cls, "node", format!("(J){ci}"))?,
            rust_object: env.get_method_id(
                &cls,
                "rustObject",
                format!("(JLjava/lang/String;){ci}"),
            )?,
            new_map: env.get_method_id(&cls, "newMap", format!("([{ci}[{ci}){ci}"))?,
            forget: env.get_method_id(&cls, "forget", "([J)V")?,
            keys_of: env.get_method_id(&cls, "keysOf", "(J)[Ljava/lang/String;")?,
            failure_of: env.get_method_id(
                &cls,
                "failureOf",
                "(Ljava/lang/Throwable;)[Ljava/lang/String;",
            )?,
        })
    }
}

/// The fields of the Java `NodeDescription`, resolved once per context:
/// reading a description is then one JNI call per field, with no class or
/// field lookup by name.
#[allow(missing_docs)]
pub struct DescriptionFields {
    pub kind: JFieldID,
    pub id: JFieldID,
    pub definition: JFieldID,
    pub classifier: JFieldID,
    pub name: JFieldID,
    pub user_path: JFieldID,
    pub integer: JFieldID,
    pub real: JFieldID,
    pub payload: JFieldID,
    pub year: JFieldID,
    pub month: JFieldID,
    pub day: JFieldID,
    pub hour: JFieldID,
    pub minute: JFieldID,
    pub second: JFieldID,
    pub precision: JFieldID,
    pub subsecond: JFieldID,
    pub source_id: JFieldID,
    pub line: JFieldID,
    pub column: JFieldID,
    pub end_line: JFieldID,
    pub end_column: JFieldID,
}

impl DescriptionFields {
    /// Resolve every field id on the description class.
    ///
    /// # Errors
    /// Any JNI lookup failure (the Java class is older than this binary).
    pub fn resolve(env: &mut JNIEnv) -> jni::errors::Result<Self> {
        let cls = env.find_class(DESCRIPTION_CLASS)?;
        let string = "Ljava/lang/String;";
        let mut field = |name: &str, sig: &str| -> jni::errors::Result<JFieldID> {
            env.get_field_id(&cls, name, sig)
        };
        Ok(Self {
            kind: field("kind", "I")?,
            id: field("id", "J")?,
            definition: field("definition", "J")?,
            classifier: field("classifier", string)?,
            name: field("name", string)?,
            user_path: field("userPath", string)?,
            integer: field("integer", "J")?,
            real: field("real", "D")?,
            payload: field("payload", string)?,
            year: field("year", "I")?,
            month: field("month", "I")?,
            day: field("day", "I")?,
            hour: field("hour", "I")?,
            minute: field("minute", "I")?,
            second: field("second", "I")?,
            precision: field("precision", "I")?,
            subsecond: field("subsecond", string)?,
            source_id: field("sourceId", string)?,
            line: field("line", "I")?,
            column: field("column", "I")?,
            end_line: field("endLine", "I")?,
            end_column: field("endColumn", "I")?,
        })
    }
}
