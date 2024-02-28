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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.natives;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;

import java.util.Iterator;

public class ResultLazyIterable extends AbstractLazyIterable
{
    private final AbstractCacheNextReadOnceForwardOnly result;

    public ResultLazyIterable(AbstractCacheNextReadOnceForwardOnly result)
    {
        this.result = result;
    }

    @Override
    public void each(Procedure procedure)
    {
        for (Object o : this)
        {
            procedure.value(o);
        }
    }

    @Override
    public Iterator iterator()
    {
        return this.result.newIterator();
    }

    //Override otherwise the result is read by the debugger, which causes repeat read issues
    @Override
    public String toString()
    {
        return "ResultLazyIterable";
    }
}
