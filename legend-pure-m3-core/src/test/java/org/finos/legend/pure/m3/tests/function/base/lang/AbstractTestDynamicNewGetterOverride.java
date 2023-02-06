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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestDynamicNewGetterOverride extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimple()
    {
        compileTestSource("fromString.pure","Enum myEnum{A,B}" +
                "Class A\n" +
                "{\n" +
                "   a: String[1];\n" +
                "   b: String[0..1];\n" +
                "   c: String[*];" +
                "   d : D[0..1];" +
                "   ds : D[*];" +
                "   enum : myEnum[1];\n" +
                "   enums : myEnum[*];\n" +
                "}" +
                "" +
                "Class D" +
                "{" +
                "   name : String[1];" +
                "}" +
                "\n" +
                "\n" +
                "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                "{\n " +
                "  let payload = $o->getHiddenPayload()->cast(@String)->toOne();\n " +
                "  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];\n" +
                "}\n" +
                "\n" +
                "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                "{\n" +
                "  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "  let a = A;\n" +
                "\n" +
                "\n" +
                "  let r = dynamicNew($a,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='a',value='rrr'),\n" +
                "                      ^KeyValue(key='b',value='eee'),\n" +
                "                      ^KeyValue(key='c',value=['zzz','kkk']),\n" +
                "                      ^KeyValue(key='enum',value=myEnum.A),\n" +
                "                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2'\n" +
                "                  )->cast(@A);\n" +
                "\n" +
                "   assert('2' == Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->toOne()->cast(@GetterOverride).hiddenPayload, |'');\n" +
                "   assert('2' == $r.elementOverride->toOne()->cast(@GetterOverride).hiddenPayload, |'');\n" +
                "   assert('2' == $r->getHiddenPayload(), |'');\n" +
                "   // DataType [1]\n" +
                "   assert('rrr' == $r.a, |'');\n" +
                "   assert('rrr' == ^$r().a, |'');\n" +
                "   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');\n" +
                "   // DataType [0..1]\n" +
                "   assert('eee' == $r.b, |'');\n" +
                "   assert('eee' == ^$r().b, |'');\n" +
                "   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');\n" +
                "   // DataType [*]\n" +
                "   assert(['zzz', 'kkk'] == $r.c, |'');\n" +
                "   assert(['zzz', 'kkk'] == ^$r().c, |'');\n" +
                "   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');\n" +
                "   // Class [1]\n" +
                "   assert('rrr2' == $r.d.name, |'');\n" +
                "   assert('rrr2' == A->classPropertyByName('d')->toOne()->eval($r)->cast(@D).name, |'');\n" +
                "   assert('rrr2' == ^$r().d.name, |'');\n" +
                "   // Class [*]\n" +
                "   assert(['rrr2','eee2'] == $r.ds.name, |'');\n" +
                "   assert(['rrr2','eee2'] == A->classPropertyByName('ds')->toOne()->eval($r)->cast(@D).name, |'');\n" +
                "   assert(['rrr2','eee2'] == ^$r().ds.name, |'');\n" +
                "   // Enum [1]\n" +
                "   assert(myEnum.A == $r.enum, |'');\n" +
                "   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');\n" +
                "   assert(myEnum.A == ^$r().enum, |'');\n" +
                "   // Enum [*]\n" +
                "   assert([myEnum.A,myEnum.B] == $r.enums, |'');\n" +
                "   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');\n" +
                "   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }

    @Test
    public void testRemoveOverride()
    {
        compileTestSource("fromString.pure","Enum myEnum{A,B}" +
                "Class A\n" +
                "{\n" +
                "   a: String[1];\n" +
                "   b: String[0..1];\n" +
                "   c: String[*];" +
                "   d : D[0..1];" +
                "   ds : D[*];" +
                "   enum : myEnum[1];\n" +
                "   enums : myEnum[*];\n" +
                "}" +
                "" +
                "Class D" +
                "{" +
                "   name : String[1];" +
                "}" +
                "\n" +
                "\n" +
                "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                "{\n " +
                "  let payload = $o->getHiddenPayload()->cast(@String)->toOne();\n " +
                "  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];\n" +
                "}\n" +
                "\n" +
                "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                "{\n" +
                "  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());\n" +
                "}\n" +
                "\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "  let a = A;\n" +
                "  let or = dynamicNew($a,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='a',value='rrr'),\n" +
                "                      ^KeyValue(key='b',value='eee'),\n" +
                "                      ^KeyValue(key='c',value=['zzz','kkk']),\n" +
                "                      ^KeyValue(key='enum',value=myEnum.A),\n" +
                "                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2'\n" +
                "                  )->cast(@A);\n" +
                "   let r = $or->removeOverride();\n" +
                "   assert(Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->isEmpty(), |'');\n" +
                "   // DataType [1]\n" +
                "   assert('rrr' == $r.a, |'');\n" +
                "   assert('rrr' == ^$r().a, |'');\n" +
                "   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');\n" +
                "   // DataType [0..1]\n" +
                "   assert('eee' == $r.b, |'');\n" +
                "   assert('eee' == ^$r().b, |'');\n" +
                "   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');\n" +
                "   // DataType [*]\n" +
                "   assert(['zzz', 'kkk'] == $r.c, |'');\n" +
                "   assert(['zzz', 'kkk'] == ^$r().c, |'');\n" +
                "   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');\n" +
                "   // Class [1]\n" +
                "   assert($r.d->isEmpty(), |'');\n" +
                "   assert(A->classPropertyByName('d')->toOne()->eval($r)->isEmpty(), |'');\n" +
                "   assert(^$r().d->isEmpty(), |'');\n" +
                "   // Class [*]\n" +
                "   assert($r.ds->isEmpty(), |'');\n" +
                "   assert(A->classPropertyByName('ds')->toOne()->eval($r)->isEmpty(), |'');\n" +
                "   assert(^$r().ds->isEmpty(), |'');\n" +
                "   // Enum [1]\n" +
                "   assert(myEnum.A == $r.enum, |'');\n" +
                "   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');\n" +
                "   assert(myEnum.A == ^$r().enum, |'');\n" +
                "   // Enum [*]\n" +
                "   assert([myEnum.A,myEnum.B] == $r.enums, |'');\n" +
                "   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');\n" +
                "   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }


    @Test
    public void testRemoveOverrideWithMay()
    {
        compileTestSource("fromString.pure","Enum myEnum{A,B}" +
                "Class A\n" +
                "{\n" +
                "   a: String[1];\n" +
                "   b: String[0..1];\n" +
                "   c: String[*];" +
                "   d : D[0..1];" +
                "   ds : D[*];" +
                "   enum : myEnum[1];\n" +
                "   enums : myEnum[*];\n" +
                "}" +
                "" +
                "Class D" +
                "{" +
                "   name : String[1];" +
                "}" +
                "\n" +
                "\n" +
                "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                "{\n " +
                "  let payload = $o->getHiddenPayload()->cast(@String)->toOne();\n " +
                "  [^D(name = $o->cast(@A).a + $payload), ^D(name = $o->cast(@A).b->toOne() + $payload)];\n" +
                "}\n" +
                "\n" +
                "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                "{\n" +
                "  ^D(name = $o->cast(@A).a + $o->getHiddenPayload()->cast(@String)->toOne());\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::lang::mayRemoveOverride<T>(value:T[0..1]):T[0..1]\n" +
                "{\n" +
                "   if ($value->isEmpty(),|$value,|$value->toOne()->removeOverride());\n" +
                "}\n" +
                "" +
                "function test():Any[*]\n" +
                "{\n" +
                "  let a = A;\n" +
                "  let or = dynamicNew($a,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='a',value='rrr'),\n" +
                "                      ^KeyValue(key='b',value='eee'),\n" +
                "                      ^KeyValue(key='c',value=['zzz','kkk']),\n" +
                "                      ^KeyValue(key='enum',value=myEnum.A),\n" +
                "                      ^KeyValue(key='enums',value=[myEnum.A, myEnum.B])\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2'\n" +
                "                  )->cast(@A);\n" +
                "   let r = $or->mayRemoveOverride()->toOne();\n" +
                "   assert(Any->classPropertyByName('elementOverride')->toOne()->rawEvalProperty($r)->isEmpty(), |'');\n" +
                "   // DataType [1]\n" +
                "   assert('rrr' == $r.a, |'');\n" +
                "   assert('rrr' == ^$r().a, |'');\n" +
                "   assert('rrr' == A->classPropertyByName('a')->toOne()->eval($r), |'');\n" +
                "   // DataType [0..1]\n" +
                "   assert('eee' == $r.b, |'');\n" +
                "   assert('eee' == ^$r().b, |'');\n" +
                "   assert('eee' == A->classPropertyByName('b')->toOne()->eval($r), |'');\n" +
                "   // DataType [*]\n" +
                "   assert(['zzz', 'kkk'] == $r.c, |'');\n" +
                "   assert(['zzz', 'kkk'] == ^$r().c, |'');\n" +
                "   assert(['zzz', 'kkk'] == A->classPropertyByName('c')->toOne()->eval($r), |'');\n" +
                "   // Class [1]\n" +
                "   assert($r.d->isEmpty(), |'');\n" +
                "   assert(A->classPropertyByName('d')->toOne()->eval($r)->isEmpty(), |'');\n" +
                "   assert(^$r().d->isEmpty(), |'');\n" +
                "   // Class [*]\n" +
                "   assert($r.ds->isEmpty(), |'');\n" +
                "   assert(A->classPropertyByName('ds')->toOne()->eval($r)->isEmpty(), |'');\n" +
                "   assert(^$r().ds->isEmpty(), |'');\n" +
                "   // Enum [1]\n" +
                "   assert(myEnum.A == $r.enum, |'');\n" +
                "   assert(myEnum.A == A->classPropertyByName('enum')->toOne()->eval($r), |'');\n" +
                "   assert(myEnum.A == ^$r().enum, |'');\n" +
                "   // Enum [*]\n" +
                "   assert([myEnum.A,myEnum.B] == $r.enums, |'');\n" +
                "   assert([myEnum.A,myEnum.B] == A->classPropertyByName('enums')->toOne()->eval($r), |'');\n" +
                "   assert([myEnum.A,myEnum.B] == ^$r().enums, |'');\n" +
                "}");
        this.compileAndExecute("test():Any[*]");
    }
}
