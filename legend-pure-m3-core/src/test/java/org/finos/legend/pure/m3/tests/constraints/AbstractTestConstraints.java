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

package org.finos.legend.pure.m3.tests.constraints;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;


public abstract class AbstractTestConstraints extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testFunction()
    {
        this.compileTestSource("function myFunction(s:String[1], k:Integer[1]):String[1]" +
                "[" +
                "   $s->startsWith('A')," +
                "   $return->startsWith('A')," +
                "   $k > 2" +
                "]" +
                "{" +
                "   $s+$k->toString();" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   myFunction('A test', 4)" +
                "}\n");
        this.execute("testNew():Any[*]");
    }

    @Test
    public void testFunctionError()
    {
        try
        {
            this.compileTestSource("function myFunction(s:String[1], k:Integer[1]):String[1]" +
                    "[" +
                    "   $s->startsWith('A')," +
                    "   $return->startsWith('A')," +
                    "   $k > 2" +
                    "]" +
                    "{" +
                    "   $s+$k->toString();" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   myFunction('A test', 2)" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint (PRE):[2] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
        }
    }

    @Test
    public void testFunctionErrorOnReturnPreConstrainId()
    {
        try
        {
            this.compileTestSource("function myFunction(s:String[1], k:Integer[1]):String[1]" +
                    "[" +
                    "   pre1:$s->startsWith('A')," +
                    "   pre2:$k > 2" +
                    "]" +
                    "{" +
                    "   'B';" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   myFunction(' test', 3)" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint (PRE):[pre1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
        }
    }

    @Test
    public void testFunctionErrorOnReturn()
    {
        try
        {
            this.compileTestSource("function myFunction(s:String[1], k:Integer[1]):String[1]" +
                    "[" +
                    "   $s->startsWith('A')," +
                    "   $return->startsWith('A')," + //this is a postconstraint
                    "   $k > 2" +
                    "]" +
                    "{" +
                    "   'B';" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   myFunction('A test', 3)" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint (POST):[1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
        }
    }


    @Test
    public void testNewError()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   $this.lastName->startsWith('A')" +
                    "]" +
                    "{" +
                    "   lastName:String[1];" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = 'CDE')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 4, 12, e);
        }
    }

    @Test
    public void testNewErrorDuplicateConstraints()
    {
        try
        {
            this.compileTestSource("Class Employee\n" +
                    "[\n" +
                    "  one: $this.lastName->startsWith('A'),\n" +
                    "  one: $this.lastName->endsWith('A')\n" +
                    "]\n" +
                    "{\n" +
                    "   lastName:String[1];\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = 'CDE')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Constraints for Employee must be unique, [one] is duplicated", 4, 3, e);
        }
    }

    @Test
    public void testNewOk()
    {
        this.compileTestSource("Class Employee" +
                "[" +
                "   rule1 : $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let t = ^Employee(lastName = 'ABC')" +
                "}\n");
        this.execute("testNew():Any[*]");
    }

    @Test
    public void testNewWarn()
    {
        this.compileTestSource("Class Employee" +
                "[" +
                "   warn($this.lastName->startsWith('A'), 'ok')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]\n" +
                "{\n" +
                "    if($b,|$b,|print($message, 1);true;)\n" +
                "}" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let t = ^Employee(lastName = 'CDE')" +
                "}\n");
        this.execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithMoreThanOneConstraint()
    {
        this.compileTestSource("Class Employee" +
                "[" +
                "   warn($this.lastName->startsWith('A'), 'ok')," +
                "   $this.lastName->endsWith('E')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let t = ^Employee(lastName = 'CDE')" +
                "}" +
                "function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]\n" +
                "{\n" +
                "    if($b,|$b,|print($message, 1);true;)\n" +
                "}" +
                "\n");
        this.execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithFilterConstraint()
    {
        this.compileTestSource("Class Employee\n" +
                "{\n" +
                "  id:Integer[1];\n" +
                "}\n" +
                "Class Firm\n" +
                "[\n" +
                "   $this.employees->filter(e | ($e.id < $this.minId) || ($e.id > $this.maxId))->isEmpty()\n" +
                "]\n" +
                "{\n" +
                "  minId:Integer[1];\n" +
                "  maxId:Integer[1];\n" +
                "  employees:Employee[*];\n" +
                "}\n" +
                "function testNewSuccess():Any[*]\n" +
                "{\n" +
                "   let f1 = ^Firm(minId=1, maxId=10);\n" +
                "   let f2 = ^Firm(minId=1, maxId=10, employees=[^Employee(id=1), ^Employee(id=9)]);\n" +
                "}\n" +
                "function testNewFailure():Any[*]\n" +
                "{\n" +
                "   ^Firm(minId=1, maxId=10, employees=[^Employee(id=1), ^Employee(id=19)]);\n" +
                "}");
        this.execute("testNewSuccess():Any[*]");
        try
        {
            this.execute("testNewFailure():Any[*]");
        }
        catch (Exception e)
        {
            this.assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 21, 4, e);
        }
    }

    @Test
    public void testClassWithMapConstraint()
    {
        this.compileTestSource("Class Employee\n" +
                "{\n" +
                "  id:Integer[1];\n" +
                "}\n" +
                "Class Firm\n" +
                "[\n" +
                "   $this.employees->removeDuplicates(e | $e.id, [])->size() == $this.employees->size()\n" +
                "]\n" +
                "{\n" +
                "  employees:Employee[*];\n" +
                "}\n" +
                "function testNewSuccess():Any[*]\n" +
                "{\n" +
                "   let f1 = ^Firm();\n" +
                "   let f2 = ^Firm(employees=[^Employee(id=1), ^Employee(id=9)]);\n" +
                "}\n" +
                "function testNewFailure():Any[*]\n" +
                "{\n" +
                "   ^Firm(employees=[^Employee(id=1), ^Employee(id=2), ^Employee(id=1)]);\n" +
                "}");
        this.execute("testNewSuccess():Any[*]");
        try
        {
            this.execute("testNewFailure():Any[*]");
        }
        catch (Exception e)
        {
            this.assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 19, 4, e);
        }
    }

    @Test
    public void testCopyError()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   $this.lastName->startsWith('A')" +
                    "]" +
                    "{" +
                    "   lastName:String[1];" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = 'ABC');\n" +
                    "   ^$t(lastName = 'KK');" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 5, 4, e);
        }
    }


    @Test
    public void testInheritanceFailingSubClass()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   $this.lastName->startsWith('A')" +
                    "]" +
                    "{" +
                    "   lastName:String[1];" +
                    "}\n" +
                    "Class Manager extends Employee\n" +
                    "{\n" +
                    "  manages:Employee[*];\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Manager(lastName = 'BC')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail("This should fail constraint validation");
        } catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 8, 12, e);
        }
    }

    @Test
    public void testInheritanceFailingSubClassConstraint()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   $this.lastName->startsWith('A')" +
                    "]" +
                    "{" +
                    "   lastName:String[1];" +
                    "}\n" +
                    "Class Manager extends Employee\n" +
                    "[" +
                    "   $this.manages->size() > 1" +
                    "]" +
                    "{\n" +
                    "  manages:Employee[*];\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Manager(lastName = 'BC')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail("This should fail constraint validation");
        } catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 8, 12, e);
        }
    }

    @Test
    public void testBooleanWithMoreThanOneOperand()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "    $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = '1234567891011121213454545')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail("This should fail constraint validation");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 4, 12, e);
        }
    }

    @Test
    public void tesIdInConstraint()
    {

        this.compileTestSource("Class Employee" +
                "[" +
                "   rule1 : $this.lastName->toOne()->length() < 10" +
                "]" +
                "{" +
                "   lastName:String[0..1];" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let t = ^Employee(lastName = '123456789')" +
                "}\n");
        this.execute("testNew():Any[*]");

    }

    @Test
    public void tesIdInConstraintWrong()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   rule1 : $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = '1234567893536536536')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 4, 12, e);
        }

    }

    @Test
    public void testIdInConstraintInWrong()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "  : $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let t = ^Employee(lastName = '123456789')" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail("This should fail parsing");
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureParserException.class, "expected: a valid identifier text; found: ':'", 1, 18, e);
        }
    }


    @Test
    public void testConstraintWithDynamicNewNoOverrides()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   rule1 : $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function testNew():Any[*] {\n" +
                    "  let r = dynamicNew(Employee,\n" +
                    "                   [\n" +
                    "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                    "                   ])->cast(@Employee);\n" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 3, 11, e);
        }

    }

    @Test
    public void testConstraintWithDynamicNew()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   rule1 : $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                    "{\n" +
                    "  [];\n" +
                    "}\n" +
                    "\n" +
                    "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                    "{\n" +
                    "  [];\n" +
                    "}\n" +
                    "function testNew():Any[*] {\n" +
                    "  let r = dynamicNew(Employee,\n" +
                    "                   [\n" +
                    "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                    "                   ],\n" +
                    "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                    "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                    "                   '2'\n" +
                    "                  )->cast(@Employee);\n" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 12, 11, e);
        }

    }
    @Test
    public void testConstraintWithGenericTypeDynamicNew()
    {
        try
        {
            this.compileTestSource("Class Employee" +
                    "[" +
                    "   rule1 : $this.lastName->toOne()->length() < 10" +
                    "]" +
                    "{" +
                    "   lastName:String[0..1];" +
                    "}\n" +
                    "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                    "{\n" +
                    "  [];\n" +
                    "}\n" +
                    "\n" +
                    "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                    "{\n" +
                    "  [];\n" +
                    "}\n" +
                    "function testNew():Any[*] {\n" +
                    "  let r = dynamicNew(^GenericType(rawType=Employee),\n" +
                    "                   [\n" +
                    "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                    "                   ],\n" +
                    "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                    "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                    "                   '2'\n" +
                    "                  )->cast(@Employee);\n" +
                    "}\n");
            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 12, 11, e);
        }

    }
    @Test
    public void testPureRuntimeClassConstraintFunctionEvaluate() throws Exception
    {
        this.compileTestSource("Class Employee" +
                "[" +
                "   $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let t = ^Employee(lastName = 'AAAAAA');" +
                "   assert(Employee.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=$t))->toOne()->cast(@Boolean), |'');" +
                "   $t;" +
                "}\n");
        this.execute("testNew():Any[*]");

    }

    @Test
    public void testExtendedConstraintGrammar()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~externalId       : 'My_Ext_Id'\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~enforcementLevel : Error\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}");
    }

    @Test
    public void testExtendedConstraintGrammarAllowedOnlyForClasses()
    {
        try
        {
            this.compileTestSource("function myFunction(s:String[1], k:Integer[1]):String[1]\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $s->startsWith('A')\n" +
                    "   )\n" +
                    "]" +
                    "{" +
                    "   $s+$k->toString();" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   myFunction('A test', 4)" +
                    "}\n");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureParserException.class, "Complex constraint specifications are supported only for class definitions", 3, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarOptionalOwner()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~enforcementLevel : Warn\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}");
    }

    @Test
    public void testExtendedConstraintGrammarOptionalLevelAndMessage()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~externalId       : 'My_Ext_Id'\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}");
    }

    @Test
    public void testExtendedConstraintGrammarFunctionRequired()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureParserException.class, "expected: '~function' found: ')'", 6, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarLevelIsWarnOrError()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~enforcementLevel : Something\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureParserException.class, "expected: ENFORCEMENT_LEVEL found: 'Something'", 7, 27, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsBoolean()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')->toString()\n" +
                    "      ~enforcementLevel : Warn\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsOneBoolean()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')->toOneMany()\n" +
                    "      ~enforcementLevel : Warn\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsString()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~enforcementLevel : Warn\n" +
                    "      ~message          : 'Contract ID: ' == $this.contractId\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 43, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsOneString()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~enforcementLevel : Warn\n" +
                    "      ~message          : [('Contract ID: ' + $this.contractId), ('Contract ID: ' + $this.contractId)]\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 27, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraints()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "   ),\n" +
                "   c2\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "");
    }



    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypes()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "   ),\n" +
                "   c2 : $this.endDate > $this.startDate\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "");
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypesAlternating()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "   ),\n" +
                "   c2 : $this.endDate > $this.startDate\n," +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "");
    }

    @Test
    public void testExtendedConstraintGrammarNameConflict()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "   ),\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    "");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintGrammarNameConflictInDifferentType()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "   ),\n" +
                    "   c1 : $this.endDate > $this.startDate\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    "");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithOwner()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                ""
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithOwnerGlobal()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Global\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)\n" +
                    "}"
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithNoOwner()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   ^Position(contractId='1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    ""
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithGlobalOwner()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Global\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01)\n" +
                "}"
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyPassesWithOwner()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);\n" +
                "   ^$a(contractId='1');\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                ""
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithOwnerGlobal()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Global\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);\n" +
                    "   ^$a(contractId='1');\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    ""
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 25, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithNoOwner()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   let a = ^Position(contractId='A1', positionType='2', startDate=%2010-01-01, endDate=%2011-01-01);\n" +
                    "   ^$a(contractId='1');\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    ""
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 24, 4, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewPassesWithOwner()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   Position->dynamicNew([\n" +
                "       ^KeyValue(key='contractId', value='1'), \n" +
                "       ^KeyValue(key='positionType', value='2'),\n" +
                "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "    ])\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                ""
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithOwnerGlobal()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Global\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   Position->dynamicNew([\n" +
                    "       ^KeyValue(key='contractId', value='1'), \n" +
                    "       ^KeyValue(key='positionType', value='2'),\n" +
                    "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                    "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                    "    ])\n" +
                    "}"
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 14, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithNoOwner()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.endDate > $this.startDate\n" +
                    "   )\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   Position->dynamicNew([\n" +
                    "       ^KeyValue(key='contractId', value='1'), \n" +
                    "       ^KeyValue(key='positionType', value='2'),\n" +
                    "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                    "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                    "    ])\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    ""
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 14, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsOtherConstraint()
    {
        try
        {
            this.compileTestSource("Class Position\n" +
                    "[\n" +
                    "   c1\n" +
                    "   (\n" +
                    "      ~owner            : Finance\n" +
                    "      ~function         : $this.contractId->startsWith('A')\n" +
                    "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                    "   ),\n" +
                    "   c3 : $this.endDate > $this.startDate\n" +
                    "]\n" +
                    "{\n" +
                    "   contractId: String[1];\n" +
                    "   positionType: String[1];\n" +
                    "   startDate: Date[1];\n" +
                    "   endDate: Date[1];\n" +
                    "}\n" +
                    "\n" +
                    "function meta::pure::functions::collection::isNotEmpty(p:Any[*]):Boolean[1]\n" +
                    "{\n" +
                    "    !isEmpty($p)\n" +
                    "}" +
                    "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                    "{\n" +
                    "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                    "}\n" +
                    "function testNew():Any[*]\n" +
                    "{\n" +
                    "   Position->dynamicNew([\n" +
                    "       ^KeyValue(key='contractId', value='1'), \n" +
                    "       ^KeyValue(key='positionType', value='2'),\n" +
                    "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                    "       ^KeyValue(key='endDate', value=%2010-01-01)\n" +
                    "    ])\n" +
                    "}"
            );

            this.execute("testNew():Any[*]");
            Assert.fail();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            this.assertOriginatingPureException(PureExecutionException.class, "Constraint :[c3] violated in the Class Position", 27, 14, e);
        }
    }

    @Test
    public void testExtendedConstraintExecutionCanEvaluateConstraint()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   assert(!Position.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=Position->dynamicNew([\n" +
                "       ^KeyValue(key='contractId', value='1'), \n" +
                "       ^KeyValue(key='positionType', value='2'),\n" +
                "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "    ])))->toOne()->cast(@Boolean), |'');\n" +
                "}"
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCanEvaluateConstraintMessage()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                "function meta::pure::functions::collection::isNotEmpty(p:Any[*]):Boolean[1]\n" +
                "{\n" +
                "    !isEmpty($p)\n" +
                "}" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   assert(Position.constraints->at(0).messageFunction->toOne()->evaluate(^List<Any>(values=Position->dynamicNew([\n" +
                "       ^KeyValue(key='contractId', value='1'), \n" +
                "       ^KeyValue(key='positionType', value='2'),\n" +
                "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "    ])))->toOne()->cast(@String) == 'Contract ID: 1', |'');\n" +
                "}"
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCanGetOwnerExtIdEnforcement()
    {
        this.compileTestSource("Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~externalId       : 'My_Ext_Id'\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~enforcementLevel : Warn\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~function         : $this.endDate > $this.startDate\n" +
                "   )\n" +
                "]\n" +
                "{\n" +
                "   contractId: String[1];\n" +
                "   positionType: String[1];\n" +
                "   startDate: Date[1];\n" +
                "   endDate: Date[1];\n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   assert(Position.constraints->at(0).owner == 'Finance', |'');\n" +
                "   assert(Position.constraints->at(0).externalId == 'My_Ext_Id', |'');\n" +
                "   assert(Position.constraints->at(0).enforcementLevel == 'Warn', |'');\n" +
                "}" +
                "function meta::pure::functions::lang::greaterThan(left:Date[0..1], right:Date[0..1]):Boolean[1]\n" +
                "{\n" +
                "   !$left->isEmpty() && !$right->isEmpty() && (compare($right->toOne(), $left->toOne()) < 0);\n" +
                "}\n" +
                ""
        );

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testDeeplyNestedThis()
    {
        compileTestSource("/test/repro.pure",
                "import my::supportDemo::*;\n" +
                        "Class my::supportDemo::Person\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class my::supportDemo::SuperPerson\n" +
                        "{\n" +
                        "   person: my::supportDemo::Person[1];\n" +
                        "   personWithTitle(title:String[1])\n" +
                        "   {\n" +
                        "     ^Person(name = $title + ' ' + $this.person.name)\n" +
                        "   }:Person[1];\n" +
                        "}\n" +
                        "function meta::pure::functions::collection::removeDuplicates<T>(col:T[*], eql:Function<{T[1],T[1]->Boolean[1]}>[1]):T[*]\n" +
                        "{\n" +
                        "    $col->removeDuplicates([], $eql)\n" +
                        "}\n" +
                        "Class my::supportDemo::SuperPeople\n" +
                        "[  \n" +
                        "   superPeopleHaveNoDuplicates\n" +
                        "   (\n" +
                        "      ~function: if($this.superPeople->isEmpty(),\n" +
                        "                    | true,\n" +
                        "                    | $this.superPeople->size() == $this.superPeople->removeDuplicates({left,right| $left.personWithTitle($this.title).name == $right.personWithTitle($this.title).name})->size())\n" +
                        "      ~enforcementLevel: Error\n" +
                        "      ~message: 'test'\n" +
                        "   )\n" +
                        "]\n" +
                        "{\n" +
                        "   superPeople: my::supportDemo::SuperPerson[*];\n" +
                        "   title:String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function testSucceed():Any[*]\n" +
                        "{\n" +
                        "   assert(SuperPeople.constraints->at(0).functionDefinition->evaluate(^List<Any>(values=SuperPeople->dynamicNew([\n" +
                        "       ^KeyValue(key='superPeople', value=[^SuperPerson(person=^Person(name='John')), ^SuperPerson(person=^Person(name='Robert'))]), \n" +
                        "       ^KeyValue(key='title', value='Dr.')\n" +
                        "    ])))->toOne()->cast(@Boolean), |'')\n" +
                        "}\n" +
                        "\n" +
                        "function testFail():Any[*]\n" +
                        "{\n" +
                        "   ^SuperPeople(superPeople=[^SuperPerson(person=^Person(name='John')), ^SuperPerson(person=^Person(name='John'))], title='Dr.')\n" +
                        "}\n");
        execute("testSucceed():Any[*]");
        try
        {
            execute("testFail():Any[*]");
            Assert.fail("Expected constraint failure");
        }
        catch (Exception e)
        {
            assertPureException(PureExecutionException.class, "Constraint :[superPeopleHaveNoDuplicates] violated in the Class SuperPeople, Message: test", "/test/repro.pure", 45, 4, 45, 4, 45, 128, e);        }
    }
}
