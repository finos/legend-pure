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

package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClassPropertyOverride extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("file.pure");
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimpleOverride()
    {
        assertCompiles(
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance propA = this.processorSupport.class_findPropertyUsingGeneralization(classA, "prop");
        CoreInstance propB = this.processorSupport.class_findPropertyUsingGeneralization(classB, "prop");
        Assert.assertNotSame(propA, propB);
    }

    @Test
    public void testSimpleOverrideWithStereotype()
    {
        assertCompiles(
                "Profile prof\n" +
                "{\n" +
                "  stereotypes: [st];" +
                "}\n" +
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  <<prof.st>> prop:String[1];\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance propA = this.processorSupport.class_findPropertyUsingGeneralization(classA, "prop");
        CoreInstance propB = this.processorSupport.class_findPropertyUsingGeneralization(classB, "prop");
        Assert.assertNotSame(propA, propB);
        Assert.assertEquals(0, Instance.getValueForMetaPropertyToManyResolved(propA, M3Properties.stereotypes, processorSupport).size());
        Assert.assertEquals(1, Instance.getValueForMetaPropertyToManyResolved(propB, M3Properties.stereotypes, processorSupport).size());
    }

    @Test
    public void testOverrideChangingType()
    {
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop:Integer[1];\n" +
                "}",
                "Property conflict on class B: property 'prop' defined on B conflicts with property 'prop' defined on A", 5, 7);
    }

    @Test
    public void testOverrideChangingMultiplicity()
    {
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop:String[*];\n" +
                "}",
                "Property conflict on class B: property 'prop' defined on B conflicts with property 'prop' defined on A", 5, 7);
    }

    @Test
    public void testSimpleQualifiedPropertyOverride()
    {
        assertCompiles(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x + $x\n" +
                "  }:String[1];\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance propA = _Class.findQualifiedPropertiesUsingGeneralization(classA, "prop", this.processorSupport).getFirst();
        CoreInstance propB = _Class.findQualifiedPropertiesUsingGeneralization(classB, "prop", this.processorSupport).getFirst();
        Assert.assertNotSame(propA, propB);
    }

    @Test
    public void testQualifiedPropertyOverrideWithValidDifferentArgType()
    {
        assertCompiles(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop(x:Any[1])\n" +
                "  {\n" +
                "    $x->toString()\n" +
                "  }:String[1];\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance propA = _Class.findQualifiedPropertiesUsingGeneralization(classA, "prop", this.processorSupport).getFirst();
        CoreInstance propB = _Class.findQualifiedPropertiesUsingGeneralization(classB, "prop", this.processorSupport).getFirst();
        Assert.assertNotSame(propA, propB);
    }

    @Test
    // TODO delete
    public void testQualifiedPropertyOverrideWithInvalidDifferentArgType()
    {
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop(x:Float[1])\n" +
                "  {\n" +
                "    $x->toString()\n" +
                "  }:String[1];\n" +
                "}",
                "Property conflict on class B: property 'prop' defined on B conflicts with property 'prop' defined on A", 8, 7);
    }

    @Test
    public void testQualifiedPropertyOverrideWithValidDifferentReturnType()
    {
        assertCompiles(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:Any[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x\n" +
                "  }:String[1];\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance propA = _Class.findQualifiedPropertiesUsingGeneralization(classA, "prop", this.processorSupport).getFirst();
        CoreInstance propB = _Class.findQualifiedPropertiesUsingGeneralization(classB, "prop", this.processorSupport).getFirst();
        Assert.assertNotSame(propA, propB);
    }

    @Test
    public void testQualifiedPropertyOverrideWithInvalidDifferentReturnType()
    {
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:String[1];\n" +
                "}\n" +
                "Class B extends A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x\n" +
                "  }:Any[1];\n" +
                "}",
                "Property conflict on class B: property 'prop' defined on B conflicts with property 'prop' defined on A", 8, 7);
    }

    @Test
    public void testValidImplicitOverrideViaGeneralization()
    {
        assertCompiles(
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class C extends A, B\n" +
                "{\n" +
                "}");
        CoreInstance classA = this.runtime.getCoreInstance("A");
        CoreInstance classB = this.runtime.getCoreInstance("B");
        CoreInstance classC = this.runtime.getCoreInstance("C");
        CoreInstance propA = this.processorSupport.class_findPropertyUsingGeneralization(classA, "prop");
        CoreInstance propB = this.processorSupport.class_findPropertyUsingGeneralization(classB, "prop");
        CoreInstance propC = this.processorSupport.class_findPropertyUsingGeneralization(classC, "prop");
        Assert.assertNotSame(propA, propB);
        Assert.assertSame(propA, propC);
    }

    @Test
    public void testInvalidImplicitOverrideViaGeneralization()
    {
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop:String[1];\n" +
                "}\n" +
                "Class B\n" +
                "{\n" +
                "  prop:Integer[1];\n" +
                "}\n" +
                "Class C extends A, B\n" +
                "{\n" +
                "}",
                "Property conflict on class C: property 'prop' defined on A conflicts with property 'prop' defined on B", 9, 7);
    }

    @Test
    public void testSimplePropertyConflictWithinClass()
    {
        // We don't check the compilation error message here as this may fail to compile for more than one reason.
//        assertCompilationException(
//                "Class A\n" +
//                "{\n" +
//                "  prop:String[1];\n" +
//                "  prop:String[1];\n" +
//                "}",
//                "Compilation error at (resource:fromString.pure line:1 column:7), \"Property conflict on class A: property 'prop' defined more than once\"");
        assertCompilationException(
                "Class A\n" +
                        "{\n" +
                        "  prop:String[1];\n" +
                        "  prop:String[1];\n" +
                        "}");
    }
    
    @Test
    public void testQualifiedPropertyConflictWithinClass()
    {
        // We don't check the compilation error message here as this may fail to compile for more than one reason.
//        assertCompilationException(
//                "Class A\n" +
//                "{\n" +
//                "  prop(x:String[1])\n" +
//                "  {\n" +
//                "    $x + $x\n" +
//                "  }:String[1];\n" +
//                "  prop(x:String[1])\n" +
//                "  {\n" +
//                "    $x\n" +
//                "  }:String[1];\n" +
//                "}",
//                "Property conflict on class A: qualified property 'prop' defined more than once", 1, 7);
        assertCompilationException(
                "Class A\n" +
                "{\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x + $x\n" +
                "  }:String[1];\n" +
                "  prop(x:String[1])\n" +
                "  {\n" +
                "    $x\n" +
                "  }:String[1];\n" +
                "}");
    }

    private void assertCompiles(String code)
    {
        compileTestSource("file.pure",code);
    }

    private void assertCompilationException(String code)
    {
        assertCompilationException(code, null);
    }

    private void assertCompilationException(String code, String expectedMessage)
    {
        assertCompilationException(code, expectedMessage, null, null);
    }

    private void assertCompilationException(String code, String expectedMessage, Integer expectedLine, Integer expectedColumn)
    {
        try
        {
            compileTestSource("fromString.pure", code);
            Assert.fail("Expected compilation error from:\n" + code);
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, expectedMessage, "fromString.pure", expectedLine, expectedColumn, e);
        }
    }
}
