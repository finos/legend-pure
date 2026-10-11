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

//! String natives: `toString` (the generic renderer, dispatching to a
//! class's own `toString()` qualified property when it declares one),
//! `format`, `joinStrings`, the predicates (`contains`, `startsWith`,
//! `endsWith`), the slicers (`substring`, `indexOf`, `length`, `split`,
//! `replace`, `reverseString`), case and trimming, and `parseDecimal`.

use legend_pure_metamodel::ids::ElementId;
use legend_pure_metamodel::types::ValueSpec;
use smol_str::SmolStr;

use crate::error::{PureException, PureRuntimeError};
use crate::native::{
    EvalContextTrait, Evaluated, NativeFunction, NativeRegistry, expect_args, force_all,
};
use crate::value::Value;

/// Pure `toString(Any[1]): String[1]` — convert any value to its
/// human-readable Pure string form. Distinct from `Value::Display`
/// (which is a debug-leaning shape with quoted strings, `%`-prefixed
/// dates, and `<Object@id>` placeholders): toString matches Java
/// Pure's `toString` shape.
///
/// Heap-object dispatch is delegated to whatever `toString()` qualified
/// property the receiver's class (or any of its generalizations) defines
/// — `Pair`, `List`, and any user class with a `toString()` qualifier
/// flow through the same path. When no such QP exists, the runtime
/// falls back to `Anonymous_<obj_id>`.
#[derive(Debug)]
pub struct ToString;

impl NativeFunction for ToString {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("toString", &values, 1)?;
        let s = pure_to_string(&values[0], ctx)?;
        Ok(Evaluated::new(Value::String(SmolStr::new(s))))
    }
}

/// Locate the `toString()` qualified property on the receiver's class
/// (walking generalizations). Returns the
/// [`FoundQp`](crate::eval::FoundQp).
fn lookup_to_string_qp(
    ctx: &dyn EvalContextTrait,
    obj_id: &crate::heap::ObjectHandle,
) -> Option<crate::eval::FoundQp> {
    let classifier = ctx.heap().classifier(obj_id).ok()?;
    let class_id = ctx.model().resolve_fqn_str(classifier.as_str())?;
    crate::eval::find_qp_with_generalization(ctx.model(), class_id, "toString", 0)
}

/// `true` iff the QP's declared return is exactly `String[1]`. Pure's
/// `toString()` contract requires this; any other declared shape is
/// treated as a malformed QP and skipped.
fn qp_returns_string_one(found: &crate::eval::FoundQp) -> bool {
    use legend_pure_metamodel::bootstrap::STRING_ID;
    use legend_pure_metamodel::types::{Multiplicity, TypeExpr};
    matches!(
        (&found.return_type, &found.return_multiplicity),
        (TypeExpr::Named { element, .. }, Multiplicity::PureOne)
            if *element == STRING_ID
    )
}

/// Render a value to its Pure-toString form.
///
/// For primitives, this returns the literal source form (mirrors Java
/// Pure's `value.getName()` for primitive types). For heap objects this
/// invokes any `toString()` qualified property visible on the receiver's
/// class (with generalization walk) — *but only if the declared return
/// is `String[1]`*; malformed QPs are skipped via
/// `qp_returns_string_one`, and the value formats as
/// `Anonymous_<id>`.
///
/// # Errors
/// Propagates any [`PureException`] raised while evaluating a class's
/// `toString` body.
pub(crate) fn pure_to_string(
    value: &Value,
    ctx: &mut dyn EvalContextTrait,
) -> Result<String, PureException> {
    enum Op<'a> {
        Visit(&'a Value),
        AssembleCollection(usize),
        AssembleUnitInstance(ElementId),
    }

    let mut ops: Vec<Op<'_>> = vec![Op::Visit(value)];
    let mut built: Vec<String> = Vec::new();

    while let Some(op) = ops.pop() {
        match op {
            Op::Visit(v) => match v {
                Value::Collection(items) => {
                    ops.push(Op::AssembleCollection(items.len()));
                    for item in items.iter().rev() {
                        ops.push(Op::Visit(item));
                    }
                }
                Value::UnitInstance { unit_id, inner } => {
                    ops.push(Op::AssembleUnitInstance(*unit_id));
                    ops.push(Op::Visit(inner));
                }
                Value::Object(obj_id) => {
                    let invoked = lookup_to_string_qp(ctx, obj_id)
                        .filter(qp_returns_string_one)
                        .map(|found| ctx.invoke_qualified_property_found(v, &found, &[]))
                        .transpose()?;
                    match invoked.as_ref() {
                        Some(Value::String(s)) => built.push(s.to_string()),
                        _ => built.push(format!("Anonymous_{:p}", std::rc::Rc::as_ptr(obj_id))),
                    }
                }
                leaf => built.push(pure_to_string_leaf(leaf, ctx)),
            },
            Op::AssembleCollection(len) => {
                let start = built.len() - len;
                let parts: Vec<String> = built.drain(start..).collect();
                built.push(format!("[{}]", parts.join(", ")));
            }
            Op::AssembleUnitInstance(unit_id) => {
                let inner_s = built.pop().ok_or_else(|| {
                    PureRuntimeError::EvaluationError(
                        "pure_to_string: internal driver bug — AssembleUnitInstance reached with empty built stack".into(),
                    )
                })?;
                let unit_name = ctx.model().element_name(unit_id).to_string();
                built.push(format!("{inner_s} {unit_name}"));
            }
        }
    }

    built.pop().ok_or_else(|| {
        PureRuntimeError::EvaluationError(
            "pure_to_string: internal driver bug — built stack empty after traversal".into(),
        )
        .into()
    })
}

/// `toString` rendering for the non-recursive (`Collection` / `UnitInstance` /
/// `Object` excluded) `Value` variants.
fn pure_to_string_leaf(value: &Value, ctx: &dyn EvalContextTrait) -> String {
    use crate::value::FunctionValue;
    match value {
        Value::String(s) => s.to_string(),
        Value::Boolean(b) => b.to_string(),
        Value::Integer(i) => i.to_string(),
        Value::Float(_) => crate::native::math::java_number_string(value),
        Value::Decimal(d) => d.to_string(),
        Value::Date(d) => d.to_string(),
        Value::Latest => "%latest".to_string(),
        Value::StrictTime(t) => t.to_string(),
        Value::Unit => String::new(),
        Value::Element(id) => crate::model_utils::element_simple_name(ctx.model(), *id).to_string(),
        Value::EnumValue { member, .. } => member.to_string(),
        Value::Function(fv) => match fv.as_ref() {
            FunctionValue::Lambda(_) => "<Lambda>".to_string(),
            FunctionValue::Compiled(id) => {
                crate::model_utils::element_simple_name(ctx.model(), *id).to_string()
            }
            FunctionValue::Path(p) => format!(
                "<Path:{}{}>",
                p.steps.len(),
                p.name.as_ref().map(|n| format!("!{n}")).unwrap_or_default()
            ),
        },
        Value::Map(m) => format!("<Map size={}>", m.borrow().entries.len()),
        Value::Collection(_) | Value::UnitInstance { .. } | Value::Object(_) => unreachable!(
            "pure_to_string_leaf called on recursive Value variant; the iterative driver should have queued these"
        ),
    }
}

/// Produce a repr string for `%r` / `toRepresentation`: strings are
/// quoted with backslash + single-quote escaped (Pure source form);
/// dates carry the leading `%`; other values fall back to Display.
fn repr_value(v: &Value) -> String {
    match v {
        Value::String(s) => {
            let escaped = s.replace('\\', "\\\\").replace('\'', "\\'");
            format!("'{escaped}'")
        }
        Value::Date(d) => format!("%{d}"),
        Value::StrictTime(t) => format!("%{t}"),
        other => other.to_string(),
    }
}

/// Pure `format(String[1], Any[*]): String[1]`
///
/// Replaces format specifiers in the template with successive values from
/// `args[1]`. Specifier syntax (mirrors Java Pure's `format`):
/// - `%s` → toString shape (unquoted strings, dates without `%`, lists
///   `[a, b]`, pairs `<a, b>`)
/// - `%r` → repr (quoted strings with escapes, dates with leading `%`)
/// - `%d` → integer; supports `%05d` zero-padded width
/// - `%f` → float; supports `%.4f` precision (truncating fractional digits
///   to N, padding with zeros if value has fewer)
/// - `%t` → date toString (same shape as `%s` for dates)
/// - `%t{pattern}` → date with explicit format pattern
///   (e.g. `%t{yyyy-MM-dd HH:mm:ss}`)
///
/// `args[1]` is the `Any[*]` collection of substitution values; if it is
/// a `Value::Collection`, its elements are iterated; otherwise it is
/// treated as a single-element list.
#[derive(Debug)]
pub struct Format;

impl NativeFunction for Format {
    #[allow(clippy::many_single_char_names, clippy::too_many_lines)]
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("format", &values, 2)?;
        let template = values[0].as_string()?.to_string();

        let subs: Vec<Value> = match &values[1] {
            Value::Collection(v) => v.iter().cloned().collect(),
            single => vec![single.clone()],
        };

        let chars: Vec<char> = template.chars().collect();
        let mut result = String::with_capacity(template.len());
        let mut i = 0usize;
        let mut sub_idx = 0usize;

        while i < chars.len() {
            let c = chars[i];
            if c != '%' {
                result.push(c);
                i += 1;
                continue;
            }
            let mut j = i + 1;
            let mut zero_pad = false;
            if j < chars.len()
                && chars[j] == '0'
                && j + 1 < chars.len()
                && chars[j + 1].is_ascii_digit()
            {
                zero_pad = true;
                j += 1;
            }
            let width_start = j;
            while j < chars.len() && chars[j].is_ascii_digit() {
                j += 1;
            }
            let width: Option<usize> = if j > width_start {
                Some(
                    chars[width_start..j]
                        .iter()
                        .collect::<String>()
                        .parse()
                        .unwrap_or(0),
                )
            } else {
                None
            };
            let precision: Option<usize> = if j < chars.len() && chars[j] == '.' {
                let p_start = j + 1;
                let mut p_end = p_start;
                while p_end < chars.len() && chars[p_end].is_ascii_digit() {
                    p_end += 1;
                }
                if p_end > p_start {
                    j = p_end;
                    Some(
                        chars[p_start..p_end]
                            .iter()
                            .collect::<String>()
                            .parse()
                            .unwrap_or(0),
                    )
                } else {
                    j = p_start;
                    None
                }
            } else {
                None
            };
            if j >= chars.len() {
                result.push('%');
                i += 1;
                continue;
            }
            let spec = chars[j];
            let date_pattern: Option<String> =
                if spec == 't' && j + 1 < chars.len() && chars[j + 1] == '{' {
                    let pat_start = j + 2;
                    if let Some(end) = chars[pat_start..].iter().position(|&c| c == '}') {
                        let pat: String = chars[pat_start..pat_start + end].iter().collect();
                        j = pat_start + end + 1;
                        Some(pat)
                    } else {
                        None
                    }
                } else {
                    j += 1;
                    None
                };
            if !matches!(spec, 's' | 'r' | 'd' | 'f' | 't') {
                result.push('%');
                i += 1;
                continue;
            }
            let v = subs.get(sub_idx).cloned().ok_or_else(|| {
                PureRuntimeError::EvaluationError(format!(
                    "Too few arguments passed to format function. Format expression \"{template}\", number of arguments [{}]",
                    subs.len()
                ))
            })?;
            sub_idx += 1;
            match spec {
                'r' => result.push_str(&repr_value(&v)),
                's' => result.push_str(&pure_to_string(&v, ctx)?),
                'd' => {
                    let Value::Integer(n) = v else {
                        let got = pure_to_string(&v, ctx)?;
                        return Err(PureRuntimeError::EvaluationError(format!(
                            "Expected Integer, got: {got}"
                        ))
                        .into());
                    };
                    let body = if let Some(w) = width {
                        if zero_pad {
                            if n < 0 {
                                format!("-{:0>1$}", -n, w)
                            } else {
                                format!("{n:0>w$}")
                            }
                        } else {
                            format!("{n:>w$}")
                        }
                    } else {
                        n.to_string()
                    };
                    result.push_str(&body);
                }
                'f' => {
                    let Value::Float(f) = v else {
                        let got = pure_to_string(&v, ctx)?;
                        return Err(PureRuntimeError::EvaluationError(format!(
                            "Expected Float, got: {got}"
                        ))
                        .into());
                    };
                    let body = if let Some(p) = precision {
                        format!("{f:.p$}")
                    } else {
                        f.to_string()
                    };
                    result.push_str(&body);
                }
                't' => {
                    let Value::Date(d) = &v else {
                        let got = pure_to_string(&v, ctx)?;
                        return Err(PureRuntimeError::EvaluationError(format!(
                            "Expected Date, got: {got}"
                        ))
                        .into());
                    };
                    if let Some(pat) = &date_pattern {
                        result.push_str(&format_date_pattern(d, pat));
                    } else {
                        result.push_str(&pure_to_string(&v, ctx)?);
                    }
                }
                _ => unreachable!(),
            }
            i = j;
        }

        if sub_idx < subs.len() {
            return Err(PureRuntimeError::EvaluationError(format!(
                "Unused format args. [{}] arguments provided to expression \"{template}\"",
                subs.len()
            ))
            .into());
        }

        Ok(Evaluated::new(Value::String(SmolStr::new(result))))
    }
}

/// Format a [`PureDate`](crate::date::PureDate) with a
/// Java-SimpleDateFormat-like pattern.
///
/// Currently supports the subset the platform tests exercise:
/// - `yyyy` (4-digit year), `MM` (2-digit month), `dd` (2-digit day)
/// - `HH` (24-hour), `hh` (12-hour), `h` (1- or 2-digit 12-hour)
/// - `mm` (minute), `ss` (second), `SSS` (millis)
/// - `a` (AM/PM), `Z` (`+0000`-style offset), `X` (`Z`-style offset)
/// - `"literal"` segments (Java-style quoted text)
/// - `[Region/City]` prefix for timezone-shifted output
///
/// Unsupported patterns pass through verbatim.
#[allow(clippy::many_single_char_names, clippy::too_many_lines)]
fn format_date_pattern(d: &crate::date::PureDate, pat: &str) -> String {
    use crate::date::PureDate;
    use std::fmt::Write as _;
    let (tz_offset_minutes, body) = if let Some(rest) = pat.strip_prefix('[') {
        if let Some(end) = rest.find(']') {
            let zone = &rest[..end];
            let offset = match zone {
                "EST" => Some(-5 * 60),
                "EDT" => Some(-4 * 60),
                "CET" => Some(60),
                "GMT" | "UTC" => Some(0),
                _ => None,
            };
            (offset, &rest[end + 1..])
        } else {
            (None, pat)
        }
    } else {
        (None, pat)
    };
    let shifted: PureDate = if let Some(off) = tz_offset_minutes {
        d.add_minutes(i64::from(off)).unwrap_or(*d)
    } else {
        *d
    };
    let inner = shifted.inner_datetime();
    let mut out = String::with_capacity(body.len());
    let chars: Vec<char> = body.chars().collect();
    let mut i = 0;
    while i < chars.len() {
        let c = chars[i];
        if c == '"' {
            i += 1;
            while i < chars.len() && chars[i] != '"' {
                out.push(chars[i]);
                i += 1;
            }
            if i < chars.len() {
                i += 1;
            }
            continue;
        }
        let mut j = i + 1;
        while j < chars.len() && chars[j] == c {
            j += 1;
        }
        let count = j - i;
        let token: String = std::iter::repeat_n(c, count).collect();
        let _ = token;
        match (c, count) {
            ('y', 4) => {
                let _ = write!(out, "{:04}", inner.year());
            }
            ('M', 2) => {
                let _ = write!(out, "{:02}", inner.month());
            }
            ('d', 2) => {
                let _ = write!(out, "{:02}", inner.day());
            }
            ('H', 2) => {
                let _ = write!(out, "{:02}", inner.hour());
            }
            ('h', n) => {
                let h12 = match inner.hour() {
                    0 => 12,
                    h if h > 12 => h - 12,
                    h => h,
                };
                if n == 2 {
                    let _ = write!(out, "{h12:02}");
                } else {
                    let _ = write!(out, "{h12}");
                }
            }
            ('m', 2) => {
                let _ = write!(out, "{:02}", inner.minute());
            }
            ('s', 2) => {
                let _ = write!(out, "{:02}", inner.second());
            }
            ('S', n) => {
                let nanos = inner.subsec_nanosecond();
                let s = format!("{nanos:09}");
                out.push_str(&s[..n.min(9)]);
            }
            ('a', 1) => out.push_str(if inner.hour() < 12 { "AM" } else { "PM" }),
            ('Z', 1) => {
                let m: i32 = tz_offset_minutes.unwrap_or(0);
                let sign = if m >= 0 { '+' } else { '-' };
                let abs = m.abs();
                let _ = write!(out, "{sign}{:02}{:02}", abs / 60, abs % 60);
            }
            ('X', 1) => {
                let m: i32 = tz_offset_minutes.unwrap_or(0);
                if m == 0 {
                    out.push('Z');
                } else {
                    let sign = if m > 0 { '+' } else { '-' };
                    let abs = m.abs();
                    let _ = write!(out, "{sign}{:02}", abs / 60);
                }
            }
            _ => {
                for _ in 0..count {
                    out.push(c);
                }
            }
        }
        i = j;
    }
    out
}

/// Pure `joinStrings` — concatenate a collection of strings with a separator.
///
/// Two arities are supported:
/// - `joinStrings(String[*], String[1]): String[1]` — elements joined by
///   `separator`.
/// - `joinStrings(String[*], String[1], String[1], String[1]): String[1]` —
///   `prefix + elements.join(separator) + suffix`.
#[derive(Debug)]
pub struct JoinStrings;

impl NativeFunction for JoinStrings {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        let (separator, prefix, suffix) = match values.len() {
            2 => (values[1].as_string()?.clone(), None, None),
            4 => (
                values[2].as_string()?.clone(),
                Some(values[1].as_string()?.clone()),
                Some(values[3].as_string()?.clone()),
            ),
            n => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "joinStrings: expected 2 or 4 argument(s), got {n}"
                ))
                .into());
            }
        };

        let coll = values[0].to_collection();
        let mut parts: Vec<String> = Vec::with_capacity(coll.len());
        for v in &coll {
            parts.push(v.as_string()?.as_str().to_owned());
        }
        let joined = parts.join(separator.as_str());

        let result = match (prefix, suffix) {
            (Some(p), Some(s)) => format!("{p}{joined}{s}"),
            _ => joined,
        };
        Ok(Evaluated::new(Value::String(SmolStr::new(result))))
    }
}

/// Java indexes strings by UTF-16 unit; these map between that and
/// byte offsets.
fn utf16_len(s: &str) -> usize {
    s.encode_utf16().count()
}

/// Byte offset of the `index`-th UTF-16 unit, clamped to the string's
/// end. An index inside a surrogate pair lands on the next character.
fn byte_at_utf16(s: &str, index: usize) -> usize {
    let mut units = 0usize;
    for (byte, c) in s.char_indices() {
        if units >= index {
            return byte;
        }
        units += c.len_utf16();
    }
    s.len()
}

fn utf16_at_byte(s: &str, byte: usize) -> usize {
    utf16_len(&s[..byte])
}

/// Pure `indexOf(String[1], String[1]): Integer[1]`
/// Pure `indexOf(String[1], String[1], Integer[1]): Integer[1]` — search
/// from a non-zero start index.
///
/// Returns -1 if not found (matching Java behavior). The 3-arg form's
/// returned index is absolute (not relative to the start), matching
/// `String.indexOf(String, int)` in Java.
#[derive(Debug)]
pub struct IndexOf;

impl NativeFunction for IndexOf {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        let s = values[0].as_string()?;
        let sub = values[1].as_string()?;
        let from = match values.len() {
            2 => 0usize,
            3 => {
                let raw = values[2].as_integer()?.max(0);
                byte_at_utf16(s, usize::try_from(raw).unwrap_or(usize::MAX))
            }
            n => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "indexOf: expected 2 or 3 argument(s), got {n}"
                ))
                .into());
            }
        };
        let idx = s[from..].find(sub.as_str()).map_or(-1, |i| {
            i64::try_from(utf16_at_byte(s, i + from)).unwrap_or(i64::MAX)
        });
        Ok(Evaluated::new(Value::Integer(idx)))
    }
}

/// Pure `startsWith(String[1], String[1]): Boolean[1]`
#[derive(Debug)]
pub struct StartsWith;

impl NativeFunction for StartsWith {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("startsWith", &values, 2)?;
        let s = values[0].as_string()?;
        let prefix = values[1].as_string()?;
        Ok(Evaluated::new(Value::Boolean(
            s.starts_with(prefix.as_str()),
        )))
    }
}

/// Pure `parseDecimal(string:String[1]):Decimal[1]`
/// Pure `parseDecimal(string:String[1], precision:Integer[1], scale:Integer[1]):Decimal[1]`
///
/// Parse a string into a `Decimal`. Strips an optional trailing `d`
/// suffix (Pure's decimal literal marker) and tolerates leading `+`
/// plus zero-padding (`+0000003.14`).
///
/// The 3-arg overload rounds to `scale` digits after the decimal
/// point using banker's rounding.
/// `precision` is accepted for signature parity but not enforced.
#[derive(Debug)]
pub struct ParseDecimal;

impl NativeFunction for ParseDecimal {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        use rust_decimal::Decimal;
        use std::str::FromStr;

        let values = force_all(args, ctx)?;
        match values.len() {
            1 => {
                let s = values[0].as_string()?;
                let trimmed = s.trim_end_matches('d');
                let d = Decimal::from_str(trimmed).map_err(|e| {
                    PureRuntimeError::EvaluationError(format!(
                        "parseDecimal: cannot parse '{s}' as Decimal: {e}"
                    ))
                })?;
                Ok(Evaluated::new(Value::Decimal(d)))
            }
            3 => {
                let s = values[0].as_string()?;
                let scale = i64_arg(&values[2], "parseDecimal scale")?;
                if !(0..=28).contains(&scale) {
                    return Err(PureRuntimeError::EvaluationError(format!(
                        "parseDecimal: scale must be in 0..=28, got {scale}"
                    ))
                    .into());
                }
                let trimmed = s.trim_end_matches('d');
                let d = Decimal::from_str(trimmed).map_err(|e| {
                    PureRuntimeError::EvaluationError(format!(
                        "parseDecimal: cannot parse '{s}' as Decimal: {e}"
                    ))
                })?;
                #[allow(clippy::cast_possible_truncation, clippy::cast_sign_loss)]
                let rounded = d.round_dp(scale as u32);
                Ok(Evaluated::new(Value::Decimal(rounded)))
            }
            n => Err(PureRuntimeError::EvaluationError(format!(
                "parseDecimal: expected 1 or 3 argument(s), got {n}"
            ))
            .into()),
        }
    }
}

/// Pure `contains(String[1], String[1]): Boolean[1]`
#[derive(Debug)]
pub struct Contains;

impl NativeFunction for Contains {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("contains", &values, 2)?;
        let s = values[0].as_string()?;
        let sub = values[1].as_string()?;
        Ok(Evaluated::new(Value::Boolean(s.contains(sub.as_str()))))
    }
}

/// Pure `endsWith(String[1], String[1]): Boolean[1]`
#[derive(Debug)]
pub struct EndsWith;

impl NativeFunction for EndsWith {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("endsWith", &values, 2)?;
        let s = values[0].as_string()?;
        let suffix = values[1].as_string()?;
        Ok(Evaluated::new(Value::Boolean(s.ends_with(suffix.as_str()))))
    }
}

/// Pure `length(String[1]): Integer[1]`
#[derive(Debug)]
pub struct Length;

impl NativeFunction for Length {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("length", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::Integer(
            i64::try_from(utf16_len(s)).unwrap_or(i64::MAX),
        )))
    }
}

/// Pure `split(str:String[1], token:String[1]):String[*]`
///
/// Splits `str` at every occurrence of `token`. When `token` is absent,
/// the result is a single-element collection holding the original
/// string. Mirror of Java's `String.split(literal)` modulo regex —
/// `token` is treated as a literal substring, not a regex.
#[derive(Debug)]
pub struct Split;

impl NativeFunction for Split {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("split", &values, 2)?;
        let s = values[0].as_string()?;
        let token = values[1].as_string()?;
        let parts: Vec<Value> = s
            .split(token.as_str())
            .map(|p| Value::String(SmolStr::new(p)))
            .collect();
        Ok(Evaluated::new(Value::from_vec(parts)))
    }
}

/// Pure `substring(String[1], Integer[1], Integer[1]): String[1]`
///
/// Pure uses 0-based start index, and the end index is exclusive.
#[derive(Debug)]
pub struct Substring;

impl NativeFunction for Substring {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        let s = values[0].as_string()?;
        let (start_i, end_i) = match values.len() {
            2 => {
                let start = values[1].as_integer()?.max(0);
                let end = i64::try_from(utf16_len(s)).unwrap_or(i64::MAX);
                (start, end)
            }
            3 => (
                values[1].as_integer()?.max(0),
                values[2].as_integer()?.max(0),
            ),
            n => {
                return Err(PureRuntimeError::EvaluationError(format!(
                    "substring: expected 2 or 3 argument(s), got {n}"
                ))
                .into());
            }
        };
        let start = byte_at_utf16(s, usize::try_from(start_i).unwrap_or(usize::MAX));
        let end = byte_at_utf16(s, usize::try_from(end_i).unwrap_or(usize::MAX));
        let start = start.min(end);
        Ok(Evaluated::new(Value::String(SmolStr::new(&s[start..end]))))
    }
}

/// Pure `replace(String[1], String[1], String[1]): String[1]` — replace all
/// occurrences of `target` in `source` with `replacement`.
#[derive(Debug)]
pub struct Replace;

impl NativeFunction for Replace {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("replace", &values, 3)?;
        let source = values[0].as_string()?;
        let target = values[1].as_string()?;
        let replacement = values[2].as_string()?;
        let out = source
            .as_str()
            .replace(target.as_str(), replacement.as_str());
        Ok(Evaluated::new(Value::String(SmolStr::new(out))))
    }
}

/// Pure `reverseString(String[1]): String[1]` — reverse the characters of
/// a string. Reversal is per Unicode scalar (`char`), not per byte, so
/// multi-byte characters are preserved intact.
#[derive(Debug)]
pub struct ReverseString;

impl NativeFunction for ReverseString {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("reverseString", &values, 1)?;
        let s = values[0].as_string()?;
        let reversed: String = s.chars().rev().collect();
        Ok(Evaluated::new(Value::String(SmolStr::new(reversed))))
    }
}

/// Pure `toLower(String[1]): String[1]`
#[derive(Debug)]
pub struct ToLower;

impl NativeFunction for ToLower {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("toLower", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::String(SmolStr::new(
            s.to_lowercase(),
        ))))
    }
}

/// Pure `toUpper(String[1]): String[1]`
#[derive(Debug)]
pub struct ToUpper;

impl NativeFunction for ToUpper {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("toUpper", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::String(SmolStr::new(
            s.to_uppercase(),
        ))))
    }
}

/// Pure `ltrim(String[1]): String[1]` — remove leading whitespace.
#[derive(Debug)]
pub struct Ltrim;

impl NativeFunction for Ltrim {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("ltrim", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::String(SmolStr::new(s.trim_start()))))
    }
}

/// Pure `rtrim(String[1]): String[1]` — remove trailing whitespace.
#[derive(Debug)]
pub struct Rtrim;

impl NativeFunction for Rtrim {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("rtrim", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::String(SmolStr::new(s.trim_end()))))
    }
}

/// Pure `trim(String[1]): String[1]`
#[derive(Debug)]
pub struct Trim;

impl NativeFunction for Trim {
    fn execute(
        &self,
        args: &[ValueSpec],
        ctx: &mut dyn EvalContextTrait,
    ) -> Result<Evaluated, PureException> {
        let values = force_all(args, ctx)?;
        expect_args("trim", &values, 1)?;
        let s = values[0].as_string()?;
        Ok(Evaluated::new(Value::String(SmolStr::new(s.trim()))))
    }
}

fn i64_arg(v: &Value, ctx: &str) -> Result<i64, PureException> {
    match v {
        Value::Integer(n) => Ok(*n),
        other => Err(PureRuntimeError::EvaluationError(format!(
            "{ctx}: expected Integer, got {}",
            other.type_name()
        ))
        .into()),
    }
}

/// Register the fished string natives.
pub fn register(registry: &mut NativeRegistry) {
    registry.register("toString_Any_1__String_1_", ToString);
    registry.register("format_String_1__Any_MANY__String_1_", Format);
    registry.register(
        "joinStrings_String_MANY__String_1__String_1__String_1__String_1_",
        JoinStrings,
    );
    registry.register("indexOf_String_1__String_1__Integer_1_", IndexOf);
    registry.register("indexOf_String_1__String_1__Integer_1__Integer_1_", IndexOf);
    registry.register("startsWith_String_1__String_1__Boolean_1_", StartsWith);
    registry.register("parseDecimal_String_1__Decimal_1_", ParseDecimal);
    registry.register(
        "parseDecimal_String_1__Integer_1__Integer_1__Decimal_1_",
        ParseDecimal,
    );
    registry.register("contains_String_1__String_1__Boolean_1_", Contains);
    registry.register("endsWith_String_1__String_1__Boolean_1_", EndsWith);
    registry.register("length_String_1__Integer_1_", Length);
    registry.register("split_String_1__String_1__String_MANY_", Split);
    registry.register(
        "substring_String_1__Integer_1__Integer_1__String_1_",
        Substring,
    );
    registry.register("substring_String_1__Integer_1__String_1_", Substring);
    registry.register("replace_String_1__String_1__String_1__String_1_", Replace);
    registry.register("reverseString_String_1__String_1_", ReverseString);
    registry.register("toLower_String_1__String_1_", ToLower);
    registry.register("toUpper_String_1__String_1_", ToUpper);
    registry.register("ltrim_String_1__String_1_", Ltrim);
    registry.register("rtrim_String_1__String_1_", Rtrim);
    registry.register("trim_String_1__String_1_", Trim);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::native::{MockCtx, lit_collection, lit_float, lit_int, lit_str};

    #[test]
    fn to_string_integer() {
        assert_eq!(
            ToString
                .execute(&[lit_int(42)], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("42".into())
        );
    }

    #[test]
    fn to_string_wrong_arg_count_errors() {
        assert!(ToString.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn format_s_specifier() {
        let coll = lit_collection(vec![lit_str("world")]);
        let r = Format
            .execute(&[lit_str("hello %s"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("hello world".into()));
    }

    #[test]
    fn format_r_specifier_quotes_string() {
        let coll = lit_collection(vec![lit_str("foo")]);
        let r = Format
            .execute(&[lit_str("%r"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("'foo'".into()));
    }

    #[test]
    fn strings_are_indexed_by_utf16_unit_like_java() {
        let n = Length.execute(&[lit_str("é😀")], &mut MockCtx).unwrap();
        assert_eq!(n.into_value(), Value::Integer(3));
        let i = IndexOf
            .execute(&[lit_str("héllo"), lit_str("l")], &mut MockCtx)
            .unwrap();
        assert_eq!(i.into_value(), Value::Integer(2));
        let i = IndexOf
            .execute(&[lit_str("héllo"), lit_str("l"), lit_int(3)], &mut MockCtx)
            .unwrap();
        assert_eq!(i.into_value(), Value::Integer(3));
        let sub = Substring
            .execute(&[lit_str("héllo"), lit_int(0), lit_int(2)], &mut MockCtx)
            .unwrap();
        assert_eq!(sub.into_value(), Value::String("hé".into()));
        let sub = Substring
            .execute(&[lit_str("héllo"), lit_int(2)], &mut MockCtx)
            .unwrap();
        assert_eq!(sub.into_value(), Value::String("llo".into()));
    }

    #[test]
    fn format_keeps_non_ascii_template_text() {
        let coll = lit_collection(vec![lit_str("x")]);
        let r = Format
            .execute(&[lit_str("café %s ∞"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("café x ∞".into()));
    }

    #[test]
    fn format_two_args() {
        let coll = lit_collection(vec![lit_int(1), lit_int(2)]);
        let r = Format
            .execute(&[lit_str("%s + %s"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("1 + 2".into()));
    }

    #[test]
    fn format_d_on_string_errors() {
        let coll = lit_collection(vec![lit_str("not an int")]);
        let err = Format
            .execute(&[lit_str("%d"), coll], &mut MockCtx)
            .expect_err("%d on String must error");
        assert!(
            err.to_string()
                .contains("Expected Integer, got: not an int"),
            "Java-parity message text not found: {err}"
        );
    }

    #[test]
    fn format_d_on_float_errors() {
        let coll = lit_collection(vec![lit_float(3.25)]);
        let err = Format
            .execute(&[lit_str("%d"), coll], &mut MockCtx)
            .expect_err("%d on Float must error (Java rejects non-Integer)");
        assert!(
            err.to_string().contains("Expected Integer, got:"),
            "got: {err}"
        );
    }

    #[test]
    fn format_f_on_string_errors() {
        let coll = lit_collection(vec![lit_str("not a float")]);
        let err = Format
            .execute(&[lit_str("%f"), coll], &mut MockCtx)
            .expect_err("%f on String must error");
        assert!(
            err.to_string().contains("Expected Float, got: not a float"),
            "got: {err}"
        );
    }

    #[test]
    fn format_f_on_integer_errors() {
        let coll = lit_collection(vec![lit_int(42)]);
        let err = Format
            .execute(&[lit_str("%f"), coll], &mut MockCtx)
            .expect_err("%f on Integer must error (Java rejects Integer for %f)");
        assert!(
            err.to_string().contains("Expected Float, got:"),
            "got: {err}"
        );
    }

    #[test]
    fn format_t_on_string_errors() {
        let coll = lit_collection(vec![lit_str("not a date")]);
        let err = Format
            .execute(&[lit_str("%t"), coll], &mut MockCtx)
            .expect_err("%t on String must error");
        assert!(
            err.to_string().contains("Expected Date, got: not a date"),
            "got: {err}"
        );
    }

    #[test]
    fn format_d_on_integer_compiles() {
        let coll = lit_collection(vec![lit_int(42)]);
        let r = Format
            .execute(&[lit_str("got %d"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("got 42".into()));
    }

    #[test]
    fn format_f_on_float_compiles() {
        let coll = lit_collection(vec![lit_float(3.25)]);
        let r = Format
            .execute(&[lit_str("%f"), coll], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("3.25".into()));
    }

    #[test]
    fn string_index_of_found() {
        assert_eq!(
            IndexOf
                .execute(&[lit_str("hello"), lit_str("ll")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(2)
        );
    }

    #[test]
    fn string_index_of_not_found() {
        assert_eq!(
            IndexOf
                .execute(&[lit_str("hello"), lit_str("xyz")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(-1)
        );
    }

    /// `testFromIndex`: the 3-arg form searches from the index and
    /// answers an absolute position.
    #[test]
    fn string_index_of_from_index() {
        let from = |i| {
            IndexOf
                .execute(
                    &[lit_str("the the"), lit_str("h"), lit_int(i)],
                    &mut MockCtx,
                )
                .unwrap()
                .into_value()
        };
        assert_eq!(from(0), Value::Integer(1));
        assert_eq!(from(3), Value::Integer(5));
    }

    #[test]
    fn string_contains() {
        assert_eq!(
            Contains
                .execute(&[lit_str("hello"), lit_str("ell")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Boolean(true)
        );
    }

    #[test]
    fn contains_type_mismatch_errors() {
        assert!(
            Contains
                .execute(&[lit_str("a"), lit_int(1)], &mut MockCtx)
                .is_err()
        );
    }

    /// `testTrueEndsWith`, `testFalseEndsWith`.
    #[test]
    fn string_ends_with() {
        let fox = "the quick brown fox jumps over the lazy dog";
        let ends = |suffix| {
            EndsWith
                .execute(&[lit_str(fox), lit_str(suffix)], &mut MockCtx)
                .unwrap()
                .into_value()
        };
        assert_eq!(ends("dog"), Value::Boolean(true));
        assert_eq!(ends("lazy dog"), Value::Boolean(true));
        assert_eq!(ends("brown fox"), Value::Boolean(false));
        assert!(EndsWith.execute(&[lit_str(fox)], &mut MockCtx).is_err());
    }

    #[test]
    fn string_length() {
        assert_eq!(
            Length
                .execute(&[lit_str("hello")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::Integer(5)
        );
    }

    #[test]
    fn length_wrong_arg_count_and_type_mismatch_error() {
        assert!(Length.execute(&[], &mut MockCtx).is_err());
        assert!(Length.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    /// `testSplit`, `testSplitWithNoSplit`: a found token splits, an
    /// absent one leaves the whole string as the single element.
    #[test]
    fn string_split() {
        let split = |s, token| {
            Split
                .execute(&[lit_str(s), lit_str(token)], &mut MockCtx)
                .unwrap()
                .into_value()
                .to_collection()
        };
        let strings = |parts: &[&str]| -> im_rc::Vector<Value> {
            parts.iter().map(|p| Value::String((*p).into())).collect()
        };
        assert_eq!(split("Hello World", " "), strings(&["Hello", "World"]));
        assert_eq!(split("Hello World", ";"), strings(&["Hello World"]));
        assert!(Split.execute(&[lit_str("a")], &mut MockCtx).is_err());
    }

    #[test]
    fn string_substring() {
        let r = Substring
            .execute(
                &[lit_str("hello world"), lit_int(6), lit_int(11)],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::String("world".into()));
    }

    /// `testStart`: the 2-arg form runs to the end of the string.
    #[test]
    fn string_substring_from_start() {
        let r = Substring
            .execute(&[lit_str("the quick"), lit_int(4)], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("quick".into()));
    }

    #[test]
    fn substring_wrong_arg_count_and_type_mismatch_error() {
        assert!(Substring.execute(&[lit_str("a")], &mut MockCtx).is_err());
        assert!(
            Substring
                .execute(&[lit_str("a"), lit_str("b"), lit_int(1)], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn string_to_lower() {
        assert_eq!(
            ToLower
                .execute(&[lit_str("Hello")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("hello".into())
        );
    }

    /// `testToUpper`.
    #[test]
    fn string_to_upper() {
        let upper = |s| {
            ToUpper
                .execute(&[lit_str(s)], &mut MockCtx)
                .unwrap()
                .into_value()
        };
        assert_eq!(upper("TesT"), Value::String("TEST".into()));
        assert_eq!(upper("TEST"), Value::String("TEST".into()));
        assert!(ToUpper.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    fn string_trim() {
        assert_eq!(
            Trim.execute(&[lit_str("  hi  ")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("hi".into())
        );
    }

    #[test]
    fn ltrim_strips_leading_whitespace() {
        assert_eq!(
            Ltrim
                .execute(&[lit_str("  hi  ")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("hi  ".into())
        );
    }

    #[test]
    fn ltrim_empty_string() {
        assert_eq!(
            Ltrim
                .execute(&[lit_str("")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("".into())
        );
    }

    #[test]
    fn ltrim_wrong_arg_count() {
        assert!(Ltrim.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn ltrim_type_mismatch() {
        assert!(Ltrim.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    fn rtrim_strips_trailing_whitespace() {
        assert_eq!(
            Rtrim
                .execute(&[lit_str("  hi  ")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("  hi".into())
        );
    }

    #[test]
    fn rtrim_empty_string() {
        assert_eq!(
            Rtrim
                .execute(&[lit_str("")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("".into())
        );
    }

    #[test]
    fn rtrim_wrong_arg_count() {
        assert!(
            Rtrim
                .execute(&[lit_str("a"), lit_str("b")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn rtrim_type_mismatch() {
        use crate::native::lit_bool;
        assert!(Rtrim.execute(&[lit_bool(true)], &mut MockCtx).is_err());
    }

    #[test]
    fn reverse_string_ascii() {
        assert_eq!(
            ReverseString
                .execute(&[lit_str("hello")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("olleh".into())
        );
    }

    #[test]
    fn reverse_string_empty() {
        assert_eq!(
            ReverseString
                .execute(&[lit_str("")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("".into())
        );
    }

    #[test]
    fn reverse_string_unicode_preserves_codepoints() {
        assert_eq!(
            ReverseString
                .execute(&[lit_str("héllo")], &mut MockCtx)
                .unwrap()
                .into_value(),
            Value::String("olléh".into())
        );
    }

    #[test]
    fn reverse_string_wrong_arg_count() {
        assert!(ReverseString.execute(&[], &mut MockCtx).is_err());
    }

    #[test]
    fn reverse_string_type_mismatch() {
        assert!(ReverseString.execute(&[lit_int(1)], &mut MockCtx).is_err());
    }

    #[test]
    fn replace_replaces_all_occurrences() {
        let r = Replace
            .execute(
                &[lit_str("foo bar foo"), lit_str("foo"), lit_str("baz")],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::String("baz bar baz".into()));
    }

    #[test]
    fn replace_no_match_returns_source_unchanged() {
        let r = Replace
            .execute(
                &[lit_str("hello"), lit_str("xyz"), lit_str("abc")],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::String("hello".into()));
    }

    #[test]
    fn replace_wrong_arg_count() {
        assert!(
            Replace
                .execute(&[lit_str("a"), lit_str("b")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn replace_type_mismatch() {
        assert!(
            Replace
                .execute(&[lit_str("a"), lit_int(1), lit_str("c")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn join_strings_2arg_basic() {
        let coll = lit_collection(vec![lit_str("a"), lit_str("b"), lit_str("c")]);
        let r = JoinStrings
            .execute(&[coll, lit_str(",")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("a,b,c".into()));
    }

    #[test]
    fn join_strings_2arg_empty_collection() {
        let coll = lit_collection(vec![]);
        let r = JoinStrings
            .execute(&[coll, lit_str(",")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("".into()));
    }

    #[test]
    fn join_strings_2arg_scalar_treated_as_singleton() {
        let r = JoinStrings
            .execute(&[lit_str("lone"), lit_str(",")], &mut MockCtx)
            .unwrap();
        assert_eq!(r.into_value(), Value::String("lone".into()));
    }

    #[test]
    fn join_strings_4arg_with_prefix_and_suffix() {
        let coll = lit_collection(vec![lit_str("a"), lit_str("b")]);
        let r = JoinStrings
            .execute(
                &[coll, lit_str("["), lit_str(", "), lit_str("]")],
                &mut MockCtx,
            )
            .unwrap();
        assert_eq!(r.into_value(), Value::String("[a, b]".into()));
    }

    #[test]
    fn join_strings_wrong_arg_count() {
        assert!(JoinStrings.execute(&[], &mut MockCtx).is_err());
        assert!(
            JoinStrings
                .execute(&[lit_str("only")], &mut MockCtx)
                .is_err()
        );
        assert!(
            JoinStrings
                .execute(&[lit_str("a"), lit_str("b"), lit_str("c")], &mut MockCtx)
                .is_err()
        );
    }

    #[test]
    fn join_strings_element_type_mismatch() {
        let coll = lit_collection(vec![lit_str("a"), lit_int(1)]);
        assert!(
            JoinStrings
                .execute(&[coll, lit_str(",")], &mut MockCtx)
                .is_err()
        );
    }

    /// Build `Collection(Collection(... Integer(0) ...))` `depth`
    /// levels deep.
    fn deep_nested_collection(depth: usize) -> Value {
        let mut v = Value::Integer(0);
        for _ in 0..depth {
            let mut inner = im_rc::Vector::new();
            inner.push_back(v);
            v = Value::Collection(Box::new(inner));
        }
        v
    }

    /// Locks the iterative `pure_to_string` contract: rendering a
    /// 10 000-level-deep `Collection(...)` chain must not overflow
    /// the Rust thread stack.
    #[test]
    fn pure_to_string_deep_collection_no_overflow() {
        let v = deep_nested_collection(10_000);
        let s = super::pure_to_string(&v, &mut MockCtx).expect("render must succeed");
        assert_eq!(s.matches('[').count(), 10_000);
        assert_eq!(s.matches(']').count(), 10_000);
        assert!(s.contains('0'));
    }

    /// Sanity: shape produced for a small list matches `toString`
    /// form (`[1, 2, 3]`, no quotes around integers).
    #[test]
    fn pure_to_string_shallow_collection_shape() {
        let mut pv = im_rc::Vector::new();
        pv.push_back(Value::Integer(1));
        pv.push_back(Value::Integer(2));
        pv.push_back(Value::Integer(3));
        let v = Value::Collection(Box::new(pv));
        assert_eq!(
            super::pure_to_string(&v, &mut MockCtx).unwrap(),
            "[1, 2, 3]"
        );
    }

    /// `testTrueStartsWith`, `testFalseStartsWith`.
    #[test]
    fn string_starts_with() {
        let fox = "the quick brown fox jumps over the lazy dog";
        let starts = |prefix| {
            StartsWith
                .execute(&[lit_str(fox), lit_str(prefix)], &mut MockCtx)
                .unwrap()
                .into_value()
        };
        assert_eq!(starts("the"), Value::Boolean(true));
        assert_eq!(starts("the quick brown fox"), Value::Boolean(true));
        assert_eq!(starts("the lazy dog"), Value::Boolean(false));
        assert!(StartsWith.execute(&[lit_str(fox)], &mut MockCtx).is_err());
    }

    fn decimal(s: &str) -> Value {
        Value::Decimal(s.parse().unwrap())
    }

    /// `testParseDecimal`, `testParseZero`: the `d` suffix, a leading `+`
    /// and zero padding are accepted.
    #[test]
    fn parse_decimal_accepts_suffix_sign_and_padding() {
        let parse = |s| {
            ParseDecimal
                .execute(&[lit_str(s)], &mut MockCtx)
                .unwrap()
                .into_value()
        };
        assert_eq!(parse("3.14159d"), decimal("3.14159"));
        assert_eq!(parse("3.14159"), decimal("3.14159"));
        assert_eq!(parse("-3.14159d"), decimal("-3.14159"));
        assert_eq!(parse("+0000003.1415900000"), decimal("3.14159"));
        assert_eq!(parse("-0000003.1415900000"), decimal("-3.14159"));
        assert_eq!(parse("0.0"), decimal("0"));
        assert_eq!(parse("-000.000"), decimal("0"));
        assert!(
            ParseDecimal
                .execute(&[lit_str("x1")], &mut MockCtx)
                .is_err()
        );
    }

    /// `testParseDecimalWithPrecisionScale`: the 3-arg form rounds to
    /// `scale`.
    #[test]
    fn parse_decimal_with_precision_and_scale_rounds_to_scale() {
        let parse = |s, precision, scale| {
            ParseDecimal
                .execute(
                    &[lit_str(s), lit_int(precision), lit_int(scale)],
                    &mut MockCtx,
                )
                .unwrap()
                .into_value()
        };
        assert_eq!(parse("123.126", 5, 2), decimal("123.13"));
        assert_eq!(parse("123.123", 7, 3), decimal("123.123"));
        assert_eq!(parse("-3.14159", 5, 2), decimal("-3.14"));
        assert_eq!(parse("123.123", 10, 5), decimal("123.12300"));
        assert_eq!(parse("+0000003.1415900000", 38, 10), decimal("3.14159"));
        assert!(
            ParseDecimal
                .execute(&[lit_str("1.5"), lit_int(5), lit_int(29)], &mut MockCtx)
                .is_err()
        );
    }
}
