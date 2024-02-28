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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.Stacks;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;

/**
 * Creates the metadata index
 */
public class MetadataBuilder
{
    public static final CompileState SERIALIZED = CompileState.COMPILE_EVENT_EXTRA_STATE_1;

    private MetadataBuilder()
    {
    }

    @Deprecated
    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        return indexAll(startingNodes, IdBuilder.newIdBuilder(processorSupport), processorSupport);
    }

    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        MetadataEager metadataEager = new MetadataEager();
        PrivateSetSearchStateWithCompileStateMarking state = new PrivateSetSearchStateWithCompileStateMarking(Stacks.mutable.withAll(startingNodes), processorSupport);
        indexNodes(state, metadataEager, idBuilder, processorSupport);
        return metadataEager;
    }

    @Deprecated
    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        return indexNew(metadataEager, startingNodes, IdBuilder.newIdBuilder(processorSupport), processorSupport);
    }

    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        CompiledStateSearchState state = new CompiledStateSearchState(Stacks.mutable.withAll(startingNodes), processorSupport);
        return indexNodes(state, metadataEager, idBuilder, processorSupport);
    }

    private static MetadataEager indexNodes(SearchState state, MetadataEager metadataEager, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        while (state.hasNodes())
        {
            CoreInstance instance = state.nextNode();
            if (state.shouldVisit(instance))
            {
                state.noteVisited(instance);

                String id = idBuilder.buildId(instance);
                CoreInstance classifier = instance.getClassifier();

                for (String key : instance.getKeys())
                {
                    for (CoreInstance value : Instance.getValueForMetaPropertyToManyResolved(instance, key, processorSupport))
                    {
                        if (!state.isPrimitiveType(value.getClassifier()))
                        {
                            state.addNode(value);
                        }
                    }
                }

                if (classifier instanceof Enumeration)
                {
                    metadataEager.add(state.getClassifierId(classifier), instance.getName(), instance);
                }
                else
                {
                    metadataEager.add(state.getClassifierId(classifier), id, instance);
                }
            }
        }

        return metadataEager;
    }

    private static class ClassifierCaches
    {
        private final SetIterable<CoreInstance> primitiveTypes;
        private final MutableMap<CoreInstance, String> classifierIdCache = Maps.mutable.empty();

        private ClassifierCaches(ProcessorSupport processorSupport)
        {
            this.primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        }

        boolean isPrimitiveType(CoreInstance classifier)
        {
            return this.primitiveTypes.contains(classifier);
        }

        String getClassifierId(CoreInstance classifier)
        {
            return this.classifierIdCache.getIfAbsentPutWithKey(classifier, ClassifierCaches::newClassifierId);
        }

        private static String newClassifierId(CoreInstance classifier)
        {
            return MetadataJavaPaths.buildMetadataKeyFromType(classifier).intern();
        }
    }

    private abstract static class SearchState extends ClassifierCaches
    {
        private final MutableStack<CoreInstance> stack;

        private SearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(processorSupport);
            this.stack = stack;
        }

        boolean hasNodes()
        {
            return this.stack.notEmpty();
        }

        CoreInstance nextNode()
        {
            return this.stack.pop();
        }

        void addNode(CoreInstance node)
        {
            this.stack.push(node);
        }

        boolean shouldVisit(CoreInstance node)
        {
            return !hasVisited(node) && !isPrimitiveType(node.getClassifier());
        }

        abstract boolean hasVisited(CoreInstance node);

        abstract void noteVisited(CoreInstance node);
    }

    private static class CompiledStateSearchState extends SearchState
    {
        private CompiledStateSearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        boolean hasVisited(CoreInstance node)
        {
            return node.hasCompileState(SERIALIZED);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            node.addCompileState(SERIALIZED);
        }
    }

    private static class PrivateSetSearchState extends SearchState
    {
        private final MutableSet<CoreInstance> visited = Sets.mutable.with();

        private PrivateSetSearchState(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        boolean hasVisited(CoreInstance node)
        {
            return this.visited.contains(node);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            this.visited.add(node);
        }
    }

    private static class PrivateSetSearchStateWithCompileStateMarking extends PrivateSetSearchState
    {
        private PrivateSetSearchStateWithCompileStateMarking(MutableStack<CoreInstance> stack, ProcessorSupport processorSupport)
        {
            super(stack, processorSupport);
        }

        @Override
        void noteVisited(CoreInstance node)
        {
            super.noteVisited(node);
            node.addCompileState(SERIALIZED);
        }
    }
}
