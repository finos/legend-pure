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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class TestMilestoningClassProcessor extends AbstractTestMilestoning
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("domain.pure");
        runtime.delete("singleInheritance.pure");

        runtime.compile();
    }

    @Test
    public void testNonTemporalStereotypeProcessing()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Profile testProfile\n" +
                        "{\n" +
                        "   stereotypes:[s1,s2];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<testProfile.s1>> meta::test::milestoning::domain::NonTemporalClassWithStereotype\n" +
                        "{\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testImportOfBusinessMilestonedClass()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::domain::account::Account\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::account::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::account::Account_Request\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association meta::test::domain::account::Case_Accounts\n" +
                        "{\n" +
                        "  Account_Request:meta::test::domain::account::Account_Request[*];\n" +
                        "  Account:meta::test::domain::account::Account[1];\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testGeneratedMilestonedPropertyHasACorrectlyGeneratedImportGroup()
    {
        runtime.createInMemorySource("sourceId.pure",
                "Class <<temporal.businesstemporal>> meta::test::domain::account::FirmAccount\n" +
                        "{\n" +
                        "}"
        );
        runtime.createInMemorySource("sourceId2.pure",
                "###Pure\n" + //this increments the import group id; '_2' in this case
                        "import meta::test::domain::account::*;\n" +
                        "Class meta::test::domain::account::trade::Contract\n" +
                        "{\n" +
                        "   firmRiskAccount : FirmAccount[1];\n" +
                        "}"
        );
        runtime.compile();
    }

    @Test
    public void testConflictingStereotypesOnParent()
    {
        runtime.createInMemorySource("domain.pure",
                "import meta::relational::tests::milestoning::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Product {}\n" +
                        "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Instrument {}\n" +
                        "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Stock extends Product, Instrument {}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:domain.pure line:4 column:77), \"A Type may only have one Temporal Stereotype, 'meta::relational::tests::milestoning::Stock' has [businesstemporal, processingtemporal]\"", e.getMessage());
    }

    @Test
    public void testPropertiesGeneratedForSubclassOfBusinessTemporalSuperClass()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account {}\n" +
                        "Class model::domain::subdom1::account::FirmAccount extends Account {}"
        );
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:3 column:40), \"Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testMilestoningStereotypeExistsAtAllLevelsInAClassHierarchy()
    {
        runtime.createInMemorySource("singleInheritance.pure",
                "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account {}\n" +
                        "Class model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account {}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:40), \"Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchy()
    {
        runtime.createInMemorySource("singleInheritance.pure",
                "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account {}\n" +
                        "Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account {}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:72), \"All temporal stereotypes in a hierarchy must be the same, class model::domain::subdom1::account::FirmAccount is processingtemporal, top most supertype model::domain::subdom1::account::Account has milestoning stereotype: 'businesstemporal'\"", e.getMessage());
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchyNonTemporalSupertype()
    {
        runtime.createInMemorySource("singleInheritance.pure",
                "Class model::domain::subdom1::account::Account {}\n" +
                        "Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account {}");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:singleInheritance.pure line:2 column:72), \"All temporal stereotypes in a hierarchy must be the same, class model::domain::subdom1::account::FirmAccount is processingtemporal, top most supertype model::domain::subdom1::account::Account is not temporal\"", e.getMessage());
    }

    @Test
    public void testValidTemporalSpecificationInAClassHierarchyCompiles()
    {
        runtime.createInMemorySource("singleInheritance.pure",
                "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account\n" +
                        "{\n" +
                        "}");
        runtime.compile();
    }

    @Test
    public void testParserGeneratedProcessingTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n"
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assert.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "milestoning")));

        Property<?, ?> processingDateProperty = parserGeneratedMilestoningProperties.detect(p -> "processingDate".equals(p._name()));
        Assert.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", processingDateProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), processingDateProperty.getSourceInformation());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assert.assertEquals("meta::pure::milestoning::ProcessingDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), milestoningProperty.getSourceInformation());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }

    @Test
    public void testParserGeneratedBusinessTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}"
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assert.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("businessDate", "milestoning")));

        Property<?, ?> businessDateProperty = parserGeneratedMilestoningProperties.detect(p -> "businessDate".equals(p._name()));
        Assert.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", businessDateProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), businessDateProperty.getSourceInformation());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assert.assertEquals("meta::pure::milestoning::BusinessDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), milestoningProperty.getSourceInformation());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }

    @Test
    public void testParserGeneratedBiTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order\n" +
                        "{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   name : String[1];\n" +
                        "}\n"
        );
        runtime.compile();

        Class<?> milestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<? extends Property<?, ?>> parserGeneratedMilestoningProperties = milestonedClass._properties().select(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport), Lists.mutable.empty());
        Assert.assertEquals(3, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(Property::_name);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "businessDate", "milestoning")));

        Property<?, ?> processingDateProperty = parserGeneratedMilestoningProperties.detect(p -> "processingDate".equals(p._name()));
        Assert.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", processingDateProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), processingDateProperty.getSourceInformation());

        Property<?, ?> businessDateProperty = parserGeneratedMilestoningProperties.detect(p -> "businessDate".equals(p._name()));
        Assert.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", businessDateProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), businessDateProperty.getSourceInformation());

        Property<?, ?> milestoningProperty = parserGeneratedMilestoningProperties.detect(p -> "milestoning".equals(p._name()));
        Assert.assertEquals("meta::pure::milestoning::BiTemporalMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());
        Assert.assertEquals(milestonedClass.getSourceInformation(), milestoningProperty.getSourceInformation());

        Class<?> nonMilestonedClass = (Class<?>) processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().noneSatisfy(p -> MilestoningFunctions.isGeneratedMilestoningDateProperty(p, processorSupport)));
    }
}
