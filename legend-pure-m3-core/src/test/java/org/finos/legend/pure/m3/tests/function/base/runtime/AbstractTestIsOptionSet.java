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

package org.finos.legend.pure.m3.tests.function.base.runtime;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.serialization.runtime.RuntimeOptions;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

public abstract class AbstractTestIsOptionSet extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testOptionThatIsSetOn()
    {
        compileTestSource(
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::pure::runtime::isOptionSet('TestSetOn');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("true", ValueSpecification.getValue(result, this.processorSupport).getName());
    }

    @Test
    public void testOptionThatIsSetOff()
    {
        compileTestSource(
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::pure::runtime::isOptionSet('TestSetOff');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("false", ValueSpecification.getValue(result, this.processorSupport).getName());
    }

    @Test
    public void testOptionThatIsNotSet()
    {
        compileTestSource(
                "function test():Boolean[1]\n" +
                        "{\n" +
                        "    meta::pure::runtime::isOptionSet('TestUnset');" +
                        "}\n");
        CoreInstance result = this.execute("test():Boolean[1]");
        Assert.assertEquals("false", ValueSpecification.getValue(result, this.processorSupport).getName());
    }

    protected RuntimeOptions getOptions()
    {
        final Map<String, Boolean> testOptions = new TreeMap<>();
        testOptions.put("TestSetOn", true);
        testOptions.put("TestSetOff", false);
        return new RuntimeOptions()
        {
            @Override
            public boolean isOptionSet(String name)
            {
                return testOptions.containsKey(name) && testOptions.get(name);
            }
        };
    }

}
