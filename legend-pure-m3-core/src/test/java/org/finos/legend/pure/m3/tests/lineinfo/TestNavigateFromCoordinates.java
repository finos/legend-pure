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

package org.finos.legend.pure.m3.tests.lineinfo;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Scanner;

public class TestNavigateFromCoordinates extends AbstractPureTestWithCoreCompiledPlatform
{
    // Not working yet
    //      properties used in a new
    //      profile in a stereotype (routes to the value)
    //      profile in a tagged value (routes to the tag)
    //      value of an enum (routes to extractEnumValue)

    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testNavigation1() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", new Scanner(TestNavigateFromCoordinates.class.getResourceAsStream("/org/finos/legend/pure/m3/tests/lineinfo/file1.pure")).useDelimiter("\\Z").next());
        this.runtime.compile();
        Assert.assertEquals(this.fromPackage("A"), this.get(28,6));
        Assert.assertEquals(this.fromPackage("B"), this.get(30,16));
        Assert.assertEquals(this.fromPackage("String"), this.get(17,17));
        Assert.assertEquals(this.fromPackage("meta::pure::metamodel::type::Any"), this.get(18,20));
        Assert.assertEquals(this.fromPackage("String"), this.get(35,24));
        Assert.assertEquals(this.fromPackage("Integer"), this.get(35,35));
        Assert.assertEquals(this.fromPackage("testFunc_String_1__Integer_1_"), this.get(32,10));
    }

    @Test
    public void testNavigation2() throws Exception
    {
        this.runtime.createInMemorySource("sourceId.pure", new Scanner(TestNavigateFromCoordinates.class.getResourceAsStream("/org/finos/legend/pure/m3/tests/lineinfo/file2.pure")).useDelimiter("\\Z").next());
        this.runtime.compile();
        Assert.assertEquals("deprecated", this.get(17,10).getName());
        Assert.assertEquals("deprecated", this.get(17,20).getName());
        Assert.assertEquals("doc", this.get(17,28).getName());
        Assert.assertEquals("doc", this.get(17,32).getName());
        Assert.assertEquals(this.fromPackage("myEnum"), this.get(29,13));
    }

    private CoreInstance fromPackage(String element)
    {
        return processorSupport.package_getByUserPath(element);
    }

    private CoreInstance get(int x, int y)
    {
        return this.runtime.getSourceById("sourceId.pure").navigate(x, y, this.runtime.getProcessorSupport());
    }

}
