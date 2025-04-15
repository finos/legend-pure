// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements.relation;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRelationType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleColumnWithTypeSuccess()
    {
        compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Number[0..1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Integer[1])>;\n" +
                        "}");
    }

    @Test
    public void testSimpleColumnWithTypeFailing()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Integer[1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Number[1])>;\n" +
                        "}")
                    );

        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::Relation<(name:Number[1])>; expected: meta::pure::metamodel::relation::Relation<(name:Integer[1])>",
                e);
    }

    @Test
    public void testSimpleColumnWithMultiplicityFailing()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "function test():meta::pure::metamodel::relation::Relation<(name:Number[1])>[1]\n" +
                        "{\n" +
                        "   @meta::pure::metamodel::relation::Relation<(name:Integer[0..1])>;\n" +
                        "}")
        );

        assertPureException(
                PureCompilationException.class,
                "Return type error in function 'test'; found: meta::pure::metamodel::relation::Relation<(name:Integer)>; expected: meta::pure::metamodel::relation::Relation<(name:Number[1])>",
                e);
    }
}
