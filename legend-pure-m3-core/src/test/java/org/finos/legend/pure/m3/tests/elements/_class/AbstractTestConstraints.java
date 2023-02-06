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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepositoryWithDependencies;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestConstraints extends AbstractPureTestWithCoreCompiled
{
    private static final String EMPLOYEE_SOURCE_NAME = "employee.pure";
    private static final String EMPLOYEE_SOURCE_CODE = "Class Employee\n" +
            "{\n" +
            "   lastName:String[1];\n" +
            "}\n";

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.delete("/test/repro.pure");
        runtime.modify(EMPLOYEE_SOURCE_NAME, EMPLOYEE_SOURCE_CODE);
        runtime.compile();
    }

    @Test
    public void testFunction()
    {
        compileTestSource("fromString.pure", "function myFunction(s:String[1], k:Integer[1]):String[1]" +
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
        execute("testNew():Any[*]");
    }

    @Test
    public void testFunctionError()
    {
        compileTestSource("fromString.pure", "function myFunction(s:String[1], k:Integer[1]):String[1]" +
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[2] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }

    @Test
    public void testFunctionErrorOnReturnPreConstrainId()
    {
        compileTestSource("fromString.pure", "function myFunction(s:String[1], k:Integer[1]):String[1]" +
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[pre1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }

    @Test
    public void testFunctionErrorOnReturn()
    {
        compileTestSource("fromString.pure", "function myFunction(s:String[1], k:Integer[1]):String[1]" +
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (POST):[1] violated. (Function:myFunction_String_1__Integer_1__String_1_)", 4, 4, e);
    }


    @Test
    public void testNewError()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'CDE')" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void testNewErrorDuplicateConstraints()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class EmployeeWithError\n" +
                        "[\n" +
                        "  one: $this.lastName->startsWith('A'),\n" +
                        "  one: $this.lastName->length() == 2\n" +
                        "]\n" +
                        "{\n" +
                        "   lastName:String[1];\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^EmployeeWithError(lastName = 'CDE')" +
                        "}\n"));
        assertPureException(PureCompilationException.class, "Constraints for EmployeeWithError must be unique, [one] is duplicated", 4, 3, e);
    }

    @Test
    public void testNewOk()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   rule1 : $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'ABC')" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testNewWarn()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   warn($this.lastName->startsWith('A'), 'ok')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]\n" +
                "{\n" +
                "    if($b,|$b,|true)\n" +
                "}");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'CDE')" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithMoreThanOneConstraint()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   warn($this.lastName->startsWith('A'), 'ok')," +
                "   $this.lastName->substring($this.lastName->length()-1) == 'E'" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n" +
                "function meta::pure::functions::constraints::warn(b:Boolean[1], message:String[1]):Boolean[1]\n" +
                "{\n" +
                "    if($b,|$b,|true;)\n" +
                "}");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'CDE')" +
                        "}" +
                        "\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testClassWithFilterConstraint()
    {
        runtime.modify("employee.pure", "Class Employee\n" +
                "{\n" +
                "  id:Integer[1];\n" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
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
        execute("testNewSuccess():Any[*]");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNewFailure():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 17, 4, e);
    }

    @Test
    public void testClassWithMapConstraint()
    {
        runtime.modify("employee.pure", "Class Employee\n" +
                "{\n" +
                "  id:Integer[1];\n" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
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
        execute("testNewSuccess():Any[*]");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNewFailure():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Firm", 15, 4, e);
    }

    @Test
    public void testCopyError()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = 'ABC');\n" +
                        "   ^$t(lastName = 'KK');" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 4, 4, e);
    }


    @Test
    public void testInheritanceFailingSubClass()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "Class Manager extends Employee\n" +
                        "{\n" +
                        "  manages:Employee[*];\n" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Manager(lastName = 'BC')" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 7, 12, e);
    }

    @Test
    public void testInheritanceFailingSubClassConstraint()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   $this.lastName->startsWith('A')" +
                "]" +
                "{" +
                "   lastName:String[1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 7, 12, e);
    }

    @Test
    public void testBooleanWithMoreThanOneOperand()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "    $this.lastName->toOne()->length() < 10" +
                "]" +
                "{" +
                "   lastName:String[0..1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = '1234567891011121213454545')" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[0] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void tesIdInConstraint()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   rule1 : $this.lastName->toOne()->length() < 10" +
                "]" +
                "{" +
                "   lastName:String[0..1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = '123456789')" +
                        "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void tesIdInConstraintWrong()
    {
        runtime.modify("employee.pure", "Class Employee" +
                "[" +
                "   rule1 : $this.lastName->toOne()->length() < 10" +
                "]" +
                "{" +
                "   lastName:String[0..1];" +
                "}\n");
        runtime.compile();
        compileTestSource("fromString.pure",
                "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = '1234567893536536536')" +
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 3, 12, e);
    }

    @Test
    public void testIdInConstraintInWrong()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Employee" +
                        "[" +
                        "  : $this.lastName->toOne()->length() < 10" +
                        "]" +
                        "{" +
                        "   lastName:String[0..1];" +
                        "}\n" +
                        "function testNew():Any[*]\n" +
                        "{\n" +
                        "   let t = ^Employee(lastName = '123456789')" +
                        "}\n"));
        assertPureException(PureParserException.class, "expected: a valid identifier text; found: ':'", 1, 18, e);
    }







    @Test
    public void testExtendedConstraintGrammar()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "function myFunction(s:String[1], k:Integer[1]):String[1]\n" +
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
                        "}\n"));
        assertPureException(PureParserException.class, "Complex constraint specifications are supported only for class definitions", 3, 4, e);
        runtime.modify("fromString.pure", "function myFunction(s:String[1], k:Integer[1]):String[1]\n" +
                "{" +
                "   $s+$k->toString();" +
                "}\n");
        runtime.compile();
    }

    @Test
    public void testExtendedConstraintGrammarOptionalOwner()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
        compileTestSource("fromString.pure", "Class Position\n" +
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
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureParserException.class, "expected: one of {'~externalId', '~function'} found: ')'", 6, 4, e);
    }

    @Test
    public void testExtendedConstraintGrammarLevelIsWarnOrError()
    {
        PureParserException e = Assert.assertThrows(PureParserException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureParserException.class, "expected: ENFORCEMENT_LEVEL found: 'Something'", 7, 27, e);
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsBoolean()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
    }

    @Test
    public void testExtendedConstraintGrammarFunctionIsOneBoolean()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureCompilationException.class, "A constraint must be of type Boolean and multiplicity one", 6, 62, e);
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsString()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 43, e);
    }

    @Test
    public void testExtendedConstraintGrammarMessageIsOneString()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}"));
        assertPureException(PureCompilationException.class, "A constraint message must be of type String and multiplicity one", 8, 27, e);
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraints()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n");
    }


    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypes()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n");
    }

    @Test
    public void testExtendedConstraintGrammarMultipleConstraintTypesAlternating()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n");
    }

    @Test
    public void testExtendedConstraintGrammarNameConflict()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}\n"));
        assertPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
    }

    @Test
    public void testExtendedConstraintGrammarNameConflictInDifferentType()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}\n"));
        assertPureException(PureCompilationException.class, "Constraints for Position must be unique, [c1] is duplicated", 8, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithOwner()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n");
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithOwnerGlobal()
    {
        compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}\n");

        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionFailsWithNoOwner()
    {
        compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionPassesWithGlobalOwner()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyPassesWithOwner()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n"
        );
        execute("testNew():Any[*]");
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithOwnerGlobal()
    {
        compileTestSource(
                "fromString.pure",
                "Class Position\n" +
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
                        "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 25, 4, e);
    }

    @Test
    public void testExtendedConstraintExecutionCopyFailsWithNoOwner()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 24, 4, e);
    }











    @Test
    public void testExtendedConstraintExecutionCanGetOwnerExtIdEnforcement()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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
                "}\n"
        );
        execute("testNew():Any[*]");
    }


    @Test
    public void testConstraintInClassWithTypeParameters()
    {
        compileTestSource("/test/repro.pure",
                "Class ClassWrapper<T|m>\n" +
                        "[\n" +
                        "   notAny: $this.classes->filter(c | $c == Any)->size() == 0\n" +
                        "]\n" +
                        "{\n" +
                        "   classes: Class<T>[m];\n" +
                        "}\n" +
                        "\n" +
                        "function testSucceed():Any[*]\n" +
                        "{\n" +
                        "    ^ClassWrapper<Type|1>(classes=Type);\n" +
                        "}\n" +
                        "\n" +
                        "function testFail():Any[*]\n" +
                        "{\n" +
                        "    ^ClassWrapper<Any|2>(classes=[Type, Any]);\n" +
                        "}\n"
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[notAny] violated in the Class ClassWrapper", "/test/repro.pure", 16, 5, 16, 5, 16, 45, e);
    }

    @Test
    public void testConstraintInClassWithTypeParameters2()
    {
        compileTestSource("/test/repro.pure",
                "Class Wrapper<T>\n" +
                        "[\n" +
                        "   notEmpty: $this.values->size() > 0\n" +
                        "]\n" +
                        "{\n" +
                        "   values: T[*];\n" +
                        "}\n" +
                        "\n" +
                        "function testSucceed():Any[*]\n" +
                        "{\n" +
                        "    ^Wrapper<Integer>(values=1);\n" +
                        "}\n" +
                        "\n" +
                        "function testFail():Any[*]\n" +
                        "{\n" +
                        "    ^Wrapper<Any>(values=[]);\n" +
                        "}\n"
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[notEmpty] violated in the Class Wrapper", "/test/repro.pure", 16, 5, 16, 5, 16, 28, e);
    }

    @Test
    public void testConstraintInFunctionWithTypeParameters()
    {
        compileTestSource("/test/repro.pure",
                "function myFunction<T>(col:T[*], toRemove:Any[*]):T[*]\n" +
                        "[\n" +
                        "   notEmptyBefore: $col->size() > 0,\n" +
                        "   notEmptyAfter: $return->size() > 0\n" +
                        "]\n" +
                        "{\n" +
                        "   $col->filter(x | !$toRemove->filter(y | $x == $y)->size() != 0);\n" +
                        "}\n" +
                        "\n" +
                        "function testSucceed():Any[*]\n" +
                        "{\n" +
                        "   myFunction([1, 2, 3], [4, 5, 6]);\n" +
                        "}\n" +
                        "\n" +
                        "function testFailPre():Any[*]\n" +
                        "{\n" +
                        "   myFunction([], [4, 5, 6]);\n" +
                        "}\n" +
                        "function testFailPost():Any[*]\n" +
                        "{\n" +
                        "   myFunction([1, 2, 3], [1, 2, 3]);\n" +
                        "}\n"
        );
        execute("testSucceed():Any[*]");
        PureExecutionException ePre = Assert.assertThrows(PureExecutionException.class, () -> execute("testFailPre():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (PRE):[notEmptyBefore] violated. (Function:myFunction_T_MANY__Any_MANY__T_MANY_)", "/test/repro.pure", 17, 4, 17, 4, 17, 13, ePre);
        PureExecutionException ePost = Assert.assertThrows(PureExecutionException.class, () -> execute("testFailPost():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint (POST):[notEmptyAfter] violated. (Function:myFunction_T_MANY__Any_MANY__T_MANY_)", "/test/repro.pure", 21, 4, 21, 4, 21, 13, ePost);
    }

    @Test
    public void testConstraintInClassWithMilestoning()
    {
        compileTestSource("/test/repro.pure",
                "Class <<temporal.businesstemporal>> MyClass\n" +
                        "[\n" +
                        "   differentName: $this.others($this.businessDate)->filter(o | $o.name == $this.name)->size() == 0\n" +
                        "]\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "   others: OtherClass[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class <<temporal.businesstemporal>> OtherClass\n" +
                        "{\n" +
                        "   name: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "function testSucceed():Any[*]\n" +
                        "{\n" +
                        "    let bd = %2023-01-11;\n" +
                        "    ^MyClass(name='me', businessDate=$bd, othersAllVersions=^OtherClass(name='you', businessDate=$bd));\n" +
                        "}\n" +
                        "\n" +
                        "function testFail():Any[*]\n" +
                        "{\n" +
                        "    let bd = %2023-01-11;\n" +
                        "    ^MyClass(name='me', businessDate=$bd, othersAllVersions=[^OtherClass(name='you', businessDate=$bd), ^OtherClass(name='me', businessDate=$bd)]);\n" +
                        "}\n"
        );
        execute("testSucceed():Any[*]");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[differentName] violated in the Class MyClass", "/test/repro.pure", 24, 5, 24, 5, 24, 146, e);
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        MutableList<CodeRepository> repositories = Lists.mutable.withAll(AbstractPureTestWithCoreCompiled.getCodeRepositories());
        CodeRepository test = new TestCodeRepositoryWithDependencies("test", null, repositories.detect(d -> d.getName().equals("platform")));
        repositories.add(test);
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(repositories));
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(EMPLOYEE_SOURCE_NAME, EMPLOYEE_SOURCE_CODE);
    }
}
