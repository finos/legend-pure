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

package org.finos.legend.pure.m2.inlinedsl.graph;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMilestonedPropertyUsageInGraph extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
    }

    @Test
    public void testGeneratedQualifiedPropertyUsage() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", "import meta::test::milestoning::domain::*;\n" +
                    "Class meta::test::milestoning::domain::Product{\n" +
                    "   classification : Classification[1];\n" +
                    "}\n" +
                    "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                    "   classificationType : String[1];\n" +
                    "}\n" +
                    "function go():Any[*]\n" +
                    "{\n" +
                    "   print(#{Product{classification{classificationType}}}#)" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:10 column:20), \"The system can't find a match for the property / qualified property: classification(). No-Arg milestoned property: 'classification' is not supported yet in graph fetch flow! It needs to be supplied with [businessDate] parameters\"", e.getMessage());
        }

        this.runtime.modify("file.pure", "import meta::test::milestoning::domain::*;\n" +
                "Class meta::test::milestoning::domain::Product{\n" +
                "   classification : Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                "   classificationType : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   print(#{Product{classification(%latest){classificationType}}}#, 1)" +
                "}\n");
        this.runtime.compile();
    }

    @Test
    public void testQualifiedPropertyInference() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", "import meta::test::milestoning::domain::*;\n" +
                    "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                    "   classification : Classification[1];\n" +
                    "}\n" +
                    "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                    "   classificationType : String[1];\n" +
                    "}\n" +
                    "function go():Any[*]\n" +
                    "{\n" +
                    "   print(#{Product{classification{classificationType}}}#)" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:10 column:20), \"No-Arg milestoned property: 'classification' is not supported yet in graph fetch flow! It needs to be supplied with [businessDate] parameters\"", e.getMessage());
        }

        this.runtime.modify("file.pure", "import meta::test::milestoning::domain::*;\n" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                "   classification : Classification[1];\n" +
                "}\n" +
                "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                "   classificationType : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   print(#{Product{classification(%2015-01-01){classificationType}}}#, 1)" +
                "}\n");
        this.runtime.compile();
    }
}
