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

//! Expression evaluator — the core interpreter loop.
//!
//! The evaluator walks a compiled Pure model and produces [`Value`]s.
//! It is the central component of the runtime, connecting the compiler's
//! IR to the native function registry, variable context, and object heap.
//!
//! # Architecture
//!
//! ```text
//! ┌──────────────┐     ┌──────────────┐     ┌────────────────┐
//! │  PureModel   │────▶│  Evaluator   │────▶│     Value      │
//! │ (compiler IR)│     │              │     │ (runtime value)│
//! └──────────────┘     │  ┌────────┐  │     └────────────────┘
//!                      │  │Heap    │  │
//!                      │  │Context │  │
//!                      │  │Natives │  │
//!                      │  │Hooks   │  │
//!                      │  └────────┘  │
//!                      └──────────────┘
//! ```
//!
//! # Error handling
//!
//! The evaluator uses the lazy call stack pattern: it does **not** maintain
//! a call stack during the happy path. Instead, when an error occurs, each
//! `map_err` in the recursive call chain appends a [`StackFrame`] as the
//! `Err(PureException)` propagates upward.
//!
//! # Generic hooks
//!
//! The evaluator is generic over `H: EvalHooks`, enabling zero-cost
//! instrumentation:
//! - [`NoOpHooks`] — production. All hook calls compile to nothing.

use std::collections::HashMap;

use crate::value::PVector;
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::{Element, PureModel};
use legend_pure_metamodel::source_info::SourceInfo;
use legend_pure_metamodel::types::{DateValue, ExprKind, FunctionCallData, TypeExpr, ValueSpec};
use smol_str::SmolStr;

use crate::context::VariableContext;
use crate::date::PureDate;
use crate::error::{PureException, PureExceptionKind, PureRuntimeError, StackFrame};
use crate::heap::{ObjectHandle, RuntimeHeap};
use crate::hooks::{EvalHooks, NoOpHooks};
use crate::native::{Evaluated, NativeFunction, NativeRegistry};
use crate::value::{FunctionValue, LambdaClosure, Value};

/// Evaluator state — holds mutable context during expression evaluation.
///
/// The evaluator walks the compiler's IR and produces [`Value`]s. It owns:
/// - A reference to the [`PureModel`] (compiled IR)
/// - A [`RuntimeHeap`] for object storage
/// - A [`VariableContext`] for scoped variable bindings
/// - A reference to the [`NativeRegistry`] for built-in function dispatch
/// - An [`EvalHooks`] instance for instrumentation
///
/// # Lazy call stack
///
/// Per chunk, per local index: not looked up yet, or the registry entry
/// (or its absence) for that element.
type NativeByElement<'model> = Vec<Vec<Option<Option<&'model dyn NativeFunction>>>>;

/// The evaluator has **no call stack field**. The Pure-level call stack is
/// built lazily via `map_err` only when an error propagates through the
/// recursive `eval()` calls.
///
/// # Lifetime
///
/// The `'model` lifetime ties the evaluator to the compiled model and
/// native registry, both of which are immutable during evaluation.
pub struct Evaluator<'model, H: EvalHooks = NoOpHooks> {
    /// The compiled Pure model (IR).
    model: &'model PureModel,

    /// The object heap — mutable storage for runtime objects.
    heap: RuntimeHeap,

    /// Scoped variable bindings with O(1) flat-undo.
    context: VariableContext,

    /// Immutable registry of native functions.
    natives: &'model NativeRegistry,

    /// The registry entry of each function element, found once: indexed
    /// by chunk and local index, `None` until looked up, then the entry
    /// or its absence.
    native_by_element: std::cell::RefCell<NativeByElement<'model>>,

    /// Memoised per-class member wrappers (`.properties`,
    /// `.qualifiedProperties`, `.propertiesFromAssociations`). Keyed by
    /// `(class_element_id, member-kind)`.
    member_wrapper_cache: HashMap<(ElementId, &'static str), Value>,

    /// Instrumentation hooks (zero-cost for `NoOpHooks`).
    hooks: H,

    /// Re-entry guard for the variable-snapshot renderer. While this
    /// is non-zero (the renderer is calling back into the evaluator
    /// to invoke `toRepresentation` and friends), `before_eval` is
    /// skipped so the renderer's own evaluations don't trigger a
    /// nested pause.
    rendering_depth: u32,
}

impl<'model> Evaluator<'model, NoOpHooks> {
    /// Create a new evaluator with production (no-op) hooks.
    ///
    /// The evaluator starts with an empty heap and variable context.
    #[must_use]
    pub fn new(model: &'model PureModel, natives: &'model NativeRegistry) -> Self {
        let mut heap = RuntimeHeap::new();
        heap.bootstrap_metamodel(model);
        Self {
            model,
            heap,
            context: VariableContext::new(),
            natives,
            native_by_element: std::cell::RefCell::new(Vec::new()),
            member_wrapper_cache: HashMap::new(),
            hooks: NoOpHooks,
            rendering_depth: 0,
        }
    }

    /// Start a fluent builder for an [`Evaluator`].
    ///
    /// See [`crate::builder::EvaluatorBuilder`].
    pub fn builder() -> crate::builder::EvaluatorBuilder<'model> {
        crate::builder::EvaluatorBuilder::new()
    }
}

impl<'model, H: EvalHooks> Evaluator<'model, H> {
    /// Create a new evaluator with custom hooks.
    ///
    /// Use this for debugging or instrumentation. For production use,
    /// prefer [`Evaluator::new`] which uses zero-cost [`NoOpHooks`].
    #[must_use]
    pub fn with_hooks(model: &'model PureModel, natives: &'model NativeRegistry, hooks: H) -> Self {
        let mut heap = RuntimeHeap::new();
        heap.bootstrap_metamodel(model);
        Self {
            model,
            heap,
            context: VariableContext::new(),
            natives,
            native_by_element: std::cell::RefCell::new(Vec::new()),
            member_wrapper_cache: HashMap::new(),
            hooks,
            rendering_depth: 0,
        }
    }

    /// Access the compiled model.
    #[must_use]
    pub fn model(&self) -> &'model PureModel {
        self.model
    }

    /// Access the object heap.
    #[must_use]
    pub fn heap(&self) -> &RuntimeHeap {
        &self.heap
    }

    /// Mutably access the object heap.
    pub fn heap_mut(&mut self) -> &mut RuntimeHeap {
        &mut self.heap
    }

    /// Access the variable context.
    #[must_use]
    pub fn context(&self) -> &VariableContext {
        &self.context
    }

    /// Mutably access the variable context.
    pub fn context_mut(&mut self) -> &mut VariableContext {
        &mut self.context
    }

    /// Access the native function registry.
    #[must_use]
    pub fn natives(&self) -> &NativeRegistry {
        self.natives
    }

    /// Consume the evaluator and return the hooks instance.
    #[must_use]
    pub fn into_hooks(self) -> H {
        self.hooks
    }

    /// Evaluate a compiled expression, producing a runtime value.
    ///
    /// This is the core recursive evaluator. It dispatches on `ExprKind`
    /// and produces `Value`s. Errors are wrapped with source location to
    /// form `PureException`s with lazy call stacks.
    ///
    /// # Errors
    /// Returns `PureException` on evaluation failure (type mismatch,
    /// variable not found, function not found, etc.).
    pub fn eval(&mut self, expr: &ValueSpec) -> Result<Value, PureException> {
        if self.rendering_depth == 0 {
            let want_snapshot = self.hooks.before_eval(&expr.source_info, &self.context);
            if want_snapshot {
                let source = expr.source_info.clone();
                self.dispatch_snapshot_pause(&source);
            }
        }

        let result = match &*expr.kind {
            ExprKind::IntegerLiteral(n) => Ok(Value::Integer(*n)),
            ExprKind::FloatLiteral(n) => Ok(Value::Float(*n)),
            ExprKind::DecimalLiteral(d) => Ok(Value::Decimal(*d)),
            ExprKind::StringLiteral(s) => Ok(Value::String(s.clone())),
            ExprKind::BooleanLiteral(b) => Ok(Value::Boolean(*b)),
            ExprKind::DateLiteral(dv) => self.eval_date_literal(dv),

            ExprKind::Variable { name } => self
                .context
                .require(name)
                .cloned()
                .map_err(PureException::from),

            ExprKind::FunctionCall(FunctionCallData {
                function,
                function_name,
                arguments,
            }) => self.eval_function_call(
                *function,
                function_name,
                arguments,
                &expr.source_info,
                expr.type_info.as_deref(),
            ),

            ExprKind::PropertyCall(FunctionCallData {
                function_name,
                arguments,
                ..
            }) => {
                debug_assert!(
                    arguments.len() == 1,
                    "PropertyCall must have exactly one argument (the receiver)"
                );
                self.eval_property_access(&arguments[0], function_name)
            }

            ExprKind::QualifiedPropertyCall(FunctionCallData {
                function_name,
                arguments,
                ..
            }) => {
                debug_assert!(
                    !arguments.is_empty(),
                    "QualifiedPropertyCall must carry a receiver in arguments[0]"
                );
                let (receiver, qp_args) = arguments
                    .split_first()
                    .unwrap_or_else(|| unreachable!("split_first guarded by debug_assert above"));
                self.eval_qualified_property(receiver, function_name, qp_args)
            }

            ExprKind::EnumValue {
                enum_element,
                value,
            } => Ok(Self::eval_enum_value(*enum_element, value)),

            ExprKind::Lambda {
                parameters,
                body,
                free,
            } => Ok(self.eval_lambda_creation(parameters, body, free)),

            ExprKind::Collection { elements } => self.eval_collection(elements),

            ExprKind::TypeReference { type_expr } => match type_expr {
                legend_pure_metamodel::types::TypeExpr::Named { element, .. } => {
                    Ok(Value::Element(*element))
                }
                _ => Ok(Value::Unit),
            },

            ExprKind::MultiplicityReference { multiplicity } => {
                let handle =
                    crate::native::meta::build_multiplicity_wrapper(&mut self.heap, multiplicity)?;
                Ok(Value::Object(handle))
            }
            ExprKind::KeyExpression {
                key,
                expression,
                add,
            } => {
                let values: Vec<Value> =
                    self.eval(expression)?.to_collection().into_iter().collect();
                let handle = self.heap.alloc_dynamic(crate::m3_paths::KEY_EXPRESSION);
                self.heap
                    .mutate_set(&handle, "key", &[Value::String(key.clone())])?;
                self.heap.mutate_set(&handle, "expression", &values)?;
                self.heap
                    .mutate_set(&handle, "add", &[Value::Boolean(*add)])?;
                Ok(Value::Object(handle))
            }
            ExprKind::RelationLiteral { .. } => Err(PureException::from(
                PureRuntimeError::Unsupported("relation literal `@(cols)`".into()),
            )),
            ExprKind::ColSpecArrayLiteral { .. } => Err(PureException::from(
                PureRuntimeError::Unsupported("column spec array literal `~[cols]`".into()),
            )),
            ExprKind::ColSpecLiteral { .. } => Err(PureException::from(
                PureRuntimeError::Unsupported("column spec literal `~name`".into()),
            )),
            ExprKind::PathLiteral { .. } => Err(PureException::from(
                PureRuntimeError::Unsupported("path literal `#/Type/prop#`".into()),
            )),

            ExprKind::PackageableElementRef { element } => Ok(Value::Element(*element)),
        }?;

        self.hooks.after_eval(&expr.source_info, &result);
        Ok(result)
    }

    /// Hand the hook a pause point.
    fn dispatch_snapshot_pause(&mut self, source: &SourceInfo) {
        self.rendering_depth = self.rendering_depth.saturating_add(1);
        self.hooks.pause_with_snapshot(source);
        self.rendering_depth = self.rendering_depth.saturating_sub(1);
    }

    /// Evaluate a sequence of expressions, returning the last value.
    ///
    /// # Errors
    /// Returns `PureException` if any underlying expression evaluation fails.
    pub fn eval_body(&mut self, body: &[ValueSpec]) -> Result<Value, PureException> {
        let mut result = Value::Unit;
        for expr in body {
            result = self.eval(expr)?;
        }
        Ok(result)
    }

    /// Call a Pure function by name with pre-evaluated arguments.
    ///
    /// Looks up the function in the model by fully qualified name, binds
    /// the arguments to parameters, and evaluates the body.
    ///
    /// # Errors
    /// Returns `PureException` if the function is not found or evaluation fails.
    pub fn call(&mut self, name: &str, args: &[Value]) -> Result<Value, PureException> {
        let segments: Vec<SmolStr> = name.split("::").map(SmolStr::new).collect();
        let element_id = self
            .model
            .resolve_function_by_path(&segments)
            .or_else(|| self.model.resolve_by_path(&segments))
            .ok_or_else(|| PureException::from(PureRuntimeError::FunctionNotFound(name.into())))?;

        self.call_user_function(element_id, args, name, None)
    }

    /// Call a native by its exact mangled registry key with already
    /// evaluated arguments (`plus_Number_MANY__Number_1_`). No model
    /// lookup and no prefix fallback: a miss is `FunctionNotFound(key)`.
    /// This is the entry point for callers outside the expression tree,
    /// such as the JNI boundary.
    ///
    /// # Errors
    /// Returns `PureException` if the key is not registered or the native fails.
    pub fn call_native(&mut self, key: &str, args: &[Value]) -> Result<Value, PureException> {
        let Some(native) = self.natives.get(key) else {
            return Err(PureException::from(PureRuntimeError::FunctionNotFound(
                key.into(),
            )));
        };
        self.execute_native_with_values(native, args)
    }

    /// Call a Pure function by its resolved [`ElementId`] with no arguments.
    ///
    /// # Errors
    /// Returns `PureException` if the element is not a function or evaluation fails.
    pub fn call_user_function_by_id(
        &mut self,
        element_id: ElementId,
    ) -> Result<Value, PureException> {
        let name = self.model.get_node(element_id).name.clone();
        self.call_user_function(element_id, &[], &name, None)
    }

    /// Call a Pure function by its resolved [`ElementId`] with explicit arguments.
    ///
    /// # Errors
    /// Returns `PureException` if the element is not a function or evaluation fails.
    pub fn call_user_function_by_id_with_args(
        &mut self,
        element_id: ElementId,
        args: &[Value],
    ) -> Result<Value, PureException> {
        let name = self.model.get_node(element_id).name.clone();
        self.call_user_function(element_id, args, &name, None)
    }

    #[allow(clippy::unused_self)]
    fn eval_date_literal(&self, dv: &DateValue) -> Result<Value, PureException> {
        match dv {
            DateValue::StrictDate { year, month, day } => {
                let date = match (month, day) {
                    (None, _) => PureDate::year(*year),
                    (Some(m), None) => PureDate::year_month(*year, *m),
                    (Some(m), Some(d)) => PureDate::strict_date(*year, *m, *d),
                };
                let date = date.map_err(|e| {
                    PureException::from(PureRuntimeError::EvaluationError(format!(
                        "Invalid date literal: {e}"
                    )))
                })?;
                Ok(Value::Date(date))
            }
            DateValue::DateTime {
                year,
                month,
                day,
                hour,
                minute,
                second,
                subsecond_nanos,
                subsecond_digits,
                has_minutes,
                has_seconds,
                tz_offset_minutes,
            } => {
                let civil = jiff::civil::DateTime::new(
                    *year,
                    *month,
                    *day,
                    *hour,
                    *minute,
                    *second,
                    *subsecond_nanos,
                )
                .map_err(|e| {
                    PureException::from(PureRuntimeError::EvaluationError(format!(
                        "Invalid datetime literal: {e}"
                    )))
                })?;
                let shifted = if let Some(offset) = tz_offset_minutes {
                    civil
                        .checked_sub(jiff::Span::new().minutes(i64::from(*offset)))
                        .map_err(|e| {
                            PureException::from(PureRuntimeError::EvaluationError(format!(
                                "Invalid datetime timezone shift: {e}"
                            )))
                        })?
                } else {
                    civil
                };
                let precision = if *subsecond_digits > 0 {
                    crate::date::TimePrecision::Subsecond(*subsecond_digits)
                } else if *has_seconds {
                    crate::date::TimePrecision::Second
                } else if *has_minutes {
                    crate::date::TimePrecision::Minute
                } else {
                    crate::date::TimePrecision::Hour
                };
                let date = PureDate::datetime(
                    shifted.year(),
                    shifted.month(),
                    shifted.day(),
                    shifted.hour(),
                    shifted.minute(),
                    shifted.second(),
                    shifted.subsec_nanosecond(),
                    precision,
                )
                .map_err(|e| {
                    PureException::from(PureRuntimeError::EvaluationError(format!(
                        "Invalid datetime literal: {e}"
                    )))
                })?;
                Ok(Value::Date(date))
            }
            DateValue::StrictTime {
                hour,
                minute,
                second,
                subsecond_nanos,
                subsecond_digits: _,
            } => {
                let time = crate::date::StrictTime::new(*hour, *minute, *second, *subsecond_nanos)
                    .map_err(|e| {
                        PureException::from(PureRuntimeError::EvaluationError(format!(
                            "Invalid time literal: {e}"
                        )))
                    })?;
                Ok(Value::StrictTime(time))
            }
            DateValue::Latest => Ok(Value::Latest),
        }
    }

    /// Dispatch a function call — the most complex evaluation case.
    fn eval_function_call(
        &mut self,
        function: Option<ElementId>,
        function_name: &str,
        arguments: &[ValueSpec],
        source_info: &legend_pure_metamodel::source_info::SourceInfo,
        return_type_info: Option<&legend_pure_metamodel::types::ResolvedType>,
    ) -> Result<Value, PureException> {
        let lookup_key = match function {
            Some(id) => self.model.get_node(id).name.as_str(),
            None => function_name,
        };
        let native = match function {
            Some(id) => self.native_for(id),
            None => self.natives.get(lookup_key),
        };
        if let Some(native) = native {
            return self.dispatch_native(native, lookup_key, arguments, source_info);
        }
        let is_native = match function {
            Some(id) => matches!(self.model.get_element(id), Element::Function(f) if f.is_native),
            None => true,
        };
        if !is_native && let Some(element_id) = function {
            let mut args = Vec::with_capacity(arguments.len());
            for arg in arguments {
                args.push(self.eval(arg)?);
            }
            let value =
                self.call_user_function(element_id, &args, function_name, Some(source_info))?;
            self.back_fill_call_type_arguments(&value, return_type_info)?;
            return Ok(value);
        }

        Err(PureException::execution(
            PureRuntimeError::FunctionNotFound(lookup_key.into()),
            source_info.clone(),
            Vec::new(),
        ))
    }

    /// The registry entry of a native function element, by element: the
    /// mangled name is hashed once per element for the evaluator's life,
    /// never per call. A Pure-bodied function, a package or a native the
    /// registry lacks has none.
    fn native_for(&self, id: ElementId) -> Option<&'model dyn NativeFunction> {
        let ElementId::InstanceId {
            chunk_id,
            local_idx,
        } = id
        else {
            return None;
        };
        let (chunk, index) = (usize::from(chunk_id), local_idx as usize);
        let mut cache = self.native_by_element.borrow_mut();
        if cache.len() <= chunk {
            cache.resize_with(chunk + 1, Vec::new);
        }
        let slots = &mut cache[chunk];
        if slots.len() <= index {
            slots.resize(index + 1, None);
        }
        if let Some(known) = slots[index] {
            return known;
        }
        let found = match self.model.get_element(id) {
            Element::Function(f) if f.is_native => {
                self.natives.get(self.model.get_node(id).name.as_str())
            }
            _ => None,
        };
        slots[index] = Some(found);
        found
    }

    /// Dispatch a native function using the expression-activator model.
    ///
    /// Hands the native the raw `ValueSpec` arguments unchanged — each native
    /// drives its own evaluation order through `ctx.evaluate`. Any exception
    /// the native raises is decorated with a stack frame before bubbling up.
    fn dispatch_native(
        &mut self,
        native: &dyn NativeFunction,
        lookup_key: &str,
        arguments: &[ValueSpec],
        source_info: &legend_pure_metamodel::source_info::SourceInfo,
    ) -> Result<Value, PureException> {
        let result = {
            let mut ctx = EvalContext { evaluator: self };
            native.execute(arguments, &mut ctx)
        };

        result.map(Evaluated::into_value).map_err(|e| {
            e.with_frame(StackFrame {
                function_name: lookup_key.into(),
                source: source_info.clone(),
            })
        })
    }

    /// Back-fill `__typeArguments` on a freshly returned heap object
    /// from a generic Pure-function call.
    ///
    /// Guarded by:
    /// - Returned value is `Value::Object(_)` (other shapes have no slot).
    /// - Return type is `TypeExpr::Named` with non-empty `type_arguments`.
    /// - Every `type_arguments[i]` is `TypeExpr::Named { .. }`.
    /// - The slot is empty.
    /// - The object's classifier matches the return type's outer class.
    fn back_fill_call_type_arguments(
        &mut self,
        value: &Value,
        return_type_info: Option<&legend_pure_metamodel::types::ResolvedType>,
    ) -> Result<(), PureException> {
        let Value::Object(obj_id) = value else {
            return Ok(());
        };
        let Some(rt) = return_type_info else {
            return Ok(());
        };
        let legend_pure_metamodel::types::TypeExpr::Named {
            element: return_outer,
            type_arguments,
            ..
        } = &rt.type_expr
        else {
            return Ok(());
        };
        if type_arguments.is_empty() {
            return Ok(());
        }
        if !type_arguments
            .iter()
            .all(|ta| matches!(ta, legend_pure_metamodel::types::TypeExpr::Named { .. }))
        {
            return Ok(());
        }
        let existing = self
            .heap
            .get_property_values(obj_id, "__typeArguments")
            .map_err(PureException::from)?;
        if !existing.is_empty() {
            return Ok(());
        }
        let classifier_path = self
            .heap
            .classifier(obj_id)
            .map_err(PureException::from)?
            .clone();
        let classifier_id = crate::m3_paths::resolve(self.model, &classifier_path);
        if classifier_id != Some(*return_outer) {
            return Ok(());
        }
        let type_args: Vec<Value> = type_arguments
            .iter()
            .filter_map(|ta| match ta {
                legend_pure_metamodel::types::TypeExpr::Named { element, .. } => {
                    Some(Value::Element(*element))
                }
                _ => None,
            })
            .collect();
        if !type_args.is_empty() {
            self.heap
                .mutate_set(obj_id, "__typeArguments", &type_args)
                .map_err(PureException::from)?;
        }
        Ok(())
    }

    /// Evaluate a user-defined function from the compiled model.
    fn call_user_function(
        &mut self,
        element_id: ElementId,
        args: &[Value],
        function_name: &str,
        call_site: Option<&legend_pure_metamodel::source_info::SourceInfo>,
    ) -> Result<Value, PureException> {
        let element = self.model.get_element(element_id);

        let Element::Function(func) = element else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                format!("{function_name} is not a function"),
            )));
        };

        let params = func.parameters.clone();
        let body = func.body.clone();

        self.hooks
            .enter_function(function_name, &self.model.get_node(element_id).source_info);

        self.context.push_scope();

        for (param, arg) in params.iter().zip(args.iter()) {
            self.context.set(param.name.clone(), arg.clone());
        }

        let result = self.eval_body(&body);

        self.context.pop_scope();

        self.hooks.leave_function(function_name);

        result.map_err(|e| {
            let stack_source_info = call_site
                .cloned()
                .unwrap_or_else(|| self.model.get_node(element_id).source_info.clone());

            e.with_frame(StackFrame {
                function_name: function_name.into(),
                source: stack_source_info,
            })
        })
    }

    fn eval_property_access(
        &mut self,
        target: &ValueSpec,
        property: &str,
    ) -> Result<Value, PureException> {
        let target_val = self.eval(target)?;
        match &target_val {
            Value::Object(id) => {
                let values = self
                    .heap
                    .get_property_values(&id.clone(), property)
                    .map_err(PureException::from)?;
                let collected: Vec<Value> = values.iter().cloned().collect();
                if !collected.is_empty() {
                    return Ok(Value::from_vec(collected));
                }
                if let Some(qp_result) = self.try_zero_arg_qp(id.clone(), property)? {
                    return Ok(qp_result);
                }
                if property == "elementOverride"
                    || property == "getterOverrideToOne"
                    || property == "getterOverrideToMany"
                    || property == "hiddenPayload"
                {
                    return Ok(Value::Unit);
                }
                self.try_getter_override(id.clone(), property)
            }
            Value::Element(id) => {
                let id = *id;
                if let Some(oid) = target_val.as_object_handle(&self.heap) {
                    let values = self
                        .heap
                        .get_property_values(&oid, property)
                        .map_err(PureException::from)?;
                    let collected: Vec<Value> = values.iter().cloned().collect();
                    if !collected.is_empty() {
                        return Ok(Value::from_vec(collected));
                    }
                }
                if matches!(self.model.get_element(id), Element::Function(_)) {
                    let fv = FunctionValue::Compiled(id);
                    let fn_val = Value::Function(Box::new(fv.clone()));
                    if let Ok(v) = self.eval_function_property(&fv, property, &fn_val) {
                        return Ok(v);
                    }
                }
                self.eval_element_property(id, property)
                    .map_err(PureException::from)
            }
            Value::Function(fv) => {
                if let Some(oid) = target_val.as_object_handle(&self.heap) {
                    let values = self
                        .heap
                        .get_property_values(&oid, property)
                        .map_err(PureException::from)?;
                    let collected: Vec<Value> = values.iter().cloned().collect();
                    if !collected.is_empty() {
                        return Ok(Value::from_vec(collected));
                    }
                }
                self.eval_function_property(fv, property, &target_val)
                    .map_err(PureException::from)
            }
            Value::Collection(items) => {
                let items = items.clone();
                let mut out: Vec<Value> = Vec::new();
                for item in items.iter() {
                    let v = self.property_access_on_value(item, property)?;
                    match &v {
                        Value::Unit => {}
                        Value::Collection(inner) => {
                            for x in inner.iter() {
                                out.push(x.clone());
                            }
                        }
                        _ => out.push(v),
                    }
                }
                Ok(Value::from_vec(out))
            }
            Value::Unit => Ok(Value::Unit),
            _ => Err(PureException::from(PureRuntimeError::EvaluationError(
                format!(
                    "Property access on non-object value: {}.{}",
                    target_val.type_name(),
                    property
                ),
            ))),
        }
    }

    /// Dispatch an unset property read through the instance's
    /// `GetterOverride` lambda when one is bound, returning the
    /// lambda's result. Returns `Value::Unit` when the instance has
    /// no `elementOverride`, no lambda matching the property's
    /// declared multiplicity, or the override property reference
    /// can't be resolved.
    ///
    /// Mirrors Java Pure's `GetterOverride` dispatch: when
    /// `dynamicNew(class, kvs, getterToOne, getterToMany,
    /// hiddenPayload)` constructs an instance, absent property reads
    /// invoke the appropriate lambda with `(receiver, propertyRef)`.
    /// `obj.name` ≡ `obj.name()` fallback. When the receiver's class
    /// declares a *zero-arg* qualified property of `name` (walking up
    /// the supertype chain), invoke its body with `$this` bound to the
    /// receiver and return the result. Returns `Ok(None)` when no
    /// matching QP exists so the caller can continue to other fallbacks.
    fn try_zero_arg_qp(
        &mut self,
        instance_id: ObjectHandle,
        property: &str,
    ) -> Result<Option<Value>, PureException> {
        let classifier = self
            .heap
            .classifier(&instance_id)
            .map_err(PureException::from)?
            .clone();
        let Some(mut current_eid) = self.model.resolve_fqn_str(classifier.as_str()) else {
            return Ok(None);
        };
        loop {
            let Element::Class(class) = self.model.get_element(current_eid) else {
                return Ok(None);
            };
            if let Some(qp) = class
                .qualified_properties
                .iter()
                .find(|q| q.name == property && q.parameters.is_empty())
            {
                let body = qp.body.clone();
                let type_var_param_names: Vec<SmolStr> = class
                    .type_variable_parameters
                    .iter()
                    .map(|p| p.name.clone())
                    .collect();
                let type_var_values: Vec<Value> = if type_var_param_names.is_empty() {
                    Vec::new()
                } else {
                    self.heap
                        .get_property_values(&instance_id, "__typeVariableValues")
                        .map(|v| v.iter().cloned().collect())
                        .unwrap_or_default()
                };
                self.context.push_scope();
                self.context
                    .set(SmolStr::new("this"), Value::Object(instance_id));
                for (name, value) in type_var_param_names.iter().zip(type_var_values.iter()) {
                    self.context.set(name.clone(), value.clone());
                }
                let result = self.eval_body(&body);
                self.context.pop_scope();
                return result.map(Some);
            }
            let Some(next_eid) = class.super_types.iter().find_map(|st| match st {
                legend_pure_metamodel::types::TypeExpr::Named { element, .. } => Some(*element),
                _ => None,
            }) else {
                return Ok(None);
            };
            current_eid = next_eid;
        }
    }

    fn try_getter_override(
        &mut self,
        instance_id: ObjectHandle,
        property: &str,
    ) -> Result<Value, PureException> {
        let override_vals = self
            .heap
            .get_property_values(&instance_id, "elementOverride")
            .map_err(PureException::from)?;
        let override_id = match override_vals.iter().next() {
            Some(Value::Object(h)) => h.clone(),
            _ => return Ok(Value::Unit),
        };
        let classifier = self
            .heap
            .classifier(&instance_id)
            .map_err(PureException::from)?
            .clone();
        let Some(class_id) = crate::m3_paths::resolve(self.model, &classifier) else {
            return Ok(Value::Unit);
        };
        let prop_mult: Option<&legend_pure_metamodel::types::Multiplicity> = {
            let mut found: Option<&legend_pure_metamodel::types::Multiplicity> = None;
            let mut visited: std::collections::HashSet<ElementId> =
                std::collections::HashSet::new();
            let mut stack: Vec<ElementId> = vec![class_id];
            while let Some(cid) = stack.pop() {
                if !visited.insert(cid) {
                    continue;
                }
                let Element::Class(c) = self.model.get_element(cid) else {
                    continue;
                };
                if let Some(p) = c.properties.iter().find(|p| p.name == property) {
                    found = Some(&p.multiplicity);
                    break;
                }
                for st in &c.super_types {
                    if let TypeExpr::Named { element, .. } = st {
                        stack.push(*element);
                    }
                }
            }
            found
        };
        let is_to_one = matches!(
            prop_mult,
            Some(
                legend_pure_metamodel::types::Multiplicity::PureOne
                    | legend_pure_metamodel::types::Multiplicity::ZeroOrOne
            )
        );
        let lambda_slot = if is_to_one {
            "getterOverrideToOne"
        } else {
            "getterOverrideToMany"
        };
        let lambda_vals = self
            .heap
            .get_property_values(&override_id, lambda_slot)
            .map_err(PureException::from)?;
        let Some(lambda) = lambda_vals.iter().next().cloned() else {
            return Ok(Value::Unit);
        };
        let prop_wrapper = self.alloc_property_wrapper(class_id, property)?;
        let receiver = Value::Object(instance_id);
        self.apply_callable(&lambda, &[receiver, Value::Object(prop_wrapper)])
    }

    /// Allocate a one-off `Property` heap wrapper carrying `_owner`
    /// (the owning Class element id) and `name` (the property's
    /// simple name).
    fn alloc_property_wrapper(
        &mut self,
        owner_class_id: ElementId,
        property: &str,
    ) -> Result<ObjectHandle, PureException> {
        let obj = self.heap.alloc_dynamic(crate::m3_paths::PROPERTY);
        self.heap
            .mutate_add(&obj, "_owner", &[Value::Element(owner_class_id)])
            .map_err(PureException::from)?;
        self.heap
            .mutate_add(&obj, "name", &[Value::String(SmolStr::new(property))])
            .map_err(PureException::from)?;
        Ok(obj)
    }

    /// Read `property` off a pre-evaluated [`Value`].
    fn property_access_on_value(
        &mut self,
        value: &Value,
        property: &str,
    ) -> Result<Value, PureException> {
        match value {
            Value::Object(id) => {
                let values = self
                    .heap
                    .get_property_values(&id.clone(), property)
                    .map_err(PureException::from)?;
                let collected: Vec<Value> = values.iter().cloned().collect();
                Ok(Value::from_vec(collected))
            }
            Value::Element(id) => self
                .eval_element_property(*id, property)
                .map_err(PureException::from),
            Value::Function(fv) => self
                .eval_function_property(fv, property, value)
                .map_err(PureException::from),
            Value::Collection(items) => {
                let items = items.clone();
                let mut out: Vec<Value> = Vec::new();
                for item in items.iter() {
                    let v = self.property_access_on_value(item, property)?;
                    match &v {
                        Value::Unit => {}
                        Value::Collection(inner) => {
                            for x in inner.iter() {
                                out.push(x.clone());
                            }
                        }
                        _ => out.push(v),
                    }
                }
                Ok(Value::from_vec(out))
            }
            Value::Unit => Ok(Value::Unit),
            other => Err(PureException::from(PureRuntimeError::EvaluationError(
                format!(
                    "Property access on non-object value: {}.{property}",
                    other.type_name()
                ),
            ))),
        }
    }

    /// Synthesize Function-metamodel properties for a [`Value::Function`].
    fn eval_function_property(
        &self,
        fv: &FunctionValue,
        property: &str,
        target_val: &Value,
    ) -> Result<Value, PureRuntimeError> {
        match property {
            "expressionSequence" => {
                let mut pv = PVector::new();
                pv.push_back(target_val.clone());
                Ok(Value::Collection(Box::new(pv)))
            }
            "functionName" | "name" => {
                let name = match fv {
                    FunctionValue::Compiled(id) => match self.model.get_element(*id) {
                        Element::Function(f) => f.function_name.clone(),
                        _ => self.model.element_name(*id).clone(),
                    },
                    FunctionValue::Lambda(_) => SmolStr::new_static("<lambda>"),
                    FunctionValue::Path(p) => {
                        p.name.clone().unwrap_or(SmolStr::new_static("<path>"))
                    }
                };
                Ok(Value::String(name))
            }
            other => Err(PureRuntimeError::EvaluationError(format!(
                "Property '{other}' not supported on Function value"
            ))),
        }
    }

    /// Read a property of a model-element reference (`Value::Element`).
    ///
    /// Supports the M3 meta-model properties used by the Pure-level test
    /// orchestrator (`surveyor.pure`):
    /// - `name`         → simple name (function name for Functions, not mangled)
    /// - `package`      → parent package as `Value::Element(Package(..))`, or `Unit` for root
    /// - `children`     → for packages, child sub-packages + child elements as a Collection
    /// - `stereotypes`  → for Functions, Classes, etc: a Collection of freshly-allocated
    ///   heap objects classifying as `meta::pure::metamodel::extension::Stereotype`
    ///   with `value`/`profile` properties — matches the M3 representation
    ///   surveyor expects.
    #[allow(clippy::too_many_lines)]
    fn eval_element_property(
        &mut self,
        id: ElementId,
        property: &str,
    ) -> Result<Value, PureRuntimeError> {
        if matches!(property, "upperBound" | "lowerBound")
            && let Some(bounds) = multiplicity_constant_bounds(self.model, id)
        {
            let cache_key = (
                id,
                if property == "upperBound" {
                    "_mult_upperBound"
                } else {
                    "_mult_lowerBound"
                },
            );
            if let Some(cached) = self.member_wrapper_cache.get(&cache_key) {
                return Ok(cached.clone());
            }
            let bound = if property == "upperBound" {
                bounds.1
            } else {
                Some(bounds.0)
            };
            let result = match bound {
                Some(b) => {
                    let mv = self.heap.alloc_dynamic(crate::m3_paths::MULTIPLICITY_VALUE);
                    self.heap.mutate_add(&mv, "value", &[Value::Integer(b)])?;
                    Value::from_vec(vec![Value::Object(mv)])
                }
                None => Value::from_vec(Vec::new()),
            };
            self.member_wrapper_cache.insert(cache_key, result.clone());
            return Ok(result);
        }

        if property == "classifierGenericType"
            && matches!(self.model.get_element(id), Element::Function(_))
        {
            let cache_key = (id, "_func_classifierGenericType");
            if let Some(cached) = self.member_wrapper_cache.get(&cache_key) {
                return Ok(cached.clone());
            }
            let fv = FunctionValue::Compiled(id);
            let inner_ft_obj = {
                let mut ctx = EvalContext { evaluator: self };
                crate::native::meta::build_function_type_wrapper(&mut ctx, &fv).map_err(
                    |e| match e.into_data().kind {
                        PureExceptionKind::ExecutionError(err) => err,
                        other => PureRuntimeError::EvaluationError(format!("{other:?}")),
                    },
                )?
            };
            let inner_gt = self.heap.alloc_dynamic(crate::m3_paths::GENERIC_TYPE);
            self.heap
                .mutate_add(&inner_gt, "rawType", &[Value::Object(inner_ft_obj)])?;
            let outer_gt = self.heap.alloc_dynamic(crate::m3_paths::GENERIC_TYPE);
            if let Some(ft_class_id) =
                crate::m3_paths::resolve(self.model, "meta::pure::metamodel::type::FunctionType")
            {
                self.heap
                    .mutate_add(&outer_gt, "rawType", &[Value::Element(ft_class_id)])?;
            }
            self.heap
                .mutate_add(&outer_gt, "typeArguments", &[Value::Object(inner_gt)])?;
            let result = Value::from_vec(vec![Value::Object(outer_gt)]);
            self.member_wrapper_cache.insert(cache_key, result.clone());
            return Ok(result);
        }

        match property {
            "name" | "functionName" => {
                let name = match self.model.get_element(id) {
                    Element::Function(f) => f.function_name.clone(),
                    Element::Unit(_) => {
                        let full = self.model.element_name(id);
                        full.rsplit_once('~')
                            .map_or_else(|| full.clone(), |(_, local)| SmolStr::new(local))
                    }
                    _ => self.model.element_name(id).clone(),
                };
                Ok(Value::String(name))
            }
            "package" => {
                let parent = match id {
                    ElementId::Package(pkg_id) => self
                        .model
                        .get_package(pkg_id)
                        .parent
                        .map(ElementId::Package),
                    ElementId::InstanceId { .. } => {
                        Some(ElementId::Package(self.model.get_node(id).parent_package))
                    }
                };
                match parent {
                    Some(pid) => Ok(Value::Element(pid)),
                    None => Ok(Value::Unit),
                }
            }
            "children" => match id {
                ElementId::Package(pkg_id) => {
                    let pkg = self.model.get_package(pkg_id);
                    let mut items: Vec<Value> = Vec::with_capacity(
                        pkg.children_packages.len() + pkg.children_elements.len(),
                    );
                    for &child_pkg_id in &pkg.children_packages {
                        items.push(Value::Element(ElementId::Package(child_pkg_id)));
                    }
                    for &child_eid in &pkg.children_elements {
                        if matches!(self.model.get_element(child_eid), Element::Unit(_)) {
                            continue;
                        }
                        items.push(Value::Element(child_eid));
                    }
                    Ok(Value::from_vec(items))
                }
                ElementId::InstanceId { .. } => Err(PureRuntimeError::EvaluationError(format!(
                    "Property 'children' is only valid for Package elements, got element {id}"
                ))),
            },
            "stereotypes" => {
                if let Some(cached) = self.member_wrapper_cache.get(&(id, "stereotypes")) {
                    return Ok(cached.clone());
                }
                let stereos: Vec<(ElementId, SmolStr)> = match self.model.get_element(id) {
                    Element::Function(f) => f
                        .stereotypes
                        .iter()
                        .map(|s| (s.profile, s.value.clone()))
                        .collect(),
                    Element::Class(c) => c
                        .stereotypes
                        .iter()
                        .map(|s| (s.profile, s.value.clone()))
                        .collect(),
                    _ => Vec::new(),
                };
                let mut items: Vec<Value> = Vec::with_capacity(stereos.len());
                for (profile, value) in stereos {
                    let obj = self.heap.alloc_dynamic(crate::m3_paths::STEREOTYPE);
                    self.heap
                        .mutate_add(&obj, "value", &[Value::String(value)])?;
                    self.heap
                        .mutate_add(&obj, "profile", &[Value::Element(profile)])?;
                    items.push(Value::Object(obj));
                }
                let result = Value::from_vec(items);
                self.member_wrapper_cache
                    .insert((id, "stereotypes"), result.clone());
                Ok(result)
            }
            "taggedValues" => {
                if let Some(cached) = self.member_wrapper_cache.get(&(id, "taggedValues")) {
                    return Ok(cached.clone());
                }
                let tags: Vec<(ElementId, SmolStr, String)> = match self.model.get_element(id) {
                    Element::Function(f) => f
                        .tagged_values
                        .iter()
                        .map(|t| (t.profile, t.tag.clone(), t.value.clone()))
                        .collect(),
                    Element::Class(c) => c
                        .tagged_values
                        .iter()
                        .map(|t| (t.profile, t.tag.clone(), t.value.clone()))
                        .collect(),
                    _ => Vec::new(),
                };
                let mut items: Vec<Value> = Vec::with_capacity(tags.len());
                for (profile, tag, value) in tags {
                    let obj = self.heap.alloc_dynamic(crate::m3_paths::TAGGED_VALUE);
                    self.heap.mutate_add(&obj, "tag", &[Value::String(tag)])?;
                    self.heap
                        .mutate_add(&obj, "profile", &[Value::Element(profile)])?;
                    self.heap
                        .mutate_add(&obj, "value", &[Value::String(SmolStr::new(value))])?;
                    items.push(Value::Object(obj));
                }
                let result = Value::from_vec(items);
                self.member_wrapper_cache
                    .insert((id, "taggedValues"), result.clone());
                Ok(result)
            }
            "properties" | "qualifiedProperties" | "propertiesFromAssociations" => {
                self.eval_class_member_collection(id, property)
            }
            "generalizations" => {
                let declared: Vec<ElementId> = match self.model.get_element(id) {
                    Element::Class(c) => c
                        .super_types
                        .iter()
                        .filter_map(|st| match st {
                            TypeExpr::Named { element, .. } => Some(*element),
                            _ => None,
                        })
                        .collect(),
                    Element::PrimitiveType(p) => p.super_type.into_iter().collect(),
                    _ => Vec::new(),
                };
                let is_typelike = matches!(
                    self.model.get_element(id),
                    Element::Class(_) | Element::PrimitiveType(_)
                );
                let super_eids: Vec<ElementId> = if declared.is_empty()
                    && is_typelike
                    && id != legend_pure_metamodel::bootstrap::ANY_ID
                {
                    vec![legend_pure_metamodel::bootstrap::ANY_ID]
                } else {
                    declared
                };
                let mut items = Vec::with_capacity(super_eids.len());
                for super_eid in super_eids {
                    let gt_obj = self.heap.alloc_dynamic(crate::m3_paths::GENERIC_TYPE);
                    self.heap
                        .mutate_add(&gt_obj, "rawType", &[Value::Element(super_eid)])?;
                    let gen_obj = self.heap.alloc_dynamic(crate::m3_paths::GENERALIZATION);
                    self.heap
                        .mutate_add(&gen_obj, "general", &[Value::Object(gt_obj)])?;
                    self.heap
                        .mutate_add(&gen_obj, "specific", &[Value::Element(id)])?;
                    items.push(Value::Object(gen_obj));
                }
                Ok(Value::from_vec(items))
            }
            _ => {
                if let Element::Enumeration(enum_def) = self.model.get_element(id)
                    && enum_def.values.iter().any(|v| v.name == property)
                {
                    return Ok(Value::EnumValue {
                        enum_id: id,
                        member: SmolStr::new(property),
                    });
                }
                if let Element::Measure(m) = self.model.get_element(id) {
                    match property {
                        "canonicalUnit" => {
                            return Ok(match m.canonical_unit {
                                Some(u) => Value::Element(u),
                                None => Value::Unit,
                            });
                        }
                        "nonCanonicalUnits" => {
                            let items: Vec<Value> = m
                                .non_canonical_units
                                .iter()
                                .copied()
                                .map(Value::Element)
                                .collect();
                            return Ok(Value::from_vec(items));
                        }
                        _ => {}
                    }
                }
                if metatype_property_exists(self.model, id, property) {
                    return Ok(Value::Unit);
                }
                Err(PureRuntimeError::EvaluationError(format!(
                    "Property '{property}' not supported on model element references"
                )))
            }
        }
    }

    /// Surface the `properties` / `qualifiedProperties` / `propertiesFromAssociations`
    /// list of a Class as a Collection of heap-backed wrapper objects.
    fn eval_class_member_collection(
        &mut self,
        id: ElementId,
        property: &str,
    ) -> Result<Value, PureRuntimeError> {
        let kind: &'static str = match property {
            "properties" => "properties",
            "qualifiedProperties" => "qualifiedProperties",
            "propertiesFromAssociations" => "propertiesFromAssociations",
            _ => unreachable!("caller restricts property value"),
        };

        if let Some(cached) = self.member_wrapper_cache.get(&(id, kind)) {
            return Ok(cached.clone());
        }

        let names: Vec<SmolStr> = match self.model.get_element(id) {
            Element::Class(c) => match kind {
                "properties" => c.properties.iter().map(|p| p.name.clone()).collect(),
                "qualifiedProperties" => c
                    .qualified_properties
                    .iter()
                    .map(|q| q.name.clone())
                    .collect(),
                "propertiesFromAssociations" => self
                    .model
                    .association_properties(id)
                    .iter()
                    .filter_map(|(assoc_id, prop_idx_self)| {
                        let Element::Association(assoc) = self.model.get_element(*assoc_id) else {
                            return None;
                        };
                        if assoc.properties.len() != 2 {
                            return None;
                        }
                        let injected_idx = 1 - *prop_idx_self;
                        Some(assoc.properties[injected_idx].name.clone())
                    })
                    .collect(),
                _ => unreachable!("kind is one of the three above"),
            },
            _ => Vec::new(),
        };

        let classifier = match kind {
            "qualifiedProperties" => crate::m3_paths::QUALIFIED_PROPERTY,
            _ => crate::m3_paths::PROPERTY,
        };

        let mut items: Vec<Value> = Vec::with_capacity(names.len());
        for name in names {
            let obj = self.heap.alloc_dynamic(classifier);
            self.heap.mutate_add(&obj, "name", &[Value::String(name)])?;
            self.heap
                .mutate_add(&obj, "_owner", &[Value::Element(id)])?;
            items.push(Value::Object(obj));
        }
        let result = Value::from_vec(items);
        self.member_wrapper_cache.insert((id, kind), result.clone());
        Ok(result)
    }

    fn eval_qualified_property(
        &mut self,
        target: &ValueSpec,
        property: &str,
        arguments: &[ValueSpec],
    ) -> Result<Value, PureException> {
        let target_val = self.eval(target)?;

        if let Value::Object(obj_id) = &target_val {
            let obj_id = obj_id.clone();
            let classifier = self
                .heap
                .classifier(&obj_id)
                .map_err(PureException::from)?
                .clone();
            if let Some(class_id) = self.model.resolve_fqn_str(classifier.as_str())
                && let Element::Class(class) = self.model.get_element(class_id)
                && let Some(qp) = class
                    .qualified_properties
                    .iter()
                    .find(|q| q.name == property && q.parameters.len() == arguments.len())
            {
                let params = qp.parameters.clone();
                let body = qp.body.clone();
                let type_var_param_names: Vec<SmolStr> = class
                    .type_variable_parameters
                    .iter()
                    .map(|p| p.name.clone())
                    .collect();
                let type_var_values: Vec<Value> = if type_var_param_names.is_empty() {
                    Vec::new()
                } else {
                    self.heap
                        .get_property_values(&obj_id, "__typeVariableValues")
                        .map(|v| v.iter().cloned().collect())
                        .unwrap_or_default()
                };
                let mut args_v: Vec<Value> = Vec::with_capacity(arguments.len());
                for arg in arguments {
                    args_v.push(self.eval(arg)?);
                }
                if is_generated_milestoning_qp(self.model, qp) {
                    return Err(PureException::from(PureRuntimeError::Unsupported(
                        "milestoning-synthesised qualified property".into(),
                    )));
                }
                self.context.push_scope();
                self.context
                    .set(SmolStr::new("this"), Value::Object(obj_id));
                for (name, value) in type_var_param_names.iter().zip(type_var_values.iter()) {
                    self.context.set(name.clone(), value.clone());
                }
                for (param, arg) in params.iter().zip(args_v.iter()) {
                    self.context.set(param.name.clone(), arg.clone());
                }
                let result = self.eval_body(&body);
                self.context.pop_scope();
                return result;
            }
        }

        Err(PureException::from(PureRuntimeError::Unsupported(format!(
            "qualified property `{property}` resolved by name and arity on the receiver's own class"
        ))))
    }

    fn eval_enum_value(enum_element: ElementId, value: &str) -> Value {
        Value::EnumValue {
            enum_id: enum_element,
            member: SmolStr::new(value),
        }
    }

    fn eval_lambda_creation(
        &self,
        parameters: &std::sync::Arc<[legend_pure_metamodel::types::Parameter]>,
        body: &std::sync::Arc<[ValueSpec]>,
        free: &std::sync::Arc<[SmolStr]>,
    ) -> Value {
        let mut captures = HashMap::with_capacity(free.len());
        for name in free.iter() {
            if let Some(val) = self.context.get(name) {
                captures.insert(name.clone(), val.clone());
            }
        }
        Value::Function(Box::new(FunctionValue::Lambda(LambdaClosure {
            parameters: std::sync::Arc::clone(parameters),
            body: std::sync::Arc::clone(body),
            captures,
        })))
    }

    fn eval_collection(&mut self, elements: &[ValueSpec]) -> Result<Value, PureException> {
        if elements.is_empty() {
            return Ok(Value::Unit);
        }

        let mut values = PVector::new();
        for elem in elements {
            let val = self.eval(elem)?;
            match &val {
                Value::Collection(inner) => {
                    for v in inner.iter() {
                        values.push_back(v.clone());
                    }
                }
                Value::Unit => {}
                _ => values.push_back(val),
            }
        }

        if values.is_empty() {
            Ok(Value::Unit)
        } else if values.len() == 1 {
            Ok(values.pop_front().unwrap_or(Value::Unit))
        } else {
            Ok(Value::Collection(Box::new(values)))
        }
    }

    /// Evaluate a lambda closure with the given arguments.
    /// # Errors
    /// Returns `PureException` if lambda execution explicitly fails or an underlying
    /// expression evaluation fails.
    pub fn eval_lambda_value(
        &mut self,
        lambda: &LambdaClosure,
        args: &[Value],
    ) -> Result<Value, PureException> {
        self.context.push_scope();

        for (name, value) in &lambda.captures {
            self.context.set(name.clone(), value.clone());
        }

        for (param, arg) in lambda.parameters.iter().zip(args.iter()) {
            self.context.set(param.name.clone(), arg.clone());
        }

        let result = self.eval_body(&lambda.body);

        self.context.pop_scope();

        result
    }

    /// Invoke a `Value::Function` with pre-evaluated arguments.
    ///
    /// Both `FunctionValue::Lambda` (anonymous closure) and `FunctionValue::Compiled`
    /// (named compiled element) are `Function` in Pure's type system and dispatched
    /// through the same entry point. The `FunctionValue` variant drives the internal
    /// strategy — callers need not distinguish.
    ///
    /// # Errors
    /// Returns `PureException` if `callable` is not a `Value::Function`, or if the
    /// underlying evaluation fails.
    pub fn apply_callable(
        &mut self,
        callable: &Value,
        args: &[Value],
    ) -> Result<Value, PureException> {
        match callable {
            Value::Function(fv) => self.eval_function_value(fv, args),
            Value::Element(id) if matches!(self.model.get_element(*id), Element::Function(_)) => {
                self.eval_function_value(&FunctionValue::Compiled(*id), args)
            }
            Value::Object(obj_id) => self.apply_object_callable(obj_id.clone(), args),
            other => Err(PureException::from(PureRuntimeError::EvaluationError(
                format!("Expected Function, got {}", other.type_name()),
            ))),
        }
    }

    /// Invoke a heap-object-shaped callable — `Property` wrappers emitted by
    /// `.properties` and `QualifiedProperty` wrappers emitted by
    /// `.qualifiedProperties`. Returns a type error for any other classifier.
    fn apply_object_callable(
        &mut self,
        id: crate::heap::ObjectHandle,
        args: &[Value],
    ) -> Result<Value, PureException> {
        let classifier = self
            .heap
            .classifier(&id)
            .map_err(PureException::from)?
            .to_string();
        match callable_wrapper_kind(self.model, &classifier) {
            Some(WrapperKind::Property) => {
                if args.is_empty() {
                    return Err(PureException::from(PureRuntimeError::EvaluationError(
                        "Property invocation expects the instance as its argument".into(),
                    )));
                }
                let name = self.read_wrapper_name(id)?;
                self.apply_property_to_instance(&name, &args[0])
            }
            Some(WrapperKind::QualifiedProperty) => self.apply_qualified_property(id, args),
            Some(WrapperKind::Column) => {
                if args.is_empty() {
                    return Err(PureException::from(PureRuntimeError::EvaluationError(
                        "Column invocation expects the row as its argument".into(),
                    )));
                }
                let name = self.read_wrapper_name(id)?;
                self.apply_property_to_instance(&name, &args[0])
            }
            None => Err(PureException::from(PureRuntimeError::EvaluationError(
                format!("Expected Function, got Object<{classifier}>"),
            ))),
        }
    }

    /// Invoke a `QualifiedProperty` wrapper.
    #[allow(clippy::needless_pass_by_value)]
    fn apply_qualified_property(
        &mut self,
        id: crate::heap::ObjectHandle,
        args: &[Value],
    ) -> Result<Value, PureException> {
        let name = self.read_wrapper_name(id.clone())?;
        let owner_vals = self
            .heap
            .get_property_values(&id, "_owner")
            .map_err(PureException::from)?;
        let Some(Value::Element(owner_id)) = owner_vals.front().cloned() else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                "QualifiedProperty wrapper is missing its '_owner' slot".into(),
            )));
        };
        let Element::Class(class) = self.model.get_element(owner_id) else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                format!("QualifiedProperty owner is not a Class: {owner_id:?}"),
            )));
        };
        let Some(qp) = class.qualified_properties.iter().find(|q| q.name == name) else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                format!(
                    "QualifiedProperty '{name}' not found on class {}",
                    self.model.element_name(owner_id)
                ),
            )));
        };
        if args.is_empty() {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                format!("QualifiedProperty '{name}' requires a receiver instance"),
            )));
        }
        let params = qp.parameters.clone();
        let body = qp.body.clone();

        let type_var_param_names: Vec<SmolStr> = class
            .type_variable_parameters
            .iter()
            .map(|p| p.name.clone())
            .collect();
        let type_var_values: Vec<Value> = match &args[0] {
            Value::Object(obj_id) if !type_var_param_names.is_empty() => self
                .heap
                .get_property_values(obj_id, "__typeVariableValues")
                .map(|v| v.iter().cloned().collect())
                .unwrap_or_default(),
            _ => Vec::new(),
        };

        self.context.push_scope();
        self.context.set(SmolStr::new("this"), args[0].clone());
        for (name, value) in type_var_param_names.iter().zip(type_var_values.iter()) {
            self.context.set(name.clone(), value.clone());
        }
        for (param, arg) in params.iter().zip(args.iter().skip(1)) {
            self.context.set(param.name.clone(), arg.clone());
        }
        let result = self.eval_body(&body);
        self.context.pop_scope();
        result
    }

    /// Read the `name` slot of a Property / `QualifiedProperty` wrapper as a
    /// plain string, erroring if it is missing or multi-valued.
    #[allow(clippy::needless_pass_by_value)]
    fn read_wrapper_name(&self, id: crate::heap::ObjectHandle) -> Result<SmolStr, PureException> {
        let values = self
            .heap
            .get_property_values(&id, "name")
            .map_err(PureException::from)?;
        match values.front() {
            Some(Value::String(s)) => Ok(s.clone()),
            _ => Err(PureException::from(PureRuntimeError::EvaluationError(
                "Property wrapper is missing its 'name' slot".into(),
            ))),
        }
    }

    /// Perform a plain property access on `instance` by `name`, returning the
    /// same multiplicity-normalised collection that the field-access path
    /// would produce.
    fn apply_property_to_instance(
        &mut self,
        name: &str,
        instance: &Value,
    ) -> Result<Value, PureException> {
        match instance {
            Value::Object(obj_id) => {
                let values = self
                    .heap
                    .get_property_values(obj_id, name)
                    .map_err(PureException::from)?;
                let collected: Vec<Value> = values.iter().cloned().collect();
                if !collected.is_empty() {
                    return Ok(Value::from_vec(collected));
                }
                if name == "elementOverride"
                    || name == "getterOverrideToOne"
                    || name == "getterOverrideToMany"
                    || name == "hiddenPayload"
                {
                    return Ok(Value::Unit);
                }
                self.try_getter_override(obj_id.clone(), name)
            }
            Value::Element(id) => {
                if matches!(self.model.get_element(*id), Element::Function(_)) {
                    let fv = FunctionValue::Compiled(*id);
                    let target = Value::Function(Box::new(fv.clone()));
                    if let Ok(v) = self.eval_function_property(&fv, name, &target) {
                        return Ok(v);
                    }
                }
                self.eval_element_property(*id, name)
                    .map_err(PureException::from)
            }
            other => Err(PureException::from(PureRuntimeError::EvaluationError(
                format!(
                    "Property invocation expects an Object instance, got {}",
                    other.type_name()
                ),
            ))),
        }
    }

    /// Dispatch a `FunctionValue` — anonymous lambda or compiled element.
    fn eval_function_value(
        &mut self,
        fv: &FunctionValue,
        args: &[Value],
    ) -> Result<Value, PureException> {
        match fv {
            FunctionValue::Lambda(closure) => self.eval_lambda_value(closure, args),
            FunctionValue::Compiled(id) => {
                let mangled: SmolStr = self.model.get_node(*id).name.clone();
                self.dispatch_compiled_function(*id, &mangled, args)
            }
            FunctionValue::Path(closure) => self.eval_path_value(&closure.clone(), args),
        }
    }

    /// Apply a [`PathClosure`] to its receiver: walks each step
    /// applying the property name to the running value.
    ///
    /// `args[0]` is the receiver `U[1]`; subsequent args (per the
    /// `Path<U,V|m>` signature) are ignored.
    ///
    /// Property *resolution* per step happens against the running
    /// receiver's classifier, so chain-of-types works dynamically
    /// without requiring static type-arg substitution. A missing
    /// property surfaces the existing property-access error path.
    fn eval_path_value(
        &mut self,
        closure: &crate::value::PathClosure,
        args: &[Value],
    ) -> Result<Value, PureException> {
        use crate::native::EvalContextTrait;
        let Some(receiver) = args.first() else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                "Path: expected at least one argument (the receiver)".into(),
            )));
        };
        let mut running = receiver.clone();

        self.context.push_scope();
        for (k, v) in &closure.captures {
            self.context.set(k.clone(), v.clone());
        }

        let result = (|| -> Result<Value, PureException> {
            for step in closure.steps.iter() {
                if step.parameters.is_empty() {
                    running = self.apply_property_to_instance(&step.property_name, &running)?;
                } else {
                    let mut param_values: Vec<Value> = Vec::with_capacity(step.parameters.len());
                    for p in &step.parameters {
                        param_values.push(self.eval(p)?);
                    }
                    let mut ctx = EvalContext { evaluator: self };
                    let invoked = ctx.invoke_qualified_property(
                        &running,
                        step.property_name.as_str(),
                        &param_values,
                    )?;
                    running = invoked.ok_or_else(|| {
                        PureException::from(PureRuntimeError::EvaluationError(format!(
                            "Path step '{}': qualified property not found on receiver",
                            step.property_name
                        )))
                    })?;
                }
            }
            Ok(running)
        })();

        self.context.pop_scope();
        result
    }

    /// Dispatch a compiled function element — native registry first, then user function.
    fn dispatch_compiled_function(
        &mut self,
        id: ElementId,
        mangled: &SmolStr,
        args: &[Value],
    ) -> Result<Value, PureException> {
        if let Some(native) = self.native_for(id) {
            return self.execute_native_with_values(native, args);
        }
        let is_native = matches!(self.model.get_element(id), Element::Function(f) if f.is_native);
        if !is_native {
            return self.call_user_function(id, args, mangled.as_str(), None);
        }
        Err(PureException::from(PureRuntimeError::FunctionNotFound(
            mangled.as_str().into(),
        )))
    }

    /// Invoke a native with already-forced `&[Value]` arguments.
    fn execute_native_with_values(
        &mut self,
        native: &dyn NativeFunction,
        args: &[Value],
    ) -> Result<Value, PureException> {
        self.context.push_scope();
        let mut specs = Vec::with_capacity(args.len());
        let placeholder_source =
            legend_pure_metamodel::source_info::SourceInfo::new("<dispatch>", 0, 0, 0, 0);
        for (i, v) in args.iter().enumerate() {
            let name: SmolStr = format!("__native_arg_{i}").into();
            self.context.set(name.clone(), v.clone());
            specs.push(ValueSpec {
                kind: Box::new(ExprKind::Variable { name }),
                source_info: placeholder_source.clone(),
                type_info: None,
            });
        }
        let result = {
            let mut ctx = EvalContext { evaluator: self };
            native.execute(&specs, &mut ctx)
        };
        self.context.pop_scope();
        result.map(Evaluated::into_value)
    }
}

/// Evaluation context passed to native functions.
///
/// Provides native functions with access to the evaluator's capabilities:
/// evaluating lambda bodies, reading/writing variables, and accessing the
/// object heap. Mirrors Java's `FunctionExecutionInterpreted` + `VariableContext`.
pub struct EvalContext<'a, 'model: 'a, H: EvalHooks = NoOpHooks> {
    /// The underlying evaluator.
    evaluator: &'a mut Evaluator<'model, H>,
}

impl<'model, H: EvalHooks> EvalContext<'_, 'model, H> {
    /// Access the compiled model.
    #[must_use]
    pub fn model(&self) -> &'model PureModel {
        self.evaluator.model
    }
}

impl<H: EvalHooks> crate::native::EvalContextTrait for EvalContext<'_, '_, H> {
    fn evaluate(&mut self, spec: &ValueSpec) -> Result<Evaluated, PureException> {
        self.evaluator.eval(spec).map(Evaluated::new)
    }

    fn for_each_value(
        &mut self,
        spec: &ValueSpec,
        each: &mut dyn FnMut(Value) -> Result<(), PureException>,
    ) -> Result<(), PureException> {
        let elements: &[ValueSpec] = match &*spec.kind {
            ExprKind::Collection { elements } => elements,
            _ => std::slice::from_ref(spec),
        };
        for element in elements {
            match self.evaluator.eval(element)? {
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
        &self.evaluator.context
    }

    fn context_mut(&mut self) -> &mut VariableContext {
        &mut self.evaluator.context
    }

    fn heap(&self) -> &RuntimeHeap {
        &self.evaluator.heap
    }

    fn heap_mut(&mut self) -> &mut RuntimeHeap {
        &mut self.evaluator.heap
    }

    fn call_function(&mut self, callable: &Value, args: &[Value]) -> Result<Value, PureException> {
        self.evaluator.apply_callable(callable, args)
    }

    fn model(&self) -> &legend_pure_metamodel::model::PureModel {
        self.evaluator.model
    }

    fn console_output(&mut self, msg: &str) {
        self.evaluator.hooks.console_output(msg);
    }

    fn invoke_qualified_property(
        &mut self,
        receiver: &Value,
        name: &str,
        args: &[Value],
    ) -> Result<Option<Value>, PureException> {
        let Value::Object(obj_id) = receiver else {
            return Ok(None);
        };
        let classifier = self
            .evaluator
            .heap
            .classifier(obj_id)
            .map_err(PureException::from)?
            .clone();
        let Some(class_id) = self.evaluator.model.resolve_fqn_str(classifier.as_str()) else {
            return Ok(None);
        };
        let Some(found) =
            find_qp_with_generalization(self.evaluator.model, class_id, name, args.len())
        else {
            return Ok(None);
        };
        self.invoke_qualified_property_found(receiver, &found, args)
            .map(Some)
    }

    fn invoke_qualified_property_found(
        &mut self,
        receiver: &Value,
        found: &FoundQp,
        args: &[Value],
    ) -> Result<Value, PureException> {
        let Value::Object(obj_id) = receiver else {
            return Err(PureException::from(PureRuntimeError::EvaluationError(
                "invoke_qualified_property_found: receiver is not a Value::Object".to_string(),
            )));
        };
        let obj_id = obj_id.clone();
        let params = std::sync::Arc::clone(&found.parameters);
        let body = std::sync::Arc::clone(&found.body);
        let type_var_param_names = found.type_var_param_names.clone();
        let type_var_values: Vec<Value> = if type_var_param_names.is_empty() {
            Vec::new()
        } else {
            self.evaluator
                .heap
                .get_property_values(&obj_id, "__typeVariableValues")
                .map(|v| v.iter().cloned().collect())
                .unwrap_or_default()
        };
        self.evaluator.context.push_scope();
        self.evaluator
            .context
            .set(SmolStr::new("this"), Value::Object(obj_id));
        for (n, v) in type_var_param_names.iter().zip(type_var_values.iter()) {
            self.evaluator.context.set(n.clone(), v.clone());
        }
        for (param, arg) in params.iter().zip(args.iter()) {
            self.evaluator.context.set(param.name.clone(), arg.clone());
        }
        let result = self.evaluator.eval_body(&body);
        self.evaluator.context.pop_scope();
        result
    }
}

impl<H: EvalHooks> std::fmt::Debug for Evaluator<'_, H> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Evaluator")
            .field("heap_objects", &self.heap.metamodel_len())
            .field("context_depth", &self.context.depth())
            .finish()
    }
}

/// Owned snapshot of a qualified-property definition that survives a
/// later `&mut self` borrow on the evaluator.
pub struct FoundQp {
    /// Declared parameters of the QP.
    pub parameters: std::sync::Arc<[legend_pure_metamodel::types::Parameter]>,
    /// Lowered body expressions of the QP.
    pub body: std::sync::Arc<[ValueSpec]>,
    /// Type-variable parameter *names* declared on the **owning
    /// class** (not the receiver's class — same contract as
    /// `eval_qualified_property`). Used to bind type-variable values
    /// pulled from the receiver's `__typeVariableValues` slot before
    /// invoking the body.
    pub type_var_param_names: Vec<SmolStr>,
    /// Declared return type of the QP.
    pub return_type: legend_pure_metamodel::types::TypeExpr,
    /// Declared return multiplicity of the QP.
    pub return_multiplicity: legend_pure_metamodel::types::Multiplicity,
}

/// Locate a qualified property by `name` and `arity` on `class_id` or any
/// of its generalizations (BFS).
///
/// Returns `None` when no class on the inheritance chain declares a QP
/// with the matching name and parameter count. Type-variable parameter
/// names are captured from the **declaring** class (the one that owns
/// the QP), not the receiver's class — same contract as
/// `eval_qualified_property`.
pub fn find_qp_with_generalization(
    model: &legend_pure_metamodel::model::PureModel,
    class_id: ElementId,
    name: &str,
    arity: usize,
) -> Option<FoundQp> {
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut queue: std::collections::VecDeque<ElementId> = std::collections::VecDeque::new();
    queue.push_back(class_id);
    visited.insert(class_id);
    while let Some(cid) = queue.pop_front() {
        let Element::Class(class) = model.get_element(cid) else {
            continue;
        };
        if let Some(qp) = class
            .qualified_properties
            .iter()
            .find(|q| q.name == name && q.parameters.len() == arity)
        {
            return Some(FoundQp {
                parameters: std::sync::Arc::clone(&qp.parameters),
                body: std::sync::Arc::clone(&qp.body),
                type_var_param_names: class
                    .type_variable_parameters
                    .iter()
                    .map(|p| p.name.clone())
                    .collect(),
                return_type: qp.return_type.clone(),
                return_multiplicity: qp.return_multiplicity.clone(),
            });
        }
        for super_ty in &class.super_types {
            if let TypeExpr::Named { element, .. } = super_ty
                && visited.insert(*element)
            {
                queue.push_back(*element);
            }
        }
    }
    None
}

/// Map a Multiplicity-constant element (under
/// `meta::pure::metamodel::multiplicity::*`) to its concrete `(lower, upper)`
/// integer bounds. Returns `None` for any element outside that package or
/// with an unrecognised name. `upper = None` means unbounded (`*`).
fn multiplicity_constant_bounds(
    model: &legend_pure_metamodel::model::PureModel,
    id: ElementId,
) -> Option<(i64, Option<i64>)> {
    let ElementId::InstanceId { .. } = id else {
        return None;
    };
    let node = model.get_node(id);
    match node.name.as_str() {
        "PureZero" => Some((0, Some(0))),
        "PureOne" => Some((1, Some(1))),
        "ZeroOne" => Some((0, Some(1))),
        "ZeroMany" => Some((0, None)),
        "OneMany" => Some((1, None)),
        _ => None,
    }
}

/// Collect the free-variable names of a lambda `body` given its declared
/// `parameters`. A "free" variable is any `Variable { name }` reference in
/// the body that isn't introduced by one of the lambda's own parameters or
/// by a `let` / nested-lambda binding along the walk.
/// The free variables of a lambda body, in a stable order, as the node
/// stores them: computed once when the node is built.
#[must_use]
pub fn free_variable_names(
    body: &[ValueSpec],
    parameters: &[legend_pure_metamodel::types::Parameter],
) -> std::sync::Arc<[SmolStr]> {
    let mut names: Vec<SmolStr> = collect_free_variables(body, parameters)
        .into_iter()
        .collect();
    names.sort_unstable();
    names.into()
}

pub(crate) fn collect_free_variables(
    body: &[ValueSpec],
    parameters: &[legend_pure_metamodel::types::Parameter],
) -> std::collections::HashSet<SmolStr> {
    let mut binders: std::collections::HashSet<SmolStr> =
        parameters.iter().map(|p| p.name.clone()).collect();
    let mut free: std::collections::HashSet<SmolStr> = std::collections::HashSet::new();
    for spec in body {
        walk_free_variables(spec, &mut binders, &mut free);
    }
    free
}

/// Walk a single `ValueSpec`, accumulating free variables into `free` and
/// respecting `let`-desugared bindings in the caller's `binders` set.
fn walk_free_variables(
    spec: &ValueSpec,
    binders: &mut std::collections::HashSet<SmolStr>,
    free: &mut std::collections::HashSet<SmolStr>,
) {
    match &*spec.kind {
        ExprKind::Variable { name } => {
            if !binders.contains(name) {
                free.insert(name.clone());
            }
        }
        ExprKind::FunctionCall(FunctionCallData {
            function_name,
            arguments,
            ..
        }) => {
            if function_name.as_str() == "letFunction"
                && arguments.len() == 2
                && let ExprKind::StringLiteral(binding_name) = &*arguments[0].kind
            {
                walk_free_variables(&arguments[1], binders, free);
                binders.insert(binding_name.clone());
                return;
            }
            for arg in arguments {
                walk_free_variables(arg, binders, free);
            }
        }
        ExprKind::Lambda {
            parameters: inner_params,
            body: inner_body,
            ..
        } => {
            let mut inner_binders = binders.clone();
            for p in inner_params.iter() {
                inner_binders.insert(p.name.clone());
            }
            for inner in inner_body.iter() {
                walk_free_variables(inner, &mut inner_binders, free);
            }
        }
        ExprKind::PropertyCall(data) | ExprKind::QualifiedPropertyCall(data) => {
            for arg in &data.arguments {
                walk_free_variables(arg, binders, free);
            }
        }
        ExprKind::KeyExpression { expression, .. } => {
            walk_free_variables(expression, binders, free);
        }
        ExprKind::Collection { elements } => {
            for e in elements {
                walk_free_variables(e, binders, free);
            }
        }
        ExprKind::PathLiteral { steps, .. } => {
            for step in steps {
                for param in &step.parameters {
                    walk_free_variables(param, binders, free);
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

/// Classify a heap object by resolving its classifier string to a
/// well-known M3 `ElementId`.
/// Returns `None` if the classifier doesn't correspond
/// to one of the callable wrapper classes.
pub(crate) fn callable_wrapper_kind(model: &PureModel, classifier: &str) -> Option<WrapperKind> {
    let id = crate::m3_paths::resolve(model, classifier)?;
    if crate::m3_paths::resolve(model, crate::m3_paths::PROPERTY) == Some(id) {
        Some(WrapperKind::Property)
    } else if crate::m3_paths::resolve(model, crate::m3_paths::QUALIFIED_PROPERTY) == Some(id) {
        Some(WrapperKind::QualifiedProperty)
    } else if crate::m3_paths::resolve(model, crate::m3_paths::COLUMN) == Some(id) {
        Some(WrapperKind::Column)
    } else {
        None
    }
}

/// True when `property` is declared on the M3 metatype that classifies
/// `id` (or any of its supertypes).
pub(crate) fn metatype_property_exists(model: &PureModel, id: ElementId, property: &str) -> bool {
    let element = model.get_element(id);
    let Some(meta_id) = legend_pure_metamodel::bootstrap::metatype_of(model, element) else {
        return false;
    };
    let mut visited: std::collections::HashSet<ElementId> = std::collections::HashSet::new();
    let mut stack: Vec<ElementId> = vec![meta_id];
    while let Some(cid) = stack.pop() {
        if !visited.insert(cid) {
            continue;
        }
        let Element::Class(c) = model.get_element(cid) else {
            continue;
        };
        for p in &c.properties {
            if p.name.as_str() == property {
                return true;
            }
        }
        for p in &c.qualified_properties {
            if p.name.as_str() == property {
                return true;
            }
        }
        for st in &c.super_types {
            if let TypeExpr::Named { element, .. } = st {
                stack.push(*element);
            }
        }
    }
    false
}

/// Callable wrapper classification — see [`callable_wrapper_kind`].
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum WrapperKind {
    /// `meta::pure::metamodel::function::property::Property` — a Class
    /// property reference, invoked as `$propRef->eval($instance)` to read
    /// the named property from `$instance`.
    Property,
    /// `meta::pure::metamodel::function::property::QualifiedProperty` — a
    /// derived/qualified property reference, invoked via
    /// `$qp->evaluate(^List<Any>(values=$instance), …)`.
    QualifiedProperty,
    /// `meta::pure::metamodel::relation::Column` — a relation column.
    /// `Column<U,V|m>` *extends `Function`* (see `platform/pure/grammar/m3.pure`),
    /// so `$col->eval($row)` reads the cell for this column off the row tuple.
    Column,
}

/// Profile and stereotype that mark a qualified property synthesised by
/// milestoning (`<<milestoning.generatedmilestoningproperty>>`).
const MILESTONING_PROFILE_FQN: &str = "meta::pure::profiles::milestoning";
const GENERATED_MILESTONING_STEREOTYPE: &str = "generatedmilestoningproperty";

fn is_generated_milestoning_qp(
    model: &PureModel,
    qp: &legend_pure_metamodel::nodes::class::QualifiedProperty,
) -> bool {
    let Some(profile) = model.resolve_fqn_str(MILESTONING_PROFILE_FQN) else {
        return false;
    };
    qp.stereotypes
        .iter()
        .any(|s| s.profile == profile && s.value == GENERATED_MILESTONING_STEREOTYPE)
}
