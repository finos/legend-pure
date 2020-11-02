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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions.IsMilestoneDatePropertyPredicate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.*;
import org.junit.rules.ExpectedException;

public class TestMilestoningClassProcessor extends AbstractTestMilestoning
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("sourceId.pure");
        runtime.delete("sourceId2.pure");
        runtime.delete("domain.pure");
        runtime.delete("singleInheritance.pure");
    }

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testNonTemporalStereotypeProcessing() throws Exception
    {

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;" +
                        "Profile testProfile{stereotypes:[s1,s2];}" +
                        "Class <<testProfile.s1>> meta::test::milestoning::domain::NonTemporalClassWithStereotype{\n" +
                        "}"
        );
        this.runtime.compile();
    }

    @Test
    public void testImportOfBusinessMilestonedClass() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class meta::test::domain::account::Account{\n" +
                        "   orderId : Integer[1];\n" +
                        "}"
        );
        this.runtime.createInMemorySource("sourceId2.pure",
                "import meta::test::domain::account::*;" +
                        "Class <<temporal.businesstemporal>> meta::test::domain::account::Account_Request{\n" +
                        "   name : String[1];\n" +
                        "}"+
                        "Association meta::test::domain::account::Case_Accounts{" +
                        "  Account_Request:meta::test::domain::account::Account_Request[*];" +
                        "  Account:meta::test::domain::account::Account[1];" +
                        "}"
        );
        this.runtime.compile();
    }

    @Test
    public void testGeneratedMilestonedPropertyHasACorrectlyGeneratedImportGroup() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "Class <<temporal.businesstemporal>> meta::test::domain::account::FirmAccount \n" +
                        "{}"
        );
        this.runtime.createInMemorySource("sourceId2.pure",
                "###Pure\n"+//this increments the import group id; '_2' in this case
                        "import meta::test::domain::account::*;"+
                        "Class meta::test::domain::account::trade::Contract \n" +
                        "{\n" +
                        "   firmRiskAccount : FirmAccount[1];"+
                        "}"
        );
        this.runtime.compile();
    }

    @Test
    public void testConflictingStereotypesOnParent() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:domain.pure line:1 column:248), \"A Type may only have one Temporal Stereotype, 'Stock' has [businesstemporal,processingtemporal]\"");

        String domain =  "Class <<temporal.businesstemporal>> meta::relational::tests::milestoning::Product{}"+
                "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Instrument{}"+
                "Class <<temporal.processingtemporal>> meta::relational::tests::milestoning::Stock extends meta::relational::tests::milestoning::Product, meta::relational::tests::milestoning::Instrument{}";

        this.runtime.createInMemorySource("domain.pure",domain);
        this.runtime.compile();

    }

    @Test
    public void testPropertiesGeneratedForSubclassOfBusinessTemporalSuperClass() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype(s): 'Account' has milestoning stereotype: 'businesstemporal'");

        this.runtime.createInMemorySource("sourceId.pure",
                "import model::domain::subdom1::account::*;" +
                        "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{} \n" +
                        "Class model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{} \n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let a = ^model::domain::subdom1::account::FirmAccount(businessDate=%2015);\n" +
                        "}"
        );
        this.runtime.compile();
    }

    @Test
    public void testMilestoningStereotypeExistsAtAllLevelsInAClassHierarchy() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Temporal stereotypes must be applied at all levels in a temporal class hierarchy, top most supertype(s): 'Account' has milestoning stereotype: 'businesstemporal'");

        String singleInheritance = "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{} \n" +
                "Class model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{} \n";

        this.runtime.createInMemorySource("singleInheritance.pure", singleInheritance );
        this.runtime.compile();
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchy() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("All temporal stereotypes in a hierarchy must be the same, class: 'FirmAccount' is processingtemporal, top most supertype(s): 'Account' has milestoning stereotype: 'businesstemporal'");

        String singleInheritance = "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{} \n" +
                "Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{} \n";

        this.runtime.createInMemorySource("singleInheritance.pure", singleInheritance );
        this.runtime.compile();
    }

    @Test
    public void testValidationOfMilestoningStereotypeConsistencyInAClassHierarchyNonTemporalSupertype() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage( "All temporal stereotypes in a hierarchy must be the same, class: 'FirmAccount' is processingtemporal, top most supertype(s): 'Account' is not temporal");

        String singleInheritance = "Class model::domain::subdom1::account::Account{} \n" +
                "Class <<temporal.processingtemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{} \n";

        this.runtime.createInMemorySource("singleInheritance.pure", singleInheritance );
        this.runtime.compile();
    }

    @Test
    public void testValidTemporalSpecificationInAClassHierarchyCompiles() throws Exception
    {
        String singleInheritance = "Class <<temporal.businesstemporal>> model::domain::subdom1::account::Account{} \n" +
                "Class <<temporal.businesstemporal>> model::domain::subdom1::account::FirmAccount extends model::domain::subdom1::account::Account{} \n";

        this.runtime.createInMemorySource("singleInheritance.pure", singleInheritance );
        this.runtime.compile();
    }

    @Test
    public void testParserGeneratedProcessingTemporalProperties() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}\n"
        );
        this.runtime.compile();

        Class milestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<Property> parserGeneratedMilestoningProperties = milestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).toList();
        Assert.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(PROPERTY_NAME);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "milestoning")));

        Property processingDateProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "processingDate");
        Assert.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", processingDateProperty._multiplicity().getName());

        Property milestoningProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "milestoning");
        Assert.assertEquals("meta::pure::milestoning::ProcessingDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class nonMilestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).isEmpty());
    }

    @Test
    public void testParserGeneratedBusinessTemporalProperties() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}\n"
        );
        this.runtime.compile();

        Class milestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<Property> parserGeneratedMilestoningProperties = milestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).toList();
        Assert.assertEquals(2, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(PROPERTY_NAME);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("businessDate", "milestoning")));

        Property businessDateProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "businessDate");
        Assert.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", businessDateProperty._multiplicity().getName());

        Property milestoningProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "milestoning");
        Assert.assertEquals("meta::pure::milestoning::BusinessDateMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class nonMilestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).isEmpty());
    }

    @Test
    public void testParserGeneratedBiTemporalProperties() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Order{\n" +
                        "   orderId : Integer[1];\n" +
                        "}\n" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   name : String[1];\n" +
                        "}\n"
        );
        this.runtime.compile();

        Class milestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Product");
        MutableList<Property> parserGeneratedMilestoningProperties = milestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).toList();
        Assert.assertEquals(3, parserGeneratedMilestoningProperties.size());
        MutableList<String> parserGeneratedMilestoningPropertyNames = parserGeneratedMilestoningProperties.collect(PROPERTY_NAME);
        Assert.assertTrue(parserGeneratedMilestoningPropertyNames.containsAll(Lists.mutable.of("processingDate", "businessDate", "milestoning")));

        Property processingDateProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "processingDate");
        Assert.assertEquals("Date", processingDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", processingDateProperty._multiplicity().getName());

        Property businessDateProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "businessDate");
        Assert.assertEquals("Date", businessDateProperty._genericType()._rawType()._name());
        Assert.assertEquals("PureOne", businessDateProperty._multiplicity().getName());

        Property milestoningProperty = parserGeneratedMilestoningProperties.detectWith(PROPERTY_BY_NAME, "milestoning");
        Assert.assertEquals("meta::pure::milestoning::BiTemporalMilestoning", PackageableElement.getUserPathForPackageableElement(milestoningProperty._genericType()._rawType()));
        Assert.assertEquals("ZeroOne", milestoningProperty._multiplicity().getName());

        Class nonMilestonedClass = (Class)this.processorSupport.package_getByUserPath("meta::test::milestoning::domain::Order");
        Assert.assertTrue(nonMilestonedClass._properties().select(new IsMilestoneDatePropertyPredicate(this.processorSupport)).isEmpty());
    }

    private static final Function<Property, String> PROPERTY_NAME = new Function<Property, String>()
    {
        @Override
        public String valueOf(Property property)
        {
            return property._name();
        }
    };

    private static final Predicate2<Property, String> PROPERTY_BY_NAME = new Predicate2<Property, String>()
    {
        @Override
        public boolean accept(Property property, String propertyName)
        {
            return property._name().equals(propertyName);
        }
    };
}
