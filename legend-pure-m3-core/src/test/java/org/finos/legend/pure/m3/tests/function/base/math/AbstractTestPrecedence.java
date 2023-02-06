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

package org.finos.legend.pure.m3.tests.function.base.math;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Test;

public abstract class AbstractTestPrecedence extends AbstractPureTestWithCoreCompiled {

    @Test
    public void testBasic() {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(14 == 2+3*4, |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testWithTimes() {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(32 == (2+3*4) + 3*2*2 + 1*6, |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testWithTimesAndDivide() {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(12.0 == (2+3*4/2) + 12/2/2/3 + 1*6/2, |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testWithTimesAndRelational() {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == 1 + 2 * 3 > 4 * 5 + 6, |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }

    @Test
    public void testWithTimesAndRelationalComplex() {
        compileTestSource("fromString.pure",
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "   assert(false == 1 + 2 + 3*4/5 -9 > 1 + 2*2, |'');\n" +
                        "}\n");
        this.execute("test():Boolean[1]");
    }
}
