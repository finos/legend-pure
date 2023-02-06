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

package org.finos.legend.pure.m2.relational.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

public class TestPureRuntimeExtendMapping extends AbstractPureRelationalTestWithCoreCompiled
{

    private static final String INITIAL_DATA = "import other::*;\n" +
            "Class other::Person\n" +
            "{\n" +
            "    name:String[1];\n" +
            "    otherInfo:String[1];\n" +
            "}\n"  ;


    private static final String STORE =
            "###Relational\n" +
                    "Database mapping::db(\n" +
                    "   Table employeeTable\n" +
                    "   (\n" +
                    "    id INT PRIMARY KEY,\n" +
                    "    name VARCHAR(200),\n" +
                    "    firmId INT,\n" +
                    "    other VARCHAR(200),\n" +
                    "    address1 VARCHAR(200),\n" +
                    "    postcode VARCHAR(10)\n" +
                    "   )\n" +
                    ")\n" ;


    private static final String INITIAL_MAPPING =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    *Person[person1]: Relational\n" +
                    "    {\n" +
                    "       otherInfo: [db]employeeTable.other\n" +
                    "    }\n" +
                    "    Person[alias1] extends [person1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeTable.name\n" +
                    "    }\n" +
                    ")\n";

    private static final String MAPPING1 =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    *Person[person1]: Relational\n" +
                    "    {\n" +
                    "       otherInfo: [db]employeeTable.other\n" +
                    "    }\n" +
                    "    Person[alias1] extends [person2] : Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeTable.name\n" +
                    "    }\n" +
                    ")\n";

    private static final String CHANGE_SUPER__MAPPING =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    *Person[person2]: Relational\n" +
                    "    {\n" +
                    "       otherInfo: [db]employeeTable.other\n" +
                    "    }\n" +
                    "    Person[alias1] extends [person1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeTable.name\n" +
                    "    }\n" +
                    ")\n";
    private static final String DELETE_SUPER__MAPPING =
            "###Mapping\n" +
                    "import other::*;\n" +
                    "import mapping::*;\n" +
                    "Mapping mappingPackage::myMapping\n" +
                    "(\n" +
                    "    Person[alias1] extends [person1]: Relational\n" +
                    "    {\n" +
                    "        name : [db]employeeTable.name\n" +
                    "    }\n" +
                    ")\n";

    @Test
    public void testChangeMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", MAPPING1)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 10, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }

    @Test
    public void testChangeExtendMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", CHANGE_SUPER__MAPPING)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 10, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }
    @Test
    public void testDeleteExtendMapping() throws Exception
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(
                Maps.mutable.with("source1.pure", INITIAL_DATA, "source3.pure", STORE, "source4.pure", INITIAL_MAPPING))
                        .compile(),
                new RuntimeTestScriptBuilder()
                        .updateSource("source4.pure", DELETE_SUPER__MAPPING)
                        .compileWithExpectedCompileFailure("Invalid superMapping for mapping [alias1]", "source4.pure", 6, 5)
                        .updateSource("source4.pure", INITIAL_MAPPING)
                        .compile()
                , this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());
    }





}
