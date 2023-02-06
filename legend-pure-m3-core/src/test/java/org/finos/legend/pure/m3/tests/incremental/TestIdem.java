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

package org.finos.legend.pure.m3.tests.incremental;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestIdem extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("sourceId.pure");
    }

    @Test
    public void testClass() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "Class U{b:String[1];}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testFunction() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "function go():Nil[0]{[]}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testPropertyCollectUsage() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 1; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "function go():Any[*]{ConcreteFunctionDefinition.all().name}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testFunctionWithBody() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "function a():Integer[1]{1+1}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testProfile() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "Profile p {tags:[a,b];}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testAssociation() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "Class A{}\n" +
                                                     "Class B{}\n" +
                                                     "Association a {a:A[1];b:B[1];}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }

    @Test
    public void testEnumeration() throws Exception
    {
        int size = this.repository.serialize().length;
        for (int i = 0; i < 10; i++)
        {
            this.runtime.createInMemorySource("sourceId.pure", "Enum e {A}");
            this.runtime.compile();
            this.runtime.delete("sourceId.pure");
            this.runtime.compile();
            Assert.assertEquals("Failed on iteration #" + i, size, this.repository.serialize().length);
        }
    }
}
