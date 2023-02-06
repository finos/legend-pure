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

package org.finos.legend.pure.m3.tests.function.base.meta;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public abstract class AbstractTestNewLambdaFunction extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void standardCall()
    {
        String[] rawSource = {
                "function a(func:LambdaFunction<Any>[1]):String[1]" +
                "{" +
                "   let funcType = $func->genericType().typeArguments->at(0).rawType->toOne()->cast(@FunctionType);\n" +
                "   $funcType.parameters->evaluateAndDeactivate()->map(v | $v.name)->joinStrings(', ');" +
                "}" +
                "function go():Any[*]",
                "{",
                "   let ftype = ^FunctionType(parameters=^VariableExpression(name='ok', genericType=^GenericType(rawType=String), multiplicity=PureOne), returnType=^GenericType(rawType=String), returnMultiplicity=PureOne);" +
                "   let newLambda = meta::pure::functions::meta::newLambdaFunction($ftype);",
                "   assert('ok' == $newLambda->a(), |'');" +
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.runtime.createInMemorySource("StandardCall.pure", source);
        this.runtime.compile();
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

}
