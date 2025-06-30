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

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.VirtualPackageMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public abstract class AbstractLazyVirtualPackage extends AbstractLazyCoreInstance
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLazyVirtualPackage.class);

    protected final String path;
    private volatile Init init;

    protected AbstractLazyVirtualPackage(ModelRepository repository, int internalSyntheticId, VirtualPackageMetadata metadata, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        super(repository, internalSyntheticId, getNameFromPath(metadata.getPath()), null, CompileStateSet.PROCESSED_VALIDATED.toBitSet(), metadata.getClassifierPath(), referenceIds);
        this.path = metadata.getPath();
        this.init = new Init(elementBuilder, referenceIds, backRefProviderDeserializer);
    }

    protected AbstractLazyVirtualPackage(AbstractLazyVirtualPackage source)
    {
        super(source);
        source.initialize();
        this.path = source.path;
        this.init = null;
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
                        BackReferenceProvider backReferenceProvider = local.backRefProviderDeserializer.get();
                        initialize(backReferenceProvider.getBackReferences(this.path), local.referenceIds, local.elementBuilder);
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

    protected abstract void initialize(ListIterable<? extends BackReference> backReferences, ReferenceIdResolvers referenceIds, ElementBuilder elementBuilder);

    private static class Init
    {
        private final ElementBuilder elementBuilder;
        private final ReferenceIdResolvers referenceIds;
        private final Supplier<? extends BackReferenceProvider> backRefProviderDeserializer;

        private Init(ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
        {
            this.elementBuilder = elementBuilder;
            this.referenceIds = referenceIds;
            this.backRefProviderDeserializer = backRefProviderDeserializer;
        }
    }
}
