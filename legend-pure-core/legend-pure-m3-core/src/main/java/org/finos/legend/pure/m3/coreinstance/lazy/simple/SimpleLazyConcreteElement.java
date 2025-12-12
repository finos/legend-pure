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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyConcreteElement;
import org.finos.legend.pure.m3.coreinstance.lazy.OneValue;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.lazy.PropertyValue;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.MetadataIndex;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstanceWithStandardPrinting;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

import java.util.function.IntFunction;
import java.util.function.Supplier;

class SimpleLazyConcreteElement extends AbstractLazyConcreteElement implements CoreInstanceWithStandardPrinting
{
    private volatile SimpleLazyCoreInstanceState state;

    SimpleLazyConcreteElement(ModelRepository repository, ConcreteElementMetadata metadata, MetadataIndex index, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        super(repository, repository.nextId(), metadata, elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);
        this.state = new SimpleLazyCoreInstanceState();
        this.state.setKeyValue(M3PropertyPaths.name, OneValue.fromSupplier(() -> (CoreInstance) primitiveValueResolver.resolveString(getName())));
        this.state.setKeyValue(M3PropertyPaths._package, computePackage(metadata.getPath(), referenceIds));
        if (index.hasPackage(this.path))
        {
            this.state.setKeyValue(M3PropertyPaths.children, computePackageChildren(this.path, index, referenceIds));
        }
    }

    private SimpleLazyConcreteElement(SimpleLazyConcreteElement source)
    {
        super(source);
        this.state = source.state.copy();
    }

    @Override
    public RichIterable<String> getKeys()
    {
        initialize();
        return getState(false).getKeys();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        switch (name)
        {
            case M3Properties.name:
            case M3Properties._package:
            case M3Properties.children:
            {
                return getState(false).getRealKey(name);
            }
            default:
            {
                initialize();
                return getState(false).getRealKey(name);
            }
        }
    }

    @Override
    public void removeProperty(String propertyNameKey)
    {
        initialize();
        getState(true).removeProperty(propertyNameKey);
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        getState(true).setKeyValues(key, value);
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        getState(true).addKeyValue(key, value);
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        this.state = (SimpleLazyCoreInstanceState) transaction.getState(this);
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        initialize();
        return getState(false).hasCompileState(state);
    }

    @Override
    public void addCompileState(CompileState state)
    {
        initialize();
        getState(true).addCompileState(state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        initialize();
        getState(true).removeCompileState(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        initialize();
        return getState(false).getCompileStates();
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        initialize();
        getState(true).setCompileStatesFrom(states);
    }

    @Override
    public CoreInstance copy()
    {
        initialize();
        return new SimpleLazyConcreteElement(this);
    }

    @Override
    protected void initialize(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)
    {
        this.state.fromInstanceData(instanceData, backReferences, referenceIdResolver, internalIdResolver, primitiveValueResolver, elementBuilder);
    }

    @Override
    protected PropertyValue<CoreInstance> getPropertyValue(String propertyName, boolean forWrite)
    {
        if (forWrite || !(propertyName.equals(M3Properties.name) || propertyName.equals(M3Properties._package) || propertyName.equals(M3Properties.children)))
        {
            initialize();
        }
        return getState(forWrite).getValue(propertyName);
    }

    private SimpleLazyCoreInstanceState getState(boolean forWrite)
    {
        ModelRepositoryTransaction transaction = this.repository.getTransaction();
        if ((transaction != null) && transaction.isOpen())
        {
            if (forWrite && !transaction.isRegistered(this))
            {
                transaction.registerModified(this, this.state.copy());
            }
            SimpleLazyCoreInstanceState transactionState = (SimpleLazyCoreInstanceState) transaction.getState(this);
            if (transactionState != null)
            {
                return transactionState;
            }
        }
        return this.state;
    }
}
