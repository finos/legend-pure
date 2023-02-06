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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler.IncrementalCompilerTransaction;
import org.finos.legend.pure.m4.transaction.framework.ThreadLocalTransactionContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSimpleTransaction extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void clearRuntime() {
        runtime.delete("source2.pure");
    }

    @Test
    public void testSimpleGraph() throws Exception
    {
        this.compileTestSource("Class myTest::Product\n" +
                "{\n" +
                "   name : String[1];\n" +
                "}\n");

        Assert.assertEquals(1, this.processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());

        Source source = this.runtime.createInMemorySource("source2.pure", "Class myTest::Synonym\n" +
                               "{\n" +
                               "   name : String[1];\n" +
                               "}\n");
        IncrementalCompilerTransaction transaction = this.runtime.getIncrementalCompiler().newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            this.runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
            Assert.assertEquals(2, this.processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        }
        Assert.assertEquals(1, this.processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assert.assertEquals(2, this.processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
        }
        transaction.rollback();
        Assert.assertEquals(1, this.processorSupport.package_getByUserPath("myTest").getValueForMetaPropertyToMany(M3Properties.children).size());
    }

    @Test
    public void testFunction() throws Exception
    {
        Source source = this.runtime.createInMemorySource("source2.pure", "function myFunc():Nil[0]" +
                               "{" +
                               "    print('ok',1);" +
                               "}");
        IncrementalCompilerTransaction transaction = this.runtime.getIncrementalCompiler().newTransaction(true);
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            this.runtime.getIncrementalCompiler().compileInCurrentTransaction(source);
            // TODO fix this: if this is called, myFunc__Nil_0__ is cached in the context both inside and outside the scope of the transaction
//            Assert.assertNotNull(this.processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        }

        Assert.assertNull(this.processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        try (ThreadLocalTransactionContext ignore = transaction.openInCurrentThread())
        {
            Assert.assertNotNull(this.processorSupport.package_getByUserPath("myFunc__Nil_0_"));
        }

        transaction.commit();
        Assert.assertNotNull(this.processorSupport.package_getByUserPath("myFunc__Nil_0_"));
    }
}
