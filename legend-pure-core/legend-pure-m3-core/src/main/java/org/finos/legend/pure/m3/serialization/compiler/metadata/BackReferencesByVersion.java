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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.list.ListIterable;

import java.util.function.ObjIntConsumer;

/**
 * Back references grouped by the reference id version that they were written with, which is the version they must be
 * resolved with. A virtual package needs them grouped this way: it has no element file, so it has no reference id
 * version of its own, and the modules that hold back references to it may have written them with different versions.
 */
public interface BackReferencesByVersion
{
    /**
     * Apply the consumer to the back references written with each reference id version, together with that version,
     * in ascending order of version.
     *
     * @param consumer consumer taking back references and the reference id version to resolve them with
     */
    void forEachVersion(ObjIntConsumer<? super ListIterable<? extends BackReference>> consumer);
}
