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

package org.finos.legend.pure.m2.ds.mapping.test.incremental;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m2.ds.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimeOperationMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("sourceId.pure");
        runtime.delete("userId.pure");
        runtime.delete("modelCode.pure");
        runtime.delete("mappingCode.pure");
    }

    @Test
    public void testSimpleOperation() throws Exception
    {
            RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder()
                                        .createInMemorySource("sourceId.pure", "Class Person{name:String[1];}\n" +
                                                                          "function a():meta::pure::mapping::SetImplementation[*]{[]}\n")
                                        .compile(),
                                new RuntimeTestScriptBuilder()
                                        .createInMemorySource("userId.pure", "" +
                                                                "###Mapping\n" +
                                                                                        "Mapping myMap(\n" +
                                                                                        "   Person[ppp]: Operation\n" +
                                                                "           {\n" +
                                                                "               a__SetImplementation_MANY_()\n" +
                                                                "           }\n" +
                                                                                                ")\n")
                                        .compile()
                                        .deleteSource("userId.pure")
                                        .compile(),
                                this.runtime, this.functionExecution, Lists.fixedSize.<RuntimeVerifier.FunctionExecutionStateVerifier>of());


    }


    @Test
    public void testSimpleOperationReverse() throws Exception
    {
        this.runtime.createInMemorySource("userId.pure", "Class Person{name:String[1];}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   Person[ppp]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    ")\n"
        );
        this.runtime.createInMemorySource("sourceId.pure", "function a():meta::pure::mapping::SetImplementation[*]{[]}\n");
        this.runtime.compile();

        int size = this.repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertEquals("Compilation error at (resource:userId.pure line:6 column:16), \"a__SetImplementation_MANY_ has not been defined!\"", e.getMessage());
            }

            this.runtime.createInMemorySource("sourceId.pure", "function a():meta::pure::mapping::SetImplementation[*]{[]}\n");
            this.runtime.compile();
        }

        Assert.assertEquals(size, this.repository.serialize().length);

    }


    @Test
    public void testSimpleOperationWithParameters() throws Exception
    {
        this.runtime.createInMemorySource("userId.pure", "Class Person{name:String[1];}\n" +
                                                    "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   *Person[op]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_(rel1,rel2)\n" +
                                                    "           }\n" +
                                                    "   Person[rel1]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    "   Person[rel2]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    ")\n"
        );
        this.runtime.compile();
    }


    @Test
    public void testSimpleOperationWithParametersWithError() throws Exception
    {
        this.runtime.createInMemorySource("userId.pure", "Class Person{name:String[1];}\n" +
                                                    "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   Person[op]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_(rel1,rel3)\n" +
                                                    "           }\n" +
                                                    "   Person[rel1]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    "   Person[rel2]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    ")\n"
        );
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            Assert.assertEquals("Compilation error at (resource:userId.pure lines:5c4-8c12), \"The SetImplementation 'rel3' can't be found in the mapping 'myMap'\"", e.getMessage());
        }
    }


    @Test
    public void testSimpleOperationWithInclude() throws Exception
    {
        this.runtime.createInMemorySource("userId.pure", "Class Person{name:String[1];}\n" +
                                                    "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMapToInclude(\n" +
                                                    "   *Person[rel1]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    "   Person[rel2]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_()\n" +
                                                    "           }\n" +
                                                    ")\n" +
                                                    "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   include myMapToInclude" +
                                                    "   Person[op]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_(rel1,rel2)\n" +
                                                    "           }\n" +
                                                    ")\n"
        );
        this.runtime.compile();
    }

    @Test
    public void testSimpleOperationWithIncludeDelta() throws Exception
    {
        this.runtime.createInMemorySource("userId.pure", "Class Person{name:String[1];}\n" +
                                                    "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" + "###Mapping\n" +
                                                    "Mapping myMap(\n" +
                                                    "   include myMapToInclude" +
                                                    "   Person[op]: Operation\n" +
                                                    "           {\n" +
                                                    "               a__SetImplementation_MANY_(rel1,rel2)\n" +
                                                    "           }\n" +
                                                    ")\n");
        String content = "###Mapping\n" +
                         "Mapping myMapToInclude(\n" +
                         "   *Person[rel1]: Operation\n" +
                         "           {\n" +
                         "               a__SetImplementation_MANY_()\n" +
                         "           }\n" +
                         "   Person[rel2]: Operation\n" +
                         "           {\n" +
                         "               a__SetImplementation_MANY_()\n" +
                         "           }\n" +
                         ")\n";
        this.runtime.createInMemorySource("sourceId.pure", content);
        this.runtime.compile();

        int size = this.repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete("sourceId.pure");
            try
            {
                this.runtime.compile();
                Assert.fail();
            }
            catch (Exception e)
            {
                Assert.assertEquals("Compilation error at (resource:userId.pure line:5 column:12), \"myMapToInclude has not been defined!\"", e.getMessage());
            }

            this.runtime.createInMemorySource("sourceId.pure", content);
            this.runtime.compile();
        }

        Assert.assertEquals(size, this.repository.serialize().length);

    }

}
