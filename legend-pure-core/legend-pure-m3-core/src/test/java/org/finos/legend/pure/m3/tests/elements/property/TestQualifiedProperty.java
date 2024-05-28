// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.elements.property;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), new CompositeCodeStorage(new ClassLoaderCodeStorage(getCodeRepositories())));
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

        Class<?> testClass1 = (Class<?>) runtime.getCoreInstance("test::TestClass1");
        QualifiedProperty<?> getNameFunction = testClass1._qualifiedProperties().getOnly();
        Assert.assertEquals("getNameFunction", getNameFunction._name());
        Assert.assertEquals("getNameFunction", getNameFunction._functionName());
        Assert.assertEquals("getNameFunction()", getNameFunction._id());
        assertGenericTypeEquals("String", getNameFunction._genericType());
        assertMultiplicityEquals("1", getNameFunction._multiplicity());

        Assert.assertEquals(Maps.mutable.with("getNameFunction()", getNameFunction), processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass1")));
        Assert.assertEquals(Maps.mutable.with("getNameFunction()", getNameFunction), processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass2")));
    }

    @Test
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

        Class<?> testClass1 = (Class<?>) runtime.getCoreInstance("test::TestClass1");
        QualifiedProperty<?> getNames1 = testClass1._qualifiedProperties().getOnly();
        Assert.assertEquals("getNames", getNames1._name());
        Assert.assertEquals("getNames", getNames1._functionName());
        Assert.assertEquals("getNames()", getNames1._id());
        assertGenericTypeEquals("String", getNames1._genericType());
        assertMultiplicityEquals("*", getNames1._multiplicity());

        Class<?> testClass2 = (Class<?>) runtime.getCoreInstance("test::TestClass2");
        QualifiedProperty<?> getNames2 = testClass2._qualifiedProperties().getOnly();
        Assert.assertEquals("getNames", getNames2._name());
        Assert.assertEquals("getNames", getNames2._functionName());
        Assert.assertEquals("getNames()", getNames2._id());
        assertGenericTypeEquals("String", getNames2._genericType());
        assertMultiplicityEquals("0..1", getNames2._multiplicity());

        Assert.assertEquals(Maps.mutable.with("getNames()", getNames1), processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass1")));
        Assert.assertEquals(Maps.mutable.with("getNames()", getNames2), processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass2")));
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
                        "     $this.fullName($withTitle, [])\n" +
                        "  }:String[1];\n" +
                        "  fullName(withTitle:Boolean[1], defaultTitle:String[0..1])\n" +
                        "  {\n" +
                        "     let titleString = if(!$withTitle, |'', |if(!$this.title->isEmpty(), |$this.title->toOne() + ' ', |if(!$defaultTitle->isEmpty(), |$defaultTitle->toOne() + ' ', |'')));\n" +
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
                        "  assertEquals('Mr Charles Ives', $charlesIves.fullName(true, 'Mr'));\n" +
                        "  assertEquals('Charles Ives', $charlesIves.fullName());\n" +
                        "  assertEquals('John Dewey', $johnDewey.fullName());\n" +
                        "  assertEquals('Professor John Dewey', $johnDewey.fullName(true));\n" +
                        "}\n");
        Class<?> testClass = (Class<?>) runtime.getCoreInstance("test::TestClass");
        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = ListHelper.wrapListIterable(testClass._qualifiedProperties());

        QualifiedProperty<?> nameCount = qualifiedProperties.get(0);
        Assert.assertEquals("nameCount", nameCount._name());
        Assert.assertEquals("nameCount", nameCount._functionName());
        Assert.assertEquals("nameCount()", nameCount._id());
        assertGenericTypeEquals("Integer", nameCount._genericType());
        assertMultiplicityEquals("1", nameCount._multiplicity());

        QualifiedProperty<?> firstName = qualifiedProperties.get(1);
        Assert.assertEquals("firstName", firstName._name());
        Assert.assertEquals("firstName", firstName._functionName());
        Assert.assertEquals("firstName()", firstName._id());
        assertGenericTypeEquals("String", firstName._genericType());
        assertMultiplicityEquals("1", firstName._multiplicity());

        QualifiedProperty<?> lastName = qualifiedProperties.get(2);
        Assert.assertEquals("lastName", lastName._name());
        Assert.assertEquals("lastName", lastName._functionName());
        Assert.assertEquals("lastName()", lastName._id());
        assertGenericTypeEquals("String", lastName._genericType());
        assertMultiplicityEquals("0..1", lastName._multiplicity());

        QualifiedProperty<?> name = qualifiedProperties.get(3);
        Assert.assertEquals("name", name._name());
        Assert.assertEquals("name", name._functionName());
        Assert.assertEquals("name(Integer[1])", name._id());
        assertGenericTypeEquals("String", name._genericType());
        assertMultiplicityEquals("1", name._multiplicity());

        QualifiedProperty<?> fullName = qualifiedProperties.get(4);
        Assert.assertEquals("fullName", fullName._name());
        Assert.assertEquals("fullName", fullName._functionName());
        Assert.assertEquals("fullName()", fullName._id());
        assertGenericTypeEquals("String", fullName._genericType());
        assertMultiplicityEquals("1", fullName._multiplicity());

        QualifiedProperty<?> fullNameWithTitle = qualifiedProperties.get(5);
        Assert.assertEquals("fullName", fullNameWithTitle._name());
        Assert.assertEquals("fullName", fullNameWithTitle._functionName());
        Assert.assertEquals("fullName(Boolean[1])", fullNameWithTitle._id());
        assertGenericTypeEquals("String", fullNameWithTitle._genericType());
        assertMultiplicityEquals("1", fullNameWithTitle._multiplicity());

        QualifiedProperty<?> fullNameWithDefaultTitle = qualifiedProperties.get(6);
        Assert.assertEquals("fullName", fullNameWithDefaultTitle._name());
        Assert.assertEquals("fullName", fullNameWithDefaultTitle._functionName());
        Assert.assertEquals("fullName(Boolean[1],String[0..1])", fullNameWithDefaultTitle._id());
        assertGenericTypeEquals("String", fullNameWithDefaultTitle._genericType());
        assertMultiplicityEquals("1", fullNameWithDefaultTitle._multiplicity());

        MutableMap<String, CoreInstance> expectedQualifiedPropertiesByName = Maps.mutable.<String, CoreInstance>empty()
                .withKeyValue("nameCount()", nameCount)
                .withKeyValue("firstName()", firstName)
                .withKeyValue("lastName()", lastName)
                .withKeyValue("name(Integer[1])", name)
                .withKeyValue("fullName()", fullName)
                .withKeyValue("fullName(Boolean[1])", fullNameWithTitle)
                .withKeyValue("fullName(Boolean[1],String[0..1])", fullNameWithDefaultTitle);
        Assert.assertEquals(expectedQualifiedPropertiesByName, processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass")));
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
        Class<?> testClass = (Class<?>) runtime.getCoreInstance("test::TestClass");
        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = ListHelper.wrapListIterable(testClass._qualifiedProperties());

        QualifiedProperty<?> firstValue = qualifiedProperties.get(0);
        Assert.assertEquals("firstValue", firstValue._name());
        Assert.assertEquals("firstValue", firstValue._functionName());
        Assert.assertEquals("firstValue()", firstValue._id());
        assertGenericTypeEquals("T", firstValue._genericType());
        assertMultiplicityEquals("0..1", firstValue._multiplicity());

        QualifiedProperty<?> contains = qualifiedProperties.get(1);
        Assert.assertEquals("contains", contains._name());
        Assert.assertEquals("contains", contains._functionName());
        Assert.assertEquals("contains(T[1])", contains._id());
        assertGenericTypeEquals("Boolean", contains._genericType());
        assertMultiplicityEquals("1", contains._multiplicity());

        QualifiedProperty<?> findAll = qualifiedProperties.get(2);
        Assert.assertEquals("findAll", findAll._name());
        Assert.assertEquals("findAll", findAll._functionName());
        Assert.assertEquals("findAll(Function<{T[1]->Boolean[1]}>[1])", findAll._id());
        assertGenericTypeEquals("T", findAll._genericType());
        assertMultiplicityEquals("*", findAll._multiplicity());

        MutableMap<String, CoreInstance> expectedQualifiedPropertiesByName = Maps.mutable.<String, CoreInstance>empty()
                .withKeyValue("firstValue()", firstValue)
                .withKeyValue("contains(T[1])", contains)
                .withKeyValue("findAll(Function<{T[1]->Boolean[1]}>[1])", findAll);
        Assert.assertEquals(expectedQualifiedPropertiesByName, processorSupport.class_getQualifiedPropertiesByName(runtime.getCoreInstance("test::TestClass")));
    }

    private static void assertGenericTypeEquals(String expected, GenericType actual)
    {
        String message = (actual.getSourceInformation() == null) ? null : actual.getSourceInformation().getMessage();
        String actualString = org.finos.legend.pure.m3.navigation.generictype.GenericType.print(actual, true, processorSupport);
        Assert.assertEquals(message, expected, actualString);
    }

    private static void assertMultiplicityEquals(String expected, Multiplicity actual)
    {
        String actualString = org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.print(actual, false);
        Assert.assertEquals(expected, actualString);
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.mutable.<CodeRepository>withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories())
                .with(GenericCodeRepository.build("test", "test(::.*)?", "platform"));
    }
}
