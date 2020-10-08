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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.PropertyOwnerStrategy;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.NativeFunctionIdentifier;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

public class MilestoningDatesPropagationFunctions
{
    public static final String PATH_MILESTONING_DATES_VARIABLE_NAME = "p_milestoning_dates";

    private MilestoningDatesPropagationFunctions()
    {
    }

    public static <V> V possiblyExecuteInNewMilestoningDateContext(FunctionExpression fe, CoreInstance possibleLambda, Function<CoreInstance, V> function, ProcessorState processorState, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        V result;
        ListIterable<String> varNames = getMilestoningDatesVarNames(possibleLambda, processorSupport);
        MilestoningDates propagatedMilestoningDates = getMilestoningDatesForFunctionsWithLambda(fe, processorState, varNames, repository, context, processorSupport);
        if (propagatedMilestoningDates != null)
        {
            processorState.pushMilestoneDateContext(propagatedMilestoningDates, varNames);
            result = function.valueOf(possibleLambda);
            processorState.popMilestoneDateContext();
        }
        else
        {
            result = function.valueOf(possibleLambda);
        }
        return result;
    }

    private static MilestoningDates getMilestoningDatesForFunctionsWithLambda(FunctionExpression fe, ProcessorState processorState, ListIterable<String> varNames, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        if (varNames.isEmpty())
        {
            return null;
        }

        String functionName = fe._functionName();
        if ((functionName == null) || !functionSupportsMilestoningDatePropagationToLambda(functionName))
        {
            return null;
        }

        RichIterable<? extends ValueSpecification> paramValues = fe._parametersValues();
        return getMilestoningDatesForValidMilestoningDataSourceTypes(paramValues.getFirst(), processorState, repository, context, processorSupport);
    }

    public static MilestoningDates getMilestoningDatesForValidMilestoningDataSourceTypes(CoreInstance fe, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        for (MilestoningDateSourceType dateSourceType : MilestoningDateSourceType.values())
        {
            if (dateSourceType.isDataSourceType(fe, repository, processorSupport))
            {
                return dateSourceType.getMilestonedDates(fe, state, repository, context, processorSupport);
            }
        }
        return null;
    }

    private static ListIterable<String> getMilestoningDatesVarNames(CoreInstance value, ProcessorSupport processorSupport)
    {
        if (value instanceof LambdaFunction)
        {
            return getLambdaInputVarNames((LambdaFunction)value, processorSupport);
        }
        //TODO: move to m2-path
        if (Instance.instanceOf(value, M3Paths.Path, processorSupport))
        {
            return Lists.immutable.with(PATH_MILESTONING_DATES_VARIABLE_NAME);
        }
        if (value instanceof InstanceValue)
        {
            ListIterable<? extends CoreInstance> instanceValues = ((InstanceValue)value)._valuesCoreInstance().toList();
            if ((instanceValues.size() == 1) && instanceValues.getFirst() instanceof LambdaFunction)
            {
                return getLambdaInputVarNames((LambdaFunction)instanceValues.get(0), processorSupport);
            }
            if ((instanceValues.size() >= 1) && Instance.instanceOf(instanceValues.getFirst(), M3Paths.Path, processorSupport))
            {
                return Lists.immutable.with(PATH_MILESTONING_DATES_VARIABLE_NAME);
            }
        }
        return Lists.immutable.empty();
    }

    private static ListIterable<String> getLambdaInputVarNames(LambdaFunction lambdaFunction, ProcessorSupport processorSupport)
    {
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(lambdaFunction);
        return functionType._parameters().toList().collect(new Function<VariableExpression, String>()
        {
            @Override
            public String valueOf(VariableExpression parameter)
            {
                return parameter._name();
            }
        });
    }

    private static boolean functionSupportsMilestoningDatePropagationToLambda(String functionName)
    {
        return !NativeFunctionIdentifier.getNativeFunctionIdentifiersWithLambdaParamsAndMatchingFunctionName(functionName).isEmpty();
    }

    public static CoreInstance getMatchingMilestoningQualifiedPropertyWithDateArg(CoreInstance property, String propertyName, ProcessorSupport processorSupport)
    {   //TODO clean up this vs validation Milestoning Functions etc
        CoreInstance propertyReturnType = property instanceof AbstractProperty ? ImportStub.withImportStubByPass(((AbstractProperty)property)._genericType()._rawTypeCoreInstance(), processorSupport) : null;
        MilestoningStereotypeEnum milestoningStereotypeEnum = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(propertyReturnType, processorSupport).getFirst();
        ListIterable<String> temporalPropertyNames = milestoningStereotypeEnum.getTemporalDatePropertyNames();
        PropertyOwner owner = property instanceof AbstractProperty ? ((AbstractProperty)property)._owner() : null;
        ListMultimap<Integer, QualifiedProperty> relatedQpsByNameAndParamCount = getMilestonedQualifiedPropertiesRelatedByNameWithParamCount(owner, propertyName, processorSupport);
        return relatedQpsByNameAndParamCount.get(temporalPropertyNames.size()).getFirst();
    }

    private static ListMultimap<Integer, QualifiedProperty> getMilestonedQualifiedPropertiesRelatedByNameWithParamCount(PropertyOwner owner, String qualifiedPropertyName, final ProcessorSupport processorSupport)
    {
        PropertyOwnerStrategy propertyOwnerStrategy = PropertyOwnerStrategy.PROPERTY_OWNER_STRATEGY_FUNCTION.valueOf(owner);
        ListIterable<QualifiedProperty> allQualifiedProperties = LazyIterate.concatenate((Iterable<QualifiedProperty>)propertyOwnerStrategy.qualifiedProperties(owner), (Iterable<QualifiedProperty>)propertyOwnerStrategy.qualifiedPropertiesFromAssociations(owner)).toList();
        ListIterable<QualifiedProperty> milestonedQpsWithSameName = allQualifiedProperties.select(Predicates.and(new MilestoningFunctions.IsMilestonePropertyPredicate(processorSupport), Predicates.attributeEqual(MilestoningFunctions.TO_FUNCTION_NAME, qualifiedPropertyName)));
        return milestonedQpsWithSameName.groupBy(new Function<QualifiedProperty, Integer>()
        {
            @Override
            public Integer valueOf(QualifiedProperty qualifiedProperty)
            {
                FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(qualifiedProperty);
                RichIterable<? extends VariableExpression> functionTypeParams = functionType._parameters();
                return ListHelper.tail(functionTypeParams).size();
            }
        });
    }

    public static CoreInstance getMilestoningQualifiedPropertyWithAllDatesSupplied(FunctionExpression functionExpression, ProcessorState state, ModelRepository repository, Context context, ProcessorSupport processorSupport, CoreInstance propertyNameInstanceVal, CoreInstance source, String propertyName, CoreInstance propertyFunc)
    {
        MilestoningDates propagatedDates = getMilestoningDatesForValidMilestoningDataSourceTypes(source, state, repository, context, processorSupport);
        if (propagatedDates != null)
        {
            updateFunctionExpressionWithMilestoningDateParams(functionExpression, propertyNameInstanceVal, propertyFunc, propagatedDates, repository, processorSupport);
            propertyFunc = getMatchingMilestoningQualifiedPropertyWithDateArg(propertyFunc, propertyName, processorSupport);
        }
        return propertyFunc;
    }

    private static void updateFunctionExpressionWithMilestoningDateParams(FunctionExpression functionExpression, CoreInstance propertyNameInstanceVal, CoreInstance propertyFunc, MilestoningDates propagatedDate, ModelRepository repository, ProcessorSupport processorSupport)
    {
        applyPropertyFunctionExpressionMilestonedDates(functionExpression, propertyFunc, propagatedDate, repository, processorSupport);
        functionExpression._propertyNameRemove();
        functionExpression._qualifiedPropertyName((InstanceValue)propertyNameInstanceVal);
    }

    private static CoreInstance getMilestonedPropertyOwningType(CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance owningType;
        if (property instanceof QualifiedProperty)
        {
            FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(property);
            VariableExpression thisVar = functionType._parameters().getFirst();
            owningType = ImportStub.withImportStubByPass(thisVar._genericType()._rawTypeCoreInstance(), processorSupport);
        }
        else
        {
            owningType = ((AbstractProperty)property)._owner();
        }
        return owningType;
    }

    public static MilestoningDates getMilestonedDates(CoreInstance milestonedQualifiedProperty, ProcessorSupport processorSupport)
    {
        CoreInstance func = milestonedQualifiedProperty instanceof FunctionExpression ? ((FunctionExpression)milestonedQualifiedProperty)._funcCoreInstance() : null;
        CoreInstance propertyReturnType = (func != null && func instanceof AbstractProperty) ? ImportStub.withImportStubByPass(((AbstractProperty)func)._genericType()._rawTypeCoreInstance(), processorSupport) : null;
        MilestoningStereotype milestoningStereotype = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(propertyReturnType, processorSupport).getFirst();
        return new MilestoningDates(milestoningStereotype, ListHelper.<ValueSpecification>tail(milestonedQualifiedProperty instanceof FunctionExpression ? (RichIterable<ValueSpecification>)((FunctionExpression)milestonedQualifiedProperty)._parametersValues() : Lists.immutable.<ValueSpecification>empty()).toList());
    }

    private static Pair<MilestoningStereotypeEnum, MilestoningStereotypeEnum> getSourceTargetMilestoningStereotypeEnums(CoreInstance func, ProcessorSupport processorSupport)
    {
        CoreInstance sourceType = getMilestonedPropertyOwningType(func, processorSupport);
        MilestoningStereotypeEnum sourceTypeMilestoningEnum = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(sourceType, processorSupport).getFirst();

        CoreInstance targetType = func instanceof AbstractProperty ? ImportStub.withImportStubByPass(((AbstractProperty)func)._genericType()._rawTypeCoreInstance(), processorSupport) : null;
        MilestoningStereotypeEnum targetTypeMilestoningEnum = MilestoningFunctions.getTemporalStereoTypesFromTopMostNonTopTypeGeneralizations(targetType, processorSupport).getFirst();
        return Tuples.pair(sourceTypeMilestoningEnum, targetTypeMilestoningEnum);
    }

    public static void applyPropertyFunctionExpressionMilestonedDates(FunctionExpression fe, CoreInstance func, MilestoningDates milestoningDates, ModelRepository repository, ProcessorSupport processorSupport)
    {
        Pair<MilestoningStereotypeEnum, MilestoningStereotypeEnum> sourceTargetMilestoningStereotypeEnums = getSourceTargetMilestoningStereotypeEnums(func, processorSupport);
        MilestoningStereotypeEnum sourceTypeMilestoningEnum = sourceTargetMilestoningStereotypeEnums.getOne();
        MilestoningStereotypeEnum targetTypeMilestoningEnum = sourceTargetMilestoningStereotypeEnums.getTwo();

        MutableList<? extends ValueSpecification> parametersValues = fe._parametersValues().toList();
        ValueSpecification[] milestoningDateParameters = new ValueSpecification[targetTypeMilestoningEnum.getTemporalDatePropertyNames().size()];

        fe._originalMilestonedPropertyCoreInstance(func);
        fe._originalMilestonedPropertyParametersValues(parametersValues);

        if (isBiTemporal(targetTypeMilestoningEnum))
        {
            if (isBiTemporal(sourceTypeMilestoningEnum) && oneDateParamSupplied(parametersValues))
            {
                setBiTemporaDates(milestoningDateParameters, new MilestoningDates(parametersValues.get(1), milestoningDates.getProcessingDate()));
            }
            else if (isSingleDateTemporal(sourceTypeMilestoningEnum) && oneDateParamSupplied(parametersValues))
            {
                int propagatedDateIndex = sourceTypeMilestoningEnum.positionInTemporalParameterValues();
                CoreInstance propagatedDate = milestoningDates.getMilestoningDate(sourceTypeMilestoningEnum);
                int otherPropagatedDateIndex = (sourceTypeMilestoningEnum == MilestoningStereotypeEnum.processingtemporal ? MilestoningStereotypeEnum.businesstemporal : MilestoningStereotypeEnum.processingtemporal).positionInTemporalParameterValues();
                setMilestoningDateParameters(milestoningDateParameters, propagatedDateIndex, propagatedDate);
                setMilestoningDateParameters(milestoningDateParameters, otherPropagatedDateIndex, parametersValues.get(1));
            }
            if (isBiTemporal(sourceTypeMilestoningEnum) && noDateParamSupplied(parametersValues))
            {
                setBiTemporaDates(milestoningDateParameters, milestoningDates);
            }
        }
        else if (isSingleDateTemporal(targetTypeMilestoningEnum) && noDateParamSupplied(parametersValues))
        {
            if (isBiTemporal(sourceTypeMilestoningEnum))
            {
                CoreInstance propagatedDate = milestoningDates.getMilestoningDate(targetTypeMilestoningEnum);
                setMilestoningDateParameters(milestoningDateParameters, 0, propagatedDate);
            }
            if (sourceTypeMilestoningEnum == targetTypeMilestoningEnum)
            {
                setMilestoningDateParameters(milestoningDateParameters, 0, milestoningDates.getMilestoningDate(targetTypeMilestoningEnum));
            }
        }
        if (!ArrayIterate.isEmpty(milestoningDateParameters))
        {
            parametersValues = LazyIterate.concatenate(FastList.<ValueSpecification>newListWith(parametersValues.get(0)), FastList.newListWith(milestoningDateParameters)).toList();
            fe._parametersValues(parametersValues);
        }
    }

    private static void setMilestoningDateParameters(CoreInstance[] dateParamValues, int index, CoreInstance milestoningDate)
    {
        dateParamValues[index] = milestoningDate;
    }

    private static void setBiTemporaDates(CoreInstance[] dateParamValues, MilestoningDates milestoningDates)
    {
        setMilestoningDateParameters(dateParamValues, MilestoningStereotypeEnum.processingtemporal.positionInTemporalParameterValues(), milestoningDates.getProcessingDate());
        setMilestoningDateParameters(dateParamValues, MilestoningStereotypeEnum.businesstemporal.positionInTemporalParameterValues(), milestoningDates.getBusinessDate());
    }

    private static boolean isBiTemporal(MilestoningStereotypeEnum milestoningEnum)
    {
        return milestoningEnum != null && milestoningEnum == MilestoningStereotypeEnum.bitemporal;
    }

    private static boolean isProcessingTemporal(MilestoningStereotypeEnum milestoningEnum)
    {
        return milestoningEnum != null && milestoningEnum == MilestoningStereotypeEnum.processingtemporal;
    }

    private static boolean isSingleDateTemporal(MilestoningStereotypeEnum milestoningEnum)
    {
        return milestoningEnum != null && isProcessingTemporal(milestoningEnum) || milestoningEnum == MilestoningStereotypeEnum.businesstemporal;
    }

    private static boolean noDateParamSupplied(ListIterable<? extends CoreInstance> parameterValues)
    {
        return parameterValues.size() == 1;
    }

    private static boolean oneDateParamSupplied(ListIterable<? extends CoreInstance> parameterValues)
    {
        return parameterValues.size() == 2;
    }

    public static void undoAutoGenMilestonedQualifier(FunctionExpression fe, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        CoreInstance originalMilestonedProperty = fe._originalMilestonedPropertyCoreInstance();
        RichIterable<? extends ValueSpecification> originalMilestonedParametersValues = fe._originalMilestonedPropertyParametersValues();
        if (originalMilestonedProperty != null)
        {
            fe._funcCoreInstance(originalMilestonedProperty);
            fe._parametersValues(originalMilestonedParametersValues);
            fe._originalMilestonedPropertyRemove();
            fe._originalMilestonedPropertyParametersValuesRemove();
            resetFunctionExpressionMilestonedPropertyName(fe, originalMilestonedProperty, modelRepository, processorSupport);
        }
    }

    private static void resetFunctionExpressionMilestonedPropertyName(FunctionExpression fe, CoreInstance originalMilestonedProperty, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(originalMilestonedProperty);
        RichIterable<? extends VariableExpression> functionTypeParams = functionType._parameters();
        int originalMilestonedQualifiedPropertyParamCount = functionTypeParams == null ? 0 : functionTypeParams.size() - 1;
        if(originalMilestonedQualifiedPropertyParamCount == 0){
            InstanceValue qualifiedPropertyName = fe._qualifiedPropertyName();
            fe._qualifiedPropertyNameRemove();
            fe._propertyName(qualifiedPropertyName);
        }
    }

}
