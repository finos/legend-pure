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

//! One call at a time on a context.
//!
//! A host serialises calls on one evaluator, but its locks are
//! re-entrant: host code that runs while Rust is inside a call (a
//! property read, a hashing strategy reading a Rust object) can call back
//! into the same evaluator on the same thread. A second mutable view of
//! the context would then alias the first. So the context is only reached
//! through [`Session::enter`], which refuses a call while another is
//! running and otherwise hands out the one mutable view. A transport
//! keeps the session behind the integer its host holds; the round trip
//! between that integer and the session is the transport's.

use std::cell::{Cell, RefCell, RefMut};

use crate::bridge::failure::{boundary_error, unsupported};
use crate::error::PureException;

/// A context and its busy flag.
pub struct Session<C> {
    context: RefCell<C>,
    /// Set once a panic unwound through a call: the context's state is
    /// unknown from then on.
    poisoned: Cell<bool>,
}

impl<C> Session<C> {
    /// A session over a context no call has entered.
    pub const fn new(context: C) -> Self {
        Self {
            context: RefCell::new(context),
            poisoned: Cell::new(false),
        }
    }

    /// Enter the context for one call: the one mutable view, released
    /// when dropped. A poisoned context can still be entered, for the
    /// housekeeping a host does before closing it; a call that runs the
    /// evaluator checks [`live`](Self::live) first.
    ///
    /// # Errors
    /// A call made while another call on the same context is still
    /// running (re-entry from the host).
    pub fn enter(&self) -> Result<RefMut<'_, C>, PureException> {
        self.context.try_borrow_mut().map_err(|_| {
            unsupported(
                "a call into the Rust evaluator while another call on it is running (re-entrant call from the host)",
            )
        })
    }

    /// Whether the context may still run: not poisoned by an earlier
    /// panic.
    ///
    /// # Errors
    /// The context is poisoned.
    pub fn live(&self) -> Result<(), PureException> {
        if self.poisoned.get() {
            return Err(boundary_error(
                "the evaluator is poisoned by an earlier panic; close it and create a new one",
            ));
        }
        Ok(())
    }

    /// Mark the context unusable after a panic unwound through a call
    /// (the view of that call was dropped by the unwind).
    pub fn poison(&self) {
        self.poisoned.set(true);
    }

    /// Whether the session may be closed now.
    ///
    /// # Errors
    /// A call on the context is still running.
    pub fn ensure_idle(&self) -> Result<(), PureException> {
        if self.context.try_borrow_mut().is_err() {
            return Err(unsupported(
                "closing the Rust evaluator while a call on it is running",
            ));
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_second_entry_while_the_first_is_held_is_refused() {
        let session = Session::new(41_i32);
        {
            let mut first = session.enter().unwrap();
            *first += 1;
            assert!(session.enter().is_err(), "re-entry must be refused");
            assert!(session.ensure_idle().is_err(), "no close while a call runs");
        }
        assert_eq!(*session.enter().unwrap(), 42);
        session.ensure_idle().unwrap();
    }

    #[test]
    fn a_poisoned_session_is_not_live_but_can_still_be_entered_and_closed() {
        let session = Session::new(0_i32);
        session.live().unwrap();
        session.poison();
        assert!(session.live().is_err());
        assert!(session.enter().is_ok());
        session.ensure_idle().unwrap();
    }
}
