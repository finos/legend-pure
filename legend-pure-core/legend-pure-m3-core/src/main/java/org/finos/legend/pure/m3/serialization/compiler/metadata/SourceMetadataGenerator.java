// Copyright 2025 Goldman Sachs
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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Collection;

public class SourceMetadataGenerator
{
    public SourceMetadata generateSourceMetadata(Source source)
    {
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        if (elementsByParser == null)
        {
            throw new IllegalStateException("Cannot generate metadata for source without linked elements: " + source.getId());
        }
        MutableList<Pair<String, ListIterable<CoreInstance>>> sections = elementsByParser.keysView()
                .collect(
                        parser -> Tuples.pair(parser.getName(), elementsByParser.get(parser)),
                        Lists.mutable.ofInitialCapacity(elementsByParser.sizeDistinct()))
                .sortThis((s1, s2) ->
                {
                    ListIterable<CoreInstance> elements1 = s1.getTwo();
                    ListIterable<CoreInstance> elements2 = s2.getTwo();
                    return elements1.isEmpty() ?
                           (elements2.isEmpty() ? s1.getOne().compareTo(s2.getOne()) : -1) :
                           (elements2.isEmpty() ? 1 : SourceInformation.compareByStartPosition(elements1.get(0).getSourceInformation(), elements2.get(0).getSourceInformation()));
                });

        SourceMetadata.Builder builder = SourceMetadata.builder(sections.size()).withSourceId(source.getId());
        sections.forEach(s -> builder.withSection(s.getOne(), s.getTwo().asLazy().collect(PackageableElement::getUserPathForPackageableElement)));
        return builder.build();
    }

    public MutableList<SourceMetadata> generateSourceMetadata(Iterable<? extends Source> sources)
    {
        return Iterate.collect(
                sources,
                this::generateSourceMetadata,
                (sources instanceof Collection) ? Lists.mutable.ofInitialCapacity(((Collection<?>) sources).size()) : Lists.mutable.empty());
    }

    public MutableList<SourceMetadata> generateSourceMetadata(Source... sources)
    {
        return generateSourceMetadata(ArrayAdapter.adapt(sources));
    }

    public MutableList<SourceMetadata> generateSourceMetadata(SourceRegistry sourceRegistry, Iterable<String> sourceIds)
    {
        return Iterate.collect(
                sourceIds,
                id ->
                {
                    Source source = sourceRegistry.getSource(id);
                    if (source == null)
                    {
                        throw new RuntimeException("Unknown source: " + id);
                    }
                    return generateSourceMetadata(source);
                },
                (sourceIds instanceof Collection) ? Lists.mutable.ofInitialCapacity(((Collection<?>) sourceIds).size()) : Lists.mutable.empty());
    }

    public MutableList<SourceMetadata> generateSourceMetadata(SourceRegistry sourceRegistry, String... sourceIds)
    {
        return generateSourceMetadata(sourceRegistry, ArrayAdapter.adapt(sourceIds));
    }

    public MutableList<SourceMetadata> generateAllSourceMetadata(SourceRegistry sourceRegistry)
    {
        return sourceRegistry.getSources().collect(this::generateSourceMetadata, Lists.mutable.ofInitialCapacity(sourceRegistry.getSourceCount()));
    }
}
