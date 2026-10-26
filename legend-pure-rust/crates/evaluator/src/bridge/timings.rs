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

//! Where the last call on this thread spent its time.

/// Where the last `start` on this thread spent its time: the lowering
/// (Phase A and B, including every host read it made) and the run.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct Timings {
    /// Lowering, nanoseconds.
    pub lower_ns: u64,
    /// Running the lowered closure, nanoseconds.
    pub run_ns: u64,
    /// Host property or capture reads made while the closure ran: zero
    /// means the run was the evaluator alone.
    pub run_host_reads: u64,
    /// Host property or capture reads made by the lowering: zero means
    /// everything came from the closure cache and the model.
    pub lower_host_reads: u64,
}

thread_local! {
    static LAST: std::cell::Cell<Timings> = const { std::cell::Cell::new(Timings { lower_ns: 0, run_ns: 0, run_host_reads: 0, lower_host_reads: 0 }) };
}

/// The timings of the last `start` on this thread.
#[must_use]
pub fn last() -> Timings {
    LAST.with(std::cell::Cell::get)
}

/// Record the timings of a `start` that just ended on this thread.
pub fn record(timings: Timings) {
    LAST.with(|t| t.set(timings));
}
