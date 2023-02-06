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
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class TestFunctionDescriptor
{
    @Test
    public void testIsPossiblyFunctionDescriptor()
    {
        Assert.assertFalse(FunctionDescriptor.isPossiblyFunctionDescriptor(""));
        Assert.assertFalse(FunctionDescriptor.isPossiblyFunctionDescriptor("abcde"));
        Assert.assertFalse(FunctionDescriptor.isPossiblyFunctionDescriptor("pkg1::pkg2::func(Boolean[1])"));
        Assert.assertFalse(FunctionDescriptor.isPossiblyFunctionDescriptor("pkg1::pkg2::func:Boolean[1]"));

        Assert.assertTrue(FunctionDescriptor.isPossiblyFunctionDescriptor("pkg1::pkg2::func(Integer[1]):Boolean[1]"));
    }

    @Test
    public void testIsValidFunctionDescriptor()
    {
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
    }

    @Test
    public void testGetFunctionDescriptor() throws InvalidFunctionDescriptorException
    {
        PureRuntime runtime = new PureRuntimeBuilder(new PureCodeStorage(Paths.get("..", "pure-code", "local"), new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();
        runtime.createInMemorySource("fromString.pure", "function pkg1::pkg2::pkg3::func1(string1:String[1], string2:String[1]):String[1]\n" +
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
                                                   "}");
        runtime.compile();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();

        CoreInstance func1 = FunctionDescriptor.getFunctionByDescriptor("pkg1::pkg2::pkg3::func1(String[1], String[1]):String[1]", processorSupport);
        Assert.assertNotNull(func1);
        Assert.assertEquals("pkg1::pkg2::pkg3::func1(String[1], String[1]):String[1]", FunctionDescriptor.getFunctionDescriptor(func1, processorSupport));

        CoreInstance func2 = FunctionDescriptor.getFunctionByDescriptor("pkg1::func2(String[1]):Integer[1]", processorSupport);
        Assert.assertNotNull(func2);
        Assert.assertEquals("pkg1::func2(String[1]):Integer[1]", FunctionDescriptor.getFunctionDescriptor(func2, processorSupport));

        CoreInstance func3 = FunctionDescriptor.getFunctionByDescriptor("func3():Boolean[1]", processorSupport);
        Assert.assertNotNull(func3);
        Assert.assertEquals("func3():Boolean[1]", FunctionDescriptor.getFunctionDescriptor(func3, processorSupport));

        CoreInstance func4 = FunctionDescriptor.getFunctionByDescriptor("func4(T[m]):T[m]", processorSupport);
        Assert.assertNotNull(func4);
        Assert.assertEquals("func4(T[m]):T[m]", FunctionDescriptor.getFunctionDescriptor(func4, processorSupport));
    }

    @Test
    public void testFunctionDescriptorToIdSimple() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("pkg1::pkg2::pkg3::func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("pkg1::pkg2::pkg3::func(Integer[1]):String[1]"));
        Assert.assertEquals("pkg1::pkg2::pkg3::func_Integer_1__String_1_", FunctionDescriptor.functionDescriptorToId("    pkg1::pkg2::pkg3::func ( Integer  \t [ 1 ]   ) : String [ 1 ]   "));
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
    public void testFunctionDescriptorToIdRealFunctions() throws InvalidFunctionDescriptorException
    {
        Assert.assertEquals("meta::pure::functions::collection::get_T_MANY__String_1__T_$0_1$_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::collection::get(T[*], String[1]):T[0..1]"));
        Assert.assertEquals("meta::json::toJSON_Any_MANY__Integer_$0_1$__Boolean_1__String_1_", FunctionDescriptor.functionDescriptorToId("meta::json::toJSON(Any[*], Integer[0..1], Boolean[1]):String[1]"));
        Assert.assertEquals("meta::pure::functions::string::toString_Any_1__String_1_", FunctionDescriptor.functionDescriptorToId("meta::pure::functions::string::toString(Any[1]):String[1]"));
    }

    @Test
    public void testInvalidFunctionDescriptorToId()
    {
        try
        {
            FunctionDescriptor.functionDescriptorToId("not a function descriptor");
            Assert.fail();
        }
        catch (InvalidFunctionDescriptorException e)
        {
            // Success
        }

        try
        {
            FunctionDescriptor.functionDescriptorToId("func(Type1, Type2):String[1]");
            Assert.fail();
        }
        catch (InvalidFunctionDescriptorException e)
        {
            // Success
        }

        try
        {
            FunctionDescriptor.functionDescriptorToId("func)");
            Assert.fail();
        }
        catch (InvalidFunctionDescriptorException e)
        {
            // Success
        }
    }

    @Test
    public void testGetFunctionByDescriptor() throws InvalidFunctionDescriptorException
    {
        PureRuntime runtime = new PureRuntimeBuilder(new PureCodeStorage(Paths.get("..", "pure-code", "local"), new ClassLoaderCodeStorage(CodeRepository.newPlatformCodeRepository()))).build();
        runtime.loadAndCompileCore();
        runtime.createInMemorySource("fromString.pure", "function test1():Integer[1]\n" +
                                                   "{\n" +
                                                   "    5\n" +
                                                   "}\n" +
                                                   "function pkg1::pkg2::test2(s:String[0..5], i:Integer[*], j:Integer[0..*]):Date[2]\n" +
                                                   "{\n" +
                                                   "    [%2014, %2013]\n" +
                                                   "}\n");
        runtime.compile();
        ProcessorSupport processorSupport = runtime.getProcessorSupport();

        CoreInstance expected;
        CoreInstance actual;

        // Test functions
        expected = processorSupport.package_getByUserPath("test1__Integer_1_");
        Assert.assertNotNull(expected);
        actual = FunctionDescriptor.getFunctionByDescriptor("test1():Integer[1]", processorSupport);
        Assert.assertSame(expected, actual);
        actual = FunctionDescriptor.getFunctionByDescriptor("test1():Integer[1..1]", processorSupport);
        Assert.assertSame(expected, actual);

        expected = processorSupport.package_getByUserPath("pkg1::pkg2::test2_String_$0_5$__Integer_MANY__Integer_MANY__Date_2_");
        Assert.assertNotNull(expected);
        actual = FunctionDescriptor.getFunctionByDescriptor("pkg1::pkg2::test2(String[0..5], Integer[*], Integer[*]):Date[2]", processorSupport);
        Assert.assertSame(expected, actual);
        actual = FunctionDescriptor.getFunctionByDescriptor("pkg1::pkg2::test2(String[0..5], Integer[0..*], Integer[0..*]):Date[2..2]", processorSupport);
        Assert.assertSame(expected, actual);

        // Real functions
//        expected = processorSupport.package_getByUserPath("meta::pure::functions::collection::get_T_MANY__String_1__T_$0_1$_");
//        Assert.assertNotNull(expected);
//        actual = FunctionDescriptor.getFunctionByDescriptor("meta::pure::functions::collection::get(T[*], String[1]):T[0..1]", processorSupport);
//        Assert.assertSame(expected, actual);

//        expected = processorSupport.package_getByUserPath("meta::pure::functions::string::toString_Any_1__String_1_");
//        Assert.assertNotNull(expected);
//        actual = FunctionDescriptor.getFunctionByDescriptor("meta::pure::functions::string::toString(Any[1]):String[1]", processorSupport);
//        Assert.assertSame(expected, actual);

        expected = processorSupport.package_getByUserPath("meta::pure::functions::string::plus_String_MANY__String_1_");
        Assert.assertNotNull(expected);
        actual = FunctionDescriptor.getFunctionByDescriptor("meta::pure::functions::string::plus(String[*]):String[1]", processorSupport);
        Assert.assertSame(expected, actual);
    }
}
