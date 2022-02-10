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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestMilestoningPropertyProcessor extends AbstractTestMilestoning
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("sourceId3.pure");
        runtime.delete("domain.pure");
        runtime.delete("mainModel.pure");

        try
        {
            runtime.compile();
        }
        catch (PureCompilationException e)
        {
            setUp();
        }
    }

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testSynthesizedPropertiesMultiplicities() throws Exception
    {
        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Order{\n" +
                        "   productOptional : Product[0..1];\n" +
                        "   productOne : Product[1];\n" +
                        "   productOneMany : Product[1..*];\n" +
                        "   productMany : Product[*];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.properties, processorSupport);
        assertMilestoningRegularProperty(properties.get(2), "productOptionalAllVersions", "Product", "*");
        assertMilestoningRegularProperty(properties.get(3), "productOneAllVersions", "Product", "1..*");
        assertMilestoningRegularProperty(properties.get(4), "productOneManyAllVersions", "Product", "1..*");
        assertMilestoningRegularProperty(properties.get(5), "productManyAllVersions", "Product", "*");

        ListIterable<? extends CoreInstance> qps = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        assertMilestoningQualifiedProperty(qps.get(0), "productOptional", "Product", "0..1", 0);
        assertMilestoningQualifiedProperty(qps.get(1), "productOptional", "Product", "0..1", 1);
        assertMilestoningQualifiedProperty(qps.get(2), "productOptionalAllVersionsInRange", "Product", "0..1", 2);
        assertMilestoningQualifiedProperty(qps.get(3), "productOne", "Product", "1", 0);
        assertMilestoningQualifiedProperty(qps.get(4), "productOne", "Product", "1", 1);
        assertMilestoningQualifiedProperty(qps.get(5), "productOneAllVersionsInRange", "Product", "1", 2);
        assertMilestoningQualifiedProperty(qps.get(6), "productOneMany", "Product", "1..*", 0);
        assertMilestoningQualifiedProperty(qps.get(7), "productOneMany", "Product", "1..*", 1);
        assertMilestoningQualifiedProperty(qps.get(8), "productOneManyAllVersionsInRange", "Product", "1..*", 2);
        assertMilestoningQualifiedProperty(qps.get(9), "productMany", "Product", "*", 0);
        assertMilestoningQualifiedProperty(qps.get(10), "productMany", "Product", "*", 1);
        assertMilestoningQualifiedProperty(qps.get(11), "productManyAllVersionsInRange", "Product", "*", 2);
    }

    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForBusinessTemporalProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.properties, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(FastList.newListWith("orderId", "productAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> srcOrderProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.originalMilestonedProperties, processorSupport);
        ListIterable<String> srcOrderPropertiesAsStrings = srcOrderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(FastList.newListWith("product"), srcOrderPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("product", propertyName.getName());
        Assert.assertEquals("Product", type.getName());
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertEquals("Product", allVersionsInRangePropertyType.getName());
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
    }

    @Test
    public void testGeneratedMilestonedQualifiedPropertiesMultiplicity() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   product : Product[1];\n" +
                        "   productOptional : Product[0..1];\n" +
                        "   productTwo : Product[2];\n" +
                        "   productMany : Product[*];\n" +
                        "   productRange : Product[2..3];\n" +

                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        ArrayAdapter.adapt(Tuples.pair("product_0", "[1]"), Tuples.pair("productOptional_0", "[0..1]"), Tuples.pair("productTwo_0", "[*]"), Tuples.pair("productMany_0", "[*]"), Tuples.pair("productRange_0", "[*]"))
                .forEach(propertyNameToExpectedMultiplicity ->
                {
                    CoreInstance product = qualifiedProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, propertyNameToExpectedMultiplicity.getOne()));
                    String multiplicityString = Multiplicity.print(product.getValueForMetaPropertyToOne(M3Properties.multiplicity));
                    Assert.assertEquals(propertyNameToExpectedMultiplicity.getTwo(), multiplicityString);
                });
    }

    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForProcessingTemporalProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +

                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.properties, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(FastList.newListWith("orderId", "productAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("product", propertyName.getName());
        Assert.assertEquals("Product", type.getName());
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertEquals("Product", allVersionsInRangePropertyType.getName());
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
    }

    @Test
    public void testGeneratedPropertiesIncludeSourcePropertyTaggedValuesAndStereotypes() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   <<service.disableStreaming>> " +
                        "   {service.contentType='HTTP'}" +
                        "   product : Product[1];\n" +
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        CoreInstance productProperty = Instance.getValueForMetaPropertyToOneResolved(order, M3Properties.properties, processorSupport);
        Assert.assertEquals("productAllVersions", CoreInstance.GET_NAME.valueOf(productProperty));

        CoreInstance servicePackage = processorSupport.package_getByUserPath(M3Paths.service);
        CoreInstance disableStreamingStereotype = servicePackage.getValueForMetaPropertyToMany(M3Properties.p_stereotypes).detect(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance serviceStereotype)
            {
                return CoreInstance.GET_NAME.valueOf(serviceStereotype).equals("disableStreaming");
            }
        });

        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(productProperty, M3Properties.stereotypes, processorSupport);
        Assert.assertEquals(2, stereotypes.size());
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.get(0));
        Assert.assertEquals(disableStreamingStereotype, stereotypes.get(1));

        ListIterable<? extends CoreInstance> taggedValues = Instance.getValueForMetaPropertyToManyResolved(productProperty, M3Properties.taggedValues, processorSupport);
        Assert.assertEquals(1, taggedValues.size());
        Assert.assertEquals("HTTP", taggedValues.get(0).getValueForMetaPropertyToOne(M3Properties.value).getName());
    }

    @Test
    public void testGeneratedPropertiesIncludeAggregationProperty() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   <<service.disableStreaming>> " +
                        "   {service.contentType='HTTP'}" +
                        "   (composite)" +
                        "   orderDetailsComposite : OrderDetails[1];\n" +
                        "   (composite)" +
                        "   orderDetailsNonTemporalComposite : OrderDetailsNonTemporal[1];\n" +
                        "   (shared)" +
                        "   orderDetailsShared : OrderDetails[1];\n" +
                        "   (shared)" +
                        "   orderDetailsNonTemporalShared : OrderDetailsNonTemporal[1];\n" +
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::domain::OrderDetails{\n" +
                        "   name : String[1];\n" +
                        "}" +
                        "Class meta::test::domain::OrderDetailsNonTemporal{\n" +
                        "   name : String[1];\n" +
                        "}" +
                        "Association  meta::test::domain::Order_OrderDetails{\n" +
                        "   order : Order[1];\n" +
                        "   (composite)" +
                        "   orderDetailsViaAssnComposite : OrderDetails[1];\n" +
                        "}"
        );

        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::test::domain::Order");
        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.properties, processorSupport);

        CoreInstance orderDetailsNonTemporalCompositeProperty = orderProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "orderDetailsNonTemporalComposite"));
        CoreInstance compositeAggregation = orderDetailsNonTemporalCompositeProperty.getValueForMetaPropertyToOne(M3Properties.aggregation);
        CoreInstance orderDetailsNonTemporalSharedProperty = orderProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "orderDetailsNonTemporalShared"));
        CoreInstance sharedAggregation = orderDetailsNonTemporalSharedProperty.getValueForMetaPropertyToOne(M3Properties.aggregation);

        CoreInstance orderDetailsTemporalCompositeProperty = orderProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "orderDetailsCompositeAllVersions"));
        CoreInstance orderDetailsTemporalSharedProperty = orderProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "orderDetailsSharedAllVersions"));
        CoreInstance orderDetailsTemporalCompositePropertyAgg = orderDetailsTemporalCompositeProperty.getValueForMetaPropertyToOne(M3Properties.aggregation);
        CoreInstance orderDetailsTemporalSharedPropertyAgg = orderDetailsTemporalSharedProperty.getValueForMetaPropertyToOne(M3Properties.aggregation);

        Assert.assertEquals(compositeAggregation, orderDetailsTemporalCompositePropertyAgg);
        Assert.assertEquals(sharedAggregation, orderDetailsTemporalSharedPropertyAgg);

        CoreInstance orderDetailsAssn = runtime.getCoreInstance("meta::test::domain::Order_OrderDetails");
        ListIterable<? extends CoreInstance> orderDetailsAssnProperties = Instance.getValueForMetaPropertyToManyResolved(orderDetailsAssn, M3Properties.properties, processorSupport);
        CoreInstance orderDetailsViaAssnCompositeAllVersions = orderDetailsAssnProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "orderDetailsViaAssnCompositeAllVersions"));
        CoreInstance orderDetailsViaAssnCompositeAllVersionsAgg = orderDetailsViaAssnCompositeAllVersions.getValueForMetaPropertyToOne(M3Properties.aggregation);
        Assert.assertEquals(compositeAggregation, orderDetailsViaAssnCompositeAllVersionsAgg);
    }


    @Test
    public void testAdditionOfSynthesizedQualifiedPropertiesForBusinessTemporalAssociations() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "}" +
                        "Association meta::relational::tests::milestoning::OrderProduct{\n" +
                        "   product : Product[1];\n" +
                        "   orders : Order[*];\n" +
                        "}"
        );
        runtime.compile();

        CoreInstance orderClass = runtime.getCoreInstance("meta::test::domain::Order");
        Assert.assertNotNull(orderClass);

        CoreInstance productClass = runtime.getCoreInstance("meta::test::domain::Product");
        Assert.assertNotNull(productClass);

        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(Lists.mutable.with("productAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("product", propertyName.getName());
        Assert.assertSame(productClass, type);
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("productAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertSame(productClass, allVersionsInRangePropertyType);
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
    }

    @Test
    public void testQualifiedPropertyVisibility() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "   product : Product[1];\n" +

                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let p = ^Product(name='test', businessDate=%2018-12-18T13:35:21);\n" +
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

        CoreInstance mainClass = runtime.getCoreInstance("test::MainClass");
        Assert.assertNotNull(mainClass);

        CoreInstance otherClass = runtime.getCoreInstance("test::OtherClass");
        Assert.assertNotNull(otherClass);

        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(FastList.newListWith("mainToOtherAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOther", propertyName.getName());
        Assert.assertSame(otherClass, type);
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertSame(otherClass, allVersionsInRangePropertyType);
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
    }


    @Test
    public void testQualifiedPropertiesFromAssociationsInSameSource()
    {
        compileTestSource("mainModel.pure",
                "Class test::MainClass\n" +
                        "{\n" +
                        "}" +
                        "Class <<temporal.businesstemporal>> test::OtherClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::OtherMainAssociation\n" +
                        "{\n" +
                        "   otherToMain : test::MainClass[*];\n" +
                        "   mainToOther : test::OtherClass[*];\n" +
                        "}");

        CoreInstance mainClass = runtime.getCoreInstance("test::MainClass");
        Assert.assertNotNull(mainClass);

        CoreInstance otherClass = runtime.getCoreInstance("test::OtherClass");
        Assert.assertNotNull(otherClass);

        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(Lists.mutable.with("mainToOtherAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOther", propertyName.getName());
        Assert.assertSame(otherClass, type);
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertSame(otherClass, allVersionsInRangePropertyType);
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
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

        CoreInstance mainClass = runtime.getCoreInstance("model::domain::test::MainClass");
        Assert.assertNotNull(mainClass);

        CoreInstance otherClass = runtime.getCoreInstance("model::domain::test::OtherClass");
        Assert.assertNotNull(otherClass);

        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> productPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(Lists.mutable.with("mainToOtherAllVersions"), productPropertiesAsStrings);

        ListIterable<? extends CoreInstance> qualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(mainClass, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Verify.assertSize(2, qualifiedProperties);
        CoreInstance qualifiedProperty = qualifiedProperties.getFirst();
        CoreInstance propertyName = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(qualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOther", propertyName.getName());
        Assert.assertSame(otherClass, type);
        ListIterable<? extends CoreInstance> stereotypes = Instance.getValueForMetaPropertyToManyResolved(qualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, stereotypes);
        Assert.assertEquals(getGeneratedMilestoningStereotype(), stereotypes.getFirst());
        CoreInstance allVersionsInRangeQualifiedProperty = qualifiedProperties.getLast();
        CoreInstance allVersionsInRangePropertyName = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.functionName, processorSupport).getFirst();
        CoreInstance allVersionsInRangePropertyType = Instance.getValueForMetaPropertyToOneResolved(allVersionsInRangeQualifiedProperty, M3Properties.genericType, M3Properties.rawType, processorSupport);
        Assert.assertEquals("mainToOtherAllVersionsInRange", allVersionsInRangePropertyName.getName());
        Assert.assertSame(otherClass, allVersionsInRangePropertyType);
        ListIterable<? extends CoreInstance> allVersionsInRangePropertyStereotypes = Instance.getValueForMetaPropertyToManyResolved(allVersionsInRangeQualifiedProperty, M3Properties.stereotypes, processorSupport);
        Verify.assertSize(1, allVersionsInRangePropertyStereotypes);
        Assert.assertEquals(this.getGeneratedMilestoningStereotype(), allVersionsInRangePropertyStereotypes.getFirst());
    }

    @Test
    public void testRemovalOfOriginalMilestonedPropertyAndMilestonedPropertyGeneration() throws Exception
    {
        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:8 column:7), \"The property 'account' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]");
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class model::domain::trading::Order{" +
                        "   account:model::domain::subdom1::account::Account[0..1];" +
                        "} \n" +
                        "" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let o = ^model::domain::trading::Order();\n" +
                        "   $o.accountAllVersions;\n" +
                        "   $o.account(%2016);\n" +
                        "   $o.account;\n" +
                        "}"
        );
        runtime.compile();

        CoreInstance order = runtime.getCoreInstance("model::domain::trading::Order");
        assertAllPropertiesInCompiledState(order, _Class.ALL_PROPERTIES_PROPERTIES);
        assertAllPropertiesHaveOwnerSet(order, _Class.ALL_PROPERTIES_PROPERTIES);

    }

    @Test
    public void testRemovalOfOriginalMilestonedPropertyAndGenerationOfAllPropertyViaAssociation() throws Exception
    {
        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:8 column:7), \"The property 'account' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]");
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class model::domain::trading::Order{} \n" +
                        "" +
                        "Association OrderAccount{" +
                        "   account:model::domain::subdom1::account::Account[0..1];" +
                        "   order:model::domain::trading::Order[0..1];" +
                        "}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let o = ^model::domain::trading::Order();\n" +
                        "   $o.accountAllVersions;\n" +
                        "   $o.account(%2016);\n" +
                        "   $o.account;\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testNoArgPropertyNotGeneratedWhenSourceClassIsNotTemporal() throws Exception
    {
        expectedEx.expectMessage("The property 'legalEntity' is milestoned with stereotypes: [ businesstemporal ] and requires date parameters: [ businessDate ]");
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;" +
                        "import model::domain::subdom1::product::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::entity::LegalEntity \n" +
                        "{}" +
                        "Class model::domain::subdom1::product::Product \n" +
                        "{legalEntity : LegalEntity[*];}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let p = ^model::domain::subdom1::product::Product();\n" +
                        "   $p.legalEntity;\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testGenerationOfNoArgQualifiedPropertyFromAssociationWhereTargetClassInheritsFromClassWithBusinessTemporalStereotype() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>>  model::domain::trading::Order{} \n" +
                        "" +
                        "Association OrderAccount{" +
                        "   account:model::domain::subdom1::account::FirmAccount[0..1];" +
                        "   order:model::domain::trading::Order[0..1];" +
                        "}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "{|model::domain::trading::Order.all(%2016)->filter(o|$o.account->isEmpty())};" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testGenerationOfNoArgQualifiedPropertyFromAssociationWhereTargetClassInheritsFromClassWithBusinessTemporalStereotypeWithMultipleSources() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>>  model::domain::trading::Order{" +
                        "   orderId : Integer[1];\n" +
                        "   orderIdQp(){$this.orderId} : Integer[1];\n" +
                        "} \n"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import model::domain::subdom1::account::*;" +
                        "Association OrderAccount{" +
                        "   account : FirmAccount[0..1];" +
                        "   order:model::domain::trading::Order[0..1];" +
                        "}"
        );
        runtime.createInMemorySource("sourceId3.pure",
                "import model::domain::subdom1::account::*;" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "{|model::domain::trading::Order.all(%2016)->filter(o|$o.account->isEmpty())};" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testPropertiesInterceptedAndTransformedToQualifiedPropertiesWhereSourceAndTargetAreBusinessTemporal() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;" +
                        "import model::domain::subdom1::product::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::entity::LegalEntity \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::product::Product \n" +
                        "{legalEntity : LegalEntity[*];}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "{|model::domain::subdom1::product::Product.all(%2016)->filter(o|$o.legalEntity->isEmpty())};" +
                        "{|model::domain::subdom1::product::Product.all(%2016)->filter(o|$o.legalEntity(%2015)->isEmpty())};" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testValidationOfOverridenProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::entity::*;" +
                        "import model::domain::subdom1::product::*;" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::VehicleOwner \n" +
                        "{" +
                        "vehicles : meta::relational::tests::milestoning::inheritance::Vehicle[*];" +
                        "}" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::Person extends meta::relational::tests::milestoning::inheritance::VehicleOwner \n" +
                        "{}" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::inheritance::Vehicle\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "   description: String[1];\n" +
                        "}" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  print('go',1); \n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testPropertyOwnerSetOnMilestonedProperties() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class meta::test::domain::Order{\n" +
                        "   product : Product[1];" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::Product{\n" +
                        "}" +
                        "Association meta::relational::tests::milestoning::OrderProduct{\n" +
                        "   assocProduct : Product[1];\n" +
                        "   orders : Order[*];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;" +
                        "Association meta::relational::tests::milestoning::OrderProduct2{\n" +
                        "   assocProduct2 : Product[1];\n" +
                        "   orders2 : Order[*];\n" +
                        "}"
        );
        runtime.compile();

        //test Association
        CoreInstance assn = runtime.getCoreInstance("meta::relational::tests::milestoning::OrderProduct");
        ListIterable<? extends CoreInstance> assnProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.properties, processorSupport);
        Verify.assertSize(2, assnProperties);
        Verify.assertContainsAll(assnProperties.collect(CoreInstance.GET_NAME), "orders", "assocProductAllVersions");

        //test Order
        CoreInstance orderClass = runtime.getCoreInstance("meta::test::domain::Order");

        ListIterable<? extends CoreInstance> orderProperties = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.properties, processorSupport);
        ListIterable<String> orderPropertiesAsStrings = orderProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(orderPropertiesAsStrings, "productAllVersions");
        ListIterable<? extends CoreInstance> orderPropertiesFromAssociations = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> orderPropertiesFromAssociationsAsStrings = orderPropertiesFromAssociations.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(orderPropertiesFromAssociationsAsStrings, "assocProduct2AllVersions", "assocProductAllVersions");
    }

    @Test
    public void testMilestoningPropertyGenerationFromInheritanceHierarchySource()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A{}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B extends A{}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::C{\n" +
                        "}" +
                        "Association AC{\n" +
                        "   c : C[1];\n" +
                        "   a : A[1];\n" +
                        "}" +
                        "Association BC{\n" +
                        "   c : C[1];\n" +
                        "   b : B[1];\n" +
                        "}"
        );
        runtime.compile();

    }

    @Test
    public void testQualifiedPropertyGenerationForAssociations() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A{}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B{\n" +
                        "}" +
                        "Association meta::test::domain::AC{\n" +
                        "   b : B[1];\n" +
                        "   a : A[1];\n" +
                        "}"

        );
        runtime.compile();

        CoreInstance orderClass = runtime.getCoreInstance("meta::test::domain::A");

        ListIterable<? extends CoreInstance> aProperties = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> aPropertiesAsStrings = aProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(aPropertiesAsStrings, "bAllVersions");

        ListIterable<? extends CoreInstance> aQpProperties = Instance.getValueForMetaPropertyToManyResolved(orderClass, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        ListIterable<String> orderPropertiesAsStrings = aQpProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(Lists.immutable.with("b_0", "b_1", "bAllVersionsInRange_2"), orderPropertiesAsStrings);

        CoreInstance assn = runtime.getCoreInstance("meta::test::domain::AC");
        ListIterable<? extends CoreInstance> assnProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.properties, processorSupport);
        Verify.assertSize(2, assnProperties);
        ListIterable<String> assnPopertiesAsStrings = assnProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(assnPopertiesAsStrings, "aAllVersions", "bAllVersions");
        ListIterable<? extends CoreInstance> assnQualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.qualifiedProperties, processorSupport);
        ListIterable<String> assnQualifiedPropertiesAsStrings = assnQualifiedProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(assnQualifiedPropertiesAsStrings, "a_3", "b_0");
    }

    @Test
    public void testQualifiedPropertyGenerationForAssociationsInDifferentSources() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A{}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B{\n" +
                        "}");

        runtime.compile();

        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;" +
                        "Association meta::test::domain::AC{\n" +
                        "   b : B[*];\n" +
                        "   a : A[*];\n" +
                        "}"
        );
        //Class's are not part of the set of instances in scope for post processing this time
        //Rely on returning/processing the generated QualifiedProperties from AssociationProcessor
        runtime.compile();

        CoreInstance aCi = runtime.getCoreInstance("meta::test::domain::A");
        CoreInstance acCi = runtime.getCoreInstance("meta::test::domain::AC");

        //validate edge point properties
        ListIterable<? extends CoreInstance> aProperties = Instance.getValueForMetaPropertyToManyResolved(aCi, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> aPropertiesAsStrings = aProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(1, aPropertiesAsStrings.size());
        Verify.assertContainsAll(aPropertiesAsStrings, "bAllVersions");
        Assert.assertEquals(acCi, aProperties.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));

        CoreInstance bCi = runtime.getCoreInstance("meta::test::domain::B");
        ListIterable<? extends CoreInstance> bProperties = Instance.getValueForMetaPropertyToManyResolved(bCi, M3Properties.propertiesFromAssociations, processorSupport);
        ListIterable<String> bPropertiesAsStrings = bProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(1, bPropertiesAsStrings.size());
        Verify.assertContainsAll(bPropertiesAsStrings, "aAllVersions");

        ListIterable<? extends CoreInstance> aQpProperties = Instance.getValueForMetaPropertyToManyResolved(aCi, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        ListIterable<String> orderPropertiesAsStrings = aQpProperties.collect(CoreInstance.GET_NAME);
        Assert.assertEquals(Lists.immutable.with("b_0", "b_1", "bAllVersionsInRange_2"), orderPropertiesAsStrings);
        ListIterable<CoreInstance> qpExprSeqFunc = aQpProperties.collect(qp -> qp.getValueForMetaPropertyToOne(M3Properties.expressionSequence).getValueForMetaPropertyToOne(M3Properties.func));
        Assert.assertEquals(3, qpExprSeqFunc.size());
        Assert.assertEquals(Sets.mutable.with("filter_T_MANY__Function_1__T_MANY_"), qpExprSeqFunc.toSet().collect(CoreInstance.GET_NAME));

        CoreInstance assn = runtime.getCoreInstance("meta::test::domain::AC");
        ListIterable<? extends CoreInstance> assnProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.properties, processorSupport);
        Assert.assertEquals(2, assnProperties.size());
        ListIterable<String> assnPopertiesAsStrings = assnProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(assnPopertiesAsStrings, "bAllVersions", "aAllVersions");
        ListIterable<? extends CoreInstance> assnQualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.qualifiedProperties, processorSupport);
        ListIterable<String> assnQualifiedPropertiesAsStrings = assnQualifiedProperties.collect(CoreInstance.GET_NAME);
        Verify.assertContainsAll(assnQualifiedPropertiesAsStrings, "b_0", "a_3");
    }

    @Test
    public void testEdgePointPropertyOwnerIsNotOverriddenWhenMultiplePropertiesAreProcessedViaAssociation() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A{}\n" +
                        "\n" +
                        "Class meta::test::domain::B{\n" +
                        "}" +
                        "Association meta::test::domain::AB{\n" +
                        "   a : A[*];\n" +
                        "   b1 : B[*];\n" +
                        "}");

        runtime.compile();
        CoreInstance assnAB = runtime.getCoreInstance("meta::test::domain::AB");
        CoreInstance b = runtime.getCoreInstance("meta::test::domain::B");
        ListIterable<? extends CoreInstance> bAssnProperties = Instance.getValueForMetaPropertyToManyResolved(b, M3Properties.propertiesFromAssociations, processorSupport);
        Assert.assertEquals(1, bAssnProperties.size());
        Assert.assertEquals(assnAB, bAssnProperties.getFirst().getValueForMetaPropertyToOne(M3Properties.owner));

        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::C{}\n" +
                        "\n" +
                        "Association meta::test::domain::AC{\n" +
                        "   b2 : B[*];\n" +
                        "   c : C[*];\n" +
                        "}"
        );
        runtime.compile();
        CoreInstance assnAC = runtime.getCoreInstance("meta::test::domain::AC");
        b = runtime.getCoreInstance("meta::test::domain::B");
        bAssnProperties = Instance.getValueForMetaPropertyToManyResolved(b, M3Properties.propertiesFromAssociations, processorSupport);
        Assert.assertEquals(2, bAssnProperties.size());
        CoreInstance propA = bAssnProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "aAllVersions"));
        CoreInstance propc = bAssnProperties.detect(Predicates.attributeEqual(CoreInstance.GET_NAME, "cAllVersions"));

        Assert.assertEquals(assnAB, propA.getValueForMetaPropertyToOne(M3Properties.owner));
        Assert.assertEquals(assnAC, propc.getValueForMetaPropertyToOne(M3Properties.owner));
    }

    @Test
    public void testSourceInformationOfGeneratedMilestonedProperties() throws Exception
    {
        Function<Boolean, String> source = new Function<Boolean, String>()
        {
            @Override
            public String valueOf(Boolean isBTemporal)
            {
                return "import meta::test::domain::*;\n" +
                        "Class meta::test::domain::A{\n" +
                        "b : B[*];}\n" +
                        "Association AB{ a2:A[0..1]; b2:B[0..1];}\n" +
                        "Class " + (isBTemporal ? "<<temporal.businesstemporal>>" : "") + "meta::test::domain::B{}\n" +
                        "function go():Any[*]{let a = ^A();" + (isBTemporal ? "$a.bAllVersions;$a.b(%2015);$a.b2AllVersions;$a.b2(%2015);" : "$a.b;$a.b2;") + "}\n";
            }
        };
        runtime.createInMemorySource("sourceId.pure", source.valueOf(false));
        runtime.compile();
        CoreInstance goFunction = runtime.getFunction("go__Any_MANY_");
        CoreInstance property = goFunction.getValueForMetaPropertyToMany(M3Properties.expressionSequence).get(1).getValueForMetaPropertyToOne(M3Properties.func);
        CoreInstance assnProperty = goFunction.getValueForMetaPropertyToMany(M3Properties.expressionSequence).get(2).getValueForMetaPropertyToOne(M3Properties.func);

        SourceInformation srcPropertySourceInformation = property.getSourceInformation();
        SourceInformation srcAssnPropertySourceInformation = assnProperty.getSourceInformation();

        runtime.delete("sourceId.pure");
        runtime.createInMemorySource("sourceId.pure", source.valueOf(true));
        runtime.compile();
        goFunction = runtime.getFunction("go__Any_MANY_");
        ListIterable expressions = goFunction.getValueForMetaPropertyToMany(M3Properties.expressionSequence);
        ListIterable<ListIterable<CoreInstance>> propertyAndAssnExpressions = ListHelper.tail(expressions).chunk(2).toList();
        for (CoreInstance expressionSequence : propertyAndAssnExpressions.get(0))
        {
            property = expressionSequence.getValueForMetaPropertyToOne(M3Properties.func);
            SourceInformation milestonedPropertySourceInformation = property.getSourceInformation();
            Assert.assertEquals(srcPropertySourceInformation, milestonedPropertySourceInformation);
        }

        for (CoreInstance expressionSequence : propertyAndAssnExpressions.get(1))
        {
            property = expressionSequence.getValueForMetaPropertyToOne(M3Properties.func);
            SourceInformation milestonedAssnPropertySourceInformation = property.getSourceInformation();
            Assert.assertEquals(srcAssnPropertySourceInformation, milestonedAssnPropertySourceInformation);
        }
    }

    @Test
    public void testAssociationPropertiesWithSamePropertyNameProcessTypeArgumentsCorrectly() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::A{}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::B{\n" +
                        "}" +
                        "Association meta::test::domain::AB{\n" +
                        "   a : A[*];\n" +
                        "   a : B[*];\n" +
                        "}");

        runtime.compile();

        CoreInstance a = runtime.getCoreInstance("meta::test::domain::A");
        ListIterable<? extends CoreInstance> aQualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(a, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        CoreInstance bGenericType = aQualifiedProperties.getFirst().getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance bRawType = Instance.getValueForMetaPropertyToOneResolved(bGenericType, M3Properties.rawType, processorSupport);

        CoreInstance b = runtime.getCoreInstance("meta::test::domain::B");
        ListIterable<? extends CoreInstance> bQualifiedProperties = Instance.getValueForMetaPropertyToManyResolved(b, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        CoreInstance aGenericType = bQualifiedProperties.getFirst().getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance aRawType = Instance.getValueForMetaPropertyToOneResolved(aGenericType, M3Properties.rawType, processorSupport);

        assertQualifiedPropertyThisTypeArgumentParamsHaveCorrectGenericType(aQualifiedProperties, aRawType);
        assertQualifiedPropertyThisTypeArgumentParamsHaveCorrectGenericType(bQualifiedProperties, bRawType);

        CoreInstance assn = runtime.getCoreInstance("meta::test::domain::AB");
        ListIterable<CoreInstance> associationPropertyATypeArguments = Lists.mutable.with(bRawType, aRawType);

        ListIterable<? extends CoreInstance> originalMilestonedProperties = Instance.getValueForMetaPropertyToManyResolved(assn, M3Properties.originalMilestonedProperties, processorSupport);
        ListIterable<ListIterable<CoreInstance>> propertiesTypeArguments = originalMilestonedProperties.collect(new Function<CoreInstance, ListIterable<CoreInstance>>()
        {
            @Override
            public ListIterable<CoreInstance> valueOf(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToMany(M3Properties.typeArguments).collect(new Function<CoreInstance, CoreInstance>()
                {
                    @Override
                    public CoreInstance valueOf(CoreInstance coreInstance)
                    {
                        return Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.rawType, processorSupport);
                    }
                });
            }
        });

        Assert.assertEquals(associationPropertyATypeArguments, propertiesTypeArguments.get(0));
        Assert.assertEquals(associationPropertyATypeArguments.asReversed().toList(), propertiesTypeArguments.get(1));

        assertAllPropertiesInCompiledState(assn, Lists.mutable.with(M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.originalMilestonedProperties));
        assertAllPropertiesHaveOwnerSet(assn, Lists.mutable.with(M3Properties.properties, M3Properties.qualifiedProperties, M3Properties.originalMilestonedProperties));
    }

    @Test
    public void testPropertyConflictsExistingPropertiesVsGeneratedProperties()
    {
        String domain = "import meta::relational::tests::milestoning::*;" +
                "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Order { other(s:String[1]){'other'}:String[1]; \n" +
                "                                                                            other2(s:String[1]){'other'}:String[1];" +
                "                                                                                    createdLocation : Location[*]; \n" +
                "                                                                            createdLocation(processingDate:Date[1], businessDate:Date[1], s:String[1]){$this.createdLocation(%latest, %latest)}: Location[*]; }\n";
        String domain2 = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}";

        runtime.createInMemorySource("domain.pure", domain);
        runtime.createInMemorySource("domain2.pure", domain2);
        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::relational::tests::milestoning::Order");
        ListIterable<? extends CoreInstance> orderQps = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        ListMultimap<String, ? extends CoreInstance> qualifiedPropertiesByName = orderQps.groupBy(CoreInstance.GET_NAME);

        Verify.assertContainsAll(qualifiedPropertiesByName.keysView(), "other_0", "other2_1", "createdLocation_2", "createdLocation_3", "createdLocation_4", "createdLocation_5");
    }

    @Test
    public void testBiTemporalPropertyGeneration() throws Exception
    {
        String domain = "import meta::relational::tests::milestoning::*;" +
                "Class meta::relational::tests::milestoning::Order { createdLocation : Location[0..1]; }\n" +
                "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Exchange { basedIn : Location[0..1]; }\n" +
                "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::LegalEntity {}\n" +
                "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::LegalEntityPt {}\n" +
                "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}" +
                "Association LegalEntity_Location{ registeredIn : Location[0..1]; legalEntity: LegalEntity[0..1]; }" +
                "Association LegalEntityPt_Location{ registeredInPt : Location[0..1]; legalEntityPt: LegalEntityPt[0..1]; }";

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
        CoreInstance order = runtime.getCoreInstance("meta::relational::tests::milestoning::Order");
        ListIterable<? extends CoreInstance> orderQps = Instance.getValueForMetaPropertyToManyResolved(order, M3Properties.qualifiedProperties, processorSupport);
        Assert.assertEquals(1, orderQps.size());
        assertMilestoningQualifiedProperty(orderQps.getFirst(), "createdLocation", "Location", "0..1", 2);

        CoreInstance exchange = runtime.getCoreInstance("meta::relational::tests::milestoning::Exchange");
        ListIterable<? extends CoreInstance> exchangeQps = Instance.getValueForMetaPropertyToManyResolved(exchange, M3Properties.qualifiedProperties, processorSupport);
        Assert.assertEquals(3, exchangeQps.size());
        assertMilestoningQualifiedProperty(exchangeQps.get(0), "basedIn", "Location", "0..1", 2);
        assertMilestoningQualifiedProperty(exchangeQps.get(1), "basedIn", "Location", "0..1", 1);
        assertMilestoningQualifiedProperty(exchangeQps.get(2), "basedIn", "Location", "0..1", 0);

        CoreInstance legalEntity = runtime.getCoreInstance("meta::relational::tests::milestoning::LegalEntity");
        ListIterable<? extends CoreInstance> legalEntityQps = Instance.getValueForMetaPropertyToManyResolved(legalEntity, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Assert.assertEquals(2, legalEntityQps.size());
        assertMilestoningQualifiedProperty(legalEntityQps.get(0), "registeredIn", "Location", "0..1", 2);
        assertMilestoningQualifiedProperty(legalEntityQps.get(1), "registeredIn", "Location", "0..1", 1);

        CoreInstance legalEntityPt = runtime.getCoreInstance("meta::relational::tests::milestoning::LegalEntityPt");
        ListIterable<? extends CoreInstance> legalEntityPtQps = Instance.getValueForMetaPropertyToManyResolved(legalEntityPt, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Assert.assertEquals(2, legalEntityPtQps.size());
        assertMilestoningQualifiedProperty(legalEntityPtQps.get(0), "registeredInPt", "Location", "0..1", 2);
        assertMilestoningQualifiedProperty(legalEntityPtQps.get(1), "registeredInPt", "Location", "0..1", 1);


        CoreInstance location = runtime.getCoreInstance("meta::relational::tests::milestoning::Location");
        ListIterable<? extends CoreInstance> locationQps = Instance.getValueForMetaPropertyToManyResolved(location, M3Properties.qualifiedPropertiesFromAssociations, processorSupport);
        Assert.assertEquals(6, locationQps.size());
        assertMilestoningQualifiedProperty(locationQps.get(0), "legalEntity", "LegalEntity", "0..1", 0);
        assertMilestoningQualifiedProperty(locationQps.get(1), "legalEntity", "LegalEntity", "0..1", 1);
        assertMilestoningQualifiedProperty(locationQps.get(2), "legalEntityAllVersionsInRange", "LegalEntity", "0..1", 2);
        assertMilestoningQualifiedProperty(locationQps.get(3), "legalEntityPt", "LegalEntityPt", "0..1", 0);
        assertMilestoningQualifiedProperty(locationQps.get(4), "legalEntityPt", "LegalEntityPt", "0..1", 1);
        assertMilestoningQualifiedProperty(locationQps.get(5), "legalEntityPtAllVersionsInRange", "LegalEntityPt", "0..1", 2);
    }

    private void assertMilestoningRegularProperty(CoreInstance property, String propertyName, String genericType, String multiplicity)
    {
        Assert.assertEquals(propertyName, Instance.getValueForMetaPropertyToManyResolved(property, M3Properties.name, processorSupport).getFirst().getName());
        assertMilestoningProperty(property, genericType, multiplicity);
    }

    private void assertMilestoningQualifiedProperty(CoreInstance property, String propertyName, String genericType, String multiplicity, int dateParamSize)
    {
        Assert.assertEquals(propertyName, Instance.getValueForMetaPropertyToManyResolved(property, M3Properties.functionName, processorSupport).getFirst().getName());
        CoreInstance functionType = property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToOne(M3Properties.rawType);
        RichIterable<? extends CoreInstance> params = ListHelper.tail(functionType.getValueForMetaPropertyToMany(M3Properties.parameters));
        Assert.assertTrue(params.allSatisfy(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M3Properties.genericType).getValueForMetaPropertyToOne(M3Properties.rawType) == _Package.getByUserPath(M3Paths.Date, processorSupport);
            }
        }));
        Assert.assertEquals(dateParamSize, params.size());
        assertMilestoningProperty(property, genericType, multiplicity);
    }

    private void assertMilestoningProperty(CoreInstance property, String genericType, String multiplicity)
    {
        Assert.assertEquals(genericType, Instance.getValueForMetaPropertyToOneResolved(property, M3Properties.genericType, M3Properties.rawType, processorSupport).getName());
        Assert.assertEquals(multiplicity, Multiplicity.print(property.getValueForMetaPropertyToOne(M3Properties.multiplicity), false));
    }

    private void assertAllPropertiesInCompiledState(CoreInstance assn, ListIterable<String> propertyKeys)
    {
        for (String propertyKey : propertyKeys)
        {
            for (CoreInstance propertyValue : Instance.getValueForMetaPropertyToManyResolved(assn, propertyKey, processorSupport))
            {
                Assert.assertTrue(propertyValue.hasCompileState(CompileState.PROCESSED));
            }
        }
    }

    private void assertAllPropertiesHaveOwnerSet(CoreInstance assn, ListIterable<String> propertyKeys)
    {
        for (String propertyKey : propertyKeys)
        {
            for (CoreInstance propertyValue : Instance.getValueForMetaPropertyToManyResolved(assn, propertyKey, processorSupport))
            {
                Assert.assertNotNull(propertyValue.getValueForMetaPropertyToOne(M3Properties.owner));
            }
        }
    }

    private void assertQualifiedPropertyThisTypeArgumentParamsHaveCorrectGenericType(ListIterable<? extends CoreInstance> qualifiedProperties, final CoreInstance expectedRawType)
    {
        qualifiedProperties.forEach(bQualifiedProperty ->
        {
            CoreInstance functionType = bQualifiedProperty.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToOne(M3Properties.rawType);
            ListIterable<? extends CoreInstance> functionTypeParams = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
            functionTypeParams.select(functionTypeParam -> Instance.instanceOf(functionTypeParam, M3Paths.VariableExpression, processorSupport) && "this".equals(functionTypeParam.getValueForMetaPropertyToOne(M3Properties.name).getName())).
                    forEach(functionTypeParam ->
                            {
                                CoreInstance functionTypeParamGenericType = functionTypeParam.getValueForMetaPropertyToOne(M3Properties.genericType);
                                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(functionTypeParamGenericType, M3Properties.rawType, processorSupport);
                                Assert.assertEquals(expectedRawType, rawType);
                            }
                    );
        });
    }
}
