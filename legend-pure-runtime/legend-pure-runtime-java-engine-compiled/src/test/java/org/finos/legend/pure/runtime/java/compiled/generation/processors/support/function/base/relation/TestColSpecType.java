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

public class TestColSpecType extends AbstractPureTestWithCoreCompiled
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
    public void testGenericTypeSetCorrectlyOnColSpec()
    {
        compileTestSource("testSource.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let type = getRelation()->testColSpecType(~id);\n" +
                        "   assertEquals('ColSpec', $type.rawType.name);\n" +
                        "   assertEquals('id', $type.typeArguments->at(0).rawType->toOne()->cast(@meta::pure::metamodel::relation::RelationType<Any>).columns->toOne().name);\n" +
                        "}\n" +
                        "function getRelation():meta::pure::metamodel::relation::Relation<(id:Number[1], code:Number[1])>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}" +
                        "function testColSpecType<T,Z>(rel: meta::pure::metamodel::relation::Relation<T>[0..1], col:meta::pure::metamodel::relation::ColSpec<Z⊆T>[1]) : GenericType[1]\n" +
                        "{\n" +
                        "   $col->genericType();\n" +
                        "}\n" +
                        "\n"
        );
        execute("go():Any[*]");
    }

    @Test
    public void testGenericTypeSetCorrectlyOnColSpecArray()
    {
        compileTestSource("testSource.pure",
                "function go():Any[*]\n" +
                        "{\n" +
                        "   let type = getRelation()->testColSpecType(~[id,code]);\n" +
                        "   assertEquals('ColSpecArray', $type.rawType.name);\n" +
                        "   assertEquals(['id', 'code'], $type.typeArguments->at(0).rawType->toOne()->cast(@meta::pure::metamodel::relation::RelationType<Any>).columns.name);\n" +
                        "}\n" +
                        "function getRelation():meta::pure::metamodel::relation::Relation<(id:Number[1], code:Number[1])>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}" +
                        "function testColSpecType<T,Z>(rel: meta::pure::metamodel::relation::Relation<T>[0..1], col:meta::pure::metamodel::relation::ColSpecArray<Z⊆T>[1]) : GenericType[1]\n" +
                        "{\n" +
                        "   $col->genericType();\n" +
                        "}\n" +
                        "\n"
        );
        execute("go():Any[*]");
    }
}
