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

package org.finos.legend.pure.runtime.java.interpreted.natives.grammar.lang.cast;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Stack;

public class ToOne extends CommonToMultiplicity
{
    public ToOne(ModelRepository repository)
    {
        super(repository, true);
    }

    @Override
    protected CoreInstance getReturnMultiplicity(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ProcessorSupport processorSupport)
    {
        return Multiplicity.newMultiplicity(1, processorSupport);
    }
}
