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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.unit;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class NewUnit extends NativeFunction
{
    private final ModelRepository repository;

    public NewUnit(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Deprecated
    public NewUnit(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this(repository);
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);

        Multiplicity multiplicity = (Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne);
        GenericType genericType = GenericTypeInstance.createPersistent(this.repository)._rawTypeCoreInstance(type);
        InstanceValue innerInstanceValue = InstanceValueInstance.createPersistent(this.repository, genericType, multiplicity)
                ._values(Lists.immutable.with(value));
        return InstanceValueInstance.createPersistent(this.repository, genericType, multiplicity)
                ._values(Lists.immutable.with(innerInstanceValue));
    }
}
