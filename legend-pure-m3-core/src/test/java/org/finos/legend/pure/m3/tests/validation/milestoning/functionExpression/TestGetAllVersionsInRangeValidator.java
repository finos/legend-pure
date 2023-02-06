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

package org.finos.legend.pure.m3.tests.validation.milestoning.functionExpression;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestGetAllVersionsInRangeValidator extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("source.pure");
        runtime.delete("test.pure");
    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testAllVersionsInRangeForBusinessTemporal()
    {
        this.runtime.createInMemorySource("source.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }

    @Test
    public void testAllVersionsInRangeForProcessingTemporal()
    {
        this.runtime.createInMemorySource("source.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }

    @Test
    public void testAllVersionsInRangeForBiTemporal()
    {
        this.expectedException.expect(PureCompilationException.class);

        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:7 column:19), \".allVersionsInRange() is applicable only for businessTemporal and processingTemporal types");
        this.runtime.createInMemorySource("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.bitemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }

    @Test
    public void testAllVersionsInRangeForNonTemporal()
    {
        this.expectedException.expect(PureCompilationException.class);

        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:7 column:19), \".allVersionsInRange() is applicable only for businessTemporal and processingTemporal types");
        this.runtime.createInMemorySource("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %2018-1-9)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }

    @Test
    public void testLatestDateUsageForAllVersionsInRangeForBusinessTemporal()
    {
        this.runtime.createInMemorySource("source.pure",
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.createInMemorySource("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%latest, %2018-1-1)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.modify("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %latest)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.modify("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%latest, %latest)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }

    @Test
    public void testLatestDateUsageForAllVersionsInRangeForProcessingTemporal()
    {
        this.runtime.createInMemorySource("source.pure",
                "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.createInMemorySource("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%latest, %2018-1-1)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.modify("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%2018-1-1, %latest)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();

        this.expectedException.expect(PureCompilationException.class);
        this.expectedException.expectMessage("Compilation error at (resource:test.pure line:4 column:19), \"%latest not a valid parameter for .allVersionsInRange()");
        this.runtime.modify("test.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.allVersionsInRange(%latest, %latest)};\n" +
                        "}\n" +
                        "\n");
        this.runtime.compile();
    }
}
