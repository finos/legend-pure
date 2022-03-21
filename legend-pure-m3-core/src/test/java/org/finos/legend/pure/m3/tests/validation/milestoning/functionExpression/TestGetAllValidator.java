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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGetAllValidator extends AbstractPureTestWithCoreCompiledPlatform
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
        runtime.delete("domain.pure");
    }

    @Test
    public void testParameterValidationForBusinessTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.all()};\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testParameterValidationForProcessingTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.all()};\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is  [processingtemporal], [processingDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testParameterValidationForBusinessTemporalSubTypeInGetAllWithVariableParam()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::BaseProduct{}\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let date=getDate();\n" +
                        "   let f={|Product.all($date)};\n" +
                        "}\n" +
                        "function getDate():Date[1]\n" +
                        "{\n" +
                        "   %9999-12-31\n" +
                        "}\n");

        runtime.compile();
    }

    @Test
    public void testParameterValidationForNonTemporalTypeInGetAll()
    {
        runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Product\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let f={|Product.all(%2015)};\n" +
                        "}\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:8 column:19), \"The type Product is not Temporal, Dates should not be supplied to all()\"", e.getMessage());
    }

    @Test
    public void testGetAllWithNonTemporalVariableParam()
    {
        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function testGetAllAsFunc<Product>(clazz:Class<Product>[1]):Any[*]\n" +
                        "{\n" +
                        "   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);\n" +
                        "}\n");
    }

    @Test
    public void testGetAllWithTemporalVariableParam()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function testGetAllAsFunc():Any[*]\n" +
                        "{\n" +
                        "   let f=getProduct()->getAll()->deactivate()->cast(@SimpleFunctionExpression);\n" +
                        "}\n" +
                        "function getProduct():Class<Product>[1]\n" +
                        "{\n" +
                        "  ^Product(businessDate=%2015)->cast(@Class<Product>);\n" +
                        "}\n"));
        Assert.assertEquals("Compilation error at (resource:sourceId.pure line:7 column:24), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testGetAllWithTemporalVariableParamWithDateSupplied()
    {

        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function testGetAllAsFunc():Any[*]\n" +
                        "{\n" +
                        "   let f=getProduct()->getAll(%2015)->deactivate()->cast(@SimpleFunctionExpression);\n" +
                        "}\n" +
                        "function getProduct():Class<Product>[1]\n" +
                        "{\n" +
                        "  ^Product(id=1, businessDate=%2015)->cast(@Class<Product>);\n" +
                        "}\n");
    }

    @Test
    public void testGetAllWithGenericVariableParam()
    {

        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;\n" +
                        "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                        "   id : Integer[1];\n" +
                        "}\n" +
                        "function testGetAllAsFunc<T>(clazz:Class<T>[1]):Any[*]\n" +
                        "{\n" +
                        "   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);\n" +
                        "}\n");
    }

    /**
     * bitemporal
     */

    @Test
    public void testBiTemporalGetAllWithCorrectTemporalDateParams() throws Exception
    {
        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   {|meta::relational::tests::milestoning::Location.all(%9999, %2017-5-26)};" +
                "}\n";

        runtime.createInMemorySource("domain.pure", domain);
        runtime.compile();
    }

    @Test
    public void testBiTemporalGetAllWithNoTemporalDateParams() throws Exception
    {
        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location\n" +
                "{\n" +
                "  place : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   {|meta::relational::tests::milestoning::Location.all()};\n" +
                "}\n";

        runtime.createInMemorySource("domain.pure", domain);

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:domain.pure line:7 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }

    @Test
    public void testBiTemporalGetAllWithInsufficientTemporalDateParams() throws Exception
    {
        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location\n" +
                "{\n" +
                "  place : String[1];\n" +
                "}\n" +
                "function go():Any[*]\n" +
                "{\n" +
                "   {|meta::relational::tests::milestoning::Location.all(%9999)};\n" +
                "}\n";

        runtime.createInMemorySource("domain.pure", domain);

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals("Compilation error at (resource:domain.pure line:7 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"", e.getMessage());
    }
}



