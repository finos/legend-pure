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

//! The date and time natives driven through a real `Evaluator`: date
//! literals and `DurationUnit` enum literals in, `PureDate` values out.
//! The expected values are the platform PCT assertions
//! (`essential/date/operation/*.pure`).

use legend_pure_evaluator::builders::{ModelBuilder, expr};
use legend_pure_evaluator::date::{PureDate, TimePrecision};
use legend_pure_evaluator::error::{PureException, PureExceptionKind, PureRuntimeError};
use legend_pure_evaluator::eval::Evaluator;
use legend_pure_evaluator::native::NativeRegistry;
use legend_pure_evaluator::value::Value;
use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::model::PureModel;
use legend_pure_metamodel::types::{DateValue, ValueSpec};

const ADJUST: &str = "adjust_Date_1__Integer_1__DurationUnit_1__Date_1_";
const DATE_DIFF: &str = "dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_";
const DATE_PART: &str = "datePart_Date_1__Date_1_";
const DAY_OF_MONTH: &str = "dayOfMonth_Date_1__Integer_1_";
const HOUR: &str = "hour_Date_1__Integer_1_";
const MINUTE: &str = "minute_Date_1__Integer_1_";
const SECOND: &str = "second_Date_1__Integer_1_";
const MONTH_NUMBER: &str = "monthNumber_Date_1__Integer_1_";
const YEAR: &str = "year_Date_1__Integer_1_";
const HAS_DAY: &str = "hasDay_Date_1__Boolean_1_";
const HAS_MONTH: &str = "hasMonth_Date_1__Boolean_1_";
const HAS_HOUR: &str = "hasHour_Date_1__Boolean_1_";
const HAS_MINUTE: &str = "hasMinute_Date_1__Boolean_1_";
const HAS_SECOND: &str = "hasSecond_Date_1__Boolean_1_";
const HAS_SUBSECOND: &str = "hasSubsecond_Date_1__Boolean_1_";
const HAS_SUBSECOND_AT_LEAST: &str =
    "hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_";

/// `%y` — a year-only date.
fn year_only(year: i16) -> ValueSpec {
    expr::date(DateValue::StrictDate {
        year,
        month: None,
        day: None,
    })
}

/// `%y-m` — a year-month date.
fn year_month(year: i16, month: i8) -> ValueSpec {
    expr::date(DateValue::StrictDate {
        year,
        month: Some(month),
        day: None,
    })
}

/// `%2015-04-15T17:09:21.398` cut at `digits` time components: 1 is
/// `T17`, 2 `T17:09`, 3 `T17:09:21`, 4 `T17:09:21.398`.
fn extract_fixture(digits: u8) -> ValueSpec {
    expr::date(DateValue::DateTime {
        year: 2015,
        month: 4,
        day: 15,
        hour: 17,
        minute: if digits >= 2 { 9 } else { 0 },
        second: if digits >= 3 { 21 } else { 0 },
        subsecond_nanos: if digits >= 4 { 398_000_000 } else { 0 },
        subsecond_digits: if digits >= 4 { 3 } else { 0 },
        has_minutes: digits >= 2,
        has_seconds: digits >= 3,
        tz_offset_minutes: None,
    })
}

/// The fixtures of `essential/date/extract/*.pure`, coarsest first:
/// `%2015-04`, `%2015-04-15`, `T17`, `T17:09`, `T17:09:21`, `T17:09:21.398`.
fn extract_ladder() -> Vec<ValueSpec> {
    let mut ladder = vec![year_month(2015, 4), strict(2015, 4, 15)];
    ladder.extend((1..=4).map(extract_fixture));
    ladder
}

/// Assert a `has*` predicate over the fixtures of
/// `essential/date/has/*.pure` — `%2015`, then the extract ladder — is
/// false for the `first_true` coarsest ones and true from there on.
fn assert_precision_predicate(key: &str, first_true: usize) {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let mut ladder = vec![year_only(2015)];
    ladder.extend(extract_ladder());
    for (i, date) in ladder.into_iter().enumerate() {
        assert_eq!(
            apply(&mut eval, key, date).unwrap(),
            Value::Boolean(i >= first_true),
            "{key} on fixture {i}"
        );
    }
}

/// Apply a one-argument date native to `date`.
#[allow(clippy::result_large_err)]
fn apply(eval: &mut Evaluator<'_>, key: &str, date: ValueSpec) -> Result<Value, PureException> {
    eval.eval(&expr::call(key, vec![date]))
}

fn evaluation_error(err: &PureException) -> Option<&str> {
    match &err.kind {
        PureExceptionKind::ExecutionError(PureRuntimeError::EvaluationError(m)) => Some(m),
        _ => None,
    }
}

/// The platform's `meta::pure::functions::date::DurationUnit` (`_structures.pure`).
fn duration_unit_model() -> (PureModel, ElementId) {
    let mut b = ModelBuilder::new();
    let unit = b.enumeration(
        "meta::pure::functions::date::DurationUnit",
        &[
            "YEARS",
            "MONTHS",
            "WEEKS",
            "DAYS",
            "HOURS",
            "MINUTES",
            "SECONDS",
            "MILLISECONDS",
            "MICROSECONDS",
            "NANOSECONDS",
        ],
    );
    (b.finish(), unit)
}

/// `%y-m-d`.
fn strict(year: i16, month: i8, day: i8) -> ValueSpec {
    expr::date(DateValue::StrictDate {
        year,
        month: Some(month),
        day: Some(day),
    })
}

/// `%y-m-dTh:mi:s`.
fn second_precision(year: i16, month: i8, day: i8, hour: i8, minute: i8, second: i8) -> ValueSpec {
    expr::date(DateValue::DateTime {
        year,
        month,
        day,
        hour,
        minute,
        second,
        subsecond_nanos: 0,
        subsecond_digits: 0,
        has_minutes: true,
        has_seconds: true,
        tz_offset_minutes: None,
    })
}

fn strict_value(year: i16, month: i8, day: i8) -> Value {
    Value::Date(PureDate::strict_date(year, month, day).unwrap())
}

fn second_value(year: i16, month: i8, day: i8, hour: i8, minute: i8, second: i8) -> Value {
    Value::Date(
        PureDate::datetime(
            year,
            month,
            day,
            hour,
            minute,
            second,
            0,
            TimePrecision::Second,
        )
        .unwrap(),
    )
}

#[test]
fn adjust_moves_a_date_by_each_calendar_unit() {
    let (model, unit) = duration_unit_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let adjust = |date: ValueSpec, n: i64, member: &str| {
        expr::call(
            ADJUST,
            vec![date, expr::int(n), expr::enum_value(unit, member)],
        )
    };
    // testAdjustByDays, testAdjustByWeeks, testAdjustByMonths, testAdjustByYears
    assert_eq!(
        eval.eval(&adjust(strict(2015, 4, 15), 1, "DAYS")).unwrap(),
        strict_value(2015, 4, 16)
    );
    assert_eq!(
        eval.eval(&adjust(strict(2015, 4, 15), -2, "WEEKS"))
            .unwrap(),
        strict_value(2015, 4, 1)
    );
    assert_eq!(
        eval.eval(&adjust(strict(2016, 1, 31), 1, "MONTHS"))
            .unwrap(),
        strict_value(2016, 2, 29)
    );
    assert_eq!(
        eval.eval(&adjust(strict(2016, 2, 29), 1, "YEARS")).unwrap(),
        strict_value(2017, 2, 28)
    );
    // testAdjustByHours
    assert_eq!(
        eval.eval(&adjust(
            second_precision(2015, 4, 15, 12, 12, 11),
            36,
            "HOURS"
        ))
        .unwrap(),
        second_value(2015, 4, 17, 0, 12, 11)
    );
}

#[test]
fn date_diff_counts_calendar_and_clock_units() {
    let (model, unit) = duration_unit_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let mut diff = |from: ValueSpec, to: ValueSpec, member: &str| {
        eval.eval(&expr::call(
            DATE_DIFF,
            vec![from, to, expr::enum_value(unit, member)],
        ))
        .unwrap()
    };
    // testDateDiffWeeks: Sunday boundaries crossed, not days / 7.
    for (from, to, weeks) in [
        ((2015, 7, 5), (2015, 7, 5), 0),
        ((2015, 7, 3), (2015, 7, 4), 0),
        ((2015, 7, 4), (2015, 7, 5), 1),
        ((2015, 7, 5), (2015, 7, 4), 0),
        ((2015, 7, 12), (2015, 7, 5), -1),
        ((2015, 8, 2), (2015, 7, 6), -3),
        ((2015, 1, 1), (2016, 1, 1), 52),
    ] {
        assert_eq!(
            diff(
                strict(from.0, from.1, from.2),
                strict(to.0, to.1, to.2),
                "WEEKS"
            ),
            Value::Integer(weeks),
            "{from:?} -> {to:?}"
        );
    }
    // testDateDiffYears, testDateDiffMonths, testDateDiffDays
    assert_eq!(
        diff(strict(2015, 12, 31), strict(2016, 1, 1), "YEARS"),
        Value::Integer(1)
    );
    assert_eq!(
        diff(strict(2015, 12, 31), strict(2016, 2, 1), "MONTHS"),
        Value::Integer(2)
    );
    assert_eq!(
        diff(strict(2015, 2, 28), strict(2015, 3, 1), "DAYS"),
        Value::Integer(1)
    );
    // testDateDiffHours
    assert_eq!(
        diff(
            second_precision(2015, 7, 5, 10, 0, 0),
            second_precision(2015, 7, 6, 12, 30, 0),
            "HOURS"
        ),
        Value::Integer(26)
    );
}

#[test]
fn adjust_rejects_an_argument_that_is_not_a_duration_unit() {
    let (model, _) = duration_unit_model();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let call = expr::call(
        ADJUST,
        vec![strict(2015, 4, 15), expr::int(1), expr::int(3)],
    );
    assert!(eval.eval(&call).is_err());
}

#[test]
fn date_part_drops_the_time_and_keeps_partial_dates() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let part = |date: ValueSpec| expr::call(DATE_PART, vec![date]);
    // testDatePart, testDatePartTrivial
    assert_eq!(
        eval.eval(&part(second_precision(2015, 4, 15, 17, 9, 21)))
            .unwrap(),
        strict_value(2015, 4, 15)
    );
    assert_eq!(
        eval.eval(&part(strict(2015, 4, 15))).unwrap(),
        strict_value(2015, 4, 15)
    );
    // testDatePartYearOnly, testDatePartYearMonthOnly
    let year_only = expr::date(DateValue::StrictDate {
        year: 1973,
        month: None,
        day: None,
    });
    assert_eq!(
        eval.eval(&part(year_only)).unwrap(),
        Value::Date(PureDate::year(1973).unwrap())
    );
    let year_month = expr::date(DateValue::StrictDate {
        year: 1973,
        month: Some(11),
        day: None,
    });
    assert_eq!(
        eval.eval(&part(year_month)).unwrap(),
        Value::Date(PureDate::year_month(1973, 11).unwrap())
    );
}

#[test]
fn day_of_month_reads_the_day_or_fails_with_the_java_message() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testDayOfMonth: every fixture from day precision down.
    for date in extract_ladder().into_iter().skip(1) {
        assert_eq!(
            apply(&mut eval, DAY_OF_MONTH, date).unwrap(),
            Value::Integer(15)
        );
    }
    // testDayOfMonthError
    let err = apply(&mut eval, DAY_OF_MONTH, year_only(2017)).unwrap_err();
    assert_eq!(
        evaluation_error(&err),
        Some("Cannot get day of month for 2017")
    );
}

#[test]
fn hour_reads_the_hour_or_fails_with_the_java_message() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testHour: every fixture from hour precision down.
    for date in (1..=4).map(extract_fixture) {
        assert_eq!(apply(&mut eval, HOUR, date).unwrap(), Value::Integer(17));
    }
    // testHourError
    let err = apply(&mut eval, HOUR, year_only(2017)).unwrap_err();
    assert_eq!(evaluation_error(&err), Some("Cannot get hour for 2017"));
}

#[test]
fn minute_reads_the_minute_or_fails_with_the_java_message() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testMinute: every fixture from minute precision down.
    for date in (2..=4).map(extract_fixture) {
        assert_eq!(apply(&mut eval, MINUTE, date).unwrap(), Value::Integer(9));
    }
    // testMinuteError
    let err = apply(&mut eval, MINUTE, year_only(2017)).unwrap_err();
    assert_eq!(evaluation_error(&err), Some("Cannot get minute for 2017"));
}

#[test]
fn second_reads_the_second_or_fails_with_the_java_message() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testSecond: every fixture from second precision down.
    for date in (3..=4).map(extract_fixture) {
        assert_eq!(apply(&mut eval, SECOND, date).unwrap(), Value::Integer(21));
    }
    // testSecondError
    let err = apply(&mut eval, SECOND, year_only(2017)).unwrap_err();
    assert_eq!(evaluation_error(&err), Some("Cannot get second for 2017"));
}

#[test]
fn month_number_reads_the_month() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testMonthNumber: every fixture from month precision down.
    for date in extract_ladder() {
        assert_eq!(
            apply(&mut eval, MONTH_NUMBER, date).unwrap(),
            Value::Integer(4)
        );
    }
    assert_eq!(
        apply(&mut eval, MONTH_NUMBER, year_month(2015, 1)).unwrap(),
        Value::Integer(1)
    );
    let err = apply(&mut eval, MONTH_NUMBER, year_only(2015)).unwrap_err();
    assert_eq!(
        evaluation_error(&err),
        Some("monthNumber: date has no month component")
    );
}

#[test]
fn year_reads_the_year_at_every_precision() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    // testYear: year-only through subsecond.
    let mut dates = vec![year_only(2015)];
    dates.extend(extract_ladder());
    for date in dates {
        assert_eq!(apply(&mut eval, YEAR, date).unwrap(), Value::Integer(2015));
    }
}

/// testHasDay: true from `%2015-04-15` down.
#[test]
fn has_day_holds_from_day_precision() {
    assert_precision_predicate(HAS_DAY, 2);
}

/// testHasMonth: true from `%2015-04` down.
#[test]
fn has_month_holds_from_month_precision() {
    assert_precision_predicate(HAS_MONTH, 1);
}

/// testHasHour: true from `%2015-04-15T17` down.
#[test]
fn has_hour_holds_from_hour_precision() {
    assert_precision_predicate(HAS_HOUR, 3);
}

/// testHasMinute: true from `%2015-04-15T17:09` down.
#[test]
fn has_minute_holds_from_minute_precision() {
    assert_precision_predicate(HAS_MINUTE, 4);
}

/// testHasSecond: true from `%2015-04-15T17:09:21` down.
#[test]
fn has_second_holds_from_second_precision() {
    assert_precision_predicate(HAS_SECOND, 5);
}

/// testHasSubsecond: true only for `%2015-04-15T17:09:21.398`.
#[test]
fn has_subsecond_holds_only_with_a_fraction() {
    assert_precision_predicate(HAS_SUBSECOND, 6);
}

/// testHasSubsecondWithAtLeastPrecision: `.398` has three digits; coarser
/// dates have none.
#[test]
fn has_subsecond_with_at_least_precision_counts_the_digits() {
    let model = ModelBuilder::new().finish();
    let registry = NativeRegistry::standard();
    let mut eval = Evaluator::new(&model, &registry);
    let mut at_least = |date: ValueSpec, digits: i64| {
        eval.eval(&expr::call(
            HAS_SUBSECOND_AT_LEAST,
            vec![date, expr::int(digits)],
        ))
        .unwrap()
    };
    for digits in 1..=5 {
        assert_eq!(
            at_least(extract_fixture(4), digits),
            Value::Boolean(digits <= 3),
            "{digits} digits"
        );
    }
    let mut coarser = vec![year_only(2015)];
    coarser.extend(extract_ladder().into_iter().take(5));
    for date in coarser {
        assert_eq!(at_least(date, 1), Value::Boolean(false));
    }
}
