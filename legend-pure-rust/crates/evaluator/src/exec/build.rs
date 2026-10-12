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
//! by name when the body binds none. An eager native that declares an
//! intrinsic becomes one when its arguments' static representations
//! select it; a representation changes through [`Op::Unbox`] and
//! [`Op::Box`] where a consumer needs another than its operand holds.

use legend_pure_metamodel::model::Element;
use legend_pure_metamodel::types::{ExprKind, FunctionCallData, Parameter, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use super::types::rep_of_type;
use super::{
    Body, BodyKind, IntrinsicOp, LambdaBody, LambdaOp, NodeIdx, Op, Rep, Slot, SlotInfo, SlotKind,
    Thunk, rep_of,
};
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
        builder.parameters(parameters);
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
        let free = crate::eval::free_variable_names(statements, parameters, &|d| self.is_let(d));
        for name in free.iter() {
            let slot = builder.bind(name.clone(), SlotKind::Capture, Rep::Value);
            builder.body.captures.push(slot);
        }
        builder.parameters(parameters);
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
        crate::eval::let_binding(data, &|d| self.is_let(d))
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
                intrinsics: 0,
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

    fn parameters(&mut self, parameters: &[Parameter]) {
        for p in parameters {
            let rep = rep_of_type(&p.type_expr, &p.multiplicity);
            let slot = self.bind(p.name.clone(), SlotKind::Parameter, rep);
            self.body.parameters.push(slot);
        }
    }

    fn bind(&mut self, name: SmolStr, kind: SlotKind, rep: Rep) -> Slot {
        let slot = Slot::try_from(self.body.slots.len()).expect("slot count fits u32");
        self.body.slots.push(SlotInfo {
            name: name.clone(),
            kind,
            rep,
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
        self.push_rep(op, spec, Rep::Value)
    }

    fn push_rep(&mut self, op: Op, spec: &ValueSpec, rep: Rep) -> NodeIdx {
        let span = u32::try_from(self.body.spans.len()).expect("span count fits u32");
        self.body.spans.push(spec.source_info.clone());
        let idx = NodeIdx::try_from(self.body.nodes.len()).expect("node count fits u32");
        self.body.nodes.push(super::Node {
            op,
            ty: spec.type_info.clone(),
            rep,
            span,
        });
        idx
    }

    /// A node over `idx` holding its value in `rep`: `idx` itself when it
    /// does, a constant read unboxed, else an [`Op::Box`] or [`Op::Unbox`]
    /// with the same type and source information.
    fn in_rep(&mut self, idx: NodeIdx, rep: Rep) -> NodeIdx {
        let node = &mut self.body.nodes[idx as usize];
        if node.rep == rep {
            return idx;
        }
        let (op, ty, span) = match (node.rep, rep) {
            (_, Rep::Value) => (Op::Box(idx), node.ty.clone(), node.span),
            (Rep::Value, _) => {
                if let Op::Const(value) = &node.op
                    && holds(value, rep)
                {
                    node.rep = rep;
                    return idx;
                }
                (Op::Unbox(idx), node.ty.clone(), node.span)
            }
            _ => {
                let boxed = self.in_rep(idx, Rep::Value);
                return self.in_rep(boxed, rep);
            }
        };
        let at = NodeIdx::try_from(self.body.nodes.len()).expect("node count fits u32");
        self.body.nodes.push(super::Node { op, ty, rep, span });
        at
    }

    fn fallback(&mut self, spec: &ValueSpec) -> NodeIdx {
        if self.binds_outside_statements(spec) {
            self.refused = true;
        }
        self.body.fallbacks += 1;
        self.push(Op::Fallback(spec.clone()), spec)
    }

    /// The node of an expression, its value boxed.
    fn expr(&mut self, spec: &ValueSpec, statement: bool) -> NodeIdx {
        let idx = self.expr_in_rep(spec, statement);
        self.in_rep(idx, Rep::Value)
    }

    /// The node of an expression, its value in the representation the
    /// node holds it in.
    fn expr_in_rep(&mut self, spec: &ValueSpec, statement: bool) -> NodeIdx {
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
                Some(slot) if (slot as usize) < self.body.captures.len() => Op::Captured(slot),
                Some(slot) => Op::Local(slot),
                None => return self.fallback(spec),
            },
            ExprKind::FunctionCall(data) => return self.call(spec, data, statement),
            ExprKind::PropertyCall(data) if data.arguments.len() == 1 => {
                let receiver = self.expr(&data.arguments[0], false);
                Op::Property {
                    receiver,
                    name: data.function_name.clone(),
                    owner: data.owner,
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
                let mark = self.body.nodes.len();
                let nodes: Vec<NodeIdx> = elements.iter().map(|e| self.expr(e, false)).collect();
                match self.constant_collection(&nodes) {
                    Some(value) => {
                        self.body.nodes.truncate(mark);
                        self.body.spans.truncate(mark);
                        Op::Const(value)
                    }
                    None => Op::Collection(nodes),
                }
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
                    let rep = rep_of(args[1].type_info.as_deref());
                    let slot = self.bind(name, SlotKind::Let, rep);
                    Op::Let {
                        slot,
                        value,
                        call: key,
                    }
                }
                ArgMode::Control(Control::If) if args.len() == 3 => {
                    let cond = self.condition(&args[0]);
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
                    let lhs = self.expr_in_rep(&args[0], false);
                    let rhs = self.expr_in_rep(&args[1], false);
                    Op::And {
                        lhs,
                        rhs,
                        call: key,
                    }
                }
                ArgMode::Control(Control::Or) if args.len() == 2 => {
                    let lhs = self.expr_in_rep(&args[0], false);
                    let rhs = self.expr_in_rep(&args[1], false);
                    Op::Or {
                        lhs,
                        rhs,
                        call: key,
                    }
                }
                ArgMode::Eager(arity) if arity.accepts(args.len()) => {
                    let nodes: Vec<NodeIdx> =
                        args.iter().map(|a| self.expr_in_rep(a, false)).collect();
                    let selected = self
                        .ev
                        .natives
                        .at(position)
                        .intrinsic()
                        .and_then(|declared| {
                            let reps: Vec<Rep> = args
                                .iter()
                                .map(|a| rep_of(a.type_info.as_deref()))
                                .collect();
                            IntrinsicOp::select(declared, &reps)
                        });
                    if let Some(op) = selected {
                        let operands = nodes
                            .iter()
                            .enumerate()
                            .map(|(i, &node)| self.in_rep(node, op.operand_rep(i)))
                            .collect();
                        self.body.intrinsics += 1;
                        return self.push_rep(
                            Op::Intrinsic {
                                op,
                                native: position,
                                call: key,
                                args: operands,
                            },
                            spec,
                            op.result_rep(),
                        );
                    }
                    let nodes = nodes
                        .into_iter()
                        .map(|node| self.in_rep(node, Rep::Value))
                        .collect();
                    Op::CallNative {
                        native: position,
                        call: key,
                        args: nodes,
                    }
                }
                ArgMode::Lazy | ArgMode::Eager(_) | ArgMode::Control(_) => {
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

    /// The value of a collection literal whose elements are all
    /// constants, flattened as the tree walker flattens it.
    fn constant_collection(&self, elements: &[NodeIdx]) -> Option<Value> {
        let mut values = crate::value::PVector::new();
        for &element in elements {
            let Op::Const(value) = &self.body.nodes[element as usize].op else {
                return None;
            };
            crate::eval::push_flattened(&mut values, value.clone());
        }
        Some(crate::eval::collection_value(values))
    }

    /// The condition of an `if`: as [`thunk`](Self::thunk), its value in
    /// the representation its node holds it in.
    fn condition(&mut self, spec: &ValueSpec) -> Thunk {
        if matches!(&*spec.kind, ExprKind::Lambda { parameters, .. } if parameters.is_empty()) {
            return self.thunk(spec);
        }
        Thunk::Value(self.expr_in_rep(spec, false))
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

/// Whether a value is held in `rep` unboxed.
fn holds(value: &Value, rep: Rep) -> bool {
    matches!(
        (value, rep),
        (Value::Integer(_), Rep::I64)
            | (Value::Float(_), Rep::F64)
            | (Value::Boolean(_), Rep::Bool)
            | (Value::String(_), Rep::Str)
    )
}
