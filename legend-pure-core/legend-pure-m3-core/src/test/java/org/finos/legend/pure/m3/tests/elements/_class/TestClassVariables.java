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

    @Test
    public void testTypeVariableNonExistentType()
    {
        assertCompileError(
                "Class test::List(x:NonExistentType[1])<U>\n" +
                        "{\n" +
                        "   values : U[1];\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:1 column:20), \"NonExistentType has not been defined!\"");
    }

    @Test
    public void testTypeVariableConflict()
    {
        assertCompileError(
                "Class test::List(x:Integer[1], x:Integer[1])<U>\n" +
                        "{\n" +
                        "   values : U[1];\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:1 column:32), \"Type variable 'x' is already defined (at fromString.pure:1c18)\"");
    }

    @Test
    public void testMissingTypeVariableInExtends()
    {
        assertCompileError(
                "Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $x,\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   ret(){$x} : Integer[1];\n" +
                        "   values : U[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::SubList(y:Integer[1])<V> extends test::List<V>\n" +
                        "[\n" +
                        "   $this.values->size() > $y\n" +
                        "]\n" +
                        "{\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:11 column:52), \"Type variable mismatch for test::List(x:Integer)<U> (expected 1, got 0): test::List<V>\"");
    }

    @Test
    public void testWrongVariableTypeInExtends()
    {
        assertCompileError(
                "Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $x,\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   ret(){$x} : Integer[1];\n" +
                        "   values : U[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::SubList(y:Integer[1])<V> extends test::List<V>(%2011-01-10)\n" +
                        "[\n" +
                        "   $this.values->size() > $y\n" +
                        "]\n" +
                        "{\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:11 column:52), \"Type variable type mismatch for test::List(x:Integer)<U> (expected Integer, got StrictDate): test::List(%2011-01-10)<V>\"");
    }

    @Test
    public void testVariableReferenceInSubclass()
    {
        // We should consider allowing this
        assertCompileError(
                "Class test::List(x:Integer[1])<U>\n" +
                        "[\n" +
                        "   $this.values->size() < $x,\n" +
                        "   bNotB(~function: $this.values->size() < $x ~message: 'error' + $x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   ret(){$x} : Integer[1];\n" +
                        "   values : U[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::SubList(y:Integer[1])<V> extends test::List<V>(%2011-01-10)\n" +
                        "[\n" +
                        "   $this.values->size() > $y,\n" +
                        "   $x < $y\n" +
                        "]\n" +
                        "{\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:14 column:5), \"The variable 'x' is unknown!\"");
    }

    @Test
    public void testExtendClassWithTypeVariable()
    {
        // We should consider allowing this
        // If we do, we need to add the below (or something like it) to the tests in new.pure
        // We would also need to consider variable name conflicts/overrides between super- and sub-classes
        // We would further need to consider the possibility of conflicting values being assigned to the same variable
        // via multiple inheritance.
        assertCompileError(
                "import test::*;\n" +
                        "\n" +
                        "Class test::MyClassWithTypeVariables(x:Integer[1])\n" +
                        "[\n" +
                        "   wx(~function:$this.text->size() < $x ~message:'Error '+$this.text->size()->toString()+' >= '+$x->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   x:Integer[1] = 0;\n" +
                        "   x(){$x}:Integer[1];\n" +
                        "   xPlusX(){$x + $this.x}:Integer[1];\n" +
                        "   res(){'1'+$x->toString()}:String[1];\n" +
                        "   res(z:String[1]){'1'+$x->toString()+$z}:String[1];\n" +
                        "   text : String[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::MySubClassWithTypeVariables(y:Integer[1]) extends MyClassWithTypeVariables(10)\n" +
                        "[\n" +
                        "   wy(~function:$this.text->size() > $y ~message:'Error '+$this.text->size()->toString()+' <= '+$y->toString())\n" +
                        "]\n" +
                        "{\n" +
                        "   y:Integer[1] = 0;\n" +
                        "   y(){$y}:Integer[1];\n" +
                        "   yPlusY(){$y + $this.y}:Integer[1];\n" +
                        "   yPlusX1(){$y + $this.x()}:Integer[1];\n" +
                        "   yPlusX2(){$y + $this.x}:Integer[1];\n" +
                        "   yPlusYPlusXPlusX(){$y + $this.y + $this.x() + $this.x}:Integer[1];\n" +
                        "   subres(){$this.res() + '2' + $y->toString()}:String[1];\n" +
                        "   subres(z:String[1]){$this.res($z) + '2' + $y->toString() + $z}:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function <<test.Test>> test::testNewWithTypeVariables():Any[*]\n" +
                        "{\n" +
                        "   assertEquals('110', ^MyClassWithTypeVariables(10)(text = ['a', 'b']).res());\n" +
                        "   assertEquals('110z', ^MyClassWithTypeVariables(10)(text = ['a', 'b']).res('z'));\n" +
                        "   assertEquals(10, ^MyClassWithTypeVariables(10)(text = ['a', 'b']).x());\n" +
                        "   assertEquals(10, ^MyClassWithTypeVariables(10)(text = ['a', 'b']).xPlusX());\n" +
                        "   assertEquals(10, ^MyClassWithTypeVariables(10)(text = ['a', 'b'], x = 5).x());\n" +
                        "   assertEquals(15, ^MyClassWithTypeVariables(10)(text = ['a', 'b'], x = 5).xPlusX());\n" +
                        "   assertError(|^MyClassWithTypeVariables(1)(text = ['a', 'b'], x = 5).res('z'), 'Constraint :[wx] violated in the Class MyClassWithTypeVariables, Message: Error 2 >= 1');\n" +
                        "\n" +
                        "   assertEquals('110', ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).res());\n" +
                        "   assertEquals('11021', ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).subres());\n" +
                        "   assertEquals('110z', ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).res('z'));\n" +
                        "   assertEquals('110z21z', ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).subres('z'));\n" +
                        "   assertEquals(10, ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).x());\n" +
                        "   assertEquals(10, ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).xPlusX());\n" +
                        "   assertEquals(15, ^MySubClassWithTypeVariables(1)(text = ['a', 'b'], x = 5).xPlusX());\n" +
                        "   assertEquals(1, ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).y());\n" +
                        "   assertEquals(1, ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).yPlusY());\n" +
                        "   assertEquals(8, ^MySubClassWithTypeVariables(1)(text = ['a', 'b'], y = 7).yPlusY());\n" +
                        "   assertEquals(11, ^MySubClassWithTypeVariables(1)(text = ['a', 'b']).yPlusX1());\n" +
                        "   assertEquals(11, ^MySubClassWithTypeVariables(1)(text = ['a', 'b'], x = 5).yPlusX1());\n" +
                        "   assertEquals(6, ^MySubClassWithTypeVariables(1)(text = ['a', 'b'], x = 5).yPlusX2());\n" +
                        "   assertEquals(23, ^MySubClassWithTypeVariables(1)(text = ['a', 'b'], x = 5, y = 7).yPlusYPlusXPlusX());\n" +
                        "   assertError(|^MyClassWithTypeVariables(5)(text = ['a', 'b']).res('z'), 'Constraint :[wy] violated in the Class MySubClassWithTypeVariables, Message: Error 2 <= 5');\n" +
                        "}\n",
                "Compilation error at (resource:fromString.pure line:16 column:63), \"Invalid generalization: test::MySubClassWithTypeVariables(y:Integer) cannot extend test::MyClassWithTypeVariables(10) as extending a class with type variables is not currently supported\"");
    }

    public static void assertCompileError(String code, String message)
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure", code));
        Assert.assertEquals(message, e.getMessage());
    }
}
