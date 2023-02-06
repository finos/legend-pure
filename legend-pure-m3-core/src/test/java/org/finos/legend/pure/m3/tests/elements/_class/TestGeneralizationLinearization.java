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

package org.finos.legend.pure.m3.tests.elements._class;

import java.util.regex.Pattern;

import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGeneralizationLinearization extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @After
    public void cleanRuntime() {
        runtime.delete("fromString.pure");
    }

    @Test
    public void testSimpleLoop()
    {
        // A
        // |
        // A
        assertCompilationException("Class A extends A {}");
    }

    @Test
    public void testIndirectLoop()
    {
        // A
        // |
        // B
        // |
        // A
        assertCompilationException("Class A extends B {}\n" +
                "Class B extends A {}");
    }

    @Test
    public void testVeryIndirectLoop()
    {
        // B
        // |
        // C
        // |
        // B
        // |
        // A
        assertCompilationException("Class A extends B {}\n" +
                "Class B extends C {}\n" +
                "Class C extends B {}");
    }

    @Test
    public void testInvalidComplexGeneralization1()
    {
        //    E
        //  /   \
        // C     D
        //  \  / |
        //   B  /
        //   | /
        //   A
        // (but A->D precedes A->B)
        //
        // This is invalid because D must precede B in the linearization since it
        // precedes it in A's generalizations, but B must precede D since D is a
        // generalization of it.
        assertCompilationException("Class A extends D, B {}\n" +
                "Class B extends C, D {}\n" +
                "Class C extends E {}\n" +
                "Class D extends E {}\n" +
                "Class E {}");
    }

    @Test
    public void testInvalidComplexGeneralization2() throws PureCompilationException
    {
        //   ________
        //  /        \
        // |    F     |
        // |  /   \   |
        // |_D     E  |
        //   \  /  | /
        //    B    C
        //    | /
        //    A
        //
        // This is invalid because D must precede E in the linearization since it
        // precedes it in B's generalizations, but E must precede D in the
        // linearization since it precedes it in C's generalizations.
        assertCompilationException("Class A extends B, C {}\n" +
                "Class B extends D, E {}\n" +
                "Class C extends E, D {}\n" +
                "Class D extends F {}\n" +
                "Class E extends F {}\n" +
                "Class F {}");
    }

    private void assertCompilationException(String code)
    {
        try
        {
            compileTestSource("fromString.pure",code);
            Assert.fail("Expected compilation error from:\n" + code);
        }
        catch (RuntimeException e)
        {
            assertPureException(PureCompilationException.class, Pattern.compile("Inconsistent generalization hierarchy for .*"), e);
        }
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsWithOneOrphanType() {
        String pureSource = "Class A{}";
        compileTestSource("fromString.pure",pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assert.assertEquals(1, leafTypes.size());
        Assert.assertEquals(classA, leafTypes.getFirst());
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsInComplexStructureWithOneSharedParent() {
        //     F
        //   /   \
        //  D     E
        //   \  /  |
        //    B    C
        //    | /
        //    A
        //
        String pureSource = "Class A extends B, C {}\n" +
                "Class B extends D, E {}\n" +
                "Class C extends E {}\n" +
                "Class D extends F {}\n" +
                "Class E extends F {}\n" +
                "Class F {}";
        compileTestSource("fromString.pure",pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assert.assertEquals(1, leafTypes.size());
        Assert.assertEquals(this.runtime.getCoreInstance("F"), leafTypes.getFirst());
    }

    @Test
    public void testGetTopMostNonTopTypeGeneralizationsInComplexStructureWithMultipleParents() {
        //  D     E
        //   \  /  |
        //    B    C
        //    | /
        //    A
        //
        String pureSource = "Class A extends B, C {}\n" +
                "Class B extends D, E {}\n" +
                "Class C extends E {}\n" +
                "Class D {}\n" +
                "Class E {}";
        compileTestSource("fromString.pure",pureSource);
        CoreInstance classA = this.runtime.getCoreInstance("A");
        MutableSet<CoreInstance> leafTypes = Type.getTopMostNonTopTypeGeneralizations(classA, this.processorSupport);
        Assert.assertEquals(2, leafTypes.size());
        Assert.assertTrue(leafTypes.contains(this.runtime.getCoreInstance("E")));
        Assert.assertTrue(leafTypes.contains(this.runtime.getCoreInstance("D")));
    }
}
