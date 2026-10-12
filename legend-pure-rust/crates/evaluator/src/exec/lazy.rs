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

//! The context a lazy native runs under inside a body: forcing one of the
//! argument expressions it was handed runs that argument's node; forcing
//! anything else runs the tree walker. A native that bound variables
//! before forcing an argument sees the tree walker for it, so a name it
//! bound shadows the body's as it would on the tree walker.

use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{ExprKind, ValueSpec};

use super::{Body, NodeIdx, Op};
use crate::context::VariableContext;
use crate::error::PureException;
use crate::eval::{EvalContext, Evaluator, FoundQp};
use crate::heap::RuntimeHeap;
use crate::hooks::EvalHooks;
use crate::native::{EvalContextTrait, Evaluated};
use crate::value::Value;

/// A lazy native's view of the evaluator while a body runs it.
pub(crate) struct LazyArgs<'a, 'm, H: EvalHooks> {
    ev: &'a mut Evaluator<'m, H>,
    body: &'a Body,
    base: usize,
    specs: &'a [ValueSpec],
    args: &'a [NodeIdx],
    /// The bindings when the native was entered.
    bindings: usize,
}

impl<'a, 'm, H: EvalHooks> LazyArgs<'a, 'm, H> {
    pub(crate) fn new(
        ev: &'a mut Evaluator<'m, H>,
        body: &'a Body,
        base: usize,
        specs: &'a [ValueSpec],
        args: &'a [NodeIdx],
    ) -> Self {
        let bindings = ev.context.binding_count();
        Self {
            ev,
            body,
            base,
            specs,
            args,
            bindings,
        }
    }

    /// Whether the variable context is as the body left it.
    fn unbound(&self) -> bool {
        self.ev.context.binding_count() == self.bindings
    }

    /// Which argument expression `spec` is, by identity.
    fn argument(&self, spec: &ValueSpec) -> Option<usize> {
        self.specs.iter().position(|s| std::ptr::eq(s, spec))
    }
}

impl<H: EvalHooks> EvalContextTrait for LazyArgs<'_, '_, H> {
    fn evaluate(&mut self, spec: &ValueSpec) -> Result<Evaluated, PureException> {
        match self.argument(spec) {
            Some(i) if self.unbound() => self
                .ev
                .run_node(self.body, self.base, self.args[i])
                .map(Evaluated::new),
            _ => self.ev.walk(spec).map(Evaluated::new),
        }
    }

    fn for_each_value(
        &mut self,
        spec: &ValueSpec,
        each: &mut dyn FnMut(Value) -> Result<(), PureException>,
    ) -> Result<(), PureException> {
        let Some(i) = self.argument(spec) else {
            return EvalContext::new(self.ev).for_each_value(spec, each);
        };
        let body = self.body;
        let node = self.args[i];
        let (element_specs, element_nodes): (&[ValueSpec], &[NodeIdx]) =
            match (&*spec.kind, &body.node(node).op) {
                (ExprKind::Collection { elements }, Op::Collection(nodes)) => (elements, nodes),
                _ => (
                    std::slice::from_ref(spec),
                    std::slice::from_ref(&self.args[i]),
                ),
            };
        for (element_spec, &element_node) in element_specs.iter().zip(element_nodes) {
            let value = if self.unbound() {
                self.ev.run_node(body, self.base, element_node)?
            } else {
                self.ev.walk(element_spec)?
            };
            match value {
                Value::Collection(items) => {
                    for item in items.iter() {
                        each(item.clone())?;
                    }
                }
                Value::Unit => {}
                single => each(single)?,
            }
        }
        Ok(())
    }

    fn context(&self) -> &VariableContext {
        &self.ev.context
    }

    fn context_mut(&mut self) -> &mut VariableContext {
        &mut self.ev.context
    }

    fn heap(&self) -> &RuntimeHeap {
        &self.ev.heap
    }

    fn heap_mut(&mut self) -> &mut RuntimeHeap {
        &mut self.ev.heap
    }

    fn call_function(&mut self, callable: &Value, args: &[Value]) -> Result<Value, PureException> {
        self.ev.apply_callable(callable, args)
    }

    fn invoke_qualified_property(
        &mut self,
        receiver: &Value,
        name: &str,
        args: &[Value],
    ) -> Result<Option<Value>, PureException> {
        EvalContext::new(self.ev).invoke_qualified_property(receiver, name, args)
    }

    fn invoke_qualified_property_found(
        &mut self,
        receiver: &Value,
        found: &FoundQp,
        args: &[Value],
    ) -> Result<Value, PureException> {
        EvalContext::new(self.ev).invoke_qualified_property_found(receiver, found, args)
    }

    fn model(&self) -> &PureModel {
        self.ev.model
    }

    fn console_output(&mut self, msg: &str) {
        self.ev.hooks.console_output(msg);
    }
}
