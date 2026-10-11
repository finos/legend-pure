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

//! Expression nodes into [`ValueSpec`]s.

use legend_pure_metamodel::types::{
    DateValue, ExprKind, FunctionCallData, Parameter, ResolvedType, ValueSpec,
};
use smol_str::SmolStr;

use super::{LowerError, Lowering, Node};
use crate::value::Value;

impl Lowering<'_> {
    /// A `LambdaFunction` node's parameters and body.
    ///
    /// # Errors
    /// Something in the body cannot be lowered.
    pub fn lower_lambda_parts(
        &mut self,
        node: &Node,
    ) -> Result<(Vec<Parameter>, Vec<ValueSpec>), LowerError> {
        let mut parameters = Vec::new();
        if let Some(ft) = self.function_type_node(node) {
            for p in ft.nodes("parameters") {
                let name = p
                    .string("name")
                    .ok_or_else(|| Self::read_error(&p, "missing parameter name"))?;
                let type_expr = self.parameter_type(&p)?;
                let multiplicity = self.parameter_multiplicity(&p)?;
                parameters.push(Parameter {
                    name,
                    type_expr,
                    multiplicity,
                    source_info: p.span(),
                });
            }
        }
        let mut body = Vec::new();
        for e in node.nodes("expressionSequence") {
            body.push(self.lower_expr(&e)?);
        }
        Ok((parameters, body))
    }

    /// A `ValueSpecification` node into a spec.
    ///
    /// # Errors
    /// The node is not an expression kind the lowering knows.
    pub fn lower_expr(&mut self, node: &Node) -> Result<ValueSpec, LowerError> {
        if let Some(spec) = self.specs.get(&node.key()) {
            return Ok(spec.clone());
        }
        let spec = if self.is(node, self.ids.instance_value) {
            self.lower_instance_value(node)?
        } else if self.is(node, self.ids.simple_function_expression) {
            self.lower_function_expression(node)?
        } else if self.is(node, self.ids.variable_expression) {
            let name = node
                .string("name")
                .ok_or_else(|| Self::read_error(node, "missing variable name"))?;
            let type_info = self.resolved_type(node)?;
            self.spec(ExprKind::Variable { name }, node.span(), type_info)
        } else {
            return Err(Self::unsupported(node, "as an expression"));
        };
        self.specs.insert(node.key(), spec.clone());
        Ok(spec)
    }

    fn lower_instance_value(&mut self, node: &Node) -> Result<ValueSpec, LowerError> {
        let values = node.prop("values");
        let span = node.span();
        // The type first: an enum value's enumeration is reached through it.
        let type_info = self.resolved_type(node)?;
        let kind = match values.len() {
            0 => return self.lower_type_literal(node, type_info),
            1 => {
                let v = values.head().cloned().expect("one value");
                return self.lower_value_as_spec(&v, &span, type_info);
            }
            _ => {
                let mut elements = Vec::with_capacity(values.len());
                for v in values.iter() {
                    elements.push(self.lower_value_as_spec(v, &span, None)?);
                }
                ExprKind::Collection { elements }
            }
        };
        Ok(self.spec(kind, span, type_info))
    }

    /// An `InstanceValue` with no values denotes a type (`@T`, the class
    /// of `^Class(...)`) or the empty collection (generic type `Nil`).
    fn lower_type_literal(
        &mut self,
        node: &Node,
        type_info: Option<Box<ResolvedType>>,
    ) -> Result<ValueSpec, LowerError> {
        let span = node.span();
        let Some(generic_type) = node.node("genericType") else {
            return Ok(self.spec(
                ExprKind::Collection {
                    elements: Vec::new(),
                },
                span,
                type_info,
            ));
        };
        let raw = generic_type.one("rawType");
        if let Some(Value::Element(id)) = raw
            && Some(id) == self.ids.nil
        {
            return Ok(self.spec(
                ExprKind::Collection {
                    elements: Vec::new(),
                },
                span,
                type_info,
            ));
        }
        // `Class<X>` with no values is the type literal `X` (what `new`
        // receives); its own type stays `Class<X>` for the back-fill.
        let type_arguments = generic_type.nodes("typeArguments");
        let type_expr = if let (Some(Value::Element(id)), 1) = (&raw, type_arguments.len())
            && Some(*id) == self.ids.class
        {
            self.lower_generic_type(&Value::Object(type_arguments[0].handle().clone()))?
        } else {
            self.lower_generic_type(&Value::Object(generic_type.handle().clone()))?
        };
        Ok(self.spec(ExprKind::TypeReference { type_expr }, span, type_info))
    }

    /// One value of an `InstanceValue` into a spec.
    fn lower_value_as_spec(
        &mut self,
        value: &Value,
        span: &legend_pure_metamodel::source_info::SourceInfo,
        type_info: Option<Box<ResolvedType>>,
    ) -> Result<ValueSpec, LowerError> {
        let kind = match value {
            Value::Integer(i) => ExprKind::IntegerLiteral(*i),
            Value::Float(f) => ExprKind::FloatLiteral(*f),
            Value::Decimal(d) => ExprKind::DecimalLiteral(*d),
            Value::String(s) => ExprKind::StringLiteral(s.clone()),
            Value::Boolean(b) => ExprKind::BooleanLiteral(*b),
            Value::Date(d) => ExprKind::DateLiteral(d.to_date_value()),
            Value::StrictTime(t) => ExprKind::DateLiteral(t.to_date_value()),
            Value::Latest => ExprKind::DateLiteral(DateValue::Latest),
            Value::Element(id) => ExprKind::PackageableElementRef { element: *id },
            Value::EnumValue { enum_id, member } => ExprKind::EnumValue {
                enum_element: *enum_id,
                value: member.clone(),
            },
            Value::Object(h) => {
                let node = Node::new(h.clone());
                if self.is(&node, self.ids.lambda_function) {
                    let (parameters, body) = self.lower_lambda_parts(&node)?;
                    ExprKind::Lambda { parameters, body }
                } else if self.is(&node, self.ids.key_expression) {
                    return self.lower_key_expression(&node);
                } else if self.is(&node, self.ids.instance_value)
                    || self.is(&node, self.ids.simple_function_expression)
                    || self.is(&node, self.ids.variable_expression)
                {
                    return self.lower_expr(&node);
                } else if let Some(enum_id) = self.enumeration_of(&node) {
                    ExprKind::EnumValue {
                        enum_element: enum_id,
                        value: node
                            .name()
                            .ok_or_else(|| Self::read_error(&node, "missing enum value name"))?,
                    }
                } else if node.user_path().is_some() {
                    ExprKind::PackageableElementRef {
                        element: self.element_for(value)?,
                    }
                } else {
                    return Err(Self::unsupported(&node, "as a value"));
                }
            }
            Value::Collection(_)
            | Value::Function(_)
            | Value::Map(_)
            | Value::Unit
            | Value::UnitInstance { .. } => {
                return Err(LowerError::Unsupported(format!(
                    "lowering a {} as a value",
                    value.type_name()
                )));
            }
        };
        Ok(self.spec(kind, span.clone(), type_info))
    }

    fn lower_key_expression(&mut self, node: &Node) -> Result<ValueSpec, LowerError> {
        let key = node
            .node("key")
            .and_then(|k| Self::string_value(k.prop("values").head()))
            .ok_or_else(|| Self::read_error(node, "missing key"))?;
        let expression = node
            .node("expression")
            .ok_or_else(|| Self::read_error(node, "missing expression"))?;
        let expression = self.lower_expr(&expression)?;
        let add = matches!(node.one("add"), Some(Value::Boolean(true)));
        Ok(self.spec(
            ExprKind::KeyExpression {
                key,
                expression,
                add,
            },
            node.span(),
            None,
        ))
    }

    fn lower_function_expression(&mut self, node: &Node) -> Result<ValueSpec, LowerError> {
        let func = node
            .one("func")
            .ok_or_else(|| Self::read_error(node, "missing func"))?;
        if let Value::Object(h) = &func {
            let f = Node::new(h.clone());
            let qualified = if self.is(&f, self.ids.property) {
                Some(false)
            } else if self.is(&f, self.ids.qualified_property) {
                Some(true)
            } else {
                None
            };
            if let Some(qualified) = qualified {
                return self.lower_property_call(node, &f, qualified);
            }
        }
        let function = self.element_for(&func)?;
        let function_name = match node.string("functionName") {
            Some(name) => name,
            None => {
                let mangled = match &func {
                    Value::Object(h) => Node::new(h.clone()).name().unwrap_or_default(),
                    _ => self.model.get_node(function).name.clone(),
                };
                SmolStr::new(mangled.split('_').next().unwrap_or(""))
            }
        };
        let mut arguments = Vec::new();
        for arg in node.nodes("parametersValues") {
            arguments.push(self.lower_expr(&arg)?);
        }
        let type_info = self.resolved_type(node)?;
        Ok(self.spec(
            ExprKind::FunctionCall(FunctionCallData {
                function: Some(function),
                function_name,
                arguments,
            }),
            node.span(),
            type_info,
        ))
    }

    /// `$x.prop` / `$x.qp(args)`: the receiver is the first argument. The
    /// owning class is reached through the receiver's type, so its
    /// (qualified) properties are in the model when the call runs.
    fn lower_property_call(
        &mut self,
        node: &Node,
        property: &Node,
        qualified: bool,
    ) -> Result<ValueSpec, LowerError> {
        let function_name = property
            .string("name")
            .ok_or_else(|| Self::read_error(property, "missing property name"))?;
        let mut arguments = Vec::new();
        for arg in node.nodes("parametersValues") {
            arguments.push(self.lower_expr(&arg)?);
        }
        let type_info = self.resolved_type(node)?;
        let data = FunctionCallData {
            function: None,
            function_name,
            arguments,
        };
        let kind = if qualified {
            ExprKind::QualifiedPropertyCall(data)
        } else {
            ExprKind::PropertyCall(data)
        };
        Ok(self.spec(kind, node.span(), type_info))
    }

    /// The enumeration a node is a value of, when its classifier is an
    /// enumeration the model or this lowering knows.
    pub(crate) fn enumeration_of(
        &self,
        node: &Node,
    ) -> Option<legend_pure_metamodel::ids::ElementId> {
        let id = self.classifier_id(node)?;
        self.is_enumeration(id).then_some(id)
    }
}
