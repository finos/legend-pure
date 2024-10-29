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

package org.finos.legend.pure.runtime.java.compiled.runtime.modeling._class;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.test.ToFix;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())), JavaModelFactoryRegistryLoader.loader(), getOptions(), getExtra());
    }

    @After
    public void clearRuntime()
    {
        runtime.delete("/test/testSource.pure");
        runtime.compile();
    }

    @Test
    public void testInheritedQualifiedProperty()
    {
        compileTestSource("/test/testSource.pure",
                "import test::*;\n" +
                        "Class test::TestClass1\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  getNameFunction()\n" +
                        "  {\n" +
                        "     $this.name + 'x'\n" +
                        "  }:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClass2 extends TestClass1\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  assertEquals('Danielx', ^TestClass1(name='Daniel').getNameFunction());\n" +
                        "  assertEquals('Benedictx', ^TestClass2(name='Benedict').getNameFunction());\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @ToFix
    @Ignore
    public void testInheritedQualifiedPropertyWithTighterMultiplicity()
    {
        compileTestSource("/test/testSource.pure",
                "import test::*;\n" +
                        "Class test::TestClass1\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  getNames()\n" +
                        "  {\n" +
                        "    $this.name->split(' ')\n" +
                        "  }:String[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClass2 extends TestClass1\n" +
                        "{\n" +
                        "  getNames()\n" +
                        "  {\n" +
                        "    let x = $this.name->split(' ');" +
                        "    $x->at($x->size()-1);\n" +
                        "  }:String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  ^TestClass1(name='Daniel Benedict').getNames()->joinStrings(', ') +\n" +
                        "    '\\n' +\n" +
                        "    ^TestClass2(name='Daniel Benedict').getNames()->joinStrings(', ')\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        CoreInstance result = functionExecution.start(func, Lists.immutable.empty());
        Assert.assertEquals("Daniel, Benedict\nBenedict", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }

    @Test
    public void testMultipleQualifiedProperties()
    {
        compileTestSource("/test/testSource.pure",
                "import test::*;\n" +
                        "Class test::TestClass\n" +
                        "{\n" +
                        "  names : String[1..*];\n" +
                        "  title : String[0..1];\n" +
                        "  nameCount()\n" +
                        "  {\n" +
                        "     $this.names->size()\n" +
                        "  }:Integer[1];\n" +
                        "  firstName()\n" +
                        "  {\n" +
                        "     $this.name(0)\n" +
                        "  }:String[1];\n" +
                        "  lastName()\n" +
                        "  {\n" +
                        "     let count = $this.nameCount();\n" +
                        "     if($count == 1, |[], |$this.name($count - 1));\n" +
                        "  }:String[0..1];\n" +
                        "  name(i:Integer[1])\n" +
                        "  {\n" +
                        "     $this.names->at($i)\n" +
                        "  }:String[1];\n" +
                        "  fullName()\n" +
                        "  {\n" +
                        "    $this.fullName(false)\n" +
                        "  }:String[1];\n" +
                        "  fullName(withTitle:Boolean[1])\n" +
                        "  {\n" +
                        "     let titleString = if($withTitle && !$this.title->isEmpty(), |$this.title->toOne() + ' ', |'');\n" +
                        "     $this.names->joinStrings($titleString, ' ', '');\n" +
                        "  }:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let ralphWaldoEmerson = ^TestClass(names=['Ralph', 'Waldo', 'Emerson']);\n" +
                        "  let charlesIves = ^TestClass(names=['Charles', 'Ives']);\n" +
                        "  let johnDewey = ^TestClass(names=['John', 'Dewey'], title='Professor');\n" +
                        "  assertEquals('Ralph Waldo Emerson', $ralphWaldoEmerson.fullName());\n" +
                        "  assertEquals('Ralph Waldo Emerson', $ralphWaldoEmerson.fullName(true));\n" +
                        "  assertEquals('Ralph', $ralphWaldoEmerson.name(0));\n" +
                        "  assertEquals('Waldo', $ralphWaldoEmerson.name(1));\n" +
                        "  assertEquals('Emerson', $ralphWaldoEmerson.name(2));\n" +
                        "  assertEquals('Charles', $charlesIves.firstName());\n" +
                        "  assertEquals('Ives', $charlesIves.lastName());\n" +
                        "  assertEquals('Charles Ives', $charlesIves.fullName());\n" +
                        "  assertEquals('John Dewey', $johnDewey.fullName());\n" +
                        "  assertEquals('Professor John Dewey', $johnDewey.fullName(true));\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testQualifiedPropertyWithTypeParameter()
    {
        compileTestSource("/test/testSource.pure",
                "import test::*;\n" +
                        "Class test::TestClass<T>\n" +
                        "{\n" +
                        "  values : T[*];\n" +
                        "  firstValue()\n" +
                        "  {\n" +
                        "     $this.values->first()\n" +
                        "  }:T[0..1];\n" +
                        "  contains(value:T[1])\n" +
                        "  {\n" +
                        "     !$this.values->filter(v | $value == $v)->isEmpty()\n" +
                        "  }:Boolean[1];\n" +
                        "  findAll(pred:Function<{T[1]->Boolean[1]}>[1])\n" +
                        "  {\n" +
                        "     $this.values->filter($pred)\n" +
                        "  }:T[*];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let vals1 = ^TestClass<StrictDate>(values=[%2024-05-24, %2023-04-23, %1922-03-22, %1921-02-21]);\n" +
                        "  let vals2 = ^TestClass<String>(values=['Charles', 'Ives']);\n" +
                        "  assertEquals(%2024-05-24, $vals1.firstValue());\n" +
                        "  assertEquals([%1922-03-22, %1921-02-21], $vals1.findAll(d | $d->toString()->startsWith('19')));\n" +
                        "  assertEquals('Charles', $vals2.firstValue());\n" +
                        "  assertEquals('Ives', $vals2.findAll(s | $s->length() < 5));\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        MutableList<CodeRepository> repositories = org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository system = GenericCodeRepository.build("system", "((meta)|(system)|(apps::pure))(::.*)?", "platform");
        CodeRepository test = GenericCodeRepository.build("test", "test(::.*)?", "platform", "system");
        repositories.add(system);
        repositories.add(test);
        return repositories;
    }
}
