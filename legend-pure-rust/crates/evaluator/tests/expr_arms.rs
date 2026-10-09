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

//! One test per `ExprKind` arm not covered elsewhere: the remaining
//! literals, and the four arms the extracted evaluator stubs as
//! `Unsupported` until their runtime is fished.

use legend_pure_evaluator::builders::{ModelBuilder, expr, named};
use legend_pure_evaluator::error::{PureException, PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::bootstrap::STRING_ID;
use legend_pure_metamodel::types::{
    ColSpecLiteralKind, DateValue, ExprKind, Multiplicity, RelationColumnLowered,
};

fn unsupported(err: &PureException) -> Option<&'static str> {
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::Unsupported(what)) => Some(what),
        _ => None,
    }
}

#[test]
fn decimal_literal() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let d = rust_decimal::Decimal::new(150, 2);
    assert_eq!(eval.eval(&expr::decimal(d)).unwrap(), Value::Decimal(d));
}

#[test]
fn date_literal() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let value = eval
        .eval(&expr::date(DateValue::StrictDate {
            year: 2024,
            month: Some(2),
            day: Some(29),
        }))
        .unwrap();
    assert!(matches!(value, Value::Date(_)), "{value:?}");
    // `Value::Latest` is not equal to itself under the archived `PartialEq`
    // (every `%latest` compares unequal); assert the shape, not equality.
    assert!(matches!(
        eval.eval(&expr::date(DateValue::Latest)).unwrap(),
        Value::Latest
    ));
}

#[test]
fn stubbed_arms_report_unsupported() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let column = || RelationColumnLowered {
        name: "c".into(),
        type_element: STRING_ID,
        multiplicity: Multiplicity::PureOne,
        init_lambda: None,
        reduce_lambda: None,
    };
    let cases = [
        (
            ExprKind::RelationLiteral {
                columns: vec![column()],
            },
            "relation literal",
        ),
        (
            ExprKind::ColSpecArrayLiteral {
                columns: vec![column()],
                kind: ColSpecLiteralKind::Plain,
            },
            "column spec array literal",
        ),
        (
            ExprKind::ColSpecLiteral {
                column: column(),
                kind: ColSpecLiteralKind::Plain,
            },
            "column spec literal",
        ),
        (
            ExprKind::PathLiteral {
                start_type: named(STRING_ID),
                steps: Vec::new(),
                name: None,
            },
            "path literal",
        ),
    ];
    for (kind, expected) in cases {
        let err = eval.eval(&expr::spec(kind)).unwrap_err();
        let what = unsupported(&err).expect("an Unsupported error");
        assert!(what.starts_with(expected), "{what} vs {expected}");
    }
}
