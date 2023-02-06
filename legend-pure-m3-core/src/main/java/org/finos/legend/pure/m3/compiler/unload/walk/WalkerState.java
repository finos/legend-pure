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

package org.finos.legend.pure.m3.compiler.unload.walk;

import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class WalkerState extends MatcherState
{
    private final MutableSet<CoreInstance> instances = Sets.mutable.with();
    private final CoreInstance mappingClassClass;

    public WalkerState(M3ProcessorSupport processorSupport)
    {
        super(processorSupport);
        this.mappingClassClass =_Package.getByUserPath("meta::pure::mapping::MappingClass", processorSupport);
    }


    public void addInstance(CoreInstance instance)
    {
        if (instance == null)
        {
            throw new IllegalArgumentException("Cannot add null instance");
        }
        if (this.mappingClassClass == null || !Instance.instanceOf(instance, this.mappingClassClass, this.processorSupport))
        {
            this.instances.add(instance);
        }
    }

    public void addInstances(Iterable<? extends CoreInstance> instances)
    {
        for (CoreInstance instance : instances)
        {
            this.addInstance(instance);
        }
    }

    public SetIterable<CoreInstance> getInstances()
    {
        return this.instances.asUnmodifiable();
    }

    @Override
    public InlineDSLLibrary getInlineDSLLibrary()
    {
        throw new RuntimeException("Not supported here.");
    }
}
