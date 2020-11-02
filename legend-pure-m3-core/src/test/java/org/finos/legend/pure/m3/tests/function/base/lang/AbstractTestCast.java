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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractTestCast extends AbstractPureTestWithCoreCompiled
{
    @Before
    public void compileInliner()
    {
        compileTestSourceFromResource("/org/finos/legend/pure/m3/cast/cast.pure");
    }

    @Test
    public void testCastError()
    {
        try
        {
            compileTestSource("fromString.pure","Class A {prop3:String[1];}\n" +
                    "Class B extends A {prop2 : String[1];}\n" +
                    "Class C {prop:String[1];}\n" +
                    "\n" +
                    "function testError():Nil[0]\n" +
                    "{\n" +
                    "   print(^A(prop3='a')->cast(@C).prop,1);\n" +
                    "}\n");
            this.execute("testError():Nil[0]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.checkCastErrorMessage(e);
        }
    }

    public void checkCastErrorMessage(Exception e)
    {
        PureException pe = PureException.findPureException(e);
        Assert.assertNotNull(pe);
        Assert.assertTrue(pe instanceof PureExecutionException);
        PureException originalPE = pe.getOriginatingPureException();
        Assert.assertNotNull(originalPE);
        Assert.assertTrue(originalPE instanceof PureExecutionException);
        Assert.assertEquals("Cast exception: A cannot be cast to C", originalPE.getInfo());

        SourceInformation sourceInfo = originalPE.getSourceInformation();
        Assert.assertNotNull(sourceInfo);
        Assert.assertEquals(7, sourceInfo.getLine());
        Assert.assertEquals(25, sourceInfo.getColumn());
    }

    @Test
    public void testInvalidCastWithTypeParameters()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Any[*]\n" +
                    "{\n" +
                    "   ^List<X>(values=^X(nameX = 'my name is X'))->castToListY().values.nameY->print(1);\n" +
                    "}");
            this.execute("test():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.checkInvalidCastWithTypeParametersErrorMessage(e);
        }
    }

    public void checkInvalidCastWithTypeParametersErrorMessage(Exception e)
    {
        PureException pe = PureException.findPureException(e);
        Assert.assertNotNull(pe);
        Assert.assertTrue(pe instanceof PureExecutionException);
        Assert.assertEquals("Cast exception: List<X> cannot be cast to List<Y>", pe.getInfo());

        SourceInformation sourceInfo = pe.getSourceInformation();
        Assert.assertNotNull(sourceInfo);
        Assert.assertEquals(3, sourceInfo.getLine());
        Assert.assertEquals(49, sourceInfo.getColumn());
    }

    @Test
    public void testInvalidPrimitiveDownCast()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Number[*]\n" +
                    "{\n" +
                    "   [1, 3.0, 'the cat sat on the mat']->cast(@Number)->plus()\n" +
                    "}\n");
            this.execute("test():Number[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.checkInvalidPrimitiveDownCastErrorMessage(e);
        }
    }

    public void checkInvalidPrimitiveDownCastErrorMessage(Exception e)
    {
        assertPureException(PureExecutionException.class, "Cast exception: String cannot be cast to Number", 3, 40, e);
    }

    @Test
    public void testPrimitiveConcreteOne()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Any[*]\n" +
                    "{\n" +
                    "   1->castToString()->joinStrings('');\n" +
                    "}");
            this.execute("test():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testPrimitiveConcreteOneErrorMessage(e);
        }
    }

    public void testPrimitiveConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Integer cannot be cast to String", 3, 7);
    }

    @Test
    public void testPrimitiveConcreteMany()
    {
        try
        {
            compileTestSource("fromString.pure","function testMany():Any[*]\n" +
                    "{\n" +
                    "   [1, 2.5, 'abc']->castToNumber()->plus();\n" +
                    "}");
            this.execute("testMany():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testPrimitiveConcreteManyErrorMessage(e);
        }
    }

    public void testPrimitiveConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Number", 3, 21);
    }

    @Test
    public void testNonPrimitiveConcreteOne()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Any[*]\n" +
                    "{\n" +
                    "   ^X()->castToY().nameY;\n" +
                    "}");
            this.execute("test():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testNonPrimitiveConcreteOneErrorMessage(e);
        }
    }

    public void testNonPrimitiveConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 3, 10);
    }

    @Test
    public void testNonPrimitiveConcreteMany()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Any[*]\n" +
                    "{\n" +
                    "   [^X(), ^Y(), ^S()]->castToY().nameY;\n" +
                    "}");
            this.execute("test():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testNonPrimitiveConcreteManyErrorMessage(e);
        }
    }

    public void testNonPrimitiveConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 3, 24);
    }

    @Test
    public void testPrimitiveNonConcreteOne()
    {
        try
        {
            compileTestSource("fromString.pure","function testConcrete():Any[*]\n" +
                    "{\n" +
                    "   1->nonConcreteCastToString()->joinStrings('');\n" +
                    "}");
            this.execute("testConcrete():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testPrimitiveNonConcreteOneErrorMessage(e);
        }
    }

    public void testPrimitiveNonConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Integer cannot be cast to String", 3, 7);
    }

    @Test
    public void testPrimitiveNonConcreteMany()
    {
        try
        {
            compileTestSource("fromString.pure","function testNonConcrete():Any[*]\n" +
                    "{\n" +
                    "   [1, 2.5, 'abc']->nonConcreteCastToNumber()->plus();\n" +
                    "}");
            this.execute("testNonConcrete():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testPrimitiveNonConcreteManyErrorMessage(e);
        }
    }

    public void testPrimitiveNonConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Number", 3, 21);
    }

    @Test
    public void testNonPrimitiveNonConcreteOne()
    {
        try
        {
            compileTestSource("fromString.pure","function testNonPrimitive():Any[*]\n" +
                    "{\n" +
                    "   ^X()->nonConcreteCastToY().nameY;\n" +
                    "}");
            this.execute("testNonPrimitive():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testNonPrimitiveNonConcreteOneErrorMessage(e);
        }
    }

    public void testNonPrimitiveNonConcreteOneErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 3, 10);
    }

    @Test
    public void testNonPrimitiveNonConcreteMany()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Any[*]\n" +
                    "{\n" +
                    "   [^X(), ^Y(), ^S()]->nonConcreteCastToY().nameY;\n" +
                    "}");
            this.execute("test():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testNonPrimitiveNonConcreteManyErrorMessage(e);
        }
    }

    public void testNonPrimitiveNonConcreteManyErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: X cannot be cast to Y", 3, 24);
    }

    @Test
    public void testEnumToStringCast()
    {
        try
        {
            compileTestSource("fromString.pure","function testEnum():Any[*]\n" +
                    "{\n" +
                    "   Month.January -> castToString() -> joinStrings('');\n" +
                    "}");
            this.execute("testEnum():Any[*]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testEnumToStringCastErrorMessage(e);
        }
    }

    public void testEnumToStringCastErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: Month cannot be cast to String", 3, 21);
    }

    @Test
    public void testStringToEnumCast()
    {
        try
        {
            compileTestSource("fromString.pure","function test():Nil[0]\n" +
                    "{\n" +
                    "   'January' -> cast(@Month) -> print(1);\n" +
                    "}");
            this.execute("test():Nil[0]");
            Assert.fail("Expected cast error");
        }
        catch (Exception e)
        {
            this.testStringToEnumCastErrorMessage(e);
        }
    }

    public void testStringToEnumCastErrorMessage(Exception e)
    {
        this.checkInvalidTypeCastErrorMessage(e, "Cast exception: String cannot be cast to Month", 3, 17);
    }

    public void checkInvalidTypeCastErrorMessage(Exception e, String message, int line, int column)
    {
        PureException pe = PureException.findPureException(e);
        Assert.assertNotNull(pe);
        Assert.assertTrue(pe instanceof PureExecutionException);
        Assert.assertEquals(message, pe.getInfo());

        SourceInformation sourceInfo = pe.getSourceInformation();
        Assert.assertNotNull(sourceInfo);
        Assert.assertEquals(line, sourceInfo.getLine());
        Assert.assertEquals(column, sourceInfo.getColumn());
    }
}
