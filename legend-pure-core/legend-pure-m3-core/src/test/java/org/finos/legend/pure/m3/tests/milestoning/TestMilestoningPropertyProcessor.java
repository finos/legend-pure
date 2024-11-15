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

package org.finos.legend.pure.m3.tests.milestoning;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class TestMilestoningPropertyProcessor extends AbstractTestMilestoning
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("sourceId3.pure");
        runtime.delete("domain.pure");
        runtime.delete("mainModel.pure");
        runtime.delete("otherModel.pure");

        runtime.compile();
    }

    @Test
    public void testSynthesizedPropertiesMultiplicities()
    {
        compileTestSourceM3("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Order\n" +
                        "{\n" +
                        "   productOptional : Product[0..1];\n" +
                        "   productOne : Product[1];\n" +
                        "   productOneMany : Product[1..*];\n" +
                        "   productMany : Product[*];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        Class<?> order = getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends Property<?, ?>> properties = getProperties(order);
        assertMilestoningRegularProperty(properties.get(2), "productOptionalAllVersions", "meta::test::domain::Product", "*");
        assertMilestoningRegularProperty(properties.get(3), "productOneAllVersions", "meta::test::domain::Product", "1..*");
        assertMilestoningRegularProperty(properties.get(4), "productOneManyAllVersions", "meta::test::domain::Product", "1..*");
        assertMilestoningRegularProperty(properties.get(5), "productManyAllVersions", "meta::test::domain::Product", "*");

        ListIterable<? extends QualifiedProperty<?>> qps = getQualifiedProperties(order);
        assertMilestoningQualifiedProperty(qps.get(0), "productOptional", "meta::test::domain::Product", "0..1", 0);
        assertMilestoningQualifiedProperty(qps.get(1), "productOptional", "meta::test::domain::Product", "0..1", 1);
        assertMilestoningQualifiedProperty(qps.get(2), "productOptionalAllVersionsInRange", "meta::test::domain::Product", "0..1", 2);
        assertMilestoningQualifiedProperty(qps.get(3), "productOne", "meta::test::domain::Product", "1", 0);
        assertMilestoningQualifiedProperty(qps.get(4), "productOne", "meta::test::domain::Product", "1", 1);
        assertMilestoningQualifiedProperty(qps.get(5), "productOneAllVersionsInRange", "meta::test::domain::Product", "1", 2);
        assertMilestoningQualifiedProperty(qps.get(6), "productOneMany", "meta::test::domain::Product", "1..*", 0);
        assertMilestoningQualifiedProperty(qps.get(7), "productOneMany", "meta::test::domain::Product", "1..*", 1);
        assertMilestoningQualifiedProperty(qps.get(8), "productOneManyAllVersionsInRange", "meta::test::domain::Product", "1..*", 2);
        assertMilestoningQualifiedProperty(qps.get(9), "productMany", "meta::test::domain::Product", "*", 0);
        assertMilestoningQualifiedProperty(qps.get(10), "productMany", "meta::test::domain::Product", "*", 1);
        assertMilestoningQualifiedProperty(qps.get(11), "productManyAllVersionsInRange", "meta::test::domain::Product", "*", 2);
    }

    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForBusinessTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> order = getCoreInstance("meta::test::domain::Order");
        Assert.assertEquals(Lists.mutable.with("orderId", "productAllVersions"), order._properties().collect(CoreInstance::getName));

        Assert.assertEquals(Lists.mutable.with("product"), order._originalMilestonedProperties().collect(CoreInstance::getName));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedProperties(order);
        Assert.assertEquals(Lists.fixedSize.with("product(Date[1])", "productAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));
        QualifiedProperty<?> productQP = qualifiedProperties.getFirst();
        Assert.assertEquals("product", productQP._functionName());
        assertGenericType("meta::test::domain::Product", productQP);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), productQP._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQP = qualifiedProperties.getLast();
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangeQP._functionName());
        assertGenericType("meta::test::domain::Product", allVersionsInRangeQP);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQP._stereotypes());
    }

    @Test
    public void testGeneratedMilestonedQualifiedPropertiesMultiplicity()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   productOptional : Product[0..1];\n" +
                        "   productTwo : Product[2];\n" +
                        "   productMany : Product[*];\n" +
                        "   productRange : Product[2..3];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> order = getCoreInstance("meta::test::domain::Order");
        assertMultiplicity("1", getQualifiedProperty(order, "product(Date[1])"));
        assertMultiplicity("0..1", getQualifiedProperty(order, "productOptional(Date[1])"));
        assertMultiplicity("*", getQualifiedProperty(order, "productTwo(Date[1])"));
        assertMultiplicity("*", getQualifiedProperty(order, "productMany(Date[1])"));
        assertMultiplicity("*", getQualifiedProperty(order, "productRange(Date[1])"));
    }

    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForProcessingTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        runtime.compile();
        Class<?> order = getCoreInstance("meta::test::domain::Order");

        Assert.assertEquals(Lists.fixedSize.with("orderId", "productAllVersions"), getProperties(order).collect(CoreInstance::getName));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedProperties(order);
        Assert.assertEquals(Lists.fixedSize.with("product(Date[1])", "productAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));

        QualifiedProperty<?> productQP = qualifiedProperties.get(0);
        Assert.assertEquals("product", productQP._functionName());
        assertGenericType("meta::test::domain::Product", productQP);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), productQP._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQP = qualifiedProperties.get(1);
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangeQP._functionName());
        assertGenericType("meta::test::domain::Product", allVersionsInRangeQP);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQP._stereotypes());
    }

    @Test
    public void testGeneratedPropertiesIncludeSourcePropertyTaggedValuesAndStereotypes()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   <<service.disableStreaming>> {service.contentType='HTTP'} product : Product[1];\n" +
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> order = getCoreInstance("meta::test::domain::Order");
        Assert.assertEquals(Lists.fixedSize.with("productAllVersions"), order._properties().collect(Property::_name));
        Property<?, ?> productProperty = getProperties(order).getOnly();

        CoreInstance servicePackage = processorSupport.package_getByUserPath(M3Paths.service);
        CoreInstance disableStreamingStereotype = servicePackage.getValueForMetaPropertyToMany(M3Properties.p_stereotypes).detect(serviceStereotype -> "disableStreaming".equals(serviceStereotype.getName()));

        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype(), disableStreamingStereotype), productProperty._stereotypes());

        RichIterable<? extends TaggedValue> taggedValues = productProperty._taggedValues();
        Assert.assertEquals(1, taggedValues.size());
        Assert.assertEquals("HTTP", taggedValues.getOnly()._value());
    }

    @Test
    public void testGeneratedPropertiesIncludeAggregationProperty()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   <<service.disableStreaming>> {service.contentType='HTTP'} (composite) orderDetailsComposite : OrderDetails[1];\n" +
                        "   (composite) orderDetailsNonTemporalComposite : OrderDetailsNonTemporal[1];\n" +
                        "   (shared) orderDetailsShared : OrderDetails[1];\n" +
                        "   (shared) orderDetailsNonTemporalShared : OrderDetailsNonTemporal[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::OrderDetails\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class meta::test::domain::OrderDetailsNonTemporal\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association  meta::test::domain::Order_OrderDetails\n" +
                        "{\n" +
                        "   order : Order[1];\n" +
                        "   (composite) orderDetailsViaAssnComposite : OrderDetails[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> order = getCoreInstance("meta::test::domain::Order");

        Property<?, ?> orderDetailsNonTemporalCompositeProperty = getProperty(order, "orderDetailsNonTemporalComposite");
        CoreInstance compositeAggregation = orderDetailsNonTemporalCompositeProperty._aggregation();
        Property<?, ?> orderDetailsNonTemporalSharedProperty = getProperty(order, "orderDetailsNonTemporalShared");
        CoreInstance sharedAggregation = orderDetailsNonTemporalSharedProperty._aggregation();

        Property<?, ?> orderDetailsTemporalCompositeProperty = getProperty(order, "orderDetailsCompositeAllVersions");
        Property<?, ?> orderDetailsTemporalSharedProperty = getProperty(order, "orderDetailsSharedAllVersions");

        Assert.assertEquals(compositeAggregation, orderDetailsTemporalCompositeProperty._aggregation());
        Assert.assertEquals(sharedAggregation, orderDetailsTemporalSharedProperty._aggregation());

        Association orderDetailsAssn = getCoreInstance("meta::test::domain::Order_OrderDetails");
        Property<?, ?> orderDetailsViaAssnCompositeAllVersions = getProperty(orderDetailsAssn, "orderDetailsViaAssnCompositeAllVersions");
        Assert.assertEquals(compositeAggregation, orderDetailsViaAssnCompositeAllVersions._aggregation());
    }


    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForBusinessTemporalAssociations()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::relational::tests::milestoning::OrderProduct\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "   orders : Order[*];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> orderClass = getCoreInstance("meta::test::domain::Order");

        Assert.assertEquals(Lists.mutable.with("productAllVersions"), getPropertiesFromAssociations(orderClass).collect(CoreInstance::getName));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedPropertiesFromAssociations(orderClass);
        Assert.assertEquals(Lists.fixedSize.with("product(Date[1])", "productAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));

        QualifiedProperty<?> qualifiedProperty = qualifiedProperties.getFirst();
        Assert.assertEquals("product", qualifiedProperty._functionName());
        assertGenericType("meta::test::domain::Product", qualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), qualifiedProperty._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangeQualifiedProperty._functionName());
        assertGenericType("meta::test::domain::Product", allVersionsInRangeQualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQualifiedProperty._stereotypes());
    }

    @Test
    public void testQualifiedPropertyVisibility()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let p = ^Product(name='test', businessDate=%2018-12-18T13:35:21);\n" +
                        "   let o = ^Order(orderId=1, productAllVersions=$p);\n" +
                        "   $o.product(%2018-12-18T13:35:21);\n" +
                        "   $o.productAllVersionsInRange(%2018-12-1, %2018-12-31);\n" +
                        "}");
        runtime.compile();
    }

    @Test
    public void testQualifiedPropertiesFromAssociationsInSeparateSource()
    {
        compileTestSource("mainModel.pure",
                "Class test::MainClass\n" +
                        "{\n" +
                        "}");
        compileTestSource("otherModel.pure",
                "Class <<temporal.businesstemporal>> test::OtherClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::OtherMainAssociation\n" +
                        "{\n" +
                        "   otherToMain : test::MainClass[*];\n" +
                        "   mainToOther : test::OtherClass[*];\n" +
                        "}");

        Class<?> mainClass = getCoreInstance("test::MainClass");

        Assert.assertEquals(Lists.mutable.with("mainToOtherAllVersions"), getPropertiesFromAssociations(mainClass).collect(Property::_name));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedPropertiesFromAssociations(mainClass);
        Assert.assertEquals(Lists.fixedSize.with("mainToOther(Date[1])", "mainToOtherAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));

        QualifiedProperty<?> qualifiedProperty = qualifiedProperties.getFirst();
        Assert.assertEquals("mainToOther", qualifiedProperty._functionName());
        assertGenericType("test::OtherClass", qualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), qualifiedProperty._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangeQualifiedProperty._functionName());
        assertGenericType("test::OtherClass", allVersionsInRangeQualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQualifiedProperty._stereotypes());
    }


    @Test
    public void testQualifiedPropertiesFromAssociationsInSameSource()
    {
        compileTestSource("mainModel.pure",
                "Class test::MainClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> test::OtherClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::OtherMainAssociation\n" +
                        "{\n" +
                        "   otherToMain : test::MainClass[*];\n" +
                        "   mainToOther : test::OtherClass[*];\n" +
                        "}");

        Class<?> mainClass = getCoreInstance("test::MainClass");

        Assert.assertEquals(Lists.mutable.with("mainToOtherAllVersions"), getPropertiesFromAssociations(mainClass).collect(Property::_name));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedPropertiesFromAssociations(mainClass);
        Assert.assertEquals(Lists.fixedSize.with("mainToOther(Date[1])", "mainToOtherAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));
        QualifiedProperty<?> qualifiedProperty = qualifiedProperties.getFirst();
        Assert.assertEquals("mainToOther", qualifiedProperty._functionName());
        assertGenericType("test::OtherClass", qualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), qualifiedProperty._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangeQualifiedProperty._functionName());
        assertGenericType("test::OtherClass", allVersionsInRangeQualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQualifiedProperty._stereotypes());
    }

    @Test
    public void testQualifiedPropertiesFromAssociationsWithPackageVisibilityRestrictions()
    {
        String source1Id = "testMainModel.pure";
        String source1Code = "Class model::domain::test::MainClass\n" +
                "{\n" +
                "}";
        String source2Id = "testOtherModel.pure";
        String source2Code = "Class <<temporal.businesstemporal>> model::domain::test::OtherClass\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association model::domain::test::OtherMainAssociation\n" +
                "{\n" +
                "   otherToMain : model::domain::test::MainClass[*];\n" +
                "   mainToOther : model::domain::test::OtherClass[*];\n" +
                "}";

        runtime.createInMemorySource(source1Id, source1Code);
        runtime.createInMemorySource(source2Id, source2Code);
        runtime.compile();

        runtime.delete(source1Id);

        runtime.createInMemorySource(source1Id, source1Code);
        runtime.compile();

        Class<?> mainClass = getCoreInstance("model::domain::test::MainClass");

        Assert.assertEquals(Lists.mutable.with("mainToOtherAllVersions"), getPropertiesFromAssociations(mainClass).collect(Property::_name));

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = getQualifiedPropertiesFromAssociations(mainClass);
        Assert.assertEquals(Lists.fixedSize.with("mainToOther(Date[1])", "mainToOtherAllVersionsInRange(Date[1],Date[1])"), qualifiedProperties.collect(QualifiedProperty::_id));
        QualifiedProperty<?> qualifiedProperty = qualifiedProperties.getFirst();
        Assert.assertEquals("mainToOther", qualifiedProperty._functionName());
        assertGenericType("model::domain::test::OtherClass", qualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), qualifiedProperty._stereotypes());

        QualifiedProperty<?> allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangeQualifiedProperty._functionName());
        assertGenericType("model::domain::test::OtherClass", allVersionsInRangeQualifiedProperty);
        Assert.assertEquals(Lists.fixedSize.with(getGeneratedMilestoningStereotype()), allVersionsInRangeQualifiedProperty._stereotypes());
    }

    @Test
    public void testRemovalOfOriginalMilestonedPropertyAndMilestonedPropertyGeneration()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "Class model::domain::trading::Order\n" +
                        "{\n" +
                        "   account:model::domain::subdom1::account::Account[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let o = ^model::domain::trading::Order();\n" +
                        "   $o.accountAllVersions;\n" +
                        "   $o.account(%2016);\n" +
                        "   $o.account;\n" +
                        "}"
        );
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:15 column:7), \"The property 'account' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]\"", e.getMessage());
    }

    @Test
    public void testRemovalOfOriginalMilestonedPropertyAndGenerationOfAllPropertyViaAssociation()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "Class model::domain::trading::Order\n" +
                        "{\n" +
                        "}\n" +
                        "Association OrderAccount\n" +
                        "{\n" +
                        "   account:model::domain::subdom1::account::Account[0..1];\n" +
                        "   order:model::domain::trading::Order[0..1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let o = ^model::domain::trading::Order();\n" +
                        "   $o.accountAllVersions;\n" +
                        "   $o.account(%2016);\n" +
                        "   $o.account;\n" +
                        "}"
        );
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:18 column:7), \"The property 'account' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]\"", e.getMessage());
    }

    @Test
    public void testNoArgPropertyNotGeneratedWhenSourceClassIsNotTemporal()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;\n" +
                        "import model::domain::subdom1::product::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::entity::LegalEntity\n" +
                        "{\n" +
                        "}\n" +
                        "Class model::domain::subdom1::product::Product\n" +
                        "{\n" +
                        "  legalEntity : LegalEntity[*];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let p = ^model::domain::subdom1::product::Product();\n" +
                        "   $p.legalEntity;\n" +
                        "}"
        );
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:13 column:7), \"The property 'legalEntity' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]\"", e.getMessage());
    }

    @Test
    public void testGenerationOfNoArgQualifiedPropertyFromAssociationWhereTargetClassInheritsFromClassWithBusinessTemporalStereotype()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>>  model::domain::trading::Order\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association OrderAccount\n" +
                        "{\n" +
                        "   account:model::domain::subdom1::account::FirmAccount[0..1];\n" +
                        "   order:model::domain::trading::Order[0..1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|model::domain::trading::Order.all(%2016)->filter(o|$o.account->isEmpty())};\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testGenerationOfNoArgQualifiedPropertyFromAssociationWhereTargetClassInheritsFromClassWithBusinessTemporalStereotypeWithMultipleSources()
    {
        runtime.createInMemorySource("sourceId.pure",
                "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> model::domain::trading::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "   orderIdQp(){$this.orderId} : Integer[1];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "Association OrderAccount\n" +
                        "{\n" +
                        "   account : FirmAccount[0..1];\n" +
                        "   order:model::domain::trading::Order[0..1];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId3.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|model::domain::trading::Order.all(%2016)->filter(o|$o.account->isEmpty())};\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testPropertiesInterceptedAndTransformedToQualifiedPropertiesWhereSourceAndTargetAreBusinessTemporal()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;\n" +
                        "import model::domain::subdom1::product::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::entity::LegalEntity\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::product::Product\n" +
                        "{\n" +
                        "   legalEntity : LegalEntity[*];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|model::domain::subdom1::product::Product.all(%2016)->filter(o|$o.legalEntity->isEmpty())};\n" +
                        "   {|model::domain::subdom1::product::Product.all(%2016)->filter(o|$o.legalEntity(%2015)->isEmpty())};\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testValidationOfOverridenProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;\n" +
                        "import model::domain::subdom1::product::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::VehicleOwner\n" +
                        "{\n" +
                        "   vehicles : meta::relational::tests::milestoning::inheritance::Vehicle[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::Person extends meta::relational::tests::milestoning::inheritance::VehicleOwner\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::Vehicle\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   description: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  print('go',1); \n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testPropertyOwnerSetOnMilestonedProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::Order\n" +
                        "{\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::relational::tests::milestoning::OrderProduct\n" +
                        "{\n" +
                        "   assocProduct : Product[1];\n" +
                        "   orders : Order[*];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;\n" +
                        "Association meta::relational::tests::milestoning::OrderProduct2\n" +
                        "{\n" +
                        "   assocProduct2 : Product[1];\n" +
                        "   orders2 : Order[*];\n" +
                        "}"
        );
        runtime.compile();

        //test Association
        Association assn = getCoreInstance("meta::relational::tests::milestoning::OrderProduct");
        Assert.assertEquals(Lists.fixedSize.with("orders", "assocProductAllVersions"), getProperties(assn).collect(Property::_name));

        //test Order
        Class<?> orderClass = getCoreInstance("meta::test::domain::Order");
        Assert.assertEquals(Lists.fixedSize.with("productAllVersions"), getProperties(orderClass).collect(Property::_name));
        Assert.assertEquals(Lists.fixedSize.with("assocProductAllVersions", "assocProduct2AllVersions"), getPropertiesFromAssociations(orderClass).collect(Property::_name));
    }

    @Test
    public void testMilestoningPropertyGenerationFromInheritanceHierarchySource()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B extends A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::C\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association AC\n" +
                        "{\n" +
                        "   c : C[1];\n" +
                        "   a : A[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association BC\n" +
                        "{\n" +
                        "   c : C[1];\n" +
                        "   b : B[1];\n" +
                        "}"
        );
        runtime.compile();

    }

    @Test
    public void testQualifiedPropertyGenerationForAssociations()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::AC\n" +
                        "{\n" +
                        "   b : B[1];\n" +
                        "   a : A[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> orderClass = getCoreInstance("meta::test::domain::A");

        Assert.assertEquals(Lists.fixedSize.with("bAllVersions"), getPropertiesFromAssociations(orderClass).collect(Property::_name));

        Assert.assertEquals(
                Lists.fixedSize.with("b()", "b(Date[1])", "bAllVersionsInRange(Date[1],Date[1])"),
                getQualifiedPropertiesFromAssociations(orderClass).collect(QualifiedProperty::_id));

        Association assn = getCoreInstance("meta::test::domain::AC");
        Assert.assertEquals(Lists.fixedSize.with("bAllVersions", "aAllVersions"), getProperties(assn).collect(Property::_name));
        Assert.assertEquals(
                Lists.fixedSize.with("b()", "b(Date[1])", "bAllVersionsInRange(Date[1],Date[1])",
                        "a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])"),
                getQualifiedProperties(assn).collect(QualifiedProperty::_id));
    }

    @Test
    public void testQualifiedPropertyGenerationForAssociationsInDifferentSources()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B\n" +
                        "{\n" +
                        "}");

        runtime.compile();

        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;\n" +
                        "Association meta::test::domain::AC\n" +
                        "{\n" +
                        "   b : B[*];\n" +
                        "   a : A[*];\n" +
                        "}"
        );
        //Classes are not part of the set of instances in scope for post processing this time
        //Rely on returning/processing the generated QualifiedProperties from AssociationProcessor
        runtime.compile();

        Class<?> aClass = getCoreInstance("meta::test::domain::A");
        Class<?> bClass = getCoreInstance("meta::test::domain::B");
        Association acAssn = getCoreInstance("meta::test::domain::AC");

        //validate edge point properties
        ListIterable<? extends Property<?, ?>> aProperties = getPropertiesFromAssociations(aClass);
        Assert.assertEquals(Lists.fixedSize.with("bAllVersions"), aProperties.collect(Property::_name));
        Assert.assertSame(acAssn, aProperties.getFirst()._owner());

        ListIterable<? extends Property<?, ?>> bProperties = getPropertiesFromAssociations(bClass);
        Assert.assertEquals(Lists.fixedSize.with("aAllVersions"), bProperties.collect(Property::_name));
        Assert.assertSame(acAssn, bProperties.getFirst()._owner());

        ListIterable<? extends QualifiedProperty<?>> aQPs = getQualifiedPropertiesFromAssociations(aClass);
        Assert.assertEquals(Lists.fixedSize.with("b()", "b(Date[1])", "bAllVersionsInRange(Date[1],Date[1])"), aQPs.collect(QualifiedProperty::_id));
        Assert.assertEquals(
                Lists.fixedSize.with("filter_T_MANY__Function_1__T_MANY_", "filter_T_MANY__Function_1__T_MANY_", "filter_T_MANY__Function_1__T_MANY_"),
                aQPs.collect(qp -> ((SimpleFunctionExpression) qp._expressionSequence().getOnly())._func().getName()));

        Association assn = getCoreInstance("meta::test::domain::AC");
        Assert.assertEquals(Lists.fixedSize.with("bAllVersions", "aAllVersions"), getProperties(assn).collect(Property::_name));
        Assert.assertEquals(
                Lists.fixedSize.with("b()", "b(Date[1])", "bAllVersionsInRange(Date[1],Date[1])",
                        "a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])"),
                getQualifiedProperties(assn).collect(QualifiedProperty::_id));
    }

    @Test
    public void testEdgePointPropertyOwnerIsNotOverriddenWhenMultiplePropertiesAreProcessedViaAssociation()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class meta::test::domain::B\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::AB\n" +
                        "{\n" +
                        "   a : A[*];\n" +
                        "   b1 : B[*];\n" +
                        "}");

        runtime.compile();

        Association assnAB = getCoreInstance("meta::test::domain::AB");
        Class<?> b = getCoreInstance("meta::test::domain::B");

        Assert.assertEquals(Lists.fixedSize.with("aAllVersions"), getPropertiesFromAssociations(b).collect(Property::_name));
        Assert.assertSame(assnAB, getPropertiesFromAssociations(b).getOnly()._owner());

        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::C\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::AC\n" +
                        "{\n" +
                        "   b2 : B[*];\n" +
                        "   c : C[*];\n" +
                        "}"
        );
        runtime.compile();

        Association assnAC = getCoreInstance("meta::test::domain::AC");

        Assert.assertEquals(Lists.fixedSize.with("aAllVersions", "cAllVersions"), getPropertiesFromAssociations(b).collect(Property::_name));
        Assert.assertEquals(assnAB, getPropertiesFromAssociations(b).get(0)._owner());
        Assert.assertEquals(assnAC, getPropertiesFromAssociations(b).get(1)._owner());
    }

    @Test
    public void testSourceInformationOfGeneratedMilestonedProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::A\n" +
                        "{\n" +
                        "   b : B[*];\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::AB\n" +
                        "{\n" +
                        "   a2:A[0..1];\n" +
                        "   b2:B[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B\n" +
                        "{\n" +
                        "}");
        runtime.compile();

        Class<?> aClass = getCoreInstance("meta::test::domain::A");
        Association abAssn = getCoreInstance("meta::test::domain::AB");

        Property<?, ?> originalB = getOriginalMilestonedProperty(aClass, "b");
        Assert.assertEquals(originalB.getSourceInformation(), getProperty(aClass, "bAllVersions").getSourceInformation());
        Assert.assertEquals(originalB.getSourceInformation(), getQualifiedProperty(aClass, "b(Date[1])").getSourceInformation());
        Assert.assertEquals(originalB.getSourceInformation(), getQualifiedProperty(aClass, "bAllVersionsInRange(Date[1],Date[1])").getSourceInformation());

        Property<?, ?> originalB2 = getOriginalMilestonedProperty(abAssn, "b2");
        Assert.assertEquals(originalB2.getSourceInformation(), getProperty(abAssn, "b2AllVersions").getSourceInformation());
        Assert.assertEquals(originalB2.getSourceInformation(), getPropertyFromAssociation(aClass, "b2AllVersions").getSourceInformation());
        Assert.assertEquals(originalB2.getSourceInformation(), getQualifiedProperty(abAssn, "b2(Date[1])").getSourceInformation());
        Assert.assertEquals(originalB2.getSourceInformation(), getQualifiedPropertyFromAssociation(aClass, "b2(Date[1])").getSourceInformation());
        Assert.assertEquals(originalB2.getSourceInformation(), getQualifiedProperty(abAssn, "b2AllVersionsInRange(Date[1],Date[1])").getSourceInformation());
        Assert.assertEquals(originalB2.getSourceInformation(), getQualifiedPropertyFromAssociation(aClass, "b2AllVersionsInRange(Date[1],Date[1])").getSourceInformation());
    }

    @Test
    public void testAssociationPropertiesWithSamePropertyNameProcessTypeArgumentsCorrectly()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::AB\n" +
                        "{\n" +
                        "   a : A[*];\n" +
                        "   a : B[*];\n" +
                        "}");

        runtime.compile();

        Class<?> a = getCoreInstance("meta::test::domain::A");
        Class<?> b = getCoreInstance("meta::test::domain::B");
        Association ab = getCoreInstance("meta::test::domain::AB");

        // properties on A
        ListIterable<? extends Property<?, ?>> aProps = getProperties(a);
        Assert.assertEquals(Lists.fixedSize.with("businessDate", "milestoning"), aProps.collect(Property::_name));
        assertGenericType(M3Paths.Date, aProps.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::A, Date|1>", aProps.get(0)._classifierGenericType());
        assertGenericType("meta::pure::milestoning::BusinessDateMilestoning", aProps.get(1));
        assertGenericType(M3Paths.Property + "<meta::test::domain::A, meta::pure::milestoning::BusinessDateMilestoning|0..1>", aProps.get(1)._classifierGenericType());

        ListIterable<? extends Property<?, ?>> aPropsFromAssocs = getPropertiesFromAssociations(a);
        Assert.assertEquals(Lists.fixedSize.with("aAllVersions"), aPropsFromAssocs.collect(Property::_name));
        assertGenericType("meta::test::domain::B", aPropsFromAssocs.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::A, meta::test::domain::B|*>", aPropsFromAssocs.get(0)._classifierGenericType());

        ListIterable<? extends QualifiedProperty<?>> aQProps = getQualifiedProperties(a);
        Assert.assertEquals(Lists.fixedSize.empty(), aQProps);

        ListIterable<? extends QualifiedProperty<?>> aQPropsFromAssocs = getQualifiedPropertiesFromAssociations(a);
        Assert.assertEquals(Lists.fixedSize.with("a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])"), aQPropsFromAssocs.collect(QualifiedProperty::_id));
        assertGenericType("meta::test::domain::B", aQPropsFromAssocs.get(0));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1]->meta::test::domain::B[*]}>", aQPropsFromAssocs.get(0)._classifierGenericType());
        assertGenericType("meta::test::domain::B", aQPropsFromAssocs.get(1));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1], Date[1]->meta::test::domain::B[*]}>", aQPropsFromAssocs.get(1)._classifierGenericType());
        assertGenericType("meta::test::domain::B", aQPropsFromAssocs.get(2));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1], Date[1], Date[1]->meta::test::domain::B[*]}>", aQPropsFromAssocs.get(2)._classifierGenericType());

        // properties on B
        ListIterable<? extends Property<?, ?>> bProps = getProperties(b);
        Assert.assertEquals(Lists.fixedSize.with("businessDate", "milestoning"), bProps.collect(Property::_name));
        assertGenericType(M3Paths.Date, bProps.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::B, Date|1>", bProps.get(0)._classifierGenericType());
        assertGenericType("meta::pure::milestoning::BusinessDateMilestoning", bProps.get(1));
        assertGenericType(M3Paths.Property + "<meta::test::domain::B, meta::pure::milestoning::BusinessDateMilestoning|0..1>", bProps.get(1)._classifierGenericType());

        ListIterable<? extends Property<?, ?>> bPropsFromAssocs = getPropertiesFromAssociations(b);
        Assert.assertEquals(Lists.fixedSize.with("aAllVersions"), bPropsFromAssocs.collect(Property::_name));
        assertGenericType("meta::test::domain::A", bPropsFromAssocs.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::B, meta::test::domain::A|*>", bPropsFromAssocs.get(0)._classifierGenericType());

        ListIterable<? extends QualifiedProperty<?>> bQProps = getQualifiedProperties(b);
        Assert.assertEquals(Lists.fixedSize.empty(), bQProps);

        ListIterable<? extends QualifiedProperty<?>> bQPropsFromAssocs = getQualifiedPropertiesFromAssociations(b);
        Assert.assertEquals(Lists.fixedSize.with("a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])"), bQPropsFromAssocs.collect(QualifiedProperty::_id));
        assertGenericType("meta::test::domain::A", bQPropsFromAssocs.get(0));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1]->meta::test::domain::A[*]}>", bQPropsFromAssocs.get(0)._classifierGenericType());
        assertGenericType("meta::test::domain::A", bQPropsFromAssocs.get(1));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1], Date[1]->meta::test::domain::A[*]}>", bQPropsFromAssocs.get(1)._classifierGenericType());
        assertGenericType("meta::test::domain::A", bQPropsFromAssocs.get(2));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1], Date[1], Date[1]->meta::test::domain::A[*]}>", bQPropsFromAssocs.get(2)._classifierGenericType());

        // properties on AB
        ListIterable<? extends Property<?, ?>> abOriginalProps = getOriginalMilestonedProperties(ab);
        Assert.assertEquals(Lists.fixedSize.with("a", "a"), abOriginalProps.collect(Property::_name));
        assertGenericType("meta::test::domain::A", abOriginalProps.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::B, meta::test::domain::A|*>", abOriginalProps.get(0)._classifierGenericType());
        assertGenericType("meta::test::domain::B", abOriginalProps.get(1));
        assertGenericType(M3Paths.Property + "<meta::test::domain::A, meta::test::domain::B|*>", abOriginalProps.get(1)._classifierGenericType());
        abOriginalProps.forEach(p -> Assert.assertSame(p._name(), ab, p._owner()));
        abOriginalProps.forEach(p -> Assert.assertTrue(p._name(), p.hasCompileState(CompileState.PROCESSED)));

        ListIterable<? extends Property<?, ?>> abProps = getProperties(ab);
        Assert.assertEquals(Lists.fixedSize.with("aAllVersions", "aAllVersions"), abProps.collect(Property::_name));
        assertGenericType("meta::test::domain::A", abProps.get(0));
        assertGenericType(M3Paths.Property + "<meta::test::domain::B, meta::test::domain::A|*>", abProps.get(0)._classifierGenericType());
        assertGenericType("meta::test::domain::B", abProps.get(1));
        assertGenericType(M3Paths.Property + "<meta::test::domain::A, meta::test::domain::B|*>", abProps.get(1)._classifierGenericType());
        getProperties(ab).forEach(p -> Assert.assertSame(p._name(), ab, p._owner()));
        getProperties(ab).forEach(p -> Assert.assertTrue(p._name(), p.hasCompileState(CompileState.PROCESSED)));

        ListIterable<? extends QualifiedProperty<?>> abQProps = getQualifiedProperties(ab);
        Assert.assertEquals(
                Lists.fixedSize.with("a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])",
                        "a()", "a(Date[1])", "aAllVersionsInRange(Date[1],Date[1])"),
                abQProps.collect(QualifiedProperty::_id));
        assertGenericType("meta::test::domain::A", abQProps.get(0));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1]->meta::test::domain::A[*]}>", abQProps.get(0)._classifierGenericType());
        assertGenericType("meta::test::domain::A", abQProps.get(1));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1], Date[1]->meta::test::domain::A[*]}>", abQProps.get(1)._classifierGenericType());
        assertGenericType("meta::test::domain::A", abQProps.get(2));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::B[1], Date[1], Date[1]->meta::test::domain::A[*]}>",abQProps.get(2)._classifierGenericType());
        assertGenericType("meta::test::domain::B", abQProps.get(3));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1]->meta::test::domain::B[*]}>", abQProps.get(3)._classifierGenericType());
        assertGenericType("meta::test::domain::B", abQProps.get(4));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1], Date[1]->meta::test::domain::B[*]}>", abQProps.get(4)._classifierGenericType());
        assertGenericType("meta::test::domain::B", abQProps.get(5));
        assertGenericType(M3Paths.QualifiedProperty + "<{meta::test::domain::A[1], Date[1], Date[1]->meta::test::domain::B[*]}>",abQProps.get(5)._classifierGenericType());
        getQualifiedProperties(ab).forEach(p -> Assert.assertSame(p._name(), ab, p._owner()));
        getQualifiedProperties(ab).forEach(p -> Assert.assertTrue(p._name(), p.hasCompileState(CompileState.PROCESSED)));
    }

    @Test
    public void testPropertyConflictsExistingPropertiesVsGeneratedProperties()
    {
        runtime.createInMemorySource("domain.pure",
                "import meta::relational::tests::milestoning::*;\n" +
                        "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Order\n" +
                        "{\n" +
                        "   other(s:String[1])\n" +
                        "   {\n" +
                        "      'other'\n" +
                        "   }:String[1];\n" +
                        "   other2(s:String[1])\n" +
                        "   {\n" +
                        "      'other'\n" +
                        "   }:String[1];\n" +
                        "   createdLocation : Location[*];\n" +
                        "   createdLocation(processingDate:Date[1], businessDate:Date[1], s:String[1])\n" +
                        "   {\n" +
                        "      $this.createdLocation(%latest, %latest)\n" +
                        "   }:Location[*];\n" +
                        "}\n");
        runtime.createInMemorySource("domain2.pure",
                "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location\n" +
                        "{\n" +
                        "   place : String[1];\n" +
                        "}");
        runtime.compile();

        Class<?> order = getCoreInstance("meta::relational::tests::milestoning::Order");
        Assert.assertEquals(
                Lists.fixedSize.with("other(String[1])", "other2(String[1])", "createdLocation(Date[1],Date[1],String[1])",
                        "createdLocation(Date[1],Date[1])", "createdLocation(Date[1])", "createdLocation()"),
                getQualifiedProperties(order).collect(QualifiedProperty::_id));
    }

    @Test
    public void testBiTemporalPropertyGeneration()
    {
        runtime.createInMemorySource("domain.pure",
                "import meta::relational::tests::milestoning::*;\n" +
                        "Class meta::relational::tests::milestoning::Order\n" +
                        "{\n" +
                        "   createdLocation : Location[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Exchange\n" +
                        "{\n" +
                        "   basedIn : Location[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::LegalEntity\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::LegalEntityPt\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location\n" +
                        "{\n" +
                        "   place : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association LegalEntity_Location\n" +
                        "{\n" +
                        "   registeredIn : Location[0..1];\n" +
                        "   legalEntity: LegalEntity[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Association LegalEntityPt_Location\n" +
                        "{\n" +
                        "   registeredInPt : Location[0..1];\n" +
                        "   legalEntityPt: LegalEntityPt[0..1];\n" +
                        "}");
        runtime.compile();

        Class<?> order = getCoreInstance("meta::relational::tests::milestoning::Order");
        ListIterable<? extends QualifiedProperty<?>> orderQps = getQualifiedProperties(order);

        Assert.assertEquals(Lists.fixedSize.with("createdLocation(Date[1],Date[1])"), orderQps.collect(QualifiedProperty::_id));
        assertMilestoningQualifiedProperty(orderQps.get(0), "createdLocation", "meta::relational::tests::milestoning::Location", "0..1", 2);

        Class<?> exchange = getCoreInstance("meta::relational::tests::milestoning::Exchange");
        ListIterable<? extends QualifiedProperty<?>> exchangeQps = getQualifiedProperties(exchange);
        Assert.assertEquals(Lists.fixedSize.with("basedIn(Date[1],Date[1])", "basedIn(Date[1])", "basedIn()"), exchangeQps.collect(QualifiedProperty::_id));
        assertMilestoningQualifiedProperty(exchangeQps.get(0), "basedIn", "meta::relational::tests::milestoning::Location", "0..1", 2);
        assertMilestoningQualifiedProperty(exchangeQps.get(1), "basedIn", "meta::relational::tests::milestoning::Location", "0..1", 1);
        assertMilestoningQualifiedProperty(exchangeQps.get(2), "basedIn", "meta::relational::tests::milestoning::Location", "0..1", 0);

        Class<?> legalEntity = getCoreInstance("meta::relational::tests::milestoning::LegalEntity");
        ListIterable<? extends QualifiedProperty<?>> legalEntityQps = getQualifiedPropertiesFromAssociations(legalEntity);
        Assert.assertEquals(Lists.fixedSize.with("registeredIn(Date[1],Date[1])", "registeredIn(Date[1])"), legalEntityQps.collect(QualifiedProperty::_id));
        assertMilestoningQualifiedProperty(legalEntityQps.get(0), "registeredIn", "meta::relational::tests::milestoning::Location", "0..1", 2);
        assertMilestoningQualifiedProperty(legalEntityQps.get(1), "registeredIn", "meta::relational::tests::milestoning::Location", "0..1", 1);

        Class<?> legalEntityPt = getCoreInstance("meta::relational::tests::milestoning::LegalEntityPt");
        ListIterable<? extends QualifiedProperty<?>> legalEntityPtQps = getQualifiedPropertiesFromAssociations(legalEntityPt);
        Assert.assertEquals(Lists.fixedSize.with("registeredInPt(Date[1],Date[1])", "registeredInPt(Date[1])"), legalEntityPtQps.collect(QualifiedProperty::_id));
        assertMilestoningQualifiedProperty(legalEntityPtQps.get(0), "registeredInPt", "meta::relational::tests::milestoning::Location", "0..1", 2);
        assertMilestoningQualifiedProperty(legalEntityPtQps.get(1), "registeredInPt", "meta::relational::tests::milestoning::Location", "0..1", 1);

        Class<?> location = getCoreInstance("meta::relational::tests::milestoning::Location");
        ListIterable<? extends QualifiedProperty<?>> locationQps = getQualifiedPropertiesFromAssociations(location);
        Assert.assertEquals(6, locationQps.size());
        Assert.assertEquals(
                Lists.fixedSize.with("legalEntity()", "legalEntity(Date[1])", "legalEntityAllVersionsInRange(Date[1],Date[1])",
                        "legalEntityPt()", "legalEntityPt(Date[1])", "legalEntityPtAllVersionsInRange(Date[1],Date[1])"),
                locationQps.collect(QualifiedProperty::_id));
        assertMilestoningQualifiedProperty(locationQps.get(0), "legalEntity", "meta::relational::tests::milestoning::LegalEntity", "0..1", 0);
        assertMilestoningQualifiedProperty(locationQps.get(1), "legalEntity", "meta::relational::tests::milestoning::LegalEntity", "0..1", 1);
        assertMilestoningQualifiedProperty(locationQps.get(2), "legalEntityAllVersionsInRange", "meta::relational::tests::milestoning::LegalEntity", "0..1", 2);
        assertMilestoningQualifiedProperty(locationQps.get(3), "legalEntityPt", "meta::relational::tests::milestoning::LegalEntityPt", "0..1", 0);
        assertMilestoningQualifiedProperty(locationQps.get(4), "legalEntityPt", "meta::relational::tests::milestoning::LegalEntityPt", "0..1", 1);
        assertMilestoningQualifiedProperty(locationQps.get(5), "legalEntityPtAllVersionsInRange", "meta::relational::tests::milestoning::LegalEntityPt", "0..1", 2);
    }

    private void assertMilestoningRegularProperty(Property<?, ?> property, String propertyName, String genericType, String multiplicity)
    {
        Assert.assertEquals(propertyName, property._name());
        assertGenericType(genericType, property);
        assertMultiplicity(multiplicity, property);
    }

    private void assertMilestoningQualifiedProperty(QualifiedProperty<?> property, String propertyName, String genericType, String multiplicity, int dateParamSize)
    {
        Assert.assertEquals(propertyName, property._functionName());

        FunctionType functionType = (FunctionType) property._classifierGenericType()._typeArguments().getOnly()._rawType();
        RichIterable<? extends CoreInstance> params = ListHelper.tail(functionType._parameters());
        Assert.assertEquals(Lists.immutable.empty(), functionType._parameters().asLazy().drop(1).reject(p -> p._genericType()._rawType() == getCoreInstance(M3Paths.Date)).collect(VariableExpression::_name, Lists.mutable.empty()));
        Assert.assertEquals(dateParamSize, params.size());
        assertGenericType(genericType, property);
        assertMultiplicity(multiplicity, property);
    }

    private void assertGenericType(String expected, AbstractProperty<?> property)
    {
        assertGenericType(expected, property._genericType());
    }

    private void assertGenericType(String expected, GenericType genericType)
    {
        Assert.assertEquals(expected, org.finos.legend.pure.m3.navigation.generictype.GenericType.print(genericType, true, processorSupport));
    }

    private void assertMultiplicity(String expected, AbstractProperty<?> property)
    {
        assertMultiplicity(expected, property._multiplicity());
    }

    private void assertMultiplicity(String expected, Multiplicity multiplicity)
    {
        Assert.assertEquals(expected, org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(multiplicity, false));
    }

    @SuppressWarnings("unchecked")
    private <T extends CoreInstance> T getCoreInstance(String path)
    {
        T result = (T) runtime.getCoreInstance(path);
        Assert.assertNotNull(path, result);
        return result;
    }

    private ListIterable<? extends Property<?, ?>> getProperties(PropertyOwner owner)
    {
        RichIterable<? extends Property<?, ?>> props = (owner instanceof Class<?>) ? ((Class<?>) owner)._properties() : ((Association) owner)._properties();
        return ListHelper.wrapListIterable(props);
    }

    private ListIterable<? extends Property<?, ?>> getPropertiesFromAssociations(Class<?> owner)
    {
        return ListHelper.wrapListIterable(owner._propertiesFromAssociations());
    }

    private ListIterable<? extends QualifiedProperty<?>> getQualifiedProperties(PropertyOwner owner)
    {
        RichIterable<? extends QualifiedProperty<?>> props = (owner instanceof Class<?>) ? ((Class<?>) owner)._qualifiedProperties() : ((Association) owner)._qualifiedProperties();
        return ListHelper.wrapListIterable(props);
    }

    private ListIterable<? extends QualifiedProperty<?>> getQualifiedPropertiesFromAssociations(Class<?> owner)
    {
        return ListHelper.wrapListIterable(owner._qualifiedPropertiesFromAssociations());
    }

    private ListIterable<? extends Property<?, ?>> getOriginalMilestonedProperties(PropertyOwner owner)
    {
        RichIterable<? extends Property<?, ?>> props = (owner instanceof Class<?>) ? ((Class<?>) owner)._originalMilestonedProperties() : ((Association) owner)._originalMilestonedProperties();
        return ListHelper.wrapListIterable(props);
    }

    private Property<?, ?> getProperty(PropertyOwner owner, String name)
    {
        return getProperties(owner).detect(p -> name.equals(p._name()));
    }

    private Property<?, ?> getPropertyFromAssociation(Class<?> owner, String name)
    {
        return getPropertiesFromAssociations(owner).detect(p -> name.equals(p._name()));
    }

    private QualifiedProperty<?> getQualifiedProperty(PropertyOwner owner, String id)
    {
        return getQualifiedProperties(owner).detect(p -> id.equals(p._id()));
    }

    private QualifiedProperty<?> getQualifiedPropertyFromAssociation(Class<?> owner, String id)
    {
        return getQualifiedPropertiesFromAssociations(owner).detect(p -> id.equals(p._id()));
    }

    private Property<?, ?> getOriginalMilestonedProperty(PropertyOwner owner, String name)
    {
        return getOriginalMilestonedProperties(owner).detect(p -> name.equals(p._name()));
    }
}
