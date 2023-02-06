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

package org.finos.legend.pure.m3.tests.incremental._package;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPureRuntimePackage extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        setUp();
    }

    @Test
    public void testPackageRemovedWhenEmpty()
    {
        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        int size = this.repository.serialize().length;

        compileTestSource("source.pure", "Class test_package1::test_package2::TestClass {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));

        this.runtime.delete("source.pure");
        this.runtime.compile();
        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assert.assertEquals(size, this.repository.serialize().length);
    }

    @Test
    public void testPackageNotRemovedWhenNotEmpty()
    {
        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass1 {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        compileTestSource("source2.pure", "Class test_package1::test_package2::TestClass2 {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass2"));

        this.runtime.delete("source1.pure");
        this.runtime.compile();
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass2"));
    }

    @Test
    public void testMixedPackageRemovedAndNot()
    {
        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass1 {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        compileTestSource("source2.pure", "Class test_package1::test_package3::TestClass2 {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3::TestClass2"));

        this.runtime.delete("source1.pure");
        this.runtime.compile();
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3::TestClass2"));
    }

    @Test
    public void testPackageWithReferenceRemoved()
    {
        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        compileTestSource("source1.pure", "Class test_package1::test_package2::TestClass {}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        compileTestSource("source2.pure",
                "function test_package1::test_package3::testFn():Package[1]\n" +
                        "{\n" +
                        "   test_package1::test_package2\n" +
                        "}");
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package2::TestClass"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_package3::testFn__Package_1_"));

        this.runtime.delete("source1.pure");
        try
        {
            this.runtime.compile();
            Assert.fail("Expected compile exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "test_package1::test_package2 has not been defined!", "source2.pure", 3, 19, e);
        }
    }

    @Test
    public void testPackageDeleteWithPropertyWithSameNameAsPackage()
    {
        String sourceId = "source.pure";
        String sourceCode = "Class test_package1::test_name::TestClass1\n" +
                "{\n" +
                "   test_name : String[1];\n" +
                "}";

        Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_name"));
        Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_name::TestClass1"));
        int beforeSize = this.repository.serialize().length;

        compileTestSource(sourceId, sourceCode);
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_name"));
        Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_name::TestClass1"));
        int afterSize = this.repository.serialize().length;

        for (int i = 0; i < 10; i++)
        {
            this.runtime.delete(sourceId);
            this.runtime.compile();
            Assert.assertNull(this.runtime.getCoreInstance("test_package1"));
            Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_name"));
            Assert.assertNull(this.runtime.getCoreInstance("test_package1::test_name::TestClass1"));
            Assert.assertEquals("Failed on iteration #" + i, beforeSize, this.repository.serialize().length);

            compileTestSource(sourceId, sourceCode);
            Assert.assertNotNull(this.runtime.getCoreInstance("test_package1"));
            Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_name"));
            Assert.assertNotNull(this.runtime.getCoreInstance("test_package1::test_name::TestClass1"));
            Assert.assertEquals("Failed on iteration #" + i, afterSize, this.repository.serialize().length);
        }
    }
}
