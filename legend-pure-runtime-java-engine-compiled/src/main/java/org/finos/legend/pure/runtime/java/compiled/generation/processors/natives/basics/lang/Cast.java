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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.basics.lang;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

/**
 * Cast native
 */
public class Cast extends AbstractNative
{
    public Cast()
    {
        super("cast_Any_m__T_1__T_m_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.multiplicity, processorSupport);
        String sourceObject = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        if ("null".equals(sourceObject) || !GenericType.isGenericTypeFullyConcrete(genericType, true, processorSupport))
        {
            String castType = TypeProcessor.typeToJavaObjectWithMul(genericType, multiplicity, processorSupport);
            return "((" + castType + ")" + (Multiplicity.isToZeroOrOne(multiplicity) ? "" : "(Object)CompiledSupport.toPureCollection(") + sourceObject + ")" + (Multiplicity.isToZeroOrOne(multiplicity) ? "" : ")");
        }
        else
        {
            SourceInformation sourceInformation = functionExpression.getSourceInformation();
            String castType = TypeProcessor.typeToJavaObjectSingle(genericType, true, processorSupport);
            String interfaceString = TypeProcessor.pureTypeToJava(genericType, false, false, true, processorSupport);
            return "CompiledSupport.<" + castType + ">castWithExceptionHandling(" + (Multiplicity.isToZeroOrOne(multiplicity) ? "" : "CompiledSupport.toPureCollection(") + sourceObject + (Multiplicity.isToZeroOrOne(multiplicity) ? "," : "),") + interfaceString + ".class," +
                    NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")";
        }
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<Object, " + FullJavaPaths.GenericType + ", Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object instances, " + FullJavaPaths.GenericType + " type, ExecutionSupport es)\n" +
                "            {\n" +
                "                if (instances instanceof RichIterable)\n" +
                "                {\n" +
                "                    return CompiledSupport.castWithExceptionHandling((RichIterable)instances, Pure.pureTypeToJavaClass(type._rawType(), es), null);\n" +
                "                }\n" +
                "                else\n" +
                "                {\n" +
                "                    return CompiledSupport.castWithExceptionHandling(instances, Pure.pureTypeToJavaClass(type._rawType(), es), null);\n" +
                "                }\n" +
                "            }\n" +
                "        }\n";
    }
}
