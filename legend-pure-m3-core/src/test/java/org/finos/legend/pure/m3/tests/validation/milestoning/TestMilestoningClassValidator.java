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

package org.finos.legend.pure.m3.tests.validation.milestoning;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMilestoningClassValidator extends AbstractPureTestWithCoreCompiledPlatform
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
    }

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubType()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::BaseProduct\n" +
                        "{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure lines:5c70-8c1), \"All temporal stereotypes in a hierarchy must be the same, class meta::test::milestoning::domain::Product is businesstemporal, top most supertype meta::test::milestoning::domain::BaseProduct is not temporal\"", e.getMessage());
    }

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubTypeWithMultipleParents() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::BaseProduct1\n" +
                        "{\n" +
                        "}\n" +
                        "Class meta::test::milestoning::domain::BaseProduct2\n" +
                        "{\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct1, BaseProduct2\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure lines:8c70-11c1), \"All temporal stereotypes in a hierarchy must be the same, class meta::test::milestoning::domain::Product is businesstemporal, top most supertypes meta::test::milestoning::domain::BaseProduct1, meta::test::milestoning::domain::BaseProduct2 are not temporal\"", e.getMessage());
    }

    @Test
    public void testATypeMayOnlyHaveOneTemporalStereotype() throws Exception
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal,temporal.processingtemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure lines:2c98-5c1), \"A Type may only have one Temporal Stereotype, 'meta::test::milestoning::domain::Product' has [businesstemporal, processingtemporal]\"", e.getMessage());
    }

    @Test
    public void testNonTemporalTypesCanHaveReservedTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   businessDate : Date[1];\n" +
                        "   processingDate : Date[1];\n" +
                        "   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];\n" +
                        "}\n");

        runtime.compile();
    }

    @Test
    public void testTemporalTypesCanNotHaveReservedTemporalProperties()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   businessDate : Date[1];\n" +
                        "   processingDate : Date[1];\n" +
                        "   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure lines:2c64-7c1), \"Type: meta::test::milestoning::domain::Product has temporal specification: [businessDate, milestoning, processingDate] properties: [businessDate, milestoning, processingDate] are reserved and should not be explicit in the Model\"", e.getMessage());
    }
}
