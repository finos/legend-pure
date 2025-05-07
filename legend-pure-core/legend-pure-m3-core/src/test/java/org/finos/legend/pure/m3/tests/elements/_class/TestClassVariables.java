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


package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClassVariables extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testClassVariable()
    {
        compileTestSource("fromString.pure",
                "Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $x,\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   ret(){$x} : Integer[1];\n" +
                        "   values : U[1];\n" +
                        "}");
        Assert.assertEquals("x Integer", ((Class<?>) runtime.getCoreInstance("test::List"))._typeVariables().collect(x -> x._name() + " " + GenericType.print(x._genericType(), true, processorSupport)).makeString(", "));
    }

    @Test
    public void testClassVariables()
    {
        compileTestSource("fromString.pure",
                "Class test::List(x:Integer[1], y:Number[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $y,\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   ret(){$x} : Integer[1];\n" +
                        "   values : U[1];\n" +
                        "}");
        Assert.assertEquals("x Integer, y Number", ((Class<?>) runtime.getCoreInstance("test::List"))._typeVariables().collect(x -> x._name() + " " + GenericType.print(x._genericType(), true, processorSupport)).makeString(", "));
    }

    @Test
    public void testClassVariableConstraintError()
    {
        assertCompileError("Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $xx\n" +
                        "]\n" +
                        "{\n" +
                        "   values : U[1];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:3 column:28), \"The variable 'xx' is unknown!\""
        );
    }

    @Test
    public void testClassVariableConstraintMessageError()
    {
        assertCompileError("Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $xx->toString())" +
                        "]\n" +
                        "{\n" +
                        "   values : U[1];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:3 column:68), \"The variable 'xx' is unknown!\""
        );
    }

    @Test
    public void testClassVariableQualifiedProperty()
    {
        assertCompileError("Class test::List(x:Integer[1])<U>\n" +
                        "{\n" +
                        "   ret(){$xx} :Integer[1];\n" +
                        "}",
                "Compilation error at (resource:fromString.pure line:3 column:11), \"The variable 'xx' is unknown!\""
        );
    }

    public static void assertCompileError(String code, String message)
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure", code));
        Assert.assertEquals(message, e.getMessage());
    }
}
