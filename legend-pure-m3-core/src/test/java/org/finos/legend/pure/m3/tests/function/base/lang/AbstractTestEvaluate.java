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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestEvaluate extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testEvaluateWrongPrimitiveType()
    {
        try
        {
            compileTestSource("fromString.pure","function myFunc(s:Integer[1]):String[1]\n" +
                    "{\n" +
                    "    $s->toString();\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "   print(myFunc_Integer_1__String_1_->eval('ok'), 1);" +
                    "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The type String is not compatible with the type Integer" ,7,39, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateWrongPureType()
    {
        try
        {
            compileTestSource("fromString.pure","Class Wave\n" +
                    "{\n" +
                    "    wavelength: Float[1];\n" +
                    "}\n" +
                    "Class ElementaryParticle\n" +
                    "{\n" +
                    "    energy: Float[1];\n" +
                    "    charge: Charge[1];\n" +
                    "}\n" +
                    "Enum Charge\n" +
                    "{\n" +
                    "   Positive, Negative, Neutral\n" +
                    "}\n" +
                    "function isNeutralParticle(e:ElementaryParticle[1]):Boolean[1]\n" +
                    "{\n" +
                    "    $e.charge == Charge.Neutral;\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "   print(isNeutralParticle_ElementaryParticle_1__Boolean_1_->eval(^Wave(wavelength=42.01)), 1);" +
                    "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The type Wave is not compatible with the type ElementaryParticle", 20, 62, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluatePropertyWrongType()
    {
        try
        {
            compileTestSource("fromString.pure","Class Wave\n" +
                    "{\n" +
                    "    wavelength: Float[1];\n" +
                    "}\n" +
                    "Class ElementaryParticle\n" +
                    "{\n" +
                    "    energy: Float[1];\n" +
                    "}\n" +
                    "function {doc.doc = 'Get all properties on the provided type / class'}\n" +
                    "   meta::pure::functions::meta::allProperties(class : Class<Any>[1]) : AbstractProperty<Any>[*]\n" +
                    "{\n" +
                    "  []\n" +
                    "     ->concatenate($class.properties)\n" +
                    "     ->concatenate($class.propertiesFromAssociations)\n" +
                    "     ->concatenate($class.qualifiedProperties)\n" +
                    "     ->concatenate($class.qualifiedPropertiesFromAssociations)\n" +
                    "}\n" +
                    "function meta::pure::versioning::classPropertyByNonMilestonedName(c:Class<Any>[1], name:String[1]):AbstractProperty<Any>[0..1] {\n" +
                    "   $c->allProperties()->filter(p|$p.name == $name || $p.name == ($name + 'AllVersions'))->first();//->meta::pure::milestoning::reverseMilestoningTransforms()->toOne();\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "   print(ElementaryParticle->classPropertyByName('energy')->toOne()->eval(^Wave(wavelength=42.01)), 1);" +
                    "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            //e.printStackTrace();
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The type Wave is not compatible with the type ElementaryParticle", 23, 70, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateAnyWrongMultiplicity()
    {
        compileTestSource("fromString.pure","function myFunc(s:String[1], p:String[1]):String[1]\n" +
                "{\n" +
                "    $s;\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_1__String_1__String_1_->evaluate([^List<String>(values='ok'), ^List<String>(values=['ok1','ok2'])]), 1);\n" +
                "}\n" +
                "function test2():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_1__String_1__String_1_->eval(['ok'], ['ok1','ok2']), 1);\n" +
                "}");
        try
        {
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [2] is not compatible with the multiplicity [1] for parameter:p", 7, 48, this.checkLineNumbers());
        }
        try
        {
            this.execute("test2():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [2] is not compatible with the multiplicity [1] for parameter:p", 11, 48, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateViolateLowerBound()
    {
        compileTestSource("fromString.pure","function myFunc(s:String[2..10], p:String[0..3]):String[1]\n" +
                "{\n" +
                "    $s->joinStrings('');\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_$2_10$__String_$0_3$__String_1_->evaluate([^List<String>(values=['ok']), ^List<String>(values=[])]),1);\n" +
                "}\n" +
                "function test2():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_$2_10$__String_$0_3$__String_1_->eval(['ok'], []),1);\n" +
                "}");
        try
        {
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [1] is not compatible with the multiplicity [2..10] for parameter:s", 7, 57, this.checkLineNumbers());
        }
        try
        {
            this.execute("test2():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [1] is not compatible with the multiplicity [2..10] for parameter:s", 11, 57, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateViolateUpperBound()
    {
        compileTestSource("fromString.pure","function myFunc(s:String[0..5], p:String[0..3]):String[1]\n" +
                "{\n" +
                "    $s->joinStrings('');\n" +
                "}\n" +
                "function test():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_$0_5$__String_$0_3$__String_1_->evaluate([^List<String>(values=['ok']), ^List<String>(values=['ok','ok2','ok3','ok4','ok5'])]), 1);\n" +
                "}\n" +
                "function test2():Nil[0]\n" +
                "{\n" +
                "   print(myFunc_String_$0_5$__String_$0_3$__String_1_->eval(['ok'], ['ok','ok2','ok3','ok4','ok5']), 1);\n" +
                "}");
        try
        {
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [5] is not compatible with the multiplicity [0..3] for parameter:p", 7, 56, this.checkLineNumbers());
        }
        try
        {
            this.execute("test2():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            this.assertExceptionInformation(e, "Error during dynamic function evaluation. The multiplicity [5] is not compatible with the multiplicity [0..3] for parameter:p", 11, 56, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateUnboundedMultiplicity()
    {
        compileTestSource("fromString.pure","function myFunc(s:String[*], x:Integer[1]):String[1]\n" +
                "{\n" +
                "    $s->joinStrings('');\n" +
                "}\n" +
                "function myFunc2(s:String[*], x:Integer[*]):String[1]" +
                "{\n" +
                "    $s->joinStrings('');\n" +
                "}\n" +
                "function test():Boolean[1]\n" +
                "{\n" +
                "   assert('3.14' == myFunc_String_MANY__Integer_1__String_1_->eval(['3','.','1','4'], 42), |'');\n" +
                "   assert('3.14' == myFunc_String_MANY__Integer_1__String_1_->evaluate([^List<String>(values=['3','.','1','4']), ^List<Integer>(values=42)])->toOne(), |'');\n" +
                "   assert('' == myFunc_String_MANY__Integer_1__String_1_->eval([],42), |'');\n" +
                "   assert('' == myFunc_String_MANY__Integer_1__String_1_->evaluate([^List<String>(values=[]), ^List<Integer>(values=42)])->toOne(), |'');\n" +
                "   assert('3' == myFunc_String_MANY__Integer_1__String_1_->eval(['3'],42), |'');\n" +
                "   assert('3' == myFunc_String_MANY__Integer_1__String_1_->evaluate([^List<String>(values=['3']), ^List<Integer>(values=42)])->toOne(), |'');\n" +
                "   assert('' == myFunc2_String_MANY__Integer_MANY__String_1_->eval([],[]), |'');\n" +
                "   assert('' == myFunc2_String_MANY__Integer_MANY__String_1_->evaluate([^List<String>(values=[]), ^List<Integer>(values=[])])->toOne(), |'');\n" +
                "}\n");
        this.execute("test():Boolean[1]");
    }


    @Test
    public void testEvaluateAssert()
    {
        try
        {
            compileTestSource("fromString.pure","function myFunc():Boolean[1]\n" +
                    "{\n" +
                    "    fail('Failed');\n" +
                    "}\n" +
                    "function test():Nil[0]\n" +
                    "{\n" +
                    "   if( 1==1 , | print(myFunc__Boolean_1_->eval(), 1) , | print('x', 1));" +
                    "}\n");
            this.execute("test():Nil[0]");
            Assert.fail();
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            this.assertExceptionInformation(e, "Failed", 3, 5, this.checkLineNumbers());
        }
    }

    @Test
    public void testEvaluateEval() {
        compileTestSource("fromString.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "  assert([] == evaluate_Function_1__List_MANY__Any_MANY_->eval(first_T_MANY__T_$0_1$_, list([])), |'');" +
                        "  assert(1 == evaluate_Function_1__List_MANY__Any_MANY_->eval(first_T_MANY__T_$0_1$_, list([1,2,3])), |'');" +
                        "}" );
        this.execute("test():Any[*]");
    }

    @Test
    public void testPureRuntimeClassConstraintFunctionEvaluate()
    {
        compileTestSource("fromString.pure",
                "Class Employee" +
                        "[" +
                        "   $this.lastName->startsWith('A')" +
                        "]" +
                        "{" +
                        "   lastName:String[1];" +
                        "}"+
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'AAAAAA');" +
                        "   assert(Employee.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=$t))->toOne()->cast(@Boolean), |'');" +
                        "   $t;" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testInheritedQualifiedPropertyWithThisInReturnedLambda()
    {
        compileTestSource("fromString.pure",
                "import test::*;\n" +
                        "Class test::TestClass1\n" +
                        "{\n" +
                        "  name : String[1];\n" +
                        "  getNameFunction()\n" +
                        "  {\n" +
                        "    {|$this->cast(@TestClass1).name}\n" +
                        "  }:Function<{->String[1]}>[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::TestClass2 extends TestClass1\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "function test::testFn():Any[*]\n" +
                        "{\n" +
                        "  ^TestClass1(name='Daniel').getNameFunction()->eval() +\n" +
                        "    ' ' +\n" +
                        "    ^TestClass2(name='Benedict').getNameFunction()->eval()\n" +
                        "}\n");
        CoreInstance func = runtime.getFunction("test::testFn():Any[*]");
        CoreInstance result = functionExecution.start(func, Lists.immutable.empty());
        Assert.assertEquals("Daniel Benedict", PrimitiveUtilities.getStringValue(result.getValueForMetaPropertyToOne(M3Properties.values)));
    }


    public void assertExceptionInformation(Exception e, String message, int lineNo, int columnNo, boolean checkLineNumbers)
    {
        PureException pe = PureException.findPureException(e);
        Assert.assertNotNull(pe);
        Assert.assertTrue(pe instanceof PureExecutionException);
        PureException originalPE = pe.getOriginatingPureException();
        Assert.assertNotNull(originalPE);
        Assert.assertTrue(originalPE instanceof PureExecutionException);
        Assert.assertEquals(message, originalPE.getInfo());

        SourceInformation sourceInfo = originalPE.getSourceInformation();
        Assert.assertNotNull(sourceInfo);
        if(checkLineNumbers)
        {
            Assert.assertEquals(lineNo, sourceInfo.getLine());
            Assert.assertEquals(columnNo, sourceInfo.getColumn());
        }
    }

    public boolean checkLineNumbers()
    {
        return true;
    }
}
