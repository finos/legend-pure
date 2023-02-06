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
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestMilestonedPropertyUsageInPaths extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
    }

    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testProcessingErrorWhenMilestoningContextNotAvailableToNoArgQualifiedPropertyFromRootInPath() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:10 column:20), \"No-Arg milestoned property: 'classification' must be either called in a milestoning context or supplied with [businessDate] parameters");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "}\n" +
                        "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   classificationType : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   print(#/Product/classification/classificationType#)" +
                        "}\n" +
                        "");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateThroughEdgePointPropertyToNoArgMilestonedPropertyInPath() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:13 column:46), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "}\n" +
                        "Class  <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   exchange : Exchange[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{\n" +
                        "   exchangeName : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   print(#/Product/classificationAllVersions/exchange/exchangeName#)" +
                        "}\n" +
                        "");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningContextNotAllowedToPropagateThroughNonMilestonedPropertyToNoArgMilestonedPropertyInPath() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:13 column:35), \"No-Arg milestoned property: 'exchange' must be either called in a milestoning context or supplied with [businessDate] parameters");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "}\n" +
                        "Class  meta::test::milestoning::domain::Classification{\n" +
                        "   exchange : Exchange[1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Exchange{\n" +
                        "   exchangeName : String[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   print(#/Product/classification/exchange/exchangeName#)" +
                        "}\n" +
                        "");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughProjectToNoArgMilestonedPropertyInLambdaPath() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];\n"+
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   classificationName : String[0..1];\n" +
                        "}\n"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "let date=%2015;\n"+
                        "  {|Product.all($date)->project([#/Product/classification/classificationName#])};\n"+
                        "}\n"+
                        "function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]\n" +
                        "{\n" +
                        " []"  +
                        "}\n");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectToNoArgMilestonedPropertyInLambdaPath() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];\n"+
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   classificationName : String[0..1];\n" +
                        "}\n"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  {|Product.all(%2015)->filter(p|!$p->isEmpty())->project([#/Product/classification/classificationName#])}\n"+
                        "}\n"+
                        "function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]\n" +
                        "{\n" +
                        " []"  +
                        "}\n");

        this.runtime.compile();
    }

    @Test
    public void testNoProcessingErrorWhenMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectAndOverridenInMilestonedPropertyWithDateParamInOneOfManyLambdaPaths() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];\n"+
                        "   nonTemporalClassification : NonTemporalClassification[0..1];\n" +
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   classificationName : String[0..1];\n" +
                        "   system : ClassificationSystem[0..1];\n" +
                        "}\n"+
                        "Class meta::test::milestoning::domain::NonTemporalClassification{\n" +
                        "   classificationName : String[0..1];\n" +
                        "}\n"+
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::ClassificationSystem{\n" +
                        "   systemName : String[0..1];\n" +
                        "}\n"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  {|Product.all(%2015)->filter(p|!$p->isEmpty())->project([#/Product/nonTemporalClassification/classificationName#,#/Product/classification(%2016-1-1)/system/systemName#])}\n"+
                        "}\n"+
                        "function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]\n" +
                        "{\n" +
                        " []"  +
                        "}\n");

        this.runtime.compile();
    }

    @Test
    public void testMilestoningContextAllowedToPropagateFromAllThroughFilterAndProjectToNoArgMilestonedPropertyInOneOfManyLambdaPaths() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   classification : Classification[1];\n" +
                        "   name: String[0..1];\n"+
                        "   classificationDp(bd:Date[1]){^Classification(businessDate=$bd)}:Classification[0..1];\n"+
                        "}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Classification{\n" +
                        "   classificationName : String[0..1];\n" +
                        "}\n"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "  {|Product.all(%2015)->project([#/Product/name#,#/Product/classification/classificationName#])}\n"+
                        "}\n"+
                        "function project<K>(set:K[*], functions:Function<{K[1]->Any[*]}>[*]):Any[0..1]\n" +
                        "{\n" +
                        " []"  +
                        "}\n");

        this.runtime.compile();
    }
}
