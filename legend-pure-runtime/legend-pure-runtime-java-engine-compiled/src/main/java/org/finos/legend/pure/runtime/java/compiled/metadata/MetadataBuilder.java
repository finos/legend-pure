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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Creates the metadata index
 */
public class MetadataBuilder
{
    public static final CompileState SERIALIZED = CompileState.COMPILE_EVENT_EXTRA_STATE_1;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataBuilder.class);
    private static final ImmutableList<String> GET_ALL_FUNCTIONS = Lists.immutable.with(
            "meta::pure::functions::collection::getAll_Class_1__T_MANY_",
            "meta::pure::functions::collection::getAllVersions_Class_1__T_MANY_",
            "meta::pure::functions::collection::getAll_Class_1__Date_1__T_MANY_",
            "meta::pure::functions::collection::getAll_Class_1__Date_1__Date_1__T_MANY_",
            "meta::pure::functions::collection::getAllVersionsInRange_Class_1__Date_1__Date_1__T_MANY_"
    );

    private MetadataBuilder()
    {
    }

    @Deprecated
    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        return indexAll(startingNodes, processorSupport);
    }

    @Deprecated
    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, IdBuilder idBuilder, ProcessorSupport processorSupport)
    {
        return indexNew(metadataEager, startingNodes, processorSupport);
    }

    public static MetadataEager indexAll(Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        long start = System.nanoTime();
        LOGGER.debug("Start indexing all");
        try
        {
            MutableMap<CoreInstance, MutableList<CoreInstance>> classifierCache = Maps.mutable.empty();
            forEachGetAllClassifier(processorSupport, c -> classifierCache.getIfAbsentPut(c, Lists.mutable::empty));
            LOGGER.debug("Found {} classifiers to cache instances", classifierCache.size());
            MutableSet<CoreInstance> visited = Sets.mutable.empty();
            forEachInstance(startingNodes, processorSupport, visited::add, node ->
            {
                MutableList<CoreInstance> classifierInstances = classifierCache.get(node.getClassifier());
                if (classifierInstances != null)
                {
                    classifierInstances.add(node);
                }
            });
            return new MetadataEager(classifierCache, processorSupport);
        }
        catch (Exception e)
        {
            LOGGER.error("Error indexing all", e);
            throw e;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished indexing all ({}s)", (end - start) / 1_000_000_000.0);
        }
    }

    public static MetadataEager indexNew(MetadataEager metadataEager, Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport)
    {
        long start = System.nanoTime();
        LOGGER.debug("Start indexing new");
        try
        {
            MutableList<CoreInstance> newInstances = Lists.mutable.empty();
            forEachInstance(startingNodes, processorSupport, node -> !node.hasCompileState(SERIALIZED), newInstances::add);
            metadataEager.addInstances(newInstances);
            return metadataEager;
        }
        catch (Exception e)
        {
            LOGGER.error("Error indexing new", e);
            throw e;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished indexing new ({}s)", (end - start) / 1_000_000_000.0);
        }
    }

    private static void forEachInstance(Iterable<? extends CoreInstance> startingNodes, ProcessorSupport processorSupport, Predicate<? super CoreInstance> shouldVisit, Consumer<? super CoreInstance> consumer)
    {
        Deque<CoreInstance> deque = Iterate.addAllTo(startingNodes, new ArrayDeque<>());
        MutableSet<CoreInstance> primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(processorSupport, Sets.mutable.ofInitialCapacity(PrimitiveUtilities.getPrimitiveTypeNames().size()));
        while (!deque.isEmpty())
        {
            CoreInstance node = deque.pollFirst();
            if (shouldVisit.test(node))
            {
                node.addCompileState(SERIALIZED);
                consumer.accept(node);
                node.getKeys().forEach(key -> Instance.getValueForMetaPropertyToManyResolved(node, key, processorSupport).forEach(value ->
                {
                    if (!primitiveTypes.contains(value.getClassifier()))
                    {
                        deque.add(value);
                    }
                }));
            }
        }
    }

    private static void forEachGetAllClassifier(ProcessorSupport processorSupport, Consumer<? super CoreInstance> consumer)
    {
        GET_ALL_FUNCTIONS.forEach(funcPath ->
        {
            CoreInstance getAll;
            try
            {
                getAll = processorSupport.package_getByUserPath(funcPath);
            }
            catch (Exception e)
            {
                LOGGER.warn("Could not find function: {}", funcPath, e);
                return;
            }
            if (getAll == null)
            {
                LOGGER.warn("Could not find function: {}", funcPath);
            }
            else
            {
                getAll.getValueForMetaPropertyToMany(M3Properties.applications).forEach(app ->
                {
                    CoreInstance classExpr = app.getValueForMetaPropertyToMany(M3Properties.parametersValues).get(0);
                    if (ValueSpecification.isInstanceValue(classExpr, processorSupport))
                    {
                        consumer.accept(Instance.getValueForMetaPropertyToOneResolved(classExpr, M3Properties.values, processorSupport));
                    }
                });
            }
        });
    }
}
