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

public class TestGraphDSLCompilation extends AbstractPureTestWithCoreCompiled
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
    public void testSimple()
    {
        try
        {
            this.runtime.createInMemorySource("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#{UnknownFirm{employees}}#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:13), \"UnknownFirm has not been defined!\"", e.getMessage());
        }
        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#{Firm{employees1}}#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:18), \"The system can't find a match for the property / qualified property: employees1()\"", e.getMessage());
        }
        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#{Firm{employees{address1}}}#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:28), \"The system can't find a match for the property / qualified property: address1()\"", e.getMessage());
        }

        this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    print(#{Firm{employees{address}}}#,2);\n" +
                "}\n");
        this.runtime.compile();
    }

    @Test
    public void testAdvanced()
    {
        //SubType
        this.runtime.createInMemorySource("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    print(#{Firm{employees{address->subType(@PersonAddress)}, address->subType(@FirmAddress)}}#,2);\n" +
                "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}\n" +
                    "function test():Any[*]\n" +
                    "{\n" +
                    "    print(#{Firm{employees{address->subType(@PersonAddress)}, address->subType(@Person)}}#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:4 column:81), \"The type Person is not compatible with Address\"", e.getMessage());
        }

        //Alias
        this.runtime.modify("file.pure", "Class Person{address:Address[1];} Class Firm {employees:Person[1];address:Address[1];} Class Address{} Class FirmAddress extends Address{} Class PersonAddress extends Address{}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "    print(#{Firm{employees{address->subType(@PersonAddress)}, 'firmAddress' : address->subType(@FirmAddress)}}#,2);\n" +
                "}\n");
        this.runtime.compile();

    }

    @Test
    public void testGraphWithImports()
    {
        try
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
                    "    print(#{\n" +
                    "               Product {\n" +
                    "                   description\n" +
                    "               }\n" +
                    "           }#,2);\n" +
                    "    print(#{\n" +
                    "               Product {\n" +
                    "                   synonymsByType1(ProductSynonymType.CUSIP) {\n" +
                    "                       value\n" +
                    "                   }\n" +
                    "               }\n" +
                    "           }#,2);\n" +
                    "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:file.pure line:30 column:20), \"The system can't find a match for the property / qualified property: synonymsByType1(_:ProductSynonymType[1])\"", e.getMessage());
        }

        this.runtime.modify("file.pure", "import meta::relational::tests::mapping::enumeration::model::domain::*;\n" +
                "Class meta::relational::tests::mapping::enumeration::model::domain::Product\n" +
                "{\n" +
                "   description: String[1];\n" +
                "   synonyms: ProductSynonym[*];\n" +
                "   synonymsByType(type:ProductSynonymType[1])\n" +
                "   {\n" +
                "      $this.synonyms->filter(s | $s.type == $type);\n" +
                "   }:ProductSynonym[*];\n" +
                "   synonymsByTypeString(typeString:String[1])\n" +
                "   {\n" +
                "      $this.synonyms->filter(s | $s.type.name == $typeString)->toOne();\n" +
                "   }:ProductSynonym[1];\n" +
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
                "    print(#{\n" +
                "               Product {\n" +
                "                   description\n" +
                "               }\n" +
                "           }#,2);\n" +
                "    print(#{\n" +
                "               Product {\n" +
                "                   'CUSIP_SYNONYM 1' : synonymsByType(ProductSynonymType.CUSIP) {\n" +
                "                       value\n" +
                "                   },\n" +
                "                   'CUSIP_SYNONYM 2' : synonymsByTypeString('CUSIP') {\n" +
                "                       value\n" +
                "                   }\n" +
                "               }\n" +
                "           }#,2);\n" +
                "}\n");
        this.runtime.compile();
    }


    @Test
    public void testGraphPropertyParameters()
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
                            "    print(#{Person{nameWithTitle()}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithTitle()\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#{Person{nameWithTitle(1)}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithTitle(_:Integer[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#{Person{nameWithTitle('1')}}#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testMultipleParameters()
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
                        "    print(#{Person{nameWithPrefixAndSuffix('a', 'b')}}#,2);\n" +
                        "}\n");
        this.runtime.compile();


        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#{Person{nameWithPrefixAndSuffix('a', ['a', 'b'])}}#,2);\n" +
                        "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix('a', [1, 2])}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Integer[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix('a', [1, 'b'])}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Any[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix('a')}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:3 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#{Person{nameWithPrefixAndSuffix([], ['a', 'b'])}}#,2);\n" +
                        "}\n");
        this.runtime.compile();
    }

    @Test
    public void testVariables()
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
                        "    let var = 'x';\n" +
                        "    print(#{Person{nameWithPrefixAndSuffix($var, 'b')}}#,2);\n" +
                        "}\n");
        this.runtime.compile();


        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#{Person{nameWithPrefixAndSuffix('a', ['a', 'b'])}}#,2);\n" +
                        "}\n");
        this.runtime.compile();

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    let y = 2;\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix('a', [1, $y])}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Integer[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    let y = 'b';\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix('a', [1, $y])}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1],_:Any[2])\"", e.getMessage());
        }

        try
        {
            this.runtime.modify("function.pure",
                    "function test():Any[*]\n" +
                            "{\n" +
                            "    let x = 'a';\n" +
                            "    print(#{Person{nameWithPrefixAndSuffix($x)}}#,2);\n" +
                            "}\n");
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:function.pure line:4 column:20), \"The system can't find a match for the property / qualified property: nameWithPrefixAndSuffix(_:String[1])\"", e.getMessage());
        }

        this.runtime.modify("function.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "    print(#{Person{nameWithPrefixAndSuffix([], ['a', 'b'])}}#,2);\n" +
                        "}\n");
        this.runtime.compile();

    }
}
