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

//! Running a [`Body`]: one invocation's slots are its bindings in the
//! variable context, from `base` on.

use std::collections::HashMap;

use smol_str::SmolStr;

use super::lazy::LazyArgs;
use super::{Body, LambdaOp, NativePos, NodeIdx, Op, Thunk};
use crate::error::{PureException, StackFrame};
use crate::eval::{EvalContext, Evaluator};
use crate::hooks::EvalHooks;
use crate::native::Evaluated;
use crate::value::{FunctionValue, LambdaClosure, PVector, Value};

impl<H: EvalHooks> Evaluator<'_, H> {
    /// Whether this evaluator runs built bodies: switched on, and its
    /// hooks do not observe every expression.
    #[must_use]
    pub fn exec_enabled(&self) -> bool {
        self.exec && !H::OBSERVES_EXPRESSIONS
    }

    /// Run a body's statements over the invocation whose first binding is
    /// at `base`; the value is the last statement's, `Unit` for none.
    #[inline]
    pub(crate) fn run_statements(
        &mut self,
        body: &Body,
        base: usize,
    ) -> Result<Value, PureException> {
        let Some((&last, init)) = body.statements.split_last() else {
            return Ok(Value::Unit);
        };
        for &statement in init {
            self.run_node(body, base, statement)?;
        }
        self.run_node(body, base, last)
    }

    /// Whether a closure's application can run `body`: every capture
    /// the body reads, and nothing else, was captured, and every
    /// parameter has an argument.
    #[inline]
    pub(crate) fn lambda_runs(body: &Body, closure: &LambdaClosure, args: &[Value]) -> bool {
        args.len() >= body.parameters.len()
            && closure.captures.len() == body.captures.len()
            && closure.captures.keys().all(|name| {
                body.captures
                    .iter()
                    .any(|&slot| body.slot_name(slot) == name)
            })
    }

    /// Apply a lambda body in a scope of its own: the closure's captures,
    /// in its order, then the parameters, then the statements. The caller
    /// checked [`lambda_runs`](Self::lambda_runs).
    #[inline]
    pub(crate) fn run_lambda(
        &mut self,
        body: &Body,
        closure: &LambdaClosure,
        args: &[Value],
    ) -> Result<Value, PureException> {
        self.context.push_scope();
        let base = self.context.binding_count();
        for (name, value) in &closure.captures {
            self.context.set(name.clone(), value.clone());
        }
        for (param, arg) in closure.parameters.iter().zip(args) {
            self.context.set(param.name.clone(), arg.clone());
        }
        let result = self.run_statements(body, base);
        self.context.pop_scope();
        result
    }

    /// The value of a slot of the invocation at `base`: a parameter or
    /// `let` by position, a capture by name among the captures.
    #[inline]
    fn slot_value(&self, body: &Body, base: usize, slot: super::Slot) -> &Value {
        if (slot as usize) >= body.captures.len() {
            return self.context.value_at(base + slot as usize);
        }
        let name = body.slot_name(slot);
        (base..base + body.captures.len())
            .map(|position| self.context.binding_at(position))
            .find(|(bound, _)| *bound == name)
            .map_or_else(|| unreachable!("a capture is bound"), |(_, value)| value)
    }

    /// Bind a function body's parameters in the scope the caller pushed
    /// and run its statements. The caller checked that every parameter
    /// has an argument.
    pub(crate) fn run_function(
        &mut self,
        body: &Body,
        args: &[Value],
    ) -> Result<Value, PureException> {
        let base = self.context.binding_count();
        for (&slot, arg) in body.parameters.iter().zip(args) {
            self.context.set(body.slot_name(slot).clone(), arg.clone());
        }
        self.run_statements(body, base)
    }

    /// Run one node: a constant or a slot read here, everything else in
    /// [`run_op`](Self::run_op).
    #[inline]
    pub(crate) fn run_node(
        &mut self,
        body: &Body,
        base: usize,
        idx: NodeIdx,
    ) -> Result<Value, PureException> {
        match &body.node(idx).op {
            Op::Const(value) => Ok(value.clone()),
            Op::Local(slot) => Ok(self.context.value_at(base + *slot as usize).clone()),
            Op::Captured(slot) => Ok(self.slot_value(body, base, *slot).clone()),
            _ => self.run_op(body, base, idx),
        }
    }

    #[inline(never)]
    fn run_op(&mut self, body: &Body, base: usize, idx: NodeIdx) -> Result<Value, PureException> {
        let node = body.node(idx);
        match &node.op {
            Op::Const(value) => Ok(value.clone()),
            Op::Local(slot) => Ok(self.context.value_at(base + *slot as usize).clone()),
            Op::Captured(slot) => Ok(self.slot_value(body, base, *slot).clone()),
            Op::Let { slot, value, call } => {
                let value = self
                    .run_node(body, base, *value)
                    .map_err(|e| e.with_frame(frame(body, idx, call)))?;
                debug_assert_eq!(self.context.binding_count(), base + *slot as usize);
                self.context
                    .set(body.slot_name(*slot).clone(), value.clone());
                Ok(value)
            }
            Op::If {
                cond,
                then,
                els,
                call,
            } => self
                .run_if(body, base, *cond, *then, *els)
                .map_err(|e| e.with_frame(frame(body, idx, call))),
            Op::And { lhs, rhs, call } => self
                .run_and_or(body, base, *lhs, *rhs, false)
                .map_err(|e| e.with_frame(frame(body, idx, call))),
            Op::Or { lhs, rhs, call } => self
                .run_and_or(body, base, *lhs, *rhs, true)
                .map_err(|e| e.with_frame(frame(body, idx, call))),
            Op::CallPure { target, name, args } => {
                self.run_pure(body, base, idx, *target, name, args)
            }
            Op::CallNative { native, call, args } => self
                .run_eager(body, base, *native, args)
                .map_err(|e| e.with_frame(frame(body, idx, call))),
            Op::CallLazyNative {
                native,
                call,
                specs,
                args,
            } => self
                .run_lazy(body, base, *native, specs, args)
                .map_err(|e| e.with_frame(frame(body, idx, call))),
            Op::Property { receiver, name, .. } => {
                let target = self.run_node(body, base, *receiver)?;
                self.property_of(target, name)
            }
            Op::Lambda(lambda) => Ok(Value::Function(Box::new(FunctionValue::Lambda(
                self.closure_of(body, base, lambda),
            )))),
            Op::Collection(elements) => self.run_collection(body, base, elements),
            Op::Fallback(spec) => self.walk(spec),
        }
    }

    #[inline(never)]
    fn run_pure(
        &mut self,
        body: &Body,
        base: usize,
        idx: NodeIdx,
        target: legend_pure_metamodel::ids::ElementId,
        name: &str,
        args: &[NodeIdx],
    ) -> Result<Value, PureException> {
        let mut values = Vec::with_capacity(args.len());
        for &arg in args {
            values.push(self.run_node(body, base, arg)?);
        }
        let value = self.call_user_function(target, &values, name, Some(body.span(idx)))?;
        self.back_fill_call_type_arguments(&value, body.node(idx).ty.as_deref())?;
        Ok(value)
    }

    #[inline(never)]
    fn run_lazy(
        &mut self,
        body: &Body,
        base: usize,
        native: NativePos,
        specs: &[legend_pure_metamodel::types::ValueSpec],
        args: &[NodeIdx],
    ) -> Result<Value, PureException> {
        let native = self.natives.at(native);
        let mut lazy = LazyArgs::new(self, body, base, specs, args);
        native.execute(specs, &mut lazy).map(Evaluated::into_value)
    }

    fn run_collection(
        &mut self,
        body: &Body,
        base: usize,
        elements: &[NodeIdx],
    ) -> Result<Value, PureException> {
        if elements.is_empty() {
            return Ok(Value::Unit);
        }
        let mut values = PVector::new();
        for &element in elements {
            let value = self.run_node(body, base, element)?;
            crate::eval::push_flattened(&mut values, value);
        }
        Ok(crate::eval::collection_value(values))
    }

    /// Force an eager native's arguments in order and run it on them.
    #[inline(never)]
    fn run_eager(
        &mut self,
        body: &Body,
        base: usize,
        native: NativePos,
        args: &[NodeIdx],
    ) -> Result<Value, PureException> {
        let native = self.natives.at(native);
        let result = match *args {
            [a] => {
                let a = self.run_node(body, base, a)?;
                native.execute_values(std::slice::from_ref(&a), &mut EvalContext::new(self))
            }
            [a, b] => {
                let a = self.run_node(body, base, a)?;
                let b = self.run_node(body, base, b)?;
                native.execute_values(&[a, b], &mut EvalContext::new(self))
            }
            [a, b, c] => {
                let a = self.run_node(body, base, a)?;
                let b = self.run_node(body, base, b)?;
                let c = self.run_node(body, base, c)?;
                native.execute_values(&[a, b, c], &mut EvalContext::new(self))
            }
            _ => {
                let mut values = Vec::with_capacity(args.len());
                for &arg in args {
                    values.push(self.run_node(body, base, arg)?);
                }
                native.execute_values(&values, &mut EvalContext::new(self))
            }
        };
        result.map(Evaluated::into_value)
    }

    #[inline(never)]
    fn run_if(
        &mut self,
        body: &Body,
        base: usize,
        cond: Thunk,
        then: Thunk,
        els: Thunk,
    ) -> Result<Value, PureException> {
        let cond = Evaluated::new(self.force_thunk(body, base, cond)?).as_boolean()?;
        self.force_thunk(body, base, if cond { then } else { els })
    }

    fn run_and_or(
        &mut self,
        body: &Body,
        base: usize,
        lhs: NodeIdx,
        rhs: NodeIdx,
        decided_by: bool,
    ) -> Result<Value, PureException> {
        let lhs = Evaluated::new(self.run_node(body, base, lhs)?).as_boolean()?;
        if lhs == decided_by {
            return Ok(Value::Boolean(decided_by));
        }
        let rhs = Evaluated::new(self.run_node(body, base, rhs)?).as_boolean()?;
        Ok(Value::Boolean(rhs))
    }

    /// An argument forced as `force_thunk` forces it.
    fn force_thunk(
        &mut self,
        body: &Body,
        base: usize,
        thunk: Thunk,
    ) -> Result<Value, PureException> {
        match thunk {
            Thunk::Value(idx) => self.run_node(body, base, idx),
            Thunk::Lambda(idx) => {
                let Op::Lambda(lambda) = &body.node(idx).op else {
                    return self.run_node(body, base, idx);
                };
                let closure = self.closure_of(body, base, lambda);
                self.eval_lambda_value(&closure, &[])
            }
        }
    }

    /// The closure a lambda expression makes: each variable it lists,
    /// where bound, from the enclosing body's slot or by name.
    fn closure_of(&self, body: &Body, base: usize, lambda: &LambdaOp) -> LambdaClosure {
        let mut captures = HashMap::with_capacity(lambda.free.len());
        for (name, source) in lambda.free.iter().zip(&lambda.sources) {
            let value = match source {
                Some(slot) => Some(self.slot_value(body, base, *slot)),
                None => self.context.get(name),
            };
            if let Some(value) = value {
                captures.insert(name.clone(), value.clone());
            }
        }
        LambdaClosure {
            parameters: lambda.parameters.clone(),
            body: lambda.body.clone(),
            captures,
            exec: Some(lambda.compiled.clone()),
        }
    }
}

/// The frame dispatch adds to an error raised under a native call.
fn frame(body: &Body, idx: NodeIdx, call: &SmolStr) -> StackFrame {
    StackFrame {
        function_name: call.clone(),
        source: body.span(idx).clone(),
    }
}
