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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.finos.legend.pure.m3.coreinstance.lazy.AbstractLazyComponentInstance;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;

import java.util.function.Function;

public abstract class AbstractCompiledLazyComponentInstance extends AbstractLazyComponentInstance implements JavaCompiledCoreInstance
{
    protected AbstractCompiledLazyComponentInstance(ModelRepository repository, InstanceData instanceData, ReferenceIdResolver referenceIdResolver)
    {
        super(repository, (repository == null) ? -1 : repository.nextId(), resolveName(instanceData, repository), instanceData, referenceIdResolver);
    }

    protected AbstractCompiledLazyComponentInstance(InstanceData instanceData, ReferenceIdResolver referenceIdResolver)
    {
        this(null, instanceData, referenceIdResolver);
    }

    protected AbstractCompiledLazyComponentInstance(AbstractCompiledLazyComponentInstance source)
    {
        super(source);
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        ConsoleCompiled.append(SafeAppendable.wrap(appendable).append(tab), this, max);
    }

    @Override
    public String toString()
    {
        return toString(null);
    }

    @Override
    public boolean equals(Object obj)
    {
        return pureEquals(obj);
    }

    @Override
    public int hashCode()
    {
        return pureHashCode();
    }

    @Override
    public boolean pureEquals(Object obj)
    {
        return super.equals(obj);
    }

    @Override
    public int pureHashCode()
    {
        return super.hashCode();
    }

    protected static Function<Object, CoreInstance> toCoreInstanceFn()
    {
        return LazyCompiledCoreInstanceUtilities.toCoreInstanceFunction();
    }

    protected static <T> Function<CoreInstance, T> fromValCoreInstanceFn()
    {
        return LazyCompiledCoreInstanceUtilities.fromValCoreInstanceFunction();
    }

    protected static <T> Function<CoreInstance, T> fromCoreInstanceFn()
    {
        return LazyCompiledCoreInstanceUtilities.fromCoreInstanceFunction();
    }

    private static String resolveName(InstanceData instanceData, ModelRepository repository)
    {
        if (instanceData.getName() != null)
        {
            return instanceData.getName();
        }
        if (repository != null)
        {
            return repository.nextAnonymousInstanceName();
        }
        return "Anonymous_NoCounter";
    }
}
