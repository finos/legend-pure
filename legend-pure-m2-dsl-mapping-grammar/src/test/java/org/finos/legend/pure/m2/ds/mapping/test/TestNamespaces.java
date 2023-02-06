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

package org.finos.legend.pure.m2.ds.mapping.test;

import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestNamespaces extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @Test
    public void testMappingNameConflict()
    {
        compileTestSource("mapping1.pure",
                "###Mapping\n" +
                        "Mapping test::MyMapping ()");
        CoreInstance myMapping = this.runtime.getCoreInstance("test::MyMapping");
        Assert.assertNotNull(myMapping);
        Assert.assertTrue(Instance.instanceOf(myMapping, M2MappingPaths.Mapping, this.processorSupport));

        try
        {
            compileTestSource("mapping2.pure",
                    "###Mapping\n" +
                            "Mapping test::MyMapping ()");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyMapping' already exists in the package 'test'", "mapping2.pure", 2, 1, 2, 15, 2, 26, e);
        }
    }
}
