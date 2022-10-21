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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPurePrimitiveTypeMapping;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;

public class ValueSpecificationProcessor
{
    public static String processValueSpecification(CoreInstance valueSpecification, ProcessorContext processorContext)
    {
        return processValueSpecification(valueSpecification, false, processorContext);
    }

    public static String processValueSpecification(CoreInstance topLevelElement, CoreInstance valueSpecification, ProcessorContext processorContext)
    {
        return processValueSpecification(topLevelElement, valueSpecification, false, processorContext);
    }

    public static String processValueSpecification(CoreInstance valueSpecification, boolean topLevel, ProcessorContext processorContext)
    {
        CoreInstance topLevelElement = topLevel ? valueSpecification : null;
        return processValueSpecification(topLevelElement, valueSpecification, topLevel, processorContext);
    }

    public static String processValueSpecification(final CoreInstance topLevelElement, CoreInstance valueSpecification, boolean topLevel, final ProcessorContext processorContext)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();

        if (processorSupport.instance_instanceOf(valueSpecification, M3Paths.FunctionExpression))
        {
            return FunctionExpressionProcessor.processFunctionExpression(topLevelElement, valueSpecification, topLevel, processorContext);
        }
        if (processorSupport.instance_instanceOf(valueSpecification, M3Paths.RoutedValueSpecification))
        {
            return processValueSpecificationContent(topLevelElement, valueSpecification.getValueForMetaPropertyToOne(M3Properties.value), processorContext, processorSupport);
        }
        if (processorSupport.instance_instanceOf(valueSpecification, M3Paths.VariableExpression))
        {
            String name = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.name, processorSupport).getName();
            return "this".equals(name) ? qualifiedThisVariable(topLevelElement, valueSpecification, processorSupport, processorContext) : "_" + name;
        }
        if (processorSupport.instance_instanceOf(valueSpecification, M3Paths.InstanceValue))
        {
            if (processorSupport.type_isPrimitiveType(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, M3Properties.rawType, processorSupport)))
            {
                return processPrimitiveValueSpecification(topLevelElement, valueSpecification, processorContext);
            }
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(valueSpecification, M3Properties.values, processorSupport);

            if (values.size() == 1 && Measure.isUnitOrMeasureInstance(valueSpecification, processorSupport))
            {
                return processUnit(valueSpecification, values.get(0), processorSupport);
            }
            if (values.size() == 1 && processorSupport.instance_instanceOf(values.get(0), M3Paths.FunctionExpression))
            {
                return FunctionExpressionProcessor.processFunctionExpression(topLevelElement, values.get(0), false, processorContext);
            }
            if (values.size() == 1 && processorSupport.instance_instanceOf(values.get(0), M3Paths.VariableExpression))
            {
                return processValueSpecification(topLevelElement, values.get(0), false, processorContext);
            }
            if (processorSupport.valueSpecification_instanceOf(valueSpecification, M3Paths.Nil))
            {
                if (values.notEmpty())
                {
                    throw new RuntimeException("Non-empty list of type " + M3Paths.Nil);
                }
                return "null";
            }
            if (processorSupport.valueSpecification_instanceOf(valueSpecification, M3Paths.Class))
            {
                if (values.isEmpty())
                {
                    CoreInstance typeArg = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, M3Properties.typeArguments, processorSupport);
                    CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(typeArg, M3Properties.rawType, processorSupport);
                    if (rawType == null)
                    {
                        CoreInstance typeParam = Instance.getValueForMetaPropertyToOneResolved(typeArg, M3Properties.typeParameter, processorSupport);
                        return "((" + FullJavaPaths.Class + "<" + Instance.getValueForMetaPropertyToOneResolved(typeParam, M3Properties.name, processorSupport);
                    }
                    else
                    {
                        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, M3Properties.typeArguments, M3Properties.rawType, processorSupport);
                        return "((" + FullJavaPaths.Class + "<" + TypeProcessor.fullyQualifiedJavaInterfaceNameForType(_class) + ">)((CompiledExecutionSupport)es).getMetadataAccessor(\"" + PackageableElement.getSystemPathForPackageableElement(_class, "::") + "\"))";
                    }
                }
                else if (values.size() == 1)
                {
                    CoreInstance cls = values.get(0);
                    while (!processorSupport.instance_instanceOf(cls, M3Paths.Class))
                    {
                        // Class is wrapped in an InstanceValue
                        cls = Instance.getValueForMetaPropertyToOneResolved(cls, M3Properties.values, processorSupport);
                    }
                    CoreInstance type = processorSupport.getClassifier(cls);
                    String classifier = MetadataJavaPaths.buildMetadataKeyFromType(type);
                    return "((" + TypeProcessor.fullyQualifiedJavaInterfaceNameForType(type) + "<" + TypeProcessor.fullyQualifiedJavaInterfaceNameForType(cls) + ">)((CompiledExecutionSupport)es).getMetadata(\"" + classifier + "\",\"" + PackageableElement.getSystemPathForPackageableElement(cls) + "\"))";
                }
                else
                {
                    MutableSet<String> types = Sets.mutable.empty();
                    String listElements = values.collect(c ->
                    {
                        CoreInstance cls = c;
                        while (!processorSupport.instance_instanceOf(cls, M3Paths.Class))
                        {
                            // Class is wrapped in an InstanceValue
                            cls = Instance.getValueForMetaPropertyToOneResolved(cls, M3Properties.values, processorSupport);
                        }

                        String type = TypeProcessor.fullyQualifiedJavaInterfaceNameForType(cls);
                        types.add(type);
                        String classifier = TypeProcessor.fullyQualifiedJavaInterfaceNameForType(processorSupport.getClassifier(cls));
                        return "((" + classifier + "<? extends " + type + ">)((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.buildMetadataKeyFromType(processorSupport.getClassifier(cls)) + "\",\"" + PackageableElement.getSystemPathForPackageableElement(cls, "::") + "\"))";
                    }).makeString();
                    String typeString = (types.size() > 1) ? ("<" + TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, processorSupport), true, processorSupport) + ">") : "";
                    return "Lists.mutable." + typeString + "with(" + listElements + ")";
                }
            }
            if (processorSupport.valueSpecification_instanceOf(valueSpecification, M3Paths.Enumeration))
            {
                String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, processorSupport), true, processorSupport);

                if (values.size() == 1)
                {
                    CoreInstance enumeration = values.get(0);
                    while (!processorSupport.instance_instanceOf(enumeration, M3Paths.Enumeration))
                    {
                        // Enumeration is wrapped in an InstanceValue
                        enumeration = Instance.getValueForMetaPropertyToOneResolved(enumeration, M3Properties.values, processorSupport);
                    }
                    return "((" + type + ")((CompiledExecutionSupport)es).getMetadataAccessor().getEnumeration(\"" + PackageableElement.getSystemPathForPackageableElement(enumeration) + "\"))";
                }
                else
                {
                    return "Lists.mutable.<" + type + ">with(" + values.collect(v -> processValueSpecificationContent(topLevelElement, v, processorContext, processorSupport)).makeString(",") + ")";
                }
            }
            if (!Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.multiplicity, processorSupport), false))
            {
                String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, processorSupport), true, processorSupport);
                ListIterable<String> processedValues = values.collect(v -> processValueSpecificationContent(topLevelElement, v, processorContext, processorSupport));
                return processedValues.size() > 1 ? "Lists.mutable.<" + type + ">with(" + processedValues.makeString(",") + ")" : processedValues.makeString(",");
            }
            if (Multiplicity.isToOne(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.multiplicity, processorSupport), false))
            {
                if (values.notEmpty())
                {
                    return processValueSpecificationContent(topLevelElement, Instance.getValueForMetaPropertyToManyResolved(valueSpecification, M3Properties.values, processorContext.getSupport()).get(0), processorContext, processorSupport);
                }
                else
                {
                    return "(" + TypeProcessor.typeToJavaObjectSingle(valueSpecification.getValueForMetaPropertyToOne(M3Properties.genericType), true, processorContext.getSupport()) + ")null";
                }
            }
            throw new RuntimeException(" To CODE ! 1" + processorSupport.getClassifier(valueSpecification) + valueSpecification.printWithoutDebug("", 1));
        }

        throw new RuntimeException(" To CODE ! 2" + processorSupport.getClassifier(valueSpecification) + valueSpecification.printWithoutDebug("", 1));
    }

    private static String processUnit(CoreInstance valueSpecification, CoreInstance firstValue, ProcessorSupport processorSupport)
    {
        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(firstValue, M3Properties.values, processorSupport);
        CoreInstance _unitType = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, M3Properties.rawType, processorSupport);

        return "new org.finos.legend.pure.generated." + UnitProcessor.convertToJavaCompatibleClassName(JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(_unitType)) + "(\"Anonymous_NoCounter\", es)._val(" + value.getName() + ")";
    }

    private static String qualifiedThisVariable(CoreInstance topLevelElement, CoreInstance valueSpecification, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        if (topLevelElement != null)
        {
            if (Instance.instanceOf(topLevelElement, M3Paths.Class, processorSupport))
            {
                CoreInstance varGT = Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, processorSupport);
                CoreInstance varRawType = Instance.getValueForMetaPropertyToOneResolved(varGT, M3Properties.rawType, processorSupport);
                String result = "this";
                if ((varRawType != null) && processorSupport.type_subTypeOf(topLevelElement, varRawType) && !varRawType.equals(processorSupport.type_TopType()))
                {
                    result = JavaPackageAndImportBuilder.buildImplClassNameFromType(topLevelElement, processorContext.getClassImplSuffix()) + "." + result;
                }
                return result;
            }
            if (Instance.instanceOf(topLevelElement, M3Paths.Constraint, processorSupport))
            {
                // TODO do we need to check anything about this variable?
                return "_this";
            }
        }
        return "this";
    }

    private static String processValueSpecificationContent(CoreInstance topLevelElement, CoreInstance content, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = Instance.extractGenericTypeFromInstance(content, processorSupport);
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
        String type = TypeProcessor.typeToJavaObjectSingle(genericType, true, processorContext.getSupport());

        if (processorSupport.instance_instanceOf(content, M3Paths.ValueSpecification))
        {
            return processValueSpecification(topLevelElement, content, processorContext);
        }
        else if (processorSupport.instance_instanceOf(content, M3Paths.LambdaFunction))
        {
            return processLambda(topLevelElement, content, processorSupport, processorContext);
        }
        else if (processorSupport.type_isPrimitiveType(rawType))
        {
            return JavaPurePrimitiveTypeMapping.convertPureCoreInstanceToJavaType(content, processorContext);
        }
        else
        {
            if (content.getSyntheticId() == -1)
            {
                // TODO We need to do this process earlier so that we can push collections faster
                return "((" + type + ")valMap.get(\"" + processorContext.addObjectToPassToDynamicallyGeneratedCode(content) + "\"))";
            }
            return "((" + type + ")((CompiledExecutionSupport)es).getMetadata(\"" + MetadataJavaPaths.buildMetadataKeyFromType(rawType) + "\",\"" + processorContext.getIdBuilder().buildId(content) + "\"))";
        }
    }

    public static String processLambda(CoreInstance topLevelElement, CoreInstance function, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {

        String pureFunctionString = createFunctionForLambda(topLevelElement, function, processorSupport, processorContext);
        String lambdaFunctionString = (processorContext.isInLineAllLambda() ?
                "(" + FullJavaPaths.LambdaFunction + ")localLambdas.get(" + System.identityHashCode(function) + ")" :
                "((CompiledExecutionSupport)es).getMetadataAccessor().getLambdaFunction(\"" + processorContext.getIdBuilder().buildId(function) + "\")");

        return "new PureCompiledLambda(\n(" + lambdaFunctionString + "\n), (\n" + pureFunctionString + "\n))\n";
    }

    public static String createFunctionForLambda(CoreInstance topLevelElement, CoreInstance function, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        return createFunctionForLambda(topLevelElement, function, true, processorSupport, processorContext);
    }

    public static String createFunctionForLambda(CoreInstance topLevelElement, CoreInstance function, boolean registerLambdasInProcessorContext, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        boolean notOpenVariables = !containsOpenVariablesLambda(function);

        CoreInstance functionType = processorSupport.function_getFunctionType(function);
        ListIterable<? extends CoreInstance> params = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);

        boolean fullyConcreteSignature = GenericType.isGenericTypeFullyConcrete(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), processorSupport) && params.allSatisfy(p -> GenericType.isGenericTypeFullyConcrete(p.getValueForMetaPropertyToOne(M3Properties.genericType), processorSupport));

        String pureFunctionString;
        if (fullyConcreteSignature && notOpenVariables && !processorContext.isInLineAllLambda())
        {
            String sourceId = IdBuilder.sourceToId(function.getSourceInformation());
            String functionId = processorContext.getIdBuilder().buildId(function);

            //Only need to create this if we are currently generating this file
            if (registerLambdasInProcessorContext && (topLevelElement == null || function.getSourceInformation().getSourceId().equals(topLevelElement.getSourceInformation().getSourceId())))
            {
                String func = createLambdaBody(topLevelElement, function, processorContext, notOpenVariables, functionType, params);
                processorContext.registerLambdaFunction(sourceId, functionId, func);
            }
            pureFunctionString = sourceId + ".__functions.get(\"" + functionId + "\")";
        }
        else
        {
            pureFunctionString = createLambdaBody(topLevelElement, function, processorContext, notOpenVariables, functionType, params);
            if (processorContext.isInLineAllLambda())
            {
                processorContext.registerLocalLambdas(System.identityHashCode(function), function);
            }

        }
        return pureFunctionString;
    }

    private static String createLambdaBody(final CoreInstance topLevelElement, CoreInstance function, final ProcessorContext processorContext, boolean notOpenVariables, CoreInstance functionType, ListIterable<? extends CoreInstance> params)
    {
        int paramCount = params.size();
        String type;
        MutableList<String> typesParams = Lists.mutable.withInitialCapacity(paramCount + 1);
        String baseType = notOpenVariables ? "DefaultPureLambdaFunction" : "DefendedPureLambdaFunction";
        if (paramCount < 3)
        {
            type = baseType + paramCount;
            for (CoreInstance param : params)
            {
                typesParams.add(TypeProcessor.typeToJavaObjectWithMul(param.getValueForMetaPropertyToOne(M3Properties.genericType), param.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorContext.getSupport()));
            }
        }
        else
        {
            type = baseType;
        }
        typesParams.add(TypeProcessor.typeToJavaObjectWithMul(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), functionType.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), processorContext.getSupport()));
        String typeParamsStr = typesParams.makeString("<", ", ", ">");

        String funcSignature = params.collect(p -> "final " + TypeProcessor.typeToJavaObjectWithMul(p.getValueForMetaPropertyToOne(M3Properties.genericType), p.getValueForMetaPropertyToOne(M3Properties.multiplicity), processorContext.getSupport()) + " _" + p.getValueForMetaPropertyToOne(M3Properties.name).getName()).makeString(",");

        String openVarsInitializer = "";

        if (!notOpenVariables)
        {
            ListIterable<? extends CoreInstance> vars = function.getValueForMetaPropertyToMany(M3Properties.openVariables);
            if (vars.size() < 4)
            {
                openVarsInitializer = "private MutableMap<String, Object> __vars = Maps.fixedSize.<String, Object>of(" + vars.collect(openVar ->
                {
                    String varName = openVar.getName();
                    String value;
                    if ("this".equals(varName) && Instance.instanceOf(topLevelElement, M3Paths.Class, processorContext.getSupport()))
                    {
                        value = JavaPackageAndImportBuilder.buildImplClassNameFromType(topLevelElement) + ".this";
                    }
                    else
                    {
                        value = "_" + varName;
                    }
                    return "\"" + varName + "\"," + value;
                }).makeString(",") + ");\n";
            }
            else
            {
                openVarsInitializer = "private MutableMap<String, Object> __vars = UnifiedMap.newMap(" + vars.size() + ");\n {" + vars.collect(var -> "__vars.put(\"" + var.getName() + "\"," + ("this".equals(var.getName()) ? JavaPackageAndImportBuilder.buildImplClassNameFromType(topLevelElement) + ".this" : "_" + var.getName()) + ")").makeString(";\n") + ";\n}";
            }
        }

        return "new " + type + typeParamsStr + "()\n" +
                "{\n" +
                openVarsInitializer +
                "     public " + typesParams.get(typesParams.size() - 1) + " execute(ListIterable<?> vars, ExecutionSupport es)\n" +
                "     {\n" +
                "         return value" + (params.isEmpty() ? "Of(" : "(") + params.zipWithIndex().collect(coreInstance -> {
            String ntype = TypeProcessor.typeToJavaObjectWithMul(coreInstance.getOne().getValueForMetaPropertyToOne(M3Properties.genericType), coreInstance.getOne().getValueForMetaPropertyToOne(M3Properties.multiplicity), processorContext.getSupport());
            boolean isToMany = !Multiplicity.isToZeroOrOne(coreInstance.getOne().getValueForMetaPropertyToOne(M3Properties.multiplicity));
            return "(" + ntype + ")" + (isToMany ? "(Object)CompiledSupport.toPureCollection(" : "CompiledSupport.makeOne(") + "vars.get(" + coreInstance.getTwo() + "))";

        }).makeString(",") + (params.isEmpty() ? "" : ", ") + "es);\n" +
                "     }\n" +
                "\n" +
                "     public " + typesParams.get(typesParams.size() - 1) + " value" + (params.isEmpty() ? "Of(" : "(") + funcSignature + (params.isEmpty() ? "" : ", ") + "final ExecutionSupport es)\n" +
                "     {\n" +
                FunctionProcessor.processFunctionDefinitionContent(topLevelElement, function, true, processorContext, processorContext.getSupport()) + "\n" +
                "     }\n" +
                (notOpenVariables ? "" :
                        "     public MutableMap<String, Object> getOpenVariables()\n" +
                                "     {\n" +
                                "         return this.__vars;\n" +
                                "     }\n") +
                "}\n";
    }

    @Deprecated
    public static boolean containsOpenVariablesLambda(CoreInstance function, ProcessorContext processorContext)
    {
        return containsOpenVariablesLambda(function);
    }

    public static boolean containsOpenVariablesLambda(CoreInstance function)
    {
        return function.getValueForMetaPropertyToMany(M3Properties.openVariables).notEmpty();
    }

    public static String processPrimitiveValueSpecification(final CoreInstance topLevelElement, CoreInstance valueSpecification, final ProcessorContext processorContext)
    {
        final ProcessorSupport support = processorContext.getSupport();
        ListIterable<? extends CoreInstance> instances = Instance.getValueForMetaPropertyToManyResolved(valueSpecification, M3Properties.values, processorContext.getSupport());

        int size = instances.size();
        switch (size)
        {
            case 0:
            {
                return "null";
            }
            case 1:
            {
                if (support.instance_instanceOf(instances.get(0), M3Paths.ValueSpecification))
                {
                    return processValueSpecification(topLevelElement, instances.get(0), processorContext);
                }
                else
                {
                    return JavaPurePrimitiveTypeMapping.convertPureCoreInstanceToJavaType(instances.get(0), processorContext);
                }
            }
            default:
            {
                String type = TypeProcessor.typeToJavaObjectSingle(Instance.getValueForMetaPropertyToOneResolved(valueSpecification, M3Properties.genericType, support), true, support);
                return "Lists.mutable.<" + type + ">with(" + instances.collect(i -> support.instance_instanceOf(i, M3Paths.ValueSpecification) ?
                        processValueSpecification(topLevelElement, i, processorContext) :
                        JavaPurePrimitiveTypeMapping.convertPureCoreInstanceToJavaType(i, processorContext))
                        .makeString(",") + ")";
            }
        }
    }

    public static String possiblyConvertProcessedValueSpecificationToCollection(String processedValueSpecification, CoreInstance targetMultiplicity, CoreInstance targetGenericType, boolean shouldCast, ProcessorContext processorContext)
    {
        String typeCast = "";
        if (shouldCast)
        {
            typeCast = "(" + TypeProcessor.typeToJavaObjectWithMul(targetGenericType, targetMultiplicity, processorContext.getSupport()) + ")";
        }
        return "null".equals(processedValueSpecification) || targetMultiplicity == null || Multiplicity.isToZeroOrOne(targetMultiplicity) ? typeCast + processedValueSpecification : "CompiledSupport.toPureCollection(" + typeCast + processedValueSpecification + ")";
    }
}
