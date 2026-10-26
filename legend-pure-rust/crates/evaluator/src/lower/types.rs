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

//! `GenericType` and `Multiplicity` nodes into the model's type shapes.

use legend_pure_metamodel::model::Element;
use legend_pure_metamodel::types::{Multiplicity, ResolvedType, TypeExpr};
use smol_str::SmolStr;

use super::{LowerError, Lowering, Node};
use crate::value::Value;

impl Lowering<'_> {
    /// A `GenericType` node (or an element standing for a raw type) into a
    /// [`TypeExpr`].
    ///
    /// # Errors
    /// The raw type is a node the lowering cannot turn into an element,
    /// or the node has neither a raw type nor a type parameter.
    pub fn lower_generic_type(&mut self, value: &Value) -> Result<TypeExpr, LowerError> {
        let node = match value {
            Value::Element(id) => return Ok(Self::named(*id, Vec::new(), Vec::new())),
            Value::Object(h) => Node::new(h.clone()),
            other => {
                return Err(LowerError::Read(format!(
                    "a GenericType should be a node, got {}",
                    other.type_name()
                )));
            }
        };
        match node.one("rawType").as_ref() {
            Some(Value::Element(id)) => {
                let type_arguments = self.lower_type_arguments(&node)?;
                let multiplicity_arguments = self.lower_multiplicity_arguments(&node)?;
                Ok(Self::named(*id, type_arguments, multiplicity_arguments))
            }
            Some(Value::Object(raw)) => {
                let raw = Node::new(raw.clone());
                if self.is(&raw, self.ids.function_type) {
                    return self.lower_function_type(&raw);
                }
                let element = self.element_for(&Value::Object(raw.handle().clone()))?;
                let type_arguments = self.lower_type_arguments(&node)?;
                let multiplicity_arguments = self.lower_multiplicity_arguments(&node)?;
                Ok(Self::named(element, type_arguments, multiplicity_arguments))
            }
            Some(other) => Err(LowerError::Read(format!(
                "rawType should be an element or a node, got {}",
                other.type_name()
            ))),
            None => match node.node("typeParameter").and_then(|p| p.string("name")) {
                Some(name) => Ok(TypeExpr::Generic(name)),
                None => Err(Self::unsupported(
                    &node,
                    "without a rawType or a typeParameter",
                )),
            },
        }
    }

    /// A `FunctionType` node into [`TypeExpr::FunctionType`].
    ///
    /// # Errors
    /// A parameter or the return type cannot be lowered.
    pub fn lower_function_type(&mut self, node: &Node) -> Result<TypeExpr, LowerError> {
        let mut parameters = Vec::new();
        for p in node.nodes("parameters") {
            let type_expr = self.parameter_type(&p)?;
            let multiplicity = self.parameter_multiplicity(&p)?;
            parameters.push((type_expr, multiplicity));
        }
        let return_type = match node.one("returnType") {
            Some(rt) => self.lower_generic_type(&rt)?,
            None => TypeExpr::Generic(SmolStr::new("T")),
        };
        let return_multiplicity = match node.one("returnMultiplicity") {
            Some(m) => self.lower_multiplicity(&m)?,
            None => Multiplicity::ZeroOrMany,
        };
        Ok(TypeExpr::FunctionType {
            parameters,
            return_type: Box::new(return_type),
            return_multiplicity,
        })
    }

    /// The declared type of a parameter (`VariableExpression`) node; a
    /// type that cannot be lowered becomes the parameter's own name as a
    /// type variable, never an error: parameter types are advisory.
    pub(crate) fn parameter_type(&mut self, parameter: &Node) -> Result<TypeExpr, LowerError> {
        let lowered = match parameter.one("genericType") {
            Some(gt) => {
                let r = self.lower_generic_type(&gt);
                self.advisory(r)?
            }
            None => None,
        };
        Ok(lowered.unwrap_or_else(|| {
            TypeExpr::Generic(
                parameter
                    .string("name")
                    .unwrap_or_else(|| SmolStr::new("T")),
            )
        }))
    }

    /// The declared multiplicity of a parameter node; `[*]` when absent
    /// or unreadable.
    pub(crate) fn parameter_multiplicity(
        &mut self,
        parameter: &Node,
    ) -> Result<Multiplicity, LowerError> {
        let lowered = match parameter.one("multiplicity") {
            Some(m) => {
                let r = self.lower_multiplicity(&m);
                self.advisory(r)?
            }
            None => None,
        };
        Ok(lowered.unwrap_or(Multiplicity::ZeroOrMany))
    }

    /// A `Multiplicity` node, or a `PackageableMultiplicity` element, into
    /// a [`Multiplicity`].
    ///
    /// # Errors
    /// The node has no readable bounds.
    pub fn lower_multiplicity(&mut self, value: &Value) -> Result<Multiplicity, LowerError> {
        let node = match value {
            Value::Element(id) => {
                return match self.model.get_element(*id) {
                    Element::PackageableMultiplicity(m) => Ok(m.clone()),
                    _ => Err(LowerError::Read(
                        "a multiplicity element that is not a PackageableMultiplicity".into(),
                    )),
                };
            }
            Value::Object(h) => Node::new(h.clone()),
            other => {
                return Err(LowerError::Read(format!(
                    "a Multiplicity should be a node, got {}",
                    other.type_name()
                )));
            }
        };
        if let Some(parameter) = node.string("multiplicityParameter") {
            return Ok(Multiplicity::Variable(parameter));
        }
        let bound = |name: &str| -> Result<Option<u32>, LowerError> {
            match node.node(name).and_then(|b| b.one("value")) {
                Some(Value::Integer(i)) => u32::try_from(i)
                    .map(Some)
                    .map_err(|_| LowerError::Read(format!("{name} {i} is out of range"))),
                Some(other) => Err(LowerError::Read(format!(
                    "{name}.value should be an Integer, got {}",
                    other.type_name()
                ))),
                None => Ok(None),
            }
        };
        let lower = bound("lowerBound")?.unwrap_or(0);
        let upper = bound("upperBound")?;
        Ok(match (lower, upper) {
            (1, Some(1)) => Multiplicity::PureOne,
            (0, Some(1)) => Multiplicity::ZeroOrOne,
            (0, None) => Multiplicity::ZeroOrMany,
            (1, None) => Multiplicity::OneOrMany,
            (lower, upper) => Multiplicity::Range { lower, upper },
        })
    }

    /// The static type of an expression node from its `genericType` and
    /// `multiplicity`, when both lower; `None` otherwise (types are
    /// advisory for the evaluator).
    pub(crate) fn resolved_type(
        &mut self,
        node: &Node,
    ) -> Result<Option<Box<ResolvedType>>, LowerError> {
        let (Some(gt), Some(m)) = (node.one("genericType"), node.one("multiplicity")) else {
            return Ok(None);
        };
        let type_expr = self.lower_generic_type(&gt);
        let Some(type_expr) = self.advisory(type_expr)? else {
            return Ok(None);
        };
        let multiplicity = self.lower_multiplicity(&m);
        let Some(multiplicity) = self.advisory(multiplicity)? else {
            return Ok(None);
        };
        Ok(Some(Box::new(ResolvedType {
            type_expr,
            multiplicity,
        })))
    }

    fn lower_type_arguments(&mut self, node: &Node) -> Result<Vec<TypeExpr>, LowerError> {
        node.prop("typeArguments")
            .iter()
            .map(|ta| self.lower_generic_type(ta))
            .collect()
    }

    fn lower_multiplicity_arguments(
        &mut self,
        node: &Node,
    ) -> Result<Vec<Multiplicity>, LowerError> {
        node.prop("multiplicityArguments")
            .iter()
            .map(|m| self.lower_multiplicity(m))
            .collect()
    }

    pub(crate) fn named(
        element: legend_pure_metamodel::ids::ElementId,
        type_arguments: Vec<TypeExpr>,
        multiplicity_arguments: Vec<Multiplicity>,
    ) -> TypeExpr {
        TypeExpr::Named {
            element,
            source_info: None,
            type_arguments,
            multiplicity_arguments,
            value_arguments: Vec::new(),
        }
    }
}
