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

package org.finos.legend.pure.rust;

/**
 * The class {@code crates/jni} resolves its method ids on, by this name:
 * every method it calls ({@code describe}, the reads, {@code forget},
 * {@code failureOf}, the primitive and date factories, {@code element},
 * {@code enumValue}, {@code node}, {@code rustObject}, {@code newMap}) is
 * {@link NodeBridge}'s, with the signature the crate names. Rust holds one
 * global reference to it per context.
 */
final class RustNodeBridge extends NodeBridge
{
    RustNodeBridge(RustEngineHost host, RustHeap owner)
    {
        super(host, owner);
    }
}
