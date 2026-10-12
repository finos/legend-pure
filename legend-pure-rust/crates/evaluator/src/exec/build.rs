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

//! A body's expression tree into a [`Body`].
//!
//! Calls resolve the way dispatch resolves them, once: by element when
//! the call names one, by registry key otherwise. A native's declared
//! [`ArgMode`] decides its operation. Variables resolve, in program
//! order, to the latest binder of their name in the body, or to a lookup
//! by name when the body binds none.

use std::collections::HashSet;

use legend_pure_metamodel::model::Element;
use legend_pure_metamodel::types::{ExprKind, FunctionCallData, Parameter, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use super::{Body, BodyKind, LambdaBody, LambdaOp, NodeIdx, Op, Slot, SlotInfo, SlotKind, Thunk};
use crate::eval::Evaluator;
use crate::hooks::EvalHooks;
use crate::native::{ArgMode, Control};
use crate::value::Value;

/// How a call resolves, as dispatch resolves it.
enum Target {
    /// A registry native at this position.
    Native(usize),
    /// A function with a Pure body.
    Pure(legend_pure_metamodel::ids::ElementId),
    /// Neither: dispatch fails, on the tree walker.
    Unresolved,
}

impl<H: EvalHooks> Evaluator<'_, H> {
    /// The body of a function: its parameters bound in order, in a scope
    /// of its own. `None` when it cannot be built.
    pub(crate) fn build_function_body(
        &self,
        parameters: &[Parameter],
        statements: &[ValueSpec],
    ) -> Option<Body> {
        let mut builder = Builder::new(self, BodyKind::Function);
        for p in parameters {
            let slot = builder.bind(p.name.clone(), SlotKind::Parameter);
            builder.body.parameters.push(slot);
        }
        builder.statements(statements)
    }

    /// The body of a lambda definition: the variables it reads and does
    /// not bind, as captures, then its parameters. `None` when it cannot
    /// be built.
    pub(crate) fn build_lambda_body(
        &self,
        parameters: &[Parameter],
        statements: &[ValueSpec],
    ) -> Option<Body> {
        let mut builder = Builder::new(self, BodyKind::Lambda);
        for name in builder.free_names(parameters, statements) {
            let slot = builder.bind(name, SlotKind::Capture);
            builder.body.captures.push(slot);
        }
        for p in parameters {
            let slot = builder.bind(p.name.clone(), SlotKind::Parameter);
            builder.body.parameters.push(slot);
        }
        builder.statements(statements)
    }

    /// Statements run in the caller's scope. `None` when they cannot be
    /// built.
    pub(crate) fn build_statements(&self, statements: &[ValueSpec]) -> Option<Body> {
        Builder::new(self, BodyKind::Statements).statements(statements)
    }

    /// How a call resolves: the registry key dispatch would use, and the
    /// target.
    fn call_target(&self, data: &FunctionCallData) -> (SmolStr, Target) {
        let key = match data.function {
            Some(id) => self.model.get_node(id).name.clone(),
            None => data.function_name.clone(),
        };
        let native = match data.function {
            Some(id) => self.native_position(id),
            None => self.natives.position(&key),
        };
        if let Some(position) = native {
            return (key, Target::Native(position));
        }
        let target = match data.function {
            Some(id) if !matches!(self.model.get_element(id), Element::Function(f) if f.is_native) => {
                Target::Pure(id)
            }
            _ => Target::Unresolved,
        };
        (key, target)
    }

    /// The name a call binds when it is a `let` of a literal name.
    fn let_binding<'s>(&self, data: &'s FunctionCallData) -> Option<&'s SmolStr> {
        if !self.is_let(data) || data.arguments.len() != 2 {
            return None;
        }
        match &*data.arguments[0].kind {
            ExprKind::StringLiteral(name) => Some(name),
            _ => None,
        }
    }

    /// Whether a call is to a native that declares [`Control::Let`].
    fn is_let(&self, data: &FunctionCallData) -> bool {
        matches!(
            self.call_target(data).1,
            Target::Native(position)
                if self.natives.at(position).arg_mode() == ArgMode::Control(Control::Let)
        )
    }
}

/// One body under construction.
struct Builder<'b, 'm, H: EvalHooks> {
    ev: &'b Evaluator<'m, H>,
    body: Body,
    /// Every binder so far, in program order: a name resolves to its
    /// latest entry.
    visible: Vec<(SmolStr, Slot)>,
    /// A binding could land where no slot says: the body is not built.
    refused: bool,
}

impl<'b, 'm, H: EvalHooks> Builder<'b, 'm, H> {
    fn new(ev: &'b Evaluator<'m, H>, kind: BodyKind) -> Self {
        Self {
            ev,
            body: Body {
                kind,
                slots: Vec::new(),
                captures: Vec::new(),
                parameters: Vec::new(),
                names: Vec::new(),
                nodes: Vec::new(),
                statements: Vec::new(),
                spans: Vec::new(),
                fallbacks: 0,
            },
            visible: Vec::new(),
            refused: false,
        }
    }

    fn statements(mut self, statements: &[ValueSpec]) -> Option<Body> {
        for statement in statements {
            let node = self.expr(statement, true);
            self.body.statements.push(node);
        }
        (!self.refused).then_some(self.body)
    }

    fn bind(&mut self, name: SmolStr, kind: SlotKind) -> Slot {
        let slot = Slot::try_from(self.body.slots.len()).expect("slot count fits u32");
        self.body.slots.push(SlotInfo {
            name: name.clone(),
            kind,
        });
        self.body.names.push((name.clone(), slot));
        self.visible.push((name, slot));
        slot
    }

    fn resolve(&self, name: &str) -> Option<Slot> {
        self.visible
            .iter()
            .rev()
            .find(|(bound, _)| bound.as_str() == name)
            .map(|(_, slot)| *slot)
    }

    fn push(&mut self, op: Op, spec: &ValueSpec) -> NodeIdx {
        let span = u32::try_from(self.body.spans.len()).expect("span count fits u32");
        self.body.spans.push(spec.source_info.clone());
        let idx = NodeIdx::try_from(self.body.nodes.len()).expect("node count fits u32");
        self.body.nodes.push(super::Node {
            op,
            ty: spec.type_info.clone(),
            span,
        });
        idx
    }

    fn fallback(&mut self, spec: &ValueSpec) -> NodeIdx {
        if self.binds_outside_statements(spec) {
            self.refused = true;
        }
        self.body.fallbacks += 1;
        self.push(Op::Fallback(spec.clone()), spec)
    }

    fn expr(&mut self, spec: &ValueSpec, statement: bool) -> NodeIdx {
        let op = match &*spec.kind {
            ExprKind::IntegerLiteral(n) => Op::Const(Value::Integer(*n)),
            ExprKind::FloatLiteral(n) => Op::Const(Value::Float(*n)),
            ExprKind::DecimalLiteral(d) => Op::Const(Value::Decimal(*d)),
            ExprKind::StringLiteral(s) => Op::Const(Value::String(s.clone())),
            ExprKind::BooleanLiteral(b) => Op::Const(Value::Boolean(*b)),
            ExprKind::DateLiteral(dv) => match self.ev.eval_date_literal(dv) {
                Ok(value) => Op::Const(value),
                Err(_) => return self.fallback(spec),
            },
            ExprKind::Variable { name } => match self.resolve(name) {
                Some(slot) => Op::Local(slot),
                None => return self.fallback(spec),
            },
            ExprKind::FunctionCall(data) => return self.call(spec, data, statement),
            ExprKind::PropertyCall(data) if data.arguments.len() == 1 => {
                let receiver = self.expr(&data.arguments[0], false);
                Op::Property {
                    receiver,
                    name: data.function_name.clone(),
                }
            }
            ExprKind::EnumValue {
                enum_element,
                value,
            } => Op::Const(Value::EnumValue {
                enum_id: *enum_element,
                member: value.clone(),
            }),
            ExprKind::Lambda {
                parameters,
                body,
                free,
            } => Op::Lambda(Box::new(LambdaOp {
                parameters: parameters.clone(),
                body: body.clone(),
                free: free.clone(),
                sources: free.iter().map(|name| self.resolve(name)).collect(),
                compiled: std::rc::Rc::new(LambdaBody::new()),
            })),
            ExprKind::Collection { elements } => {
                let nodes = elements.iter().map(|e| self.expr(e, false)).collect();
                Op::Collection(nodes)
            }
            ExprKind::TypeReference { type_expr } => match type_expr {
                TypeExpr::Named { element, .. } => Op::Const(Value::Element(*element)),
                _ => Op::Const(Value::Unit),
            },
            ExprKind::PackageableElementRef { element } => Op::Const(Value::Element(*element)),
            ExprKind::PropertyCall(_)
            | ExprKind::QualifiedPropertyCall(_)
            | ExprKind::MultiplicityReference { .. }
            | ExprKind::KeyExpression { .. }
            | ExprKind::RelationLiteral { .. }
            | ExprKind::ColSpecArrayLiteral { .. }
            | ExprKind::ColSpecLiteral { .. }
            | ExprKind::PathLiteral { .. } => return self.fallback(spec),
        };
        self.push(op, spec)
    }

    fn call(&mut self, spec: &ValueSpec, data: &FunctionCallData, statement: bool) -> NodeIdx {
        let (key, target) = self.ev.call_target(data);
        let args = &data.arguments;
        let op = match target {
            Target::Native(position) => match self.ev.natives.at(position).arg_mode() {
                ArgMode::Control(Control::Let) => {
                    let name = match (statement, self.ev.let_binding(data)) {
                        (true, Some(name)) => name.clone(),
                        _ => {
                            self.refused = true;
                            return self.fallback(spec);
                        }
                    };
                    let value = self.expr(&args[1], false);
                    let slot = self.bind(name, SlotKind::Let);
                    Op::Let {
                        slot,
                        value,
                        call: key,
                    }
                }
                ArgMode::Control(Control::If) if args.len() == 3 => {
                    let cond = self.thunk(&args[0]);
                    let then = self.thunk(&args[1]);
                    let els = self.thunk(&args[2]);
                    Op::If {
                        cond,
                        then,
                        els,
                        call: key,
                    }
                }
                ArgMode::Control(Control::And) if args.len() == 2 => {
                    let lhs = self.expr(&args[0], false);
                    let rhs = self.expr(&args[1], false);
                    Op::And {
                        lhs,
                        rhs,
                        call: key,
                    }
                }
                ArgMode::Control(Control::Or) if args.len() == 2 => {
                    let lhs = self.expr(&args[0], false);
                    let rhs = self.expr(&args[1], false);
                    Op::Or {
                        lhs,
                        rhs,
                        call: key,
                    }
                }
                ArgMode::Lazy | ArgMode::Control(_) => {
                    let nodes = args.iter().map(|a| self.expr(a, false)).collect();
                    Op::CallLazyNative {
                        native: position,
                        call: key,
                        specs: args.clone(),
                        args: nodes,
                    }
                }
            },
            Target::Pure(target) => {
                let nodes = args.iter().map(|a| self.expr(a, false)).collect();
                Op::CallPure {
                    target,
                    name: data.function_name.clone(),
                    args: nodes,
                }
            }
            Target::Unresolved => return self.fallback(spec),
        };
        self.push(op, spec)
    }

    /// An argument forced through `force_thunk`: a lambda without
    /// parameters is applied, anything else is its value.
    fn thunk(&mut self, spec: &ValueSpec) -> Thunk {
        let is_lambda0 =
            matches!(&*spec.kind, ExprKind::Lambda { parameters, .. } if parameters.is_empty());
        let node = self.expr(spec, false);
        if is_lambda0 {
            Thunk::Lambda(node)
        } else {
            Thunk::Value(node)
        }
    }

    /// Whether a binding made while the tree walker runs `spec` would
    /// land in the body's own scope: a `let` anywhere in it outside a
    /// lambda.
    fn binds_outside_statements(&self, spec: &ValueSpec) -> bool {
        let mut found = false;
        visit_outside_lambdas(spec, &mut |s| {
            if let ExprKind::FunctionCall(data) = &*s.kind
                && self.ev.is_let(data)
            {
                found = true;
            }
        });
        found
    }

    /// The variables a lambda body reads and binds neither as a
    /// parameter nor by a `let` before the read, sorted.
    fn free_names(&self, parameters: &[Parameter], statements: &[ValueSpec]) -> Vec<SmolStr> {
        let mut binders: HashSet<SmolStr> = parameters.iter().map(|p| p.name.clone()).collect();
        let mut free: HashSet<SmolStr> = HashSet::new();
        for statement in statements {
            self.walk_free(statement, &mut binders, &mut free);
        }
        let mut names: Vec<SmolStr> = free.into_iter().collect();
        names.sort_unstable();
        names
    }

    fn walk_free(
        &self,
        spec: &ValueSpec,
        binders: &mut HashSet<SmolStr>,
        free: &mut HashSet<SmolStr>,
    ) {
        match &*spec.kind {
            ExprKind::Variable { name } => {
                if !binders.contains(name) {
                    free.insert(name.clone());
                }
            }
            ExprKind::FunctionCall(data) => {
                if let Some(name) = self.ev.let_binding(data) {
                    self.walk_free(&data.arguments[1], binders, free);
                    binders.insert(name.clone());
                    return;
                }
                for arg in &data.arguments {
                    self.walk_free(arg, binders, free);
                }
            }
            ExprKind::Lambda {
                parameters, body, ..
            } => {
                let mut inner = binders.clone();
                for p in parameters.iter() {
                    inner.insert(p.name.clone());
                }
                for statement in body.iter() {
                    self.walk_free(statement, &mut inner, free);
                }
            }
            ExprKind::PropertyCall(data) | ExprKind::QualifiedPropertyCall(data) => {
                for arg in &data.arguments {
                    self.walk_free(arg, binders, free);
                }
            }
            ExprKind::KeyExpression { expression, .. } => {
                self.walk_free(expression, binders, free);
            }
            ExprKind::Collection { elements } => {
                for e in elements {
                    self.walk_free(e, binders, free);
                }
            }
            ExprKind::PathLiteral { steps, .. } => {
                for step in steps {
                    for p in &step.parameters {
                        self.walk_free(p, binders, free);
                    }
                }
            }
            ExprKind::IntegerLiteral(_)
            | ExprKind::FloatLiteral(_)
            | ExprKind::DecimalLiteral(_)
            | ExprKind::StringLiteral(_)
            | ExprKind::BooleanLiteral(_)
            | ExprKind::DateLiteral(_)
            | ExprKind::EnumValue { .. }
            | ExprKind::TypeReference { .. }
            | ExprKind::MultiplicityReference { .. }
            | ExprKind::PackageableElementRef { .. }
            | ExprKind::RelationLiteral { .. }
            | ExprKind::ColSpecArrayLiteral { .. }
            | ExprKind::ColSpecLiteral { .. } => {}
        }
    }
}

/// Visit `spec` and every expression under it the tree walker evaluates
/// in the same scope: everything but the bodies of lambdas.
fn visit_outside_lambdas(spec: &ValueSpec, visit: &mut dyn FnMut(&ValueSpec)) {
    visit(spec);
    match &*spec.kind {
        ExprKind::FunctionCall(data)
        | ExprKind::PropertyCall(data)
        | ExprKind::QualifiedPropertyCall(data) => {
            for arg in &data.arguments {
                visit_outside_lambdas(arg, visit);
            }
        }
        ExprKind::KeyExpression { expression, .. } => visit_outside_lambdas(expression, visit),
        ExprKind::Collection { elements } => {
            for e in elements {
                visit_outside_lambdas(e, visit);
            }
        }
        ExprKind::PathLiteral { steps, .. } => {
            for step in steps {
                for p in &step.parameters {
                    visit_outside_lambdas(p, visit);
                }
            }
        }
        ExprKind::IntegerLiteral(_)
        | ExprKind::FloatLiteral(_)
        | ExprKind::DecimalLiteral(_)
        | ExprKind::StringLiteral(_)
        | ExprKind::BooleanLiteral(_)
        | ExprKind::DateLiteral(_)
        | ExprKind::Variable { .. }
        | ExprKind::EnumValue { .. }
        | ExprKind::Lambda { .. }
        | ExprKind::TypeReference { .. }
        | ExprKind::MultiplicityReference { .. }
        | ExprKind::PackageableElementRef { .. }
        | ExprKind::RelationLiteral { .. }
        | ExprKind::ColSpecArrayLiteral { .. }
        | ExprKind::ColSpecLiteral { .. } => {}
    }
}
