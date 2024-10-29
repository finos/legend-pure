// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.interpreted.runtime.dynamicTypePropagation;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDynamicTypeTracking extends AbstractPureTestWithCoreCompiled
{
    static FunctionExecutionInterpreted functionExecution = new FunctionExecutionInterpreted();

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testPropertyContravariance()
    {
        compileTestSource("fromString.pure",
                "Class X{name:String[1];}" +
                        "Class Y{z:String[1];}" +
                        "function process(c:Class<Any>[*]):Pair<Class<Any>, Property<Nil,Any|*>>[*]\n" +
                        "{\n" +
                        "  $c->map(x|$x.properties->map(w|pair($x,$w)));\n" +
                        "}" +
                        "function z():Boolean[1]" +
                        "{" +
                        " [X,Y]->process();" +
                        " true;" +
                        "}");

        CoreInstance func = runtime.getFunction("z():Boolean[1]");
        Assert.assertNotNull(func);

        try
        {
            functionExecution.start(func, Lists.immutable.with());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //assertPureException(PureExecutionException.class, "Error executing the function:testFn(String[1], String[1]):String[1]. Mismatch between the number of function parameters (2) and the number of supplied arguments (0)\n", e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return functionExecution;
    }
}
