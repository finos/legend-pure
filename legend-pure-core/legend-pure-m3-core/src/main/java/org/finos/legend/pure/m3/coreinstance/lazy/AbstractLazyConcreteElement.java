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

package org.finos.legend.pure.m3.coreinstance.lazy;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class AbstractLazyConcreteElement extends AbstractLazyCoreInstance
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLazyConcreteElement.class);

    protected final String path;
    private volatile Init init;

    protected AbstractLazyConcreteElement(ModelRepository repository, int internalSyntheticId, ConcreteElementMetadata metadata, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        super(repository, internalSyntheticId, getNameFromPath(metadata.getPath()), metadata.getSourceInformation(), 0, metadata.getClassifierPath(), referenceIds.packagePathResolver());
        this.path = metadata.getPath();
        this.init = new Init(elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);
    }

    protected AbstractLazyConcreteElement(AbstractLazyConcreteElement source)
    {
        super(source);
        source.initialize();
        this.path = source.path;
        this.init = null;
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        initialize();
        return super.hasCompileState(state);
    }

    @Override
    public void addCompileState(CompileState state)
    {
        initialize();
        super.addCompileState(state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        initialize();
        super.removeCompileState(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        initialize();
        return super.getCompileStates();
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        initialize();
        super.setCompileStatesFrom(states);
    }

    @Override
    public boolean isInitialized()
    {
        return this.init == null;
    }

    protected void initialize()
    {
        Init local = this.init;
        if (local != null)
        {
            synchronized (local)
            {
                if (this.init != null)
                {
                    long start = System.nanoTime();
                    LOGGER.debug("Initializing {}", this.path);
                    try
                    {
                        DeserializedConcreteElement deserialized = local.deserializer.get();
                        BackReferenceProvider backRefProvider = local.backRefProviderDeserializer.get();
                        ReferenceIdResolver refIdResolver = local.referenceIds.resolver(deserialized.getReferenceIdVersion());
                        IntFunction<CoreInstance> intIdResolver = newInternalIdResolver(deserialized, backRefProvider, local.elementBuilder, refIdResolver);

                        InstanceData concreteElementData = deserialized.getConcreteElementData();
                        setCompileStatesFrom(concreteElementData.getCompileStateBitSet());
                        initialize(concreteElementData, backRefProvider.getBackReferences(concreteElementData.getReferenceId()), refIdResolver, intIdResolver, local.primitiveValueResolver, local.elementBuilder);
                        this.init = null;
                    }
                    catch (Throwable t)
                    {
                        LOGGER.error("Error initializing {}", this.path, t);
                        throw t;
                    }
                    finally
                    {
                        long end = System.nanoTime();
                        LOGGER.debug("Finished initializing {} in {}s", this.path, (end - start) / 1_000_000_000.0);
                    }
                }
            }
        }
    }

    protected IntFunction<CoreInstance> newInternalIdResolver(DeserializedConcreteElement deserialized, BackReferenceProvider backRefProvider, ElementBuilder elementBuilder, ReferenceIdResolver referenceIdResolver)
    {
        return new InternalIdResolver(this, deserialized, backRefProvider, elementBuilder, referenceIdResolver);
    }

    protected abstract void initialize(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder);

    private static class Init
    {
        private final ElementBuilder elementBuilder;
        private final ReferenceIdResolvers referenceIds;
        private final PrimitiveValueResolver primitiveValueResolver;
        private final Supplier<? extends DeserializedConcreteElement> deserializer;
        private final Supplier<? extends BackReferenceProvider> backRefProviderDeserializer;

        private Init(ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.elementBuilder = elementBuilder;
            this.referenceIds = referenceIds;
            this.primitiveValueResolver = primitiveValueResolver;
            this.deserializer = deserializer;
            this.backRefProviderDeserializer = backRefProviderDeserializer;
        }
    }

    protected static class InternalIdResolver implements IntFunction<CoreInstance>
    {
        private final CoreInstance[] index;

        protected InternalIdResolver(CoreInstance concreteElement, DeserializedConcreteElement deserialized, BackReferenceProvider backRefProvider, ElementBuilder elementBuilder, ReferenceIdResolver referenceIdResolver)
        {
            ImmutableList<InstanceData> internalInstances = deserialized.getInstanceData();
            LOGGER.debug("Creating {} internal instances for {}", internalInstances.size(), deserialized.getPath());
            this.index = new CoreInstance[internalInstances.size()];
            this.index[0] = concreteElement;
            if (internalInstances.size() > 1)
            {
                internalInstances.forEachWithIndex(1, internalInstances.size() - 1,
                        (d, i) -> this.index[i] = elementBuilder.buildComponentInstance(d, backRefProvider.getBackReferences(d.getReferenceId()), referenceIdResolver, this));
            }
        }

        @Override
        public CoreInstance apply(int id)
        {
            try
            {
                return this.index[id];
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new IllegalArgumentException("Invalid internal id: " + id + " (valid ids are 0-" + (this.index.length - 1) + ")");
            }
        }
    }
}
