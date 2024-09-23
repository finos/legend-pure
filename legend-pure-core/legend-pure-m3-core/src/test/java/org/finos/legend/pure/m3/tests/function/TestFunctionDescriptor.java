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

package org.finos.legend.pure.m3.tests.function;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public class TestFunctionDescriptor
{
    @Test
    public void testIsValidFunctionDescriptor()
    {
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor(null));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor(""));
        //parameter name not allowed just parameter type and it's multiplicity
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::meta::pathToElement(String[1])"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::meta::pathToElement:PackageableElement[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::invalid#%@@+!!~?characters::func(Integer[1]):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[**]):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func():Boolean[1],SecondReturnType[*]"));
        //trailing comma after last parameter
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Foo[1],    ):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[*..*]):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[*..1]):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[m..n]):Boolean[m..n]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[5..n]):Boolean[5..n]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[m..10]):Boolean[m..10]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(my::other::package::MyClass[1]):OtherClass[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(MyClass[1]):my::different::package::OtherClass[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Foo[]):Boolean[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Foo[1]):Boolean[]"));

        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::meta::pathToElement(String[1]):PackageableElement[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[16..20]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[1..*]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[m]):Boolean[m]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[MultiplicityParam]):Boolean[MultiplicityParam]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::p$__$valid::characters::func(Integer[1]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func():Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[1]  , \t String[1..*], SomeClass[*]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg1::pkg2::func(Integer[1], String[ 1..  5]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("   pkg1::pkg2::func  (   \t Integer [ 1 ] , String [ 1       ..  5   ] )   : Boolean\t[  1 ] "));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("func(Integer[16..20]):Boolean[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("func(A<K>[16..20]):Z<K>[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("pkg::myFunc():Mass~Gram[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::collection::removeDuplicates(T[*],Function<{T[1]->V[1]}>[0..1],Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::collection::removeDuplicates(T[*], Function<{T[1]->V[1]}>[0..1], Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("meta::pure::functions::collection::removeDuplicates(T[*], Function< { T [1] -> V [1] } > [0..1], Function< { V [1], V [1] -> Boolean [ 1 ] } >[0..1]):T[*]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("curry(Function<{T[m],U[n]->V[o]}>[1], T[m]):Function<{U[n]->V[o]}>[1]"));
        Assert.assertTrue(FunctionDescriptor.isValidFunctionDescriptor("curry(Function<{T[m],U[n],V[o]->W[p]}>[1], T[m]):Function<{U[n],V[o]->W[p]}>[1]"));
        Assert.assertFalse(FunctionDescriptor.isValidFunctionDescriptor("curry({T[m],U[n],V[o]->W[p]}[1], T[m]):Function<{U[n],V[o]->W[p]}>[1]"));
    }

    @Test
    public void testFunctionDescriptorToIdSimple() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("pkg1::pkg2::pkg3::func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("pkg1::pkg2::pkg3::func(Integer[1]):String[1]"));
        Assert.assertEquals("pkg1::pkg2::pkg3::func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("    pkg1::pkg2::pkg3::func ( Integer  \t [ 1 ]   ) : String [ 1 ]   "));
        Assert.assertEquals("pkg1::pkg2::pkg3::func_Integer_MANY__String_$1_MANY$_", FunctionDescriptor.functionDescriptorToId("pkg1::pkg2::pkg3::func(Integer[0..*]):String[1..*]"));
    }

    @Test
    public void testFunctionDescriptorToIdNoPackage() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("func(Integer[1]):String[1]"));
        Assert.assertEquals("func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("\t  \tfunc ( Integer\t[1])   : String[1]\t\t"));
    }

    @Test
    public void testFunctionDescriptorToIdNoParameters() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("pkg::myFunc__String_1_", FunctionDescriptor.functionDescriptorToId("pkg::myFunc():String[1]"));
        Assert.assertEquals("pkg::myFunc__String_1_", FunctionDescriptor.functionDescriptorToId(" \t \tpkg::myFunc(       ) : String[\t\t1\t]"));
    }

    @Test
    public void testFunctionDescriptorToIdWithTypeParameters() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("curry_Function_1__T_m__Function_1_", FunctionDescriptor.functionDescriptorToId("curry(Function<{T[m],U[n]->V[o]}>[1], T[m]):Function<{U[n]->V[o]}>[1]"));
        Assert.assertEquals("curry_Function_1__T_m__Function_1_", FunctionDescriptor.functionDescriptorToId("curry(Function<{T[m],U[n],V[o]->W[p]}>[1], T[m]):Function<{U[n],V[o]->W[p]}>[1]"));
    }

    @Test
    public void testFunctionDescriptorToIdUnits() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("pkg::myFunc__Mass$Gram_1_", FunctionDescriptor.functionDescriptorToId("pkg::myFunc():Mass~Gram[1]"));
        Assert.assertEquals("pkg::myFunc__Mass$Gram_1_", FunctionDescriptor.functionDescriptorToId(" \t \tpkg::myFunc(       ) : Mass ~ Gram[\t\t1\t]"));
    }

    @Test
    public void testFunctionDescriptorToIdRealFunctions() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("meta::pure::functions::collection::get_T_MANY__String_1__T_$0_1$_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::get(T[*], String[1]):T[0..1]"));
        Assert.assertEquals("meta::json::toJSON_Any_MANY__Integer_$0_1$__Boolean_1__String_1_", FunctionDescriptor.functionDescriptorToId("meta::json::toJSON(Any[*], Integer[0..1], Boolean[1]):String[1]"));
        Assert.assertEquals("meta::pure::functions::string::toString_Any_1__String_1_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::string::toString(Any[1]):String[1]"));
        Assert.assertEquals("meta::pure::functions::collection::tests::map::classPropertyByName_Class_1__String_1__Property_$0_1$_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::tests::map::classPropertyByName(Class<Any>[1], String[1]):Property<Nil,Any|*>[0..1]"));
        Assert.assertEquals("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::removeDuplicates(T[*],Function<{T[1]->V[1]}>[0..1],Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*]"));
        Assert.assertEquals("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::removeDuplicates(T[*], Function<{T[1]->V[1]}>[0..1], Function<{V[1],V[1]->Boolean[1]}>[0..1]):T[*]"));
        Assert.assertEquals("meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::removeDuplicates(T[*], Function< { T [1] -> V [1] } > [0..1], Function< { V [1], V [1] -> Boolean [ 1 ] } >[0..1]):T[*]"));
    }

    @Test
    public void testInvalidFunctionDescriptorToId()
    {
        assertInvalidFunctionDescriptor("Invalid function descriptor: 'not a function descriptor'", "not a function descriptor");
        assertInvalidFunctionDescriptor("Invalid function descriptor: 'func(Type1, Type2):String[1]'", "func(Type1, Type2):String[1]");
        assertInvalidFunctionDescriptor("Invalid function descriptor: 'func)'", "func)");
    }

    private void assertInvalidFunctionDescriptor(String expectedMessage, String string)
    {
        InvalidFunctionDescriptorException e = Assert.assertThrows(InvalidFunctionDescriptorException.class, () -> FunctionDescriptor.functionDescriptorToId(string));
        Assert.assertEquals(expectedMessage, e.getMessage());
    }

    @Test
    public void testGetFunctionDescriptor() throws InvalidFunctionDescriptorException
    {
        PureRuntime runtime = new PureRuntimeBuilder(new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();
        runtime.createInMemorySource(
                "fromString.pure",
                "function pkg1::pkg2::pkg3::func1(string1:String[1], string2:String[1]):String[1]\n" +
                        "{\n" +
                        "    $string1 + $string2 + ' (from private)'\n" +
                        "}\n" +
                        "\n" +
                        "function pkg1::func2(s:String[1]):Integer[1]\n" +
                        "{\n" +
                        "    pkg1::pkg2::pkg3::func1($s, ' from public')->length()\n" +
                        "}\n" +
                        "\n" +
                        "function func3():Boolean[1]\n" +
                        "{\n" +
                        "    false\n" +
                        "}\n" +
                        "\n" +
                        "function func4<T|m>(t:T[m]):T[m]\n" +
                        "{\n" +
                        "    $t\n" +
                        "}\n" +
                        "\n" +
                        "function test1():Integer[1]\n" +
                        "{\n" +
                        "    5\n" +
                        "}\n" +
                        "\n" +
                        "function pkg1::pkg2::test2(s:String[0..5], i:Integer[*], j:Integer[0..*]):Date[2]\n" +
                        "{\n" +
                        "    [%2014, %2013]\n" +
                        "}\n" +
                        "\n" +
                        "Measure pkg1::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}\n" +
                        "\n" +
                        "function my::test::testUnits(k: pkg1::Mass~Kilogram[1]):pkg1::Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   $k;\n" +
                        "}\n");
        runtime.compile();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();

        // Test functions
        assertFunctionDescriptor(
                processorSupport,
                "pkg1::pkg2::pkg3::func1_String_1__String_1__String_1_",
                "pkg1::pkg2::pkg3::func1(String[1], String[1]):String[1]",
                "pkg1::pkg2::pkg3::func1(String[1],String[1]):String[1]",
                "pkg1::pkg2::pkg3::func1( String [ 1 ] , String[1] ) : String [ 1 ] ");
        assertFunctionDescriptor(
                processorSupport,
                "pkg1::func2_String_1__Integer_1_",
                "pkg1::func2(String[1]):Integer[1]",
                "pkg1::func2(\tString[1])\t:\tInteger[1..1]");
        assertFunctionDescriptor(
                processorSupport,
                "func3__Boolean_1_",
                "func3():Boolean[1]",
                "func3(   ):Boolean[1..1]");
        assertFunctionDescriptor(
                processorSupport,
                "func4_T_m__T_m_",
                "func4(T[m]):T[m]",
                "func4( T [m]) : T[m]");
        assertFunctionDescriptor(
                processorSupport,
                "test1__Integer_1_",
                "test1():Integer[1]",
                "test1():Integer[1..1]");
        assertFunctionDescriptor(
                processorSupport,
                "pkg1::pkg2::test2_String_$0_5$__Integer_MANY__Integer_MANY__Date_2_",
                "pkg1::pkg2::test2(String[0..5], Integer[*], Integer[*]):Date[2]",
                "pkg1::pkg2::test2(String[0..5],Integer[0..*],Integer[*]):Date[2..2]",
                "pkg1::pkg2::test2(String[0..5], Integer[0..*], Integer[0..*]):Date[2..2]");
        assertFunctionDescriptor(
                processorSupport,
                "my::test::testUnits_Mass$Kilogram_1__Mass$Kilogram_1_",
                "my::test::testUnits(Mass~Kilogram[1]):Mass~Kilogram[1]",
                "my::test::testUnits(\tMass~Kilogram[1] ) : Mass~Kilogram[1..1]");

        // Real functions
        assertFunctionDescriptor(
                processorSupport,
                "meta::pure::functions::collection::removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_",
                "meta::pure::functions::collection::removeDuplicates(T[*], Function<{T[1]->V[1]}>[0..1], Function<{V[1], V[1]->Boolean[1]}>[0..1]):T[*]",
                "meta::pure::functions::collection::removeDuplicates(T[*], Function<{T[1]->V[1]}>[0..1], Function[0..1]):T[*]",
                "meta::pure::functions::collection::removeDuplicates(T[*], Function[0..1], Function[0..1]):T[*]");
        assertFunctionDescriptor(
                processorSupport,
                "meta::pure::functions::string::plus_String_MANY__String_1_",
                "meta::pure::functions::string::plus(String[*]):String[1]",
                "meta::pure::functions::string::plus(String[0..*]):String [1..1]          ");
    }

    private void assertFunctionDescriptor(ProcessorSupport processorSupport, String functionPath, String expectedDescriptor, String... lookupDescriptors) throws InvalidFunctionDescriptorException
    {
        CoreInstance func = processorSupport.package_getByUserPath(functionPath);
        Assert.assertNotNull(functionPath, func);

        Assert.assertEquals(expectedDescriptor, FunctionDescriptor.getFunctionDescriptor(func, processorSupport));
        Assert.assertSame(expectedDescriptor, func, FunctionDescriptor.getFunctionByDescriptor(expectedDescriptor, processorSupport));

        for (String descriptor : lookupDescriptors)
        {
            CoreInstance actual = FunctionDescriptor.getFunctionByDescriptor(descriptor, processorSupport);
            Assert.assertSame(descriptor, actual, func);
        }
    }
}
