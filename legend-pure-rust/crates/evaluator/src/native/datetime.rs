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

//! Date and time natives over [`crate::date`]: the constructors (`date`,
//! `parseDate`), the arithmetic (`adjust`, `dateDiff`, `datePart`), the
//! component accessors (`year`, `monthNumber`, `dayOfMonth`, `hour`,
//! `minute`, `second`) and the precision predicates (`hasDay`, `hasHour`,
//! `hasSubsecond`, …).
//!
//! All arithmetic and precision rules flow through `PureDate`.
//! `DurationUnit` values arrive as `Value::EnumValue` and are read by
//! their member name.

use legend_pure_metamodel::types::ValueSpec;

use crate::date::{DatePrecision, PureDate, TimePrecision};
use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all,
};
use crate::value::Value;

/// The time-granularity unit accepted by `adjust` and `dateDiff`.
///
/// Pure's `DurationUnit` enum values reach the runtime as
/// `Value::EnumValue`; the historical `Value::String("DurationUnit.<VARIANT>")`
/// shape is still accepted and parsed by suffix.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DurationUnit {
    /// Gregorian years.
    Years,
    /// Calendar months.
    Months,
    /// 7-day weeks.
    Weeks,
    /// Calendar days.
    Days,
    /// Hours.
    Hours,
    /// Minutes.
    Minutes,
    /// Seconds.
    Seconds,
    /// Milliseconds.
    Milliseconds,
    /// Microseconds.
    Microseconds,
    /// Nanoseconds.
    Nanoseconds,
}

/// Parse a `DurationUnit` from a Pure enum value (either typed
/// `Value::EnumValue` reading off `.member`, or a fallback string of
/// the form `"DurationUnit.DAYS"` / `"DAYS"`).
fn duration_unit(v: &Value) -> Result<DurationUnit, PureRuntimeError> {
    let suffix_owned;
    let suffix: &str = match v {
        Value::EnumValue { member, .. } => member.as_str(),
        Value::String(s) => {
            suffix_owned = s.rsplit('.').next().unwrap_or(s.as_str()).to_string();
            &suffix_owned[..]
        }
        other => {
            return Err(PureRuntimeError::type_mismatch("DurationUnit", other));
        }
    };
    match suffix {
        "YEARS" => Ok(DurationUnit::Years),
        "MONTHS" => Ok(DurationUnit::Months),
        "WEEKS" => Ok(DurationUnit::Weeks),
        "DAYS" => Ok(DurationUnit::Days),
        "HOURS" => Ok(DurationUnit::Hours),
        "MINUTES" => Ok(DurationUnit::Minutes),
        "SECONDS" => Ok(DurationUnit::Seconds),
        "MILLISECONDS" => Ok(DurationUnit::Milliseconds),
        "MICROSECONDS" => Ok(DurationUnit::Microseconds),
        "NANOSECONDS" => Ok(DurationUnit::Nanoseconds),
        other => Err(PureRuntimeError::EvaluationError(format!(
            "unknown DurationUnit: {other}"
        ))),
    }
}

/// Pure `year(Date[1]): Integer[1]`.
#[derive(Debug)]
pub struct Year;

impl NativeFunction for Year {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("year", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Integer(i64::from(d.get_year()))))
    }
}

/// Pure `monthNumber(Date[1]): Integer[1]`.
#[derive(Debug)]
pub struct MonthNumber;

impl NativeFunction for MonthNumber {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("monthNumber", &values, 1)?;
        let d = values[0].as_date()?;
        match d.get_month() {
            Some(m) => Ok(Evaluated::new(Value::Integer(i64::from(m)))),
            None => Err(PureRuntimeError::EvaluationError(
                "monthNumber: date has no month component".into(),
            )
            .into()),
        }
    }
}

/// Pure `dayOfMonth(Date[1]): Integer[1]`.
///
/// Throws `"Cannot get day of month for <date>"`
/// when the date has no day component (year-only, year-month).
#[derive(Debug)]
pub struct DayOfMonth;

impl NativeFunction for DayOfMonth {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("dayOfMonth", &values, 1)?;
        let d = values[0].as_date()?;
        match d.get_day() {
            Some(day) => Ok(Evaluated::new(Value::Integer(i64::from(day)))),
            None => Err(PureRuntimeError::EvaluationError(format!(
                "Cannot get day of month for {d}"
            ))
            .into()),
        }
    }
}

/// Pure `hour(DateTime[1]): Integer[1]`. Throws "Cannot get hour for
/// `<date>`" when no time component present.
#[derive(Debug)]
pub struct Hour;

impl NativeFunction for Hour {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hour", &values, 1)?;
        let d = values[0].as_date()?;
        match d.get_hour() {
            Some(h) => Ok(Evaluated::new(Value::Integer(i64::from(h)))),
            None => {
                Err(PureRuntimeError::EvaluationError(format!("Cannot get hour for {d}")).into())
            }
        }
    }
}

/// Pure `minute(DateTime[1]): Integer[1]`. Throws "Cannot get minute
/// for `<date>`" when no minute component.
#[derive(Debug)]
pub struct Minute;

impl NativeFunction for Minute {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("minute", &values, 1)?;
        let d = values[0].as_date()?;
        match d.get_minute() {
            Some(m) => Ok(Evaluated::new(Value::Integer(i64::from(m)))),
            None => {
                Err(PureRuntimeError::EvaluationError(format!("Cannot get minute for {d}")).into())
            }
        }
    }
}

/// Pure `second(DateTime[1]): Integer[1]`. Throws "Cannot get second
/// for `<date>`" when no second component.
#[derive(Debug)]
pub struct Second;

impl NativeFunction for Second {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("second", &values, 1)?;
        let d = values[0].as_date()?;
        match d.get_second() {
            Some(s) => Ok(Evaluated::new(Value::Integer(i64::from(s)))),
            None => {
                Err(PureRuntimeError::EvaluationError(format!("Cannot get second for {d}")).into())
            }
        }
    }
}

/// Pure `datePart(Date[1]): Date[1]` — drop the time component.
///
/// Day-or-finer precision drops to
/// strict-date (year+month+day, no time); year- or month-only inputs
/// pass through.
#[derive(Debug)]
pub struct DatePart;

impl NativeFunction for DatePart {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("datePart", &values, 1)?;
        let d = values[0].as_date()?;
        let Some(month) = d.get_month() else {
            return Ok(Evaluated::new(Value::Date(d)));
        };
        let Some(day) = d.get_day() else {
            return Ok(Evaluated::new(Value::Date(d)));
        };
        let year = d.get_year();
        let result = PureDate::strict_date(year, month, day).map(Value::Date)?;
        Ok(Evaluated::new(result))
    }
}

/// Count Sunday boundaries crossed between two civil dates.
///
/// Returns a positive integer when `b > a`, negative when `b < a`, and
/// 0 when they're equal. Forward intervals count Sundays in `(a, b]`;
/// backward intervals count Sundays in `[b, a)` and negate.
fn sunday_boundaries_between(a: jiff::civil::Date, b: jiff::civil::Date, days: i64) -> i64 {
    use jiff::civil::Weekday;
    /// Days until the *next* Sunday strictly after this weekday.
    /// Sunday → 7 (a full week to the next Sunday).
    fn to_next_sun(w: Weekday) -> i64 {
        match w {
            Weekday::Sunday => 7,
            Weekday::Monday => 6,
            Weekday::Tuesday => 5,
            Weekday::Wednesday => 4,
            Weekday::Thursday => 3,
            Weekday::Friday => 2,
            Weekday::Saturday => 1,
        }
    }
    /// Days from this weekday to the next Sunday on/after it (Sunday
    /// itself → 0).
    fn to_this_or_next_sun(w: Weekday) -> i64 {
        match w {
            Weekday::Sunday => 0,
            other => to_next_sun(other),
        }
    }
    if days == 0 {
        return 0;
    }
    if days > 0 {
        let span = days;
        let to_first = to_next_sun(a.weekday());
        if to_first > span {
            0
        } else {
            (span - to_first) / 7 + 1
        }
    } else {
        let span = -days;
        let to_first = to_this_or_next_sun(b.weekday());
        if to_first >= span {
            0
        } else {
            -(((span - to_first - 1) / 7) + 1)
        }
    }
}

/// Pure `dateDiff(Date[1], Date[1], DurationUnit[1]): Integer[1]`.
///
/// Returns the signed span `d2 - d1` in the requested unit. Sub-second
/// units require both inputs to have at least second precision; larger
/// units require the corresponding component on both sides.
#[derive(Debug)]
pub struct DateDiff;

impl NativeFunction for DateDiff {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("dateDiff", &values, 3)?;
        let d1 = values[0].as_date()?;
        let d2 = values[1].as_date()?;
        let unit = duration_unit(&values[2])?;

        let needs_time = matches!(
            unit,
            DurationUnit::Hours
                | DurationUnit::Minutes
                | DurationUnit::Seconds
                | DurationUnit::Milliseconds
                | DurationUnit::Microseconds
                | DurationUnit::Nanoseconds
        );

        if needs_time {
            let a = d1.to_civil_datetime()?;
            let b = d2.to_civil_datetime()?;
            let jiff_unit = match unit {
                DurationUnit::Hours => jiff::Unit::Hour,
                DurationUnit::Minutes => jiff::Unit::Minute,
                DurationUnit::Seconds => jiff::Unit::Second,
                DurationUnit::Milliseconds => jiff::Unit::Millisecond,
                DurationUnit::Microseconds => jiff::Unit::Microsecond,
                DurationUnit::Nanoseconds => jiff::Unit::Nanosecond,
                _ => unreachable!(),
            };
            let span = a
                .until((jiff_unit, b))
                .map_err(|e| PureRuntimeError::EvaluationError(format!("dateDiff: {e}")))?;
            let v = match unit {
                DurationUnit::Hours => i64::from(span.get_hours()),
                DurationUnit::Minutes => span.get_minutes(),
                DurationUnit::Seconds => span.get_seconds(),
                DurationUnit::Milliseconds => span.get_milliseconds(),
                DurationUnit::Microseconds => span.get_microseconds(),
                DurationUnit::Nanoseconds => span.get_nanoseconds(),
                _ => unreachable!(),
            };
            Ok(Evaluated::new(Value::Integer(v)))
        } else {
            let a = d1.to_civil_date()?;
            let b = d2.to_civil_date()?;
            let v = match unit {
                DurationUnit::Years => i64::from(b.year()) - i64::from(a.year()),
                DurationUnit::Months => {
                    (i64::from(b.year()) - i64::from(a.year())) * 12
                        + (i64::from(b.month()) - i64::from(a.month()))
                }
                DurationUnit::Days => {
                    let span = a
                        .until(b)
                        .map_err(|e| PureRuntimeError::EvaluationError(format!("dateDiff: {e}")))?;
                    i64::from(span.get_days())
                }
                DurationUnit::Weeks => {
                    let span = a
                        .until(b)
                        .map_err(|e| PureRuntimeError::EvaluationError(format!("dateDiff: {e}")))?;
                    let days = i64::from(span.get_days());
                    sunday_boundaries_between(a, b, days)
                }
                _ => unreachable!(),
            };
            Ok(Evaluated::new(Value::Integer(v)))
        }
    }
}

/// Pure `adjust(Date[1], Integer[1], DurationUnit[1]): Date[1]`.
///
/// Every unit goes through the matching `PureDate::add_*`; a week is
/// seven days. Sub-second units require at least second precision on
/// the input (see `PureDate::add_milliseconds`).
#[derive(Debug)]
pub struct Adjust;

impl NativeFunction for Adjust {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("adjust", &values, 3)?;
        let d = values[0].as_date()?;
        let n = values[1].as_integer()?;
        let unit = duration_unit(&values[2])?;

        let new_date = match unit {
            DurationUnit::Years => d.add_years(n),
            DurationUnit::Months => d.add_months(n),
            DurationUnit::Weeks => d.add_days(n.checked_mul(7).ok_or_else(|| {
                PureRuntimeError::EvaluationError("adjust: weeks overflow".into())
            })?),
            DurationUnit::Days => d.add_days(n),
            DurationUnit::Hours => d.add_hours(n),
            DurationUnit::Minutes => d.add_minutes(n),
            DurationUnit::Seconds => d.add_seconds(n),
            DurationUnit::Milliseconds => d.add_milliseconds(n),
            DurationUnit::Microseconds => d.add_microseconds(n),
            DurationUnit::Nanoseconds => d.add_nanoseconds(n),
        }?;
        Ok(Evaluated::new(Value::Date(new_date)))
    }
}

fn has_precision_at_least(d: &PureDate, at: DatePrecision) -> bool {
    match (d.precision(), at) {
        (DatePrecision::Year, DatePrecision::Year) => true,
        (p, DatePrecision::Year) => p >= DatePrecision::Year || matches!(p, DatePrecision::Time(_)),
        (p, DatePrecision::Month) => {
            p >= DatePrecision::Month || matches!(p, DatePrecision::Time(_))
        }
        (p, DatePrecision::Day) => p >= DatePrecision::Day || matches!(p, DatePrecision::Time(_)),
        (DatePrecision::Time(a), DatePrecision::Time(b)) => a >= b,
        (_, DatePrecision::Time(_)) => false,
    }
}

/// Pure `hasMonth(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasMonth;

impl NativeFunction for HasMonth {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasMonth", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(has_precision_at_least(
            &d,
            DatePrecision::Month,
        ))))
    }
}

/// Pure `hasDay(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasDay;

impl NativeFunction for HasDay {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasDay", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(has_precision_at_least(
            &d,
            DatePrecision::Day,
        ))))
    }
}

/// Pure `hasHour(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasHour;

impl NativeFunction for HasHour {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasHour", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(d.has_time())))
    }
}

/// Pure `hasMinute(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasMinute;

impl NativeFunction for HasMinute {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasMinute", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(matches!(
            d.precision(),
            DatePrecision::Time(tp) if tp >= TimePrecision::Minute
        ))))
    }
}

/// Pure `hasSecond(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasSecond;

impl NativeFunction for HasSecond {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasSecond", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(matches!(
            d.precision(),
            DatePrecision::Time(tp) if tp >= TimePrecision::Second
        ))))
    }
}

/// Pure `hasSubsecond(Date[1]): Boolean[1]`.
#[derive(Debug)]
pub struct HasSubsecond;

impl NativeFunction for HasSubsecond {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasSubsecond", &values, 1)?;
        let d = values[0].as_date()?;
        Ok(Evaluated::new(Value::Boolean(matches!(
            d.precision(),
            DatePrecision::Time(TimePrecision::Subsecond(_))
        ))))
    }
}

/// Pure `hasSubsecondWithAtLeastPrecision(Date[1], Integer[1]): Boolean[1]`.
///
/// The second argument is the required number of subsecond digits (1–9).
/// Returns `true` iff the date carries subsecond precision whose digit
/// count is at least that value.
#[derive(Debug)]
pub struct HasSubsecondWithAtLeastPrecision;

impl NativeFunction for HasSubsecondWithAtLeastPrecision {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("hasSubsecondWithAtLeastPrecision", &values, 2)?;
        let d = values[0].as_date()?;
        let required = values[1].as_integer()?;
        let have = match d.precision() {
            DatePrecision::Time(TimePrecision::Subsecond(digits)) => i64::from(digits),
            _ => return Ok(Evaluated::new(Value::Boolean(false))),
        };
        Ok(Evaluated::new(Value::Boolean(have >= required)))
    }
}

/// Pure `parseDate(String[1]): Date[1]`.
///
/// Accepts the same family of ISO-like literals the Pure compiler
/// recognises for `%`-prefixed date literals:
/// * `"YYYY[-M[-D[THH[:MM[:SS[.fffffffff]]]][Z|±HHMM]]]"`
/// * `"YYYY-MM-DD"`                                   → `StrictDate`
///
/// Single-digit month/day are accepted (`"2014-2-27T…"`). A trailing
/// `Z` is treated as the UTC offset `+0000`. `±HHMM` shifts the
/// resulting instant to UTC, mirroring the evaluator's date-literal
/// path. Returns `EvaluationError` when no shape matches.
#[derive(Debug)]
pub struct ParseDate;

impl NativeFunction for ParseDate {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("parseDate", &values, 1)?;
        let raw = values[0].as_string()?;
        let s = raw.as_str();
        let date = parse_date_lenient(s).ok_or_else(|| {
            PureRuntimeError::EvaluationError(format!("parseDate: invalid date string {s:?}"))
        })?;
        Ok(Evaluated::new(Value::Date(date)))
    }
}

/// Parse a Pure-flavoured ISO date string into a [`PureDate`].
/// Returns `None`
/// when no recognised shape matches.
fn parse_date_lenient(input: &str) -> Option<PureDate> {
    let s = input.strip_prefix('%').unwrap_or(input);
    let (date_part, time_part) = match s.split_once('T') {
        Some((d, t)) => (d, Some(t)),
        None => (s, None),
    };
    let date_parts: Vec<&str> = date_part.split('-').collect();
    if date_parts.is_empty() || date_parts.len() > 3 {
        return None;
    }
    let year: i16 = date_parts.first()?.parse().ok()?;
    let month: Option<i8> = date_parts.get(1).and_then(|s| s.parse().ok());
    let day: Option<i8> = date_parts.get(2).and_then(|s| s.parse().ok());

    let Some(time_part) = time_part else {
        return match (month, day) {
            (Some(m), Some(d)) => PureDate::strict_date(year, m, d).ok(),
            (Some(m), None) => PureDate::year_month(year, m).ok(),
            (None, None) => PureDate::year(year).ok(),
            _ => None,
        };
    };

    let (time_part, zulu) = match time_part.strip_suffix('Z') {
        Some(rest) => (rest, true),
        None => (time_part, false),
    };
    let (time_body, parsed_offset) = split_tz_native(time_part);
    let tz_offset_minutes: Option<i16> = if zulu { Some(0) } else { parsed_offset };

    let (time_main, subsec_str) = match time_body.split_once('.') {
        Some((main, frac)) => (main, frac),
        None => (time_body, ""),
    };
    let time_components: Vec<&str> = time_main.split(':').collect();
    if time_components.is_empty() {
        return None;
    }
    let hour: i8 = time_components.first()?.parse().ok()?;
    let minute: i8 = time_components
        .get(1)
        .and_then(|s| s.parse().ok())
        .unwrap_or(0);
    let second: i8 = time_components
        .get(2)
        .and_then(|s| s.parse().ok())
        .unwrap_or(0);
    let (subsec_nanos, subsec_digits) = parse_subsecond_lenient(subsec_str);

    let m = month?;
    let d = day?;
    let civil = jiff::civil::DateTime::new(year, m, d, hour, minute, second, subsec_nanos).ok()?;
    let shifted = if let Some(offset) = tz_offset_minutes {
        civil
            .checked_sub(jiff::Span::new().minutes(i64::from(offset)))
            .ok()?
    } else {
        civil
    };
    let precision = if subsec_digits > 0 {
        TimePrecision::Subsecond(subsec_digits)
    } else if time_components.len() >= 3 {
        TimePrecision::Second
    } else if time_components.len() >= 2 {
        TimePrecision::Minute
    } else {
        TimePrecision::Hour
    };
    PureDate::datetime(
        shifted.year(),
        shifted.month(),
        shifted.day(),
        shifted.hour(),
        shifted.minute(),
        shifted.second(),
        shifted.subsec_nanosecond(),
        precision,
    )
    .ok()
}

/// Find the trailing `±HHMM` offset on a time string and split it off.
fn split_tz_native(s: &str) -> (&str, Option<i16>) {
    for (idx, _) in s.char_indices().rev() {
        let byte = s.as_bytes()[idx];
        if byte == b'+' || byte == b'-' {
            let tail = &s[idx..];
            if tail.len() == 5 && tail.as_bytes()[1..].iter().all(u8::is_ascii_digit) {
                let sign: i16 = if byte == b'+' { 1 } else { -1 };
                let hh: i16 = tail[1..3].parse().unwrap_or(0);
                let mm: i16 = tail[3..5].parse().unwrap_or(0);
                return (&s[..idx], Some(sign * (hh * 60 + mm)));
            }
        }
    }
    (s, None)
}

/// Pad a fractional-second string out to 9 nanos, returning the
/// nanoseconds and the number of digits written.
fn parse_subsecond_lenient(frac: &str) -> (i32, u8) {
    if frac.is_empty() {
        return (0, 0);
    }
    let trimmed: String = frac.chars().take(9).collect();
    #[allow(clippy::cast_possible_truncation)]
    let digits: u8 = trimmed.len() as u8;
    let mut padded = String::with_capacity(9);
    padded.push_str(&trimmed);
    while padded.len() < 9 {
        padded.push('0');
    }
    (padded.parse().unwrap_or(0), digits)
}

fn i64_to_i16_arg(name: &str, n: i64) -> Result<i16, PureRuntimeError> {
    i16::try_from(n)
        .map_err(|_| PureRuntimeError::EvaluationError(format!("{name}: year {n} out of range")))
}

/// Saturating i64-to-i8 cast for date components, used by
/// [`DateConstruct`].
fn i64_to_i8_arg_lenient(_name: &str, n: i64) -> i8 {
    #[allow(clippy::cast_possible_truncation)]
    let clamped = n.clamp(i64::from(i8::MIN), i64::from(i8::MAX)) as i8;
    clamped
}

/// Split the `second:Number[1]` argument of the 6-arg `date(...)`
/// constructor into integer seconds, subsecond nanoseconds, and
/// (when applicable) the source's subsecond-digit count.
///
/// Returns `(seconds, nanos, subsec_digits)`:
/// - `Integer` → `(n, 0, None)` (no subsecond precision)
/// - `Float`   → split via trunc/fract; digit count derived from
///   nanos (3/6/9, like `PureDate::from_civil_datetime`). When the
///   fractional part is exactly zero the user still wrote a Float
///   literal (`11.0`) and expects 1-digit subsecond rendering, so
///   `subsec_digits` is `Some(1)`.
/// - `Decimal` → split via `trunc()`/scale-aware extraction; the
///   `Decimal::scale()` method gives the original literal's exact
///   trailing-digit count, so `59.999D` → `Some(3)` and `11.0D` →
///   `Some(1)`.
fn decompose_second(v: &Value) -> Result<(i64, i32, Option<u8>), PureException> {
    use rust_decimal::Decimal;
    use rust_decimal::prelude::ToPrimitive;
    match v {
        Value::Integer(n) => Ok((*n, 0, None)),
        Value::Float(f) => {
            let trunc = f.trunc();
            let frac = f - trunc;
            #[allow(clippy::cast_possible_truncation)]
            let nanos = (frac * 1_000_000_000.0).round() as i32;
            let digits = if nanos == 0 {
                1
            } else if nanos % 1_000_000 == 0 {
                3
            } else if nanos % 1_000 == 0 {
                6
            } else {
                9
            };
            #[allow(clippy::cast_possible_truncation)]
            let s = trunc as i64;
            Ok((s, nanos, Some(digits)))
        }
        Value::Decimal(d) => {
            let int_part = d.trunc();
            let frac_part = (*d) - int_part;
            #[allow(clippy::cast_possible_truncation)]
            let scale = d.scale().min(9) as u8;
            let nanos_dec = frac_part * Decimal::from(1_000_000_000_i64);
            let nanos = nanos_dec.round().to_i32().unwrap_or(0);
            let s = int_part.to_i64().unwrap_or(0);
            let digits = if scale == 0 { 1 } else { scale };
            Ok((s, nanos, Some(digits)))
        }
        other => Err(PureRuntimeError::type_mismatch("Number", other).into()),
    }
}

/// Translate a `PureRuntimeError` from a `PureDate` constructor
/// (which wraps `jiff::Error`) into the Java-Pure
/// `"Invalid <component>: <value>"` shape that platform
/// `assertError` tests pin. Falls back to the original message when
/// the jiff text doesn't match a known shape.
///
/// Input mapping mirrors `DateConstruct`'s positional contract:
/// `values[0]=year, [1]=month, [2]=day, [3]=hour, [4]=minute, [5]=second`.
/// Pulled values are the original `i64` inputs, so out-of-range
/// arguments surface verbatim instead of the saturated value jiff
/// rejected.
fn translate_date_error(err: PureRuntimeError, values: &[Value]) -> PureRuntimeError {
    let msg = err.to_string();
    let Some(after_param) = msg.split("parameter '").nth(1) else {
        return err;
    };
    let Some(name) = after_param.split('\'').next() else {
        return err;
    };
    let value_at =
        |idx: usize| -> Option<i64> { values.get(idx).and_then(|v| v.as_integer().ok()) };
    match name {
        "month" => {
            if let Some(v) = value_at(1) {
                return PureRuntimeError::EvaluationError(format!("Invalid month: {v}"));
            }
        }
        "day" => {
            if let (Some(y), Some(m), Some(d)) = (value_at(0), value_at(1), value_at(2)) {
                return PureRuntimeError::EvaluationError(format!(
                    "Invalid day: {y:04}-{m:02}-{d}"
                ));
            }
        }
        "hour" => {
            if let Some(v) = value_at(3) {
                return PureRuntimeError::EvaluationError(format!("Invalid hour: {v}"));
            }
        }
        "minute" => {
            if let Some(v) = value_at(4) {
                return PureRuntimeError::EvaluationError(format!("Invalid minute: {v}"));
            }
        }
        "second" => {
            if let Some(v) = value_at(5) {
                return PureRuntimeError::EvaluationError(format!("Invalid second: {v}"));
            }
        }
        _ => {}
    }
    err
}

/// Pure `date(Integer[1], ...): Date[1]` — construct a date from 1–6
/// Integer components (year; year+month; year+month+day; …; +hour; +min;
/// +sec). Arity selects precision.
#[derive(Debug)]
pub struct DateConstruct;

impl NativeFunction for DateConstruct {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        if values.is_empty() || values.len() > 6 {
            return Err(PureRuntimeError::EvaluationError(format!(
                "date: expected 1..=6 arguments, got {}",
                values.len()
            ))
            .into());
        }

        let year_raw = values[0].as_integer()?;
        let year = i64_to_i16_arg("date", year_raw)?;
        if values.len() == 1 {
            return PureDate::year(year)
                .map(Value::Date)
                .map(Evaluated::new)
                .map_err(|e| translate_date_error(e, &values).into());
        }

        let month_raw = values[1].as_integer()?;
        let month = i64_to_i8_arg_lenient("date", month_raw);
        if values.len() == 2 {
            return PureDate::year_month(year, month)
                .map(Value::Date)
                .map(Evaluated::new)
                .map_err(|e| translate_date_error(e, &values).into());
        }

        let day_raw = values[2].as_integer()?;
        let day = i64_to_i8_arg_lenient("date", day_raw);
        if values.len() == 3 {
            return PureDate::strict_date(year, month, day)
                .map(Value::Date)
                .map(Evaluated::new)
                .map_err(|e| translate_date_error(e, &values).into());
        }

        let hour_raw = values[3].as_integer()?;
        let hour = i64_to_i8_arg_lenient("date", hour_raw);
        if values.len() == 4 {
            return PureDate::datetime(year, month, day, hour, 0, 0, 0, TimePrecision::Hour)
                .map(Value::Date)
                .map(Evaluated::new)
                .map_err(|e| translate_date_error(e, &values).into());
        }

        let minute_raw = values[4].as_integer()?;
        let minute = i64_to_i8_arg_lenient("date", minute_raw);
        if values.len() == 5 {
            return PureDate::datetime(year, month, day, hour, minute, 0, 0, TimePrecision::Minute)
                .map(Value::Date)
                .map(Evaluated::new)
                .map_err(|e| translate_date_error(e, &values).into());
        }

        let (second_int, subsec_nanos, sub_digits) = decompose_second(&values[5])?;
        let second = i64_to_i8_arg_lenient("date", second_int);
        let precision = if let Some(d) = sub_digits {
            TimePrecision::Subsecond(d)
        } else {
            TimePrecision::Second
        };
        PureDate::datetime(
            year,
            month,
            day,
            hour,
            minute,
            second,
            subsec_nanos,
            precision,
        )
        .map(Value::Date)
        .map(Evaluated::new)
        .map_err(|e| translate_date_error(e, &values).into())
    }
}

/// Register the fished date and time natives under their platform keys.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("year_Date_1__Integer_1_", Year);
    registry.register("monthNumber_Date_1__Integer_1_", MonthNumber);
    registry.register("dayOfMonth_Date_1__Integer_1_", DayOfMonth);
    registry.register("hour_Date_1__Integer_1_", Hour);
    registry.register("minute_Date_1__Integer_1_", Minute);
    registry.register("second_Date_1__Integer_1_", Second);

    registry.register("datePart_Date_1__Date_1_", DatePart);
    registry.register(
        "dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_",
        DateDiff,
    );
    registry.register("adjust_Date_1__Integer_1__DurationUnit_1__Date_1_", Adjust);

    registry.register("hasMonth_Date_1__Boolean_1_", HasMonth);
    registry.register("hasDay_Date_1__Boolean_1_", HasDay);
    registry.register("hasHour_Date_1__Boolean_1_", HasHour);
    registry.register("hasMinute_Date_1__Boolean_1_", HasMinute);
    registry.register("hasSecond_Date_1__Boolean_1_", HasSecond);
    registry.register("hasSubsecond_Date_1__Boolean_1_", HasSubsecond);
    registry.register(
        "hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_",
        HasSubsecondWithAtLeastPrecision,
    );

    registry.register("parseDate_String_1__Date_1_", ParseDate);

    registry.register("date_Integer_1__Date_1_", DateConstruct);
    registry.register("date_Integer_1__Integer_1__Date_1_", DateConstruct);
    registry.register(
        "date_Integer_1__Integer_1__Integer_1__StrictDate_1_",
        DateConstruct,
    );
    registry.register(
        "date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
        DateConstruct,
    );
    registry.register(
        "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_",
        DateConstruct,
    );
    registry.register(
        "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_",
        DateConstruct,
    );
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_int, lit_str};

    fn date(y: i16, m: i8, d: i8) -> Value {
        Value::Date(PureDate::strict_date(y, m, d).unwrap())
    }

    #[allow(clippy::many_single_char_names)]
    fn datetime_s(y: i16, m: i8, d: i8, h: i8, mi: i8, s: i8) -> Value {
        Value::Date(PureDate::datetime(y, m, d, h, mi, s, 0, TimePrecision::Second).unwrap())
    }

    fn error_message(err: &PureException) -> String {
        match &err.kind {
            crate::error::PureExceptionKind::ExecutionError(e) => e.to_string(),
            other => panic!("expected an execution error, got {other:?}"),
        }
    }

    #[test]
    fn duration_unit_fqn_form() {
        assert_eq!(
            duration_unit(&Value::String("DurationUnit.DAYS".into())).unwrap(),
            DurationUnit::Days
        );
    }

    #[test]
    fn duration_unit_bare_form() {
        assert_eq!(
            duration_unit(&Value::String("YEARS".into())).unwrap(),
            DurationUnit::Years
        );
    }

    #[test]
    fn duration_unit_unknown_errors() {
        assert!(duration_unit(&Value::String("WEEKS_AND_DAYS".into())).is_err());
    }

    #[test]
    fn duration_unit_type_mismatch_errors() {
        assert!(duration_unit(&Value::Integer(5)).is_err());
    }

    #[test]
    fn year_wrong_arg_count() {
        assert!(Year.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn year_type_mismatch() {
        assert!(Year.execute(&[lit_int(5)], &mut MockCtx).is_err());
    }

    #[test]
    fn date_part_wrong_arg_count() {
        assert!(DatePart.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn date_part_type_mismatch() {
        assert!(DatePart.execute(&[lit_int(5)], &mut MockCtx).is_err());
    }

    #[test]
    fn date_diff_wrong_arg_count() {
        assert!(DateDiff.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn adjust_wrong_arg_count() {
        assert!(Adjust.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn has_subsecond_with_at_least_precision_wrong_args() {
        assert!(
            HasSubsecondWithAtLeastPrecision
                .execute(&[], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_date_strict() {
        let r = ParseDate
            .execute(&[lit_str("2024-03-15")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), date(2024, 3, 15));
    }

    #[test]
    fn parse_date_datetime() {
        let r = ParseDate
            .execute(&[lit_str("2024-03-15T10:30:45")], &mut MockCtx)
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date");
        };
        assert_eq!(pd.get_hour(), Some(10));
        assert_eq!(pd.get_minute(), Some(30));
        assert_eq!(pd.get_second(), Some(45));
    }

    #[test]
    fn parse_date_invalid() {
        assert!(
            ParseDate
                .execute(&[lit_str("not-a-date")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_date_type_mismatch() {
        assert!(
            ParseDate
                .execute(&[lit_int(20_240_315)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn parse_date_wrong_arg_count() {
        assert!(ParseDate.execute(&[], &mut MockCtx).is_err());
    }

    /// testParseDate / testParseDateWithZ / testParseDateWithTimezone:
    /// single-digit month, `Z`, and a `-0500` offset shifted to UTC.
    #[test]
    fn parse_date_offsets_land_in_utc() {
        let parse = |s: &str| {
            let Value::Date(pd) = ParseDate
                .execute(&[lit_str(s)], &mut MockCtx)
                .unwrap()
                .into_value()
            else {
                panic!("expected Date")
            };
            pd
        };
        let expected = PureDate::datetime(
            2014,
            2,
            27,
            10,
            1,
            35,
            231_000_000,
            TimePrecision::Subsecond(3),
        )
        .unwrap();
        assert_eq!(parse("2014-2-27T10:01:35.231"), expected);
        assert_eq!(parse("2014-02-27T10:01:35.231Z"), expected);
        assert_eq!(
            parse("2014-02-27T10:01:35.231-0500"),
            PureDate::datetime(
                2014,
                2,
                27,
                15,
                1,
                35,
                231_000_000,
                TimePrecision::Subsecond(3)
            )
            .unwrap()
        );
    }

    #[test]
    fn date_construct_year() {
        let r = DateConstruct
            .execute(&[lit_int(2024)], &mut MockCtx)
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date")
        };
        assert_eq!(pd.precision(), DatePrecision::Year);
        assert_eq!(pd.get_year(), 2024);
    }

    #[test]
    fn date_construct_year_month() {
        let r = DateConstruct
            .execute(&[lit_int(2024), lit_int(3)], &mut MockCtx)
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date")
        };
        assert_eq!(pd.precision(), DatePrecision::Month);
        assert_eq!(pd.get_month(), Some(3));
    }

    #[test]
    fn date_construct_strict_date() {
        let r = DateConstruct
            .execute(&[lit_int(2024), lit_int(3), lit_int(15)], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), date(2024, 3, 15));
    }

    #[test]
    fn date_construct_hour() {
        let r = DateConstruct
            .execute(
                &[lit_int(2024), lit_int(3), lit_int(15), lit_int(10)],
                &mut MockCtx,
            )
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date")
        };
        assert_eq!(pd.precision(), DatePrecision::Time(TimePrecision::Hour));
        assert_eq!(pd.get_hour(), Some(10));
    }

    #[test]
    fn date_construct_minute() {
        let r = DateConstruct
            .execute(
                &[
                    lit_int(2024),
                    lit_int(3),
                    lit_int(15),
                    lit_int(10),
                    lit_int(30),
                ],
                &mut MockCtx,
            )
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date")
        };
        assert_eq!(pd.precision(), DatePrecision::Time(TimePrecision::Minute));
        assert_eq!(pd.get_minute(), Some(30));
    }

    #[test]
    fn date_construct_second() {
        let r = DateConstruct
            .execute(
                &[
                    lit_int(2024),
                    lit_int(3),
                    lit_int(15),
                    lit_int(10),
                    lit_int(30),
                    lit_int(45),
                ],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), datetime_s(2024, 3, 15, 10, 30, 45));
    }

    /// `testDateFromSubSecond`: a Float second carries subsecond precision.
    #[test]
    fn date_construct_float_second() {
        let r = DateConstruct
            .execute(
                &[
                    lit_int(2015),
                    lit_int(4),
                    lit_int(16),
                    lit_int(14),
                    lit_int(51),
                    crate::native::lit_float(59.999),
                ],
                &mut MockCtx,
            )
            .unwrap();
        let Value::Date(pd) = r.into_value() else {
            panic!("expected Date")
        };
        assert_eq!(
            pd.precision(),
            DatePrecision::Time(TimePrecision::Subsecond(3))
        );
        assert_eq!(pd.get_second(), Some(59));
        assert_eq!(pd.get_subsec_nanosecond(), Some(999_000_000));
    }

    #[test]
    fn date_construct_empty_errors() {
        assert!(DateConstruct.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn date_construct_too_many_errors() {
        let args: Vec<_> = (0..7).map(|_| lit_int(1)).collect();
        assert!(DateConstruct.execute(&args, &mut MockCtx).is_err());
    }

    #[test]
    fn date_construct_invalid_month_errors() {
        assert!(
            DateConstruct
                .execute(&[lit_int(2024), lit_int(13)], &mut MockCtx)
                .is_err()
        );
    }

    /// `testNewDateError` pins Java's message text verbatim.
    #[test]
    fn date_construct_errors_carry_the_java_messages() {
        let err = |args: &[ValueSpec]| {
            error_message(&DateConstruct.execute(args, &mut MockCtx).unwrap_err())
        };
        assert_eq!(err(&[lit_int(2016), lit_int(13)]), "Invalid month: 13");
        assert_eq!(
            err(&[lit_int(2016), lit_int(12), lit_int(32)]),
            "Invalid day: 2016-12-32"
        );
        assert_eq!(
            err(&[lit_int(2016), lit_int(12), lit_int(31), lit_int(24)]),
            "Invalid hour: 24"
        );
    }

    #[test]
    fn date_construct_year_out_of_range() {
        assert!(
            DateConstruct
                .execute(&[lit_int(10_000_000)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn date_construct_type_mismatch() {
        assert!(
            DateConstruct
                .execute(&[lit_str("2024")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn register_populates_registry() {
        let mut reg = NativeRegistry::new();
        register(&mut reg);
        assert!(
            reg.get("adjust_Date_1__Integer_1__DurationUnit_1__Date_1_")
                .is_some()
        );
        assert!(
            reg.get("dateDiff_Date_1__Date_1__DurationUnit_1__Integer_1_")
                .is_some()
        );
        assert!(reg.get("year_Date_1__Integer_1_").is_some());
        assert!(reg.get("parseDate_String_1__Date_1_").is_some());
        assert!(reg.get("date_Integer_1__Date_1_").is_some());
        assert!(
            reg.get(
                "date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__Number_1__DateTime_1_"
            )
            .is_some()
        );
    }
}
