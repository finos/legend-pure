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

package org.finos.legend.pure.m3.compiler.postprocessing.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.GeneratedMilestonedProperties;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningPropertyProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ProjectionUtil;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class AssociationProcessor extends Processor<Association>
{
    @Override
    public String getClassName()
    {
        return M3Paths.Association;
    }

    @Override
    public void process(Association association, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        association._properties().forEach(p -> PostProcessor.processElement(matcher, p, state, processorSupport));
        association._originalMilestonedProperties().forEach(p -> PostProcessor.processElement(matcher, p, state, processorSupport));
        association._qualifiedProperties().forEach(p -> PostProcessor.processElement(matcher, p, state, processorSupport));
    }

    @Override
    public void populateReferenceUsages(Association association, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (association instanceof AssociationProjection)
        {
            addReferenceUsageForToOneProperty(association, ((AssociationProjection) association)._projectedAssociationCoreInstance(), M3Properties.projectedAssociation, repository, processorSupport);
        }
    }

    public static Iterable<AbstractProperty<?>> process(Association association, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository) throws PureCompilationException
    {
        if (association instanceof AssociationProjection)
        {
            preProcessAssociationProjection((AssociationProjection) association, modelRepository, processorSupport);
        }

        // Process properties
        ListIterable<? extends Property<?, ?>> properties = ListHelper.wrapListIterable(association._properties());
        if (properties.size() != 2)
        {
            throw new PureCompilationException(association.getSourceInformation(), "Expected 2 properties for association '" + PackageableElement.getUserPathForPackageableElement(association) + "', found " + properties.size());
        }

        if (association instanceof AssociationProjection)
        {
            ListIterable<? extends CoreInstance> projections = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(((AssociationProjection) association)._projectionsCoreInstance()), processorSupport);
            if (projections.size() != 2)
            {
                throw new PureCompilationException(association.getSourceInformation(), "Expected exactly two class projections, found " + projections.size());
            }
        }

        // Get left and right properties
        Property<?, ?> leftProperty = properties.get(0);
        Property<?, ?> rightProperty = properties.get(1);

        // Get left and right types
        GenericType leftType = ((FunctionType) processorSupport.function_getFunctionType(rightProperty))._returnType();
        Type leftRawType = (Type) ImportStub.withImportStubByPass(leftType._rawTypeCoreInstance(), processorSupport);

        GenericType rightType = ((FunctionType) processorSupport.function_getFunctionType(leftProperty))._returnType();
        Type rightRawType = (Type) ImportStub.withImportStubByPass(rightType._rawTypeCoreInstance(), processorSupport);

        validateAssociationPropertiesRawTypes(association, leftRawType, rightRawType);

        return processAssociationProperties(association, context, processorSupport, modelRepository, leftProperty, rightProperty, leftType, (Class<?>) leftRawType, rightType, (Class<?>) rightRawType);
    }

    private static void preProcessAssociationProjection(AssociationProjection associationProjection, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        Association projectedAssociation = (Association) ImportStub.withImportStubByPass(associationProjection._projectedAssociationCoreInstance(), processorSupport);

        ListIterable<? extends CoreInstance> projections = ImportStub.withImportStubByPasses(ListHelper.wrapListIterable(associationProjection._projectionsCoreInstance()), processorSupport);
        projections.forEach(p -> checkForValidProjectionType(associationProjection, p));

        ListIterable<? extends Property<?, ?>> projectedProperties = ListHelper.wrapListIterable(projectedAssociation._properties());

        Property<?, ?> leftProperty = projectedProperties.getFirst();
        Property<?, ?> rightProperty = projectedProperties.getLast();

        Class<?> projectedPropertyLeftRawType = (Class<?>) ImportStub.withImportStubByPass(((FunctionType) processorSupport.function_getFunctionType(rightProperty))._returnType()._rawTypeCoreInstance(), processorSupport);
        Class<?> projectedPropertyRightRawType = (Class<?>) ImportStub.withImportStubByPass(((FunctionType) processorSupport.function_getFunctionType(leftProperty))._returnType()._rawTypeCoreInstance(), processorSupport);

        ClassProjection<?> firstProjection = (ClassProjection<?>) projections.getFirst();
        ClassProjection<?> lastProjection = (ClassProjection<?>) projections.getLast();

        Class<?> leftProjectedRawType = (Class<?>) ImportStub.withImportStubByPass(firstProjection._projectionSpecification()._type()._rawTypeCoreInstance(), processorSupport);
        Class<?> rightProjectedRawType = (Class<?>) ImportStub.withImportStubByPass(lastProjection._projectionSpecification()._type()._rawTypeCoreInstance(), processorSupport);

        ClassProjection<?> leftProjection = findProjectionTypeMatch(leftProjectedRawType, rightProjectedRawType, projectedPropertyLeftRawType, firstProjection, lastProjection, processorSupport);
        if (leftProjection == null)
        {
            throwInvalidProjectionException(associationProjection, rightProperty);
        }
        ClassProjection<?> rightProjection = findProjectionTypeMatch(leftProjectedRawType, rightProjectedRawType, projectedPropertyRightRawType, (ClassProjection<?>) projections.getFirst(), (ClassProjection<?>) projections.getLast(), processorSupport);
        if (rightProjection == null)
        {
            throwInvalidProjectionException(associationProjection, leftProperty);
        }

        Property<?, ?> leftPropertyCopy = (Property<?, ?>) ProjectionUtil.createPropertyCopy(leftProperty, associationProjection, modelRepository, processorSupport)
                ._owner(null);
        GenericType leftPropertyCopyGT = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(rightProjection, leftPropertyCopy.getSourceInformation(), processorSupport);
        replacePropertyGenericType(leftPropertyCopy, leftPropertyCopyGT);
        replacePropertyReturnType(leftPropertyCopy, leftPropertyCopyGT);

        Property<?, ?> rightPropertyCopy = (Property<?, ?>) ProjectionUtil.createPropertyCopy(rightProperty, associationProjection, modelRepository, processorSupport)
                ._owner(null);
        GenericType rightPropertyCopyGT = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(leftProjection, rightPropertyCopy.getSourceInformation(), processorSupport);
        replacePropertyGenericType(rightPropertyCopy, rightPropertyCopyGT);
        replacePropertyReturnType(rightPropertyCopy, rightPropertyCopyGT);

        associationProjection._propertiesAddAll(Lists.immutable.with(leftPropertyCopy, rightPropertyCopy));
    }

    private static void checkForValidProjectionType(AssociationProjection associationProjection, CoreInstance firstProjection)
    {
        if (!(firstProjection instanceof ClassProjection))
        {
            throw new PureCompilationException(associationProjection.getSourceInformation(), "AssociationProjection '" + PackageableElement.getUserPathForPackageableElement(associationProjection) + "' can only be applied to ClassProjections; '" + PackageableElement.getUserPathForPackageableElement(firstProjection) + "' is not a ClassProjection");
        }
    }

    private static ClassProjection<?> findProjectionTypeMatch(Class<?> projectedRawType1, Class<?> projectedRawType2, Class<?> projectedPropertyRawType1, ClassProjection<?> projectionType1, ClassProjection<?> projectionType2, ProcessorSupport processorSupport)
    {
        if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(projectedRawType1, projectedPropertyRawType1, processorSupport))
        {
            return projectionType1;
        }
        if (org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(projectedRawType2, projectedPropertyRawType1, processorSupport))
        {
            return projectionType2;
        }
        return null;
    }

    private static void throwInvalidProjectionException(AssociationProjection association, Property<?, ?> property)
    {
        StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Invalid AssociationProjection '"), association);
        builder.append("'. Projection for property '").append(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property)).append("' is not specified.");
        throw new PureCompilationException(association.getSourceInformation(), builder.toString());
    }

    private static Iterable<AbstractProperty<?>> processAssociationProperties(Association association, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository, Property<?, ?> leftProperty, Property<?, ?> rightProperty, GenericType leftType, Class<?> leftRawType, GenericType rightType, Class<?> rightRawType)
    {
        if ((leftRawType == rightRawType) && org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(leftProperty).equals(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(rightProperty)))
        {
            throw new PureCompilationException(association.getSourceInformation(), "Property conflict on association " + PackageableElement.getUserPathForPackageableElement(association) + ": property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(leftProperty) + "' defined more than once with the same target type");
        }

        processNonMilestonedQualifiedProperties(association, leftRawType, rightRawType, context, processorSupport);

        ListIterable<AbstractProperty<?>> leftGeneratedProperties = processAssociationProperty(association, leftProperty, leftType, leftRawType, modelRepository, context, processorSupport);
        ListIterable<AbstractProperty<?>> rightGeneratedProperties = processAssociationProperty(association, rightProperty, rightType, rightRawType, modelRepository, context, processorSupport);

        return Lists.mutable.<AbstractProperty<?>>ofInitialCapacity(leftGeneratedProperties.size() + rightGeneratedProperties.size()).withAll(leftGeneratedProperties).withAll(rightGeneratedProperties);
    }

    private static ListIterable<AbstractProperty<?>> processAssociationProperty(Association association, Property<?, ?> property, GenericType sourceGenericType, Class<?> sourceRawType, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        GeneratedMilestonedProperties generatedMilestonedProperties = MilestoningPropertyProcessor.processAssociationProperty(association, sourceRawType, property, context, processorSupport, modelRepository);
        if (generatedMilestonedProperties.hasGeneratedProperties())
        {
            processOriginalMilestonedProperty(association, property, sourceGenericType, context);

            Property<?, ?> edgePointProperty = (Property<?, ?>) generatedMilestonedProperties.getEdgePointProperty();
            processAssociationProperty_internal(edgePointProperty, sourceGenericType, sourceRawType, context);

            generatedMilestonedProperties.getQualifiedProperties().forEach(qp -> processAssociationQualifiedProperty_internal(association, (QualifiedProperty<?>) qp, sourceRawType, context, processorSupport));
            if (association.hasBeenValidated())
            {
                association.markNotValidated();
            }
            return generatedMilestonedProperties.getAllGeneratedProperties();
        }
        else
        {
            processAssociationProperty_internal(property, sourceGenericType, sourceRawType, context);
            return Lists.immutable.empty();
        }
    }

    private static void processOriginalMilestonedProperty(Association association, Property<?, ?> property, GenericType sourceGenericType, Context context)
    {
        replacePropertySourceType(property, sourceGenericType);
        MilestoningPropertyProcessor.moveProcessedOriginalMilestonedProperties(association, Lists.immutable.with(property), context);
        context.update(property);
    }

    private static void processAssociationProperty_internal(Property<?, ?> property, GenericType sourceGenericType, Class<?> sourceRawType, Context context)
    {
        replacePropertySourceType(property, sourceGenericType);
        addPropertyToRawType(property, sourceRawType, context);
        context.update(property);
    }

    private static void processAssociationQualifiedProperty_internal(Association association, QualifiedProperty<?> qualifiedProperty, Class<?> sourceRawType, Context context, ProcessorSupport processorSupport)
    {
        addQualifiedPropertyToRawType(qualifiedProperty, sourceRawType, context);

        Class<?> qualifiedPropertyReturnType = (Class<?>) ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);
        ListIterable<? extends Property<?, ?>> assnProperties = association._properties().toList();
        updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, context, processorSupport);

        context.update(qualifiedProperty);
    }

    private static void processNonMilestonedQualifiedProperties(Association association, Class<?> leftRawType, Class<?> rightRawType, Context context, ProcessorSupport processorSupport)
    {
        RichIterable<? extends QualifiedProperty<?>> qualifiedProperties = association._qualifiedProperties();
        if (qualifiedProperties.notEmpty())
        {
            SetIterable<? extends Class<?>> validReturnTypes = Sets.immutable.with(leftRawType, rightRawType);
            ListIterable<? extends Property<?, ?>> assnProperties = association._properties().toList();

            for (QualifiedProperty<?> qualifiedProperty : qualifiedProperties)
            {
                Class<?> qualifiedPropertyReturnType = (Class<?>) ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);

                validateQualifiedPropertyReturnType(association, qualifiedProperty, qualifiedPropertyReturnType, validReturnTypes);

                Class<?> sourceType = (leftRawType == qualifiedPropertyReturnType) ? rightRawType : leftRawType;
                addQualifiedPropertyToRawType(qualifiedProperty, sourceType, context);
                updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, context, processorSupport);
                context.update(qualifiedProperty);
            }
        }
    }

    private static void validateQualifiedPropertyReturnType(Association association, QualifiedProperty<?> qualifiedProperty, Class<?> qualifiedPropertyReturnType, SetIterable<? extends Class<?>> validTypes)
    {
        if (!validTypes.contains(qualifiedPropertyReturnType))
        {
            throw new PureCompilationException(qualifiedProperty.getSourceInformation(), qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + "has returnType of : " + qualifiedPropertyReturnType.getName() + " it should be one of Association: '" + association.getName() + "' properties' return types: " + validTypes.collect(CoreInstance::getName).makeString("[", ", ", "]"));
        }
    }

    private static void updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(Association association, QualifiedProperty<?> qualifiedProperty, Class<?> qualifiedPropertyReturnType, ListIterable<? extends Property<?, ?>> assnProperties, Context context, ProcessorSupport processorSupport)
    {
        validateQualifiedPropertyLeftSideOfFilterByPropertyName(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, processorSupport);
        Property<?, ?> leftSideOfFilterProperty = getQualifiedPropertiesFilterLeftSideParam(qualifiedPropertyReturnType, assnProperties, processorSupport);
        Property<?, ?> leftSideOfFilterOtherProperty = assnProperties.detect(p -> !leftSideOfFilterProperty.equals(p));
        GenericType leftSideOfFilterOtherPropertyGenericType = leftSideOfFilterOtherProperty._genericType();

        FunctionType functionType = (FunctionType) qualifiedProperty._classifierGenericType()._typeArguments().getOnly()._rawTypeCoreInstance();
        Iterable<? extends VariableExpression> functionTypeParams = functionType._parameters();
        for (VariableExpression functionTypeParam : functionTypeParams)
        {
            if (functionTypeParam != null && "this".equals(functionTypeParam._name()))
            {
                GenericType genericTypeCopy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(leftSideOfFilterOtherPropertyGenericType, true, processorSupport);
                functionTypeParam._genericType(genericTypeCopy);
                context.update(functionTypeParam);
                if (functionTypeParam.hasBeenValidated())
                {
                    functionTypeParam.markNotValidated();
                    context.update(functionTypeParam);
                }
            }
        }
    }

    private static Property<?, ?> getQualifiedPropertiesFilterLeftSideParam(Class<?> qualifiedPropertyReturnType, ListIterable<? extends Property<?, ?>> assnProperties, ProcessorSupport processorSupport)
    {
        return assnProperties.detect(property -> qualifiedPropertyReturnType.equals(ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport)));
    }

    private static void validateQualifiedPropertyLeftSideOfFilterByPropertyName(Association association, QualifiedProperty<?> qualifiedProperty, Class<?> qualifiedPropertyReturnType, ListIterable<? extends Property<?, ?>> assnProperties, ProcessorSupport processorSupport)
    {
        if (propertiesHaveDifferentNames(assnProperties))
        {
            String leftSideOfFilterPropertyName = getLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty);
            Property<?, ?> leftSideOfFilterProperty = assnProperties.detect(p -> leftSideOfFilterPropertyName.equals(p.getName()));
            Class<?> leftSideOfFilterPropertyReturnType = (Class<?>) ImportStub.withImportStubByPass(leftSideOfFilterProperty._genericType()._rawTypeCoreInstance(), processorSupport);
            if (leftSideOfFilterPropertyReturnType != qualifiedPropertyReturnType)
            {
                throw new PureCompilationException(qualifiedProperty.getSourceInformation(), qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + "should return a subset of property: '" + leftSideOfFilterPropertyName + "' (left side of filter) and consequently should have a returnType of : '" + leftSideOfFilterPropertyReturnType.getName() + "'");
            }
        }
    }

    private static boolean propertiesHaveDifferentNames(ListIterable<? extends Property<?, ?>> assnProperties)
    {
        return assnProperties.collect(CoreInstance::getName, Sets.mutable.empty()).size() == 2;
    }

    private static String getLeftSideOfQualifiedPropertyFilter(Association association, QualifiedProperty<?> qualifiedProperty)
    {
        ListIterable<? extends ValueSpecification> exprSequence = ListHelper.wrapListIterable(qualifiedProperty._expressionSequence());
        if (exprSequence.size() > 1)
        {
            throw new PureCompilationException(qualifiedProperty.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " has more than one Expression Sequence");
        }
        return getPropertyNameForLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty, exprSequence.getFirst());
    }

    private static String getPropertyNameForLeftSideOfQualifiedPropertyFilter(Association association, QualifiedProperty<?> qualifiedProperty, ValueSpecification instance)
    {
        if (instance instanceof FunctionExpression)
        {
            FunctionExpression functionExpression = (FunctionExpression) instance;
            String functionName = functionExpression._functionName();
            if ("filter".equals(functionName))
            {
                ValueSpecification leftSideOfFilter = ListHelper.wrapListIterable(functionExpression._parametersValues()).getFirst();
                if (leftSideOfFilter instanceof FunctionExpression)
                {
                    FunctionExpression leftSideOfFilterFunctionExpression = (FunctionExpression) leftSideOfFilter;
                    CoreInstance propertyName = leftSideOfFilterFunctionExpression._propertyName()._valuesCoreInstance().getAny();
                    if (propertyName != null)
                    {
                        ValueSpecification variableExpression = ListHelper.wrapListIterable(leftSideOfFilterFunctionExpression._parametersValues()).getFirst();
                        if (variableExpression instanceof VariableExpression)
                        {
                            String variableExpressionName = ((VariableExpression) variableExpression)._name();
                            if (!"this".equals(variableExpressionName))
                            {
                                throw new PureCompilationException(instance.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " left side of filter should refer to '$this' not '" + variableExpressionName + "'");
                            }
                        }
                        return propertyName.getName();
                    }
                }
                throw new PureCompilationException(instance.getSourceInformation(), "Could not get property name for left side of qualified property filter");
            }

            ValueSpecification firstParamValue = ListHelper.wrapListIterable(functionExpression._parametersValues()).getFirst();
            if (firstParamValue != null)
            {
                return getPropertyNameForLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty, firstParamValue);
            }
        }
        throw new PureCompilationException(qualifiedProperty.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " does not use the 'filter' function");
    }

    private static String qualifiedPropertyCompileErrorMsgPrefix(Association association, QualifiedProperty<?> qualifiedProperty)
    {
        return "Qualified property: '" + qualifiedProperty.getName() + "' in association: '" + association.getName() + "' ";
    }

    private static String validQualifiedPropertyInAssociationMsg()
    {
        return "Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. ";
    }

    private static void validateAssociationPropertiesRawTypes(Association association, Type leftRawType, Type rightRawType)
    {
        if (!(association instanceof AssociationProjection))
        {
            if (!(leftRawType instanceof Class))
            {
                throw new PureCompilationException(association.getSourceInformation(), "Association '" + PackageableElement.getUserPathForPackageableElement(association) + "' can only be applied to Classes; '" + PackageableElement.getUserPathForPackageableElement(leftRawType) + "' is not a Class");
            }
            if (!(rightRawType instanceof Class))
            {
                throw new PureCompilationException(association.getSourceInformation(), "Association '" + PackageableElement.getUserPathForPackageableElement(association) + "' can only be applied to Classes; '" + PackageableElement.getUserPathForPackageableElement(rightRawType) + "' is not a Class");
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addPropertyToRawType(Property property, Class rawType, Context context)
    {
        rawType._propertiesFromAssociationsAdd(property);
        updateContext(rawType, context);
    }

    private static void addQualifiedPropertyToRawType(QualifiedProperty<?> property, Class<?> rawType, Context context)
    {
        rawType._qualifiedPropertiesFromAssociationsAdd(property);
        updateContext(rawType, context);
    }

    private static void updateContext(Class<?> rawType, Context context)
    {
        context.update(rawType);
        if (rawType.hasBeenValidated())
        {
            rawType.markNotValidated();
        }
    }

    private static void replacePropertySourceType(Property<?, ?> property, GenericType newGenericType)
    {
        replacePropertyType(property, newGenericType, 0);
    }

    private static void replacePropertyReturnType(Property<?, ?> property, GenericType newGenericType)
    {
        replacePropertyType(property, newGenericType, 1);
    }

    private static void replacePropertyType(Property<?, ?> property, GenericType newGenericType, int index)
    {
        GenericType classifierGenericType = property._classifierGenericType();
        MutableList<GenericType> typeArguments = Lists.mutable.withAll(classifierGenericType._typeArguments());
        typeArguments.set(index, newGenericType);
        classifierGenericType._typeArguments(typeArguments);
    }

    private static void replacePropertyGenericType(Property<?, ?> property, GenericType genericType)
    {
        property._genericType(genericType);
    }
}
