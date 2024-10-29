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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.essentials.lang.cast;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
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
        CoreInstance targetGenericType = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport);
        CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(0), M3Properties.multiplicity, processorSupport);

        String sourceObject = ValueSpecificationProcessor.processValueSpecification(topLevelElement, parametersValues.get(0), processorContext);
        SourceInformation sourceInformation = functionExpression.getSourceInformation();

        CoreInstance targetRawType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport);
        if (Type.isExtendedPrimitiveType(targetRawType, processorSupport))
        {
            String runnable = "new Runnable(){public void run() {\n" +
                    GenericType.getAllSuperTypesIncludingSelf(targetGenericType, processorSupport).collect(genericType ->
                    {
                        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
                        MutableList<String> vars = genericType.getValueForMetaPropertyToMany(M3Properties.typeVariableValues).collect(x -> x.getValueForMetaPropertyToMany(M3Properties.values).collect(CoreInstance::getName).makeString(",")).toList();
                        vars.add(sourceObject);
                        if (rawType.getValueForMetaPropertyToMany(M3Properties.constraints).notEmpty())
                        {
                            return JavaPackageAndImportBuilder.buildImplClassNameFromType(rawType, processorSupport) + "._validate(" + vars.makeString(", ") + ", " + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ", es);";
                        }
                        return "";
                    }).makeString("\n")
                    + "}}";
            return "CompiledSupport.castExtendedPrimitive(" + sourceObject + ", " + runnable + ", " + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")";
        }
        else
        {
            if ("null".equals(sourceObject) || !GenericType.isGenericTypeFullyConcrete(targetGenericType, true, processorSupport))
            {
                String castType = TypeProcessor.typeToJavaObjectWithMul(targetGenericType, multiplicity, processorSupport);
                return "((" + castType + ")" + (Multiplicity.isToZeroOrOne(multiplicity) ? "" : "(Object)CompiledSupport.toPureCollection(") + sourceObject + ")" + (Multiplicity.isToZeroOrOne(multiplicity) ? "" : ")");
            }
            else
            {
                String castType = TypeProcessor.typeToJavaObjectSingle(targetGenericType, true, processorSupport);
                String interfaceString = TypeProcessor.pureTypeToJava(targetGenericType, false, false, true, processorSupport);
                return "CompiledSupport.<" + castType + ">castWithExceptionHandling(" +
                        (Multiplicity.isToZeroOrOne(multiplicity) ? "" : "CompiledSupport.toPureCollection(") + sourceObject + (Multiplicity.isToZeroOrOne(multiplicity) ? "," : "),") +
                        interfaceString + ".class," +
                        NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) +
                        ")";
            }
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
