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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestMapCollection extends AbstractPureTestWithCoreCompiled
{
    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testGetIfAbsentPutWithKey()
    {
        compileTestSource("fromString.pure",
                "function testGetIfAbsentPutWithKey():Any[*]\n" +
                        "{\n" +
                        "   let m = newMap([pair(1,'_1'), pair(2,'_2')]);\n" +
                        "   assert($m->get(3)->isEmpty(), |'');\n" +
                        "   assert('_3' == $m->getIfAbsentPutWithKey(3, {k:Integer[1]|'_'+$k->toString()}), |'');\n" +
                        "   assert('_3' == $m->get(3), |'');" +
                        "}\n");

        this.execute("testGetIfAbsentPutWithKey():Any[*]");
    }

    @Test
    public void testReplaceAll()
    {
        compileTestSource("fromString.pure",
                "Class my::Class1" +
                        "{" +
                        "   <<equality.Key>> a:Integer[1];" +
                        "   b:Integer[0..1];" +
                        "}" +
                        "" +
                        "function testMapBehaviour():Any[*]\n" +
                        "{\n" +
                        "   let m = newMap([pair(^my::Class1(a = 1),'_1'), pair(^my::Class1(a = 2),'_2')]);\n" +
                        "   let m2 = $m->replaceAll([pair(^my::Class1(a = 1),'_1_1'), pair(^my::Class1(a = 3),'_3')]);\n" +

                        "   assert($m->keyValues()->size() == 2);\n" +
                        "   assert($m2->keyValues()->size() == 2);\n" +

                        "   assert($m->get(^my::Class1(a = 1, b = 1)) == '_1');\n" +
                        "   assert($m->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +

                        "   assert($m2->get(^my::Class1(a = 1, b = 1)) == '_1_1');\n" +
                        "   assert($m2->get(^my::Class1(a = 3, b = 1)) == '_3');\n" +
                        "}\n");

        this.execute("testMapBehaviour():Any[*]");
    }

    @Test
    public void testMapPut()
    {
        compileTestSource("fromString.pure",
                "Class my::Class1" +
                        "{" +
                        "   <<equality.Key>> a:Integer[1];" +
                        "   b:Integer[0..1];" +
                        "}" +
                        "" +
                        "function testMapBehaviour():Any[*]\n" +
                        "{\n" +
                        "   let m = newMap([pair(^my::Class1(a = 1),'_1'), pair(^my::Class1(a = 2),'_2')]);\n" +
                        "   let m2 = $m->put(^my::Class1(a = 3), '_3');\n" +
                        "   let m3 = $m->put(^my::Class1(a = 4), '_4');\n" +
                        "   let m4 = $m3->put(^my::Class1(a = 1, b = 1), '_1_1');\n" + //put in a replacement

                        "   assert($m->keyValues()->size() == 2);\n" +
                        "   assert($m2->keyValues()->size() == 3);\n" +
                        "   assert($m3->keyValues()->size() == 3);\n" +
                        "   assert($m4->keyValues()->size() == 3);\n" +

                        "   assert($m->get(^my::Class1(a = 1, b = 1)) == '_1');\n" +
                        "   assert($m->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +

                        "   assert($m2->get(^my::Class1(a = 1, b = 1)) == '_1');\n" +
                        "   assert($m2->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +
                        "   assert($m2->get(^my::Class1(a = 3, b = 1)) == '_3');\n" +

                        "   assert($m3->get(^my::Class1(a = 1, b = 1)) == '_1');\n" +
                        "   assert($m3->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +
                        "   assert($m3->get(^my::Class1(a = 4, b = 1)) == '_4');\n" +

                        "   assert($m4->get(^my::Class1(a = 1)) == '_1_1');\n" +
                        "   assert($m4->get(^my::Class1(a = 2)) == '_2');\n" +
                        "   assert($m4->get(^my::Class1(a = 4)) == '_4');\n" +
                        "}\n");

        this.execute("testMapBehaviour():Any[*]");
    }

    @Test
    public void testMapPutAll()
    {
        compileTestSource("fromString.pure",
                "Class my::Class1\n" +
                        "{\n" +
                        "   <<equality.Key>> a:Integer[1];\n" +
                        "   b:Integer[0..1];\n" +
                        "}\n" +
                        "" +
                        "function testMapBehaviour():Any[*]\n" +
                        "{\n" +
                        "   let m = newMap([pair(^my::Class1(a = 1),'_1'), pair(^my::Class1(a = 2),'_2')]);\n" +
                        "   let m2 = newMap([pair(^my::Class1(a = 1, b = 1),'_1_1'), pair(^my::Class1(a = 4),'_4')]);\n" +

                        "   let m3 = $m->putAll($m2);\n" +
                        "   let m4 = $m3->putAll([pair(^my::Class1(a = 2),'_2'), pair(^my::Class1(a = 3),'_3')]);\n" +

                        "   assert($m->keyValues()->size() == 2);\n" +
                        "   assert($m2->keyValues()->size() == 2);\n" +
                        "   assert($m3->keyValues()->size() == 3);\n" +
                        "   assert($m4->keyValues()->size() == 4);\n" +

                        "   assert($m->get(^my::Class1(a = 1, b = 1)) == '_1');\n" +
                        "   assert($m->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +

                        "   assert($m2->get(^my::Class1(a = 1, b = 1)) == '_1_1');\n" +
                        "   assert($m2->get(^my::Class1(a = 4, b = 1)) == '_4');\n" +

                        "   assert($m3->get(^my::Class1(a = 1, b = 1)) == '_1_1');\n" +
                        "   assert($m3->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +
                        "   assert($m3->get(^my::Class1(a = 4, b = 1)) == '_4');\n" +

                        "   assert($m4->get(^my::Class1(a = 1, b = 1)) == '_1_1');\n" +
                        "   assert($m4->get(^my::Class1(a = 2, b = 1)) == '_2');\n" +
                        "   assert($m4->get(^my::Class1(a = 3, b = 1)) == '_3');\n" +
                        "   assert($m4->get(^my::Class1(a = 4, b = 1)) == '_4');\n" +
                        "}\n");

        this.execute("testMapBehaviour():Any[*]");
    }

    @Test
    public void testGetMapStats()
    {
        compileTestSource("fromString.pure",
                "function testGetMapStats():Any[*]\n" +
                        "{\n" +
                        "   let m = newMap([pair(1,'_1'), pair(2,'_2')]);\n" +
                        "   assert($m->get(3)->isEmpty(), |'');\n" +
                        "   assert(0 == $m->getMapStats().getIfAbsentCounter, |'');\n" +
                        "   assert('_3'== $m->getIfAbsentPutWithKey(3, {k:Integer[1]|'_'+$k->toString()}), |'');\n" +
                        "   assert(1== $m->getMapStats().getIfAbsentCounter, |'');\n" +
                        "   assert('_3'== $m->get(3), |'');" +
                        "   assert(1== $m->getMapStats().getIfAbsentCounter, |'');\n" +
                        "}\n");

        this.execute("testGetMapStats():Any[*]");
    }
}
