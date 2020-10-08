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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestMilestoningClassValidator extends AbstractPureTestWithCoreCompiledPlatform {

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubType() throws Exception {

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure lines:2c70-4c1), \"All temporal stereotypes in a hierarchy must be the same, class: 'Product' is businesstemporal, top most supertype(s): 'BaseProduct' is not temporal'\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class meta::test::milestoning::domain::BaseProduct{}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct{\n" +
                        "   id : Integer[1];\n" +
                        "}");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningStereotypeCannotBeAppliedInASubTypeWithMultipleParents() throws Exception {

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure lines:3c70-5c1), \"All temporal stereotypes in a hierarchy must be the same, class: 'Product' is businesstemporal, top most supertype(s): 'BaseProduct1,BaseProduct2' is not temporal'\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class meta::test::milestoning::domain::BaseProduct1{}\n" +
                        "Class meta::test::milestoning::domain::BaseProduct2{}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct1, BaseProduct2{\n" +
                        "   id : Integer[1];\n" +
                        "}");

        this.runtime.compile();
    }

    @Test
    public void testATypeMayOnlyHaveOneTemporalStereotype() throws Exception {

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure lines:1c140-3c1), \"A Type may only have one Temporal Stereotype, 'Product' has [businesstemporal,processingtemporal]\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class <<temporal.businesstemporal,temporal.processingtemporal>> meta::test::milestoning::domain::Product {\n" +
                        "   id : Integer[1];\n" +
                        "}");

        this.runtime.compile();
    }

    @Test
    public void testNonTemporalTypesCanHaveReservedTemporalProperties()
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class meta::test::milestoning::domain::Product {\n" +
                        "   businessDate : Date[1];\n" +
                        "   processingDate : Date[1];\n" +
                        "   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];\n" +
                        "}");

        this.runtime.compile();
    }

    @Test
    public void testTemporalTypesCanNotHaveReservedTemporalProperties()
    {

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure lines:1c106-5c1), \"Type: Product has temporal specification: [processingDate,businessDate,milestoning] properties:[businessDate,processingDate,milestoning] are reserved and should not be explicit in the Model\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product {\n" +
                        "   businessDate : Date[1];\n" +
                        "   processingDate : Date[1];\n" +
                        "   milestoning : meta::pure::milestoning::BiTemporalMilestoning[1];\n" +
                        "}");

        this.runtime.compile();
    }
}
