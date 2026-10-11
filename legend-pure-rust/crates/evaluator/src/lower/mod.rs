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

//! Lowering: M3 nodes of another graph into the evaluator's own shapes.
//!
//! The nodes come from a Java graph through the JNI bridge today and from
//! PELT files later; both reach this module as heap objects
//! ([`TypedObject`](crate::heap::TypedObject)), the one node interface the
//! plan asks for. A lowering turns expression nodes
//! (`SimpleFunctionExpression`, `InstanceValue`, `VariableExpression`,
//! `LambdaFunction`, `KeyExpression`) into [`ValueSpec`]s and the elements
//! they reference (functions, classes, enumerations) into model elements
//! collected in a [`ModelExtension`], which the caller applies to the
//! model once the lowering is complete and nothing borrows it.
//!
//! The lowering reads what the Java compiler already resolved — calls
//! carry their target function, every expression its inferred type — and
//! recomputes nothing. A node shape it does not know is an
//! [`Unsupported`](LowerError::Unsupported) failure naming the classifier,
//! which the PCT manifests then record per test.

mod body;
mod elements;
mod extension;
mod node;
pub mod node_builders;
mod types;

use std::collections::HashMap;

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::ValueSpec;
use smol_str::SmolStr;

use crate::error::{PureException, PureRuntimeError};
use crate::m3_paths;
use crate::value::{LambdaClosure, Value};

pub use extension::ModelExtension;
pub use node::{Node, NodeKey};

/// Why a lowering stopped.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum LowerError {
    /// A node shape the lowering does not handle; names the classifier.
    Unsupported(String),
    /// The node graph could not be read (a missing slot, a wrong value kind).
    Read(String),
}

impl From<LowerError> for PureRuntimeError {
    fn from(e: LowerError) -> Self {
        match e {
            LowerError::Unsupported(what) => PureRuntimeError::Unsupported(what),
            LowerError::Read(what) => {
                PureRuntimeError::EvaluationError(format!("lowering: {what}"))
            }
        }
    }
}

impl From<LowerError> for PureException {
    fn from(e: LowerError) -> Self {
        PureException::from(PureRuntimeError::from(e))
    }
}

/// The M3 classifiers the lowering dispatches on, resolved once to the
/// model's ids so no classifier is ever compared as a string.
#[derive(Debug, Clone, Copy)]
pub(crate) struct M3Ids {
    pub class: Option<ElementId>,
    pub nil: Option<ElementId>,
    pub lambda_function: Option<ElementId>,
    pub key_expression: Option<ElementId>,
    pub instance_value: Option<ElementId>,
    pub simple_function_expression: Option<ElementId>,
    pub variable_expression: Option<ElementId>,
    pub native_function: Option<ElementId>,
    pub concrete_function_definition: Option<ElementId>,
    pub enumeration: Option<ElementId>,
    pub primitive_type: Option<ElementId>,
    pub function_type: Option<ElementId>,
    pub property: Option<ElementId>,
    pub qualified_property: Option<ElementId>,
}

impl M3Ids {
    fn resolve(model: &PureModel) -> Self {
        let r = |p| m3_paths::resolve(model, p);
        Self {
            class: r(m3_paths::CLASS),
            nil: r(m3_paths::NIL),
            lambda_function: r(m3_paths::LAMBDA_FUNCTION),
            key_expression: r(m3_paths::KEY_EXPRESSION),
            instance_value: r(m3_paths::INSTANCE_VALUE),
            simple_function_expression: r(m3_paths::SIMPLE_FUNCTION_EXPRESSION),
            variable_expression: r(m3_paths::VARIABLE_EXPRESSION),
            native_function: r(m3_paths::NATIVE_FUNCTION),
            concrete_function_definition: r(m3_paths::CONCRETE_FUNCTION_DEFINITION),
            enumeration: r(m3_paths::ENUMERATION),
            primitive_type: r(m3_paths::PRIMITIVE_TYPE),
            function_type: r(m3_paths::FUNCTION_TYPE),
            property: r(m3_paths::PROPERTY),
            qualified_property: r(m3_paths::QUALIFIED_PROPERTY),
        }
    }
}

/// One lowering over a model: reads nodes, builds specs, collects the
/// elements to add. Read-only over the model; the additions live in the
/// [`ModelExtension`] returned by [`into_extension`](Self::into_extension).
pub struct Lowering<'m> {
    model: &'m PureModel,
    ids: M3Ids,
    ext: ModelExtension,
    /// Specs already built for a node, so a node reached twice (a lambda
    /// that is both nested and captured) lowers once.
    specs: HashMap<NodeKey, ValueSpec>,
}

impl<'m> Lowering<'m> {
    /// A lowering whose new elements go to `chunk_id` starting at
    /// `base_idx` (the chunk's current length).
    #[must_use]
    pub fn new(model: &'m PureModel, chunk_id: u16, base_idx: u32) -> Self {
        Self {
            model,
            ids: M3Ids::resolve(model),
            ext: ModelExtension::new(chunk_id, base_idx),
            specs: HashMap::new(),
        }
    }

    /// The model this lowering reads.
    #[must_use]
    pub fn model(&self) -> &'m PureModel {
        self.model
    }

    /// The elements collected so far.
    #[must_use]
    pub fn extension(&self) -> &ModelExtension {
        &self.ext
    }

    /// Hand over the collected elements.
    #[must_use]
    pub fn into_extension(self) -> ModelExtension {
        self.ext
    }

    /// The root entry: a `LambdaFunction` node that is about to be run,
    /// lowered to a closure whose captures are the node's open variables
    /// read through [`TypedObject::captured_values`](crate::heap::TypedObject::captured_values).
    ///
    /// # Errors
    /// The value is not a lambda node, or something in it cannot be lowered.
    pub fn lower_root_lambda(&mut self, value: &Value) -> Result<LambdaClosure, LowerError> {
        let Value::Object(handle) = value else {
            return Err(LowerError::Read(format!(
                "the root of a lowering must be a node, got {}",
                value.type_name()
            )));
        };
        let node = Node::new(handle.clone());
        if !self.is(&node, self.ids.lambda_function) {
            return Err(LowerError::Unsupported(format!(
                "lowering {} as the root lambda",
                node.classifier()
            )));
        }
        let (parameters, body) = self.lower_lambda_parts(&node)?;
        let mut captures = HashMap::new();
        for open in node.prop("openVariables").iter() {
            let name = match open {
                Value::String(s) => s.clone(),
                other => {
                    return Err(LowerError::Read(format!(
                        "openVariables holds a {}",
                        other.type_name()
                    )));
                }
            };
            let Some(values) = node.captured_values(&name) else {
                return Err(LowerError::Unsupported(format!(
                    "lowering captured variables (`{name}` of a lambda without its context)"
                )));
            };
            let values: Vec<Value> = values.iter().cloned().collect();
            let lowered = self.lower_value(&Value::from_vec(values))?;
            captures.insert(name, lowered);
        }
        Ok(LambdaClosure {
            parameters: parameters.into(),
            body: body.into(),
            captures,
        })
    }

    /// A value read from the node graph, made evaluator-native: a lambda
    /// node becomes a closure, a node of a known element becomes that
    /// element, collections element-wise; everything else passes through.
    ///
    /// # Errors
    /// A node in the value cannot be lowered.
    pub fn lower_value(&mut self, value: &Value) -> Result<Value, LowerError> {
        match value {
            Value::Collection(items) => {
                let mut out = Vec::with_capacity(items.len());
                for item in items.iter() {
                    out.push(self.lower_value(item)?);
                }
                Ok(Value::from_vec(out))
            }
            Value::Object(handle) => {
                let node = Node::new(handle.clone());
                if self.is(&node, self.ids.lambda_function) {
                    let closure = self.lower_root_lambda(value)?;
                    return Ok(Value::Function(Box::new(
                        crate::value::FunctionValue::Lambda(closure),
                    )));
                }
                if let Some(enum_id) = self.enumeration_of(&node) {
                    let member = node.name().ok_or_else(|| {
                        LowerError::Read("an enum value node without a name".into())
                    })?;
                    return Ok(Value::EnumValue { enum_id, member });
                }
                if node.user_path().is_some() {
                    return Ok(Value::Element(self.element_for(value)?));
                }
                Ok(value.clone())
            }
            other => Ok(other.clone()),
        }
    }

    /// Resolve a node's classifier to a model element, through the model
    /// first and the pending elements second.
    pub(crate) fn classifier_id(&self, node: &Node) -> Option<ElementId> {
        let classifier = node.classifier();
        self.model
            .resolve_fqn_str(&classifier)
            .or_else(|| self.ext.lookup(&classifier))
    }

    pub(crate) fn is(&self, node: &Node, id: Option<ElementId>) -> bool {
        id.is_some() && self.classifier_id(node) == id
    }

    /// Whether an element (pending in this lowering, or in the model) is an
    /// enumeration.
    pub(crate) fn is_enumeration(&self, id: ElementId) -> bool {
        use legend_pure_metamodel::model::Element;
        if let Some(element) = self.ext.pending_element(id) {
            return matches!(element, Element::Enumeration(_));
        }
        match id {
            ElementId::InstanceId {
                chunk_id,
                local_idx,
            } if local_idx < self.model.chunk_len(chunk_id) => {
                matches!(self.model.get_element(id), Element::Enumeration(_))
            }
            _ => false,
        }
    }

    /// Types are advisory: a type that cannot be lowered is dropped, not
    /// an error — unless the failure happened while an element was being
    /// lowered (a reservation is unfilled), in which case the extension
    /// cannot be applied and the error must propagate.
    pub(crate) fn advisory<T>(
        &self,
        result: Result<T, LowerError>,
    ) -> Result<Option<T>, LowerError> {
        match result {
            Ok(t) => Ok(Some(t)),
            Err(e) if self.ext.unfilled() == 0 => {
                let _ = e;
                Ok(None)
            }
            Err(e) => Err(e),
        }
    }

    pub(crate) fn spec(
        &self,
        kind: legend_pure_metamodel::types::ExprKind,
        span: SourceInfo,
        type_info: Option<Box<legend_pure_metamodel::types::ResolvedType>>,
    ) -> ValueSpec {
        ValueSpec {
            kind: Box::new(kind),
            source_info: span,
            type_info,
        }
    }

    pub(crate) fn unsupported(node: &Node, what: &str) -> LowerError {
        LowerError::Unsupported(format!("lowering {} {what}", node.classifier()))
    }

    pub(crate) fn read_error(node: &Node, what: &str) -> LowerError {
        LowerError::Read(format!("{what} on a {}", node.classifier()))
    }

    pub(crate) fn string_value(value: Option<&Value>) -> Option<SmolStr> {
        match value {
            Some(Value::String(s)) => Some(s.clone()),
            _ => None,
        }
    }
}
