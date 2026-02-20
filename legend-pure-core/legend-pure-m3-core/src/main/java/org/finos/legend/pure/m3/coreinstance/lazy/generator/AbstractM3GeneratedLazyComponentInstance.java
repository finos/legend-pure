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

import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyComponentInstance;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceMutableState;
import org.finos.legend.pure.m4.coreinstance.CoreInstanceStandardPrinter;
import org.finos.legend.pure.m4.coreinstance.CoreInstanceWithStandardPrinting;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public abstract class AbstractM3GeneratedLazyComponentInstance extends AbstractLazyComponentInstance implements CoreInstanceWithStandardPrinting
{
    protected AbstractM3GeneratedLazyComponentInstance(ModelRepository repository, InstanceData instanceData, ReferenceIdResolver referenceIdResolver)
    {
        super(repository, repository.nextId(), resolveName(instanceData, repository), instanceData, referenceIdResolver);
    }

    protected AbstractM3GeneratedLazyComponentInstance(AbstractM3GeneratedLazyComponentInstance source)
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

    private static String resolveName(InstanceData instanceData, ModelRepository repository)
    {
        return (instanceData.getName() == null) ? repository.nextAnonymousInstanceName() : instanceData.getName();
    }

    protected abstract static class _AbstractState extends AbstractCoreInstanceMutableState
    {
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
