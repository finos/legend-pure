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

//! Fluent builder for constructing [`Evaluator`] instances.
//!
//! ```ignore
//! use legend_pure_evaluator::eval::Evaluator;
//!
//! let eval = Evaluator::builder().registry(&registry).build(&model);
//! ```

use legend_pure_metamodel::model::PureModel;

use crate::eval::Evaluator;
use crate::native::NativeRegistry;

/// Fluent builder for [`Evaluator`] — see the [module docs](crate::builder).
///
/// An unset registry falls back to [`NativeRegistry::standard`] at
/// [`build`](Self::build) time. The builder is single-use (consumed
/// by [`build`](Self::build)); construct a fresh one per `Evaluator`.
#[must_use = "EvaluatorBuilder does nothing until `.build(&model)` is called"]
pub struct EvaluatorBuilder<'a> {
    registry: Option<&'a NativeRegistry>,
}

impl<'a> EvaluatorBuilder<'a> {
    /// Create a builder with every knob unset.
    pub fn new() -> Self {
        Self { registry: None }
    }

    /// Use the supplied [`NativeRegistry`] for native dispatch.
    pub fn registry(mut self, registry: &'a NativeRegistry) -> Self {
        self.registry = Some(registry);
        self
    }

    /// Construct the [`Evaluator`].
    ///
    /// When no registry was supplied, [`NativeRegistry::standard`] is
    /// built and leaked to `'static` so the evaluator's borrow is
    /// satisfied (one leak per default build; callers that construct
    /// many evaluators should pass a pre-built registry).
    pub fn build(self, model: &'a PureModel) -> Evaluator<'a> {
        let registry: &'a NativeRegistry = self
            .registry
            .unwrap_or_else(|| Box::leak(Box::new(NativeRegistry::standard())));
        Evaluator::new(model, registry)
    }
}

impl Default for EvaluatorBuilder<'_> {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn build_with_defaults_constructs_evaluator() {
        let model = PureModel::new();
        let _eval = Evaluator::builder().build(&model);
    }

    #[test]
    fn build_with_explicit_registry_uses_it() {
        let model = PureModel::new();
        let registry = NativeRegistry::standard();
        let eval = Evaluator::builder().registry(&registry).build(&model);
        assert_eq!(eval.natives().len(), NativeRegistry::standard().len());
    }
}
