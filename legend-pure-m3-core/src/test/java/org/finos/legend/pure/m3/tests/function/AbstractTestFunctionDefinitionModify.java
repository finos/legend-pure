// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.TestCodeRepositoryWithDependencies;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractTestFunctionDefinitionModify extends AbstractPureTestWithCoreCompiled
{
    private static final String DECLARATION = "" +
            "function performCompare(origLambda : FunctionDefinition<{String[1]->String[1]}>[1], \n" +
            "                   mutator : FunctionDefinition<{FunctionDefinition<{String[1]->String[1]}>[1],\n" +
            "                                                 FunctionDefinition<{String[1]->String[1]}>[1]\n" +
            "                                                 ->FunctionDefinition<{String[1]->String[1]}>[1]\n" +
            "                                               }>[1],\n" +
            "                   expectedLambda : FunctionDefinition<{String[1]->String[1]}>[1]):Boolean[1]\n" +
            "{ \n" +
            "    if($origLambda->openVariableValues()->keyValues()->isEmpty(), " +
            "         | ''," +
            "         | print('WARNING: Copy/clone of lambdas with open variables fully supported, failures may occur', 1)" +
            "         );\n" +
            "  \n" +
            "  let newLambda = $mutator->eval($origLambda, $expectedLambda);\n" +
            "  \n" +
            "  let inputVal = 'hello';\n" +
            "\n" +
            "  print('Evaluating $origLambda\\n', 1);\n" +
            "  let resultOrigLambda = $origLambda->eval($inputVal);\n" +
            "  print('Evaluating $expectedLambda\\n', 1);\n" +
            "  let resultExpectedLambda = $expectedLambda->eval($inputVal);\n" +
            "  print('Evaluating $newLambda\\n', 1);\n" +
            "  let resultNewLambda = $newLambda->eval($inputVal);\n" +
            "\n" +
            "  print('$resultOrigLambda: ' + $resultOrigLambda + '\\n', 1);\n" +
            "  print('$resultExpectedLambda: ' + $resultExpectedLambda + '\\n', 1);\n" +
            "  print('$resultNewLambda: ' + $resultNewLambda + '\\n', 1);\n" +
            "  print('$resultOrigLambda sourceInformation: ', 1);\n" +
            "  print($resultOrigLambda->sourceInformation(), 1);\n" +
            "  print('$resultNewLambda sourceInformation: ', 1);\n" +
            "  print($resultNewLambda->sourceInformation(), 1);\n" +
            "\n" +
            "  //if($resultNewLambda == $resultOrigLambda,\n" +
            "  //     | fail('Modified lambda result not changed, got original: \\'' + $resultOrigLambda +  '\\''),\n" +
            "  //     | true);\n" +
            "\n" +
            "  if($resultNewLambda != $resultExpectedLambda,\n" +
            "       | fail('Modified lambda result not as expected, expected: \\'' + $resultExpectedLambda +  '\\' got: \\'' + $resultNewLambda +  '\\''),\n" +
            "       | true);\n" +
            "}\n" +
            "\n" +
            "\n" +
            "function test::hierarchicalProperties(class:Class<Any>[1]):Property<Nil,Any|*>[*]\n" +
            "{\n" +
            "   if($class==Any,\n" +
            "      | [],\n" +
            "      | $class.properties->concatenate($class.generalizations->map(g| test::hierarchicalProperties($g.general.rawType->cast(@Class<Any>)->toOne())))->removeDuplicates()\n" +
            "   );\n" +
            "}\n" +
            "\n" +
            "function modifyExpressionSequenceWithDynamicNew<T>(fd:FunctionDefinition<T>[1], es : ValueSpecification[1..*]) : FunctionDefinition<T>[1]\n" +
            "{\n" +
            "    let genericType = ^KeyValue(key='classifierGenericType', value= $fd.classifierGenericType);\n" +
            "\n" +
            "    let fdClass = $fd->type()->cast(@Class<Any>);\n" +
            "    let properties = $fdClass->test::hierarchicalProperties()->map(p|\n" +
            "      if($p.name == 'expressionSequence', \n" +
            "        | ^KeyValue(key=$p.name->toOne(), value= $es), \n" +
            "        | ^KeyValue(key=$p.name->toOne(), value= $p->eval($fd))\n" +
            "        );\n" +
            "      );\n" +
            "\n" +
            "    dynamicNew($fd.classifierGenericType->toOne(), $properties->concatenate($genericType))->cast($fd);\n" +
            "}\n" +
            "\n";

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.compile();
    }

    @Test
    public void testConcreteFunctionDefinitionModifyWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function xx::myFunc(s : String[1]) : String[1] { 'answer: ' + $s; }\n"
                        + "\n"
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origFunc = xx::myFunc_String_1__String_1_;\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f(expressionSequence = $f2->evaluateAndDeactivate().expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda);\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testConcreteFunctionDefinitionModifyWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function xx::myFunc(s : String[1]) : String[1] { 'answer: ' + $s; }\n"
                        + "\n"
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origFunc = xx::myFunc_String_1__String_1_;\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f2.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda);\n" +
                        "  performCompare($origFunc, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testLambdaModifyWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origLambda = {s:String[1]|'answer: ' + $s};\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f(expressionSequence = $f2->evaluateAndDeactivate().expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda);\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @Ignore(value = "This is not supported (Dynamic new can't pass in the variable context / values for open variables)")
    public void testLambdaCloneWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n" +
                        "  let openVarValue = 'xyz';\n" +
                        "  \n" +
                        "  let origLambda = {s:String[1]|'answer: ' + $openVarValue + '/' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $origLambda);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    @Ignore(value = "This is not supported (copy doesn't in the variable context / values for open variables)")
    public void testLambdaCloneWithCopyConstructor()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n" +
                        "  let openVarValue = 'xyz';\n" +
                        "  \n" +
                        "  let origLambda = {s:String[1]|'answer: ' + $openVarValue + '/' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    ^$f()\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $origLambda);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    @Test
    public void testLambdaModifyWithDynamicNew()
    {
        compileTestSource("testSource.pure",
                DECLARATION
                        + "function test():Any[*] \n{"
                        + "\n"
                        + "  let origLambda = {s:String[1]|'answer: ' + $s};\n" +
                        "  let replacementLambda = {s:String[1]|'not your input:' + $s};\n" +
                        "  let replacementLambda2 = {s:String[1]|'not your input2:' + $s};\n" +
                        "  \n" +
                        "  let mutator = {f:FunctionDefinition<{String[1]->String[1]}>[1], f2:FunctionDefinition<{String[1]->String[1]}>[1]|\n" +
                        "    $f->modifyExpressionSequenceWithDynamicNew($f2.expressionSequence)\n" +
                        "    };\n" +
                        "\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda);\n" +
                        "  performCompare($origLambda, $mutator, $replacementLambda2);\n" +
                        "\n"
                        + "}\n");

        CoreInstance func = runtime.getFunction("test():Any[*]");
        functionExecution.start(func, Lists.immutable.empty());
    }

    protected static MutableCodeStorage getCodeStorage()
    {
        CodeRepository platform = CodeRepository.newPlatformCodeRepository();
        CodeRepository test = new TestCodeRepositoryWithDependencies("test", null, platform);
        return new PureCodeStorage(null, new ClassLoaderCodeStorage(platform, test));
    }
}
