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

package org.finos.legend.pure.runtime.java.compiled.constraints;

import org.finos.legend.pure.generated.CoreJavaModelFactoryRegistry;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.tests.constraints.AbstractTestConstraints;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConstraints extends AbstractTestConstraints
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getFunctionExecution(), getFactoryRegistryOverride());
        runtime.createInMemorySource("employee.pure", "Class Employee" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("/test/source1.pure");
        runtime.delete("/test/source2.pure");
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testInheritanceInSeparateFile()
    {
        String source1Name = "/test/source1.pure";
        String source1Code = "Class test::SuperClass\n" +
                "[\n" +
                "  nameNotEmpty: $this.name->length() > 0\n" +
                "]\n" +
                "{\n" +
                "  name : String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::OtherClass\n" +
                "[\n" +
                "  otherNameNotEmpty: $this.otherName->length() > 0\n" +
                "]\n" +
                "{\n" +
                "  otherName : String[1];\n" +
                "}\n";
        String source2Name = "/test/source2.pure";
        String source2Code = "Class test::SubClass extends test::SuperClass\n" +
                "{\n" +
                "}\n" +
                "\n" +
                "function test::testNew():Any[*]\n" +
                "{\n" +
                "  ^test::SubClass(name='')\n" +
                "}\n";
        // The two sources must be compiled together to test the issue
        this.runtime.createInMemoryAndCompile(Tuples.pair(source1Name, source1Code), Tuples.pair(source2Name, source2Code));
        try
        {
            this.execute("test::testNew():Any[*]");
            Assert.fail("This should fail constraint validation");
        }
        catch (Exception e)
        {
            assertPureException(PureExecutionException.class, "Constraint :[nameNotEmpty] violated in the Class SuperClass", "/test/source2.pure", 7, 3, e);
        }
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }

    protected static CoreInstanceFactoryRegistry getFactoryRegistryOverride()
    {
        return CoreJavaModelFactoryRegistry.REGISTRY;
    }
}
