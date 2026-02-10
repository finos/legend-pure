// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.base.relation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestColSpecAnnotations extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(new FunctionExecutionCompiledBuilder().build(), JavaModelFactoryRegistryLoader.loader());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleColumnWithStereotype()
    {
        compileTestSource("testSource.pure",
                "Profile test::myProfile\n" +
                        "{\n" +
                        "   stereotypes: [ignore];\n" +
                        "   tags: [myTag];\n" +
                        "}\n" +
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let colSpec = getColSpec();\n" +
                        "   let columns = $colSpec->genericType().typeArguments->at(0).rawType->cast(@meta::pure::metamodel::relation::RelationType<(firstName:String)>).columns;\n" +
                        "   assertEquals(1, $columns->size());\n" +
                        "   assertEquals(1, $columns->at(0).stereotypes->size());\n" +
                        "   assertEquals('test', $columns->at(0).stereotypes->at(0).profile.package.name);\n" +
                        "   assertEquals('myProfile', $columns->at(0).stereotypes->at(0).profile.name);\n" +
                        "   assertEquals('ignore', $columns->at(0).stereotypes->at(0).value);\n" +
                        "}\n" +
                        "function getColSpec():meta::pure::metamodel::relation::ColSpec<(firstName:String)>[1]\n" +
                        "{\n" +
                        "   ~<<test::myProfile.ignore>> firstName:String[1];\n" +
                        "}\n"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testSimpleColumnWithTaggedValue()
    {
        compileTestSource("testSource.pure",
                "Profile test::myProfile\n" +
                        "{\n" +
                        "   stereotypes: [ignore];\n" +
                        "   tags: [myTag];\n" +
                        "}\n" +
                        "function go():Any[*]\n" +
                        "{\n" +
                        "   let colSpec = getColSpec();\n" +
                        "   let columns = $colSpec->genericType().typeArguments->at(0).rawType->cast(@meta::pure::metamodel::relation::RelationType<(firstName:String)>).columns;\n" +
                        "   assertEquals(1, $columns->size());\n" +
                        "   assertEquals(1, $columns->at(0).taggedValues->size());\n" +
                        "   assertEquals('test', $columns->at(0).taggedValues->at(0).tag.profile.package.name);\n" +
                        "   assertEquals('myProfile', $columns->at(0).taggedValues->at(0).tag.profile.name);\n" +
                        "   assertEquals('myTag', $columns->at(0).taggedValues->at(0).tag.value);\n" +
                        "   assertEquals('test tag value', $columns->at(0).taggedValues->at(0).value);\n" +
                        "}\n" +
                        "function getColSpec():meta::pure::metamodel::relation::ColSpec<(firstName:String)>[1]\n" +
                        "{\n" +
                        "   ~{test::myProfile.myTag = 'test tag value'} firstName:String[1];\n" +
                        "}\n"
        );
        execute("go():Any[*]");
    }
}
