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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Iterator;

public abstract class AbstractCacheNextReadOnceForwardOnly
{
    protected CoreInstance next;
    protected final MutableList<CoreInstance> cachedResults = Lists.mutable.of();
    protected long currentIndex;

    public boolean hasNext(int index)
    {
        if (index == this.currentIndex + 1)
        {
            this.readNext();
            this.currentIndex++;
        }

        if (index == this.currentIndex)
        {
            return this.next != null;
        }
        else if (index < this.currentIndex && index < this.cachedResults.size())
        {
            //Already cached
            return true;
        }
        else
        {
            throw new PureExecutionException(this.streamingExceptionMessage());
        }

    }

    public CoreInstance next(int index)
    {
        if (index == this.currentIndex)
        {
            return this.next;
        }
        else if (index < this.currentIndex && index < this.cachedResults.size())
        {
            return this.cachedResults.get(index);
        }
        else
        {
            throw new PureExecutionException(this.streamingExceptionMessage());
        }
    }

    protected abstract void readNext();
    protected abstract String streamingExceptionMessage();

    public class ResultIterator implements Iterator<CoreInstance>
    {
        private int index = 0;

        @Override
        public boolean hasNext()
        {
            return AbstractCacheNextReadOnceForwardOnly.this.hasNext(this.index);
        }

        @Override
        public CoreInstance next()
        {
            CoreInstance result = AbstractCacheNextReadOnceForwardOnly.this.next(this.index);
            this.index++;
            return result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Remove is not supported");
        }
    }

    public Iterator<CoreInstance> newIterator()
    {
        return new ResultIterator();
    }
}


