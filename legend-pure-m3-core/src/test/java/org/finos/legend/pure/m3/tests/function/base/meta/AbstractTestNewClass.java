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

public abstract class AbstractTestNewClass extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void standardCall()
    {
        String[] rawSource = {
                "function go():Any[*]",
                "{",
                    "let newClass = 'meta::pure::functions::meta::newClass'->newClass();",
                    "assert('newClass' == $newClass.name, |'');",
                    "assert('meta' == $newClass.package.name, |'');",
                    "assert('meta::pure::functions::meta::newClass' == $newClass->elementToPath(), |'');",
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
                "let newClass = 'MyNewClass'->newClass();",
                "assert('MyNewClass' == $newClass.name, |'');",
                "assert('MyNewClass' == $newClass->elementToPath(), |'');",
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
                "let newClass = ''->newClass();",
                "assert('' == $newClass.name, |'');",
                "assert('' == $newClass->elementToPath(), |'');",
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
                    "let newClass = 'foo::bar::newClass'->newClass();",
                    "assert('foo::bar::newClass' == $newClass->elementToPath(), |'');",
                "}"
        };
        String source = StringUtils.join(rawSource, "\n") + "\n";

        this.compileTestSource("StandardCall.pure", source);
        CoreInstance func = this.runtime.getFunction("go():Any[*]");
        this.functionExecution.start(func, FastList.<CoreInstance>newList());
    }

}
