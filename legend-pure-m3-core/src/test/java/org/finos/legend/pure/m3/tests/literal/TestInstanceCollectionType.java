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

package org.finos.legend.pure.m3.tests.literal;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

public class TestInstanceCollectionType extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");

        try
        {
            runtime.compile();
        }
        catch (PureCompilationException e)
        {
            setUp();
        }
    }

    @Test
    public void testPrimitivesSameType()
    {
        assertValueSpecificationGenericType(M3Paths.Integer, "[1, 2, 3]");
        assertValueSpecificationGenericType(M3Paths.Float, "[1.0, 2.0, 3.0]");
        assertValueSpecificationGenericType(M3Paths.String, "['a', 'b', 'c']");
        assertValueSpecificationGenericType(M3Paths.Boolean, "[true, false]");
    }

    @Test
    public void testPrimitivesMixedTypes()
    {
        assertValueSpecificationGenericType(M3Paths.Number, "[1, 2.0, 3]");
        assertValueSpecificationGenericType(M3Paths.Any, "[1, 2.0, 'c']");
    }

    @Test
    public void testClassesSameType()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A {}\n" +
                "Class test::B extends A {}\n" +
                "Class test::C extends B {}\n" +
                "Class test::D extends B {}\n" +
                "Class test::E extends A {}\n" +
                "Class test::F {}\n");

        assertValueSpecificationGenericType("test::A", "[^A(), ^A()]");
        assertValueSpecificationGenericType("test::B", "[^B(), ^B()]");
        assertValueSpecificationGenericType("test::C", "[^C(), ^C()]");
        assertValueSpecificationGenericType("test::D", "[^D(), ^D()]");
        assertValueSpecificationGenericType("test::E", "[^E(), ^E()]");
        assertValueSpecificationGenericType("test::F", "[^F(), ^F()]");
    }

    @Test
    public void testClassesMixedTypes()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A {}\n" +
                "Class test::B extends A {}\n" +
                "Class test::C extends B {}\n" +
                "Class test::D extends B {}\n" +
                "Class test::E extends A {}\n" +
                "Class test::F {}\n");

        assertValueSpecificationGenericType("test::A", "[^A(), ^B()]");
        assertValueSpecificationGenericType("test::A", "[^B(), ^A()]");
        assertValueSpecificationGenericType("test::A", "[^A(), ^B(), ^C()]");
        assertValueSpecificationGenericType("test::A", "[^A(), ^B(), ^C(), ^D()]");
        assertValueSpecificationGenericType("test::A", "[^A(), ^B(), ^C(), ^D(), ^E()]");

        assertValueSpecificationGenericType("test::A", "[^D(), ^E()]");
        assertValueSpecificationGenericType("test::A", "[^E(), ^C(), ^D()]");

        assertValueSpecificationGenericType("test::B", "[^B(), ^C()]");
        assertValueSpecificationGenericType("test::B", "[^B(), ^D()]");
        assertValueSpecificationGenericType("test::B", "[^D(), ^B()]");
        assertValueSpecificationGenericType("test::B", "[^D(), ^B(), ^C()]");

        assertValueSpecificationGenericType(M3Paths.Any, "[^A(), ^F()]");
        assertValueSpecificationGenericType(M3Paths.Any, "[^A(), ^B(), ^F()]");
        assertValueSpecificationGenericType(M3Paths.Any, "[^A(), ^B(), ^E(), ^F()]");
        assertValueSpecificationGenericType(M3Paths.Any, "[^A(), ^B(), ^C(), ^D(), ^E(), ^F()]");
    }

    @Test
    public void testClassesWithGenerics()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A<X|m> {}\n" +
                "Class test::B<T|n> extends A<T|n> {}\n" +
                "Class test::C<U|o> extends B<U|o> {}\n" +
                "Class test::D<V, Y|p> extends B<V|p> {}\n" +
                "Class test::E<W> extends A<W|1> {}\n" +
                "Class test::F<Y|q> {}\n" +
                "Class test::G<Z, S|r> extends F<Z|r> {}\n" +
                "Class test::H<R> extends F<R|1> {}\n");

        assertValueSpecificationGenericType("test::B<Integer|1>", "[^B<Integer|1>(), ^C<Integer|1>()]");
        assertValueSpecificationGenericType("test::B<Number|0..2>", "[^B<Integer|1..2>(), ^C<Float|0..1>()]");
        assertValueSpecificationGenericType("test::B<Number|*>", "[^C<Integer|1>(), ^D<Float, Boolean|*>()]");
        assertValueSpecificationGenericType("test::B<" + M3Paths.Any + "|1..100>", "[^B<Integer|1>(), ^C<Float|7..10>(), ^D<String, Boolean|50..100>()]");
        assertValueSpecificationGenericType("test::A<Number|1..*>", "[^C<Integer|1>(), ^D<Float, Date|100..*>(), ^E<Number>()]");

        assertValueSpecificationGenericType("test::B<test::E<Integer>|1..2>", "[^B<E<Integer>|1>(), ^C<E<Integer>|2>()]");
        assertValueSpecificationGenericType("test::B<test::E<Number>|*>", "[^B<E<Integer>|1>(), ^C<E<Float>|2>(), ^D<E<Number>, A<Date|1>|*>()]");

        assertValueSpecificationGenericType("test::E<test::B<Number|0..1>>", "[^E<C<Integer|1>>(), ^E<D<Float, String|0..1>>()]");
        assertValueSpecificationGenericType("test::A<test::A<Number|*>|0..1>", "[^E<C<Integer|0..1>>(), ^E<D<Float, String|1..*>>(), ^C<E<Integer>|0..1>()]");
        assertValueSpecificationGenericType("test::A<test::F<Number|*>|0..1>", "[^E<F<Integer|0..1>>(), ^E<G<Float, String|1..*>>(), ^C<H<Integer>|0..1>()]");
    }

    @Test
    public void testClassesWithAndWithoutGenerics()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A {}\n" +
                "Class test::B<T|m> extends A {}\n" +
                "Class test::C<U|n> extends B<U|o> {}\n" +
                "Class test::D<V|o> extends B<V|o> {}\n" +
                "Class test::E extends A {}\n" +
                "Class test::F {}\n");

        assertValueSpecificationGenericType("test::A", "[^B<Integer|1>(), ^C<Float|0..1>(), ^D<String|*>(), ^A()]");
        assertValueSpecificationGenericType("test::A", "[^A(), ^B<Integer|*>()]");
    }

    @Test
    public void testFunctionTypes()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A<X> {}\n" +
                "Class test::B<T> extends A<T> {}\n" +
                "Class test::C<U> extends B<U> {}\n" +
                "Class test::D<V> extends B<V> {}\n" +
                "Class test::E<W> extends A<W> {}\n");

        assertValueSpecificationGenericType("test::A<" + M3Paths.Function + "<{Integer[1]->Integer[1]}>>", "[^A<Function<{Integer[1]->Integer[1]}>>(), ^A<Function<{Integer[1]->Integer[1]}>>()]");
        assertValueSpecificationGenericType("test::B<" + M3Paths.Function + "<{Integer[1]->" + M3Paths.Any + "[1..*]}>>", "[^C<Function<{Integer[1]->Integer[1]}>>(), ^D<Function<{Integer[1]->Date[2..*]}>>()]");
        assertValueSpecificationGenericType("test::A<" + M3Paths.Function + "<" + M3Paths.Any + ">>", "[^C<Function<{Integer[1]->Integer[1]}>>(), ^E<Function<{Integer[1], String[0..1]->Date[2..*]}>>()]");
        assertValueSpecificationGenericType("test::B<" + M3Paths.Function + "<{Integer[1]->Number[0..1]}>>", "[^B<Function<{Number[1]->Integer[1]}>>(), ^D<Function<{Integer[1]->Float[0..1]}>>()]");
    }

    @Test
    public void testClasses()
    {
        compileTestSource("fromString.pure", "import test::*;\n" +
                "Class test::A {}\n" +
                "Class test::B extends A {}\n" +
                "Class test::C extends B {}\n" +
                "Class test::D extends B {}\n" +
                "Class test::E extends A {}\n" +
                "Class test::F {}\n");

        assertValueSpecificationGenericType(M3Paths.Class + "<test::A>", "[A, A]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::A>", "[A, B]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::A>", "[B, C, D, E]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::A>", "[A, B, C, D, E]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::A>", "[C, E]");

        assertValueSpecificationGenericType(M3Paths.Class + "<test::B>", "[B, C]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::B>", "[C, D]");
        assertValueSpecificationGenericType(M3Paths.Class + "<test::B>", "[B, C, D]");

        assertValueSpecificationGenericType(M3Paths.Class + "<" + M3Paths.Any + ">", "[A, F]");
        assertValueSpecificationGenericType(M3Paths.Class + "<" + M3Paths.Any + ">", "[B, F]");
        assertValueSpecificationGenericType(M3Paths.Class + "<" + M3Paths.Any + ">", "[C, D, F]");
        assertValueSpecificationGenericType(M3Paths.Class + "<" + M3Paths.Any + ">", "[E, F]");
    }

    private void assertValueSpecificationGenericType(String expectedGenericTypeString, String valueSpecificationString)
    {
        assertValueSpecificationGenericType(null, expectedGenericTypeString, valueSpecificationString);
    }

    private void assertValueSpecificationGenericType(String message, String expectedGenericTypeString, String valueSpecificationString)
    {
        String functionDescriptor = "test::" + UUID.randomUUID().toString().replace('-', '_') + "():Any[*]";
        compileTestSource("sourceId.pure", "import test::*;\nfunction " + functionDescriptor + "\n{\n" + valueSpecificationString + "\n}\n");
        FunctionDefinition<?> function = (FunctionDefinition<?>) runtime.getFunction(functionDescriptor);
        ValueSpecification valueSpecification = function._expressionSequence().getFirst();
        assertGenericTypesEqual((message == null) ? valueSpecificationString : message, expectedGenericTypeString, valueSpecification._genericType());
        runtime.delete("sourceId.pure");
    }

    private void assertGenericTypesEqual(String message, String expectedGenericTypeString, CoreInstance actualGenericType)
    {
        String actualString = GenericType.print(actualGenericType, true, processorSupport);
        Assert.assertEquals(message, expectedGenericTypeString, actualString);
    }
}
