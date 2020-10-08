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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

public class FunctionProcessor
{
    public static String processFunctionDefinition(final CoreInstance functionDefinition, final ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        String typeParams = processorSupport.function_getFunctionType(functionDefinition).getValueForMetaPropertyToMany(M3Properties.typeParameters).collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M3Properties.name).getName();
            }
        }).makeString(",");

        String func = "public static " + (typeParams.isEmpty() ? "" : "<" + typeParams + "> ") + functionSignature(functionDefinition, false, true, false, "", processorContext, true) + "\n" +
                      "{\n" +
                      processFunctionDefinitionContent(functionDefinition, functionDefinition, true, processorContext, processorSupport) + "\n" +
                      "}";

        // Constraints
        if (functionDefinition.getValueForMetaPropertyToMany(M3Properties.preConstraints).notEmpty() ||
                functionDefinition.getValueForMetaPropertyToMany(M3Properties.postConstraints).notEmpty())
        {
            CoreInstance functionType = processorSupport.function_getFunctionType(functionDefinition);
            String returnType = TypeProcessor.typeToJavaPrimitiveWithMul(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport), true, processorContext);
            ListIterable<? extends CoreInstance> preConstraints = functionDefinition.getValueForMetaPropertyToMany(M3Properties.preConstraints);
            ListIterable<? extends CoreInstance> postConstraints = functionDefinition.getValueForMetaPropertyToMany(M3Properties.postConstraints);
            ListIterable<? extends CoreInstance> params = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);
            final String stringParams = params.collect(new Function<CoreInstance, Object>()
            {
                @Override
                public String valueOf(CoreInstance coreInstance)
                {
                    return  " _" + Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.name, processorContext.getSupport()).getName();
                }
            }).makeString(", ");

            String executor = "public static " + (typeParams.isEmpty() ? "" : "<" + typeParams + "> ") + functionSignature(functionDefinition, true, true, false, "_withConstraints", processorContext, true) + "\n" +
                              "{\n" +
                    preConstraints.collect(new Function<CoreInstance, String>()
                              {
                                  @Override
                                  public String valueOf(CoreInstance constraint)
                                  {
                                      CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorContext.getSupport());
                                      String eval = "(Boolean) "+ ValueSpecificationProcessor.createFunctionForLambda(constraint,definition,processorContext.getSupport(),processorContext)+".execute(Lists.mutable.with("+stringParams+"),es)";
                                      String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorContext.getSupport()).getName();
                                      return "if(! (" + eval + ")){throw new org.finos.legend.pure.m3.exception.PureExecutionException(_sourceInformation, \"Constraint (PRE):[" + ruleId + "] violated. (Function:" + functionDefinition.getName() + ")\");}\n";
                                  }
                              }).makeString("")+
                    "     final " + returnType + " _return = " + IdBuilder.sourceToId(functionDefinition.getSourceInformation()) + "." + functionNameToJava(functionDefinition) + "(" + functionType.getValueForMetaPropertyToMany("parameters").collect(new Function<CoreInstance, String>()
            {
                @Override
                public String valueOf(CoreInstance coreInstance)
                {
                    return "_"+coreInstance.getValueForMetaPropertyToOne(M3Properties.name).getName();
                }
            }).makeString(", ") + ", es);\n" +

                    postConstraints.collect(new Function<CoreInstance, String>()
                              {
                                  @Override
                                  public String valueOf(CoreInstance constraint)
                                  {
                                      CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, processorContext.getSupport());
                                      String eval = "(Boolean) "+ ValueSpecificationProcessor.createFunctionForLambda(constraint,definition,processorContext.getSupport(),processorContext)+".execute(Lists.mutable.with("+stringParams+",_return),es)";
                                      String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, processorContext.getSupport()).getName();
                                      return "if(! (" + eval + ")){throw new org.finos.legend.pure.m3.exception.PureExecutionException(_sourceInformation, \"Constraint (POST):[" + ruleId + "] violated. (Function:" + functionDefinition.getName() + ")\");}\n";
                                  }
                              }).makeString("")+

                              "return _return;"+
                              "}";
            func += executor;
        }
        processorContext.registerFunctionDefinition(functionDefinition, func);
        return func;
    }


    public static String functionSignature(CoreInstance func, boolean addSourceInformation, boolean fullName, boolean tail, String suffix, final ProcessorContext processorContext, final boolean typeParams)
    {
        final ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance functionType = processorSupport.function_getFunctionType(func);
        ListIterable<? extends CoreInstance> params = Instance.getValueForMetaPropertyToManyResolved(functionType, M3Properties.parameters, processorSupport);

        String stringParams = (tail ? ListHelper.tail(params) : params).collect(new Function<CoreInstance, Object>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {
                return "final " + TypeProcessor.typeToJavaPrimitiveWithMul(Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.genericType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.multiplicity, processorSupport), typeParams, processorContext) + " _" + Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.name, processorSupport).getName();
            }
        }).makeString(", ");

        return TypeProcessor.typeToJavaPrimitiveWithMul(Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport), Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport), typeParams, processorContext) + " " + (fullName ? functionNameToJava(func) : JavaTools.makeValidJavaIdentifier(func.getValueForMetaPropertyToOne(M3Properties.functionName).getName())) + suffix + "(" +

               (addSourceInformation?"org.finos.legend.pure.m4.coreinstance.SourceInformation _sourceInformation"+(stringParams.isEmpty()?"":","):"")+
               stringParams
               + (stringParams.isEmpty() && !addSourceInformation?"":",") + "final ExecutionSupport es"
               +  ")";
    }

    public static String buildExternalizableFunction(CoreInstance function, ProcessorContext processorContext)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        CoreInstance functionType = processorSupport.function_getFunctionType(function);

        StringBuilder builder = new StringBuilder(512);

        ListIterable<? extends CoreInstance> parameters = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
        CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);

        builder.append("    /**\n");
        builder.append("     * ");
        FunctionDescriptor.writeFunctionDescriptor(builder, function, processorSupport);
        builder.append('\n');
        builder.append("     */\n");
        builder.append("    public static ");
        builder.append(TypeProcessor.typeToJavaPrimitiveWithMul(returnType, returnMultiplicity, false, processorContext));
        builder.append(' ');
        builder.append(JavaTools.makeValidJavaIdentifier(PrimitiveUtilities.getStringValue(function.getValueForMetaPropertyToOne(M3Properties.functionName))));
        builder.append('(');
        boolean first = true;
        for (CoreInstance parameter : parameters)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(", ");
            }
            CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            builder.append(TypeProcessor.typeToJavaPrimitiveWithMul(type, multiplicity, false, processorContext));
            builder.append(' ');
            builder.append(JavaTools.makeValidJavaIdentifier(PrimitiveUtilities.getStringValue(parameter.getValueForMetaPropertyToOne(M3Properties.name)), "_"));
        }
        builder.append(")\n    {\n");
        for (CoreInstance parameter : parameters)
        {
            CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, processorSupport);
            if (Multiplicity.isToOne(multiplicity, true))
            {
                CoreInstance genericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
                if (!TypeProcessor.isJavaPrimitivePossible(genericType, processorSupport))
                {
                    String parameterName = JavaTools.makeValidJavaIdentifier(PrimitiveUtilities.getStringValue(parameter.getValueForMetaPropertyToOne(M3Properties.name)), "_");
                    builder.append("        if (").append(parameterName).append(" == null)\n");
                    builder.append("        {\n");
                    builder.append("            throw new IllegalArgumentException(\"").append(parameterName).append(" may not be null\");\n");
                    builder.append("        }\n");
                }
            }
            else if (Multiplicity.isZeroToMany(multiplicity))
            {
                String parameterName = JavaTools.makeValidJavaIdentifier(PrimitiveUtilities.getStringValue(parameter.getValueForMetaPropertyToOne(M3Properties.name)), "_");
                builder.append("        if (").append(parameterName).append(" == null)\n");
                builder.append("        {\n");
                builder.append("            ").append(parameterName).append(" = Lists.immutable.empty();\n");
                builder.append("        }\n");
            }
        }
        builder.append("try {\n");
        builder.append("        return ");
        builder.append(JavaPackageAndImportBuilder.rootPackage());
        builder.append('.');
        builder.append(IdBuilder.sourceToId(function.getSourceInformation()));
        builder.append('.');
        writeJavaFunctionName(builder, function);
        builder.append('(');
        for (CoreInstance parameter : parameters)
        {
            builder.append(JavaTools.makeValidJavaIdentifier(PrimitiveUtilities.getStringValue(parameter.getValueForMetaPropertyToOne(M3Properties.name)), "_"));
            builder.append(", ");
        }
        builder.append("_getExecutionSupport());\n");
        builder.append("}catch(Exception ex)");
        builder.append("{");
        builder.append("    org.finos.legend.pure.m4.exception.PureException pureException = org.finos.legend.pure.m4.exception.PureException.findPureException(ex);\n");
        builder.append("            if (pureException != null)\n");
        builder.append("            {\n");
        builder.append("                throw pureException;\n");
        builder.append("            }\n");
        builder.append("            else\n");
        builder.append("            {\n");
        builder.append("                throw new RuntimeException(\"Unexpected error executing function _validate\" , ex);\n");
        builder.append( "            }");
        builder.append("}");
        builder.append("    }");
        return builder.toString();
    }

    public static String functionNameToJava(CoreInstance function)
    {
        String result = PackageableElement.getSystemPathForPackageableElement(function, "_");
        return result.contains("~") ? UnitProcessor.convertToJavaCompatibleClassName(result) : result;
    }

    public static void writeJavaFunctionName(StringBuilder builder, CoreInstance function)
    {
        PackageableElement.writeSystemPathForPackageableElement(builder, function, "_");
    }

    private static boolean isTestFunction(CoreInstance functionDefinition, ProcessorSupport processorSupport)
    {
        final CoreInstance testStereotype = processorSupport.package_getByUserPath("meta::pure::profiles::test").getValueForMetaPropertyToMany("p_stereotypes").detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return "Test".equals(coreInstance.getName());
            }
        });
        return Instance.getValueForMetaPropertyToManyResolved(functionDefinition, "stereotypes", processorSupport).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return coreInstance.equals(testStereotype);
            }
        }) != null;
    }

    public static String processFunctionDefinitionContent(CoreInstance topLevelElement, CoreInstance functionDefinition, boolean returnValue, ProcessorContext processorContext, ProcessorSupport processorSupport)
    {
        ProcessorSupport support = processorContext.getSupport();

        CoreInstance functionType = support.function_getFunctionType(functionDefinition);
        CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, processorSupport);
        CoreInstance returnGenericType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);

        ListIterable<? extends CoreInstance> expressionSequence = Instance.getValueForMetaPropertyToManyResolved(functionDefinition, M3Properties.expressionSequence, processorSupport);
        int expressionCount = expressionSequence.size();
        MutableList<String> transformed = FastList.newList(expressionCount);
        for (int i = 0; i < expressionCount - 1; i++)
        {
            if (isIfExpression(expressionSequence.get(i), processorSupport) || isMatchExpression(expressionSequence.get(i), processorSupport))
            {
                transformed.add("Object unreferenced" + (isIfExpression(expressionSequence.get(i), processorSupport) ? "IfExpr_" : "MatchExpr_") + i + " = " + ValueSpecificationProcessor.processValueSpecification(topLevelElement, expressionSequence.get(i), true, processorContext));
            }
            else
            {
                String processed = ValueSpecificationProcessor.processValueSpecification(topLevelElement, expressionSequence.get(i), true, processorContext);
                transformed.add(shouldAssignToUnreferencedVariable(expressionSequence.get(i), processorSupport) ? "Object unreferenced" + i + " = " + processed : processed);
            }
        }

        CoreInstance lastExpression = expressionSequence.getLast();
        if (isLetFunctionExpression(lastExpression, processorSupport))
        {
            CoreInstance letRightSide = Instance.getValueForMetaPropertyToManyResolved(lastExpression, M3Properties.parametersValues, processorSupport).getLast();
            transformed.add(ValueSpecificationProcessor.possiblyConvertProcessedValueSpecificationToCollection(ValueSpecificationProcessor.processValueSpecification(topLevelElement, letRightSide, false, processorContext), returnMultiplicity, returnGenericType, processorContext.getSupport().valueSpecification_instanceOf(letRightSide, M3Paths.Nil) && returnGenericType != null && GenericType.isGenericTypeConcrete(returnGenericType, processorContext.getSupport()), processorContext));
        }
        else
        {
            transformed.add(ValueSpecificationProcessor.possiblyConvertProcessedValueSpecificationToCollection(ValueSpecificationProcessor.processValueSpecification(topLevelElement, lastExpression, false, processorContext), returnMultiplicity, returnGenericType, processorContext.getSupport().valueSpecification_instanceOf(lastExpression, M3Paths.Nil) && returnGenericType != null && GenericType.isGenericTypeConcrete(returnGenericType, processorContext.getSupport()), processorContext));
        }

        String body = ListHelper.init(transformed).makeString(";\n");

        if (returnValue)
        {
            body += (body.isEmpty() ? "" : ";\n") + "return " + transformed.getLast() + ";";
        }
        else
        {
            // No return value means no ";" (used in "bool?val1:val2" case)
            body += transformed.getLast();
        }
        return body;
    }

    private static boolean isIfExpression(CoreInstance expression, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport) != null && "if_Boolean_1__Function_1__Function_1__T_m_".equals(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport).getName());
    }

    private static boolean isMatchExpression(CoreInstance expression, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport) != null && "match_Any_MANY__Function_$1_MANY$__T_m_".equals(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport).getName());
    }

    public static boolean isLetFunctionExpression(CoreInstance expression, ProcessorSupport processorSupport)
    {
        return Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport) != null && "letFunction_String_1__T_m__T_m_".equals(Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport).getName());
    }

    public static boolean isNilReturnTypeFunctionExpression(CoreInstance expression, ProcessorSupport processorSupport)
    {
        CoreInstance func = Instance.getValueForMetaPropertyToOneResolved(expression, M3Properties.func, processorSupport);
        if(func != null)
        {
            CoreInstance returnGT = Instance.getValueForMetaPropertyToOneResolved(processorSupport.function_getFunctionType(func), M3Properties.returnType, processorSupport);
            CoreInstance returnType = Instance.getValueForMetaPropertyToOneResolved(returnGT, M3Properties.rawType, processorSupport);
            return Type.isBottomType(returnType, processorSupport);
        }
        return false;
    }

    private static boolean shouldAssignToUnreferencedVariable(CoreInstance expression, ProcessorSupport processorSupport)
    {
        return processorSupport.instance_instanceOf(expression, M3Paths.VariableExpression) || processorSupport.instance_instanceOf(expression, M3Paths.InstanceValue);
    }
}
