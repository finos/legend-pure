// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

import org.junit.Assert;
import org.junit.Test;

public class TestXStoreMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testXStoreMapping()
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Relational\n" +
                        "   {\n" +
                        "      +id:String[1] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Relational\n" +
                        "   {\n" +
                        "      +firmId:String[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm[e, f1] : $this.firmId == $that.id,\n" +
                        "      employees[f1, e] : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Relational\n" +
                        "   {\n" +
                        "      +id:String[1] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Relational\n" +
                        "   {\n" +
                        "      +firmId:Strixng[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm[e, f1] : $this.firmId == $that.id,\n" +
                        "      employees[f1, e] : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Parser error at (resource:mapping.pure line:14), (Not Found: Strixng) in\n" +
                    "'\n" +
                    "Mapping FirmMapping\n" +
                    "(\n" +
                    "   Firm[f1] : Relational\n" +
                    "   {\n" +
                    "      +id:String[1] : [db]FirmTable.id,\n" +
                    "      legalName : [db]FirmTable.legal_name\n" +
                    "   }\n" +
                    "   \n" +
                    "   Person[e] : Relational\n" +
                    "   {\n" +
                    "      +firmId:Strixng[1] : [db]PersonTable.firmId,\n" +
                    "      lastName : [db]PersonTable.lastName\n" +
                    "   }\n" +
                    "   \n" +
                    "   Firm_Person : XStore\n" +
                    "   {\n" +
                    "      firm[e, f1] : $this.firmId == $that.id,\n" +
                    "      employees[f1, e] : $this.id == $that.firmId\n" +
                    "   }\n" +
                    ")'", e.getMessage());
        }
    }

    @Test
    public void testXStoreMappingDiffMul()
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm[f1] : Relational\n" +
                        "   {\n" +
                        "      +id:String[*] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person[e] : Relational\n" +
                        "   {\n" +
                        "      +firmId:String[0..1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm[e, f1] : $this.firmId == $that.id->toOne(),\n" +
                        "      employees[f1, e] : $this.id->toOne() == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNotSetId()
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      +id:Integer[1] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person : Relational\n" +
                        "   {\n" +
                        "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm : $this.firmId == $that.id,\n" +
                        "      employees : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNaturalProperty()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "Class Firm\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      id : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person : Relational\n" +
                        "   {\n" +
                        "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm : $this.firmId == $that.id,\n" +
                        "      employees : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
    }

    @Test
    public void testXStoreMappingNaturalPropertyError()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "Class Firm\n" +
                        "{\n" +
                        "   id : Integer[1];\n" +
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      id : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person : Relational\n" +
                        "   {\n" +
                        "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm : $this.firmId == $that.ixd,\n" +
                        "      employees : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:mapping.pure line:34 column:36), \"Can't find the property 'ixd' in the class Firm\"", e.getMessage());
        }

    }

    @Test
    public void testXStoreMappingNaturalPropertyUsingInheritance()
    {
        this.runtime.createInMemorySource("mapping.pure",
                "Class SuperFirm" +
                        "{" +
                        "   id : Integer[1];\n" +
                        "}" +
                        "Class Firm extends SuperFirm\n" +
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
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      id : [db]FirmTable.id,\n" +
                        "      +xid:Integer[1] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person : Relational\n" +
                        "   {\n" +
                        "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm : $this.firmId == $that.id,\n" +
                        "      employees : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
    }


    @Test
    public void testXStoreMappingToMilestonedType()
    {
        this.runtime.createInMemorySource("mapping.pure",
                        "Class Firm\n" +
                        "{\n" +
                        "   legalName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>>Person\n" +
                        "{\n" +
                        "   lastName : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Association Firm_Person\n" +
                        "{\n" +
                        "   firm : Firm[1];\n" +
                        "   employees : Person[*];\n" +
                        "}\n" +
                        "###Mapping\n" +
                        "Mapping FirmMapping\n" +
                        "(\n" +
                        "   Firm : Relational\n" +
                        "   {\n" +
                        "      +id:Integer[1] : [db]FirmTable.id,\n" +
                        "      legalName : [db]FirmTable.legal_name\n" +
                        "   }\n" +
                        "   \n" +
                        "   Person : Relational\n" +
                        "   {\n" +
                        "      +firmId:Integer[1] : [db]PersonTable.firmId,\n" +
                        "      lastName : [db]PersonTable.lastName\n" +
                        "   }\n" +
                        "   \n" +
                        "   Firm_Person : XStore\n" +
                        "   {\n" +
                        "      firm : $this.firmId == $that.id,\n" +
                        "      employees : $this.id == $that.firmId\n" +
                        "   }\n" +
                        ")\n" +
                        "###Relational\n" +
                        "Database db\n" +
                        "(\n" +
                        "   Table FirmTable (id INTEGER, legal_name VARCHAR(200))\n" +
                        "   Table PersonTable (firmId INTEGER, lastName VARCHAR(200))\n" +
                        ")");
        this.runtime.compile();
    }
}
