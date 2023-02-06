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

package org.finos.legend.pure.m2.inlinedsl.path;


import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDSLCompilation extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("file.pure");
        runtime.delete("function.pure");
    }

    @Test
    public void testSimple() throws Exception
    {

        try
        {
            this.runtime.createInMemorySource("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#/UnknownFirm/employees/address#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:12), \"UnknownFirm has not been defined!\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#/Firm/employees/address#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:12), \"Type argument mismatch for the class Firm<T> (expected 1, got 0): Firm\"", e.getMessage());
        }


        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#/Firm<BlaBla>/employees/address#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:18), \"BlaBla has not been defined!\"", e.getMessage());
        }


        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#/Firm<Any>/employee/address#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:23), \"The property 'employee' can't be found in the type 'Firm' (or any supertype).\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm<T> {employees : Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#/Firm/employees/address2#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:28), \"The property 'address2' can't be found in the type 'Person' (or any supertype).\"", e.getMessage());
        }

    }

    @Test
    public void testPathWithImports() throws Exception
    {
        this.runtime.createInMemorySource("file.pure", "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::Product\n" +
                "{\n" +
                "   description: String[1];\n" +
                "   synonyms: ProductSynonym[*];\n" +
                "   synonymsByType(type:ProductSynonymType[1])\n" +
                "   {\n" +
                "      $this.synonyms->filter(s | $s.type == $type);\n" +
                "   }:ProductSynonym[*];\n" +
                "}\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::ProductSynonym\n" +
                "{\n" +
                "   type:ProductSynonymType[1];\n" +
                "   value:String[1];\n" +
                "}\n" +
                "Enum meta::relational::tests::mapping::enumeration::model::domain::ProductSynonymType\n" +
                "{\n" +
                "   CUSIP,\n" +
                "   GS_NUMBER\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    print(#/Product/description#,2);\n" +
                "    print(#/Product/synonymsByType(ProductSynonymType.CUSIP)/value!cusip#,2);\n" +
                "}\n");
        this.runtime.compile();
    }


    @Test
    public void testParameters() throws Exception
    {

        this.runtime.createInMemorySource("file.pure",
                "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "}\n");

        try
        {
            this.runtime.createInMemorySource("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/Person/nameWithTitle()#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Error finding match for function 'nameWithTitle'. Incorrect number of parameters, function expects 1 parameters\"", e.getMessage());
        }


        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/Person/nameWithTitle(1)#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithTitle'. Expected:String, Found:Integer\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#/Person/nameWithTitle('1')#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testMultipleParameters() throws Exception
    {

        this.runtime.createInMemorySource("file.pure",
                "Class Person\n" +
                        "{\n" +
                        "    firstName : String[1];\n" +
                        "    lastName : String[1];\n" +
                        "    nameWithTitle(title:String[1]){$title+' '+$this.firstName+' '+$this.lastName}:String[1];" +
                        "    nameWithPrefixAndSuffix(prefix:String[0..1], suffixes:String[*])\n" +
                        "    {\n" +
                        "        if($prefix->isEmpty(),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')),\n" +
                        "           | if($suffixes->isEmpty(),\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName,\n" +
                        "                | $prefix->toOne() + ' ' + $this.firstName + ' ' + $this.lastName + ', ' + $suffixes->joinStrings(', ')))\n" +
                        "    }:String[1];" +
                        "    memberOf(org:Organization[1]){true}:Boolean[1];" +
                        "}\n" +
                        "Class Organization\n" +
                        "{\n" +
                        "}" +
                        "Class Team extends Organization\n" +
                        "{\n" +
                        "}");

        this.runtime.createInMemorySource("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#/Person/nameWithPrefixAndSuffix('a', 'b')#,2);\n" +
                        "}\n");
        this.runtime.compile();


        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#/Person/nameWithPrefixAndSuffix('a', ['a', 'b'])#,2);\n" +
                        "}\n");
        this.runtime.compile();

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#/Person/nameWithPrefixAndSuffix([], ['a', 'b'])#,2);\n" +
                        "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/Person/nameWithPrefixAndSuffix('a', [1, 2])#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Integer\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/Person/nameWithPrefixAndSuffix('a', [1, 'b'])#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Parameter type mismatch for function 'nameWithPrefixAndSuffix'. Expected:String, Found:Any\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/Person/nameWithPrefixAndSuffix('a')#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:12), \"Error finding match for function 'nameWithPrefixAndSuffix'. Incorrect number of parameters, function expects 2 parameters\"", e.getMessage());
        }
    }

    @Test
    public void testVisibility() throws Exception
    {
        try
        {
            this.runtime.createInMemorySource("file.pure",
                    "Class <<access.private>> a::Person\n" +
                            "{\n" +
                            "    firstName : String[1];\n" +
                            "}\n");

            this.runtime.createInMemorySource("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#/a::Person/firstName#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:16), \"a::Person is not accessible in Root\"", e.getMessage());
        }
    }

    @Test
    public void testMapReturn() throws Exception
    {
        this.runtime.createInMemorySource("file.pure",
                "Class <<access.private>> Person\n" +
                        "{\n" +
                        "    firstName : String[1];" +
                        "    stuff : Map<String, Integer>[1];\n" +
                        "}\n");

        this.runtime.createInMemorySource("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#/Person/stuff#,2);\n" +
                        "}\n");
        this.runtime.compile();

    }
}
