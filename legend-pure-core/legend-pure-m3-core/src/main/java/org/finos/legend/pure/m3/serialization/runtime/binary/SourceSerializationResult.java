// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;

public class SourceSerializationResult
{
    private final String sourceId;
    private final ImmutableSet<String> serializedInstances;
    private final ImmutableSet<String> externalReferences;

    SourceSerializationResult(String sourceId, Iterable<String> serializedInstance, Iterable<String> externalReferences)
    {
        this.sourceId = sourceId;
        this.serializedInstances = Sets.immutable.withAll(serializedInstance);
        this.externalReferences = Sets.immutable.withAll(externalReferences);
    }

    public String getSourceId()
    {
        return this.sourceId;
    }

    public SetIterable<String> getSerializedInstances()
    {
        return this.serializedInstances;
    }

    public SetIterable<String> getExternalReferences()
    {
        return this.externalReferences;
    }
}
