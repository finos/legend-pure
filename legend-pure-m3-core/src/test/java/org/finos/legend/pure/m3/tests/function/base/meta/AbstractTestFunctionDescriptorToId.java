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
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestFunctionDescriptorToId extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testInvalidFunctionDescriptorException()
    {
        try
        {
            compileTestSource("testFunc.pure",
                    "function test():String[1]\n" +
                    "{\n" +
                    "   meta::pure::functions::meta::functionDescriptorToId('meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]');\n" +
                    "}\n");
            this.execute("test():String[1]");
            Assert.fail();
        }
        catch (Exception e)
        {
            PureException pe = PureException.findPureException(e);
            Assert.assertNotNull(pe);
            Assert.assertTrue(pe instanceof PureExecutionException);
            PureException originalPE = pe.getOriginatingPureException();
            Assert.assertNotNull(originalPE);
            Assert.assertTrue(originalPE instanceof PureExecutionException);
            Assert.assertEquals("Invalid function descriptor: meta::pure::functions::meta::pathToElement(path:String[1]):PackageableElement[1]", originalPE.getInfo());
        }
    }

    @Test
    public void testCorrectDescriptorNoException()
    {
            compileTestSource("testFunc.pure",
                    "function test():String[1]\n" +
                    "{\n" +
                    "   meta::pure::functions::meta::functionDescriptorToId('meta::pure::functions::meta::pathToElement(String[1]):PackageableElement[1]');\n" +
                    "}\n");
        CoreInstance result = this.execute("test():String[1]");
        Assert.assertEquals("meta::pure::functions::meta::pathToElement_String_1__PackageableElement_1_", result.getValueForMetaPropertyToMany(M3Properties.values).get(0).getName());
    }

    @Test
    public void testUnitFunctionDescriptorNoException()
    {
        compileTestSource("testModel.pure",
                "Measure pkg::Mass\n" +
                        "{\n" +
                        "   *Gram: x -> $x;\n" +
                        "   Kilogram: x -> $x*1000;\n" +
                        "   Pound: x -> $x*453.59;\n" +
                        "}\n");
        compileTestSource("testFunc.pure",
                "import pkg::*;\n" +
                        "function my::test::testUnits(k: Mass~Kilogram[1]):Mass~Kilogram[1]\n" +
                        "{\n" +
                        "   $k;\n" +
                        "}\n" +
                        "function test():String[1]\n" +
                        "{\n" +
                        "   meta::pure::functions::meta::functionDescriptorToId('my::test::testUnits(Mass~Kilogram[1]):Mass~Kilogram[1]');\n" +
                        "}\n");
        CoreInstance result = this.execute("test():String[1]");
        Assert.assertEquals("my::test::testUnits_Mass~Kilogram_1__Mass~Kilogram_1_", result.getValueForMetaPropertyToMany(M3Properties.values).get(0).getName());
    }
}
