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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class TestGetAllValidator extends AbstractPureTestWithCoreCompiledPlatform
{
    @Rule
    public final ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testParameterValidationForBusinessTemporalTypeInGetAll(){

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:4 column:20), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function go():Any[*]\n" +
                "{" +
                "   let f={|Product.all()};" +
                "}" +
                "");

        this.runtime.compile();
    }

    @Test
    public void testParameterValidationForProcessingTemporalTypeInGetAll(){

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:4 column:20), \"The type Product is  [processingtemporal], [processingDate] should be supplied as a parameter to all()\"");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.processingtemporal>> meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function go():Any[*]\n" +
                "{" +
                "   let f={|Product.all()};" +
                "}" +
                "");

        this.runtime.compile();
    }

    @Test
    public void testParameterValidationForBusinessTemporalSubTypeInGetAllWithVariableParam(){

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::BaseProduct{}\n" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product extends BaseProduct{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function go():Any[*]\n" +
                "{" +
                "   let date=getDate();"+
                "   let f={|Product.all($date)};" +
                "}" +
                "function getDate():Date[1]"+
                "{"+
                "   %9999-12-31"+
                "}"+
                "");

        this.runtime.compile();
    }

    @Test
    public void testParameterValidationForNonTemporalTypeInGetAll(){

        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:4 column:20), \"The type Product is not Temporal, Dates should not be supplied to all()");

        this.runtime.createInMemorySource("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function go():Any[*]\n" +
                "{" +
                "   let f={|Product.all(%2015)};" +
                "}" +
                "");

        this.runtime.compile();
    }

    @Test
    public void testGetAllWithNonTemporalVariableParam(){
        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function testGetAllAsFunc<Product>(clazz:Class<Product>[1]):Any[*]\n" +
                "{" +
                "   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);" +
                "}" +
                "");
    }

    @Test
    public void testGetAllWithTemporalVariableParam(){
        expectedEx.expect(PureCompilationException.class);
        expectedEx.expectMessage("Compilation error at (resource:sourceId.pure line:4 column:25), \"The type Product is  [businesstemporal], [businessDate] should be supplied as a parameter to all()\"");

        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function testGetAllAsFunc():Any[*]\n" +
                "{" +
                "   let f=getProduct()->getAll()->deactivate()->cast(@SimpleFunctionExpression);" +
                "}" +
                "function getProduct():Class<Product>[1]\n" +
                "{" +
                "  ^Product(businessDate=%2015)->cast(@Class<Product>); " +
                "}" +
                "");
    }

    @Test
    public void testGetAllWithTemporalVariableParamWithDateSupplied(){

        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function testGetAllAsFunc():Any[*]\n" +
                "{" +
                "   let f=getProduct()->getAll(%2015)->deactivate()->cast(@SimpleFunctionExpression);" +
                "}" +
                "function getProduct():Class<Product>[1]\n" +
                "{" +
                "  ^Product(id=1, businessDate=%2015)->cast(@Class<Product>); " +
                "}" +
                "");
    }


    @Test
    public void testGetAllWithGenericVariableParam(){

        this.compileTestSourceM3("sourceId.pure",
                "import meta::test::milestoning::domain::*;" +
                "Class <<temporal.businesstemporal>> meta::test::milestoning::domain::Product{\n" +
                "   id : Integer[1];\n" +
                "}" +
                "function testGetAllAsFunc<T>(clazz:Class<T>[1]):Any[*]\n" +
                "{" +
                "   let f=$clazz->getAll()->deactivate()->cast(@SimpleFunctionExpression);" +
                "}" +
                "");
    }

    /** bitemporal */

    @Test
    public void testBiTemporalGetAllWithCorrectTemporalDateParams() throws Exception
    {
        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}"+
                "function go():Any[*]\n" +
                "{\n" +
                "   {|meta::relational::tests::milestoning::Location.all(%9999, %2017-5-26)};" +
                "}\n";

        this.runtime.createInMemorySource("domain.pure",domain);
        this.runtime.compile();
    }

    @Test
    public void testBiTemporalGetAllWithNoTemporalDateParams() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:domain.pure line:3 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"");

        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}"+
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   {|meta::relational::tests::milestoning::Location.all()};" +
                        "}\n";

        this.runtime.createInMemorySource("domain.pure",domain);
        this.runtime.compile();
    }

    @Test
    public void testBiTemporalGetAllWithInsufficientTemporalDateParams() throws Exception
    {
        this.expectedEx.expect(PureCompilationException.class);
        this.expectedEx.expectMessage("Compilation error at (resource:domain.pure line:3 column:52), \"The type Location is  [bitemporal], [processingDate,businessDate] should be supplied as a parameter to all()\"");

        String domain = "Class <<temporal.bitemporal>> meta::relational::tests::milestoning::Location{ place : String[1];}"+
                "function go():Any[*]\n" +
                "{\n" +
                "   {|meta::relational::tests::milestoning::Location.all(%9999)};" +
                "}\n";

        this.runtime.createInMemorySource("domain.pure",domain);
        this.runtime.compile();
    }

}



