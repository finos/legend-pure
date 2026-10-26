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

//! Intrinsics: the total operations a native declares
//! ([`NativeFunction::intrinsic`](crate::native::NativeFunction::intrinsic)),
//! selected at a call site by the representations of its arguments'
//! static types and run inline on unboxed operands. The native stays the
//! definition: when an operand does not hold the representation its
//! static type promised, every operand goes to the native, boxed, and
//! its result or error is the call's.

use std::cmp::Ordering;

use super::Rep;
use crate::native::Intrinsic;

/// A number operand of an ordering intrinsic.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Num {
    /// An `Integer[1]`, run as [`Rep::I64`].
    I64,
    /// A `Float[1]`, run as [`Rep::F64`].
    F64,
}

impl Num {
    fn of(rep: Rep) -> Option<Self> {
        match rep {
            Rep::I64 => Some(Self::I64),
            Rep::F64 => Some(Self::F64),
            _ => None,
        }
    }

    /// The representation the operand is run in.
    #[must_use]
    pub fn rep(self) -> Rep {
        match self {
            Self::I64 => Rep::I64,
            Self::F64 => Rep::F64,
        }
    }
}

/// An intrinsic as a call site selected it: the declared operation and
/// the representations its operands run in.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum IntrinsicOp {
    /// [`Intrinsic::Equal`] on two operands of this representation:
    /// [`Rep::I64`], [`Rep::F64`], [`Rep::Bool`] or [`Rep::Str`].
    Equal(Rep),
    /// [`Intrinsic::LessThan`] on two numbers.
    LessThan(Num, Num),
    /// [`Intrinsic::LessThanEqual`] on two numbers.
    LessThanEqual(Num, Num),
    /// [`Intrinsic::Not`] on a [`Rep::Bool`].
    Not,
    /// [`Intrinsic::Size`] on a value of any representation, run as
    /// [`Rep::Value`].
    Size,
    /// [`Intrinsic::IsEmpty`] on a value of any representation, run as
    /// [`Rep::Value`].
    IsEmpty,
}

impl IntrinsicOp {
    /// The intrinsic a call to a native declaring `declared` runs, for
    /// arguments whose static types have the representations `reps`;
    /// `None` when there is none for them.
    #[must_use]
    pub fn select(declared: Intrinsic, reps: &[Rep]) -> Option<Self> {
        match (declared, reps) {
            (Intrinsic::Equal, &[a, b])
                if a == b && matches!(a, Rep::I64 | Rep::F64 | Rep::Bool | Rep::Str) =>
            {
                Some(Self::Equal(a))
            }
            (Intrinsic::LessThan, &[a, b]) => Some(Self::LessThan(Num::of(a)?, Num::of(b)?)),
            (Intrinsic::LessThanEqual, &[a, b]) => {
                Some(Self::LessThanEqual(Num::of(a)?, Num::of(b)?))
            }
            (Intrinsic::Not, &[Rep::Bool]) => Some(Self::Not),
            (Intrinsic::Size, &[_]) => Some(Self::Size),
            (Intrinsic::IsEmpty, &[_]) => Some(Self::IsEmpty),
            _ => None,
        }
    }

    /// The representation operand `i` runs in.
    #[must_use]
    pub fn operand_rep(self, i: usize) -> Rep {
        match (self, i) {
            (Self::Equal(rep), _) => rep,
            (Self::LessThan(a, _) | Self::LessThanEqual(a, _), 0) => a.rep(),
            (Self::LessThan(_, b) | Self::LessThanEqual(_, b), _) => b.rep(),
            (Self::Not, _) => Rep::Bool,
            (Self::Size | Self::IsEmpty, _) => Rep::Value,
        }
    }

    /// The representation of the result.
    #[must_use]
    pub fn result_rep(self) -> Rep {
        match self {
            Self::Size => Rep::I64,
            Self::Equal(_)
            | Self::LessThan(..)
            | Self::LessThanEqual(..)
            | Self::Not
            | Self::IsEmpty => Rep::Bool,
        }
    }
}

/// An unboxed number operand.
#[derive(Debug, Clone, Copy)]
pub(crate) enum Number {
    I64(i64),
    F64(f64),
}

impl Number {
    /// The order of two numbers as `compare_values` orders them.
    #[allow(clippy::cast_precision_loss)]
    pub(crate) fn order(self, other: Self) -> Ordering {
        match (self, other) {
            (Self::I64(a), Self::I64(b)) => a.cmp(&b),
            (Self::F64(a), Self::F64(b)) => a.total_cmp(&b),
            (Self::I64(a), Self::F64(b)) => (a as f64).total_cmp(&b),
            (Self::F64(a), Self::I64(b)) => a.total_cmp(&(b as f64)),
        }
    }
}

/// `Float` equality as [`Value`](crate::value::Value)'s `==` decides it.
#[allow(clippy::float_cmp)]
pub(crate) fn float_equal(a: f64, b: f64) -> bool {
    a == b || (a.is_nan() && b.is_nan())
}
