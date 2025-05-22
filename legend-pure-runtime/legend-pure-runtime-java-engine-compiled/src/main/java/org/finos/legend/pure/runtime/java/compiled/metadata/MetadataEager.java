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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;

public final class MetadataEager implements Metadata
{
    private final ProcessorSupport processorSupport;
    private final ReferenceIdResolver resolver;
    private final ConcurrentMutableMap<String, CoreInstance> cache = ConcurrentHashMap.newMap();
    private final ThreadLocal<ConcurrentMutableMap<String, CoreInstance>> transaction = new ThreadLocal<>();

    public MetadataEager(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
        this.resolver = new ReferenceIdExtensionV1().newResolver(this.processorSupport::package_getByUserPath);
    }

    @Override
    public void startTransaction()
    {
        this.transaction.set(ConcurrentHashMap.newMap());
    }

    @Override
    public void commitTransaction()
    {
        ConcurrentMutableMap<String, CoreInstance> fromTransaction = this.transaction.get();
        if (fromTransaction != null)
        {
            this.transaction.remove();
            this.cache.clear();
        }
    }

    @Override
    public void rollbackTransaction()
    {
        this.transaction.remove();
    }

    public void clear()
    {
        ConcurrentMutableMap<String, CoreInstance> transactionCache = this.transaction.get();
        if (transactionCache == null)
        {
            this.cache.clear();
        }
        else
        {
            this.transaction.set(ConcurrentHashMap.newMap());
        }
    }

    public void reset()
    {
        this.transaction.remove();
        this.cache.clear();
    }

    @Deprecated
    public void addChild(String packageClassifier, String packageId, String objectClassifier, String instanceId)
    {
        // nothing to do
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        return getFromCache(enumerationName + "." + M3Properties.values + "['" + enumName + "']");
    }

    @Deprecated
    public void invalidateCoreInstances(RichIterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        clear();
    }

    @Deprecated
    public void add(String classifier, String id, CoreInstance instance)
    {
        // nothing to do
    }


    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        // for backward compatibility
        if (id.startsWith("Root::"))
        {
            id = id.substring(6);
        }

        try
        {
            return getFromCache(id);
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Element " + id + " of type " + classifier + " does not exist", e);
        }
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        IdBuilder idBuilder = IdBuilder.newIdBuilder(this.processorSupport, true);
        MutableMap<String, CoreInstance> instances = Maps.mutable.empty();
        getClassifierInstances(classifier).forEach(inst -> instances.put(idBuilder.buildId(inst), inst));
        return instances;
    }

    @Override
    public RichIterable<CoreInstance> getClassifierInstances(String classifier)
    {
        CoreInstance classifierInstance;
        try
        {
            classifierInstance = getFromCache(classifier);
        }
        catch (Exception e)
        {
            // unknown classifier
            return Lists.immutable.empty();
        }

        return GraphNodeIterable.builder()
                .withStartingNodes(GraphTools.getTopLevels(this.processorSupport))
                .withNodeFilter(node -> GraphWalkFilterResult.cont(classifierInstance == this.processorSupport.getClassifier(node)))
                .build();
    }

    @Deprecated
    public int getSize()
    {
        return GraphNodeIterable.builder()
                .withStartingNodes(GraphTools.getTopLevels(this.processorSupport))
                .build()
                .size();
    }

    private CoreInstance getFromCache(String id)
    {
        return getCache().getIfAbsentPutWithKey(id, this::resolveId);
    }

    private ConcurrentMutableMap<String, CoreInstance> getCache()
    {
        ConcurrentMutableMap<String, CoreInstance> fromTransaction = this.transaction.get();
        return (fromTransaction == null) ? this.cache : fromTransaction;
    }

    private CoreInstance resolveId(String id)
    {
        return this.resolver.resolveReference(id);
    }
}
