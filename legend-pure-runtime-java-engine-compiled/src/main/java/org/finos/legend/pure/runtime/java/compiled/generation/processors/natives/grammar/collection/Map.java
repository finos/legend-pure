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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.Native;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class Map extends AbstractNative implements Native
{
    public Map()
    {
        super("map_T_m__Function_1__V_m_", "map_T_MANY__Function_1__V_MANY_", "map_T_$0_1$__Function_1__V_$0_1$_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        ListIterable<? extends CoreInstance> parametersValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorSupport);
        CoreInstance functionExpressionMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, processorSupport);

        CoreInstance lambdaOrProperty = parametersValues.get(1);
        if (Instance.instanceOf(lambdaOrProperty, M3Paths.RoutedValueSpecification, processorSupport))
        {
            lambdaOrProperty = lambdaOrProperty.getValueForMetaPropertyToOne(M3Properties.value);
        }
        CoreInstance source = parametersValues.get(0);
        if (Instance.instanceOf(source, M3Paths.RoutedValueSpecification, processorSupport))
        {
            source = source.getValueForMetaPropertyToOne(M3Properties.value);
        }
        CoreInstance sourceMultiplicity = source.getValueForMetaPropertyToOne(M3Properties.multiplicity);

        boolean isSourceToOne = Multiplicity.isToOne(sourceMultiplicity, false);
        boolean isFunctionReturnToOne;
        boolean isFunctionExpressionToOne = Multiplicity.isToOne(functionExpressionMultiplicity, false);

        String list = transformedParams.get(0);

        StringBuilder result = new StringBuilder("CompiledSupport.mapTo");
        if (processorSupport.valueSpecification_instanceOf(lambdaOrProperty, M3Paths.Property))
        {
            if (processorSupport.instance_instanceOf(lambdaOrProperty, M3Paths.VariableExpression))
            {
                return "CompiledSupport.notSupportedYet()";
            }

            CoreInstance property = Instance.getValueForMetaPropertyToOneResolved(lambdaOrProperty, M3Properties.values, processorSupport);
            CoreInstance classifierGenericType = Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.classifierGenericType, processorSupport);
            CoreInstance resolvedReturnType = GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(source.getValueForMetaPropertyToOne(M3Properties.genericType), Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.typeArguments, processorSupport).get(1), processorSupport);
            CoreInstance returnMultiplicity = classifierGenericType.getValueForMetaPropertyToMany(M3Properties.multiplicityArguments).get(0);
            isFunctionReturnToOne = Multiplicity.isToOne(sourceMultiplicity, false);

            String returnType = TypeProcessor.typeToJavaObjectWithMul(resolvedReturnType, returnMultiplicity, processorSupport);

            CoreInstance sourceType = GenericType.reprocessTypeParametersUsingGenericTypeOwnerContext(source.getValueForMetaPropertyToOne(M3Properties.genericType), Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, "typeArguments", processorSupport).get(0), processorSupport);
            String type = TypeProcessor.typeToJavaObjectSingle(sourceType, true, processorSupport);

            result.append(Multiplicity.isToOne(returnMultiplicity, false) ? "One" : "Many");
            result.append("Over");
            result.append(isSourceToOne ? "One" : "Many");
            result.append('(');
            result.append(isFunctionReturnToOne ? list : "CompiledSupport.toPureCollection(" + list + ")");
            result.append(", new org.eclipse.collections.api.block.function.Function2<");
            result.append(type);
            result.append(", ExecutionSupport, ");
            result.append(returnType);
            result.append(">(){public ");
            result.append(returnType);
            result.append(" value(final ");
            result.append(type);
            result.append(" _lambdaParameter, final ExecutionSupport executionSupport){return _lambdaParameter._");
            result.append(PackageableElement.getSystemPathForPackageableElement(property, "_"));
            result.append("();}}, es)\n");
        }
        else
        {
            CoreInstance functionType = GenericType.resolveFunctionGenericType(Instance.getValueForMetaPropertyToOneResolved(lambdaOrProperty, M3Properties.genericType, processorSupport), processorSupport);
            String returnType = TypeProcessor.typeToJavaObjectWithMul(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorSupport);
            CoreInstance param = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();
            String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), true, processorSupport);

            isFunctionReturnToOne = Multiplicity.isToZeroOrOne(functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity));

            if ("this".equals(list))
            {
                list = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), false, processorSupport) + processorContext.getClassImplSuffix() + "." + list;
            }

            result.append(isFunctionReturnToOne ? "One" : "Many");
            result.append("Over");
            result.append(isSourceToOne ? "One" : "Many");
            result.append('(');
            result.append(isSourceToOne ? list : "CompiledSupport.toPureCollection(" + list + ")");
            result.append(", (org.eclipse.collections.api.block.function.Function2<");
            result.append(type);
            result.append(", ExecutionSupport, ");
            result.append(returnType);

            if (processorSupport.valueSpecification_instanceOf(lambdaOrProperty, M3Paths.LambdaFunction) && processorSupport.instance_instanceOf(lambdaOrProperty, M3Paths.InstanceValue) && !processorContext.isInLineAllLambda())
            {
                result.append(">)(");
                result.append(ValueSpecificationProcessor.createFunctionForLambda(topLevelElement, lambdaOrProperty.getValueForMetaPropertyToOne(M3Properties.values), false, processorSupport, processorContext));
                result.append("), es)\n");
            }
            else
            {
                result.append(">)(PureCompiledLambda.getPureFunction(");
                result.append(transformedParams.get(1));
                result.append(", es)), es)\n");
            }
        }
        if (isSourceToOne && isFunctionReturnToOne && !isFunctionExpressionToOne)
        {
            result.insert(0, "CompiledSupport.toPureCollection(");
            result.append(')');
        }
        return result.toString();
    }

    @Override
    public String buildBody()
    {
        return "new DefendedPureFunction2<Object, Object, Object>()\n" +
               "        {\n" +
               "            @Override\n" +
               "            public Object value(Object o, Object o2, final ExecutionSupport es)\n" +
               "            {\n" +
               "                " + FullJavaPaths.Function + " func = ("  + FullJavaPaths.Function + ")o2;\n" +
               "                " + FullJavaPaths.Multiplicity + " multiplicity;\n" +
               "                if (func instanceof " + FullJavaPaths.AbstractProperty + ")\n" +
               "                {\n" +
               "                    multiplicity = func._classifierGenericType()._multiplicityArguments().getFirst();\n" +
               "                }\n" +
               "                else if (func instanceof " + FullJavaPaths.FunctionDefinition + " || func instanceof " + FullJavaPaths.NativeFunction + ")\n" +
               "                {\n" +
               "                    multiplicity = ((" + FullJavaPaths.FunctionType + ")func._classifierGenericType()._typeArguments().getFirst()._rawType())._returnMultiplicity();\n" +
               "                }\n" +
               "                else\n" +
               "                {\n" +
               "                    throw new RuntimeException(func+\" is not supported yet!\");\n" +
               "                }\n" +
               "                if (Pure.hasToOneUpperBound(multiplicity))\n" +
               "                {\n" +
               "                    return CompiledSupport.mapToOneOverMany((RichIterable)o, (Function2)CoreGen.getSharedPureFunction(func,es), es);\n" +
               "                }\n" +
               "                else\n" +
               "                {\n" +
               "                    return CompiledSupport.mapToManyOverMany((RichIterable)o, (Function2)CoreGen.getSharedPureFunction(func,es), es);\n" +
               "                }\n" +
               "            }\n" +
               "        }";
    }
}
