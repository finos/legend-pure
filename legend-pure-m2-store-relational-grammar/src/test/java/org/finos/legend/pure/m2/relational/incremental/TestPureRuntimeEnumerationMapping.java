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

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m2.relational.AbstractPureRelationalTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.Test;

import java.util.List;

public class TestPureRuntimeEnumerationMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    private static final String TEST_ENUM_MODEL_SOURCE_ID = "testModel.pure";
    private static final String RELATIONAL_DB_SOURCE_ID = "testDb.pure";
    private static final String TEST_ENUMERATION_MAPPING_SOURCE_ID = "testMapping.pure";

    private static final String FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE = "###Pure\n" +
            "function test():Boolean[1]{assert(1 == employeeTestMapping.enumerationMappings->size(), |'');}";

    private static final ImmutableMap<String, String> TEST_SOURCES = Maps.immutable.with(TEST_ENUM_MODEL_SOURCE_ID,
            "Class Employee\n" +
                    "{\n" +
                    "    id: Integer[1];\n" +
                    "    name: String[1];\n" +
                    "    dateOfHire: Date[1];\n" +
                    "    type: EmployeeType[0..1];\n" +
                    "}\n" +
                    "\n" +
                    "Enum EmployeeType\n" +
                    "{\n" +
                    "    CONTRACT,\n" +
                    "    FULL_TIME\n" +
                    "}",
            RELATIONAL_DB_SOURCE_ID, "###Relational\n" +
                    "\n" +
                    "Database myDB\n" +
                    "(\n" +
                    "    Table employeeTable\n" +
                    "    (\n" +
                    "        id INT,\n" +
                    "        name VARCHAR(200),\n" +
                    "        firmId INT,\n" +
                    "        doh DATE,\n" +
                    "        type VARCHAR(20)\n" +
                    "    )\n" +
                    ")\n",
            TEST_ENUMERATION_MAPPING_SOURCE_ID,
            "###Mapping\n" +
                    "\n" +
                    "Mapping employeeTestMapping\n" +
                    "(\n" +
                    "\n" +
                    "    EmployeeType: EnumerationMapping Foo\n" +
                    "    {\n" +
                    "    /* comment */\n" +
                    "        CONTRACT:  ['FTC', 'FTO'],\n" +
                    "        FULL_TIME: 'FTE'\n" +
                    "    }\n" +
                    "   Employee: Relational\n" +
                    "   {\n" +
                    "        scope([myDB]default.employeeTable)\n" +
                    "        (\n" +
                    "            id: id,\n" +
                    "            name: name,\n" +
                    "            dateOfHire: doh,\n" +
                    "            type : EnumerationMapping Foo : type\n" +
                    "        )\n" +
                    "   }\n" +
                    ")\n"
    );

    @Test
    public void testDeleteAndReloadEachSource() throws Exception
    {
        this.testDeleteAndReloadEachSource(
                TEST_SOURCES, FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadEachSource(ImmutableMap<String, String> sources, String testFunctionSource)
    {

        for (Pair<String, String> source : sources.keyValuesView())
        {
            RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(sources)
                            .createInMemorySource("functionSourceId.pure", testFunctionSource)
                            .compile(),
                    new RuntimeTestScriptBuilder()
                            .deleteSource(source.getOne())
                            .compileWithExpectedCompileFailure(null, null, 0, 0)
                            .createInMemorySource(source.getOne(), source.getTwo())
                            .compile(),
                    this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());

            //reset
            this.setUpRuntime();
        }

    }

    @Test
    public void testDeleteAndReloadSourcePairs() throws Exception
    {
        this.testDeleteAndReloadTwoSources(
                TEST_SOURCES, FUNCTION_TEST_ENUMERATION_MAPPINGS_SIZE);
    }

    public void testDeleteAndReloadTwoSources(ImmutableMap<String, String> sources,
                                              String testFunctionSource)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            List<Pair<String, String>> sourcesClone = sources.keyValuesView().toList();
            sourcesClone.remove(source);

            for (Pair<String, String> secondSource : sourcesClone)
            {
                RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySources(sources)
                                .createInMemorySource("functionSourceId.pure", testFunctionSource)
                                .compile(),
                        new RuntimeTestScriptBuilder()
                                .deleteSources(Lists.fixedSize.of(source.getOne(), secondSource.getOne()))
                                .compileWithExpectedCompileFailure(null, null, 0, 0)
                                .createInMemorySource(source.getOne(), source.getTwo())
                                .createInMemorySource(secondSource.getOne(), secondSource.getTwo())
                                .compile(),
                        this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());

                //reset so that the next iteration has a clean environment
                this.setUpRuntime();
            }
        }

    }

}

