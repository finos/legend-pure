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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.serialization.runtime.Source;

public class SourceDeserializationResult
{
    public static final Predicate<SourceDeserializationResult> HAS_NODES = SourceDeserializationResult::hasDeserializationNodes;
    public static final Function<SourceDeserializationResult, ImmutableList<DeserializationNode>> GET_NODES = SourceDeserializationResult::getDeserializationNodes;

    private final Source source;
    private final ImmutableListMultimap<String, String> instancesByParser;
    private final ImmutableList<String> otherInstances;
    private final ImmutableSet<String> externalReferences;
    private final ImmutableList<DeserializationNode> deserializationNodes;

    SourceDeserializationResult(Source source, ListMultimap<String, String> instancesByParser, ListIterable<String> otherInstances, Iterable<String> externalReferences, Iterable<? extends DeserializationNode> deserializationNodes)
    {
        this.source = source;
        this.instancesByParser = (instancesByParser == null) ? null : instancesByParser.toImmutable();
        this.otherInstances = (otherInstances == null) ? null : otherInstances.toImmutable();
        this.externalReferences = (externalReferences == null) ? null : Sets.immutable.withAll(externalReferences);
        this.deserializationNodes = (deserializationNodes == null) ? null : Lists.immutable.withAll(deserializationNodes);
    }

    public Source getSource()
    {
        return this.source;
    }

    public boolean hasInstancesByParser()
    {
        return this.instancesByParser != null;
    }

    public ListMultimap<String, String> getInstancesByParser()
    {
        return this.instancesByParser;
    }

    public boolean hasOtherInstances()
    {
        return this.otherInstances != null;
    }

    public ListIterable<String> getOtherInstances()
    {
        return this.otherInstances;
    }

    public RichIterable<String> getInstances()
    {
        if ((this.instancesByParser == null) && (this.otherInstances == null))
        {
            return Lists.immutable.empty();
        }
        if (this.instancesByParser == null)
        {
            return this.otherInstances;
        }
        if (this.otherInstances == null)
        {
            return this.instancesByParser.valuesView();
        }
        return this.instancesByParser.valuesView().asLazy().concatenate(this.otherInstances);
    }

    public boolean hasExternalReferences()
    {
        return this.externalReferences != null;
    }

    public SetIterable<String> getExternalReferences()
    {
        return this.externalReferences;
    }

    public boolean hasDeserializationNodes()
    {
        return this.deserializationNodes != null;
    }

    public ImmutableList<DeserializationNode> getDeserializationNodes()
    {
        return this.deserializationNodes;
    }
}
