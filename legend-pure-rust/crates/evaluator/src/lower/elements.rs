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

//! Element nodes (functions; classes and enumerations follow) into model
//! elements.

use std::sync::Arc;

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::Element;
use legend_pure_metamodel::nodes::class::{Class, Property, QualifiedProperty, TypeParameter};
use legend_pure_metamodel::nodes::enumeration::{EnumValue, Enumeration};
use legend_pure_metamodel::nodes::function::Function;
use legend_pure_metamodel::types::{Multiplicity, Parameter, TypeExpr};
use smol_str::SmolStr;

use super::{LowerError, Lowering, Node};
use crate::value::Value;

impl Lowering<'_> {
    /// The model element a value stands for: an element as is; a node
    /// with a user path through the model, then the pending elements,
    /// then lowered by its classifier.
    ///
    /// # Errors
    /// The node is not a packageable element, or its kind is not lowered
    /// yet (`Unsupported("lowering <classifier>")`).
    pub fn element_for(&mut self, value: &Value) -> Result<ElementId, LowerError> {
        let node = match value {
            Value::Element(id) => return Ok(*id),
            Value::Object(h) => Node::new(h.clone()),
            other => {
                return Err(LowerError::Read(format!(
                    "an element should be a node, got {}",
                    other.type_name()
                )));
            }
        };
        let Some(path) = node.user_path() else {
            return Err(Self::unsupported(&node, "as an element (no user path)"));
        };
        let segments: Vec<SmolStr> = path.split("::").map(SmolStr::new).collect();
        if let Some(id) = self.model.resolve_by_fqn(&segments) {
            return Ok(id);
        }
        if let Some(id) = self
            .ext
            .lookup(&path)
            .or_else(|| self.ext.lookup_node(node.key()))
        {
            return Ok(id);
        }
        if self.is(&node, self.ids.native_function)
            || self.is(&node, self.ids.concrete_function_definition)
        {
            return self.lower_function(&node);
        }
        if self.is(&node, self.ids.class) {
            return self.lower_class(&node);
        }
        if self.is(&node, self.ids.enumeration) {
            return self.lower_enumeration(&node);
        }
        if self.is(&node, self.ids.primitive_type) {
            return self.lower_primitive_type(&node);
        }
        if self.is(&node, self.ids.profile) {
            return self.lower_profile(&node);
        }
        Err(Self::unsupported(&node, &format!("`{path}` as an element")))
    }

    /// A `NativeFunction` or `ConcreteFunctionDefinition` node into a
    /// [`Function`] element whose node name is the Java mangled name,
    /// verbatim: dispatch reads it as the registry key. A Pure body is
    /// lowered with it, transitively.
    ///
    /// # Errors
    /// The node has no path or name, or its body cannot be lowered.
    pub fn lower_function(&mut self, node: &Node) -> Result<ElementId, LowerError> {
        if let Some(id) = self.ext.lookup_node(node.key()) {
            return Ok(id);
        }
        let path = node
            .user_path()
            .ok_or_else(|| Self::read_error(node, "missing user path"))?;
        let mangled = node
            .name()
            .ok_or_else(|| Self::read_error(node, "missing name"))?;
        let is_native = self.is(node, self.ids.native_function);
        let id = self.ext.reserve(node, &path, mangled.clone(), node.span());
        let function_name = node
            .string("functionName")
            .unwrap_or_else(|| SmolStr::new(mangled.split('_').next().unwrap_or(mangled.as_str())));
        let (parameters, return_type, return_multiplicity) = self.function_signature(node)?;
        let mut body: Vec<legend_pure_metamodel::types::ValueSpec> = Vec::new();
        if !is_native {
            for e in node.nodes("expressionSequence") {
                body.push(self.lower_expr(&e)?);
            }
        }
        self.ext.fill(
            id,
            Element::Function(Function {
                function_name,
                is_native,
                parameters: parameters.into(),
                return_type,
                return_multiplicity,
                body: body.into(),
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
            }),
        );
        Ok(id)
    }

    /// Parameters, return type and return multiplicity from the function
    /// type a `FunctionDefinition` / `LambdaFunction` carries at
    /// `classifierGenericType.typeArguments[0].rawType`. A signature the
    /// node does not carry (some natives) is empty with a `T[*]` return.
    pub(crate) fn function_signature(
        &mut self,
        node: &Node,
    ) -> Result<(Vec<Parameter>, TypeExpr, Multiplicity), LowerError> {
        let function_type = self.function_type_node(node);
        let mut parameters = Vec::new();
        if let Some(ft) = &function_type {
            for p in ft.nodes("parameters") {
                let name = p.string("name").unwrap_or_default();
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
        let return_type = match function_type.as_ref().and_then(|ft| ft.one("returnType")) {
            Some(rt) => {
                let r = self.lower_generic_type(&rt);
                self.advisory(r)?
            }
            None => None,
        }
        .unwrap_or_else(|| TypeExpr::Generic(SmolStr::new("T")));
        let return_multiplicity = match function_type
            .as_ref()
            .and_then(|ft| ft.one("returnMultiplicity"))
        {
            Some(m) => {
                let r = self.lower_multiplicity(&m);
                self.advisory(r)?
            }
            None => None,
        }
        .unwrap_or(Multiplicity::ZeroOrMany);
        Ok((parameters, return_type, return_multiplicity))
    }

    /// A `Class` node into a [`Class`] element: type and multiplicity
    /// parameters, supertypes, properties (with lowered default values),
    /// qualified properties (bodies lowered; the leading `this` parameter
    /// of the M3 function type is the receiver, bound by the evaluator).
    ///
    /// # Errors
    /// The node has no path, or a member cannot be lowered.
    pub fn lower_class(&mut self, node: &Node) -> Result<ElementId, LowerError> {
        if let Some(id) = self.ext.lookup_node(node.key()) {
            return Ok(id);
        }
        let path = node
            .user_path()
            .ok_or_else(|| Self::read_error(node, "missing user path"))?;
        let name = node
            .name()
            .ok_or_else(|| Self::read_error(node, "missing name"))?;
        let id = self.ext.reserve(node, &path, name, node.span());
        let type_parameters = node
            .nodes("typeParameters")
            .iter()
            .filter_map(|tp| tp.string("name"))
            .map(TypeParameter::invariant)
            .collect();
        let multiplicity_parameters = node
            .nodes("multiplicityParameters")
            .iter()
            .filter_map(|mp| Self::string_value(mp.prop("values").head()))
            .collect();
        let mut super_types = Vec::new();
        for g in node.nodes("generalizations") {
            let general = g
                .one("general")
                .ok_or_else(|| Self::read_error(&g, "missing general"))?;
            super_types.push(self.lower_generic_type(&general)?);
        }
        let mut properties = Vec::new();
        for p in node.nodes("properties") {
            properties.push(self.lower_property(&p)?);
        }
        let mut qualified_properties = Vec::new();
        for qp in node.nodes("qualifiedProperties") {
            qualified_properties.push(self.lower_qualified_property(&qp)?);
        }
        self.ext.fill(
            id,
            Element::Class(Class {
                type_parameters,
                multiplicity_parameters,
                type_variable_parameters: Vec::new(),
                super_types,
                properties,
                qualified_properties,
                constraints: Vec::new(),
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
                original_milestoned_properties: Vec::new(),
            }),
        );
        Ok(id)
    }

    /// A `PrimitiveType` node declared outside the bootstrap
    /// (`Primitive X extends Integer`) into a `PrimitiveType` element
    /// whose supertype is its one generalization.
    ///
    /// # Errors
    /// The node has no path or name, or its supertype cannot be lowered;
    /// `Unsupported` for type variables, constraints or type-variable
    /// values on the supertype, which the element would otherwise lose.
    pub fn lower_primitive_type(&mut self, node: &Node) -> Result<ElementId, LowerError> {
        if let Some(id) = self.ext.lookup_node(node.key()) {
            return Ok(id);
        }
        let path = node
            .user_path()
            .ok_or_else(|| Self::read_error(node, "missing user path"))?;
        let name = node
            .name()
            .ok_or_else(|| Self::read_error(node, "missing name"))?;
        if !node.nodes("typeVariables").is_empty() || !node.nodes("constraints").is_empty() {
            return Err(Self::unsupported(
                node,
                &format!("`{path}` with type variables or constraints"),
            ));
        }
        let mut super_type = None;
        if let Some(g) = node.nodes("generalizations").first() {
            let general = g
                .one("general")
                .ok_or_else(|| Self::read_error(g, "missing general"))?;
            if let Value::Object(h) = &general
                && !Node::new(h.clone()).nodes("typeVariableValues").is_empty()
            {
                return Err(Self::unsupported(
                    node,
                    &format!("`{path}` with type-variable values on its supertype"),
                ));
            }
            match self.lower_generic_type(&general)? {
                TypeExpr::Named { element, .. } => super_type = Some(element),
                other => {
                    return Err(Self::unsupported(
                        node,
                        &format!("`{path}` extending {other:?}"),
                    ));
                }
            }
        }
        let id = self.ext.reserve(node, &path, name, node.span());
        self.ext.fill(
            id,
            Element::PrimitiveType(legend_pure_metamodel::types::PrimitiveType {
                super_type,
                super_type_value_arguments: Vec::new(),
                type_variable_parameters: Vec::new(),
                constraints: Vec::new(),
            }),
        );
        Ok(id)
    }

    /// A `Profile` node into a `Profile` element: the names of the
    /// stereotypes and tags it declares.
    ///
    /// # Errors
    /// The node has no path or name.
    pub fn lower_profile(&mut self, node: &Node) -> Result<ElementId, LowerError> {
        if let Some(id) = self.ext.lookup_node(node.key()) {
            return Ok(id);
        }
        let path = node
            .user_path()
            .ok_or_else(|| Self::read_error(node, "missing user path"))?;
        let name = node
            .name()
            .ok_or_else(|| Self::read_error(node, "missing name"))?;
        let id = self.ext.reserve(node, &path, name, node.span());
        let declared =
            |nodes: Vec<Node>| -> Vec<legend_pure_metamodel::source_info::SpannedString> {
                nodes
                    .iter()
                    .filter_map(|n| n.string("value"))
                    .map(legend_pure_metamodel::nodes::profile::bootstrap_spanned_name)
                    .collect()
            };
        self.ext.fill(
            id,
            Element::Profile(legend_pure_metamodel::nodes::profile::Profile {
                stereotypes: declared(node.nodes("p_stereotypes")),
                tags: declared(node.nodes("p_tags")),
            }),
        );
        Ok(id)
    }

    /// The stereotypes on an annotated node (`<<profile.value>>`), each
    /// resolved to its profile element.
    fn lower_stereotypes(
        &mut self,
        node: &Node,
    ) -> Result<Vec<legend_pure_metamodel::annotations::StereotypeRef>, LowerError> {
        let mut out = Vec::new();
        for s in node.nodes("stereotypes") {
            let profile = s
                .one("profile")
                .ok_or_else(|| Self::read_error(&s, "missing profile"))?;
            let value = s
                .string("value")
                .ok_or_else(|| Self::read_error(&s, "missing stereotype value"))?;
            out.push(legend_pure_metamodel::annotations::StereotypeRef {
                profile: self.element_for(&profile)?,
                value,
                source_info: None,
            });
        }
        Ok(out)
    }

    fn lower_property(&mut self, p: &Node) -> Result<Property, LowerError> {
        let name = p
            .string("name")
            .ok_or_else(|| Self::read_error(p, "missing property name"))?;
        let type_expr = match p.one("genericType") {
            Some(gt) => self.lower_generic_type(&gt)?,
            None => TypeExpr::Generic(name.clone()),
        };
        let multiplicity = match p.one("multiplicity") {
            Some(m) => self.lower_multiplicity(&m)?,
            None => Multiplicity::ZeroOrMany,
        };
        let default_value = match p.node("defaultValue") {
            Some(dv) => {
                let body = dv
                    .node("functionDefinition")
                    .map(|f| f.nodes("expressionSequence"))
                    .unwrap_or_default();
                match body.first() {
                    Some(e) => Some(self.lower_expr(e)?),
                    None => return Err(Self::unsupported(&dv, "without a function definition")),
                }
            }
            None => None,
        };
        let stereotypes = self.lower_stereotypes(p)?;
        Ok(Property {
            name,
            source_info: p.span(),
            type_expr,
            multiplicity,
            aggregation: None,
            default_value,
            stereotypes,
            tagged_values: Vec::new(),
        })
    }

    fn lower_qualified_property(&mut self, qp: &Node) -> Result<QualifiedProperty, LowerError> {
        let name = qp
            .string("name")
            .ok_or_else(|| Self::read_error(qp, "missing qualified property name"))?;
        let (mut parameters, return_type, return_multiplicity) = self.function_signature(qp)?;
        if parameters.first().is_some_and(|p| p.name == "this") {
            parameters.remove(0);
        }
        let mut body = Vec::new();
        for e in qp.nodes("expressionSequence") {
            body.push(self.lower_expr(&e)?);
        }
        Ok(QualifiedProperty {
            name,
            source_info: qp.span(),
            parameters: parameters.into(),
            return_type,
            return_multiplicity,
            body: body.into(),
            stereotypes: Vec::new(),
            tagged_values: Vec::new(),
        })
    }

    /// An `Enumeration` node into an [`Enumeration`] element.
    ///
    /// # Errors
    /// The node has no path.
    pub fn lower_enumeration(&mut self, node: &Node) -> Result<ElementId, LowerError> {
        if let Some(id) = self.ext.lookup_node(node.key()) {
            return Ok(id);
        }
        let path = node
            .user_path()
            .ok_or_else(|| Self::read_error(node, "missing user path"))?;
        let name = node
            .name()
            .ok_or_else(|| Self::read_error(node, "missing name"))?;
        let id = self.ext.reserve(node, &path, name, node.span());
        let values = node
            .nodes("values")
            .iter()
            .filter_map(|v| v.name().or_else(|| v.string("name")).map(|n| (n, v.span())))
            .map(|(name, source_info)| EnumValue {
                name,
                source_info,
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
            })
            .collect();
        self.ext.fill(
            id,
            Element::Enumeration(Enumeration {
                values,
                stereotypes: Vec::new(),
                tagged_values: Vec::new(),
            }),
        );
        Ok(id)
    }

    pub(crate) fn function_type_node(&self, node: &Node) -> Option<Node> {
        let ft = node
            .node("classifierGenericType")?
            .nodes("typeArguments")
            .into_iter()
            .next()?
            .node("rawType")?;
        self.is(&ft, self.ids.function_type).then_some(ft)
    }
}

#[allow(dead_code)]
fn _assert_arc_from_vec(v: Vec<Parameter>) -> Arc<[Parameter]> {
    v.into()
}
