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
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.GenericTypeTraceability;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.Objects;

public class FunctionExpressionProcessor extends Processor<FunctionExpression>
{
    @Override
    public String getClassName()
    {
        return M3Paths.FunctionExpression;
    }

    @Override
    public void process(FunctionExpression functionExpression, final ProcessorState state, final Matcher matcher, final ModelRepository repository, final Context context, final ProcessorSupport processorSupport)
    {
        TypeInferenceObserver observer = state.getObserver();
        state.pushTypeInferenceContext();

        ListIterable<? extends ValueSpecification> parametersValues = ListHelper.wrapListIterable(functionExpression._parametersValues());

        // Process the function's parameters (FIRST PASS)
        boolean inferenceSuccess = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);

        // Function matching
        ListIterable<? extends Function<?>> foundFunctions = null != functionExpression._funcCoreInstance() ?
                Lists.immutable.with((Function<?>) ImportStub.withImportStubByPass(functionExpression._funcCoreInstance(), processorSupport))
                : Lists.immutable.empty();

        String functionName = null;
        if (foundFunctions.isEmpty())
        {
            // Check if the function is a property
            InstanceValue propertyNameInstanceVal = functionExpression._propertyName();
            if (null != propertyNameInstanceVal)
            {
                ValueSpecification source = parametersValues.get(0);

                String propertyName = ImportStub.withImportStubByPass(propertyNameInstanceVal._valuesCoreInstance().toList().get(0), processorSupport).getName();
                GenericType sourceGenericType = extractAndValidateGenericType(processorSupport, propertyName, source);

                //Is it an enum?
                if (org.finos.legend.pure.m3.navigation.generictype.GenericType.subTypeOf(sourceGenericType, org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(processorSupport.package_getByUserPath(M3Paths.Enumeration), processorSupport), processorSupport))
                {
                    reprocessEnumValueInExtractEnumValue(functionExpression, propertyName, state, repository, processorSupport);
                }
                else
                {
                    Multiplicity sourceMultiplicity = source._multiplicity();
                    if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(sourceMultiplicity, true))
                    {
                        AbstractProperty<?> propertyFunc = findFunctionForPropertyBasedOnMultiplicity(functionExpression, sourceGenericType, state, processorSupport, matcher);
                        if (null != propertyFunc)
                        {
                            if (MilestoningFunctions.isGeneratedMilestonedQualifiedPropertyWithMissingDates(propertyFunc, processorSupport))
                            {
                                propertyFunc = (AbstractProperty<?>) MilestoningDatesPropagationFunctions.getMilestoningQualifiedPropertyWithAllDatesSupplied(functionExpression, state, repository, context, processorSupport, propertyNameInstanceVal, source, propertyName, propertyFunc);
                            }
                            foundFunctions = Lists.immutable.with(propertyFunc);
                        }
                    }
                    else
                    {
                        //Automap
                        reprocessPropertyForManySources(functionExpression, parametersValues, M3Properties.propertyName, sourceGenericType, repository, processorSupport);
                        //The parameters values are now different, so update
                        parametersValues = functionExpression._parametersValues().toList();
                        //Have another go at type inference
                        inferenceSuccess = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
                        //return;
                    }
                }
            }
            // Check if the function is a qualifiedProperty
            else
            {
                InstanceValue qualifiedPropertyNameVal = functionExpression._qualifiedPropertyName();
                if (null != qualifiedPropertyNameVal)
                {
                    ValueSpecification source = parametersValues.get(0);

                    String qualifiedPropertyName = ImportStub.withImportStubByPass(qualifiedPropertyNameVal._valuesCoreInstance().toList().get(0), processorSupport).getName();
                    GenericType sourceGenericType = extractAndValidateGenericType(processorSupport, qualifiedPropertyName, source);

                    Multiplicity sourceMultiplicity = source._multiplicity();
//                    if (!org.finos.legend.pure.m3.bootstrap.type.multiplicity.Multiplicity.isToOne(sourceMultiplicity, true) && org.finos.legend.pure.m3.bootstrap.type.multiplicity.Multiplicity.isToOne(sourceMultiplicity, false))
//                    {
//                        SourceInformation si = functionExpression.getSourceInformation();
//                        String textToAppend = "{\"file\":\""+si.getSourceId()+"\",\"line\":"+si.getLine()+", \"column\":"+si.getColumn()+"}\n"; //new line in content
//                        Path path = Paths.get("h:/temp/script.json");
//                        try
//                        {
//                            System.out.print(textToAppend);
//                            Files.write(path, textToAppend.getBytes(), StandardOpenOption.APPEND);  //Append mode
//                        }
//                        catch (IOException e)
//                        {
//                            e.printStackTrace();
//                        }
//                    }
                    if (org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(sourceMultiplicity, true))
                    {
                        ListIterable<QualifiedProperty<?>> qualifiedPropertyFuncs = findFunctionsForQualifiedPropertyBasedOnMultiplicity(functionExpression, sourceGenericType, parametersValues, processorSupport, matcher, state);
                        if (qualifiedPropertyFuncs.size() == 1 && MilestoningFunctions.isGeneratedMilestonedQualifiedPropertyWithMissingDates(qualifiedPropertyFuncs.getFirst(), processorSupport))
                        {
                            Function<?> mqp = (Function<?>) MilestoningDatesPropagationFunctions.getMilestoningQualifiedPropertyWithAllDatesSupplied(functionExpression, state, repository, context, processorSupport, qualifiedPropertyNameVal, source, qualifiedPropertyName, qualifiedPropertyFuncs.getFirst());
                            foundFunctions = Lists.immutable.with(mqp);
                        }
                        else
                        {
                            foundFunctions = qualifiedPropertyFuncs;
                        }
                    }
                    else
                    {
                        //Automap
                        reprocessPropertyForManySources(functionExpression, parametersValues, M3Properties.qualifiedPropertyName, sourceGenericType, repository, processorSupport);
                        //The parameters values are now different, so update
                        parametersValues = functionExpression._parametersValues().toList();
                        //Have another go at type inference
                        inferenceSuccess = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
                    }

                }
            }

            if (foundFunctions.isEmpty())
            {
                // Match the functionExpression with the Function library (may still need to do it even if the function is a property because it may have been reprocessed as a Collect!)
                foundFunctions = FunctionExpressionMatcher.findMatchingFunctionsInTheRepository(functionExpression, true, processorSupport);
                functionName = getFunctionName(functionExpression);
            }
        }

        Function<?> finalFunction = null;
        boolean someInferenceFailed = false;
        for (Function<?> foundFunction : foundFunctions)
        {
            functionExpression._funcRemove();
            functionExpression._funcCoreInstance(foundFunction);

            state.getTypeInferenceContext().setScope(foundFunction);
            FunctionType foundFunctionType = (FunctionType) processorSupport.function_getFunctionType(foundFunction);

            observer.functionMatched(foundFunction, foundFunctionType);

            // SECOND PASS
            ListIterable<? extends VariableExpression> paramsType = foundFunctionType._parameters().toList();
            // enumValues, autoMaps, etc...
            parametersValues = functionExpression._parametersValues().toList();

            boolean success = true;
            if (!inferenceSuccess)
            {
                observer.firstPassInferenceFailed();
                observer.shiftTab();
                observer.shiftTab();
                observer.matchTypeParamsFromFoundFunction(foundFunction);
                observer.shiftTab();
                for (int z = 0; z < parametersValues.size(); z++)
                {
                    ValueSpecification instance = parametersValues.get(z);
                    if (isInferenceSuccess(instance, processorSupport))
                    {
                        observer.matchParam(z);
                        GenericType templateGenType = paramsType.get(z)._genericType();
                        TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                        typeInferenceContext.register(templateGenType, instance._genericType(), typeInferenceContext.getTopContext(), observer);
                        typeInferenceContext.registerMul(paramsType.get(z)._multiplicity(), instance._multiplicity(), typeInferenceContext.getTopContext(), observer);
                    }
                    else
                    {
                        observer.paramInferenceFailed(z);
                    }
                }
                observer.unShiftTab();
                observer.reverseMatching();

                for (int z = 0; z < parametersValues.size(); z++)
                {
                    final ValueSpecification instance = parametersValues.get(z);
                    observer.processingParameter(functionExpression, z, instance);

                    GenericType templateGenType = paramsType.get(z)._genericType();
                    Multiplicity templateMultiplicity = paramsType.get(z)._multiplicity();
                    GenericType resolvedGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(templateGenType, state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
                    Multiplicity resolvedMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(templateMultiplicity, state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());
                    if (isLambdaWithEmptyParamType(instance, processorSupport))
                    {
                        observer.shiftTab();
                        final VariableExpression templateToMatchLambdaTo = Objects.requireNonNull(getRawTypeFromGenericType(foundFunction, processorSupport))._parameters().toList().get(z);

                        observer.register(templateToMatchLambdaTo, templateToMatchLambdaTo, state.getTypeInferenceContext(), state.getTypeInferenceContext());

                        for (final CoreInstance val : ((InstanceValue) instance)._valuesCoreInstance())
                        {
                            if (val instanceof LambdaFunction)
                            {
                                org.eclipse.collections.api.block.function.Function<CoreInstance, Boolean> processParamTypesOfLambdaUsedAsAFunctionExpressionParamValue = coreInstance -> !TypeInference.processParamTypesOfLambdaUsedAsAFunctionExpressionParamValue(instance, (LambdaFunction<?>) val, templateToMatchLambdaTo, matcher, state, repository, processorSupport);
                                success = success && MilestoningDatesPropagationFunctions.possiblyExecuteInNewMilestoningDateContext(functionExpression, val, processParamTypesOfLambdaUsedAsAFunctionExpressionParamValue, state, repository, context, processorSupport);

                                // Manage return type in any case
                                GenericType templateGenericType = templateToMatchLambdaTo._genericType();
                                ClassInstance functionClass = (ClassInstance) processorSupport.package_getByUserPath(M3Paths.Function);
                                if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateGenericType, processorSupport) && org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(ImportStub.withImportStubByPass(templateGenericType._rawTypeCoreInstance(), processorSupport), functionClass, processorSupport))
                                {
                                    GenericType templateGenFunctionType = templateGenericType._typeArguments().toList().get(0);
                                    if (org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateGenFunctionType, processorSupport) && !org.finos.legend.pure.m3.navigation.type.Type.isTopType(Instance.getValueForMetaPropertyToOneResolved(templateGenFunctionType, M3Properties.rawType, processorSupport), processorSupport))
                                    {
                                        GenericType templateReturnType = null != ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport) ?
                                                ((FunctionType) ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport))._returnType() : null;

                                        // Generics in lambdas are relative to their environment (i.e. the function in which they are defined)
                                        TypeInferenceContext lambdaInferenceContext = state.getTypeInferenceContext().getTopContext();

                                        if (null != templateReturnType)
                                        {
                                            FunctionType lambdaFunctionType = Objects.requireNonNull(getRawTypeFromGenericType((LambdaFunction<?>) val, processorSupport));
                                            GenericType concreteGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(lambdaFunctionType._returnType(), lambdaInferenceContext.getTypeParameterToGenericType(), lambdaInferenceContext.getMultiplicityParameterToMultiplicity(), processorSupport);
                                            lambdaFunctionType._returnTypeRemove();
                                            lambdaFunctionType._returnType(concreteGenericType);
                                            if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(templateReturnType, processorSupport))
                                            {
                                                TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                                                typeInferenceContext.register(templateReturnType, concreteGenericType, typeInferenceContext.getParent(), observer);
                                            }
                                        }

                                        Multiplicity templateReturnMultiplicity = null != ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport) ?
                                                ((FunctionType) ImportStub.withImportStubByPass(templateGenFunctionType._rawTypeCoreInstance(), processorSupport))._returnMultiplicity() : null;
                                        if (null != templateReturnMultiplicity)
                                        {
                                            FunctionType lambdaFunctionType = Objects.requireNonNull(getRawTypeFromGenericType((LambdaFunction<?>) val, processorSupport));
                                            Multiplicity concreteMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(Instance.getValueForMetaPropertyToOneResolved(lambdaFunctionType, M3Properties.returnMultiplicity, processorSupport), lambdaInferenceContext.getMultiplicityParameterToMultiplicity());

                                            lambdaFunctionType._returnMultiplicityRemove();
                                            lambdaFunctionType._returnMultiplicity(concreteMultiplicity);
                                            if (null != concreteMultiplicity)
                                            {
                                                TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                                                typeInferenceContext.registerMul(templateReturnMultiplicity, concreteMultiplicity, typeInferenceContext.getParent(), observer);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        observer.unShiftTab();
                    }
                    else
                    {
                        state.pushTypeInferenceContextAhead();
                        TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                        typeInferenceContext.setScope(instance instanceof SimpleFunctionExpression ? ((SimpleFunctionExpression) instance)._funcCoreInstance() : null);
                        typeInferenceContext.register(instance._genericType(), resolvedGenericType, typeInferenceContext.getParent(), observer);
                        typeInferenceContext.registerMul(instance._multiplicity(), resolvedMultiplicity, typeInferenceContext.getParent(), observer);
                        cleanProcess(instance, state, repository, context, processorSupport);
                        PostProcessor.processElement(matcher, instance, state, processorSupport);
                        state.popTypeInferenceContextAhead();
                    }
                }
                observer.unShiftTab();
                observer.unShiftTab();
            }
            else
            {
                observer.parameterInferenceSucceeded();
                observer.shiftTab();
                observer.shiftTab();
                parametersValues.forEachWithIndex((instance, z) ->
                {
                    TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                    typeInferenceContext.register(paramsType.get(z)._genericType(), instance._genericType(), typeInferenceContext.getTopContext(), observer);
                    typeInferenceContext.registerMul(paramsType.get(z)._multiplicity(), instance._multiplicity(), typeInferenceContext.getTopContext(), observer);
                });

                // WARNING / returnType may need reverse matching to be found
                GenericType returnGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
                observer.returnType(returnGenericType);
                if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(returnGenericType, processorSupport) && !state.getTypeInferenceContext().isTop(org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(returnGenericType, processorSupport)))
                {
                    observer.shiftTab();
                    observer.returnTypeNotConcrete();

                    // reverse matching
                    parametersValues.forEachWithIndex((instance, z) ->
                    {
                        GenericType templateGenType = paramsType.get(z)._genericType();
                        Multiplicity templateMultiplicity = paramsType.get(z)._multiplicity();
                        GenericType resolvedGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(templateGenType, state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
                        Multiplicity resolvedMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(templateMultiplicity, state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());

                        state.pushTypeInferenceContextAhead();
                        state.getTypeInferenceContext().setScope(instance instanceof FunctionExpression ? ((FunctionExpression) instance)._funcCoreInstance() : null);
                        observer.processingParameter(functionExpression, z, instance);
                        TypeInferenceContext typeInferenceContext = state.getTypeInferenceContext();
                        typeInferenceContext.register(instance._genericType(), resolvedGenericType, typeInferenceContext.getParent(), observer);
                        typeInferenceContext.registerMul(instance._multiplicity(), resolvedMultiplicity, typeInferenceContext.getParent(), observer);

                        observer.shiftTab();
                        observer.reprocessingTheParameter();
                        observer.shiftTab();

                        cleanProcess(instance, state, repository, context, processorSupport);
                        PostProcessor.processElement(matcher, instance, state, processorSupport);

                        observer.unShiftTab();
                        observer.finishedProcessParameter();
                        observer.unShiftTab();

                        state.popTypeInferenceContextAhead();
                    });
                    observer.unShiftTab();

                    returnGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);
                    observer.newReturnType(returnGenericType);
                }
                observer.unShiftTab();
                observer.unShiftTab();
                observer.finishedRegisteringParametersAndMultiplicities();
            }

            // We can infer the parameter types for Lambdas given as parameters (now that we know which function to use).
            if (success)
            {
                TypeInference.storeInferredTypeParametersInFunctionExpression(functionExpression, state, processorSupport, foundFunction);

                // Get the return type information
                //Pair<CoreInstance, CoreInstance> result = FunctionExpression.resolveFunctionGenericReturnTypeAndMultiplicity(functionExpression, context, processorSupport);
                GenericType returnGenericType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.makeTypeArgumentAsConcreteAsPossible(foundFunctionType._returnType(), state.getTypeInferenceContext().getTypeParameterToGenericType(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity(), processorSupport);//result.getOne();
                Multiplicity returnMultiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.makeMultiplicityAsConcreteAsPossible(foundFunctionType._returnMultiplicity(), state.getTypeInferenceContext().getMultiplicityParameterToMultiplicity());

                if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(returnGenericType, processorSupport) && !state.getTypeInferenceContext().isTop(org.finos.legend.pure.m3.navigation.generictype.GenericType
                        .getTypeParameterName(returnGenericType, processorSupport)))
                {
                    throw new PureCompilationException(functionExpression.getSourceInformation(), "The system is not capable of inferring the return type of the function '" + functionExpression.getValueForMetaPropertyToOne(M3Properties.func).getValueForMetaPropertyToOne(M3Properties.functionName).getName() + "'. Check your signatures!");
                }

                // Update the type
                GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericTypeAsInferredGenericType(returnGenericType, functionExpression.getSourceInformation(), processorSupport);
                functionExpression._genericType(genericTypeCopy);

                // Update the multiplicity
                Multiplicity returnMultiplicityCopy = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.copyMultiplicity(returnMultiplicity, functionExpression.getSourceInformation(), processorSupport);
                functionExpression._multiplicity(returnMultiplicityCopy);
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
            }

            if (null == functionName)
            {
                finalFunction = foundFunction;
            }
            else if (!success)
            {
                someInferenceFailed = true;
            }
            else
            {
                Function<?> bestMatch = FunctionExpressionMatcher.getBestFunctionMatch(foundFunctions, parametersValues, functionName, functionExpression.getSourceInformation(), false, processorSupport);
                if (bestMatch == foundFunction)
                {
                    finalFunction = foundFunction;
                }
            }

            if (null != finalFunction)
            {
                break;
            }

            // Clean up before re-trying
            if (1 < foundFunctions.size())
            {
                for (ValueSpecification parameterValue : parametersValues)
                {
                    cleanProcess(parameterValue, state, repository, context, processorSupport);
                }
                inferenceSuccess = firstPassTypeInference(functionExpression, parametersValues, state, matcher, repository, context, processorSupport);
            }
        }

        if (null != finalFunction)
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
                    state.getVariableContext().getParent().registerValue(((InstanceValue) parametersValues.get(0))._valuesCoreInstance().toList().get(0).getName(), parametersValues.get(1));
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

        observer.unShiftTab();
        observer.finishedProcessingFunctionExpression(functionExpression);
        state.popTypeInferenceContext();
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

    private boolean firstPassTypeInference(FunctionExpression functionExpression, ListIterable<? extends ValueSpecification> parametersValues, ProcessorState processorState, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        TypeInferenceObserver observer = processorState.getObserver();
        int i = 0;
        observer.startProcessingFunctionExpression(functionExpression);
        boolean inferenceSuccess = true;
        observer.shiftTab();
        observer.startFirstPassParametersProcessing();
        observer.shiftTab();
        observer.shiftTab();

        org.eclipse.collections.api.block.function.Function<CoreInstance, Void> processElement = coreInstance ->
        {
            PostProcessor.processElement(matcher, coreInstance, processorState, processorSupport);
            return null;
        };

        for (ValueSpecification boundVariable : parametersValues)
        {
            observer.processingParameter(functionExpression, i, boundVariable);
            observer.shiftTab();

            MilestoningDatesPropagationFunctions.possiblyExecuteInNewMilestoningDateContext(functionExpression, boundVariable, processElement, processorState, repository, context, processorSupport);
            boolean success = this.isInferenceSuccess(boundVariable, processorSupport);
            observer.inferenceResult(success);
            inferenceSuccess = inferenceSuccess && success;
            addTraceForParameterValue(functionExpression, i, boundVariable, processorSupport);
            i++;

            observer.unShiftTab();
        }
        observer.unShiftTab();
        observer.unShiftTab();
        return inferenceSuccess;
    }

    private void cleanProcess(ValueSpecification instance, ProcessorState processorState, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        MutableList<CoreInstance> visited = Lists.mutable.empty();
        Unbinder.process(Sets.mutable.with(instance), repository, processorState.getParserLibrary(), processorState.getInlineDSLLibrary(), context, processorSupport, new UnbindState(context, processorState.getURLPatternLibrary(), processorSupport)
        {
            @Override
            public boolean noteVisited(CoreInstance instance)
            {
                visited.add(instance);
                return super.noteVisited(instance);
            }
        }, processorState.getMessage());
        for (CoreInstance visitedNode : visited)
        {
            processorState.removeVisited(visitedNode);
            visitedNode.markNotProcessed();
            String functionName = visitedNode instanceof FunctionExpression ? ((FunctionExpression) visitedNode)._functionName() : null;
            if (null != functionName)
            {
                ListIterable<? extends ValueSpecification> parametersValues = ((FunctionExpression) visitedNode)._parametersValues().toList();
                if (("new".equals(functionName) || "copy".equals(functionName)) && 3 == parametersValues.size())
                {
                    for (CoreInstance value : ((InstanceValue) parametersValues.get(2))._valuesCoreInstance())
                    {
                        processorState.removeVisited(value);
                        value.markNotProcessed();
                    }
                }
            }
        }
    }

    private boolean isInferenceSuccess(CoreInstance boundVariable, ProcessorSupport processorSupport)
    {
        if (isLambdaWithEmptyParamType(boundVariable, processorSupport))
        {
            return false;
        }
        if (boundVariable instanceof InstanceValue)
        {
            for (CoreInstance value : ((InstanceValue) boundVariable)._valuesCoreInstance())
            {
                if (!isInferenceSuccess(value, processorSupport))
                {
                    return false;
                }
            }
            return true;
        }
        if (boundVariable instanceof FunctionExpression)
        {
            Function<?> func = (Function<?>) ImportStub.withImportStubByPass(((FunctionExpression) boundVariable)._funcCoreInstance(), processorSupport);
            if (!(func instanceof AbstractProperty))
            {
                FunctionType fType = (FunctionType) processorSupport.function_getFunctionType(func);
                return fType._typeParameters().isEmpty() || ((FunctionExpression) boundVariable)._resolvedTypeParameters().notEmpty();
            }
        }
        return true;
    }

    public static boolean isLambdaWithEmptyParamType(CoreInstance boundVariable, ProcessorSupport processorSupport)
    {
        if (boundVariable instanceof InstanceValue)
        {
            for (CoreInstance val : ((InstanceValue) boundVariable)._valuesCoreInstance())
            {
                if (val instanceof FunctionDefinition && shouldInferTypesForFunctionParameters(val, processorSupport))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldInferTypesForFunctionParameters(CoreInstance val, ProcessorSupport processorSupport)
    {
        FunctionType functionType = getRawTypeFromGenericType((FunctionDefinition<?>) val, processorSupport);
        return FunctionDefinitionProcessor.shouldInferTypesForFunctionParameters(functionType);
    }

    private static FunctionType getRawTypeFromGenericType(Function<?> val, ProcessorSupport processorSupport)
    {
        if (null != val._classifierGenericType())
        {
            if (null != val._classifierGenericType()._typeArguments().toList().get(0))
            {
                return (FunctionType) ImportStub.withImportStubByPass(val._classifierGenericType()._typeArguments().toList().get(0)._rawTypeCoreInstance(), processorSupport);
            }
        }
        return null;
    }

    private static void addTraceForParameterValue(FunctionExpression functionExpression, int i, ValueSpecification boundVariable, ProcessorSupport processorSupport)
    {
        if (null != boundVariable && null == boundVariable._usageContext())
        {
            ParameterValueSpecificationContext usageContext = (ParameterValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.ParameterValueSpecificationContext);

            usageContext._offset(i);
            usageContext._functionExpression(functionExpression);
            boundVariable._usageContext(usageContext);
        }
    }

    private void addTraceForKeyExpressions(FunctionExpression functionExpression, ProcessorSupport processorSupport)
    {
        ListIterable<? extends ValueSpecification> params = functionExpression._parametersValues().toList();
        if (2 < params.size())
        {
            int z = 0;
            for (CoreInstance keyValue : ImportStub.withImportStubByPasses(((InstanceValue) params.get(2))._valuesCoreInstance().toList(), processorSupport))
            {
                if (keyValue instanceof KeyExpression)
                {
                    KeyValueValueSpecificationContext usageContext = (KeyValueValueSpecificationContext) processorSupport.newAnonymousCoreInstance(null, M3Paths.KeyValueValueSpecificationContext);

                    usageContext._offset(z);
                    usageContext._functionExpression(functionExpression);

                    ValueSpecification expression = ((KeyExpression) keyValue)._expression();
                    if (null != expression._usageContext())
                    {
                        expression._usageContextRemove();
                    }
                    expression._usageContext(usageContext);
                }
                z++;
            }
        }
    }

    private static AbstractProperty<?> findFunctionForPropertyBasedOnMultiplicity(FunctionExpression propertyFunction, GenericType sourceGenericType, ProcessorState state, ProcessorSupport processorSupport, Matcher matcher) throws PureCompilationException
    {
        String propertyName = propertyFunction._propertyName()._valuesCoreInstance().toList().get(0).getName();

        Type sourceType = (Type) ImportStub.withImportStubByPass(sourceGenericType._rawTypeCoreInstance(), processorSupport);
        AbstractProperty<?> property = (AbstractProperty<?>) processorSupport.class_findPropertyUsingGeneralization(sourceType, propertyName);
        if (null == property)
        {
            if (sourceType instanceof ClassProjection)
            {
                PostProcessor.processElement(matcher, sourceType, state, processorSupport);
                property = (AbstractProperty<?>) processorSupport.class_findPropertyUsingGeneralization(sourceType, propertyName);
            }
            if (null == property)
            {
                ListIterable<QualifiedProperty<?>> qualifiedProperties = _Class.findQualifiedPropertiesUsingGeneralization(sourceType, propertyName, processorSupport);
                if (qualifiedProperties.isEmpty() && sourceType instanceof ClassProjection)
                {
                    PostProcessor.processElement(matcher, sourceType, state, processorSupport);
                    qualifiedProperties = _Class.findQualifiedPropertiesUsingGeneralization(sourceType, propertyName, processorSupport);
                }
                property = (AbstractProperty<?>) findSingleArgumentQualifiedProperty(qualifiedProperties, processorSupport);
                if (null == property)
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

    private static CoreInstance findSingleArgumentQualifiedProperty(RichIterable<? extends CoreInstance> qualifiedProperties, ProcessorSupport processorSupport)
    {
        for (CoreInstance qualifiedProperty : qualifiedProperties)
        {
            FunctionType funcType = (FunctionType) processorSupport.function_getFunctionType(qualifiedProperty);
            if (1 == funcType._parameters().size())
            {
                return qualifiedProperty;
            }
        }
        return null;
    }

    private static GenericType extractAndValidateGenericType(ProcessorSupport processorSupport, String propertyName, ValueSpecification source)
    {
        GenericType sourceGenericType = source._genericType();
        if (!org.finos.legend.pure.m3.navigation.generictype.GenericType.isGenericTypeConcrete(sourceGenericType, processorSupport))
        {
            throw new PureCompilationException(source.getSourceInformation(), "The type '" + org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(sourceGenericType, processorSupport) + "' can't be inferred yet. Please specify it. (Property:'" + propertyName + "')");
        }
        return sourceGenericType;
    }

    private static ListIterable<QualifiedProperty<?>> findFunctionsForQualifiedPropertyBasedOnMultiplicity(FunctionExpression propertyFunction, GenericType sourceGenericType, ListIterable<? extends ValueSpecification> parametersValues, ProcessorSupport processorSupport, Matcher matcher, ProcessorState state) throws PureCompilationException
    {
        String propertyName = propertyFunction._qualifiedPropertyName()._valuesCoreInstance().toList().get(0).getName();

        Type sourceRawType = (Type) ImportStub.withImportStubByPass(sourceGenericType._rawTypeCoreInstance(), processorSupport);

        if (sourceRawType instanceof ClassProjection)
        {
            PostProcessor.processElement(matcher, sourceRawType, state, processorSupport);
        }

        VariableExpression firstParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(null, M3Paths.VariableExpression);
        firstParam._genericType(parametersValues.get(0)._genericType());
        firstParam._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));

        MutableList<ValueSpecification> params = Lists.mutable.<ValueSpecification>with(firstParam).withAll(ListHelper.tail(parametersValues));
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

        addTraceForParameterValue(functionExpression, 1, functionExpression._parametersValues().toList().get(1), processorSupport);
    }

    private static FunctionExpression reprocessPropertyForManySources(FunctionExpression functionExpression, ListIterable<? extends ValueSpecification> parametersValues, String propertyOrQualifiedPropertyNameProperty, GenericType sourceGenericType, ModelRepository repository, ProcessorSupport processorSupport)
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

        return functionExpression;
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
        StringBuilder functionSignatureBuilder = new StringBuilder();
        org.finos.legend.pure.m3.navigation.functionexpression.FunctionExpression.printFunctionSignatureFromExpression(functionSignatureBuilder, functionExpression, processorSupport);
        message.append(functionSignatureBuilder);
        SourceInformation functionExpressionSourceInformation = functionExpression.getSourceInformation();

        String functionName = getFunctionName(functionExpression);
        SetIterable<CoreInstance> possibleFunctions = processorSupport.function_getFunctionsForName(functionName);
        if ((0 < possibleFunctions.size()) && (possibleFunctions.size() < 20))
        {
            ImportGroup functionExpressionImportGroup = functionExpression._importGroup();
            SetIterable<String> alreadyImportedPackages = functionExpressionImportGroup._imports().collect(ImportAccessor::_path, Sets.mutable.empty());
            PartitionSet<CoreInstance> partition = possibleFunctions.partition(f ->
            {
                Package functionPackage = ((Function<?>) f)._package();
                return null == functionPackage._package() || alreadyImportedPackages.contains(PackageableElement.getUserPathForPackageableElement(functionPackage));
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
        if (null == functionName)
        {
            return null;
        }
        int index = functionName.lastIndexOf(':');
        return -1 == index ? functionName : functionName.substring(index + 1);
    }

    private static void populateFunctionCandidates(ProcessorState processorState, ProcessorSupport processorSupport, SourceInformation functionExpressionSourceInformation, SetIterable<CoreInstance> possibleFunctions, MutableList<CoreInstance> candidatesNotInCoreImports, MutableList<CoreInstance> candidatesInCoreImports, MutableSet<String> coreImports)
    {
        for (CoreInstance function : possibleFunctions)
        {
            if (Visibility.isVisibleInSource(function, null == functionExpressionSourceInformation ? null : functionExpressionSourceInformation.getSourceId(), processorState.getCodeStorage().getAllRepositories(), processorSupport))
            {
                CoreInstance pkg = ((Function<?>) function)._package();
                StringBuilder packageName = new StringBuilder();
                if (null != pkg && !M3Paths.Root.equals(pkg.getName()))
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
