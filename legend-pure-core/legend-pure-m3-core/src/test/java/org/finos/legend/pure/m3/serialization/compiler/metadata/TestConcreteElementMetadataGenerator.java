// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.ReferenceIdExtensionV1;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConcreteElementMetadataGenerator extends AbstractReferenceTest
{
    private static ReferenceIdProviders referenceIds;
    private static ConcreteElementMetadataGenerator generator;

    @BeforeClass
    public static void setUpGenerator()
    {
        referenceIds = ReferenceIdProviders.builder().withProcessorSupport(processorSupport).withExtension(new ReferenceIdExtensionV1()).build();
        generator = new ConcreteElementMetadataGenerator(referenceIds.provider(), processorSupport);
    }

    @Test
    public void testSimpleClass()
    {
        String path = "test::model::SimpleClass";
        Class<?> simpleClass = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(simpleClass))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, simpleClass.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Class, M3Paths.Integer, M3Paths.Property, M3Paths.PureOne, M3Paths.String, "test::model")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['id'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['name'].classifierGenericType", "rawType"))
                .build();
        assertMetadata(expected, simpleClass);
    }

    @Test
    public void testEnumeration()
    {
        String path = "test::model::SimpleEnumeration";
        Enumeration<? extends Enum<?>> simpleEnumeration = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(simpleEnumeration))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Enumeration, simpleEnumeration.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.Enum, M3Paths.Enumeration, "test::model")
                .withBackReferences(
                        M3Paths.Enum,
                        M3Paths.Enum,
                        specialization(path + ".generalizations[0]"))
                .build();
        assertMetadata(expected, simpleEnumeration);
    }

    @Test
    public void testAssociation()
    {
        String path = "test::model::LeftRight";
        String leftPath = "test::model::Left";
        String rightPath = "test::model::Right";

        Association leftRight = getCoreInstance(path);
        ModuleMetadata expectedLR = ModuleMetadata.builder(ModuleHelper.getElementModule(leftRight))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Association, leftRight.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Association, M3Paths.Boolean, M3Paths.Integer, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.QualifiedProperty, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "test::model", leftPath, rightPath, leftPath + ".properties['name']", rightPath + ".properties['id']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['toLeft'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toRight'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_",
                        "meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0]"))
                .withBackReferences(
                        leftPath,
                        leftPath,
                        propFromAssoc(path + ".properties['toRight']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toRight(Integer[1])']"),
                        refUsage(path + ".properties['toLeft'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toRight'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['l'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"))
                .withBackReferences(
                        leftPath,
                        leftPath + ".properties['name']",
                        application(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        rightPath,
                        rightPath,
                        propFromAssoc(path + ".properties['toLeft']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toLeft(String[1])']"),
                        refUsage(path + ".properties['toLeft'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".properties['toRight'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['r'].genericType", "rawType"))
                .withBackReferences(
                        rightPath,
                        rightPath + ".properties['id']",
                        application(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .build();
        assertMetadata(expectedLR, leftRight);

        Class<?> left = getCoreInstance(leftPath);
        ModuleMetadata expectedLeft = ModuleMetadata.builder(ModuleHelper.getElementModule(left))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(leftPath, M3Paths.Class, left.getSourceInformation()))
                .withExternalReferences(
                        leftPath,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Class, M3Paths.Property, M3Paths.PureOne, M3Paths.String, "test::model")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(leftPath + ".properties['name'].classifierGenericType", "rawType"))
                .build();
        assertMetadata(expectedLeft, left);

        Class<?> right = getCoreInstance(rightPath);
        ModuleMetadata expectedRight = ModuleMetadata.builder(ModuleHelper.getElementModule(right))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(rightPath, M3Paths.Class, right.getSourceInformation()))
                .withExternalReferences(
                        rightPath,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Class, M3Paths.Integer, M3Paths.Property, M3Paths.PureOne, "test::model")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(rightPath + ".properties['id'].classifierGenericType", "rawType"))
                .build();
        assertMetadata(expectedRight, right);
    }

    @Test
    public void testSimpleProfile()
    {
        String path = "test::model::SimpleProfile";
        Profile simpleProfile = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(simpleProfile))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Profile, simpleProfile.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.Profile, "test::model")
                .build();
        assertMetadata(expected, simpleProfile);
    }

    @Test
    public void testClassWithGeneralizations()
    {
        String path = "test::model::BothSides";
        Class<?> bothSides = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(bothSides))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, bothSides.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Class, M3Paths.Integer, M3Paths.Property, M3Paths.PureOne,
                        "test::model", "test::model::Left", "test::model::Right")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['leftCount'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['rightCount'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "test::model::Left",
                        "test::model::Left",
                        refUsage(path + ".generalizations[0].general", "rawType"),
                        specialization(path + ".generalizations[0]"))
                .withBackReferences(
                        "test::model::Right",
                        "test::model::Right",
                        refUsage(path + ".generalizations[1].general", "rawType"),
                        specialization(path + ".generalizations[1]"))
                .build();
        assertMetadata(expected, bothSides);
    }

    @Test
    public void testClassWithAnnotations()
    {
        String path = "test::model::ClassWithAnnotations";
        Class<?> classWithAnnotations = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithAnnotations))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithAnnotations.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Class, M3Paths.Date, M3Paths.Property, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroOne,
                        "test::model", "meta::pure::profiles::doc.p_stereotypes[value='deprecated']", "meta::pure::profiles::doc.p_tags[value='doc']", "meta::pure::profiles::doc.p_tags[value='todo']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['deprecated'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['alsoDeprecated'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['date'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::profiles::doc",
                        "meta::pure::profiles::doc.p_stereotypes[value='deprecated']",
                        modelElement(path),
                        modelElement(path + ".properties['deprecated']"),
                        modelElement(path + ".properties['alsoDeprecated']"))
                .withBackReferences(
                        "meta::pure::profiles::doc",
                        "meta::pure::profiles::doc.p_tags[value='doc']",
                        modelElement(path),
                        modelElement(path + ".properties['alsoDeprecated']"),
                        modelElement(path + ".properties['date']"))
                .withBackReferences(
                        "meta::pure::profiles::doc",
                        "meta::pure::profiles::doc.p_tags[value='todo']",
                        modelElement(path + ".properties['date']"))
                .build();
        assertMetadata(expected, classWithAnnotations);
    }

    @Test
    public void testClassWithTypeAndMultiplicityParameters()
    {
        String path = "test::model::ClassWithTypeAndMultParams";
        Class<?> classWithTypeMultParams = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithTypeMultParams))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithTypeMultParams.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Class, M3Paths.Property, M3Paths.PureOne, M3Paths.String, "test::model")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['propT'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['propV'].classifierGenericType", "rawType"))
                .build();
        assertMetadata(expected, classWithTypeMultParams);
    }

    @Test
    public void testClassWithQualifiedProperties()
    {
        String path = "test::model::ClassWithQualifiedProperties";
        Class<?> classWithQualifiedProps = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithQualifiedProps))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithQualifiedProps.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Boolean, M3Paths.Class, M3Paths.Integer, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.PureOne, M3Paths.QualifiedProperty, M3Paths.String, M3Paths.ZeroMany, M3Paths.ZeroOne,
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::not_Boolean_1__Boolean_1_",
                        "meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_",
                        "meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_",
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1__String_1__String_1_",
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        "test::model")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".properties['names'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['title'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::boolean::not_Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::not_Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_",
                        "meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_",
                        application(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_",
                        "meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_",
                        application(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        application(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0]"))
                .withBackReferences(
                        "meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1__String_1__String_1_",
                        "meta::pure::functions::string::joinStrings_String_MANY__String_1__String_1__String_1__String_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1]"))
                .withBackReferences(
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        application(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0]"))
                .build();
        assertMetadata(expected, classWithQualifiedProps);
    }

    @Test
    public void testClassWithMilestoning1()
    {
        String path = "test::model::ClassWithMilestoning1";
        Class<?> classWithMilestoning1 = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithMilestoning1))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithMilestoning1.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Boolean, M3Paths.Class, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.OneMany, M3Paths.Property, M3Paths.PureOne, M3Paths.QualifiedProperty, M3Paths.String, M3Paths.ZeroOne, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::milestoning::BusinessDateMilestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        "meta::pure::profiles::temporal.p_stereotypes[value='businesstemporal']",
                        "test::model",
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        "test::model::ClassWithMilestoning3.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass2'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass3'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['businessDate'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['milestoning'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass2AllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass3AllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::milestoning::BusinessDateMilestoning",
                        "meta::pure::milestoning::BusinessDateMilestoning",
                        refUsage(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", "rawType"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass2AllVersions']"),
                        modelElement(path + ".properties['toClass3AllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])']"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        modelElement(path + ".properties['businessDate']"),
                        modelElement(path + ".properties['milestoning']"))
                .withBackReferences(
                        "meta::pure::profiles::temporal",
                        "meta::pure::profiles::temporal.p_stereotypes[value='businesstemporal']",
                        modelElement(path))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2",
                        refUsage(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3",
                        refUsage(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, classWithMilestoning1);
    }

    @Test
    public void testClassWithMilestoning2()
    {
        String path = "test::model::ClassWithMilestoning2";
        Class<?> classWithMilestoning2 = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithMilestoning2))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithMilestoning2.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Boolean, M3Paths.Class, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.OneMany, M3Paths.PureOne, M3Paths.QualifiedProperty, M3Paths.String, M3Paths.ZeroOne, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::milestoning::ProcessingDateMilestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        "meta::pure::profiles::temporal.p_stereotypes[value='processingtemporal']",
                        "test::model",
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        "test::model::ClassWithMilestoning3.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass1'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass3'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['milestoning'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['processingDate'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass1AllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass3AllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::milestoning::ProcessingDateMilestoning",
                        "meta::pure::milestoning::ProcessingDateMilestoning",
                        refUsage(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", "rawType"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass1AllVersions']"),
                        modelElement(path + ".properties['toClass3AllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])']"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        modelElement(path + ".properties['milestoning']"),
                        modelElement(path + ".properties['processingDate']"))
                .withBackReferences(
                        "meta::pure::profiles::temporal",
                        "meta::pure::profiles::temporal.p_stereotypes[value='processingtemporal']",
                        modelElement(path))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1",
                        refUsage(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3",
                        refUsage(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, classWithMilestoning2);
    }

    @Test
    public void testClassWithMilestoning3()
    {
        String path = "test::model::ClassWithMilestoning3";
        Class<?> classWithMilestoning3 = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(classWithMilestoning3))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Class, classWithMilestoning3.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Any, M3Paths.Boolean, M3Paths.Class, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.PureOne, M3Paths.QualifiedProperty, M3Paths.String, M3Paths.ZeroOne, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::first_T_MANY__T_$0_1$_",
                        "meta::pure::milestoning::BiTemporalMilestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        "meta::pure::profiles::temporal.p_stereotypes[value='bitemporal']",
                        "test::model",
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass1'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass2'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['businessDate'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['milestoning'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['processingDate'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass1AllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass2AllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::first_T_MANY__T_$0_1$_",
                        "meta::pure::functions::collection::first_T_MANY__T_$0_1$_",
                        application(path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::milestoning::BiTemporalMilestoning",
                        "meta::pure::milestoning::BiTemporalMilestoning",
                        refUsage(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", "rawType"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass1AllVersions']"),
                        modelElement(path + ".properties['toClass2AllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1()']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2()']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])']"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningdateproperty']",
                        modelElement(path + ".properties['businessDate']"),
                        modelElement(path + ".properties['milestoning']"),
                        modelElement(path + ".properties['processingDate']"))
                .withBackReferences(
                        "meta::pure::profiles::temporal",
                        "meta::pure::profiles::temporal.p_stereotypes[value='bitemporal']",
                        modelElement(path))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1",
                        refUsage(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1()'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2",
                        refUsage(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2()'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, classWithMilestoning3);
    }

    @Test
    public void testAssociationWithMilestoning1()
    {
        String path = "test::model::AssociationWithMilestoning1";
        Association association = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(association))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Association, association.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Association, M3Paths.Boolean, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.QualifiedProperty, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "test::model",
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass1A'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass2A'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass1AAllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass2AAllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass1AAllVersions']"),
                        modelElement(path + ".properties['toClass2AAllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1A(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2A(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])']"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1",
                        propFromAssoc(path + ".properties['toClass2AAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass2A(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass1A'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1AAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass2AAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2",
                        propFromAssoc(path + ".properties['toClass1AAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass1A(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass2A'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1AAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".properties['toClass2AAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, association);
    }

    @Test
    public void testAssociationWithMilestoning2()
    {
        String path = "test::model::AssociationWithMilestoning2";
        Association association = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(association))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Association, association.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Association, M3Paths.Boolean, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.QualifiedProperty, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "test::model",
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        "test::model::ClassWithMilestoning3.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass1B'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass3B'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass1BAllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass3BAllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass1BAllVersions']"),
                        modelElement(path + ".properties['toClass3BAllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1B()']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1B(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3B(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])']"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1",
                        propFromAssoc(path + ".properties['toClass3BAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass3B(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass1B'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1BAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass3BAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning1",
                        "test::model::ClassWithMilestoning1.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3",
                        propFromAssoc(path + ".properties['toClass1BAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass1B()']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass1B(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass3B'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass1BAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".properties['toClass3BAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, association);
    }

    @Test
    public void testAssociationWithMilestoning3()
    {
        String path = "test::model::AssociationWithMilestoning3";
        Association association = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(association))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Association, association.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.AggregationKind + ".values['None']", M3Paths.Association, M3Paths.Boolean, M3Paths.Date, M3Paths.LambdaFunction, M3Paths.Property, M3Paths.QualifiedProperty, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroMany,
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        "test::model",
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        "test::model::ClassWithMilestoning3.properties['processingDate']")
                .withBackReferences(
                        M3Paths.Property,
                        M3Paths.Property,
                        refUsage(path + ".originalMilestonedProperties['toClass2C'].classifierGenericType", "rawType"),
                        refUsage(path + ".originalMilestonedProperties['toClass3C'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass2CAllVersions'].classifierGenericType", "rawType"),
                        refUsage(path + ".properties['toClass3CAllVersions'].classifierGenericType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        "meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_",
                        application(path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_",
                        application(path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::profiles::milestoning",
                        "meta::pure::profiles::milestoning.p_stereotypes[value='generatedmilestoningproperty']",
                        modelElement(path + ".properties['toClass2CAllVersions']"),
                        modelElement(path + ".properties['toClass3CAllVersions']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2C()']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2C(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3C(Date[1])']"),
                        modelElement(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])']"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2",
                        propFromAssoc(path + ".properties['toClass3CAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass3C(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass2C'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass2CAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass3CAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning2",
                        "test::model::ClassWithMilestoning2.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3",
                        propFromAssoc(path + ".properties['toClass2CAllVersions']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass2C()']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass2C(Date[1])']"),
                        qualPropFromAssoc(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])']"),
                        refUsage(path + ".originalMilestonedProperties['toClass3C'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".properties['toClass2CAllVersions'].classifierGenericType.typeArguments[0]", "rawType"),
                        refUsage(path + ".properties['toClass3CAllVersions'].classifierGenericType.typeArguments[1]", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"),
                        refUsage(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType", "rawType"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['businessDate']",
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "test::model::ClassWithMilestoning3",
                        "test::model::ClassWithMilestoning3.properties['processingDate']",
                        application(path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"),
                        application(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]"))
                .build();
        assertMetadata(expected, association);
    }

    @Test
    public void testNativeFunction()
    {
        String path = "meta::pure::functions::lang::compare_T_1__T_1__Integer_1_";
        NativeFunction<?> compare = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(compare))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.NativeFunction, compare.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.Integer, M3Paths.NativeFunction, M3Paths.PureOne,
                        "meta::pure::functions::lang",
                        "meta::pure::profiles::doc.p_tags[value='doc']",
                        "meta::pure::test::pct::PCT.p_stereotypes[value='function']",
                        "meta::pure::test::pct::PCT.p_tags[value='grammarDoc']")
                .withBackReferences(
                        "meta::pure::profiles::doc",
                        "meta::pure::profiles::doc.p_tags[value='doc']",
                        modelElement(path))
                .withBackReferences(
                        "meta::pure::test::pct::PCT",
                        "meta::pure::test::pct::PCT.p_stereotypes[value='function']",
                        modelElement(path))
                .withBackReferences(
                        "meta::pure::test::pct::PCT",
                        "meta::pure::test::pct::PCT.p_tags[value='grammarDoc']",
                        modelElement(path))
                .withFunctionByName("compare", path)
                .build();
        assertMetadata(expected, compare);
    }

    @Test
    public void testFunction()
    {
        String path = "test::model::testFunc_T_m__Function_$0_1$__String_m_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(testFunction))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.ConcreteFunctionDefinition, testFunction.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.Boolean, M3Paths.ConcreteFunctionDefinition, M3Paths.Function, M3Paths.LambdaFunction, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroOne,
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        "meta::pure::functions::collection::map_T_m__Function_1__V_m_",
                        "meta::pure::functions::lang::eval_Function_1__T_n__V_m_",
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::string::toString_Any_1__String_1_",
                        "test::model")
                .withBackReferences(
                        M3Paths.Function,
                        M3Paths.Function,
                        refUsage(path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType", "rawType"),
                        refUsage(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"))
                .withBackReferences(
                        M3Paths.LambdaFunction,
                        M3Paths.LambdaFunction,
                        refUsage(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"))
                .withBackReferences(
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        "meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_",
                        application(path + ".expressionSequence[0].parametersValues[1].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::collection::map_T_m__Function_1__V_m_",
                        "meta::pure::functions::collection::map_T_m__Function_1__V_m_",
                        application(path + ".expressionSequence[1]"))
                .withBackReferences(
                        "meta::pure::functions::lang::eval_Function_1__T_n__V_m_",
                        "meta::pure::functions::lang::eval_Function_1__T_n__V_m_",
                        application(path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_",
                        application(path + ".expressionSequence[0].parametersValues[1]"))
                .withBackReferences(
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        application(path + ".expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        application(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0]"))
                .withBackReferences(
                        "meta::pure::functions::string::toString_Any_1__String_1_",
                        "meta::pure::functions::string::toString_Any_1__String_1_",
                        application(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0]"))
                .withFunctionByName("testFunc", path)
                .build();
        assertMetadata(expected, testFunction);
    }

    @Test
    public void testFunction2()
    {
        String path = "test::model::testFunc2__String_1_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(testFunction))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.ConcreteFunctionDefinition, testFunction.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.ConcreteFunctionDefinition, M3Paths.Measure, M3Paths.Package, M3Paths.PureOne, M3Paths.String,
                        M3Paths.Type + ".properties['name']", M3Paths.Unit, M3Paths.Unit + ".properties['measure']", M3Paths.ZeroOne,
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::meta::elementToPath_PackageableElement_1__String_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        "test::model",
                        "test::model::Mass.nonCanonicalUnits['Pound']")
                .withBackReferences(
                        M3Paths.Type,
                        M3Paths.Type + ".properties['name']",
                        application(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0]"),
                        application(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0]"))
                .withBackReferences(
                        M3Paths.Unit,
                        M3Paths.Unit + ".properties['measure']",
                        application(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0]"))
                .withBackReferences(
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_",
                        application(path + ".expressionSequence[0]"),
                        application(path + ".expressionSequence[1]"))
                .withBackReferences(
                        "meta::pure::functions::meta::elementToPath_PackageableElement_1__String_1_",
                        "meta::pure::functions::meta::elementToPath_PackageableElement_1__String_1_",
                        application(path + ".expressionSequence[2].parametersValues[0].values[0]"))
                .withBackReferences(
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_",
                        application(path + ".expressionSequence[2].parametersValues[0].values[2]"),
                        application(path + ".expressionSequence[2].parametersValues[0].values[4]"))
                .withBackReferences(
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        "meta::pure::functions::string::plus_String_MANY__String_1_",
                        application(path + ".expressionSequence[2]"))
                .withBackReferences(
                        "test::model",
                        "test::model",
                        refUsage(path + ".expressionSequence[0].parametersValues[1]", "values"))
                .withBackReferences(
                        "test::model::Mass",
                        "test::model::Mass.nonCanonicalUnits['Pound']",
                        refUsage(path + ".expressionSequence[1].parametersValues[1]", "values"))
                .withFunctionByName("testFunc2", path)
                .build();
        assertMetadata(expected, testFunction);
    }

    @Test
    public void testFunction3()
    {
        String path = "test::model::testFunc3__Any_MANY_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(testFunction))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.ConcreteFunctionDefinition, testFunction.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.Any, M3Paths.ConcreteFunctionDefinition, M3Paths.LambdaFunction, M3Paths.Package, M3Paths.Package + ".properties['children']", M3Paths.PackageableElement, M3Paths.PureOne, M3Paths.String, M3Paths.ZeroMany, "test::model")
                .withBackReferences(
                        M3Paths.Any,
                        M3Paths.Any,
                        refUsage(path + ".classifierGenericType.typeArguments[0].rawType.returnType", "rawType"))
                .withBackReferences(
                        M3Paths.Package,
                        M3Paths.Package + ".properties['children']",
                        application(path + ".expressionSequence[0].values[4].expressionSequence[0]"))
                .withBackReferences(
                        M3Paths.PackageableElement,
                        M3Paths.PackageableElement,
                        refUsage(path + ".expressionSequence[0].values[4].classifierGenericType.typeArguments[0].rawType.returnType", "rawType"))
                .withBackReferences(
                        "test::model",
                        "test::model",
                        refUsage(path + ".expressionSequence[0].values[0]", "values"),
                        refUsage(path + ".expressionSequence[0].values[4].expressionSequence[0].parametersValues[0]", "values"))
                .withFunctionByName("testFunc3", path)
                .build();
        assertMetadata(expected, testFunction);
    }

    @Test
    public void testMeasureWithNonconvertibleUnits()
    {
        String path = "test::model::Currency";
        Measure currency = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(currency))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Measure, currency.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.DataType, M3Paths.Measure, M3Paths.Unit, "test::model")
                .withBackReferences(
                        M3Paths.DataType,
                        M3Paths.DataType,
                        specialization(path + ".generalizations[0]"))
                .build();
        assertMetadata(expected, currency);
    }

    @Test
    public void testMeasureWithConvertibleUnits()
    {
        String path = "test::model::Mass";
        Measure mass = getCoreInstance(path);
        ModuleMetadata expected = ModuleMetadata.builder(ModuleHelper.getElementModule(mass))
                .withReferenceIdVersion(referenceIds.getDefaultVersion())
                .withElement(new ConcreteElementMetadata(path, M3Paths.Measure, mass.getSourceInformation()))
                .withExternalReferences(
                        path,
                        M3Paths.DataType, M3Paths.Float, M3Paths.Integer, M3Paths.LambdaFunction, M3Paths.Measure, M3Paths.Number, M3Paths.PureOne, M3Paths.Unit,
                        "meta::pure::functions::math::times_Number_MANY__Number_1_", "test::model")
                .withBackReferences(
                        M3Paths.DataType,
                        M3Paths.DataType,
                        specialization(path + ".generalizations[0]"))
                .withBackReferences(
                        "meta::pure::functions::math::times_Number_MANY__Number_1_",
                        "meta::pure::functions::math::times_Number_MANY__Number_1_",
                        application(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0]"),
                        application(path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0]"))
                .build();
        assertMetadata(expected, mass);
    }

    private void assertMetadata(ModuleMetadata expected, CoreInstance concreteElement)
    {
        ModuleMetadata actual = generator.computeMetadata(
                ModuleMetadata.builder(ModuleHelper.getElementModule(concreteElement)).withReferenceIdVersion(referenceIds.getDefaultVersion()),
                concreteElement)
                .build();
        Assert.assertEquals(expected, actual);
    }

    private static BackReference.Application application(String funcExpr)
    {
        return BackReference.newApplication(funcExpr);
    }

    private static BackReference.ModelElement modelElement(String element)
    {
        return BackReference.newModelElement(element);
    }

    private static BackReference.PropertyFromAssociation propFromAssoc(String property)
    {
        return BackReference.newPropertyFromAssociation(property);
    }

    private static BackReference.QualifiedPropertyFromAssociation qualPropFromAssoc(String qualifiedProperty)
    {
        return BackReference.newQualifiedPropertyFromAssociation(qualifiedProperty);
    }

    private static BackReference.ReferenceUsage refUsage(String owner, String property)
    {
        return refUsage(owner, property, 0);
    }

    private static BackReference.ReferenceUsage refUsage(String owner, String property, int offset)
    {
        return BackReference.newReferenceUsage(owner, property, offset);
    }

    private static BackReference.Specialization specialization(String generalization)
    {
        return BackReference.newSpecialization(generalization);
    }
}
