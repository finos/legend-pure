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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class SourceInformation extends NativeFunction
{
    private final ModelRepository repository;

    public SourceInformation(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {

        org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getSourceInformation();
        if (sourceInformation == null)
        {
            return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
        }
        else
        {
            CoreInstance newSourceInfo = createSourceInfoCoreInstance(this.repository, processorSupport, sourceInformation);
            return ValueSpecificationBootstrap.wrapValueSpecification(newSourceInfo, true, processorSupport);
        }
    }

    private static CoreInstance createSourceInfoCoreInstance(ModelRepository repository, ProcessorSupport processorSupport, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation)
    {
        CoreInstance newSourceInfo = createSourceInfoCoreInstanceWithoutSourceId(repository, processorSupport, sourceInformation);
        Instance.addValueToProperty(newSourceInfo, M3Properties.source, repository.newCoreInstance(sourceInformation.getSourceId(), repository.getTopLevel(M3Paths.String), null), processorSupport);
        return newSourceInfo;
    }

    public static CoreInstance createSourceInfoCoreInstanceWithoutSourceId(ModelRepository repository, ProcessorSupport processorSupport, org.finos.legend.pure.m4.coreinstance.SourceInformation sourceInformation)
    {
        CoreInstance newSourceInfo = repository.newAnonymousCoreInstance(null, processorSupport.package_getByUserPath(M3Paths.SourceInformation));

        CoreInstance integerType = repository.getTopLevel(ModelRepository.INTEGER_TYPE_NAME);
        Instance.addValueToProperty(newSourceInfo, M3Properties.startLine, repository.newCoreInstance(String.valueOf(sourceInformation.getStartLine()), integerType, null), processorSupport);
        Instance.addValueToProperty(newSourceInfo, M3Properties.startColumn, repository.newCoreInstance(String.valueOf(sourceInformation.getStartColumn()), integerType, null), processorSupport);
        Instance.addValueToProperty(newSourceInfo, M3Properties.line, repository.newCoreInstance(String.valueOf(sourceInformation.getLine()), integerType, null), processorSupport);
        Instance.addValueToProperty(newSourceInfo, M3Properties.column, repository.newCoreInstance(String.valueOf(sourceInformation.getColumn()), integerType, null), processorSupport);
        Instance.addValueToProperty(newSourceInfo, M3Properties.endLine, repository.newCoreInstance(String.valueOf(sourceInformation.getEndLine()), integerType, null), processorSupport);
        Instance.addValueToProperty(newSourceInfo, M3Properties.endColumn, repository.newCoreInstance(String.valueOf(sourceInformation.getEndColumn()), integerType, null), processorSupport);
        return newSourceInfo;
    }
}
