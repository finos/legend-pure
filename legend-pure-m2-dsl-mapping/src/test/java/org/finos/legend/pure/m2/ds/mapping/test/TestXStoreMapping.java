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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestXStoreMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("mapping.pure");
    }

    @Test
    public void testXStoreMapping()
    {
        String source = "Class Firm\n" +
                "{\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class Person\n" +
                "{\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "\n" +
                "Association Firm_Person\n" +
                "{\n" +
                "   firm : Firm[1];\n" +
                "   employees : Person[*];\n" +
                "}\n" +
                "Class SrcFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class SrcPerson\n" +
                "{\n" +
                "   _lastName : String[1];\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping FirmMapping\n" +
                "(\n" +
                "   Firm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcFirm " +
                "      +id:String[1] : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   \n" +
                "   Person[e] : Pure\n" +
                "   {\n" +
                "      ~src SrcPerson" +
                "      +firmId:String[1] : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   \n" +
                "   Firm_Person : XStore\n" +
                "   {\n" +
                "      firm[e, f1] : $this.firmId == $that.id,\n" +
                "      employees[f1, e] : $this.id == $that.firmId\n" +
                "   }\n" +
                ")\n";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_Person");
    }

    @Test
    public void testXStoreMappingTypeError()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "Class Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class Person\n" +
                        "{\n" +
                        "   lastName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association Firm_Person\n" +
                        "{\n" +
                        "   firm : Firm[1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n" +
                        "Class SrcFirm\n" +
                        "{\n" +
                        "   _id : String[1];\n" +
                        "   _legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class SrcPerson\n" +
                        "{\n" +
                        "   _lastName : String[1];\n" +
                        "   _firmId : String[1];\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcFirm " +
                        "      +id:String[1] : $src._id,\n" +
                        "      legalName : $src._legalName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Pure\n" +
                        "   {\n" +
                        "      ~src SrcPerson" +
                        "      +firmId:Strixng[1] : $src._firmId,\n" +
                        "      lastName : $src._lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm[e, f1] : $this.firmId == $that.id,\n" +
                        "      employees[f1, e] : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:38 column:35), \"Strixng has not been defined!\"", e.getMessage());
        }
    }

    @Test
    public void testXStoreMappingDiffMul()
    {
        String source = "Class Firm\n" +
                "{\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class Person\n" +
                "{\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "\n" +
                "Association Firm_Person\n" +
                "{\n" +
                "   firm : Firm[1];\n" +
                "   employees : Person[*];\n" +
                "}\n" +
                "Class SrcFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class SrcPerson\n" +
                "{\n" +
                "   _lastName : String[1];\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping FirmMapping\n" +
                "(\n" +
                "   Firm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcFirm " +
                "      +id:String[*] : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   \n" +
                "   Person[e] : Pure\n" +
                "   {\n" +
                "      ~src SrcPerson" +
                "      +firmId:String[0..1] : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   \n" +
                "   Firm_Person : XStore\n" +
                "   {\n" +
                "      firm[e, f1] : $this.firmId == $that.id->toOne(),\n" +
                "      employees[f1, e] : $this.id->toOne() == $that.firmId\n" +
                "   }\n" +
                ")\n";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_Person");
    }


    @Test
    public void testXStoreAssociationSubtypeMapping()
    {
        String source = "Class Firm\n" +
                "{\n" +
                "   legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class Person\n" +
                "{\n" +
                "   lastName : String[1];\n" +
                "}\n" +
                "Class MyPerson extends Person\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "Association Firm_MyPerson\n" +
                "{\n" +
                "   firm : Firm[1];\n" +
                "   employees : MyPerson[*];\n" +
                "}\n" +
                "Class SrcFirm\n" +
                "{\n" +
                "   _id : String[1];\n" +
                "   _legalName : String[1];\n" +
                "}\n" +
                "\n" +
                "Class SrcPerson\n" +
                "{\n" +
                "   _lastName : String[1];\n" +
                "   _firmId : String[1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping FirmMapping\n" +
                "(\n" +
                "   Firm[f1] : Pure\n" +
                "   {\n" +
                "      ~src SrcFirm " +
                "      +id:String[*] : $src._id,\n" +
                "      legalName : $src._legalName\n" +
                "   }\n" +
                "   \n" +
                "   Person : Pure\n" +
                "   {\n" +
                "      ~src SrcPerson" +
                "      +firmId:String[0..1] : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   \n" +
                "   MyPerson : Pure\n" +
                "   {\n" +
                "      ~src SrcPerson" +
                "      +firmId:String[0..1] : $src._firmId,\n" +
                "      lastName : $src._lastName\n" +
                "   }\n" +
                "   \n" +
                "   Firm_MyPerson : XStore\n" +
                "   {\n" +
                "      firm[MyPerson, f1] : $this.firmId == $that.id->toOne(),\n" +
                "      employees[f1, MyPerson] : $this.id->toOne() == $that.firmId\n" +
                "   }\n" +
                ")\n";
        this.runtime.createInMemorySource("mapping.pure", source);
        this.runtime.compile();
        assertSetSourceInformation(source, "Firm");
        assertSetSourceInformation(source, "Person");
        assertSetSourceInformation(source, "Firm_MyPerson");
    }
}
