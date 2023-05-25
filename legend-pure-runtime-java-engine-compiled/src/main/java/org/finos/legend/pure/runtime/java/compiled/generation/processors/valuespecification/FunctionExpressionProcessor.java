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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.NativeFunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.SourceInfoProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;

public class FunctionExpressionProcessor
{
    public static String processFunctionExpression(CoreInstance topLevelElement, CoreInstance functionExpression, boolean topLevel, ProcessorContext processorContext)
    {
        ProcessorSupport support = processorContext.getSupport();

        CoreInstance function = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, support);
        if (support.instance_instanceOf(function, M3Paths.NativeFunction))
        {
            String functionCall = processorContext.getNativeFunctionProcessor().processNativeFunction(topLevelElement, functionExpression, processorContext);
            return new ExecWrapper(processorContext, functionCall, functionExpression).possiblyGeneratePureStackTrace();
        }
        if (support.instance_instanceOf(function, M3Paths.FunctionDefinition) && !support.instance_instanceOf(function, M3Paths.QualifiedProperty))
        {
            CoreInstance functionType = support.function_getFunctionType(function);
            // May want to be a little bit more specific (when a parameter has a type which is a functionType that has type parameters leveraged in the returnType)
            boolean shouldCast = !topLevel && !GenericType.isGenericTypeFullyConcrete(functionType.getValueForMetaPropertyToOne(M3Properties.returnType), support);
            String castType = null;
            CoreInstance genericType = functionExpression.getValueForMetaPropertyToOne(M3Properties.genericType);
            if (shouldCast)
            {
                castType = TypeProcessor.typeToJavaObjectWithMul(genericType, functionExpression.getValueForMetaPropertyToOne(M3Properties.multiplicity), support);
            }
            boolean addCastToOne = false;
            CoreInstance returnMultiplicity = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnMultiplicity, support);
            if (!Multiplicity.isMultiplicityConcrete(returnMultiplicity))
            {
                CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, support);
                addCastToOne = Multiplicity.isToOne(multiplicity, false);
            }

            String parameters = processFunctionParameterValues(topLevelElement, functionExpression, false, processorContext);
            String functionCall = function.getValueForMetaPropertyToMany(M3Properties.preConstraints).isEmpty() && function.getValueForMetaPropertyToMany(M3Properties.postConstraints).isEmpty() ?
                    FunctionProcessor.functionNameToJava(function) + "(" + parameters + ")" :
                    FunctionProcessor.functionNameToJava(function) + "_withConstraints(" + SourceInfoProcessor.sourceInfoToString(functionExpression.getSourceInformation()) + (parameters.isEmpty() ? "" : ",") + parameters + ")";
            String qualifiedFunctionCall = IdBuilder.sourceToId(function.getSourceInformation()) + "." + functionCall;
            String possiblyWrappedFunctionCall = new ExecWrapper(processorContext, qualifiedFunctionCall, functionExpression).withShouldCast(shouldCast).withCastType(castType).possiblyGeneratePureStackTrace();
            if (shouldCast && GenericType.isGenericTypeFullyConcrete(genericType, true, support))
            {
                castType = TypeProcessor.typeToJavaObjectSingle(genericType, true, support);
                String interfaceString = TypeProcessor.pureTypeToJava(genericType, false, false, true, support);
                SourceInformation sourceInformation = functionExpression.getSourceInformation();
                return "(CompiledSupport.<" + castType + ">castWithExceptionHandling(" + (addCastToOne ? "CompiledSupport.makeOne(" : "") + possiblyWrappedFunctionCall + (addCastToOne ? ")" : "") + "," + interfaceString + ".class, "
                        + NativeFunctionProcessor.buildM4LineColumnSourceInformation(sourceInformation) + "))";
            }
            return (shouldCast ? "((" + castType + ")" : "") + (addCastToOne ? "CompiledSupport.makeOne(" : "") + possiblyWrappedFunctionCall + (addCastToOne ? ")" : "") + (shouldCast ? ")" : "");
        }
        if (support.instance_instanceOf(function, M3Paths.Property))
        {
            CoreInstance firstParam = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport()).getFirst();
            String processedOwnerInstance = ValueSpecificationProcessor.processValueSpecification(topLevelElement, firstParam, processorContext);

            boolean addCastToOne = false;
            CoreInstance propertyMultiplicity = Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.multiplicity, support);
            if (!Multiplicity.isMultiplicityConcrete(propertyMultiplicity))
            {
                CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.multiplicity, support);
                addCastToOne = Multiplicity.isToOne(multiplicity, false);
            }

            CoreInstance rawTypeParam = Instance.getValueForMetaPropertyToOneResolved(firstParam, M3Properties.genericType, M3Properties.rawType, support);

            String varName = "this".equals(processedOwnerInstance) ? topLevelElement != null && support.instance_instanceOf(topLevelElement, M3Paths.Constraint) ? "_this" : "" : processedOwnerInstance;
            //If the type is Any, then the instance will be of type Object, so we need to cast here
            varName = !varName.isEmpty() && rawTypeParam != null && Type.isTopType(rawTypeParam, support) ? "((" + FullJavaPaths.Any + ")" + varName + ")" : varName;
            varName = varName.isEmpty() ? varName : varName + ".";
            return varName + "_" + Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.name, support).getName() + "()" + (addCastToOne ? ".getFirst()" : "");
        }
        if (support.instance_instanceOf(function, M3Paths.QualifiedProperty))
        {
            CoreInstance firstParam = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport()).getFirst();
            String processedOwnerInstance = ValueSpecificationProcessor.processValueSpecification(topLevelElement, firstParam, processorContext);
            return ("this".equals(processedOwnerInstance) ? "" : processedOwnerInstance + ".") +
                    JavaTools.makeValidJavaIdentifier(Instance.getValueForMetaPropertyToOneResolved(function, M3Properties.name, support).getName())
                    + "(" +
                    processFunctionParameterValues(topLevelElement, functionExpression, true, processorContext)
                    + ")";
        }
        throw new RuntimeException("To Code! " + function.print(""));
    }

    private static String processFunctionParameterValues(CoreInstance topLevelElement, CoreInstance functionExpression, boolean qualifier, ProcessorContext processorContext)
    {
        ProcessorSupport support = processorContext.getSupport();
        MutableList<String> result = Lists.mutable.empty();

        CoreInstance function = Instance.getValueForMetaPropertyToOneResolved(functionExpression, M3Properties.func, support);
        ListIterable<? extends CoreInstance> parameters = Instance.getValueForMetaPropertyToManyResolved(processorContext.getSupport().function_getFunctionType(function), M3Properties.parameters, processorContext.getSupport());
        ListIterable<? extends CoreInstance> parameterValues = Instance.getValueForMetaPropertyToManyResolved(functionExpression, M3Properties.parametersValues, processorContext.getSupport());

        int parameterCount = parameters.size();
        int index = qualifier ? 1 : 0;
        if (parameterCount > index)
        {
            for (; index < parameterCount; index++)
            {
                CoreInstance parameter = parameters.get(index);
                CoreInstance parameterMultiplicity = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.multiplicity, support);
                CoreInstance parameterGenericType = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, support);
                CoreInstance parameterValue = parameterValues.get(index);
                result.add(ValueSpecificationProcessor.possiblyConvertProcessedValueSpecificationToCollection(ValueSpecificationProcessor.processValueSpecification(topLevelElement, parameterValue, processorContext), parameterMultiplicity, parameterGenericType, processorContext.getSupport().valueSpecification_instanceOf(parameterValue, M3Paths.Nil) && parameterGenericType != null && GenericType.isGenericTypeConcrete(parameterGenericType), processorContext));
            }
        }
        return result.with("es").makeString(",");
    }

    private static class ExecWrapper
    {
        private final ProcessorContext processorContext;
        private final String functionCall;
        private final CoreInstance functionExpression;
        private String castType;
        private boolean shouldCast;

        public ExecWrapper(ProcessorContext processorContext, String functionCall, CoreInstance functionExpression)
        {
            this.processorContext = processorContext;
            this.functionCall = functionCall;
            this.functionExpression = functionExpression;
        }

        public ExecWrapper withCastType(String castType)
        {
            this.castType = castType;
            return this;
        }

        public ExecWrapper withShouldCast(boolean shouldCast)
        {
            this.shouldCast = shouldCast;
            return this;
        }


        protected String possiblyGeneratePureStackTrace()
        {
            if (this.processorContext.includePureStackTrace())
            {
                return this.isNotWrappable()
                        ? this.functionCall : this.wrapInExec();
            }
            else
            {
                return this.functionCall;
            }
        }

        private boolean isNotWrappable()
        {
            return FunctionProcessor.isLetFunctionExpression(this.functionExpression, this.processorContext.getSupport())
                    || FunctionProcessor.isNilReturnTypeFunctionExpression(this.functionExpression, this.processorContext.getSupport());
        }

        private String wrapInExec()
        {
            CoreInstance multiplicity = this.functionExpression.getValueForMetaPropertyToOne(M3Properties.multiplicity);
            CoreInstance genericType = this.functionExpression.getValueForMetaPropertyToOne(M3Properties.genericType);
            String returnType = TypeProcessor.typeToJavaObjectWithMul(genericType, multiplicity, this.processorContext.getSupport());
            String processedFunctionCall = ValueSpecificationProcessor.possiblyConvertProcessedValueSpecificationToCollection(this.functionCall, multiplicity, genericType, false, this.processorContext);
            processedFunctionCall = this.shouldCast ? castFunctionCall(this.functionCall, this.castType) : processedFunctionCall;
            return this.generateExecStringWithPureStacktrace(processedFunctionCall, returnType);
        }

        private String generateExecStringWithPureStacktrace(String processedFunctionCall, String returnType)
        {
            return "E_.e_(new DefendedFunction0<" + returnType + ">()" +
                    "{" +
                    "@Override public " + returnType + " value()" +
                    "{ return " + processedFunctionCall + ";" +
                    "}}, \"" + this.functionExpression.getSourceInformation().getSourceId() + "\", "
                    + this.functionExpression.getSourceInformation().getLine() + ", "
                    + this.functionExpression.getSourceInformation().getColumn() + ", "
                    + this.functionExpression.getSourceInformation().getEndLine() + ", "
                    + this.functionExpression.getSourceInformation().getEndColumn() + ")";
        }

        private static String castFunctionCall(String functionCall, String castType)
        {
            return "(" + castType + ") " + functionCall;
        }
    }
}
