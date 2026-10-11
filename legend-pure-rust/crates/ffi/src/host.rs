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

//! The host behind a callback table: every read the bridge makes is one
//! call through a function pointer the host handed over, answered into a
//! [`Reply`] Rust owns for the duration of the callback.

use std::ffi::c_void;

use legend_pure_evaluator::bridge::description::Description;
use legend_pure_evaluator::bridge::failure::{boundary_error, typed};
use legend_pure_evaluator::bridge::host::Host;
use legend_pure_evaluator::error::{PureException, PureRuntimeError};
use smol_str::SmolStr;

use crate::abi::{
    ForgetFn, KeysFn, PropertyValuesFn, PureDescription, PureHostTable, PureReply, PureString,
    failure_kind, status,
};
use crate::decode;

/// What a callback answers with: descriptions, names, or why it failed.
/// The callback's status decides which is read; a recorded failure wins
/// over any status.
pub struct Reply {
    what: String,
    descriptions: Vec<Description>,
    names: Vec<SmolStr>,
    failure: Option<PureException>,
}

impl Reply {
    /// An empty reply for the callback named by `what`.
    #[must_use]
    pub fn new(what: String) -> Self {
        Self {
            what,
            descriptions: Vec::new(),
            names: Vec::new(),
            failure: None,
        }
    }

    /// The reply behind the opaque pointer a callback was handed.
    ///
    /// # Safety
    /// `ptr` is the pointer a callback received, still inside that
    /// callback, and no other reference to the reply is live.
    ///
    /// # Errors
    /// A NULL pointer.
    pub unsafe fn from_ptr<'a>(ptr: *mut PureReply) -> Result<&'a mut Self, PureException> {
        if ptr.is_null() {
            return Err(boundary_error("a NULL reply"));
        }
        // SAFETY: per this function's contract, the pointer is the one
        // `FfiHost` made from a live `Reply` for the running callback.
        Ok(unsafe { &mut *ptr.cast::<Self>() })
    }

    /// Keep a failure; the first one is kept.
    pub fn note(&mut self, failure: PureException) {
        if self.failure.is_none() {
            self.failure = Some(failure);
        }
    }

    /// Copy `count` descriptions at `items` into the reply.
    ///
    /// # Safety
    /// `items` satisfies [`decode::descriptions`]'s contract.
    ///
    /// # Errors
    /// A malformed array or description; nothing is pushed then.
    pub unsafe fn push_descriptions(
        &mut self,
        items: *const PureDescription,
        count: usize,
    ) -> Result<(), PureException> {
        // SAFETY: this function's contract is `decode::descriptions`'.
        let items = unsafe { decode::descriptions(items, count, &self.what) }?;
        let mut copied = Vec::with_capacity(items.len());
        for item in items {
            // SAFETY: this function's contract covers each description.
            copied.push(unsafe { decode::description(item) }?);
        }
        self.descriptions.extend(copied);
        Ok(())
    }

    /// Copy `count` names at `items` into the reply.
    ///
    /// # Safety
    /// `items` is NULL or points to `count` strings, each satisfying
    /// [`decode::text`]'s contract.
    ///
    /// # Errors
    /// A malformed array or an absent name; nothing is pushed then.
    pub unsafe fn push_names(
        &mut self,
        items: *const PureString,
        count: usize,
    ) -> Result<(), PureException> {
        if count == 0 {
            return Ok(());
        }
        if items.is_null() {
            return Err(boundary_error(format!(
                "{}: a NULL array of {count} names",
                self.what
            )));
        }
        // SAFETY: per this function's contract, `items` is non-NULL
        // here and points to `count` strings valid for the call.
        let items = unsafe { std::slice::from_raw_parts(items, count) };
        let mut copied = Vec::with_capacity(items.len());
        for item in items {
            // SAFETY: this function's contract covers each string.
            let name = unsafe { decode::optional(*item, &self.what) }?
                .ok_or_else(|| boundary_error(format!("{}: an absent name", self.what)))?;
            copied.push(SmolStr::new(name));
        }
        self.names.extend(copied);
        Ok(())
    }

    /// Record why the callback failed, with the kind the host named:
    /// `FUNCTION_NOT_FOUND`, `UNSUPPORTED` and `NOT_IMPLEMENTED` keep
    /// their kind; anything else is an execution error naming the read.
    pub fn fail(&mut self, kind: i32, message: &str) {
        let failure = match kind {
            failure_kind::UNSUPPORTED => typed(PureRuntimeError::Unsupported(message.to_owned())),
            failure_kind::NOT_IMPLEMENTED => {
                typed(PureRuntimeError::NotImplemented(SmolStr::new(message)))
            }
            failure_kind::FUNCTION_NOT_FOUND => {
                typed(PureRuntimeError::FunctionNotFound(SmolStr::new(message)))
            }
            _ => boundary_error(format!("{}: {message}", self.what)),
        };
        self.note(failure);
    }

    /// The failure a status other than `OK` and `ABSENT` stands for when
    /// the host recorded none.
    fn unexplained(&self, status: i32) -> PureException {
        boundary_error(format!(
            "{}: the host answered with status {status} and no failure",
            self.what
        ))
    }

    /// The descriptions a callback answered with: `None` for `ABSENT`.
    ///
    /// # Errors
    /// A recorded failure, or a status that is neither `OK` nor `ABSENT`.
    pub fn into_descriptions(self, status: i32) -> Result<Option<Vec<Description>>, PureException> {
        if let Some(failure) = self.failure {
            return Err(failure);
        }
        match status {
            status::OK => Ok(Some(self.descriptions)),
            status::ABSENT => Ok(None),
            other => Err(self.unexplained(other)),
        }
    }

    /// The names a callback answered with; `ABSENT` reads as none.
    ///
    /// # Errors
    /// A recorded failure, or a status that is neither `OK` nor `ABSENT`.
    pub fn into_names(self, status: i32) -> Result<Vec<SmolStr>, PureException> {
        if let Some(failure) = self.failure {
            return Err(failure);
        }
        match status {
            status::OK => Ok(self.names),
            status::ABSENT => Ok(Vec::new()),
            other => Err(self.unexplained(other)),
        }
    }
}

/// The host of one context: the callbacks of its table, checked once.
pub struct FfiHost {
    host: *mut c_void,
    property_values: PropertyValuesFn,
    captured_values: PropertyValuesFn,
    keys: KeysFn,
    forget: Option<ForgetFn>,
}

impl FfiHost {
    /// A host over a table the context copied.
    ///
    /// # Errors
    /// A required callback is NULL.
    pub fn new(table: &PureHostTable) -> Result<Self, PureException> {
        let required = |name: &str| boundary_error(format!("the host table's `{name}` is NULL"));
        Ok(Self {
            host: table.host,
            property_values: table
                .property_values
                .ok_or_else(|| required("property_values"))?,
            captured_values: table
                .captured_values
                .ok_or_else(|| required("captured_values"))?,
            keys: table.keys.ok_or_else(|| required("keys"))?,
            forget: table.forget,
        })
    }

    /// Call a `(host, id, name, reply)` callback over a fresh reply.
    fn ask(&self, callback: PropertyValuesFn, id: i64, name: &str, what: &str) -> (Reply, i32) {
        let mut reply = Reply::new(format!("reading `{name}` on host node {id} ({what})"));
        let ptr = std::ptr::from_mut(&mut reply).cast::<PureReply>();
        // SAFETY: the host handed this callback over for exactly this
        // call shape (header, the callback table): `host` is its own
        // pointer, `name` is `name.len()` readable bytes for the call,
        // and the reply is a live local the callback uses only inside
        // the call.
        let status = unsafe { callback(self.host, id, name.as_ptr(), name.len(), ptr) };
        (reply, status)
    }
}

impl Host for FfiHost {
    fn property_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        let (reply, status) = self.ask(self.property_values, id, name, "property values");
        reply.into_descriptions(status)
    }

    fn captured_values(
        &self,
        id: i64,
        name: &str,
    ) -> Result<Option<Vec<Description>>, PureException> {
        let (reply, status) = self.ask(self.captured_values, id, name, "captured values");
        reply.into_descriptions(status)
    }

    fn keys_of(&self, id: i64) -> Result<Vec<SmolStr>, PureException> {
        let mut reply = Reply::new(format!("reading the keys of host node {id}"));
        let ptr = std::ptr::from_mut(&mut reply).cast::<PureReply>();
        // SAFETY: as for `ask`: the host's own callback and pointer, and
        // a live local reply used only inside the call.
        let status = unsafe { (self.keys)(self.host, id, ptr) };
        reply.into_names(status)
    }

    fn forget(&self, ids: &[i64]) {
        if let Some(forget) = self.forget {
            // SAFETY: the host's own callback and pointer; `ids` is
            // `ids.len()` readable integers for the call.
            unsafe { forget(self.host, ids.as_ptr(), ids.len()) };
        }
    }
}
