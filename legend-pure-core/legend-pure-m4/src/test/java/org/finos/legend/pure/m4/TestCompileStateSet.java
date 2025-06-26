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

package org.finos.legend.pure.m4;

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.junit.Assert;
import org.junit.Test;

public class TestCompileStateSet
{
    @Test
    public void testGetBitFlag()
    {
        for (CompileState state : CompileState.values())
        {
            Assert.assertEquals((1 << state.ordinal()), CompileStateSet.getBitFlag(state));
        }
    }

    @Test
    public void testToBitSet()
    {
        for (CompileState state1 : CompileState.values())
        {
            Assert.assertEquals(CompileStateSet.getBitFlag(state1), CompileStateSet.toBitSet(state1));
            for (CompileState state2 : CompileState.values())
            {
                Assert.assertEquals(CompileStateSet.getBitFlag(state1) | CompileStateSet.getBitFlag(state2), CompileStateSet.toBitSet(state1, state2));
                for (CompileState state3 : CompileState.values())
                {
                    Assert.assertEquals(CompileStateSet.getBitFlag(state1) | CompileStateSet.getBitFlag(state2) | CompileStateSet.getBitFlag(state3), CompileStateSet.toBitSet(state1, state2, state3));
                    Assert.assertEquals(CompileStateSet.getBitFlag(state1) | CompileStateSet.getBitFlag(state2) | CompileStateSet.getBitFlag(state3), CompileStateSet.toBitSet(Lists.immutable.with(state1, state2, state3)));
                }
            }
        }

        Assert.assertEquals(0, CompileStateSet.toBitSet(Lists.immutable.empty()));
        Assert.assertEquals(1, CompileStateSet.toBitSet(CompileState.PROCESSED));
        Assert.assertEquals(2, CompileStateSet.toBitSet(CompileState.VALIDATED));
        Assert.assertEquals(3, CompileStateSet.toBitSet(CompileState.PROCESSED, CompileState.VALIDATED));
        Assert.assertEquals(3, CompileStateSet.toBitSet(CompileState.VALIDATED, CompileState.PROCESSED));
    }

    @Test
    public void testFromBitSet()
    {
        Assert.assertEquals(CompileStateSet.with(), CompileStateSet.fromBitSet(0));
        for (CompileState state1 : CompileState.values())
        {
            Assert.assertEquals(CompileStateSet.with(state1), CompileStateSet.fromBitSet(CompileStateSet.toBitSet(state1)));
            for (CompileState state2 : CompileState.values())
            {
                Assert.assertEquals(CompileStateSet.with(state1, state2), CompileStateSet.fromBitSet(CompileStateSet.toBitSet(state1, state2)));
                for (CompileState state3 : CompileState.values())
                {
                    Assert.assertEquals(CompileStateSet.with(state1, state2, state3), CompileStateSet.fromBitSet(CompileStateSet.toBitSet(state1, state2, state3)));
                }
            }
        }
    }

    @Test
    public void testAddRemoveHasCompileState()
    {
        for (CompileState state : CompileState.values())
        {
            int bitSet = 0;
            Assert.assertFalse(CompileStateSet.bitSetHasCompileState(bitSet, state));
            bitSet = CompileStateSet.addCompileStateToBitSet(bitSet, state);
            Assert.assertTrue(CompileStateSet.bitSetHasCompileState(bitSet, state));
            bitSet = CompileStateSet.removeCompileStateFromBitSet(bitSet, state);
            Assert.assertFalse(CompileStateSet.bitSetHasCompileState(bitSet, state));
        }
    }

    @Test
    public void testRemoveExtraCompileState()
    {
        Assert.assertEquals((short)0, CompileStateSet.removeExtraCompileStates((short)0));
        Assert.assertEquals(CompileStateSet.toBitSet(CompileState.PROCESSED), CompileStateSet.removeExtraCompileStates(CompileStateSet.toBitSet(CompileState.PROCESSED, CompileState.COMPILE_EVENT_EXTRA_STATE_1)));
        Assert.assertEquals(CompileStateSet.toBitSet(CompileState.VALIDATED), CompileStateSet.removeExtraCompileStates(CompileStateSet.toBitSet(CompileState.VALIDATED, CompileState.COMPILE_EVENT_EXTRA_STATE_3)));
        Assert.assertEquals(CompileStateSet.toBitSet(CompileState.PROCESSED, CompileState.VALIDATED), CompileStateSet.removeExtraCompileStates(CompileStateSet.toBitSet(CompileState.PROCESSED, CompileState.VALIDATED, CompileState.COMPILE_EVENT_EXTRA_STATE_2, CompileState.COMPILE_EVENT_EXTRA_STATE_4)));
    }

    @Test
    public void testNormalizeCompileStateBitSet()
    {
        Assert.assertEquals(0, CompileStateSet.normalizeCompileStateBitSet(0));
        Assert.assertEquals(CompileStateSet.toBitSet(CompileState.values()), CompileStateSet.normalizeCompileStateBitSet(Integer.MAX_VALUE));
    }

    @Test
    public void testSize()
    {
        Assert.assertEquals(0, CompileStateSet.with().size());
        Assert.assertEquals(1, CompileStateSet.with(CompileState.PROCESSED).size());
        Assert.assertEquals(2, CompileStateSet.with(CompileState.PROCESSED, CompileState.VALIDATED).size());
        Assert.assertEquals(CompileState.values().length, CompileStateSet.with(CompileState.values()).size());
    }

    @Test
    public void testGetFirstLast()
    {
        Assert.assertNull(CompileStateSet.with().getFirst());
        Assert.assertNull(CompileStateSet.with().getLast());
        CompileState[] allStates = CompileState.values();
        for (int i = 0; i < allStates.length; i++)
        {
            CompileState state1 = allStates[i];
            Assert.assertSame(state1, CompileStateSet.with(state1).getFirst());
            Assert.assertSame(state1, CompileStateSet.with(state1).getLast());
            for (int j = i + 1; j < allStates.length; j++)
            {
                CompileState state2 = allStates[j];
                Assert.assertSame(state1, CompileStateSet.with(state1, state2).getFirst());
                Assert.assertSame(state2, CompileStateSet.with(state1, state2).getLast());
                for (int k = j + 1; k < allStates.length; k++)
                {
                    CompileState state3 = allStates[k];
                    Assert.assertSame(state1, CompileStateSet.with(state1, state2, state3).getFirst());
                    Assert.assertSame(state3, CompileStateSet.with(state1, state2, state3).getLast());
                }
            }
        }
    }

    @Test
    public void testIteration()
    {
        Assert.assertArrayEquals(CompileState.values(), CompileStateSet.with(CompileState.values()).toArray());
        Assert.assertEquals(ArrayAdapter.adapt(CompileState.values()), Lists.mutable.withAll(CompileStateSet.with(CompileState.values())));
    }

    @Test
    public void testStaticProcessedValidatedSet()
    {
        Assert.assertSame(CompileStateSet.PROCESSED_VALIDATED, CompileStateSet.with(CompileState.PROCESSED, CompileState.VALIDATED));
        Assert.assertSame(CompileStateSet.PROCESSED_VALIDATED, CompileStateSet.with(CompileState.VALIDATED, CompileState.PROCESSED));
    }
}
