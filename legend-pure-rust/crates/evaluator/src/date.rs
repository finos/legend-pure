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

//! Pure date types — variable-precision temporal values.
//!
//! Pure has a unique date model where values can have different precision
//! levels. A `%2024` is a year-only date, `%2024-03-15` is a strict date,
//! and `%2024-03-15T10:30:00.123+0500` is a datetime with subseconds
//! (stored as UTC after parsing).
//!
//! # Design
//!
//! The Java `PureDate` interface has seven concrete implementations
//! (`Year`, `YearMonth`, `StrictDate`, `DateWithHour`, `DateWithMinute`,
//! `DateWithSecond`, `DateWithSubsecond`). We consolidate these into a
//! single enum with four variants, using `jiff::civil` types for
//! `StrictDate` and `DateTime` to get native calendar arithmetic.
//!
//! # Timezone Handling
//!
//! Pure values are always stored as **UTC**. When a datetime is parsed
//! with a timezone offset (e.g., `+0500`), the offset is applied to
//! convert to UTC and then discarded. When formatting, the `format()`
//! native function accepts a `[America/New_York]` prefix to convert
//! from UTC to a target timezone for **display only** — the stored
//! value is never modified.
//!
//! # `StrictTime`
//!
//! `StrictTime` (time without date) is a separate value — not part of
//! this enum. It uses [`jiff::civil::Time`] directly.

use std::fmt;

use crate::error::PureRuntimeError;

/// The precision level of the time component in a [`PureDate`].
///
/// Pure dates have variable time precision: a value can have just the
/// hour, or hour+minute, or full second/subsecond resolution. This tag
/// tracks the original precision for correct serialization.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum TimePrecision {
    /// Only hour: `%2024-03-15T10`
    Hour,
    /// Hour + minute: `%2024-03-15T10:30`
    Minute,
    /// Hour + minute + second: `%2024-03-15T10:30:00`
    Second,
    /// Full subsecond: `%2024-03-15T10:30:00.123456789`
    /// The `u8` stores the number of subsecond digits (1-9) for
    /// serialization fidelity: `"100"` (3 digits) vs `"1"` (1 digit).
    Subsecond(u8),
}

/// A Pure temporal value with variable precision.
///
/// Mirrors the Java `PureDate` hierarchy:
///
/// ```text
/// %2024                          → Year
/// %2024-03                       → YearMonth
/// %2024-03-15                    → StrictDate (jiff::civil::Date)
/// %2024-03-15T10                 → DateTime(precision: Hour)
/// %2024-03-15T10:30+0500         → DateTime(precision: Minute) [UTC-adjusted]
/// %2024-03-15T10:30:00           → DateTime(precision: Second)
/// %2024-03-15T10:30:00.123456789 → DateTime(precision: Subsecond(9))
/// ```
///
/// All datetime values are stored as **UTC**. Timezone offsets from input
/// are applied during parsing and discarded.
///
/// # Size
///
/// The largest variant (`DateTime`) holds a `jiff::civil::DateTime` plus
/// a `TimePrecision` tag. Both `PureDate` and `rust_decimal::Decimal`
/// are `Copy`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct PureDate {
    /// The full datetime representation. For variants with lower precision,
    /// the unused fields are set to their minimum valid values (month=1,
    /// day=1, hour=0, minute=0, second=0, nanosecond=0).
    inner: jiff::civil::DateTime,
    /// Which components are "active" — determines serialization format.
    precision: DatePrecision,
}

/// The precision level of a [`PureDate`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub enum DatePrecision {
    /// Year only: `%2024`
    Year,
    /// Year + Month: `%2024-03`
    Month,
    /// Year + Month + Day (StrictDate): `%2024-03-15`
    Day,
    /// Year + Month + Day + Time with specified precision
    Time(TimePrecision),
}

impl PureDate {
    /// Create a year-only date.
    ///
    /// # Errors
    /// Returns an error if the year is out of range.
    pub fn year(year: i16) -> Result<Self, PureRuntimeError> {
        let dt = jiff::civil::DateTime::new(year, 1, 1, 0, 0, 0, 0)
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Invalid year: {e}")))?;
        Ok(Self {
            inner: dt,
            precision: DatePrecision::Year,
        })
    }

    /// Create a year-month date.
    ///
    /// # Errors
    /// Returns an error if the year or month is out of range.
    pub fn year_month(year: i16, month: i8) -> Result<Self, PureRuntimeError> {
        let dt = jiff::civil::DateTime::new(year, month, 1, 0, 0, 0, 0)
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Invalid year-month: {e}")))?;
        Ok(Self {
            inner: dt,
            precision: DatePrecision::Month,
        })
    }

    /// Create a strict date (year + month + day, no time).
    ///
    /// # Errors
    /// Returns an error if the date is invalid.
    pub fn strict_date(year: i16, month: i8, day: i8) -> Result<Self, PureRuntimeError> {
        let dt = jiff::civil::DateTime::new(year, month, day, 0, 0, 0, 0)
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Invalid date: {e}")))?;
        Ok(Self {
            inner: dt,
            precision: DatePrecision::Day,
        })
    }

    /// Create a datetime with the specified time precision.
    ///
    /// # Errors
    /// Returns an error if the datetime components are invalid.
    #[allow(clippy::too_many_arguments)]
    pub fn datetime(
        year: i16,
        month: i8,
        day: i8,
        hour: i8,
        minute: i8,
        second: i8,
        nanosecond: i32,
        precision: TimePrecision,
    ) -> Result<Self, PureRuntimeError> {
        let dt = jiff::civil::DateTime::new(year, month, day, hour, minute, second, nanosecond)
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Invalid datetime: {e}")))?;
        Ok(Self {
            inner: dt,
            precision: DatePrecision::Time(precision),
        })
    }

    /// Create from a [`jiff::civil::Date`] (`StrictDate`).
    #[must_use]
    pub fn from_civil_date(date: jiff::civil::Date) -> Self {
        Self {
            inner: date.at(0, 0, 0, 0),
            precision: DatePrecision::Day,
        }
    }

    /// Create from a `jiff::civil::DateTime` with subsecond precision.
    #[must_use]
    pub fn from_civil_datetime(dt: jiff::civil::DateTime) -> Self {
        let precision = if dt.subsec_nanosecond() != 0 {
            // Count significant digits in the subsecond
            let nanos = dt.subsec_nanosecond();
            let digits = if nanos % 1_000_000 == 0 {
                3 // millisecond precision
            } else if nanos % 1_000 == 0 {
                6 // microsecond precision
            } else {
                9 // nanosecond precision
            };
            DatePrecision::Time(TimePrecision::Subsecond(digits))
        } else {
            DatePrecision::Time(TimePrecision::Second)
        };
        Self {
            inner: dt,
            precision,
        }
    }

    /// Get the year.
    #[must_use]
    pub fn get_year(&self) -> i16 {
        self.inner.year()
    }

    /// Get the month (1-12), if this date has month precision.
    #[must_use]
    pub fn get_month(&self) -> Option<i8> {
        if self.precision >= DatePrecision::Month {
            Some(self.inner.month())
        } else {
            None
        }
    }

    /// Get the day (1-31), if this date has day precision.
    #[must_use]
    pub fn get_day(&self) -> Option<i8> {
        if self.precision >= DatePrecision::Day {
            Some(self.inner.day())
        } else {
            None
        }
    }

    /// Whether this date has a time component.
    #[must_use]
    pub fn has_time(&self) -> bool {
        matches!(self.precision, DatePrecision::Time(_))
    }

    /// Get the hour (0-23), if this date has time precision.
    #[must_use]
    pub fn get_hour(&self) -> Option<i8> {
        if let DatePrecision::Time(_) = self.precision {
            Some(self.inner.hour())
        } else {
            None
        }
    }

    /// Get the minute (0-59), if available.
    #[must_use]
    pub fn get_minute(&self) -> Option<i8> {
        match self.precision {
            DatePrecision::Time(tp) if tp >= TimePrecision::Minute => Some(self.inner.minute()),
            _ => None,
        }
    }

    /// Get the second (0-59), if available.
    #[must_use]
    pub fn get_second(&self) -> Option<i8> {
        match self.precision {
            DatePrecision::Time(tp) if tp >= TimePrecision::Second => Some(self.inner.second()),
            _ => None,
        }
    }

    /// Get the subsecond nanoseconds, if available.
    #[must_use]
    pub fn get_subsec_nanosecond(&self) -> Option<i32> {
        if let DatePrecision::Time(TimePrecision::Subsecond(_)) = self.precision {
            Some(self.inner.subsec_nanosecond())
        } else {
            None
        }
    }

    /// The precision of this date.
    #[must_use]
    pub fn precision(&self) -> DatePrecision {
        self.precision
    }

    /// Borrow the underlying `jiff::civil::DateTime` directly. Useful
    /// for low-level format-string formatters that need every component
    /// without going through precision-gated accessors. Year- and
    /// month-only dates default month=1, day=1, hour=0, etc., so every
    /// field is well-formed.
    #[must_use]
    pub fn inner_datetime(&self) -> jiff::civil::DateTime {
        self.inner
    }

    /// Get the underlying `jiff::civil::Date`.
    ///
    /// Year- and month-only `PureDates` default month=1 and day=1, so
    /// the underlying date is well-formed at every precision. The
    /// returned date is meaningful for any precision-tolerant use:
    /// `dateDiff(%2015, %2016, YEARS)` measures Jan 1 → Jan 1 = 1 yr,
    /// matching Java Pure semantics where year-precision dates compare
    /// at their nominal start. Callers that need a *strict* day-level
    /// guarantee (e.g. `datePart` returning a `StrictDate`) should still
    /// gate on `precision >= DatePrecision::Day` themselves.
    /// # Errors
    ///
    /// Always returns `Ok`; the `Result` is kept for API symmetry with `to_civil_datetime`.
    #[allow(clippy::unnecessary_wraps)]
    pub fn to_civil_date(&self) -> Result<jiff::civil::Date, PureRuntimeError> {
        Ok(self.inner.date())
    }

    /// Get the underlying `jiff::civil::DateTime`.
    ///
    /// # Errors
    /// Returns an error if this date doesn't have time precision.
    pub fn to_civil_datetime(&self) -> Result<jiff::civil::DateTime, PureRuntimeError> {
        if self.has_time() {
            Ok(self.inner)
        } else {
            Err(PureRuntimeError::EvaluationError(
                "Date does not have time precision".into(),
            ))
        }
    }

    /// Add years (works at all precision levels).
    ///
    /// # Errors
    /// Returns an error if the result overflows.
    pub fn add_years(&self, years: i64) -> Result<Self, PureRuntimeError> {
        // jiff::Span clamps each unit to ±19998 years; large adjustments
        // outside that range can't be expressed as a Span and would
        // panic on construction. Surface as a clean overflow error
        // (BigNumber tests expecting years like 800_002_016 fall here).
        let new_dt = jiff::Span::new()
            .try_years(years)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add months (requires at least month precision).
    ///
    /// # Errors
    /// Returns an error if the result overflows or precision is insufficient.
    pub fn add_months(&self, months: i64) -> Result<Self, PureRuntimeError> {
        if self.precision < DatePrecision::Month {
            return Err(PureRuntimeError::EvaluationError(
                "Cannot add months to a year-only date".into(),
            ));
        }
        let new_dt = jiff::Span::new()
            .try_months(months)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add days (requires at least day precision).
    ///
    /// # Errors
    /// Returns an error if the result overflows or precision is insufficient.
    pub fn add_days(&self, days: i64) -> Result<Self, PureRuntimeError> {
        if self.precision < DatePrecision::Day {
            return Err(PureRuntimeError::EvaluationError(
                "Cannot add days to a date without day precision".into(),
            ));
        }
        let new_dt = jiff::Span::new()
            .try_days(days)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add hours (requires time precision).
    ///
    /// # Errors
    /// Returns an error if the result overflows or precision is insufficient.
    pub fn add_hours(&self, hours: i64) -> Result<Self, PureRuntimeError> {
        if !self.has_time() {
            return Err(PureRuntimeError::EvaluationError(
                "Cannot add hours to a date without time precision".into(),
            ));
        }
        let new_dt = jiff::Span::new()
            .try_hours(hours)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add minutes (requires at least minute precision).
    ///
    /// # Errors
    /// Returns an error if the result overflows or precision is insufficient.
    pub fn add_minutes(&self, minutes: i64) -> Result<Self, PureRuntimeError> {
        match self.precision {
            DatePrecision::Time(tp) if tp >= TimePrecision::Minute => {}
            _ => {
                return Err(PureRuntimeError::EvaluationError(
                    "Cannot add minutes to a date without minute precision".into(),
                ));
            }
        }
        let new_dt = jiff::Span::new()
            .try_minutes(minutes)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add seconds (requires at least second precision).
    ///
    /// # Errors
    /// Returns an error if the result overflows or precision is insufficient.
    pub fn add_seconds(&self, seconds: i64) -> Result<Self, PureRuntimeError> {
        match self.precision {
            DatePrecision::Time(tp) if tp >= TimePrecision::Second => {}
            _ => {
                return Err(PureRuntimeError::EvaluationError(
                    "Cannot add seconds to a date without second precision".into(),
                ));
            }
        }
        let new_dt = jiff::Span::new()
            .try_seconds(seconds)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }

    /// Add milliseconds (requires sub-second precision).
    ///
    /// # Errors
    /// Returns an error on overflow or insufficient precision.
    pub fn add_milliseconds(&self, ms: i64) -> Result<Self, PureRuntimeError> {
        self.add_subsecond_unit("milliseconds", ms, jiff::Span::try_milliseconds)
    }

    /// Add microseconds (requires sub-second precision).
    ///
    /// # Errors
    /// Returns an error on overflow or insufficient precision.
    pub fn add_microseconds(&self, us: i64) -> Result<Self, PureRuntimeError> {
        self.add_subsecond_unit("microseconds", us, jiff::Span::try_microseconds)
    }

    /// Add nanoseconds (requires sub-second precision).
    ///
    /// # Errors
    /// Returns an error on overflow or insufficient precision.
    pub fn add_nanoseconds(&self, ns: i64) -> Result<Self, PureRuntimeError> {
        self.add_subsecond_unit("nanoseconds", ns, jiff::Span::try_nanoseconds)
    }

    /// Shared helper for sub-second arithmetic. The Pure platform tests
    /// (testAdjustByMilliseconds, etc.) accept any time-precision input
    /// — milliseconds added to a date with second precision still work
    /// in Java Pure (the result simply gains sub-second precision).
    /// We require at least second precision here; sub-second adjustments
    /// to year/month/day-only dates still error per the platform's
    /// "needs time" contract on adjust.
    fn add_subsecond_unit<F>(
        &self,
        unit_name: &'static str,
        n: i64,
        build: F,
    ) -> Result<Self, PureRuntimeError>
    where
        F: FnOnce(jiff::Span, i64) -> Result<jiff::Span, jiff::Error>,
    {
        match self.precision {
            DatePrecision::Time(tp) if tp >= TimePrecision::Second => {}
            _ => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "Cannot add {unit_name} to a date without second precision"
                )));
            }
        }
        let new_dt = build(jiff::Span::new(), n)
            .and_then(|sp| self.inner.checked_add(sp))
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Date overflow: {e}")))?;
        Ok(Self {
            inner: new_dt,
            precision: self.precision,
        })
    }
}

impl PartialOrd for PureDate {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for PureDate {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.inner.cmp(&other.inner)
    }
}

impl fmt::Display for PureDate {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let dt = &self.inner;
        match self.precision {
            DatePrecision::Year => {
                write!(f, "{:04}", dt.year())
            }
            DatePrecision::Month => {
                write!(f, "{:04}-{:02}", dt.year(), dt.month())
            }
            DatePrecision::Day => {
                write!(f, "{:04}-{:02}-{:02}", dt.year(), dt.month(), dt.day())
            }
            DatePrecision::Time(tp) => {
                write!(
                    f,
                    "{:04}-{:02}-{:02}T{:02}",
                    dt.year(),
                    dt.month(),
                    dt.day(),
                    dt.hour()
                )?;
                match tp {
                    TimePrecision::Hour => Ok(()),
                    TimePrecision::Minute => {
                        write!(f, ":{:02}+0000", dt.minute())
                    }
                    TimePrecision::Second => {
                        write!(f, ":{:02}:{:02}+0000", dt.minute(), dt.second())
                    }
                    TimePrecision::Subsecond(digits) => {
                        write!(f, ":{:02}:{:02}.", dt.minute(), dt.second())?;
                        let nanos = dt.subsec_nanosecond();
                        // Format with the original number of digits
                        let s = format!("{nanos:09}");
                        let d = digits as usize;
                        write!(f, "{}+0000", &s[..d])
                    }
                }
            }
        }
    }
}

/// Pure `StrictTime` — time of day without a date component.
///
/// Backed by `jiff::civil::Time` — `Copy`, nanosecond precision.
///
/// ```text
/// Pure                    Rust
/// %10:30:00               StrictTime(Time::new(10, 30, 0, 0))
/// %10:30:00.123456789     StrictTime(Time::new(10, 30, 0, 123456789))
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct StrictTime(pub jiff::civil::Time);

impl StrictTime {
    /// Create a new strict time.
    ///
    /// # Errors
    /// Returns an error if the time components are invalid.
    pub fn new(
        hour: i8,
        minute: i8,
        second: i8,
        nanosecond: i32,
    ) -> Result<Self, PureRuntimeError> {
        let t = jiff::civil::Time::new(hour, minute, second, nanosecond)
            .map_err(|e| PureRuntimeError::EvaluationError(format!("Invalid time: {e}")))?;
        Ok(Self(t))
    }

    /// Get the hour (0-23).
    #[must_use]
    pub fn hour(&self) -> i8 {
        self.0.hour()
    }

    /// Get the minute (0-59).
    #[must_use]
    pub fn minute(&self) -> i8 {
        self.0.minute()
    }

    /// Get the second (0-59).
    #[must_use]
    pub fn second(&self) -> i8 {
        self.0.second()
    }

    /// Get the nanosecond (0-999_999_999).
    #[must_use]
    pub fn nanosecond(&self) -> i32 {
        self.0.subsec_nanosecond()
    }
}

impl fmt::Display for StrictTime {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let t = &self.0;
        write!(f, "{:02}:{:02}:{:02}", t.hour(), t.minute(), t.second())?;
        if t.subsec_nanosecond() != 0 {
            let s = format!("{:09}", t.subsec_nanosecond());
            let trimmed = s.trim_end_matches('0');
            write!(f, ".{trimmed}")?;
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn year_only() {
        let d = PureDate::year(2024).unwrap();
        assert_eq!(d.get_year(), 2024);
        assert_eq!(d.get_month(), None);
        assert_eq!(d.get_day(), None);
        assert!(!d.has_time());
        assert_eq!(d.to_string(), "2024");
        assert!(d.add_hours(1).is_err());
        assert!(d.add_minutes(1).is_err());
        assert!(d.add_seconds(1).is_err());
    }

    #[test]
    fn strict_time_tests() {
        let t = StrictTime::new(10, 30, 45, 123_456_000).unwrap();
        assert_eq!(t.hour(), 10);
        assert_eq!(t.minute(), 30);
        assert_eq!(t.second(), 45);
        assert_eq!(t.nanosecond(), 123_456_000);
        assert_eq!(t.to_string(), "10:30:45.123456");

        let t2 = StrictTime::new(23, 59, 59, 0).unwrap();
        assert_eq!(t2.to_string(), "23:59:59");
    }

    #[test]
    fn time_addition() {
        let d = PureDate::datetime(2024, 3, 15, 10, 30, 0, 0, TimePrecision::Minute).unwrap();
        let d_h = d.add_hours(2).unwrap();
        assert_eq!(d_h.to_string(), "2024-03-15T12:30+0000");

        let d_m = d.add_minutes(15).unwrap();
        assert_eq!(d_m.to_string(), "2024-03-15T10:45+0000");

        assert!(d.add_seconds(30).is_err());

        let d2 = PureDate::datetime(2024, 3, 15, 10, 30, 15, 0, TimePrecision::Second).unwrap();
        let d2_s = d2.add_seconds(30).unwrap();
        assert_eq!(d2_s.to_string(), "2024-03-15T10:30:45+0000");
    }

    #[test]
    fn year_month() {
        let d = PureDate::year_month(2024, 3).unwrap();
        assert_eq!(d.get_year(), 2024);
        assert_eq!(d.get_month(), Some(3));
        assert_eq!(d.get_day(), None);
        assert_eq!(d.to_string(), "2024-03");
    }

    #[test]
    fn strict_date() {
        let d = PureDate::strict_date(2024, 3, 15).unwrap();
        assert_eq!(d.get_year(), 2024);
        assert_eq!(d.get_month(), Some(3));
        assert_eq!(d.get_day(), Some(15));
        assert!(!d.has_time());
        assert_eq!(d.to_string(), "2024-03-15");
    }

    #[test]
    fn datetime_with_seconds() {
        let d = PureDate::datetime(2024, 3, 15, 10, 30, 0, 0, TimePrecision::Second).unwrap();
        assert_eq!(d.get_hour(), Some(10));
        assert_eq!(d.get_minute(), Some(30));
        assert_eq!(d.get_second(), Some(0));
        assert_eq!(d.to_string(), "2024-03-15T10:30:00+0000");
    }

    #[test]
    fn datetime_with_subseconds() {
        let d = PureDate::datetime(
            2024,
            3,
            15,
            10,
            30,
            0,
            123_000_000,
            TimePrecision::Subsecond(3),
        )
        .unwrap();
        assert_eq!(d.get_subsec_nanosecond(), Some(123_000_000));
        assert_eq!(d.to_string(), "2024-03-15T10:30:00.123+0000");
    }

    #[test]
    fn add_days_to_strict_date() {
        let d = PureDate::strict_date(2024, 3, 15).unwrap();
        let d2 = d.add_days(5).unwrap();
        assert_eq!(d2.to_string(), "2024-03-20");
    }

    #[test]
    fn add_days_across_month_boundary() {
        let d = PureDate::strict_date(2024, 1, 30).unwrap();
        let d2 = d.add_days(5).unwrap();
        assert_eq!(d2.to_string(), "2024-02-04"); // 2024 is leap year
    }

    #[test]
    fn add_months() {
        let d = PureDate::strict_date(2024, 1, 31).unwrap();
        let d2 = d.add_months(1).unwrap();
        // jiff handles end-of-month clamping
        assert_eq!(d2.get_month(), Some(2));
    }

    #[test]
    fn add_years() {
        let d = PureDate::year(2024).unwrap();
        let d2 = d.add_years(10).unwrap();
        assert_eq!(d2.get_year(), 2034);
        assert_eq!(d2.precision(), DatePrecision::Year);
    }

    #[test]
    fn add_days_to_year_errors() {
        let d = PureDate::year(2024).unwrap();
        assert!(d.add_days(5).is_err());
    }

    #[test]
    fn ordering() {
        let d1 = PureDate::strict_date(2024, 1, 1).unwrap();
        let d2 = PureDate::strict_date(2024, 1, 2).unwrap();
        assert!(d1 < d2);
    }

    #[test]
    fn strict_time_basic() {
        let t = StrictTime::new(10, 30, 45, 0).unwrap();
        assert_eq!(t.hour(), 10);
        assert_eq!(t.minute(), 30);
        assert_eq!(t.second(), 45);
        assert_eq!(t.to_string(), "10:30:45");
    }

    #[test]
    fn strict_time_with_nanos() {
        let t = StrictTime::new(10, 30, 45, 123_456_789).unwrap();
        assert_eq!(t.to_string(), "10:30:45.123456789");
    }

    #[test]
    fn pure_date_is_copy() {
        let d = PureDate::strict_date(2024, 3, 15).unwrap();
        let d2 = d; // Copy
        assert_eq!(d, d2);
    }

    #[test]
    fn size_check() {
        assert!(
            std::mem::size_of::<PureDate>() <= 16,
            "PureDate should be <= 16 bytes, got {}",
            std::mem::size_of::<PureDate>()
        );
    }
}
