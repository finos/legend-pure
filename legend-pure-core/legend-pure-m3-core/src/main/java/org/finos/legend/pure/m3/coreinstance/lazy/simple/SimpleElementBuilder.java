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

package org.finos.legend.pure.m3.coreinstance.lazy.simple;

import org.eclipse.collections.api.list.ListIterable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class SimpleElementBuilder implements ElementBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleElementBuilder.class);

    private final ModelRepository repository;
    private final PrimitiveValueResolver primitiveValueResolver;

    private SimpleElementBuilder(ModelRepository repository)
    {
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
        return new SimpleLazyVirtualPackage(this.repository, metadata, index, this, referenceIds, this.primitiveValueResolver, backRefProviderDeserializer);
    }

    @Override
    public CoreInstance buildConcreteElement(ConcreteElementMetadata metadata, MetadataIndex index, ReferenceIdResolvers referenceIds, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        LOGGER.debug("Building concrete element {} of type {}", metadata.getPath(), metadata.getClassifierPath());
        return new SimpleLazyConcreteElement(this.repository, metadata, index, this, referenceIds, this.primitiveValueResolver, deserializer, backRefProviderDeserializer);
    }

    @Override
    public CoreInstance buildComponentInstance(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver)
    {
        LOGGER.debug("Building component instance of type {} with id {}", instanceData.getClassifierPath(), instanceData.getReferenceId());
        return new SimpleLazyComponentInstance(this.repository, instanceData, backReferences, referenceIdResolver, internalIdResolver, this.primitiveValueResolver, this);
    }

    public static SimpleElementBuilder newElementBuilder(ModelRepository repository)
    {
        return new SimpleElementBuilder(repository);
    }
}
