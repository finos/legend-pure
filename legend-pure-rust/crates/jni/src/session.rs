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

//! The allocation behind the `long` a Java `PureRustEvaluator` holds: the
//! context, and the flag that makes every entry point exclusive.
//!
//! Java serialises calls on one evaluator with its monitor, but a monitor
//! is re-entrant: Java code that runs while Rust is inside `nativeStart`
//! (a bridge method, a hashing strategy reading a Rust object's property)
//! can call back into the same evaluator on the same thread. A second
//! `&mut JniContext` would then alias the first. So the context is only
//! reached through [`Session::enter`], which refuses a call while another
//! is running and otherwise hands out the one mutable view.

use std::cell::{Cell, UnsafeCell};

use jni::sys::jlong;
use legend_pure_evaluator::error::PureException;

use crate::failure::{ffi_error, unsupported};

/// The context and its busy flag. Only ever accessed through shared
/// references: the flag is a `Cell`, the context sits in an `UnsafeCell`
/// and is lent out mutably by [`Entered`] alone.
pub(crate) struct Session<C> {
    busy: Cell<bool>,
    context: UnsafeCell<C>,
}

impl<C> Session<C> {
    /// Box a context and hand Java the pointer it will pass back.
    pub(crate) fn into_raw(context: C) -> jlong {
        let session = Box::new(Session {
            busy: Cell::new(false),
            context: UnsafeCell::new(context),
        });
        Box::into_raw(session) as jlong
    }

    /// The session behind a pointer Java passed back.
    ///
    /// # Safety
    /// `ptr` is zero or a value [`into_raw`](Self::into_raw) returned that
    /// [`free`](Self::free) has not freed yet. Java guarantees both: it
    /// keeps the pointer in one field, frees it once in `close()` and
    /// refuses every call after that (`checkOpen`).
    unsafe fn get<'a>(ptr: jlong) -> Result<&'a Session<C>, PureException> {
        if ptr == 0 {
            return Err(ffi_error("the evaluator has no context"));
        }
        // SAFETY: per this function's contract the pointer came from
        // `Box::into_raw` and is still live; a shared reference is sound
        // because nothing mutates a `Session` except through its cells.
        Ok(unsafe { &*(ptr as *const Session<C>) })
    }

    /// Enter the context behind `ptr` for one call.
    ///
    /// # Safety
    /// As for [`get`](Self::get).
    ///
    /// # Errors
    /// A null pointer, or a call made while another call on the same
    /// evaluator is still running (re-entry from Java).
    pub(crate) unsafe fn enter<'a>(ptr: jlong) -> Result<Entered<'a, C>, PureException> {
        // SAFETY: this function's contract is `get`'s.
        let session = unsafe { Self::get(ptr) }?;
        if session.busy.replace(true) {
            return Err(unsupported(
                "a call into the Rust evaluator while another call on it is running (re-entrant call from Java)",
            ));
        }
        Ok(Entered { session })
    }

    /// Free the session behind `ptr`.
    ///
    /// # Safety
    /// As for [`get`](Self::get), and `ptr` is not used again.
    ///
    /// # Errors
    /// A call on the same evaluator is still running.
    pub(crate) unsafe fn free(ptr: jlong) -> Result<(), PureException> {
        if ptr == 0 {
            return Ok(());
        }
        // SAFETY: this function's contract is `get`'s.
        let session = unsafe { Self::get(ptr) }?;
        if session.busy.get() {
            return Err(unsupported(
                "closing the Rust evaluator while a call on it is running",
            ));
        }
        // SAFETY: the pointer came from `Box::into_raw` in `into_raw`, no
        // call is running (so no `Entered` and no reference into the
        // session exists), and Java never passes it again after `close()`.
        drop(unsafe { Box::from_raw(ptr as *mut Session<C>) });
        Ok(())
    }
}

/// The one mutable view of a context, for the duration of one call.
pub(crate) struct Entered<'a, C> {
    session: &'a Session<C>,
}

impl<C> std::ops::Deref for Entered<'_, C> {
    type Target = C;

    fn deref(&self) -> &C {
        // SAFETY: this `Entered` set `busy`, so no other `Entered` exists
        // and the context is reachable through this one only.
        unsafe { &*self.session.context.get() }
    }
}

impl<C> std::ops::DerefMut for Entered<'_, C> {
    fn deref_mut(&mut self) -> &mut C {
        // SAFETY: as for `deref`; `&mut self` makes this borrow unique.
        unsafe { &mut *self.session.context.get() }
    }
}

impl<C> Drop for Entered<'_, C> {
    fn drop(&mut self) {
        self.session.busy.set(false);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_second_entry_while_the_first_is_held_is_refused() {
        let ptr = Session::into_raw(41_i32);
        // SAFETY: `ptr` comes from `into_raw` above and is freed once, last.
        unsafe {
            {
                let mut first = Session::<i32>::enter(ptr).unwrap();
                *first += 1;
                let second = Session::<i32>::enter(ptr);
                assert!(second.is_err(), "re-entry must be refused");
                assert!(
                    Session::<i32>::free(ptr).is_err(),
                    "no free while a call runs"
                );
            }
            assert_eq!(*Session::<i32>::enter(ptr).unwrap(), 42);
            Session::<i32>::free(ptr).unwrap();
        }
    }

    #[test]
    fn a_null_pointer_is_an_error_not_a_dereference() {
        // SAFETY: zero is never dereferenced.
        unsafe {
            assert!(Session::<i32>::enter(0).is_err());
            assert!(Session::<i32>::free(0).is_ok());
        }
    }
}
