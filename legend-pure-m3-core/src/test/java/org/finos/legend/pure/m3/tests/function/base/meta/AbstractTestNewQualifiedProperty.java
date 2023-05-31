// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

public class AbstractTestNewQualifiedProperty extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void standardCall()
    {
        String source =
            "function go():Any[*]\n" +
            "{\n" +
                "let classA = 'meta::pure::functions::meta::A'->newClass();\n" +
                "let classB = 'meta::pure::functions::meta::B'->newClass();\n" +
                "let qualifiedProperty = newQualifiedProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne, [^VariableExpression(name = 'newName', multiplicity = ZeroOne, genericType = ^GenericType(rawType = Any))]);\n" +
                "assert('a' == $qualifiedProperty.name, |'Expected qualified property to have name');\n" +
                "assert('a' == $qualifiedProperty.functionName, |'Expected qualified property to have name');\n" +
                "assert('B' == $qualifiedProperty.owner.name->toOne(), |'Expected qualified property owner to have name');\n" +
                "assert(PureOne == $qualifiedProperty.multiplicity, |'Expected qualified property multiplicity');\n" +
                "assert('A' == $qualifiedProperty.genericType.rawType->toOne().name, |'Expected qualified property generic type to have name');\n" +
                "assert('QualifiedProperty' == $qualifiedProperty.classifierGenericType.rawType->toOne().name, |'Expected qualified property generic type');\n" +
                "let typeArguments = $qualifiedProperty.classifierGenericType.typeArguments;\n" +
                "assert(1 == $typeArguments->size(), |'Expected qualified property to have one type argument');\n" +
                "assert($typeArguments->toOne().rawType->toOne()->instanceOf(FunctionType), |'Expected qualified property type argument to be instance of FunctionType');\n" +
                "assert('A' == $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnType.rawType->toOne().name, |'Expected function type return type to be qualified property return type');\n" + 
                "assert(PureOne == $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).returnMultiplicity, |'Expected function type return multiplicity to be qualified property return multiplicity');\n" + 
                "let params = $typeArguments->toOne().rawType->toOne()->cast(@FunctionType).parameters->evaluateAndDeactivate();\n" +
                "assert(1 == $params->size(), |'Expected function type to have one parameter');\n" +
                "assert($params->toOne()->instanceOf(Any), |'Expected function type to have one parameter');\n" +
                "assert(ZeroOne == $params->toOne().multiplicity, |'Expected function type parameter multiplicity to be ZeroOne');\n" +
                "assert('newName' == $params.name, |'Expected function type parameter name to be name');\n" + 
            "}";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, Lists.immutable.empty());
    }
}
