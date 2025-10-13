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

package org.finos.legend.pure.m3.coreinstance.lazy.generator;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.lazy.ModelRepositoryPrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementLoader;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class M3GeneratedLazyElementBuilder implements ElementBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(M3GeneratedLazyElementBuilder.class);

    private final ClassLoader classLoader;
    private final ModelRepository repository;
    private final PrimitiveValueResolver primitiveValueResolver;

    private final ConcurrentMutableMap<String, Constructor<? extends CoreInstance>> concreteElementConstructors = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, Constructor<? extends CoreInstance>> componentElementConstructors = ConcurrentHashMap.newMap();
    private final AtomicReference<Constructor<? extends CoreInstance>> virtualPackageConstructor = new AtomicReference<>();
    private final AtomicReference<Constructor<? extends CoreInstance>> enumConstructor = new AtomicReference<>();

    private M3GeneratedLazyElementBuilder(ClassLoader classLoader, ModelRepository repository)
    {
        this.classLoader = Objects.requireNonNull(classLoader);
        this.repository = Objects.requireNonNull(repository);
        this.primitiveValueResolver = new ModelRepositoryPrimitiveValueResolver(this.repository);
    }

    @Override
    public void initialize(ElementLoader elementLoader)
    {
        // Load the top level elements eagerly to avoid bootstrapping issues (especially with classifiers for
        // primitive values)
        LOGGER.debug("Initializing element builder");
        PrimitiveUtilities.getPrimitiveTypeNames().forEach(n -> this.repository.addTopLevel(elementLoader.loadElementStrict(n)));
        this.repository.addTopLevel(elementLoader.loadElementStrict(M3Paths.Root));
        this.repository.addTopLevel(elementLoader.loadElementStrict(M3Paths.Package));
    }

    @Override
    public CoreInstance buildVirtualPackage(VirtualPackageMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        LOGGER.debug("Building virtual package {}", metadata.getPath());
        try
        {
            Constructor<? extends CoreInstance> constructor = getVirtualPackageConstructorFromCache();
            return constructor.newInstance(this.repository, metadata, index, this, referenceIds, this.primitiveValueResolver, backRefProviderDeserializer);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error building virtual package " + metadata.getPath(), e);
        }
    }

    @Override
    public CoreInstance buildConcreteElement(ConcreteElementMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        LOGGER.debug("Building concrete element {} of type {}", metadata.getPath(), metadata.getClassifierPath());
        try
        {
            Constructor<? extends CoreInstance> constructor = getConcreteElementConstructorFromCache(metadata.getClassifierPath());
            return constructor.newInstance(this.repository, metadata, index, this, referenceIds, this.primitiveValueResolver, deserializer, backRefProviderDeserializer);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error building concrete element ").append(metadata.getPath()).append(" of type ").append(metadata.getClassifierPath());
            SourceInformation sourceInfo = metadata.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(" (")).append(')');
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    @Override
    public CoreInstance buildComponentInstance(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver)
    {
        LOGGER.debug("Building component instance of type {} with id {}", instanceData.getClassifierPath(), instanceData.getReferenceId());
        try
        {
            Constructor<? extends CoreInstance> constructor = isEnum(instanceData) ? getEnumConstructorFromCache() : getComponentElementConstructorFromCache(instanceData.getClassifierPath());
            return constructor.newInstance(this.repository, instanceData, backReferences, referenceIdResolver, internalIdResolver, this.primitiveValueResolver, this);
        }
        catch (Exception e)
        {
            StringBuilder builder = new StringBuilder("Error building component instance of type ").append(instanceData.getClassifierPath());
            String refId = instanceData.getReferenceId();
            if (refId != null)
            {
                builder.append(" with id ").append(refId);
            }
            SourceInformation sourceInfo = instanceData.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(" (")).append(')');
            }
            throw new RuntimeException(builder.toString(), e);
        }
    }

    private boolean isEnum(InstanceData instanceData)
    {
        return instanceData.getPropertyValues().anySatisfy(pv -> M3Paths.Enum.equals(pv.getPropertySourceType()));
    }

    private Constructor<? extends CoreInstance> getVirtualPackageConstructorFromCache()
    {
        Constructor<? extends CoreInstance> local = this.virtualPackageConstructor.get();
        if (local == null)
        {
            local = getVirtualPackageConstructor();
            if (!this.virtualPackageConstructor.compareAndSet(null, local))
            {
                return this.virtualPackageConstructor.get();
            }
        }
        return local;
    }

    private Constructor<? extends CoreInstance> getVirtualPackageConstructor()
    {
        try
        {
            String javaClassName = M3LazyCoreInstanceGenerator.buildLazyVirtualPackageClassReference();
            Class<? extends CoreInstance> javaClass = loadJavaClass(javaClassName);
            return javaClass.getConstructor(ModelRepository.class, VirtualPackageMetadata.class, MetadataIndex.class, ElementBuilder.class, ReferenceIdResolvers.class, PrimitiveValueResolver.class, Supplier.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting virtual package constructor", e);
        }
    }

    private Constructor<? extends CoreInstance> getConcreteElementConstructorFromCache(String classifierPath)
    {
        return this.concreteElementConstructors.getIfAbsentPutWithKey(classifierPath, this::getConcreteElementConstructor);
    }

    private Constructor<? extends CoreInstance> getConcreteElementConstructor(String classifierPath)
    {
        try
        {
            String javaClassName = M3LazyCoreInstanceGenerator.buildLazyConcreteElementClassReferenceFromUserPath(classifierPath);
            Class<? extends CoreInstance> javaClass = loadJavaClass(javaClassName);
            return javaClass.getConstructor(ModelRepository.class, ConcreteElementMetadata.class, MetadataIndex.class, ElementBuilder.class, ReferenceIdResolvers.class, PrimitiveValueResolver.class, Supplier.class, Supplier.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting concrete element constructor for " + classifierPath, e);
        }
    }

    private Constructor<? extends CoreInstance> getComponentElementConstructorFromCache(String classifierPath)
    {
        return this.componentElementConstructors.getIfAbsentPutWithKey(classifierPath, this::getComponentElementConstructor);
    }

    private Constructor<? extends CoreInstance> getComponentElementConstructor(String classifierPath)
    {
        try
        {
            String javaClassName = M3LazyCoreInstanceGenerator.buildLazyComponentInstanceClassReferenceFromUserPath(classifierPath);
            Class<? extends CoreInstance> javaClass = loadJavaClass(javaClassName);
            return getComponentElementConstructor(javaClass);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting component element constructor for " + classifierPath, e);
        }
    }

    private <T> Constructor<T> getComponentElementConstructor(Class<T> javaClass) throws NoSuchMethodException
    {
        return javaClass.getConstructor(ModelRepository.class, InstanceData.class, ListIterable.class, ReferenceIdResolver.class, IntFunction.class, PrimitiveValueResolver.class, ElementBuilder.class);
    }

    private Constructor<? extends CoreInstance> getEnumConstructorFromCache()
    {
        Constructor<? extends CoreInstance> local = this.enumConstructor.get();
        if (local == null)
        {
            local = getEnumConstructor();
            if (!this.enumConstructor.compareAndSet(null, local))
            {
                return this.enumConstructor.get();
            }
        }
        return local;
    }

    private Constructor<? extends CoreInstance> getEnumConstructor()
    {
        try
        {
            String javaClassName = M3LazyCoreInstanceGenerator.buildLazyEnumClassReference();
            Class<? extends CoreInstance> javaClass = loadJavaClass(javaClassName);
            return getComponentElementConstructor(javaClass);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error getting component Enum constructor", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends CoreInstance> loadJavaClass(String javaClassName) throws ClassNotFoundException
    {
        return (Class<? extends CoreInstance>) this.classLoader.loadClass(javaClassName);
    }

    public static M3GeneratedLazyElementBuilder newElementBuilder(ClassLoader classLoader, ModelRepository repository)
    {
        return new M3GeneratedLazyElementBuilder(classLoader, repository);
    }
}
