// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;

public class Project extends AbstractNative implements Native
{
    public Project()
    {
        super("project_C_MANY__FuncColSpecArray_1__Relation_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        StringBuilder result = new StringBuilder("org.finos.legend.pure.runtime.java.extension.external.relation.compiled.RelationNativeImplementation.project");
        result.append("(CompiledSupport.toPureCollection(");
        result.append(transformedParams.get(0));
        result.append("), ");
        result.append(transformedParams.get(1));
        result.append("._names(), ");
        result.append(transformedParams.get(1));
        result.append("._functions().collect(ff -> ");
        result.append(" (org.eclipse.collections.api.block.function.Function2)PureCompiledLambda.getPureFunction((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>)ff, es)");
        result.append("),");
        result.append(transformedParams.get(1));
        result.append("._functions().collect(ff -> ");
        result.append(" ((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?>)ff)._classifierGenericType()._typeArguments().toList().get(0)._rawType())._returnType()._rawType()._name()");
        result.append("), es)\n");
        return result.toString();
    }
}
