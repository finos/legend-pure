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

package org.finos.legend.pure.m2.dsl.diagram.test.incremental;

import org.finos.legend.pure.m2.dsl.diagram.M2DiagramPaths;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDiagramValidation extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime();
    }

    @Test
    public void testDiagramNameConflict()
    {
        compileTestSource("diagram1.pure",
                "###Diagram\n" +
                        "Diagram test::MyDiagram {}");
        CoreInstance myDiagram = this.runtime.getCoreInstance("test::MyDiagram");
        Assert.assertNotNull(myDiagram);
        Assert.assertTrue(Instance.instanceOf(myDiagram, M2DiagramPaths.Diagram, this.processorSupport));

        try
        {
            compileTestSource("diagram2.pure",
                    "###Diagram\n" +
                            "Diagram test::MyDiagram {}");
            Assert.fail("Expected compilation error");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "The element 'MyDiagram' already exists in the package 'test'", "diagram2.pure", 2, 1, 2, 15, 2, 26, e);
        }
    }
}
