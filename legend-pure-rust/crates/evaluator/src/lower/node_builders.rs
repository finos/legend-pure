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

//! Hand-built M3 nodes for testing the lowering without any other graph:
//! a [`TestNode`] is a [`TypedObject`] whose properties are plain values,
//! and the `m3` constructors assemble the shapes the Java compiler
//! produces (`SimpleFunctionExpression`, `InstanceValue`, …).

use std::cell::RefCell;
use std::collections::HashMap;
use std::rc::Rc;

use im_rc::Vector as PVector;
use legend_pure_metamodel::source_info::SourceInfo;
use smol_str::SmolStr;

use crate::error::PureRuntimeError;
use crate::heap::{HeapEntry, TypedObject};
use crate::value::Value;

/// A node whose properties are given directly.
#[derive(Debug, Default)]
pub struct TestNode {
    /// Classifier user path.
    pub classifier: SmolStr,
    /// `getName()`.
    pub name: Option<SmolStr>,
    /// User path of a packageable element.
    pub user_path: Option<SmolStr>,
    /// Source span.
    pub source: Option<SourceInfo>,
    /// Property values.
    pub props: HashMap<SmolStr, PVector<Value>>,
    /// Captured variables, when the node is a lambda with its context.
    pub captured: Option<HashMap<SmolStr, PVector<Value>>>,
}

impl TestNode {
    /// A node of `classifier` with no properties.
    #[must_use]
    pub fn new(classifier: &str) -> Self {
        Self {
            classifier: SmolStr::new(classifier),
            ..Self::default()
        }
    }

    /// Set the name.
    #[must_use]
    pub fn name(mut self, name: &str) -> Self {
        self.name = Some(SmolStr::new(name));
        self
    }

    /// Set the user path (and the name to its last segment when unset).
    #[must_use]
    pub fn path(mut self, path: &str) -> Self {
        self.user_path = Some(SmolStr::new(path));
        if self.name.is_none() {
            self.name = path.rsplit("::").next().map(SmolStr::new);
        }
        self
    }

    /// Set the span.
    #[must_use]
    pub fn at(mut self, source: &str, line: u32, column: u32) -> Self {
        self.source = Some(SourceInfo::new(source, line, column, line, column));
        self
    }

    /// Set a property's values.
    #[must_use]
    pub fn prop(mut self, name: &str, values: Vec<Value>) -> Self {
        self.props
            .insert(SmolStr::new(name), values.into_iter().collect());
        self
    }

    /// Set a to-one property.
    #[must_use]
    pub fn one(self, name: &str, value: Value) -> Self {
        self.prop(name, vec![value])
    }

    /// Mark the node as a lambda with its context, capturing `values` as
    /// `name` (and listing `name` in `openVariables`).
    #[must_use]
    pub fn captured(mut self, name: &str, values: Vec<Value>) -> Self {
        self.captured
            .get_or_insert_with(HashMap::new)
            .insert(SmolStr::new(name), values.into_iter().collect());
        let open = self.props.entry(SmolStr::new("openVariables")).or_default();
        open.push_back(Value::String(SmolStr::new(name)));
        self
    }

    /// The node as a heap value.
    #[must_use]
    pub fn build(self) -> Value {
        Value::Object(Rc::new(RefCell::new(HeapEntry::Typed(Box::new(self)))))
    }
}

impl TypedObject for TestNode {
    fn classifier_path(&self) -> &str {
        &self.classifier
    }

    fn get_property(&self, name: &str) -> Option<Value> {
        self.props.get(name).and_then(|v| v.head()).cloned()
    }

    fn get_property_values(&self, name: &str) -> PVector<Value> {
        self.props.get(name).cloned().unwrap_or_default()
    }

    fn property_names(&self) -> Vec<SmolStr> {
        self.props.keys().cloned().collect()
    }

    fn set_property(&mut self, name: &str, value: Value) -> Result<(), PureRuntimeError> {
        self.props
            .entry(SmolStr::new(name))
            .or_default()
            .push_back(value);
        Ok(())
    }

    fn node_name(&self) -> Option<SmolStr> {
        self.name.clone()
    }

    fn user_path(&self) -> Option<SmolStr> {
        self.user_path.clone()
    }

    fn source_info(&self) -> Option<SourceInfo> {
        self.source.clone()
    }

    fn captured_values(&self, name: &str) -> Option<PVector<Value>> {
        self.captured
            .as_ref()
            .map(|c| c.get(name).cloned().unwrap_or_default())
    }

    fn as_any(&self) -> &dyn std::any::Any {
        self
    }

    fn as_any_mut(&mut self) -> &mut dyn std::any::Any {
        self
    }
}

/// Constructors for the M3 shapes the Java compiler produces.
pub mod m3 {
    use super::{TestNode, Value};
    use crate::m3_paths;

    /// `InstanceValue` with `values`, and a generic type / multiplicity
    /// when given.
    #[must_use]
    pub fn iv(
        values: Vec<Value>,
        generic_type: Option<Value>,
        multiplicity: Option<Value>,
    ) -> Value {
        let mut n = TestNode::new(m3_paths::INSTANCE_VALUE).prop("values", values);
        if let Some(gt) = generic_type {
            n = n.one("genericType", gt);
        }
        if let Some(m) = multiplicity {
            n = n.one("multiplicity", m);
        }
        n.build()
    }

    /// `GenericType` with a raw type (an element or a node) and type arguments.
    #[must_use]
    pub fn generic(raw_type: Option<Value>, type_arguments: Vec<Value>) -> Value {
        let mut n = TestNode::new(m3_paths::GENERIC_TYPE).prop("typeArguments", type_arguments);
        if let Some(raw) = raw_type {
            n = n.one("rawType", raw);
        }
        n.build()
    }

    /// `GenericType` whose raw type is a type parameter.
    #[must_use]
    pub fn type_parameter(name: &str) -> Value {
        let tp = TestNode::new("meta::pure::metamodel::type::generics::TypeParameter")
            .one("name", Value::String(name.into()))
            .build();
        TestNode::new(m3_paths::GENERIC_TYPE)
            .one("typeParameter", tp)
            .build()
    }

    /// An anonymous `Multiplicity` node, `upper == None` for `*`.
    #[must_use]
    pub fn mult(lower: i64, upper: Option<i64>) -> Value {
        let bound = |v: i64| {
            TestNode::new(m3_paths::MULTIPLICITY_VALUE)
                .one("value", Value::Integer(v))
                .build()
        };
        let mut n = TestNode::new(m3_paths::MULTIPLICITY).one("lowerBound", bound(lower));
        n = match upper {
            Some(u) => n.one("upperBound", bound(u)),
            None => n.one(
                "upperBound",
                TestNode::new(m3_paths::MULTIPLICITY_VALUE).build(),
            ),
        };
        n.build()
    }

    /// `SimpleFunctionExpression` calling `func` (an element or a function node).
    #[must_use]
    pub fn sfe(func: Value, function_name: &str, arguments: Vec<Value>) -> Value {
        TestNode::new(m3_paths::SIMPLE_FUNCTION_EXPRESSION)
            .one("func", func)
            .one("functionName", Value::String(function_name.into()))
            .prop("parametersValues", arguments)
            .build()
    }

    /// `VariableExpression`.
    #[must_use]
    pub fn var(name: &str) -> Value {
        TestNode::new(m3_paths::VARIABLE_EXPRESSION)
            .one("name", Value::String(name.into()))
            .build()
    }

    /// A parameter of a function type: a `VariableExpression` with a type
    /// and multiplicity.
    #[must_use]
    pub fn param(name: &str, generic_type: Value, multiplicity: Value) -> Value {
        TestNode::new(m3_paths::VARIABLE_EXPRESSION)
            .one("name", Value::String(name.into()))
            .one("genericType", generic_type)
            .one("multiplicity", multiplicity)
            .build()
    }

    /// `FunctionType` node.
    #[must_use]
    pub fn function_type(
        parameters: Vec<Value>,
        return_type: Value,
        return_multiplicity: Value,
    ) -> Value {
        TestNode::new(m3_paths::FUNCTION_TYPE)
            .prop("parameters", parameters)
            .one("returnType", return_type)
            .one("returnMultiplicity", return_multiplicity)
            .build()
    }

    /// The `classifierGenericType` a function definition carries: a generic
    /// type whose first type argument's raw type is the function type.
    #[must_use]
    pub fn classifier_generic_type(function_type: Value) -> Value {
        generic(None, vec![generic(Some(function_type), Vec::new())])
    }

    /// `LambdaFunction` with parameters (`param` nodes), a body, and the
    /// given return type / multiplicity.
    #[must_use]
    pub fn lambda(
        parameters: Vec<Value>,
        body: Vec<Value>,
        return_type: Value,
        return_multiplicity: Value,
    ) -> TestNode {
        TestNode::new(m3_paths::LAMBDA_FUNCTION)
            .name("lambda$0")
            .one(
                "classifierGenericType",
                classifier_generic_type(function_type(
                    parameters,
                    return_type,
                    return_multiplicity,
                )),
            )
            .prop("expressionSequence", body)
    }

    /// A `NativeFunction` element at `path`, named by its mangled signature.
    #[must_use]
    pub fn native_function(path: &str, simple_name: &str, function_type: Value) -> Value {
        TestNode::new(m3_paths::NATIVE_FUNCTION)
            .path(path)
            .one("functionName", Value::String(simple_name.into()))
            .one(
                "classifierGenericType",
                classifier_generic_type(function_type),
            )
            .build()
    }

    /// A `ConcreteFunctionDefinition` element at `path` with a body.
    #[must_use]
    pub fn concrete_function(
        path: &str,
        simple_name: &str,
        function_type: Value,
        body: Vec<Value>,
    ) -> Value {
        TestNode::new(m3_paths::CONCRETE_FUNCTION_DEFINITION)
            .path(path)
            .one("functionName", Value::String(simple_name.into()))
            .one(
                "classifierGenericType",
                classifier_generic_type(function_type),
            )
            .prop("expressionSequence", body)
            .build()
    }

    /// A `Property` node of a class.
    #[must_use]
    pub fn property(name: &str, generic_type: Value, multiplicity: Value) -> Value {
        TestNode::new(m3_paths::PROPERTY)
            .name(name)
            .one("name", Value::String(name.into()))
            .one("genericType", generic_type)
            .one("multiplicity", multiplicity)
            .build()
    }

    /// A `QualifiedProperty` node: its function type's first parameter is
    /// `this`, as the Java compiler writes it.
    #[must_use]
    pub fn qualified_property(
        name: &str,
        this_type: Value,
        parameters: Vec<Value>,
        return_type: Value,
        return_multiplicity: Value,
        body: Vec<Value>,
    ) -> Value {
        let one = mult(1, Some(1));
        let mut params = vec![param("this", this_type, one)];
        params.extend(parameters);
        TestNode::new(m3_paths::QUALIFIED_PROPERTY)
            .name(name)
            .one("name", Value::String(name.into()))
            .one(
                "classifierGenericType",
                classifier_generic_type(function_type(params, return_type, return_multiplicity)),
            )
            .prop("expressionSequence", body)
            .build()
    }

    /// A `Class` element at `path` with properties and qualified properties.
    #[must_use]
    pub fn class(path: &str, properties: Vec<Value>, qualified_properties: Vec<Value>) -> Value {
        TestNode::new(m3_paths::CLASS)
            .path(path)
            .prop("properties", properties)
            .prop("qualifiedProperties", qualified_properties)
            .build()
    }

    /// An `Enumeration` element at `path` with the given values; each value
    /// node is classified by the enumeration's path, as in the Java graph.
    #[must_use]
    pub fn enumeration(path: &str, values: &[&str]) -> Value {
        let nodes = values
            .iter()
            .map(|v| TestNode::new(path).name(v).build())
            .collect();
        TestNode::new(m3_paths::ENUMERATION)
            .path(path)
            .prop("values", nodes)
            .build()
    }

    /// `KeyExpression` of `^Class(key = expression)`.
    #[must_use]
    pub fn key_expression(key: &str, expression: Value, add: bool) -> Value {
        TestNode::new(m3_paths::KEY_EXPRESSION)
            .one("key", iv(vec![Value::String(key.into())], None, None))
            .one("expression", expression)
            .one("add", Value::Boolean(add))
            .build()
    }
}
