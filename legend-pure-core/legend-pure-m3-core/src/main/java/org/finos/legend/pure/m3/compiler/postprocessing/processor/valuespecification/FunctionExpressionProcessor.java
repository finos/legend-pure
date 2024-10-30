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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.set.PartitionSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState.MilestoningDateContextScope;
import org.finos.legend.pure.m3.compiler.postprocessing.VariableContext.VariableNameConflictException;
import org.finos.legend.pure.m3.compiler.postprocessing.functionmatch.FunctionExpressionMatcher;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInference;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceContext;
import org.finos.legend.pure.m3.compiler.postprocessing.inference.TypeInferenceObserver;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.FunctionDefinitionProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestonedPropertyMetaData;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningDatesPropagationFunctions;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.compiler.unload.Unbinder;
import org.finos.legend.pure.m3.compiler.unload.unbind.UnbindState;
import org.finos.legend.pure.m3.compiler.visibility.Visibility;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.PackageableFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.KeyValueValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ParameterValueSpecificationContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.Objects;
import java.util.Optional;

public class FunctionExpressionProcessor extends Processor<FunctionExpression>
{
    @Override
    public String getClassName()
    {
        return M3Paths.FunctionExpression;
    }

    private static class FunctionMatchResult
    {
        IntSet parametersRequiringTypeInference;
        String functionName;
        MutableList<Function<?>> foundFunctions;
        ListIterable<? extends ValueSpecification> parametersValues;

        public FunctionMatchResult(MutableList<Function<?>> foundFunctions, IntSet parametersRequiringTypeInference, String functionName, ListIterable<? extends ValueSpecification> parametersValues)
        {
            this.parametersRequiringTypeInference = parametersRequiringTypeInference;
            this.functionName = functionName;
            this.foundFunctions = foundFunctions;
            this.parametersValues = parametersValues;
        }
    }

    @Override
    public void process(FunctionExpression functionExpression, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        TypeInferenceObserver observer = state.getObserver();
        state.pushTypeInferenceContext();

        FunctionMatchResult matchResult = matchFunction(functionExpression, state, matcher, repository, context, processorSupport);

        Function<?> finalFunction = null;
        boolean someInferenceFailed = false;

        for (Function<?> foundFunction : matchResult.foundFunctions)
        {
            functionExpression._funcRemove();
            functionExpression._funcCoreInstance(foundFunction);

            state.getTypeInferenceContext().setScope(foundFunction);
            FunctionType foundFunctionType = (FunctionType) processorSupport.function_getFunctionType(foundFunction);

            observer.functionMatched(foundFunction, foundFunctionType);

            // SECOND PASS
            ListIterable<? extends VariableExpression> paramsType = ListHelper.wrapListIterable(foundFunctionType._parameters());
            // enumValues, autoMaps, etc...
            ListIterable<? extends ValueSpecification> parametersValues = ListHelper.wrapListIterable(functionExpression._parametersValues());

            boolean lambdaParametersInferenceSuccess = true;
            boolean columnTypeInferenceSuccess = true;

            if (matchResult.parametersRequiringTypeInference.notEmpty())
            {
                observer.firstPassInferenceFailed().shiftTab(2).matchTypeParamsFromFoundFunction(foundFunction).shiftTab();

                potentiallyUpdateTypeInferenceContextUsingFunctionSignature(parametersValues, paramsType, observer, state, processorSupport);

                observer.unShiftTab().reverseMatching().shiftTab();
                for (int z = 0; z < parametersValues.size(); z++)
                {
                    ValueSpecification instance = parametersValues.get(z);
                    observer.processingParameter(functionExpression, z, instance).shiftTab();

                    VariableExpression templateVariable = ListHelper.wrapListIterable(Objects.requireNonNull(getRawTypeFromGenericType(foundFunction, processorSupport))._parameters()).get(z);
                    GenericType templateGenericType = templateVariable._genericType();

                    if (isColumnWithEmptyType(instance, processorSupport))
                    {
                        columnTypeInferenceSuccess = processEmptyColumnType(templateGenericType, instance, paramsType, z, functionExpression.getSourceInformation(), observer, state, processorSupport);
                    }
                    else if (isLambdaWithEmptyParamType(instance, processorSupport))
                    {
                        lambdaParametersInferenceSuccess = processLambda(functionExpression, state, matcher, repository, context, processorSupport, foundFunction, z, observer, instance, lambdaParametersInferenceSuccess, templateGenericType);
                    }
                    else if (matchResult.parametersRequiringTypeInference.contains(z))
                    {
                        handleParameter(state, matcher, repository, context, processorSupport, observer, paramsType, z, templateGenericType, instance);
                    }
                    observer.unShiftTab();
                }
                observer.unShiftTab(3);
            }
            else
            {
                observer.parameterInferenceSucceeded().shiftTab(2);

                updateTypeInferenceContextUsingFunctionSignature(parametersValues, paramsType, observer, state);

                // WARNING / returnType may need reverse matching to be found
                GenericType returnGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
                observer.returnType(returnGenericType);
                if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(returnGenericType) && !state.getTypeInferenceContext().isTop(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(returnGenericType)))
                {
                    updateTypeInferenceContextSoThatReturnTypeIsConcrete(functionExpression, state, matcher, repository, context, processorSupport, observer, parametersValues, paramsType, foundFunctionType);
                }
                observer.unShiftTab(2).finishedRegisteringParametersAndMultiplicities();
            }

            columnTypeInferenceSuccess = manageMagicColumnFunctions(functionExpression, foundFunction, columnTypeInferenceSuccess, state.getTypeInferenceContext(), observer, processorSupport);

            updateFunctionExpressionReturnTypeAndMultiplicity(functionExpression, state, processorSupport, foundFunction, lambdaParametersInferenceSuccess && columnTypeInferenceSuccess, foundFunctionType, observer);

            if (matchResult.functionName == null)
            {
                finalFunction = foundFunction;
            }
            else if (!lambdaParametersInferenceSuccess || !columnTypeInferenceSuccess)
            {
                someInferenceFailed = true;
            }
            else
            {
                Function<?> bestMatch = FunctionExpressionMatcher.getBestFunctionMatch(matchResult.foundFunctions, parametersValues, matchResult.functionName, functionExpression.getSourceInformation(), false, processorSupport);
                if (bestMatch == foundFunction)
                {
                    finalFunction = foundFunction;
                }
            }

            if (finalFunction != null)
            {
                break;
            }

            // Clean up before re-trying
            if (matchResult.foundFunctions.size() > 1)
            {
                parametersValues.forEach(pv -> cleanProcess(pv, state, repository, context, processorSupport));
                matchResult.parametersRequiringTypeInference = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
            }
        }


        if (finalFunction != null)
        {
            finalFunction._applications(Lists.immutable.<FunctionExpression>withAll(finalFunction._applications()).newWith(functionExpression));

            // Update the function in the function expression in the reverse
            if ("new_Class_1__String_1__KeyExpression_MANY__T_1_".equals(finalFunction.getName()) || "new_Class_1__String_1__T_1_".equals(finalFunction.getName()))
            {
                this.addTraceForKeyExpressions(functionExpression, processorSupport);
            }

            if ("copy_T_1__String_1__KeyExpression_MANY__T_1_".equals(finalFunction.getName()) || "copy_T_1__String_1__T_1_".equals(finalFunction.getName()))
            {
                this.addTraceForKeyExpressions(functionExpression, processorSupport);
            }

            if ("letFunction_String_1__T_m__T_m_".equals(finalFunction.getName()))
            {
                try
                {
                    state.getVariableContext().getParent().registerValue(((InstanceValue) matchResult.parametersValues.get(0))._valuesCoreInstance().getAny().getName(), matchResult.parametersValues.get(1));
                }
                catch (VariableNameConflictException e)
                {
                    throw new PureCompilationException(functionExpression.getSourceInformation(), e.getMessage());
                }
            }
        }
        else if (!someInferenceFailed)
        {
            throwNoMatchException(functionExpression, state, processorSupport);
        }

        observer.unShiftTab().finishedProcessingFunctionExpression(functionExpression);
        state.popTypeInferenceContext();
    }

    private FunctionMatchResult matchFunction(FunctionExpression functionExpression, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        ListIterable<? extends ValueSpecification> parametersValues = ListHelper.wrapListIterable(functionExpression._parametersValues());

        // Process the function's parameters (FIRST PASS)
        IntSet parametersRequiringTypeInference = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);

        // Function matching
        MutableList<Function<?>> foundFunctions = Lists.mutable.empty();
        String functionName = null;
        if (functionExpression._funcCoreInstance() != null)
        {
            foundFunctions.add((Function<?>) ImportStub.withImportStubByPass(functionExpression._funcCoreInstance(), processorSupport));
        }
        else
        {
            // Check if the function is a property
            InstanceValue propertyNameInstanceVal = functionExpression._propertyName();
            if (propertyNameInstanceVal != null)
            {
                ValueSpecification source = parametersValues.get(0);

                String propertyName = ImportStub.withImportStubByPass(propertyNameInstanceVal._valuesCoreInstance().getAny(), processorSupport).getName();
                GenericType sourceGenericType = extractAndValidateGenericType(propertyName, source);

                //Is it an enum?
                if (org.finos.legend.pure.m3.navigation.generictype.GenericType.subTypeOf(sourceGenericType, org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Enumeration), processorSupport), processorSupport))
                {
                    reprocessEnumValueInExtractEnumValue(functionExpression, propertyName, state, repository, processorSupport);
                }
                else if (_RelationType.isRelationType(sourceGenericType._rawType(), processorSupport))
                {
                    String name = functionExpression._propertyName()._valuesCoreInstance().getAny().getName();
                    foundFunctions.add(_RelationType.findColumn((RelationType<?>) sourceGenericType._rawType(), name, functionExpression.getSourceInformation(), processorSupport));
                }
                else
                {
                    Multiplicity sourceMultiplicity = source._multiplicity();
                    if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(sourceMultiplicity, true))
                    {
                        AbstractProperty<?> propertyFunc = findFunctionForPropertyBasedOnMultiplicity(functionExpression, sourceGenericType, state, processorSupport, matcher);
                        if (MilestoningFunctions.isGeneratedMilestonedQualifiedPropertyWithMissingDates(propertyFunc, processorSupport))
                        {
                            propertyFunc = (AbstractProperty<?>) MilestoningDatesPropagationFunctions.getMilestoningQualifiedPropertyWithAllDatesSupplied(functionExpression, state, repository, context, processorSupport, propertyNameInstanceVal, source, propertyName, propertyFunc);
                        }
                        foundFunctions.add(propertyFunc);
                    }
                    else
                    {
                        //Automap
                        reprocessPropertyForManySources(functionExpression, parametersValues, M3Properties.propertyName, sourceGenericType, repository, processorSupport);
                        //The parameters values are now different, so update
                        parametersValues = ListHelper.wrapListIterable(functionExpression._parametersValues());
                        //Have another go at type inference
                        parametersRequiringTypeInference = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
                        //return;
                    }
                }
            }
            // Check if the function is a qualifiedProperty
            else
            {
                InstanceValue qualifiedPropertyNameVal = functionExpression._qualifiedPropertyName();
                if (qualifiedPropertyNameVal != null)
                {
                    ValueSpecification source = parametersValues.get(0);

                    String qualifiedPropertyName = ImportStub.withImportStubByPass(qualifiedPropertyNameVal._valuesCoreInstance().getAny(), processorSupport).getName();
                    GenericType sourceGenericType = extractAndValidateGenericType(qualifiedPropertyName, source);

                    Multiplicity sourceMultiplicity = source._multiplicity();

                    if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(sourceMultiplicity, true))
                    {
                        ListIterable<QualifiedProperty<?>> qualifiedPropertyFuncs = findFunctionsForQualifiedPropertyBasedOnMultiplicity(functionExpression, sourceGenericType, parametersValues, processorSupport, matcher, state);
                        if (qualifiedPropertyFuncs.size() == 1 && MilestoningFunctions.isGeneratedMilestonedQualifiedPropertyWithMissingDates(qualifiedPropertyFuncs.getFirst(), processorSupport))
                        {
                            Function<?> mqp = (Function<?>) MilestoningDatesPropagationFunctions.getMilestoningQualifiedPropertyWithAllDatesSupplied(functionExpression, state, repository, context, processorSupport, qualifiedPropertyNameVal, source, qualifiedPropertyName, qualifiedPropertyFuncs.getFirst());
                            foundFunctions.add(mqp);
                        }
                        else
                        {
                            foundFunctions.addAllIterable(qualifiedPropertyFuncs);
                        }
                    }
                    else
                    {
                        //Automap
                        reprocessPropertyForManySources(functionExpression, parametersValues, M3Properties.qualifiedPropertyName, sourceGenericType, repository, processorSupport);
                        //The parameters values are now different, so update
                        parametersValues = ListHelper.wrapListIterable(functionExpression._parametersValues());
                        //Have another go at type inference
                        parametersRequiringTypeInference = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
                    }

                }
            }
            if (foundFunctions.isEmpty())
            {
                // Match the functionExpression with the Function library (may still need to do it even if the function is a property because it may have been reprocessed as a Collect!)
                foundFunctions.addAllIterable(FunctionExpressionMatcher.findMatchingFunctionsInTheRepository(functionExpression, true, processorSupport));
                functionName = getFunctionName(functionExpression);
            }
        }
        return new FunctionMatchResult(foundFunctions, parametersRequiringTypeInference, functionName, parametersValues);
    }

    private static boolean manageMagicColumnFunctions(FunctionExpression functionExpression, Function<?> foundFunction, boolean columnTypeInferenceSuccess, TypeInferenceContext ctx, TypeInferenceObserver observer, ProcessorSupport processorSupport)
    {
        if ("funcColSpecArray_FuncColSpec_MANY__P_1__FuncColSpecArray_1_".equals(foundFunction.getName()) || "funcColSpecArray2_FuncColSpec_MANY__P_1__FuncColSpecArray_1_".equals(foundFunction.getName()))
        {
            MutableList<ValueSpecification> parameters = Lists.mutable.withAll(functionExpression._parametersValues());
            MutableList<? extends Column<?, ?>> found = ((InstanceValue) parameters.get(0))._values().collect(v ->
            {
                Type relationType = ((ValueSpecification) v)._genericType()._typeArguments().toList().getLast()._rawType();
                return relationType == null ? null : ((RelationType<?>) relationType)._columns().getFirst();
            }, Lists.mutable.empty());
            if (found.contains(null))
            {
                columnTypeInferenceSuccess = false;
            }
            else
            {
                ctx.register((GenericType) processorSupport.function_getFunctionType(foundFunction).getValueForMetaPropertyToMany("parameters").get(1).getValueForMetaPropertyToOne("genericType"), (GenericType) processorSupport.type_wrapGenericType(_RelationType.build(found.collect(foundC -> _Column.getColumnInstance(foundC._name(), false, _Column.getColumnType(foundC), _Column.getColumnMultiplicity(foundC), functionExpression.getSourceInformation(), processorSupport)), null, processorSupport)), ctx, observer);
                columnTypeInferenceSuccess = true;
            }
        }
        if ("funcColSpec_Function_1__String_1__T_1__FuncColSpec_1_".equals(foundFunction.getName()) || "funcColSpec2_Function_1__String_1__T_1__FuncColSpec_1_".equals(foundFunction.getName()))
        {
            MutableList<ValueSpecification> parameters = Lists.mutable.withAll(functionExpression._parametersValues());
            MutableList<CoreInstance> lambdas = Lists.mutable.withAll(parameters.get(0).getValueForMetaPropertyToMany("values"));
            MutableList<CoreInstance> columns = Lists.mutable.withAll(parameters.get(2).getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType").getValueForMetaPropertyToMany("columns"));
            if (lambdas.size() != columns.size())
            {
                throw new PureCompilationException("Error while processing funcColSpecArray. The lambda count is different from the column number (" + lambdas.size() + " != " + columns.size() + ")");
            }
            columnTypeInferenceSuccess = true;
            for (int i = 0; i < lambdas.size(); i++)
            {
                CoreInstance lambdaReturnType = lambdas.get(i).getValueForMetaPropertyToMany("expressionSequence").getLast().getValueForMetaPropertyToOne("genericType");
                if (lambdaReturnType != null)
                {
                    CoreInstance columnGenericType = _Column.getColumnType((Column<?, ?>) columns.get(i));
                    columnGenericType.setKeyValues(Lists.mutable.with("rawType"), Lists.mutable.with(lambdaReturnType.getValueForMetaPropertyToOne("rawType")));
                    ctx.register((GenericType) processorSupport.function_getFunctionType(foundFunction).getValueForMetaPropertyToMany("parameters").get(2).getValueForMetaPropertyToOne("genericType"), (GenericType) parameters.get(2).getValueForMetaPropertyToOne("genericType"), ctx, observer);
                }
                else
                {
                    columnTypeInferenceSuccess = false;
                }
            }
        }
        if ("aggColSpec_Function_1__Function_1__String_1__T_1__AggColSpec_1_".equals(foundFunction.getName()) || "aggColSpec2_Function_1__Function_1__String_1__T_1__AggColSpec_1_".equals(foundFunction.getName()))
        {
            MutableList<ValueSpecification> parameters = Lists.mutable.withAll(functionExpression._parametersValues());
            CoreInstance reduceLambda = parameters.get(1).getValueForMetaPropertyToOne("values");
            CoreInstance column = parameters.get(3).getValueForMetaPropertyToOne("genericType").getValueForMetaPropertyToOne("rawType").getValueForMetaPropertyToOne("columns");
            CoreInstance lambdaReturnType = reduceLambda.getValueForMetaPropertyToMany("expressionSequence").getLast().getValueForMetaPropertyToOne("genericType");
            if (lambdaReturnType != null)
            {
                CoreInstance columnGenericType = _Column.getColumnType((Column<?, ?>) column);
                columnGenericType.setKeyValues(Lists.mutable.with("rawType"), Lists.mutable.with(lambdaReturnType.getValueForMetaPropertyToOne("rawType")));
                ctx.register((GenericType) processorSupport.function_getFunctionType(foundFunction).getValueForMetaPropertyToMany("parameters").get(3).getValueForMetaPropertyToOne("genericType"), (GenericType) parameters.get(3).getValueForMetaPropertyToOne("genericType"), ctx, observer);
                columnTypeInferenceSuccess = true;
            }
        }
        if ("aggColSpecArray_AggColSpec_MANY__P_1__AggColSpecArray_1_".equals(foundFunction.getName()) || "aggColSpecArray2_AggColSpec_MANY__P_1__AggColSpecArray_1_".equals(foundFunction.getName()))
        {
            MutableList<ValueSpecification> parameters = Lists.mutable.withAll(functionExpression._parametersValues());
            MutableList<? extends Column<?, ?>> found = ((InstanceValue) parameters.get(0))._values().collect(v ->
            {
                Type relationType = ((ValueSpecification) v)._genericType()._typeArguments().toList().getLast()._rawType();
                return relationType == null ? null : ((RelationType<?>) relationType)._columns().getFirst();
            }, Lists.mutable.empty());
            if (found.contains(null))
            {
                columnTypeInferenceSuccess = false;
            }
            else
            {
                ctx.register((GenericType) processorSupport.function_getFunctionType(foundFunction).getValueForMetaPropertyToMany("parameters").get(1).getValueForMetaPropertyToOne("genericType"), (GenericType) processorSupport.type_wrapGenericType(_RelationType.build(found.collect(foundC -> _Column.getColumnInstance(foundC._name(), false, _Column.getColumnType(foundC), _Column.getColumnMultiplicity(foundC), functionExpression.getSourceInformation(), processorSupport)), null, processorSupport)), ctx, observer);
                columnTypeInferenceSuccess = true;
            }
        }
        return columnTypeInferenceSuccess;
    }

    private void updateTypeInferenceContextSoThatReturnTypeIsConcrete(FunctionExpression functionExpression, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport, TypeInferenceObserver observer, ListIterable<? extends ValueSpecification> parametersValues, ListIterable<? extends VariableExpression> paramsType, FunctionType foundFunctionType)
    {
        observer.shiftTab().returnTypeNotConcrete();

        // reverse matching
        parametersValues.forEachWithIndex((instance, z) ->
        {
            GenericType templateGenType = paramsType.get(z)._genericType();
            Multiplicity templateMultiplicity = paramsType.get(z)._multiplicity();
            GenericType resolvedGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(templateGenType, state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
            Multiplicity resolvedMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(templateMultiplicity, state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());

            state.pushTypeInferenceContextAhead();
            state.getTypeInferenceContext().setScope(instance instanceof FunctionExpression ? ((FunctionExpression) instance)._funcCoreInstance() : null);
            observer.processingParameter(functionExpression, z, instance).shiftTab();
            TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
            typeInferenceContext.register(instance._genericType(), resolvedGenericType, typeInferenceContext.getParent(), observer);
            typeInferenceContext.registerMul(instance._multiplicity(), resolvedMultiplicity, typeInferenceContext.getParent(), observer);

            observer.reprocessingTheParameter().shiftTab();

            // TODO do we need to clean here?
            cleanProcess(instance, state, repository, context, processorSupport);
            PostProcessor.processElement(matcher, instance, state, processorSupport);

            observer.unShiftTab().finishedProcessParameter().unShiftTab();

            state.popTypeInferenceContextAhead();
        });
        observer.unShiftTab();

        observer.newReturnType(org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport));
    }


    private static void updateFunctionExpressionReturnTypeAndMultiplicity(FunctionExpression functionExpression, ProcessorState state, ProcessorSupport processorSupport, Function<?> foundFunction, boolean parametersInferenceSuccess, FunctionType foundFunctionType, TypeInferenceObserver observer)
    {
        if (parametersInferenceSuccess)
        {
            TypeInference.storeInferredTypeParametersInFunctionExpression(functionExpression, state, processorSupport, foundFunction, observer);

            // Get the return type information
            GenericType returnGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType().reject((s, gt) -> org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeOperationEqual((GenericType) gt)), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
            Multiplicity returnMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(foundFunctionType._returnMultiplicity(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());

            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(returnGenericType) && !state.getTypeInferenceContext().isTop(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(returnGenericType)))
            {
                CoreInstance func = functionExpression.getValueForMetaPropertyToOne(M3Properties.func);
                String funcType = (func instanceof Property) ? "property" : ((func instanceof QualifiedProperty) ? "qualified property" : "function");
                CoreInstance funcName = func.getValueForMetaPropertyToOne("property".equals(funcType) ? M3Properties.name : M3Properties.functionName);
                throw new PureCompilationException(functionExpression.getSourceInformation(), "The system is not capable of inferring the return type (" + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(returnGenericType, processorSupport) + ") of the " + funcType + " '" + PrimitiveUtilities.getStringValue(funcName) + "'. Check your signatures!");
            }

            // Update the type
            GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(returnGenericType, functionExpression.getSourceInformation(), processorSupport);
            functionExpression._genericType(genericTypeCopy);

            // Update the multiplicity
            Multiplicity returnMultiplicityCopy = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.copyMultiplicity(returnMultiplicity, functionExpression.getSourceInformation(), processorSupport);
            functionExpression._multiplicity(returnMultiplicityCopy);
            observer.updateFunctionExpressionReturn(parametersInferenceSuccess, genericTypeCopy, returnMultiplicityCopy);
        }
        else
        {
            // Inference failed...
            FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(foundFunction);
            GenericType returnGenericType = functionType._returnType();
            GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(returnGenericType, functionExpression.getSourceInformation(), processorSupport);
            functionExpression._genericType(genericTypeCopy);
            Multiplicity returnMultiplicity = functionType._returnMultiplicity();
            Multiplicity returnMultiplicityCopy = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.copyMultiplicity(returnMultiplicity, functionExpression.getSourceInformation(), processorSupport);
            functionExpression._multiplicity(returnMultiplicityCopy);
            observer.updateFunctionExpressionReturn(parametersInferenceSuccess, genericTypeCopy, returnMultiplicityCopy);
        }
    }

    private static void potentiallyUpdateTypeInferenceContextUsingFunctionSignature(ListIterable<? extends ValueSpecification> parametersValues, ListIterable<? extends VariableExpression> paramsType, TypeInferenceObserver observer, ProcessorState state, ProcessorSupport processorSupport)
    {
        parametersValues.forEachWithIndex((instance, z) ->
        {
            boolean isInferenceSuccess = isInferenceSuccess(instance, processorSupport);
            if (isInferenceSuccess)
            {
                observer.matchParam(z);
                TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                typeInferenceContext.register(paramsType.get(z)._genericType(), instance._genericType(), typeInferenceContext.getTopContext(), observer);
                typeInferenceContext.registerMul(paramsType.get(z)._multiplicity(), instance._multiplicity(), typeInferenceContext.getTopContext(), observer);
            }
            else
            {
                observer.paramInferenceFailed(z);
            }
        });
    }

    private static void updateTypeInferenceContextUsingFunctionSignature(ListIterable<? extends ValueSpecification> parametersValues, ListIterable<? extends VariableExpression> paramsType, TypeInferenceObserver observer, ProcessorState state)
    {
        parametersValues.forEachWithIndex((instance, z) ->
        {
            TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
            typeInferenceContext.register(paramsType.get(z)._genericType(), instance._genericType(), typeInferenceContext.getTopContext(), true, observer);
            typeInferenceContext.registerMul(paramsType.get(z)._multiplicity(), instance._multiplicity(), typeInferenceContext.getTopContext(), observer);
        });
    }

    private void handleParameter(ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport, TypeInferenceObserver observer, ListIterable<? extends VariableExpression> paramsType, int z, GenericType templateGenericType, ValueSpecification instance)
    {
        observer.reprocessingTheParameter().shiftTab();

        GenericType templateGenType = paramsType.get(z)._genericType();
        Multiplicity templateMultiplicity = paramsType.get(z)._multiplicity();
        GenericType resolvedGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(templateGenType, state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
        Multiplicity resolvedMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(templateMultiplicity, state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());

        state.pushTypeInferenceContextAhead();
        TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
        typeInferenceContext.setScope(instance instanceof SimpleFunctionExpression ? ((SimpleFunctionExpression) instance)._funcCoreInstance() : null);

        typeInferenceContext.register(instance._genericType(), resolvedGenericType, typeInferenceContext.getParent(), observer);
        typeInferenceContext.registerMul(instance._multiplicity(), resolvedMultiplicity, typeInferenceContext.getParent(), observer);
        cleanProcess(instance, state, repository, context, processorSupport);
        PostProcessor.processElement(matcher, instance, state, processorSupport);
        observer.unShiftTab();

        state.popTypeInferenceContextAhead();
    }

    private static boolean processLambda(FunctionExpression functionExpression, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport, Function<?> foundFunction, int z, TypeInferenceObserver observer, ValueSpecification instance, boolean lambdaParametersInferenceSuccess, GenericType templateGenericType)
    {
        VariableExpression templateToMatchLambdaTo = ListHelper.wrapListIterable(Objects.requireNonNull(getRawTypeFromGenericType(foundFunction, processorSupport))._parameters()).get(z);
        observer.register(templateToMatchLambdaTo, templateToMatchLambdaTo, state.getTypeInferenceContext(), state.getTypeInferenceContext());

        for (CoreInstance val : ((InstanceValue) instance)._valuesCoreInstance())
        {
            if (val instanceof LambdaFunction)
            {
                try (MilestoningDateContextScope ignore = MilestoningDatesPropagationFunctions.withNewMilestoningDateContext(functionExpression, val, state, repository, context, processorSupport))
                {
                    lambdaParametersInferenceSuccess = lambdaParametersInferenceSuccess && !TypeInference.processParamTypesOfLambdaUsedAsAFunctionExpressionParamValue(instance, (LambdaFunction<?>) val, templateToMatchLambdaTo, matcher, state, repository, processorSupport);
                }

                // Manage return type in any case
                ClassInstance functionClass = (ClassInstance) processorSupport.package_getByUserPath(M3Paths.Function);
                if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateGenericType) && org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(ImportStub.withImportStubByPass(templateGenericType._rawTypeCoreInstance(), processorSupport), functionClass, processorSupport))
                {
                    GenericType templateGenFunctionType = ListHelper.wrapListIterable(templateGenericType._typeArguments()).get(0);
                    if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateGenFunctionType) && !org.finos.legend.pure.m3.navigation.type.Type.isTopType(Instance.getValueForMetaPropertyToOneResolved(templateGenFunctionType, M3Properties.rawType, processorSupport), processorSupport))
                    {
                        GenericType templateReturnType = Optional.ofNullable(ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport)).map(i -> ((FunctionType) i)._returnType()).orElse(null);

                        // Generics in lambdas are relative to their environment (i.e. the function in which they are defined)
                        TypeInferenceContext lambdaInferenceContext = state.getTypeInferenceContext().getTopContext();

                        if (templateReturnType != null)
                        {
                            FunctionType lambdaFunctionType = Objects.requireNonNull(getRawTypeFromGenericType((LambdaFunction<?>) val, processorSupport));
                            GenericType concreteGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(lambdaFunctionType._returnType(), lambdaInferenceContext.getTypeParameterToGenericType(), lambdaInferenceContext.getMultiplicityParameterToMultiplicity(), processorSupport);
                            lambdaFunctionType._returnTypeRemove();
                            lambdaFunctionType._returnType(concreteGenericType);
                            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateReturnType))
                            {
                                TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                                typeInferenceContext.register(templateReturnType, concreteGenericType, typeInferenceContext.getParent(), observer);
                            }
                        }

                        Multiplicity templateReturnMultiplicity = Optional.ofNullable(ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport)).map(i -> ((FunctionType) i)._returnMultiplicity()).orElse(null);
                        if (templateReturnMultiplicity != null)
                        {
                            FunctionType lambdaFunctionType = Objects.requireNonNull(getRawTypeFromGenericType((LambdaFunction<?>) val, processorSupport));
                            Multiplicity concreteMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(lambdaFunctionType, M3Properties.returnMultiplicity, processorSupport), lambdaInferenceContext.getMultiplicityParameterToMultiplicity());

                            lambdaFunctionType._returnMultiplicityRemove();
                            lambdaFunctionType._returnMultiplicity(concreteMultiplicity);
                            if (concreteMultiplicity != null)
                            {
                                TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                                typeInferenceContext.registerMul(templateReturnMultiplicity, concreteMultiplicity, typeInferenceContext.getParent(), observer);
                            }
                        }
                    }
                }
            }
        }
        return lambdaParametersInferenceSuccess;
    }

    private static boolean processEmptyColumnType(GenericType templateGenericType, ValueSpecification instance, ListIterable<? extends VariableExpression> paramsType, int z, SourceInformation sourceInformation, TypeInferenceObserver observer, ProcessorState state, ProcessorSupport processorSupport)
    {
        CoreInstance actualTemplateToInferColumnType = state.getTypeInferenceContext().getTypeParameterToGenericType().get(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(templateGenericType));
        if (actualTemplateToInferColumnType != null)
        {
            GenericType _typeToAnalyze = (GenericType) actualTemplateToInferColumnType;
            GenericType equalLeft = null;
            if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeOperationEqual(_typeToAnalyze))
            {
                equalLeft = (GenericType) _typeToAnalyze.getValueForMetaPropertyToOne("left");
                _typeToAnalyze = (GenericType) _typeToAnalyze.getValueForMetaPropertyToOne("right");
            }
            TypeInferenceContext foundParentToUpdate = state.getTypeInferenceContext().findParentForOperation(actualTemplateToInferColumnType);

            GenericType instanceGenericType = instance._genericType();
            GenericType typeToAnalyze = _typeToAnalyze;
            ((RelationType<?>) instanceGenericType._rawType())._columns().forEach(currentColumn ->
            {
                String colName = currentColumn._name();
                GenericType potentialEmptyType = _Column.getColumnType(currentColumn);
                if (potentialEmptyType._rawType() == null)
                {
                    if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeOperationSubset(typeToAnalyze))
                    {
                        // Search the reference type (right of contains) for the missing type column
                        RelationType<?> referenceRelation = (RelationType<?>) ((GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.getSetFromSubset(typeToAnalyze))._rawType();
                        if (referenceRelation != null)
                        {
                            Column<?, ?> foundColumn = referenceRelation._columns().detect(e -> e._name().equals(colName));
                            if (foundColumn == null)
                            {
                                throw new PureCompilationException(sourceInformation, "The column '" + colName + "' can't be found in the relation " + _RelationType.print(referenceRelation, processorSupport));
                            }
                            Type foundColumnType = _Column.getColumnType(foundColumn)._rawType();

                            // Fill the type
                            potentialEmptyType._rawType(foundColumnType);

                            CoreInstance left = org.finos.legend.pure.m3.navigation.generictype.GenericType.getLeftFromSubset(typeToAnalyze);
                            // Check if left is an EQUAL operation
                            if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeOperation(left, processorSupport))
                            {
                                // Set the Type Param to the INSTANCE genericType
                                CoreInstance param = left.getValueForMetaPropertyToOne("left");
                                foundParentToUpdate.register((GenericType) param, instanceGenericType, foundParentToUpdate, observer);
                                // Continue with the right side of EQUAL
                                left = left.getValueForMetaPropertyToOne("right");
                            }

                            // Check compatibility
                            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericCompatibleWith(instanceGenericType, left, processorSupport))
                            {
                                throw new RuntimeException(org.finos.legend.pure.m3.navigation.generictype.GenericType.print(left, processorSupport) + " is not compatible with " + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(instanceGenericType, processorSupport));
                            }
                            state.getTypeInferenceContext().replace(actualTemplateToInferColumnType, instanceGenericType);
                            foundParentToUpdate.register((GenericType) left, instanceGenericType, foundParentToUpdate, observer);
                        }
                    }
                }
            });

            // Set the type parameter of the missing Column Type to the found type
            if (equalLeft != null)
            {
                foundParentToUpdate.register(equalLeft, instanceGenericType, foundParentToUpdate, observer);
            }
        }
        else
        {
            GenericType templateGenType = paramsType.get(z)._genericType();
            TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
            typeInferenceContext.register(templateGenType, instance._genericType(), typeInferenceContext.getTopContext(), observer);
            typeInferenceContext.registerMul(paramsType.get(z)._multiplicity(), instance._multiplicity(), typeInferenceContext.getTopContext(), observer);
            return org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeFullyConcrete(instance._genericType(), processorSupport);
        }
        return true;
    }

    @Override
    public void populateReferenceUsages(FunctionExpression functionExpression, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance func = functionExpression._funcCoreInstance();
        if ("extractEnumValue_Enumeration_1__String_1__T_1_".equals(func.getName()))
        {
            GenericTypeTraceability.addTraceForEnum(functionExpression, repository, processorSupport);
        }
    }

    private IntSet firstPassTypeInference(FunctionExpression functionExpression, ListIterable<? extends ValueSpecification> parametersValues, ProcessorState processorState, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        TypeInferenceObserver observer = processorState.getObserver();
        observer.startProcessingFunctionExpression(functionExpression).shiftTab()
                .startFirstPassParametersProcessing().shiftTab(2);
        MutableIntSet unsuccessful = IntSets.mutable.empty();
        parametersValues.forEachWithIndex((boundVariable, i) ->
        {
            observer.processingParameter(functionExpression, i, boundVariable).shiftTab();

            try (MilestoningDateContextScope ignore = MilestoningDatesPropagationFunctions.withNewMilestoningDateContext(functionExpression, boundVariable, processorState, repository, context, processorSupport))
            {
                PostProcessor.processElement(matcher, boundVariable, processorState, processorSupport);
            }
            boolean success = isInferenceSuccess(boundVariable, processorSupport);
            observer.inferenceResult(success);
            if (!success)
            {
                unsuccessful.add(i);
            }
            addTraceForParameterValue(functionExpression, i, boundVariable, processorSupport);
            observer.unShiftTab();
        });
        observer.unShiftTab(2);
        return unsuccessful;
    }

    private void cleanProcess(ValueSpecification instance, ProcessorState processorState, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> visited = Lists.mutable.empty();
        Unbinder.process(Sets.mutable.with(instance), repository, processorState.getParserLibrary(), processorState.getInlineDSLLibrary(), context, processorSupport, new UnbindState(context, processorState.getURLPatternLibrary(), processorState.getInlineDSLLibrary(), processorSupport)
        {
            @Override
            public boolean noteVisited(CoreInstance instance)
            {
                visited.add(instance);
                return super.noteVisited(instance);
            }
        }, processorState.getMessage());
        visited.forEach(visitedNode ->
        {
            processorState.removeVisited(visitedNode);
            visitedNode.markNotProcessed();
            String functionName = visitedNode instanceof FunctionExpression ? ((FunctionExpression) visitedNode)._functionName() : null;
            if (functionName != null)
            {
                ListIterable<? extends ValueSpecification> parametersValues = ListHelper.wrapListIterable(((FunctionExpression) visitedNode)._parametersValues());
                if (("new".equals(functionName) || "copy".equals(functionName)) && (parametersValues.size() == 3))
                {
                    ((InstanceValue) parametersValues.get(2))._valuesCoreInstance().forEach(value ->
                    {
                        processorState.removeVisited(value);
                        value.markNotProcessed();
                    });
                }
            }
        });
    }

    private static boolean isInferenceSuccess(CoreInstance boundVariable, ProcessorSupport processorSupport)
    {
        if (isColumnWithEmptyType(boundVariable, processorSupport))
        {
            return false;
        }
        if (isLambdaWithEmptyParamType(boundVariable, processorSupport))
        {
            return false;
        }
        if (boundVariable instanceof InstanceValue)
        {
            return ((InstanceValue) boundVariable)._valuesCoreInstance().allSatisfy(v -> isInferenceSuccess(v, processorSupport));
        }
        if (boundVariable instanceof FunctionExpression)
        {
            Function<?> func = (Function<?>) ImportStub.withImportStubByPass(((FunctionExpression) boundVariable)._funcCoreInstance(), processorSupport);
            if (!(func instanceof AbstractProperty))
            {
                FunctionType fType = (FunctionType) processorSupport.function_getFunctionType(func);
                RichIterable<? extends GenericType> resolved = ((FunctionExpression) boundVariable)._resolvedTypeParameters();
                return fType._typeParameters().isEmpty() || (resolved.notEmpty() && resolved.select(c -> isColumnWithEmptyTypeGenericType(c, processorSupport)).isEmpty());
            }
        }
        return true;
    }

    public static boolean isLambdaWithEmptyParamType(CoreInstance boundVariable, ProcessorSupport processorSupport)
    {
        return (boundVariable instanceof InstanceValue) &&
                ((InstanceValue) boundVariable)._valuesCoreInstance().anySatisfy(v -> (v instanceof FunctionDefinition) && shouldInferTypesForFunctionParameters((FunctionDefinition<?>) v, processorSupport));
    }

    public static boolean isColumnWithEmptyType(CoreInstance boundVariable, ProcessorSupport processorSupport)
    {
        if (boundVariable instanceof InstanceValue)
        {
            CoreInstance genericType = boundVariable.getValueForMetaPropertyToOne("genericType");
            return isColumnWithEmptyTypeGenericType(genericType, processorSupport);
        }
        return false;
    }

    public static boolean isColumnWithEmptyTypeGenericType(CoreInstance genericType, ProcessorSupport processorSupport)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, "rawType", processorSupport);
        if (rawType != null && processorSupport.instance_instanceOf(rawType, M3Paths.RelationType))
        {
            return rawType.getValueForMetaPropertyToMany("columns").injectInto(true, (a, c) -> a && c.getValueForMetaPropertyToOne("classifierGenericType").getValueForMetaPropertyToMany("typeArguments").get(1).getValueForMetaPropertyToOne("rawType") == null);
        }
        return false;
    }

    private static boolean shouldInferTypesForFunctionParameters(FunctionDefinition<?> func, ProcessorSupport processorSupport)
    {
        FunctionType functionType = getRawTypeFromGenericType(func, processorSupport);
        return (functionType != null) && FunctionDefinitionProcessor.shouldInferTypesForFunctionParameters(functionType);
    }

    private static FunctionType getRawTypeFromGenericType(Function<?> val, ProcessorSupport processorSupport)
    {
        if (val._classifierGenericType() != null)
        {
            if (ListHelper.wrapListIterable(val._classifierGenericType()._typeArguments()).get(0) != null)
            {
                return (FunctionType) ImportStub.withImportStubByPass(ListHelper.wrapListIterable(val._classifierGenericType()._typeArguments()).get(0)._rawTypeCoreInstance(), processorSupport);
            }
        }
        return null;
    }

    private static void addTraceForParameterValue(FunctionExpression functionExpression, int i, ValueSpecification boundVariable, ProcessorSupport processorSupport)
    {
        if ((boundVariable != null) && (boundVariable._usageContext() == null))
        {
            boundVariable._usageContext(((ParameterValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.ParameterValueSpecificationContext))
                    ._offset(i)
                    ._functionExpression(functionExpression));
        }
    }

    private void addTraceForKeyExpressions(FunctionExpression functionExpression, ProcessorSupport processorSupport)
    {
        ListIterable<? extends ValueSpecification> params = ListHelper.wrapListIterable(functionExpression._parametersValues());
        if (params.size() > 2)
        {
            ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(((InstanceValue) params.get(2))._valuesCoreInstance()), processorSupport).forEachWithIndex((keyValue, z) ->
            {
                if (keyValue instanceof KeyExpression)
                {
                    KeyValueValueSpecificationContext usageContext = ((KeyValueValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.KeyValueValueSpecificationContext))
                            ._offset(z)
                            ._functionExpression(functionExpression);
                    ((KeyExpression) keyValue)._expression()._usageContext(usageContext);
                }
            });
        }
    }

    private static AbstractProperty<?> findFunctionForPropertyBasedOnMultiplicity(FunctionExpression propertyFunction, GenericType sourceGenericType, ProcessorState state, ProcessorSupport processorSupport, Matcher matcher) throws PureCompilationException
    {
        String propertyName = propertyFunction._propertyName()._valuesCoreInstance().getAny().getName();

        Type sourceType = (Type) ImportStub.withImportStubByPass(sourceGenericType._rawTypeCoreInstance(), processorSupport);
        AbstractProperty<?> property = (AbstractProperty<?>) processorSupport.class_findPropertyUsingGeneralization(sourceType, propertyName);
        if (property == null)
        {
            if (sourceType instanceof ClassProjection)
            {
                PostProcessor.processElement(matcher, sourceType, state, processorSupport);
                property = (AbstractProperty<?>) processorSupport.class_findPropertyUsingGeneralization(sourceType, propertyName);
            }
            if (property == null)
            {
                ListIterable<QualifiedProperty<?>> qualifiedProperties = _Class.findQualifiedPropertiesUsingGeneralization(sourceType, propertyName, processorSupport);
                if (qualifiedProperties.isEmpty() && sourceType instanceof ClassProjection)
                {
                    PostProcessor.processElement(matcher, sourceType, state, processorSupport);
                    qualifiedProperties = _Class.findQualifiedPropertiesUsingGeneralization(sourceType, propertyName, processorSupport);
                }
                property = (AbstractProperty<?>) findSingleArgumentQualifiedProperty(sourceType, qualifiedProperties, processorSupport);
                if (property == null)
                {
                    StringBuilder message = new StringBuilder();
                    switch (qualifiedProperties.size())
                    {
                        case 0:
                        {
                            PackageableElement.writeUserPathForPackageableElement(message.append("Can't find the property '").append(propertyName).append("' in the class "), sourceType);
                            break;
                        }
                        case 1:
                        {
                            property = qualifiedProperties.get(0);
                            message.append("The property '").append(propertyName).append("' ");
                            if (MilestoningFunctions.isGeneratedMilestoningProperty(property, processorSupport))
                            {
                                if (MilestoningFunctions.isAllVersionsInRangeProperty(property, processorSupport))
                                {
                                    milestoningMissingDateParamErrorMsgForAllVersionInRange(processorSupport, property, message);
                                }
                                else
                                {
                                    milestoningMissingDateParamErrorMsg(processorSupport, property, message);
                                }
                            }
                            else
                            {
                                message.append("requires some parameters.");
                            }
                            break;
                        }
                        default:
                        {
                            if (qualifiedProperties.allSatisfy(qp -> MilestoningFunctions.isGeneratedMilestoningProperty(qp, processorSupport))) //bitemporal
                            {
                                message.append("The property '").append(propertyName).append("' ");
                                milestoningMissingDateParamErrorMsg(processorSupport, qualifiedProperties.getFirst(), message);
                            }
                            else
                            {
                                message.append("There are ").append(qualifiedProperties.size()).append(" properties named '").append(propertyName).append("' and all require additional parameters.");
                            }
                        }
                    }
                    SourceInformation sourceInfo = propertyFunction._propertyName().getSourceInformation();
                    throw new PureCompilationException(sourceInfo, message.toString());
                }
            }
        }
        return property;
    }

    private static void milestoningMissingDateParamErrorMsgForAllVersionInRange(ProcessorSupport processorSupport, AbstractProperty<?> property, StringBuilder message)
    {
        MilestonedPropertyMetaData milestonedPropertyMetaData = MilestoningFunctions.getMilestonedMetaDataForProperty((QualifiedProperty<?>) property, processorSupport);
        message.append("is milestoned with stereotypes: ");
        milestonedPropertyMetaData.getClassTemporalStereotypes().appendString(message, "[ ", ",", " ]");
        message.append(" and requires 2 date parameters : [start, end]");
    }

    private static void milestoningMissingDateParamErrorMsg(ProcessorSupport processorSupport, AbstractProperty<?> property, StringBuilder message)
    {
        MilestonedPropertyMetaData milestonedPropertyMetaData = MilestoningFunctions.getMilestonedMetaDataForProperty((QualifiedProperty<?>) property, processorSupport);
        message.append("is milestoned with stereotypes: ");
        milestonedPropertyMetaData.getClassTemporalStereotypes().appendString(message, "[ ", ",", " ]");
        message.append(" and requires date parameters: ");
        milestonedPropertyMetaData.getTemporalDatePropertyNamesForStereotypes().appendString(message, "[ ", ", ", " ]");
    }

    private static CoreInstance findSingleArgumentQualifiedProperty(CoreInstance sourceType, RichIterable<? extends CoreInstance> qualifiedProperties, ProcessorSupport processorSupport)
    {
        int toCheck = sourceType.getValueForMetaPropertyToMany(M3Properties.typeVariables).size() + 1;
        for (CoreInstance qualifiedProperty : qualifiedProperties)
        {
            FunctionType funcType = (FunctionType) processorSupport.function_getFunctionType(qualifiedProperty);
            if (toCheck == funcType._parameters().size())
            {
                return qualifiedProperty;
            }
        }
        return null;
    }

    private static GenericType extractAndValidateGenericType(String propertyName, ValueSpecification source)
    {
        GenericType sourceGenericType = source._genericType();
        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(sourceGenericType))
        {
            throw new PureCompilationException(source.getSourceInformation(), "The type '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(sourceGenericType) + "' can't be inferred yet. Please specify it. (Property:'" + propertyName + "')");
        }
        return sourceGenericType;
    }

    private static ListIterable<QualifiedProperty<?>> findFunctionsForQualifiedPropertyBasedOnMultiplicity(FunctionExpression propertyFunction, GenericType sourceGenericType, ListIterable<? extends ValueSpecification> parametersValues, ProcessorSupport processorSupport, Matcher matcher, ProcessorState state) throws PureCompilationException
    {
        String propertyName = propertyFunction._qualifiedPropertyName()._valuesCoreInstance().getAny().getName();

        Type sourceRawType = (Type) ImportStub.withImportStubByPass(sourceGenericType._rawTypeCoreInstance(), processorSupport);

        if (sourceRawType instanceof ClassProjection)
        {
            PostProcessor.processElement(matcher, sourceRawType, state, processorSupport);
        }

        VariableExpression firstParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
        firstParam._genericType(parametersValues.get(0)._genericType());
        firstParam._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));

        MutableList<ValueSpecification> params = Lists.mutable.<ValueSpecification>with(firstParam).withAll(sourceGenericType._typeVariableValues()).withAll(ListHelper.tail(parametersValues));
        ListIterable<QualifiedProperty<?>> properties = _Class.findQualifiedPropertiesUsingGeneralization(sourceRawType, propertyName, processorSupport);
        SourceInformation sourceInformation = propertyFunction._qualifiedPropertyName().getSourceInformation();
        ListIterable<QualifiedProperty<?>> foundFunctions = FunctionExpressionMatcher.getFunctionMatches(properties, params, propertyName, sourceInformation, true, processorSupport);

        if (foundFunctions.isEmpty())
        {
            throwNoMatchException(propertyName, params, sourceInformation, processorSupport);
        }
        return foundFunctions;
    }

    private static void reprocessEnumValueInExtractEnumValue(FunctionExpression functionExpression, String propertyName, ProcessorState state, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // Add the propertyName to the functionExpression variables
        ValueSpecification propertyAsValueSpecification = (ValueSpecification) ValueSpecificationBootstrap.newStringLiteral(repository, propertyName, processorSupport);
        state.noteProcessed(propertyAsValueSpecification);
        functionExpression._parametersValues(Lists.immutable.<ValueSpecification>withAll(functionExpression._parametersValues()).newWith(propertyAsValueSpecification));
        functionExpression._functionName("extractEnumValue");
        functionExpression._propertyNameRemove();

        addTraceForParameterValue(functionExpression, 1, ListHelper.wrapListIterable(functionExpression._parametersValues()).get(1), processorSupport);
    }

    private static void reprocessPropertyForManySources(FunctionExpression functionExpression, ListIterable<? extends ValueSpecification> parametersValues, String propertyOrQualifiedPropertyNameProperty, GenericType sourceGenericType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        LambdaFunction<?> lambda = buildLambdaForMapWithProperty(functionExpression, ListHelper.tail(parametersValues).toList(), propertyOrQualifiedPropertyNameProperty, sourceGenericType, repository, processorSupport);

        //DO NOT CALL WRAP VALUE SPECIFICATION IT UPSETS THE TYPE INFERENCE
        InstanceValue lambdaAsValueSpecification = (InstanceValue) repository.newAnonymousCoreInstance(functionExpression.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.InstanceValue), true);
        lambdaAsValueSpecification._values(Lists.immutable.with(lambda));

        // Add the property to the functionExpression variables
        functionExpression._parametersValues(Lists.fixedSize.of(parametersValues.get(0), lambdaAsValueSpecification));

        // Make the function a map
        functionExpression._functionName("map");

        if (M3Properties.qualifiedPropertyName.equals(propertyOrQualifiedPropertyNameProperty))
        {
            functionExpression._qualifiedPropertyNameRemove();
        }
        else if (M3Properties.propertyName.equals(propertyOrQualifiedPropertyNameProperty))
        {
            functionExpression._propertyNameRemove();
        }
    }

    private static LambdaFunction<?> buildLambdaForMapWithProperty(FunctionExpression functionExpression, ListIterable<? extends ValueSpecification> qualifierParams, String propertyOrQualifiedPropertyNameProperty, GenericType sourceGenericType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        VariableExpression lambdaVarExpr = buildLambdaVariableExpression(functionExpression, repository, processorSupport);
        GenericType varExpGenT = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(sourceGenericType, functionExpression.getSourceInformation(), processorSupport);

        lambdaVarExpr._genericType(varExpGenT);
        lambdaVarExpr._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));

        FunctionType functionType = (FunctionType) repository.newAnonymousCoreInstance(functionExpression.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.FunctionType), true);
        functionType._parameters(Lists.immutable.with(lambdaVarExpr));

        GenericType functionTypeGt = (GenericType) repository.newAnonymousCoreInstance(functionExpression.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.GenericType), true);
        functionTypeGt._rawTypeCoreInstance(functionType);

        ClassInstance lambdaFunctionClass = (ClassInstance) processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
        GenericType lambdaGenericType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(lambdaFunctionClass, processorSupport);
        lambdaGenericType._typeArguments(Lists.immutable.with(functionTypeGt));

        VariableExpression paramVarExpr = buildLambdaVariableExpression(functionExpression, repository, processorSupport);

        SimpleFunctionExpression propertySfe = (SimpleFunctionExpression) repository.newAnonymousCoreInstance(functionExpression.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.SimpleFunctionExpression), true);

        String lambdaContextName;
        if (M3Properties.qualifiedPropertyName.equals(propertyOrQualifiedPropertyNameProperty))
        {
            lambdaContextName = functionExpression._qualifiedPropertyName().getName();
            propertySfe._qualifiedPropertyName(functionExpression._qualifiedPropertyName());
        }
        else if (M3Properties.propertyName.equals(propertyOrQualifiedPropertyNameProperty))
        {
            lambdaContextName = functionExpression._propertyName().getName();
            propertySfe._propertyName(functionExpression._propertyName());
        }
        else
        {
            throw new PureCompilationException("Unexpected property name" + propertyOrQualifiedPropertyNameProperty);
        }
        propertySfe._importGroup(functionExpression._importGroup());
        propertySfe._parametersValues(Lists.immutable.<ValueSpecification>with(paramVarExpr).newWithAll(qualifierParams));

        LambdaFunctionInstance lambdaFunctionInst = LambdaFunctionInstance.createPersistent(repository, lambdaContextName, functionExpression.getSourceInformation());
        lambdaFunctionInst._expressionSequence(Lists.immutable.with(propertySfe));
        lambdaFunctionInst._classifierGenericType(lambdaGenericType);

        return lambdaFunctionInst;
    }

    private static VariableExpression buildLambdaVariableExpression(FunctionExpression functionExpression, ModelRepository repository, ProcessorSupport processorSupport)
    {
        VariableExpression paramVarExpr = (VariableExpression) repository.newAnonymousCoreInstance(functionExpression.getSourceInformation(), processorSupport.package_getByUserPath(M3Paths.VariableExpression), true);
        paramVarExpr._name(Automap.AUTOMAP_LAMBDA_VARIABLE_NAME);
        return paramVarExpr;
    }

    private static void throwNoMatchException(FunctionExpression functionExpression, ProcessorState processorState, ProcessorSupport processorSupport) throws PureCompilationException
    {
        StringBuilder message = new StringBuilder("The system can't find a match for the function: ");
        StringBuilder functionSignatureBuilder = org.finos.legend.pure.m3.navigation.functionexpression.FunctionExpression.printFunctionSignatureFromExpression(new StringBuilder(), functionExpression, processorSupport);
        message.append(functionSignatureBuilder);
        SourceInformation functionExpressionSourceInformation = functionExpression.getSourceInformation();

        String functionName = getFunctionName(functionExpression);
        SetIterable<CoreInstance> possibleFunctions = processorSupport.function_getFunctionsForName(functionName);
        if (possibleFunctions.notEmpty() && (possibleFunctions.size() < 20))
        {
            ImportGroup functionExpressionImportGroup = functionExpression._importGroup();
            SetIterable<String> alreadyImportedPackages = functionExpressionImportGroup._imports().collect(ImportAccessor::_path, Sets.mutable.empty());
            PartitionSet<CoreInstance> partition = possibleFunctions.partition(f ->
            {
                Package functionPackage = ((PackageableFunction<?>) f)._package();
                return (functionPackage._package() == null) || alreadyImportedPackages.contains(PackageableElement.getUserPathForPackageableElement(functionPackage));
            });
            SetIterable<CoreInstance> possibleFunctionsWithPackageNotImported = partition.getRejected();
            SetIterable<CoreInstance> possibleFunctionsWithPackageImported = partition.getSelected();

            MutableList<CoreInstance> candidatesNotInCoreImportsWithPackageNotImported = Lists.mutable.empty();
            MutableList<CoreInstance> candidatesInCoreImportsWithPackageNotImported = Lists.mutable.empty();
            MutableList<CoreInstance> candidatesNotInCoreImportsWithPackageImported = Lists.mutable.empty();
            MutableList<CoreInstance> candidatesInCoreImportsWithPackageImported = Lists.mutable.empty();
            // Get core imports, do not add functions found in coreImports to candidates
            ImportGroup coreImport = (ImportGroup) processorSupport.package_getByUserPath(M3Paths.coreImport);
            MutableSet<String> coreImports = coreImport._imports().collect(ImportAccessor::_path).toSet();
            populateFunctionCandidates(
                    processorState, processorSupport, functionExpressionSourceInformation, possibleFunctionsWithPackageNotImported,
                    candidatesNotInCoreImportsWithPackageNotImported, candidatesInCoreImportsWithPackageNotImported, coreImports
            );
            populateFunctionCandidates(
                    processorState, processorSupport, functionExpressionSourceInformation, possibleFunctionsWithPackageImported,
                    candidatesNotInCoreImportsWithPackageImported, candidatesInCoreImportsWithPackageImported, coreImports
            );
            throw new PureUnmatchedFunctionException(
                    functionExpression.getSourceInformation(), functionSignatureBuilder.toString(), functionName,
                    candidatesInCoreImportsWithPackageNotImported, candidatesNotInCoreImportsWithPackageNotImported,
                    candidatesInCoreImportsWithPackageImported, candidatesNotInCoreImportsWithPackageImported,
                    functionExpressionImportGroup, processorSupport
            );
        }

        throw new PureCompilationException(functionExpression.getSourceInformation(), message.toString());
    }

    private static void throwNoMatchException(String functionName, ListIterable<? extends ValueSpecification> parametersValues, SourceInformation sourceInfo, ProcessorSupport processorSupport) throws PureCompilationException
    {
        StringBuilder message = new StringBuilder("The system can't find a match for the function: ");
        org.finos.legend.pure.m3.navigation.functionexpression.FunctionExpression.printFunctionSignatureFromExpression(message, functionName, parametersValues, processorSupport);
        throw new PureCompilationException(sourceInfo, message.toString());
    }

    private static String getFunctionName(FunctionExpression functionExpression)
    {
        String functionName = functionExpression._functionName();
        return (functionName == null) ? null : functionName.substring(functionName.lastIndexOf(':') + 1);
    }

    private static void populateFunctionCandidates(ProcessorState processorState, ProcessorSupport processorSupport, SourceInformation functionExpressionSourceInformation, SetIterable<CoreInstance> possibleFunctions, MutableList<CoreInstance> candidatesNotInCoreImports, MutableList<CoreInstance> candidatesInCoreImports, MutableSet<String> coreImports)
    {
        for (CoreInstance function : possibleFunctions)
        {
            if (Visibility.isVisibleInSource(function, (functionExpressionSourceInformation == null) ? null : functionExpressionSourceInformation.getSourceId(), processorState.getCodeStorage() == null ? CodeRepositoryProviderHelper.findCodeRepositories() : processorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                CoreInstance pkg = ((PackageableFunction<?>) function)._package();
                StringBuilder packageName = new StringBuilder();
                if ((pkg != null) && !M3Paths.Root.equals(pkg.getName()))
                {
                    PackageableElement.writeUserPathForPackageableElement(packageName, pkg, "::");
                }
                if (coreImports.contains(packageName.toString()))
                {
                    candidatesInCoreImports.add(function);
                }
                else
                {
                    candidatesNotInCoreImports.add(function);
                }
            }
        }
    }
}
