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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepositoryWithDependencies;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.tests.elements.property.AbstractTestDefaultValue;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestDynamicNew extends AbstractPureTestWithCoreCompiled
{
    private static final String DECLARATION = "Enum myEnum{A,B}"
            + "Class A \n"
            + "[ testConstraint: $this.a == 'rrr']\n"
            + "{ \n"
            + "    a: String[1];      \n"
            + "    b: String[0..1];   \n"
            + "    c: String[*];      \n"
            + "    d : D[0..1];       \n"
            + "    ds : D[*];         \n"
            + "    enum : myEnum[1];  \n"
            + "    enums : myEnum[*]; \n"
            + "}  \n"
            + "Class D \n"
            + "{  \n"
            + "   name : String[1];\n"
            + "}                   \n"
            + "                    \n"
            + "Class E \n"
            + "{  \n"
            + "   handler : ConstraintsOverride[0..1];\n"
            + "}                   \n"
            + "                    \n"
            + "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*] \n"
            + "{\n"
            + "    [^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne()), ^D(name = $o->cast(@A).b->toOne() + $o->getHiddenPayload()->cast(@String)->toOne())]  \n"
            + "}  \n"
            + "   \n"
            + "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1] \n"
            + "{ \n"
            + "   ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());   \n"
            + "} ";

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("defaultValueSource.pure");
        runtime.delete("/test/testModel.pure");
        runtime.compile();
    }

    @Test
    public void testSimpleClassDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[1] \n{"
                        + "let a = A;  \n"
                        + "let r = dynamicNew($a,  \n"
                        + " [^KeyValue(key='a',value='rrr'),\n"
                        + "  ^KeyValue(key='b',value='eee'),\n"
                        + "  ^KeyValue(key='c',value=['zzz','kkk']),\n"
                        + "  ^KeyValue(key='enum',value=myEnum.A),\n"
                        + "  ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + " getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,getterOverrideToMany_Any_1__Property_1__Any_MANY_, '2')->cast(@A);\n"
                        + " assert('2' == $r->getHiddenPayload(), |'');\n"
                        + " assert(['rrr2','eee2'] == $r.ds.name, |'');\n"
                        + "}\n"
        );
    }

    @Test
    public void testSimpleGenericDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(^GenericType(rawType=A), \n"
                        + " [ ^KeyValue(key='a',value='rrr'), \n"
                        + "   ^KeyValue(key='b',value='eee'), \n"
                        + "   ^KeyValue(key='c',value=['zzz','kkk']), \n"
                        + "   ^KeyValue(key='enum',value=myEnum.A), \n"
                        + "   ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + "  getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,getterOverrideToMany_Any_1__Property_1__Any_MANY_,'2')->cast(@A) ;\n"
                        + "   assert('2' == $r->getHiddenPayload(), |'');\n"
                        + "   assert(['rrr2','eee2'] == $r.ds.name, |'');\n"
                        + "}\n");
    }


    @Test
    public void testSimpleClassDynamicNewWithConstraintOverrideOnly()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function constraintsManager(o:Any[1]):Any[1]\n"
                        + "{\n"
                        + "  $o;\n"
                        + "}"
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(A, \n"
                        + " [ ^KeyValue(key='a',value='eee'), \n"
                        + "   ^KeyValue(key='b',value='eee'), \n"
                        + "   ^KeyValue(key='d',value=^D(name='rrr2')),"
                        + "   ^KeyValue(key='ds',value=[^D(name='rrr2'),^D(name='eee2')]),"
                        + "   ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])],\n"
                        + "  [],[],[],constraintsManager_Any_1__Any_1_)->cast(@A) ;\n"
                        + "   assert([] == $r->getHiddenPayload(), |'');\n"
                        + "   assert('eee' == $r.a, |'');\n"
                        + "   assert('rrr2' == $r.d.name, |'');\n"
                        + "   assertSameElements(['rrr2','eee2'], $r.ds.name);\n"
                        + "}\n");
    }

    @Test
    public void testSimpleGenericDynamicNewWithConstraintOverrideOnly()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function constraintsManager(o:Any[1]):Any[1]\n"
                        + "{\n"
                        + "  $o;\n"
                        + "}"
                        + "function test():Any[1] \n{"
                        + "let r = dynamicNew(^GenericType(rawType=A), \n"
                        + " [ ^KeyValue(key='a',value='eee'), \n"
                        + "   ^KeyValue(key='b',value='eee')], \n"
                        + "  [],[],[],constraintsManager_Any_1__Any_1_)->cast(@A) ;\n"
                        + "   assert([] == $r->getHiddenPayload(), |'');\n"
                        + "   assert('eee' == $r.a, |'');\n"
                        + "}\n");
    }

    @Test
    public void testPrinting()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + " let r = dynamicNew(A, \n"
                        + "     [ ^KeyValue(key='a',value='rrr'), \n"
                        + "         ^KeyValue(key='b',value='eee'), \n"
                        + "         ^KeyValue(key='d',value=^D(name='rrr2')),"
                        + "         ^KeyValue(key='ds',value=[^D(name='rrr2'),^D(name='eee2')]),"
                        + "         ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])]);\n"
                        + " print($r, 1);\n"
                        + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void cyclicalReferencesAreNotImplicit()
    {
        compileTestSource("testSource.pure",
                "Class F \n"
                        + "{            \n"
                        + "   str : String[1]; \n"
                        + "   g : G[1]; \n"
                        + "}            \n"
                        + "Class G      \n"
                        + "{            \n"
                        + "   f : F[1]; \n"
                        + "}            \n"
                        + "function test():Any[*] \n"
                        + "{"
                        + " let f = dynamicNew(F, \n"
                        + "     [ ^KeyValue(key='str',value='foo') ] ) -> cast(@F);\n"
                        + " let g = dynamicNew(G, \n"
                        + "     [ ^KeyValue(key='f',value=$f) ]) -> cast(@G);\n"
                        + " assert($f.g == [], |'');\n"
                        + " assert($g.f == $f, |'');\n"
                        + " assert($g.f.g == [], |'');\n"
                        + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void cyclicalReferencesAreImplicitWhenFromAssociations()
    {
        compileTestSource("testSource.pure",
                "Class H        \n"
                        + "{            \n"
                        + "   str : String[1]; \n"
                        + "}            \n"
                        + "Class I      \n"
                        + "{}            \n"
                        + "Association assoc \n"
                        + "{"
                        + "    i : I[1];   \n"
                        + "    h : H[1];   \n"
                        + "}"
                        + "function test():Any[*] \n"
                        + "{"
                        + "    let h = dynamicNew(H, \n"
                        + "        [ ^KeyValue(key='str',value='foo') ] ) -> cast(@H);\n"
                        + "    let i = dynamicNew(I, \n"
                        + "        [ ^KeyValue(key='h',value=$h) ]) -> cast(@I);\n"
                        + "    assert($h.i == $i, |'');\n"
                        + "    assert($i.h == $h, |'');\n"
                        + "    assert($i.h.i == $i, |'');\n"
                        + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDynamicNewWithZeroToOneAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                "import test::*;\n" +
                        "Class test::TestClassA\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClassB\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::TestAssocAB\n" +
                        "{\n" +
                        "  toB : TestClassB[0..1];\n" +
                        "  toA : TestClassA[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let a = dynamicNew(TestClassA, [^KeyValue(key='name', value='A'), ^KeyValue(key='toB', value=[])])->cast(@TestClassA);\n" +
                        "  assert('A' == $a.name, |'');\n" +
                        "  assert($a.toB->isEmpty(), |'');\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDynamicNewWithZeroToManyAssociationExplicitNull()
    {
        compileTestSource("/test/testModel.pure",
                "import test::*;\n" +
                        "Class test::TestClassA\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClassB\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Association test::TestAssocAB\n" +
                        "{\n" +
                        "  toB : TestClassB[*];\n" +
                        "  toA : TestClassA[1];\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  let a = dynamicNew(TestClassA, [^KeyValue(key='name', value='A'), ^KeyValue(key='toB', value=[])])->cast(@TestClassA);\n" +
                        "  assert('A' == $a.name, |'');\n" +
                        "  assert($a.toB->isEmpty(), |'');\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithDynamicNew()
    {
        compileTestSource("testSource.pure", AbstractTestDefaultValue.DECLARATION
                + "function test():Any[*] \n{"
                + " let r = dynamicNew(A, \n"
                + "     [ ^KeyValue(key='stringProperty',value='dynamicNew')]);\n"
                + " assertEquals(0.12, $r->cast(@A).inheritProperty);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testDefaultValueWithKeyValuePassedAsVariableToDynamicNew()
    {
        compileTestSource("testSource.pure", AbstractTestDefaultValue.DECLARATION
                + "function test():Any[*] \n{"
                + " let a = ^KeyValue(key='stringProperty',value='variable');"
                + " let b = ^KeyValue(key='enumProperty',value=EnumWithDefault.AnotherValue);"
                + " let r = dynamicNew(A, \n"
                + "     [ $a, $b ]);\n"
                + " print($r, 1);\n"
                + "}\n");
        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(org.eclipse.collections.impl.factory.Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories()));
        CodeRepository test = new TestCodeRepositoryWithDependencies("test", null, repositories.detect(x -> x.getName().equals("platform")), repositories.detect(x -> x.getName().equals("platform_functions")));
        repositories.add(test);
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(repositories));
    }
}
