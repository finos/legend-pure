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

import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRoot extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("userId.pure");
    }

    @Test
    public void testRoot()
    {
        String source = "Class Person{name:String[1];}\n" +
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
                ")\n";
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testRootError() throws Exception
    {
        runtime.createInMemorySource("userId.pure",
                "Class Person{name:String[1];}\n" +
                        "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                        "###Mapping\n" +
                        "Mapping myMap(\n" +
                        "   *Person[op]: Operation\n" +
                        "           {\n" +
                        "               a__SetImplementation_MANY_(rel1,rel2)\n" +
                        "           }\n" +
                        "   *Person[rel1]: Operation\n" +
                        "           {\n" +
                        "               a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        "   Person[rel2]: Operation\n" +
                        "           {\n" +
                        "               a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        ")\n"
        );
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "The class 'Person' is mapped by 3 set implementations and has 2 roots. There should be exactly one root set implementation for the class, and it should be marked with a '*'.", "userId.pure", 4, 9, e);
    }


    @Test
    public void testRootWithInclude()
    {
        String source = "Class Person{name:String[1];}\n" +
                "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                "###Mapping\n" +
                "Mapping myMap1(\n" +
                "   *Person[one]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                "   Person[two]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping myMap2(\n" +
                "   *Person[one_1]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                "   Person[two_1]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                ")\n" +
                "Mapping includeMap(\n" +
                "   include myMap1" +
                "   include myMap2" +
                ")\n";
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testRootWithIncludeDuplicate()
    {
        String source = "Class Person{name:String[1];}\n" +
                "Enum OK {e_true,e_false}\n" +
                "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                "###Mapping\n" +
                "Mapping myMap1(" +
                "   OK: EnumerationMapping Foo\n" +
                "   {\n" +
                "        e_true:  ['FTC', 'FTO'],\n" +
                "        e_false: 'FTE'\n" +
                "   }\n" +
                "   *Person[one]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                "   Person[two]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                ")\n" +
                "###Mapping\n" +
                "Mapping myMap2(\n" +
                "   *Person[one_1]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                "   Person[two_1]: Operation\n" +
                "           {\n" +
                "               a__SetImplementation_MANY_()\n" +
                "           }\n" +
                ")\n" +
                "Mapping myMap3\n" +
                "(\n" +
                "   include myMap1\n" +
                ")\n" +
                "Mapping includeMap(\n" +
                "   include myMap1\n" +
                "   include myMap2\n" +
                "   include myMap3\n" +
                ")\n";
        runtime.createInMemorySource("userId.pure", source);
        runtime.compile();
        assertSetSourceInformation(source, "Person");
    }

    @Test
    public void testDuplicateError()
    {
        runtime.createInMemorySource("userId.pure",
                "Class Person{name:String[1];}\n" +
                        "function a():meta::pure::mapping::SetImplementation[*]{[]}\n" +
                        "###Mapping\n" +
                        "Mapping myMap1(\n" +
                        "   *Person[one]: Operation\n" +
                        "           {\n" +
                        "               a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        "   Person[one]: Operation\n" +
                        "           {\n" +
                        "               a__SetImplementation_MANY_()\n" +
                        "           }\n" +
                        ")\n");

        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Duplicate mapping found with id: 'one' in mapping myMap1", 9, 4, e);
    }
}


