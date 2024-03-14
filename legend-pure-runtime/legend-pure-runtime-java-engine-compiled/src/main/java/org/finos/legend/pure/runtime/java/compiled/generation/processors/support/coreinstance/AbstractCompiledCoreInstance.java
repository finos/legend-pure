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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;

public abstract class AbstractCompiledCoreInstance extends AbstractCoreInstance implements JavaCompiledCoreInstance
{
    private int compileStateBitSet = 0;

    @Override
    public CoreInstance getClassifier()
    {
        throw new UnsupportedOperationException("Modify the code to use ProcessorSupport.getClassifier instead (" + this.getClass().getName() + ")");
    }

    @Override
    public void addCompileState(CompileState state)
    {
        this.compileStateBitSet = CompileStateSet.addCompileStateToBitSet(this.compileStateBitSet, state);
    }

    @Override
    public void removeCompileState(CompileState state)
    {
        this.compileStateBitSet = CompileStateSet.removeCompileStateFromBitSet(this.compileStateBitSet, state);
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return CompileStateSet.bitSetHasCompileState(this.compileStateBitSet, state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return CompileStateSet.fromBitSet(this.compileStateBitSet);
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
        this.compileStateBitSet = states.toBitSet();
    }

    @Override
    public void removeProperty(String propertyNameKey)
    {
        throw new UnsupportedOperationException("This is not possible in compiled mode");
    }

    /**
     * Default implementation
     */
    @Override
    public boolean pureEquals(Object obj)
    {
        return super.equals(obj);
    }

    /**
     * Default implementation
     */
    @Override
    public int pureHashCode()
    {
        return super.hashCode();
    }

    /**
     * Default hash code and equals on the runtime compiled mode objects to use the pureHashCode and pureEquals
     */
    @Override
    public int hashCode()
    {
        return this.pureHashCode();
    }

    /**
     * Default hash code and equals on the runtime compiled mode objects to use the pureHashCode and pureEquals
     */
    @Override
    public boolean equals(Object obj)
    {
        return this.pureEquals(obj);
    }
}
