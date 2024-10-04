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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.grammar.collection.iteration;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
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

public class ParallelMap extends AbstractNative implements Native
{
    public ParallelMap()
    {
        super("parallelMap_T_m__Function_1__Integer_1__V_m_", "parallelMap_T_MANY__Function_1__Integer_1__V_MANY_", "parallelMap_T_$0_1$__Function_1__Integer_1__V_$0_1$_");
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
        String parallelism = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(parametersValues.get(2), M3Properties.values, processorSupport)).toString();

        StringBuilder result = new StringBuilder("CompiledSupport.parallelMapTo");

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

            result.append(Multiplicity.isToOne(returnMultiplicity, false) ? "One" : "Many").append("Over").append(isSourceToOne ? "One" : "Many").append('(');
            if (isFunctionReturnToOne)
            {
                result.append(list);
            }
            else
            {
                result.append("CompiledSupport.toPureCollection(").append(list).append(")");
            }
            result.append(", new org.eclipse.collections.api.block.function.Function2<").append(type).append(", ExecutionSupport, ").append(returnType).append(">(){public ").append(returnType);
            result.append(" value(final ").append(type).append(" _lambdaParameter, final ExecutionSupport executionSupport){return _lambdaParameter._");
            PackageableElement.writeSystemPathForPackageableElement(result, property, "_").append("();}}, es)\n");
        }
        else
        {
            CoreInstance functionType = GenericType.resolveFunctionGenericType(Instance.getValueForMetaPropertyToOneResolved(lambdaOrProperty, M3Properties.genericType, processorSupport), processorSupport);
            isFunctionReturnToOne = Multiplicity.isToZeroOrOne(functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity));

            String singleReturnType = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), true, processorSupport);
            String returnType;
            if (!isSourceToOne && !isFunctionReturnToOne)
            {
                returnType = "java.util.Collection<" + singleReturnType + ">";
            }
            else if (isSourceToOne && !isFunctionReturnToOne)
            {
                returnType = "RichIterable<? extends " + singleReturnType + ">";
            }
            else
            {
                returnType = singleReturnType;
            }

            CoreInstance param = functionType.getValueForMetaPropertyToMany(M3Properties.parameters).getFirst();
            String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), true, processorSupport);

            if ("this".equals(list))
            {
                list = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(param, M3Properties.genericType, processorSupport), false, processorSupport) + processorContext.getClassImplSuffix() + "." + list;
            }

            result.append(isFunctionReturnToOne ? "One" : "Many").append("Over").append(isSourceToOne ? "One" : "Many").append('(');
            if (isSourceToOne)
            {
                result.append(list);
            }
            else
            {
                result.append("CompiledSupport.toPureCollection(").append(list).append(")");
            }
            result.append(", (org.eclipse.collections.api.block.function.Function2<").append(type).append(", ExecutionSupport, ").append(returnType);

            if (processorSupport.valueSpecification_instanceOf(lambdaOrProperty, M3Paths.LambdaFunction) && processorSupport.instance_instanceOf(lambdaOrProperty, M3Paths.InstanceValue) && !processorContext.isInLineAllLambda())
            {
                result.append(">)(")
                        .append(ValueSpecificationProcessor.createFunctionForLambda(topLevelElement, lambdaOrProperty.getValueForMetaPropertyToOne(M3Properties.values), false, processorSupport, processorContext))
                        .append("),").append(parallelism).append(", es)\n");
            }
            else
            {
                result.append(">)(PureCompiledLambda.getPureFunction(").append(transformedParams.get(1)).append(", es)),").append(parallelism).append(", es)\n");
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
        return "new DefendedPureFunction3<Object, Object, Object, Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object value(Object o, Object o2, Object o3, final ExecutionSupport es)\n" +
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
                "                    throw new RuntimeException(func + \" is not supported yet!\");\n" +
                "                }\n" +
                "                if (Pure.hasToOneUpperBound(multiplicity))\n" +
                "                {\n" +
                "                    return (o instanceof RichIterable) ? CompiledSupport.parallelMapToOneOverMany((RichIterable)o, (Function2)CoreGen.getSharedPureFunction(func,es), ((java.lang.Long)o3).intValue(), es) : CompiledSupport.mapToOneOverOne(o, (Function2)CoreGen.getSharedPureFunction(func,es), es);\n" +
                "                }\n" +
                "                else\n" +
                "                {\n" +
                "                    return (o instanceof RichIterable) ? CompiledSupport.parallelMapToManyOverMany((RichIterable)o, (Function2)CoreGen.getSharedPureFunction(func,es), ((java.lang.Long)o3).intValue(), es) : CompiledSupport.mapToManyOverOne(o, (Function2)CoreGen.getSharedPureFunction(func,es), es);\n" +
                "                }\n" +
                "            }\n" +
                "        }";
    }
}
