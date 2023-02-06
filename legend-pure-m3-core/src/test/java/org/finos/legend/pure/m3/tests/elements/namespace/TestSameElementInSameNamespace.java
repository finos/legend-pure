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

package org.finos.legend.pure.m3.tests.elements.namespace;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSameElementInSameNamespace extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra());
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", PlatformCodeRepository.NAME),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME, "system"));
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("/test/testSource.pure");
        runtime.delete("/test/testSource1.pure");
        runtime.delete("/test/testSource2.pure");
        runtime.compile();
    }

    @Test
    public void testClass()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "/test/testSource.pure",
                "Class test::model::Person\n" +
                        "{\n" +
                        "   lastName:String[1];\n" +
                        "}\n" +
                        "Class test::model::Person\n" +
                        "{\n" +
                        "   otherName:String[1];\n" +
                        "}\n"));
        assertPureException(PureParserException.class, "The element 'Person' already exists in the package 'test::model'", "/test/testSource.pure", 5, 20, e);
    }


    @Test
    public void testFunction()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                "function test::go():Nil[0]{[];}\n" +
                        "function test::go():Nil[0]{[];}\n"));
        assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package 'test' at: /test/testSource.pure (line:1 column:16), /test/testSource.pure (line:2 column:16)", "/test/testSource.pure", 2, 16, e);
    }

    @Test
    public void testDuplicateFunctionsInDifferentSections()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource.pure",
                "###Pure\n" +
                        "function test::go():Nil[0]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}\n" +
                        "###Pure\n" +
                        "function test::go():Nil[0]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package 'test' at: /test/testSource.pure (line:2 column:16), /test/testSource.pure (line:7 column:16)", e);
    }

    @Test
    public void testDuplicateFunctionsInDifferentFiles()
    {
        compileTestSource("/test/testSource1.pure",
                "function test::go():Nil[0]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}\n");
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "/test/testSource2.pure",
                "function test::go():Nil[0]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}"));
        assertPureException(PureCompilationException.class, "The function 'go__Nil_0_' is defined more than once in the package 'test' at: /test/testSource1.pure (line:1 column:16), /test/testSource2.pure (line:1 column:16)", "/test/testSource2.pure", 1, 16, e);
    }

    @Test
    public void testAssociation()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "/test/testSource.pure",
                "Class test::Firm {}" +
                        "Class test::Person {}\n" +
                        "Association test::arg::myAsso {firm:Firm[1]; employees:Person[*];}\n" +
                        "Association test::arg::myAsso {firm:Firm[1]; employees:Person[*];}\n"));
        assertPureException(PureParserException.class, "The element 'myAsso' already exists in the package 'test::arg'", "/test/testSource.pure", 3, 24, e);
    }

    @Test
    public void testEnum()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "/test/testSource.pure",
                "Enum test::myEnum {CUSIP, SEDOL}\n" +
                        "Enum test::myEnum {CUSIP, SEDOL}\n"));
        assertPureException(PureParserException.class, "The element 'myEnum' already exists in the package 'test'", "/test/testSource.pure", 2, 12, e);
    }
}
