// Copyright 2021 Goldman Sachs
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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Test;

public abstract class AbstractTestNewAssociation extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void standardCall()
    {
        String[] rawSource = {
                "function go():Any[*]",
                "{",
                    "let classA = 'meta::pure::functions::meta::A'->newClass();",
                    "let classB = 'meta::pure::functions::meta::B'->newClass();",
                    "let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);",
                    "let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);",
                    "let newAssociation = 'meta::pure::functions::meta::A_B'->newAssociation($propertyA, $propertyB);",
                    "assert('A_B' == $newAssociation.name, |'');",
                    "assert('meta' == $newAssociation.package.name, |'');",
                    "assert('meta::pure::functions::meta::A_B' == $newAssociation->elementToPath(), |'');",
                    "assert('a' == $newAssociation.properties->at(0).name, |'not a');",
                    "assert('b' == $newAssociation.properties->at(1).name, |'');",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void callWithEmptyPackage()
    {
        String[] rawSource = {
                "function go():Any[*]",
                "{",
                    "let classA = 'A'->newClass();",
                    "let classB = 'B'->newClass();",
                    "let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);",
                    "let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);",
                    "let newAssociation = 'A_B'->newAssociation($propertyA, $propertyB);",
                    "assert('A_B' == $newAssociation.name, |'');",
                    "assert('A_B' == $newAssociation->elementToPath(), |'');",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void callWithEmpty()
    {
        String[] rawSource = {
                "function go():Any[*]",
                "{",
                    "let classA = 'A'->newClass();",
                    "let classB = 'B'->newClass();",
                    "let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);",
                    "let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);",
                    "let newAssociation = ''->newAssociation($propertyA, $propertyB);",
                    "assert('' == $newAssociation.name, |'');",
                    "assert('' == $newAssociation->elementToPath(), |'');",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

    @Test
    public void newPackage()
    {
        String[] rawSource = {
                "function go():Any[*]",
                "{",
                    "let classA = 'A'->newClass();",
                    "let classB = 'B'->newClass();",
                    "let propertyA = newProperty('a', ^GenericType(rawType=$classB), ^GenericType(rawType=$classA), PureOne);",
                    "let propertyB = newProperty('b', ^GenericType(rawType=$classA), ^GenericType(rawType=$classB), ZeroMany);",
                    "let newAssociation = 'foo::bar::A_B'->newAssociation($propertyA, $propertyB);",
                    "assert('foo::bar::A_B' == $newAssociation->elementToPath(), |'');",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

}
