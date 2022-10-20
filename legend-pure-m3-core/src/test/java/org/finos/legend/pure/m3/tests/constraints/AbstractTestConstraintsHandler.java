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
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestConstraintsHandler extends AbstractPureTestWithCoreCompiled
{

    @Test
    public void testClassDefaultConstraintHandler()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
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
                "  let func = [];\n" +
                "  let r = dynamicNew(Employee,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2'\n" +
                "                  )->cast(@Employee);\n" +
                "}\n");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> compileAndExecute("testNew():Any[*]"));
        assertOriginatingPureException(PureExecutionException.class, "Constraint :[rule1] violated in the Class Employee", 19, 11, e);
    }

    @Test
    public void testClassInvalidConstraintHandler()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("fromString.pure",
                "Class Employee\n" +
                        "[\n" +
                        "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                        "]\n" +
                        "{\n" +
                        "   lastName:String[0..1];\n" +
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
                        "function constraintsManager(o:Any[1]):Any[*]\n" +
                        "{\n" +
                        "  [$o,$o];\n" +
                        "}\n" +
                        "function testNew():Any[*] {\n" +
                        "  let r = dynamicNew(Employee,\n" +
                        "                   [\n" +
                        "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                        "                   ],\n" +
                        "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                        "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                        "                   '2',\n" +
                        "                   constraintsManager_Any_1__Any_MANY_\n" +
                        "                  )->cast(@Employee);\n" +
                        "}\n"));

        assertOriginatingPureException(PureUnmatchedFunctionException.class, PureUnmatchedFunctionException.FUNCTION_UNMATCHED_MESSAGE + "dynamicNew(_:Class<Employee>[1],_:KeyValue[1],_:ConcreteFunctionDefinition<{Any[1], Property<Nil, Any|0..1>[1]->Any[0..1]}>[1],_:ConcreteFunctionDefinition<{Any[1], Property<Nil, Any|*>[1]->Any[*]}>[1],_:String[1],_:ConcreteFunctionDefinition<{Any[1]->Any[*]}>[1])\n" +
                PureUnmatchedFunctionException.EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE +
                PureUnmatchedFunctionException.NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE +
                "\tmeta::pure::functions::lang::dynamicNew(Class[1], KeyValue[*]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(Class[1], KeyValue[*], Function[0..1], Function[0..1], Any[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(Class[1], KeyValue[*], Function[0..1], Function[0..1], Any[0..1], Function[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*], Function[0..1], Function[0..1], Any[0..1]):Any[1]\n" +
                "\tmeta::pure::functions::lang::dynamicNew(GenericType[1], KeyValue[*], Function[0..1], Function[0..1], Any[0..1], Function[0..1]):Any[1]\n", 22, 11, e);
    }

    @Test
    public void testClassConstraintHandler()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  ^Employee(lastName='new');\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                "  let r = dynamicNew(Employee,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2',\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  )->cast(@Employee);\n" +
                " assert('new' == $r.lastName, |'');\n" +
                "}\n");

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerSignature()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  $o;\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                "  let r = ^ConstraintsOverride(constraintsManager=\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  );\n" +
                " assert(constraintsManager_Any_1__Any_1_ == $r.constraintsManager, |'');\n" +
                "}\n");

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerNoException()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
                "}\n" +
                "Class ConstraintResult" +
                "{" +
                "   instance:Any[1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  print('in handler', 1);\n" +
                "  print($o->genericType(), 1);\n" +
                "  assert(!$o->genericType().rawType->isEmpty(), |'no raw type');\n" +
                "  assert($o->genericType().rawType->toOne()->instanceOf(ElementWithConstraints), |'input is not a sub type of ElementWithConstraints');\n" +
                "  let constraints = $o->genericType().rawType->cast(@ElementWithConstraints).constraints;\n" +
                "  assert($constraints->size()>0, |'constraints should not be empty');\n" +
                "  ^ConstraintResult(instance=$o);\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                " let r1 = constraintsManager(^Employee(lastName='name')); \n" +
                " assert($r1->instanceOf(ConstraintResult), |'');\n" +
                " assert($r1->cast(@ConstraintResult).instance->instanceOf(Employee), |'');\n" +
                " assert('name' == $r1->at(0)->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');\n" +
                " let r = dynamicNew(Employee,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2',\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  );\n" +
                " assert($r->instanceOf(ConstraintResult), |'');\n" +
                " assert($r->cast(@ConstraintResult).instance->instanceOf(Employee), |'');\n" +
                " assert('1234567891000' == $r->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');\n" +
                "}\n");

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testGenericTypeConstraintHandlerNoException()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10,\n" +
                "   rule2: $this.lastName->toOne()->length() > 3\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
                "}\n" +
                "Class ConstraintResult\n" +
                "{" +
                "   instance:Any[1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  assert(!$o->genericType().rawType->isEmpty(), |'no raw type');\n" +
                "  assert($o->genericType().rawType->toOne()->instanceOf(ElementWithConstraints), |'input is not a sub type of ElementWithConstraints');\n" +
                "  let constraints = $o->genericType().rawType->cast(@ElementWithConstraints).constraints;\n" +
                "  assert($constraints->size()>0, |'constraints should not be empty');\n" +
                "  ^ConstraintResult(instance=$o);\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                " let r1 = constraintsManager(^Employee(lastName='name')); \n" +
                " assert($r1->instanceOf(ConstraintResult), |'');\n" +
                " assert($r1->cast(@ConstraintResult).instance->instanceOf(Employee), |'');\n" +
                " assert('name' == $r1->at(0)->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');\n" +
                " let r = dynamicNew(^GenericType(rawType=Employee),\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2',\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  );\n" +
                " assert($r->instanceOf(ConstraintResult), |'');\n" +
                " assert($r->cast(@ConstraintResult).instance->instanceOf(Employee), |'');\n" +
                " assert('1234567891000' == $r->cast(@ConstraintResult).instance->cast(@Employee).lastName, |'');\n" +
                "}\n");

        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testGenericTypeConstraintHandlerCopyAfterDynamicNew()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
                "}\n" +
                "Class ConstraintResult\n" +
                "{\n" +
                "   instance:Any[1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  $o;\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                " let r1 = constraintsManager(^Employee(lastName='name')); \n" +
                " assert($r1->instanceOf(Employee), |'');\n" +
                " let r = dynamicNew(^GenericType(rawType=Employee),\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='12345687458973425839855')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2',\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  );\n" +
                " assert($r->instanceOf(Employee), |'');\n" +
                " assert('12345687458973425839855' == $r->cast(@Employee).lastName, |'');\n" +
                " let emp = $r->cast(@Employee);\n" +
                " assert('123456789101010101' == ^$emp(lastName='123456789101010101').lastName, |'');\n" +
                "}\n");


        this.compileAndExecute("testNew():Any[*]");
    }

    @Test
    public void testClassConstraintHandlerCopyAfterDynamicNew()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
                "}\n" +
                "Class ConstraintResult\n" +
                "{\n" +
                "   instance:Any[1];\n" +
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
                "function constraintsManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  $o;\n" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                " let r1 = constraintsManager(^Employee(lastName='name')); \n" +
                " assert($r1->instanceOf(Employee), |'');\n" +
                " let r = dynamicNew(Employee,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='12345687458973425839855')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2',\n" +
                "                   constraintsManager_Any_1__Any_1_\n" +
                "                  )->cast(@Employee);\n" +
                " assert('12345687458973425839855' == $r.lastName, |'');\n" +
                " assert('123456789101010101' == ^$r(lastName='123456789101010101').lastName, |'');\n" +
                "}\n");
        this.compileAndExecute("testNew():Any[*]");
    }


    @Test
    public void testEvaluateConstraint()
    {
        compileTestSource("fromString.pure", "Class Employee\n" +
                "[\n" +
                "   rule1 : $this.lastName->toOne()->length() < 10\n" +
                "]\n" +
                "{\n" +
                "   lastName:String[0..1];\n" +
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
                "function constraintManager(o:Any[1]):Any[1]\n" +
                "{\n" +
                "  let failed = evaluate(Employee.constraints->at(0).functionDefinition,^List<Any>(values=$o))->toOne()->cast(@Boolean);\n" +
                "  print($failed,1);" +
                "  ^Pair<Boolean,Any>(first=$failed,second=$o);" +
                "}\n" +
                "function testNew():Any[*] {\n" +
                "  let r = dynamicNew(Employee,\n" +
                "                   [\n" +
                "                      ^KeyValue(key='lastName',value='1234567891000')\n" +
                "                   ],\n" +
                "                   getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                   getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                   '2'," +
                "                   constraintManager_Any_1__Any_1_\n" +
                "                  )->cast(@Pair<Boolean,Any>);\n" +
                "  assert(!$r.first, |'');\n" +
                "}\n");

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testConstraintManagerWithExtendedConstraintGrammar()
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
                "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                "{\n" +
                "  [];\n" +
                "}\n" +
                "\n" +
                "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                "{\n" +
                "  [];\n" +
                "}\n" +
                "\n" +
                "function constraintManager(o: Any[1]):Any[1]\n" +
                "{\n" +
                "   let constraints = $o->genericType()->genericTypeClass().constraints;\n" +
                "   ^List<Boolean>(values=$constraints->map(c | evaluate($c.functionDefinition, ^List<Any>(values=$o))->cast(@Boolean)->toOne())); \n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let res = Position->dynamicNew([\n" +
                "                            ^KeyValue(key='contractId', value='1'), \n" +
                "                            ^KeyValue(key='positionType', value='2'),\n" +
                "                            ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "                            ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "                         ],\n" +
                "                         getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                         getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                         '2',\n" +
                "                         constraintManager_Any_1__Any_1_\n" +
                "                        );\n" +
                "   assert($res->cast(@List<Boolean>).values == [false, true], |'');\n" +
                "}");

        this.execute("testNew():Any[*]");
    }

    @Test
    public void testConstraintManager2WithExtendedConstraintGrammar()
    {
        compileTestSource("fromString.pure", "Class Position\n" +
                "[\n" +
                "   c1\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~externalId       : 'My_Ext_Id_1'\n" +
                "      ~function         : $this.contractId->startsWith('A')\n" +
                "      ~message          : 'Contract ID: ' + $this.contractId\n" +
                "   ),\n" +
                "   c3\n" +
                "   (\n" +
                "      ~owner            : Finance\n" +
                "      ~externalId       : 'My_Ext_Id_2'\n" +
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
                "function getterOverrideToMany(o:Any[1], property:Property<Nil,Any|*>[1]):Any[*]\n" +
                "{\n" +
                "  [];\n" +
                "}\n" +
                "\n" +
                "function getterOverrideToOne(o:Any[1], property:Property<Nil,Any|0..1>[1]):Any[0..1]\n" +
                "{\n" +
                "  [];\n" +
                "}\n" +
                "\n" +
                "function constraintManager(o: Any[1]):Any[1]\n" +
                "{\n" +
                "   let constraints = $o->genericType()->genericTypeClass().constraints;\n" +
                "   ^List<Any>(values=$constraints->map(c | [evaluate($c.functionDefinition, ^List<Any>(values=$o))->cast(@Boolean)->toOne()]->concatenate($c.externalId->toOne())->concatenate(if($c.messageFunction->isEmpty(),|[],|evaluate($c.messageFunction->toOne(), ^List<Any>(values=$o))->cast(@String)->toOne())))); \n" +
                "}\n" +
                "\n" +
                "function testNew():Any[*]\n" +
                "{\n" +
                "   let res = Position->dynamicNew([\n" +
                "                            ^KeyValue(key='contractId', value='1'), \n" +
                "                            ^KeyValue(key='positionType', value='2'),\n" +
                "                            ^KeyValue(key='startDate', value=%2010-01-01),\n" +
                "                            ^KeyValue(key='endDate', value=%2011-01-01)\n" +
                "                         ],\n" +
                "                         getterOverrideToOne_Any_1__Property_1__Any_$0_1$_,\n" +
                "                         getterOverrideToMany_Any_1__Property_1__Any_MANY_,\n" +
                "                         '2',\n" +
                "                         constraintManager_Any_1__Any_1_\n" +
                "                        );\n" +
                "   assert($res->cast(@List<Any>).values == [false, 'My_Ext_Id_1', 'Contract ID: 1', true, 'My_Ext_Id_2'], |'');\n" +
                "}");

        this.execute("testNew():Any[*]");
    }

}
