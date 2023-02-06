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

package org.finos.legend.pure.m2.inlinedsl.path.milestoning;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMilestonedPropertyUsageInFunctonExpressionsWithPath extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @Test
    public void testLatestDateCompilationValidationAndPropagationDate() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   name:String[1];\n"+
                        "}\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Trader{\n" +
                        "   name:String[1];\n"+
                        "   coveredProducts:Product[*];\n"+
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   name:String[1];\n"+
                        "   classification : Classification[0..1];\n"+
                        "}\n" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::BiTemporalProduct{\n" +
                        "   name:String[1];\n"+
                        "   classification : Classification[0..1];\n"+
                        "}\n" +
                        "Class  <<temporal.processingtemporal>> meta::test::milestoning::domain::Order{\n" +
                        "   product : Product[0..1];\n" +
                        "   biTemporalProduct : BiTemporalProduct[0..1];\n" +
                        "   trader : Trader[0..1];\n" +
                        "}\n"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|Order.all(%latest).product(%latest).classification(%latest).name};" +
                        "   {|Order.all(%latest).biTemporalProduct(%latest, %latest).name};" +
                        "   {|Order.all(%latest).trader.coveredProducts(%latest).name};" +
                        "   {|BiTemporalProduct.all(%latest, %latest).name};" +
                        "   {|#/Order/biTemporalProduct(%latest, %latest)/name#};"+
                        "}\n"
        );

        this.runtime.compile();
    }

}
