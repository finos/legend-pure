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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.collection;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class Fold extends AbstractNativeFunctionGeneric
{
    public Fold()
    {
        super(getMethod(CompiledSupport.class, "fold"), "fold_T_MANY__Function_1__V_m__V_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        if (!transformedParams.isEmpty())
        {
            throw new IllegalArgumentException("transformed parameters must be empty");
        }

        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);

        String list = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        CoreInstance valueMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.multiplicity, processorSupport);
        //TODO Remove this hack
        if (Multiplicity.isToZeroOrOne(valueMultiplicity))
        {
            list = "CompiledSupport.toPureCollection(" + list + ")";
        }
        CoreInstance functionType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport);
        ListIterable<? extends CoreInstance> functionParams = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
        String param = Instance.getValueForMetaPropertyToOneResolved(functionParams.get(0), M3Properties.name, processorSupport).getName();
        String param2 = Instance.getValueForMetaPropertyToOneResolved(functionParams.get(1), M3Properties.name, processorSupport).getName();
        String init = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(2), processorContext);
        String sourceTypeO1 = TypeProcessor.typeToJavaObjectWithMul(functionParams.get(0).getValueForMetaPropertyToOne(M3Properties.genericType), functionParams.get(0).getValueForMetaPropertyToOne(M3Properties.multiplicity), processorSupport);
        String sourceTypeO2 = TypeProcessor.typeToJavaObjectWithMul(functionParams.get(1).getValueForMetaPropertyToOne(M3Properties.genericType), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorSupport);
        String functionReturnTypeWithMul = TypeProcessor.typeToJavaObjectWithMul(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorSupport);
        if (!Multiplicity.isToZeroOrOne(functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity)) && Multiplicity.isToZeroOrOne(parametersValues.get(2).getValueForMetaPropertyToOne(M3Properties.multiplicity)))
        {
            init = "CompiledSupport.<" + TypeProcessor.typeToJavaObjectSingle(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), true, processorSupport) + ">toPureCollection(" + init + ")";
        }
        CoreInstance sourceTypeO2GenericType = functionParams.get(1).getValueForMetaPropertyToOne(M3Properties.genericType);
        String functionParamTypeO2 = GenericType.isGenericTypeConcrete(sourceTypeO2GenericType, processorSupport) && "Nil".equals(sourceTypeO2GenericType.getValueForMetaPropertyToOne(M3Properties.rawType).getName()) ? functionReturnTypeWithMul : sourceTypeO2;
        return "CompiledSupport.fold(" + list + "," + "new DefendedFunction2<" + functionParamTypeO2 + "," + sourceTypeO1 + "," + functionReturnTypeWithMul + ">(){public " + functionReturnTypeWithMul + " value(final " + functionParamTypeO2 + " _" + param2 + ", final " + sourceTypeO1 + " _" + param + "){" + FunctionProcessor.processFunctionDefinitionContent(topLevelElement, Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.values, processorSupport), true, processorContext, processorSupport) + "}}" + "," + init + ")";
    }

    @Override
    public ListIterable<String> transformParameterValues(ListIterable<? extends CoreInstance> parametersValues, CoreInstance topLevelElement, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        return Lists.immutable.empty();
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction3<RichIterable, " + FullJavaPaths.Function + ", Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(RichIterable value, final " + FullJavaPaths.Function + " func, Object accumulator, final ExecutionSupport es)\n" +
                "            {\n" +
                "                org.eclipse.collections.api.block.function.Function2 function = new DefendedFunction2<Object, Object, Object>()\n" +
                "                {\n" +
                "                   public Object value(Object o1, Object o2) {\n" +
                "                       return CoreGen.evaluate(es, func, o2, o1);\n" +
                "                   }\n" +
                "                };\n" +
                "                return CompiledSupport.fold(value, function, accumulator);\n" +
                "            }\n" +
                "        }";
    }
}
