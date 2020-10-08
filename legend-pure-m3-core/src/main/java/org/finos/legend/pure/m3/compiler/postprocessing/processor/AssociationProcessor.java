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
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.GeneratedMilestonedProperties;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningPropertyProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.projection.ProjectionUtil;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
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
        RichIterable<? extends AbstractProperty> properties = association._properties();
        RichIterable<? extends AbstractProperty> originalMilestonedProperties = association._originalMilestonedProperties();
        RichIterable<? extends AbstractProperty> qualifiedProperties = association._qualifiedProperties();

        for (AbstractProperty property : LazyIterate.concatenate((Iterable<AbstractProperty>)properties, (Iterable<AbstractProperty>)qualifiedProperties).concatenate((Iterable<AbstractProperty>)originalMilestonedProperties))
        {
            PostProcessor.processElement(matcher, property, state, processorSupport);
        }
    }

    @Override
    public void populateReferenceUsages(Association association, ModelRepository repository, ProcessorSupport processorSupport)
    {
        if (association instanceof AssociationProjection)
        {
            addReferenceUsageForToOneProperty(association, ((AssociationProjection)association)._projectedAssociationCoreInstance(), M3Properties.projectedAssociation, repository, processorSupport);
        }
    }

    public static Iterable<AbstractProperty<?>> process(Association association, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository) throws PureCompilationException
    {
        if (association instanceof AssociationProjection)
        {
            preProcessAssociationProjection((AssociationProjection)association, modelRepository, context, processorSupport);
        }
        // TODO what if the generic type on the properties has concrete type arguments?
        // Process properties
        ListIterable<? extends Property<?, ?>> properties = association._properties().toList();

        if (properties.size() != 2)
        {
            throw new PureCompilationException(association.getSourceInformation(), "Expected 2 properties for association '" + PackageableElement.getUserPathForPackageableElement(association) + "', found " + properties.size());
        }

        // Get left and right properties
        Property leftProperty = properties.get(0);
        Property rightProperty = properties.get(1);

        // Get left and right types
        GenericType leftType;
        Type leftRawType;
        GenericType rightType;
        Type rightRawType;
        if (association instanceof AssociationProjection)
        {
            ListIterable<? extends ClassProjection> projections = (ListIterable<? extends ClassProjection>)ImportStub.withImportStubByPasses(((AssociationProjection)association)._projectionsCoreInstance().toList(), processorSupport);
            if (projections.size() != 2)
            {
                throw new PureCompilationException(association.getSourceInformation(), "Expected exactly two class projections, found " + projections.size());
            }
        }

        leftType = ((FunctionType)processorSupport.function_getFunctionType(rightProperty))._returnType();
        leftRawType = (Type)ImportStub.withImportStubByPass(leftType._rawTypeCoreInstance(), processorSupport);

        rightType = ((FunctionType)processorSupport.function_getFunctionType(leftProperty))._returnType();
        rightRawType = (Type)ImportStub.withImportStubByPass(rightType._rawTypeCoreInstance(), processorSupport);

        validateAssociationPropertiesRawTypes(association, context, leftRawType, rightRawType);

        return processAssociationProperties(association, context, processorSupport, modelRepository, leftProperty, rightProperty, leftType, (Class)leftRawType, rightType, (Class)rightRawType);
    }

    private static void preProcessAssociationProjection(AssociationProjection associationProjection, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        Association projectedAssociation = (Association)ImportStub.withImportStubByPass(associationProjection._projectedAssociationCoreInstance(), processorSupport);

        ListIterable<? extends CoreInstance> projections = ImportStub.withImportStubByPasses(associationProjection._projectionsCoreInstance().toList(), processorSupport);

        ListIterable<? extends Property<?, ?>> projectedProperties = projectedAssociation._properties().toList();

        Property leftProperty = projectedProperties.getFirst();
        Property rightProperty = projectedProperties.getLast();

        Class projectedPropertyLeftRawType = (Class)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(rightProperty))._returnType()._rawTypeCoreInstance(), processorSupport);
        Class projectedPropertyRightRawType = (Class)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(leftProperty))._returnType()._rawTypeCoreInstance(), processorSupport);

        checkForValidProjectionType(associationProjection, projections.getFirst(), context);
        checkForValidProjectionType(associationProjection, projections.getLast(), context);

        Class leftProjectedRawType = (Class)ImportStub.withImportStubByPass(((ClassProjection)projections.getFirst())._projectionSpecification()._type()._rawTypeCoreInstance(), processorSupport);

        Class rightProjectedRawType = (Class)ImportStub.withImportStubByPass(((ClassProjection)projections.getLast())._projectionSpecification()._type()._rawTypeCoreInstance(), processorSupport);

        MutableList<Property<?, ?>> projectedPropertiesCopy = FastList.newList(2);

        ClassProjection leftProjection = findProjectionTypeMatch(leftProjectedRawType, rightProjectedRawType, projectedPropertyLeftRawType, (ClassProjection)projections.getFirst(), (ClassProjection)projections.getLast(), context, processorSupport);
        if (leftProjection == null)
        {
            throwInvalidProjectionException(associationProjection, projectedPropertyLeftRawType, rightProperty);
        }
        Property leftPropertyCopy = (Property)ProjectionUtil.createPropertyCopy(leftProperty, associationProjection, modelRepository, processorSupport);
        leftPropertyCopy._owner(null);
        GenericType leftProjectionGTCopy = (GenericType)org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(leftProjection, processorSupport);
        projectedPropertiesCopy.add(leftPropertyCopy);

        ClassProjection rightProjection = findProjectionTypeMatch(leftProjectedRawType, rightProjectedRawType, projectedPropertyRightRawType, (ClassProjection)projections.getFirst(), (ClassProjection)projections.getLast(), context, processorSupport);
        if (rightProjection == null)
        {
            throwInvalidProjectionException(associationProjection, projectedPropertyRightRawType, leftProperty);
        }

        Property rightPropertyCopy = (Property)ProjectionUtil.createPropertyCopy(rightProperty, associationProjection, modelRepository, processorSupport);
        rightPropertyCopy._owner(null);
        GenericType rightProjectionGTCopy = (GenericType)org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(rightProjection, processorSupport);
        projectedPropertiesCopy.add(rightPropertyCopy);

        replacePropertyGenericType(leftPropertyCopy, rightProjectionGTCopy, context);
        replacePropertyReturnType(leftPropertyCopy, rightProjectionGTCopy, context);

        replacePropertyGenericType(rightPropertyCopy, leftProjectionGTCopy, context);
        replacePropertyReturnType(rightPropertyCopy, leftProjectionGTCopy, context);


        associationProjection._propertiesCoreInstance(Lists.immutable.<Property<?,?>>ofAll(associationProjection._properties()).newWithAll(projectedPropertiesCopy));
    }

    private static void checkForValidProjectionType(AssociationProjection associationProjection, CoreInstance firstProjection, Context context)
    {
        if (!(firstProjection instanceof ClassProjection))
        {
            throw new PureCompilationException(associationProjection.getSourceInformation(), "AssociationProjection '" + PackageableElement.getUserPathForPackageableElement(associationProjection) + "' can only be applied to ClassProjections; '" + PackageableElement.getUserPathForPackageableElement(firstProjection) + "' is not a ClassProjection");
        }
    }

    private static ClassProjection findProjectionTypeMatch(Class projectedRawType1, Class projectedRawType2, Class projectedPropertyRawType1, ClassProjection projectionType1, ClassProjection projectionType2, Context context, ProcessorSupport processorSupport)
    {
        return org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(projectedRawType1, projectedPropertyRawType1, processorSupport) ? projectionType1 :
                org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(projectedRawType2, projectedPropertyRawType1, processorSupport) ? projectionType2 : null;
    }

    private static void throwInvalidProjectionException(AssociationProjection association, Class projectedPropertyRawType, Property property)
    {
        throw new PureCompilationException(association.getSourceInformation(), String.format("Invalid AssociationProjection. Projection for property '%s' is not specified.", org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property), PackageableElement.getUserPathForPackageableElement(projectedPropertyRawType)));
    }

    private static Iterable<AbstractProperty<?>> processAssociationProperties(Association association, Context context, ProcessorSupport processorSupport, ModelRepository modelRepository, Property leftProperty, Property rightProperty, GenericType leftType, Class leftRawType, GenericType rightType, Class rightRawType)
    {
        if ((leftRawType == rightRawType) && org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(leftProperty).equals(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(rightProperty)))
        {
            throw new PureCompilationException(association.getSourceInformation(), "Property conflict on association " + PackageableElement.getUserPathForPackageableElement(association) + ": property '" + org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(leftProperty) + "' defined more than once with the same target type");
        }

        processNonMilestonedQualifiedProperties(association, leftRawType, rightRawType, context, processorSupport);

        ListIterable<AbstractProperty<?>> leftGeneratedProperties = processAssociationProperty(association, leftProperty, leftType, leftRawType, modelRepository, context, processorSupport);
        ListIterable<AbstractProperty<?>> rightGeneratedProperties = processAssociationProperty(association, rightProperty, rightType, rightRawType, modelRepository, context, processorSupport);

        return LazyIterate.concatenate(leftGeneratedProperties, rightGeneratedProperties);
    }

    private static ListIterable<AbstractProperty<?>> processAssociationProperty(Association association, Property property, GenericType sourceGenericType, Class sourceRawType, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        GeneratedMilestonedProperties generatedMilestonedProperties = MilestoningPropertyProcessor.processAssociationProperty(association, sourceRawType, property, context, processorSupport, modelRepository);
        if (generatedMilestonedProperties.hasGeneratedProperties())
        {
            processOriginalMilestonedProperty(association, property, sourceGenericType, context, processorSupport);

            Property edgePointProperty = (Property)generatedMilestonedProperties.getEdgePointProperty();
            processAssociationProperty_internal(edgePointProperty, sourceGenericType, sourceRawType, context);

            for (CoreInstance qualifiedProperty : generatedMilestonedProperties.getQualifiedProperties())
            {
                processAssociationQualifiedProperty_internal(association, (QualifiedProperty)qualifiedProperty, sourceRawType, context, processorSupport);
            }
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

    private static void processOriginalMilestonedProperty(Association association, Property<?, ?> property, GenericType sourceGenericType, Context context, ProcessorSupport processorSupport)
    {
        replacePropertySourceType(property, sourceGenericType, context);
        MilestoningPropertyProcessor.moveProcessedoriginalMilestonedProperties(association, FastList.<Property<?, ?>>newListWith(property), context, processorSupport);
        context.update(property);
    }

    private static void processAssociationProperty_internal(Property property, GenericType sourceGenericType, Class sourceRawType, Context context)
    {
        replacePropertySourceType(property, sourceGenericType, context);
        addPropertyToRawType(property, sourceRawType, context);
        context.update(property);
    }

    private static void processAssociationQualifiedProperty_internal(Association association, QualifiedProperty qualifiedProperty, Class sourceRawType, Context context, ProcessorSupport processorSupport)
    {
        addQualifiedPropertyToRawType(qualifiedProperty, sourceRawType, context);

        Class qualifiedPropertyReturnType = (Class)ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);
        ListIterable<? extends Property<?, ?>> assnProperties = association._properties().toList();
        updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, context, processorSupport);

        context.update(qualifiedProperty);
    }

    private static void processNonMilestonedQualifiedProperties(Association association, Class leftRawType, Class rightRawType, Context context, ProcessorSupport processorSupport)
    {
        RichIterable<? extends QualifiedProperty> qualifiedProperties = association._qualifiedProperties();
        if (qualifiedProperties.notEmpty())
        {
            SetIterable<? extends Class> validReturnTypes = Sets.immutable.with(leftRawType, rightRawType);
            ListIterable<? extends Property<?, ?>> assnProperties = association._properties().toList();

            for (QualifiedProperty qualifiedProperty : qualifiedProperties)
            {
                Class qualifiedPropertyReturnType = (Class)ImportStub.withImportStubByPass(qualifiedProperty._genericType()._rawTypeCoreInstance(), processorSupport);

                validateQualifiedPropertyReturnType(association, qualifiedProperty, qualifiedPropertyReturnType, validReturnTypes);

                Class sourceType = (leftRawType == qualifiedPropertyReturnType) ? rightRawType : leftRawType;
                addQualifiedPropertyToRawType(qualifiedProperty, sourceType, context);
                updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, context, processorSupport);
                context.update(qualifiedProperty);
            }
        }
    }

    private static void validateQualifiedPropertyReturnType(Association association, QualifiedProperty qualifiedProperty, Class qualifiedPropertyReturnType, SetIterable<? extends Class> validTypes)
    {
        if (!validTypes.contains(qualifiedPropertyReturnType))
        {
            throw new PureCompilationException(qualifiedProperty.getSourceInformation(), qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + "has returnType of : " + qualifiedPropertyReturnType.getName() + " it should be one of Association: '" + association.getName() + "' properties' return types: " + validTypes.collect(CoreInstance.GET_NAME).makeString("[", ", ", "]"));
        }
    }

    private static void updateClassifierGenericTypeForQualifiedPropertiesThisVarExprParams(Association association, QualifiedProperty qualifiedProperty, final Class qualifiedPropertyReturnType, ListIterable<? extends Property> assnProperties, final Context context, final ProcessorSupport processorSupport)
    {
        validateQualifiedPropertyLeftSideOfFilterByPropertyName(association, qualifiedProperty, qualifiedPropertyReturnType, assnProperties, context, processorSupport);
        Property leftSideOfFilterProperty = getQualifiedPropertiesFilterLeftSideParam(qualifiedPropertyReturnType, assnProperties, processorSupport);
        Property leftSideOfFilterOtherProperty = assnProperties.detect(Predicates.notEqual(leftSideOfFilterProperty));
        GenericType leftSideOfFilterOtherPropertyGenericType = leftSideOfFilterOtherProperty._genericType();

        FunctionType functionType = (FunctionType)qualifiedProperty._classifierGenericType()._typeArguments().toList().getFirst()._rawTypeCoreInstance();
        Iterable<? extends VariableExpression> functionTypeParams = functionType._parameters();
        for (VariableExpression functionTypeParam : functionTypeParams)
        {
            if (functionTypeParam != null && "this".equals(functionTypeParam._name()))
            {
                GenericType genericTypeCopy = (GenericType)org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(leftSideOfFilterOtherPropertyGenericType, processorSupport);
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

    private static Property getQualifiedPropertiesFilterLeftSideParam(final Class qualifiedPropertyReturnType, ListIterable<? extends Property> assnProperties, final ProcessorSupport processorSupport)
    {
        Property leftSideOfFilterProperty = assnProperties.detect(new Predicate<Property>()
        {
            @Override
            public boolean accept(Property property)
            {
                Class returnType = (Class)ImportStub.withImportStubByPass(property._genericType()._rawTypeCoreInstance(), processorSupport);
                return returnType.equals(qualifiedPropertyReturnType);
            }
        });
        return leftSideOfFilterProperty;
    }

    private static void validateQualifiedPropertyLeftSideOfFilterByPropertyName(Association association, QualifiedProperty qualifiedProperty, Class qualifiedPropertyReturnType, ListIterable<? extends Property> assnProperties, Context context, ProcessorSupport processorSupport)
    {
        if (propertiesHaveDifferentNames(assnProperties))
        {
            String leftSideOfFilterPropertyName = getLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty, context, processorSupport);
            Property leftSideOfFilterProperty = assnProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, leftSideOfFilterPropertyName));
            Class leftSideOfFilterPropertyReturnType = (Class)ImportStub.withImportStubByPass(leftSideOfFilterProperty._genericType()._rawTypeCoreInstance(), processorSupport);
            if (leftSideOfFilterPropertyReturnType != qualifiedPropertyReturnType)
            {
                throw new PureCompilationException(qualifiedProperty.getSourceInformation(), qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + "should return a subset of property: '" + leftSideOfFilterPropertyName + "' (left side of filter) and consequently should have a returnType of : '" + leftSideOfFilterPropertyReturnType.getName() + "'");
            }
        }
    }

    private static boolean propertiesHaveDifferentNames(ListIterable<? extends Property> assnProperties)
    {
        return assnProperties.collect(CoreInstance.GET_NAME).distinct().size() == 2;
    }

    private static String getLeftSideOfQualifiedPropertyFilter(Association association, QualifiedProperty qualifiedProperty, Context context, ProcessorSupport processorSupport)
    {
        ListIterable<? extends ValueSpecification> exprSequence = qualifiedProperty._expressionSequence().toList();
        if (exprSequence.size() > 1)
        {
            throw new PureCompilationException(qualifiedProperty.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " has more than one Expression Sequence");
        }
        return getPropertyNameForLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty, exprSequence.getFirst(), context, processorSupport);
    }

    private static String getPropertyNameForLeftSideOfQualifiedPropertyFilter(Association association, QualifiedProperty qualifiedProperty, ValueSpecification instance, Context context, ProcessorSupport processorSupport)
    {
        String functionName = instance instanceof FunctionExpression ? ((FunctionExpression)instance)._functionName() : null;
        String propertyNameForLeftSideOfQualifiedPropertyFilter;
        if ("filter".equals(functionName))
        {
            ValueSpecification leftSideOfFilter = ((FunctionExpression)instance)._parametersValues().toList().getFirst();

            CoreInstance propertyName = leftSideOfFilter instanceof FunctionExpression ? ((FunctionExpression)leftSideOfFilter)._propertyName()._valuesCoreInstance().toList().getFirst() : null;
            ValueSpecification variableExpression = leftSideOfFilter instanceof FunctionExpression ? ((FunctionExpression)leftSideOfFilter)._parametersValues().toList().getFirst() : null;
            String variableExpressionName = variableExpression instanceof VariableExpression ? ((VariableExpression)variableExpression)._name() : null;
            if (!"this".equals(variableExpressionName))
            {
                throw new PureCompilationException(instance.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " left side of filter should refer to '$this' not '" + variableExpressionName + "'");
            }
            propertyNameForLeftSideOfQualifiedPropertyFilter = propertyName.getName();
        }
        else
        {
            ValueSpecification firstParamValue = instance instanceof FunctionExpression ? ((FunctionExpression)instance)._parametersValues().toList().getFirst() : null;
            if (firstParamValue != null)
            {
                propertyNameForLeftSideOfQualifiedPropertyFilter = getPropertyNameForLeftSideOfQualifiedPropertyFilter(association, qualifiedProperty, firstParamValue, context, processorSupport);
            }
            else
            {
                throw new PureCompilationException(qualifiedProperty.getSourceInformation(), validQualifiedPropertyInAssociationMsg() + qualifiedPropertyCompileErrorMsgPrefix(association, qualifiedProperty) + " does not use the 'filter' function");
            }
        }
        return propertyNameForLeftSideOfQualifiedPropertyFilter;
    }

    private static String qualifiedPropertyCompileErrorMsgPrefix(Association association, QualifiedProperty qualifiedProperty)
    {
        return "Qualified property: '" + qualifiedProperty.getName() + "' in association: '" + association.getName() + "' ";
    }

    private static String validQualifiedPropertyInAssociationMsg()
    {
        return "Association Qualified Properties must follow the following pattern '$this.<<associationProperty>>->filter(p|<<lambdaExpression>>)'. ";
    }

    private static void validateAssociationPropertiesRawTypes(Association association, Context context, Type leftRawType, Type rightRawType)
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

    private static void replacePropertySourceType(Property property, GenericType newGenericType, Context context)
    {
        MutableList<GenericType> typeArguments = (MutableList<GenericType>)property._classifierGenericType()._typeArguments().toList();
        typeArguments.set(0, newGenericType);
        property._classifierGenericType()._typeArguments(typeArguments);
    }

    private static void addPropertyToRawType(Property property, Class rawType, Context context)
    {
        rawType._propertiesFromAssociations(Lists.immutable.ofAll(rawType._propertiesFromAssociations()).newWith(property));
        updateContext(rawType, context);
    }

    private static void addQualifiedPropertyToRawType(QualifiedProperty property, Class rawType, Context context)
    {
        rawType._qualifiedPropertiesFromAssociations(Lists.immutable.ofAll(rawType._qualifiedPropertiesFromAssociations()).newWith(property));
        updateContext(rawType, context);
    }

    private static void updateContext(Class rawType, Context context)
    {
        context.update(rawType);
        if (rawType.hasBeenValidated())
        {
            rawType.markNotValidated();
        }
    }

    private static void replacePropertyReturnType(Property property, GenericType newGenericType, Context context)
    {
        MutableList<GenericType> typeArguments = (MutableList<GenericType>)property._classifierGenericType()._typeArguments().toList();
        typeArguments.set(1, newGenericType);
        property._classifierGenericType()._typeArguments(typeArguments);
    }

    private static void replacePropertyGenericType(Property property, GenericType genericType, Context context)
    {
        property._genericType(genericType);
    }
}
