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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class ElementIndex
{
    private final MapIterable<String, ConcreteElementMetadata> pathIndex;
    private volatile MapIterable<String, ImmutableList<ConcreteElementMetadata>> sourceIndex;
    private volatile MapIterable<String, ImmutableList<ConcreteElementMetadata>> classifierIndex;

    private ElementIndex(MapIterable<String, ConcreteElementMetadata> pathIndex)
    {
        this.pathIndex = pathIndex;
    }

    @Override
    public boolean equals(Object other)
    {
        return (this == other) ||
                ((other instanceof ElementIndex) && this.pathIndex.equals(((ElementIndex) other).pathIndex));
    }

    @Override
    public int hashCode()
    {
        return this.pathIndex.hashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName());
        this.pathIndex.keysView().appendString(builder, " elements=[", ", ", "]>");
        return builder.toString();
    }

    int getElementCount()
    {
        return this.pathIndex.size();
    }

    // Elements by path

    boolean hasElement(String path)
    {
        return this.pathIndex.containsKey(path);
    }

    RichIterable<String> getAllElementPaths()
    {
        return this.pathIndex.keysView();
    }

    ConcreteElementMetadata getElement(String path)
    {
        return this.pathIndex.get(path);
    }

    RichIterable<ConcreteElementMetadata> getAllElements()
    {
        return this.pathIndex.valuesView();
    }

    void forEachElement(Consumer<? super ConcreteElementMetadata> consumer)
    {
        this.pathIndex.forEachValue(consumer::accept);
    }

    // Elements by source

    boolean hasSource(String sourceId)
    {
        return getSourceIndex().containsKey(sourceId);
    }

    RichIterable<String> getAllSources()
    {
        return getSourceIndex().keysView();
    }

    ImmutableList<ConcreteElementMetadata> getSourceElements(String sourceId)
    {
        return getSourceIndex().get(sourceId);
    }

    void forEachSourceWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        getSourceIndex().forEachKeyValue(consumer::accept);
    }

    private MapIterable<String, ImmutableList<ConcreteElementMetadata>> getSourceIndex()
    {
        if (this.sourceIndex == null)
        {
            synchronized (this)
            {
                if (this.sourceIndex == null)
                {
                    this.sourceIndex = buildSourceIndex();
                }
            }
        }
        return this.sourceIndex;
    }

    private MapIterable<String, ImmutableList<ConcreteElementMetadata>> buildSourceIndex()
    {
        MutableMap<String, MutableList<ConcreteElementMetadata>> initialIndex = Maps.mutable.empty();
        this.pathIndex.forEachValue(element -> initialIndex.getIfAbsentPut(element.getSourceInformation().getSourceId(), Lists.mutable::empty).add(element));

        MutableMap<String, ImmutableList<ConcreteElementMetadata>> index = Maps.mutable.ofInitialCapacity(initialIndex.size());
        Comparator<ConcreteElementMetadata> comparator = (e1, e2) ->
        {
            // We sort by the start position: we can do this because there is no overlap between source info of different elements
            SourceInformation si1 = e1.getSourceInformation();
            SourceInformation si2 = e2.getSourceInformation();
            return SourceInformation.comparePositions(si1.getStartLine(), si1.getStartColumn(), si2.getStartLine(), si2.getStartColumn());
        };
        initialIndex.forEachKeyValue((source, sourceElements) -> index.put(source, sourceElements.sortThis(comparator).toImmutable()));
        return index;
    }

    // Elements by classifier

    boolean hasClassifier(String path)
    {
        return getClassifierIndex().containsKey(path);
    }

    RichIterable<String> getAllClassifiers()
    {
        return getClassifierIndex().keysView();
    }

    ImmutableList<ConcreteElementMetadata> getClassifierElements(String classifierPath)
    {
        return getClassifierIndex().get(classifierPath);
    }

    void forEachClassifierWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        getClassifierIndex().forEachKeyValue(consumer::accept);
    }

    private MapIterable<String, ImmutableList<ConcreteElementMetadata>> getClassifierIndex()
    {
        if (this.classifierIndex == null)
        {
            synchronized (this)
            {
                if (this.classifierIndex == null)
                {
                    this.classifierIndex = buildClassifierIndex();
                }
            }
        }
        return this.classifierIndex;
    }

    private MapIterable<String, ImmutableList<ConcreteElementMetadata>> buildClassifierIndex()
    {
        MutableMap<String, MutableList<ConcreteElementMetadata>> initialIndex = Maps.mutable.empty();
        this.pathIndex.forEachValue(element -> initialIndex.getIfAbsentPut(element.getClassifierPath(), Lists.mutable::empty).add(element));

        MutableMap<String, ImmutableList<ConcreteElementMetadata>> index = Maps.mutable.ofInitialCapacity(initialIndex.size());
        Comparator<PackageableElementMetadata> comparator = Comparator.comparing(PackageableElementMetadata::getPath);
        initialIndex.forEachKeyValue((classifier, classifierElements) -> index.put(classifier, classifierElements.sortThis(comparator).toImmutable()));
        return index;
    }

    static ElementIndex buildIndex(ModuleIndex modules)
    {
        // Build element by path index eagerly and other indexes lazily
        MutableMap<String, ConcreteElementMetadata> pathIndex = Maps.mutable.ofInitialCapacity(getTotalElementCount(modules));
        modules.forEachModule(module -> module.forEachElement(element ->
        {
            ConcreteElementMetadata previous = pathIndex.put(element.getPath(), element);
            if ((previous != null) && !element.equals(previous))
            {
                StringBuilder builder = new StringBuilder("Multiple elements with path ").append(element.getPath());
                builder.append(": instance of ").append(previous.getClassifierPath()).append(" at ");
                previous.getSourceInformation().appendMessage(builder);
                builder.append(" and instance of ").append(element.getClassifierPath()).append(" at ");
                element.getSourceInformation().appendMessage(builder);
                throw new IllegalArgumentException(builder.toString());
            }
        }));
        return new ElementIndex(pathIndex);
    }

    private static int getTotalElementCount(ModuleIndex modules)
    {
        int[] count = {0};
        modules.forEachModule(module -> count[0] += module.getElementCount());
        return count[0];
    }
}
