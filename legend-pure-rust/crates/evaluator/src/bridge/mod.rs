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

//! The bridge: the evaluator behind a host that hands nodes in and takes
//! values out.
//!
//! A *host* is a program holding a Pure graph of its own (a Java engine,
//! later any program that can call C); a *transport* is the code that
//! carries calls between the host and this crate. Everything a transport
//! does not have to know about lives here: what a host node is once it
//! has crossed and what Rust asks the host about it ([`host`]), how a
//! host describes a node ([`description`]), the lowering of one call
//! through the closure cache and the model's growth ([`context`]), the
//! one-call-at-a-time guard ([`session`]), the identity of Rust objects
//! a host holds ([`handles`]), the shape a value takes on its way out
//! ([`outbound`]), the failure a host receives ([`failure`]) and where a
//! call spent its time ([`timings`]).
//!
//! A transport implements [`Host`](host::Host) over its host's graph,
//! builds a [`BridgeContext`](context::BridgeContext) behind a
//! [`Session`](session::Session), fills a
//! [`Description`](description::Description) for every node it hands in
//! through [`description_to_value`](host::description_to_value), and
//! materialises the [`Leaf`](outbound::Leaf) of every value it takes out.

pub mod context;
pub mod description;
pub mod failure;
pub mod handles;
pub mod host;
pub mod outbound;
pub mod session;
pub mod timings;
