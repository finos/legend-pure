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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestDynamicNewConstraints extends AbstractPureTestWithCoreCompiled
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
    public void testExtendedConstraintExecutionCanEvaluateConstraintMessage()
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
        execute("testNew():Any[*]");
    }
    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithOwnerGlobal()
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
                "   Position->dynamicNew([\n" +
                "       ^KeyValue(key='contractId', value='1'), \n" +
                "       ^KeyValue(key='positionType', value='2'),\n" +
                "       ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "       ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "    ])\n" +
                "}"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 28, 14, e);
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testFail():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[superPeopleHaveNoDuplicates] violated in the Class SuperPeople, Message: test", "/test/repro.pure", 45, 4, 45, 4, 45, 128, e);
    }


    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsOtherConstraint()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
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

        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c3] violated in the Class Position", 27, 14, e);
    }

    @Test
    public void testConstraintWithDynamicNew()
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 11, 11, e);
    }

    @Test
    public void testExtendedConstraintExecutionDynamicNewFailsWithNoOwner()
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
                "}\n"
        );
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[c1] violated in the Class Position, Message: Contract ID: 1", 23, 14, e);
    }

    @Test
    public void testConstraintWithGenericTypeDynamicNew()
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
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 11, 11, e);
    }

    @Test
    public void testConstraintWithDynamicNewNoOverrides()
    {
        compileTestSource("fromString.pure", "Class EmployeeWithError" +
                "[" +
                "   rule1 : $this.lastName->toOne()->length() < 10" +
                "]" +
                "{" +
                "   lastName:String[0..1];" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                "  let r = dynamicNew(EmployeeWithError,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ])->cast(@EmployeeWithError);\n" +
                "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("testNew():Any[*]"));
        assertPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class EmployeeWithError", 3, 11, e);
    }

    @Test
    public void testExtendedConstraintExecutionCanEvaluateConstraint()
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
        execute("testNew():Any[*]");
    }


    @Test
    public void testExtendedConstraintExecutionDynamicNewPassesWithOwner()
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
                "}\n"
        );
        execute("testNew():Any[*]");
    }


}
