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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNativeFunctionGeneric;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public abstract class AbstractMatch extends AbstractNativeFunctionGeneric
{
    protected AbstractMatch(String methodName, Class<?>[] parameterTypes, String... signatures)
    {
        super(methodName, parameterTypes, false, true, false, signatures);
    }

    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext, boolean isMatchWith)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();

        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());

        CoreInstance functionType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(1), M3Properties.genericType, processorSupport), M3Properties.typeArguments, processorSupport).getFirst(), M3Properties.rawType, processorSupport);
        CoreInstance funcReturnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, "returnMultiplicity", processorSupport);
        boolean resultMany = !Multiplicity.isToOne(funcReturnMultiplicity, false);

        String type = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport), processorSupport);
        String input = transformedParams.get(0);

        SourceInformation sourceInformation = functionExpression.getSourceInformation();

        String match = "(" + type + ")";
        if (Instance.instanceOf(parametersValues.get(1), M3Paths.InstanceValue, processorSupport))
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(parametersValues.get(1), M3Properties.values, processorSupport);
            String endBrackets = "";

            for (CoreInstance matchFunc : values)
            {
                CoreInstance matchFunctionType = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(Instance.getValueForMetaPropertyToOneResolved(matchFunc, M3Properties.classifierGenericType, processorSupport), M3Properties.typeArguments, processorSupport).getFirst(), M3Properties.rawType, processorSupport);
                CoreInstance funcParam = Instance.getValueForMetaPropertyToManyResolved(matchFunctionType, M3Properties.parameters, processorSupport).getFirst();
                CoreInstance sourceGenericType = funcParam.getValueForMetaPropertyToOne(M3Properties.genericType);
                CoreInstance sourceRawType = Instance.getValueForMetaPropertyToOneResolved(sourceGenericType, M3Properties.rawType, processorSupport);
                String sourceType = TypeProcessor.typeToJavaObjectSingle(sourceGenericType, false, processorSupport);
                CoreInstance funcParam2 = isMatchWith ? Instance.getValueForMetaPropertyToManyResolved(matchFunctionType, M3Properties.parameters, processorSupport).get(1) : null;
                String otherType = isMatchWith ? TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(funcParam2, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(funcParam2, M3Properties.multiplicity, processorSupport), processorSupport) : null;
                CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(funcParam, M3Properties.multiplicity, processorSupport);
                int lowerBound = Multiplicity.multiplicityLowerBoundToInt(multiplicity);
                int upperBound = Multiplicity.multiplicityUpperBoundToInt(multiplicity);
                String funcParamName = Instance.getValueForMetaPropertyToOneResolved(funcParam, M3Properties.name, processorSupport).getName();
                String funcParamName2 = isMatchWith ? Instance.getValueForMetaPropertyToOneResolved(funcParam2, M3Properties.name, processorSupport).getName() : "";
                String value = FunctionProcessor.processFunctionDefinitionContent(null, matchFunc, true, processorContext, processorSupport);

                if (Instance.instanceOf(sourceRawType, M3Paths.Enumeration, processorSupport))
                {
                    String enumerationSystemPath = PackageableElement.getSystemPathForPackageableElement(sourceRawType);
                    match += "(Pure.matchesEnumeration(" + input + ",\"" + enumerationSystemPath + "\"," + lowerBound + "," + upperBound + ")?\n";
                }
                else
                {
                    match += "(Pure.matches(" + input + "," + sourceType + ".class," + lowerBound + "," + upperBound + ")?\n";
                }
                match += (resultMany ? "CompiledSupport.toPureCollection(" : "") +
                        "   (new DefendedFunction" + (isMatchWith ? "2<Object," + otherType + ", Object>" : "") + "()\n" +
                        "   {\n" +
                        "       public Object value" + (isMatchWith ? "" : "Of") + "(final Object _" + funcParamName + "_As_Object" + (isMatchWith ? ", final " + otherType + " _" + funcParamName2 : "") + ")\n" +
                        "       {\n" +
                        (Multiplicity.isToOne(funcParam.getValueForMetaPropertyToOne(M3Properties.multiplicity), false) ?
                                "            final " + sourceType + " _" + funcParamName + " = (" + sourceType + ")CompiledSupport.makeOne(_" + funcParamName + "_As_Object);\n"
                                :
                                "            final RichIterable _" + funcParamName + " = CompiledSupport.toPureCollection(_" + funcParamName + "_As_Object);\n"
                        ) +
                        "            " + value + "\n" +
                        "       }\n" +

                        "   }).value" + (isMatchWith ? "" : "Of") + "(" + ((upperBound == 1) ? "CompiledSupport.first(" : "CompiledSupport.toPureCollection(") + input + ")" + (isMatchWith ? ", " + transformedParams.get(2) : "") + ")" + (resultMany ? ")" : "") + "\n" +
                        ":\n";


                endBrackets += ")";
            }

            match += "CompiledSupport.matchFailure(" + input + "," + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")" + endBrackets + "\n";
            return match;
        }
        else
        {
            return "(" + type + ")(Object)" + (resultMany ? "CompiledSupport.toPureCollection(" : "") +
                    (isMatchWith ? "FunctionsGen.dynamicMatchWith" : "CoreGen.dynamicMatch") + "(" + input + ",(RichIterable<" + FullJavaPaths.Function + "<? extends Object>>)(Object)CompiledSupport.toOneMany(" + transformedParams.get(1) +
                    "," + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + ")" + (isMatchWith ? "," + transformedParams.get(2) : "") + ", es)" +
                    (resultMany ? ")" : "");
        }
    }
}
