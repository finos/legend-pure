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

import java.util.function.BiConsumer;
import java.util.function.Consumer;

class ElementIndex
{
    private final MapIterable<String, ConcreteElementMetadata> pathIndex;
    private final MapIterable<String, ImmutableList<ConcreteElementMetadata>> sourceIndex;
    private final MapIterable<String, ImmutableList<ConcreteElementMetadata>> classifierIndex;

    private ElementIndex(MapIterable<String, ConcreteElementMetadata> pathIndex, MapIterable<String, ImmutableList<ConcreteElementMetadata>> sourceIndex, MapIterable<String, ImmutableList<ConcreteElementMetadata>> classifierIndex)
    {
        this.pathIndex = pathIndex;
        this.sourceIndex = sourceIndex;
        this.classifierIndex = classifierIndex;
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
        return this.sourceIndex.containsKey(sourceId);
    }

    RichIterable<String> getAllSources()
    {
        return this.sourceIndex.keysView();
    }

    ImmutableList<ConcreteElementMetadata> getSourceElements(String sourceId)
    {
        return this.sourceIndex.get(sourceId);
    }

    void forEachSourceWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        this.sourceIndex.forEachKeyValue(consumer::accept);
    }

    // Elements by classifier

    boolean hasClassifier(String path)
    {
        return this.classifierIndex.containsKey(path);
    }

    RichIterable<String> getAllClassifiers()
    {
        return this.classifierIndex.keysView();
    }

    ImmutableList<ConcreteElementMetadata> getClassifierElements(String classifierPath)
    {
        return this.classifierIndex.get(classifierPath);
    }

    void forEachClassifierWithElements(BiConsumer<? super String, ? super ImmutableList<ConcreteElementMetadata>> consumer)
    {
        this.classifierIndex.forEachKeyValue(consumer::accept);
    }

    static ElementIndex buildIndex(ModuleIndex modules)
    {
        // Build initial indexes
        MutableMap<String, ConcreteElementMetadata> pathIndex = Maps.mutable.empty();
        MutableMap<String, MutableList<ConcreteElementMetadata>> initialSourceIndex = Maps.mutable.empty();
        MutableMap<String, MutableList<ConcreteElementMetadata>> initialClassifierIndex = Maps.mutable.empty();
        modules.forEachModule(module -> module.forEachElement(element ->
        {
            // By path
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

            // By source
            initialSourceIndex.getIfAbsentPut(element.getSourceInformation().getSourceId(), Lists.mutable::empty).add(element);

            // By classifier
            initialClassifierIndex.getIfAbsentPut(element.getClassifierPath(), Lists.mutable::empty).add(element);
        }));

        // Build final source index
        MutableMap<String, ImmutableList<ConcreteElementMetadata>> sourceIndex = Maps.mutable.ofInitialCapacity(initialSourceIndex.size());
        initialSourceIndex.forEachKeyValue((source, elements) ->
        {
            if (elements.size() > 1)
            {
                // We sort by the start position: we can do this because there is no overlap between source info of different elements
                elements.sortThis((e1, e2) ->
                {
                    SourceInformation si1 = e1.getSourceInformation();
                    SourceInformation si2 = e2.getSourceInformation();
                    return SourceInformation.comparePositions(si1.getStartLine(), si1.getStartColumn(), si2.getStartLine(), si2.getStartColumn());
                });
            }
            sourceIndex.put(source, elements.toImmutable());
        });

        // Build final classifier index
        MutableMap<String, ImmutableList<ConcreteElementMetadata>> classifierIndex = Maps.mutable.ofInitialCapacity(initialClassifierIndex.size());
        initialClassifierIndex.forEachKeyValue((classifier, elements) -> classifierIndex.put(classifier, elements.sortThisBy(PackageableElementMetadata::getPath).toImmutable()));

        return new ElementIndex(pathIndex, sourceIndex, classifierIndex);
    }
}
