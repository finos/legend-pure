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

import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyConcreteElement;
import org.finos.legend.pure.m3.coreinstance.lazy.OneValue;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReferenceProvider;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ConcreteElementMetadata;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolvers;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceMutableState;
import org.finos.legend.pure.m4.coreinstance.CoreInstanceStandardPrinter;
import org.finos.legend.pure.m4.coreinstance.CoreInstanceWithStandardPrinting;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.function.Supplier;

public abstract class AbstractM3GeneratedLazyConcreteElement extends AbstractLazyConcreteElement implements CoreInstanceWithStandardPrinting
{
    protected AbstractM3GeneratedLazyConcreteElement(ModelRepository repository, ConcreteElementMetadata metadata, ElementBuilder elementBuilder, ReferenceIdResolvers referenceIds, PrimitiveValueResolver primitiveValueResolver, Supplier<? extends DeserializedConcreteElement> deserializer, Supplier<? extends BackReferenceProvider> backRefProviderDeserializer)
    {
        super(repository, repository.nextId(), metadata, elementBuilder, referenceIds, primitiveValueResolver, deserializer, backRefProviderDeserializer);
    }

    protected AbstractM3GeneratedLazyConcreteElement(AbstractM3GeneratedLazyConcreteElement source)
    {
        super(source);
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {
        CoreInstanceStandardPrinter.printFull(appendable, this, tab);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        CoreInstanceStandardPrinter.print(appendable, this, tab, max);
    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        CoreInstanceStandardPrinter.printWithoutDebug(appendable, this, tab, max);
    }

    protected <_T> _T mandatory(_T value, String propertyName)
    {
        if (value == null)
        {
            throw new PureCompilationException(getSourceInformation(), "'" + propertyName + "' is a mandatory property");
        }
        return value;
    }

    protected static OneValue<StringCoreInstance> computeName(String name, PrimitiveValueResolver primitiveValueResolver)
    {
        return OneValue.fromSupplier(() -> (StringCoreInstance) primitiveValueResolver.resolveString(name));
    }

    protected abstract static class _AbstractState extends AbstractCoreInstanceMutableState
    {
        protected _AbstractState()
        {
            // compile state bit set defaults to 0
        }

        protected _AbstractState(int compileStateBitSet)
        {
            setCompileStateBitSet(compileStateBitSet);
        }

        protected _AbstractState(InstanceData instanceData)
        {
            this(instanceData.getCompileStateBitSet());
        }

        protected _AbstractState(_AbstractState source)
        {
            this(source.getCompileStateBitSet());
        }
    }
}
