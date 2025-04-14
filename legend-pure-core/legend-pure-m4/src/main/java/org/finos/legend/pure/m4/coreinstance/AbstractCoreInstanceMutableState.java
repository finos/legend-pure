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

package org.finos.legend.pure.m4.coreinstance;

import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public abstract class AbstractCoreInstanceMutableState
{
    private static final AtomicIntegerFieldUpdater<AbstractCoreInstanceMutableState> UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractCoreInstanceMutableState.class, "compileStateBitSet");

    private volatile int compileStateBitSet = 0;

    public boolean hasCompileState(CompileState state)
    {
        return CompileStateSet.bitSetHasCompileState(this.compileStateBitSet, state);
    }

    public void addCompileState(CompileState state)
    {
        addCompileStateSet(CompileStateSet.with(state));
    }

    public void addCompileStates(Iterable<? extends CompileState> states)
    {
        addCompileStateSet(CompileStateSet.withAll(states));
    }

    private void addCompileStateSet(CompileStateSet toAdd)
    {
        int currentState;
        do
        {
            currentState = this.compileStateBitSet;
        }
        while (!UPDATER.compareAndSet(this, currentState, CompileStateSet.addCompileStatesToBitSet(currentState, toAdd)));
    }

    public void removeCompileState(CompileState state)
    {
        removeCompileStateSet(CompileStateSet.with(state));
    }

    public void removeCompileStates(Iterable<? extends CompileState> states)
    {
        removeCompileStateSet(CompileStateSet.withAll(states));
    }

    private void removeCompileStateSet(CompileStateSet toRemove)
    {
        int currentState;
        do
        {
            currentState = this.compileStateBitSet;
        }
        while (!UPDATER.compareAndSet(this, currentState, CompileStateSet.removeCompileStatesFromBitSet(currentState, toRemove)));
    }

    public CompileStateSet getCompileStates()
    {
        return CompileStateSet.fromBitSet(this.compileStateBitSet);
    }

    public void setCompileStatesFrom(CompileStateSet states)
    {
        this.compileStateBitSet = states.toBitSet();
    }

    protected int getCompileStateBitSet()
    {
        return this.compileStateBitSet;
    }

    protected void setCompileStateBitSet(int compileStateBitSet)
    {
        this.compileStateBitSet = compileStateBitSet;
    }
}
