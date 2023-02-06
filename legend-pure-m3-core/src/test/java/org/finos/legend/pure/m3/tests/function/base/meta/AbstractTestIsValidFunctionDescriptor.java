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

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestIsValidFunctionDescriptor extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testBothValidAndInvalidDescriptors()
    {
        compileTestSource(
                "function testValid():Boolean[1]\n" +
                        "{\n" +
                        "   meta::pure::functions::meta::isValidFunctionDescriptor('meta::pure::functions::meta::pathToElement(String[1]):PackageableElement[1]');\n" +
                        "}\n" +
                "function testInvalid():Boolean[1]\n" +
                        "{\n" +
                        "   meta::pure::functions::meta::isValidFunctionDescriptor('meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]');\n" +
                        "}");
        CoreInstance resultTrue = this.execute("testValid():Boolean[1]");
        Assert.assertEquals("true", resultTrue.getValueForMetaPropertyToMany(M3Properties.values).get(0).getName());
        CoreInstance resultFalse = this.execute("testInvalid():Boolean[1]");
        Assert.assertEquals("false", resultFalse.getValueForMetaPropertyToMany(M3Properties.values).get(0).getName());
    }
}
