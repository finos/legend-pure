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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

public abstract class AbstractTestMapZeroOne extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testSimple() throws Exception
    {
        compileTestSource("Class Ok\n" +
                "{\n" +
                "  other : Other[0..1]; \n" +
                "}\n" +
                "\n" +
                "Class Other\n" +
                "{\n" +
                "   value : X[0..1];\n" +
                "}" +
                "Class X" +
                "{" +
                "   val : String[1];" +
                "}" +
                "" +
                "function re():Ok[1]" +
                "{" +
                "   ^Ok()" +
                "}\n" +
                "function do():Any[*]\n" +
                "{\n" +
                "   print(re().other->map(o|$o.value)->map(z|$z.val), 1);" +
                "}");
        CoreInstance func = this.runtime.getFunction("do():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());

    }
}
