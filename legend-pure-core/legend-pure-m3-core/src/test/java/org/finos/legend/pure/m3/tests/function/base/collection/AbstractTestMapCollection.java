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
import org.junit.Test;

public abstract class AbstractTestMapCollection extends AbstractPureTestWithCoreCompiled
{
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