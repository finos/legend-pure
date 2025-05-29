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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MetadataEager implements Metadata
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataEager.class);

    private final ProcessorSupport processorSupport;
    private final ReferenceIdResolver resolver;
    private final Cache cache;
    private final ThreadLocal<Cache> transaction = new ThreadLocal<>();

    MetadataEager(MapIterable<CoreInstance, ? extends ListIterable<? extends CoreInstance>> classifierInstances, ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
        this.resolver = new ReferenceIdExtensionV1().newResolver(this.processorSupport::package_getByUserPath);
        this.cache = newCache(classifierInstances);
    }

    public MetadataEager(ProcessorSupport processorSupport)
    {
        this(null, processorSupport);
    }

    @Override
    public void startTransaction()
    {
        this.transaction.set(newCache(this.cache));
    }

    @Override
    public void commitTransaction()
    {
        Cache fromTransaction = this.transaction.get();
        if (fromTransaction != null)
        {
            this.cache.clear();
            this.transaction.remove();
        }
    }

    @Override
    public void rollbackTransaction()
    {
        this.transaction.remove();
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        try
        {
            return getCache().getById(enumerationName + "." + M3Properties.values + "['" + enumName + "']");
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Enum " + enumName + " of Enumeration " + enumerationName + " does not exist", e);
        }
    }

    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        // for backward compatibility
        String resolvedId = id.startsWith("Root::") ? id.substring(6) : id;
        try
        {
            return getCache().getById(resolvedId);
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Element " + id + " of type " + classifier + " does not exist", e);
        }
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        ImmutableList<CoreInstance> instances = getCache().getClassifierInstances(classifier);
        if (instances.isEmpty())
        {
            return Maps.immutable.empty();
        }

        IdBuilder idBuilder = IdBuilder.newIdBuilder(this.processorSupport, true);
        MutableMap<String, CoreInstance> byId = Maps.mutable.ofInitialCapacity(instances.size());
        instances.forEach(inst -> byId.put(idBuilder.buildId(inst), inst));
        return byId;
    }

    @Override
    public RichIterable<CoreInstance> getClassifierInstances(String classifier)
    {
        return getCache().getClassifierInstances(classifier);
    }

    public void addInstances(RichIterable<? extends CoreInstance> instances)
    {
        getCache().addInstances(instances);
    }

    public void invalidateCoreInstances(RichIterable<? extends CoreInstance> instances)
    {
        getCache().invalidateInstances(instances);
    }

    @Deprecated
    public int getSize()
    {
        return getCache().idCache.size();
    }

    private Cache getCache()
    {
        Cache fromTransaction = this.transaction.get();
        return (fromTransaction == null) ? this.cache : fromTransaction;
    }

    private Cache newCache()
    {
        return new Cache(ConcurrentHashMap.newMap(), ConcurrentHashMap.newMap());
    }

    private Cache newCache(Cache source)
    {
        return new Cache(ConcurrentHashMap.newMap(source.idCache), ConcurrentHashMap.newMap(source.classifierCache));
    }

    private Cache newCache(MapIterable<CoreInstance, ? extends ListIterable<? extends CoreInstance>> classifierInstances)
    {
        if ((classifierInstances == null) || classifierInstances.isEmpty())
        {
            return newCache();
        }

        LOGGER.debug("Initializing cache with {} classifiers", classifierInstances.size());
        ConcurrentMutableMap<String, ImmutableList<CoreInstance>> classifierCache = ConcurrentHashMap.newMap(classifierInstances.size());
        classifierInstances.forEachKeyValue((classifier, instances) ->
        {
            String classifierId = PackageableElement.getUserPathForPackageableElement(classifier);
            if (instances.notEmpty())
            {
                LOGGER.debug("Classifier {} has {} instances in cache", classifierId, instances.size());
            }
            classifierCache.put(classifierId, Lists.immutable.withAll(instances));
        });
        return new Cache(ConcurrentHashMap.newMap(), classifierCache);
    }

    private class Cache
    {
        private final ConcurrentMutableMap<String, CoreInstance> idCache;
        private final ConcurrentMutableMap<String, ImmutableList<CoreInstance>> classifierCache;

        private Cache(ConcurrentMutableMap<String, CoreInstance> idCache, ConcurrentMutableMap<String, ImmutableList<CoreInstance>> classifierCache)
        {
            this.idCache = idCache;
            this.classifierCache = classifierCache;
        }

        CoreInstance getById(String id)
        {
            return this.idCache.getIfAbsentPutWithKey(id, this::resolveId);
        }

        ImmutableList<CoreInstance> getClassifierInstances(String classifierPath)
        {
            return this.classifierCache.getIfAbsentPutWithKey(classifierPath, this::computeClassifierInstances);
        }

        void clear()
        {
            clearIdCache();
            clearClassifierCache();
        }

        void clearIdCache()
        {
            this.idCache.clear();
        }

        void clearClassifierCache()
        {
            this.classifierCache.clear();
        }

        void clearClassifierCache(String classifierId)
        {
            this.classifierCache.remove(classifierId);
        }

        void addInstances(Iterable<? extends CoreInstance> newInstances)
        {
            MutableMap<CoreInstance, MutableList<CoreInstance>> byClassifier = Maps.mutable.empty();
            newInstances.forEach(i -> byClassifier.getIfAbsentPut(getClassifier(i), Lists.mutable::empty).add(i));
            byClassifier.forEachKeyValue((classifier, classifierInstances) ->
            {
                String classifierId = PackageableElement.getUserPathForPackageableElement(classifier);
                ImmutableList<CoreInstance> cachedInstances = this.classifierCache.get(classifierId);
                if (cachedInstances != null)
                {
                    this.classifierCache.put(classifierId, cachedInstances.newWithAll(classifierInstances));
                }
            });
        }

        void invalidateInstances(RichIterable<? extends CoreInstance> toRemove)
        {
            clearIdCache();
            toRemove.collect(this::getClassifier, Sets.mutable.empty()).forEach(classifier ->
            {
                String classifierId = PackageableElement.getUserPathForPackageableElement(classifier);
                clearClassifierCache(classifierId);
            });
        }

        private CoreInstance resolveId(String id)
        {
            return MetadataEager.this.resolver.resolveReference(id);
        }

        private ImmutableList<CoreInstance> computeClassifierInstances(String classifierPath)
        {
            long start = System.nanoTime();
            LOGGER.debug("Computing instances for classifier {}", classifierPath);
            try
            {
                CoreInstance classifierInstance;
                try
                {
                    classifierInstance = getById(classifierPath);
                }
                catch (Exception e)
                {
                    // unknown classifier
                    return Lists.immutable.empty();
                }

                return GraphNodeIterable.builder()
                        .withStartingNodes(GraphTools.getTopLevels(MetadataEager.this.processorSupport))
                        .withNodeFilter(node -> GraphWalkFilterResult.cont(classifierInstance == getClassifier(node)))
                        .build()
                        .toList()
                        .toImmutable();
            }
            catch (Exception e)
            {
                LOGGER.error("Error computing instances for classifier {}", classifierPath, e);
                throw e;
            }
            finally
            {
                long end = System.nanoTime();
                LOGGER.debug("Finished computing instances for classifier {} ({}s)", classifierPath, (end - start) / 1_000_000_000.0);
            }
        }

        private CoreInstance getClassifier(CoreInstance instance)
        {
            return MetadataEager.this.processorSupport.getClassifier(instance);
        }
    }
}
