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

package org.finos.legend.pure.m3.tests;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestGeneralizationAtRuntime extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testSuperTypeStaticInstantiation()
    {
        compileTestSource(
                "^meta::pure::metamodel::PackageableElement a (name='wee')" +
                "function go():Any[*]" +
                "{" +
                "   assert(meta::pure::metamodel::PackageableElement.specializations->size() > 0, |'');\n" +
                "   print(a, 1);" +
                "}");
        this.execute("go():Any[*]");
        Assert.assertEquals("a instance PackageableElement\n" +
                "    name(Property):\n" +
                "        wee instance String\n" +
                "    referenceUsages(Property):\n" +
                "        Anonymous_StripedId instance ReferenceUsage\n" +
                "            offset(Property):\n" +
                "                [>1] 0 instance Integer\n" +
                "            owner(Property):\n" +
                "                [>1] Anonymous_StripedId instance InstanceValue\n" +
                "            propertyName(Property):\n" +
                "                [>1] values instance String", this.functionExecution.getConsole().getLine(0));
    }
}
