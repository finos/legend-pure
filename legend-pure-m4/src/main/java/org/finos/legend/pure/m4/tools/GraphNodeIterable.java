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

package org.finos.legend.pure.m4.tools;

import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.Stacks;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.utility.internal.IteratorIterate;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class GraphNodeIterable extends AbstractLazyIterable<CoreInstance>
{
    private final ImmutableList<CoreInstance> startingNodes;

    private GraphNodeIterable(Iterable<? extends CoreInstance> startingNodes)
    {
        this.startingNodes = Lists.immutable.withAll(startingNodes);
    }

    @Override
    public Iterator<CoreInstance> iterator()
    {
        return new GraphNodeIterator(this.startingNodes);
    }

    @Override
    public void each(Procedure<? super CoreInstance> procedure)
    {
        IteratorIterate.forEach(iterator(), procedure);
    }

    public static GraphNodeIterable fromNodes(Iterable<? extends CoreInstance> startingNodes)
    {
        if (startingNodes == null)
        {
            throw new IllegalArgumentException("Starting nodes may not be null");
        }
        return new GraphNodeIterable(startingNodes);
    }

    public static GraphNodeIterable fromModelRepository(ModelRepository repository)
    {
        return fromNodes(repository.getTopLevels());
    }

    public static MutableSet<CoreInstance> allInstancesFromRepository(ModelRepository repository)
    {
        GraphNodeIterator iterator = new GraphNodeIterator(repository.getTopLevels());
        while (iterator.hasNext())
        {
            iterator.next();
        }
        return iterator.visited;
    }

    private static class GraphNodeIterator implements Iterator<CoreInstance>
    {
        private final MutableStack<CoreInstance> stack;
        private final MutableSet<CoreInstance> visited;
        private CoreInstance next = null;

        private GraphNodeIterator(Iterable<? extends CoreInstance> startingNodes)
        {
            this.stack = Stacks.mutable.withAll(startingNodes);
            this.visited = Sets.mutable.empty();
            update();
        }

        @Override
        public boolean hasNext()
        {
            return this.next != null;
        }

        @Override
        public CoreInstance next()
        {
            CoreInstance node = this.next;
            if (node == null)
            {
                throw new NoSuchElementException();
            }
            update();
            return node;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void update()
        {
            CoreInstance node = getNextUnvisitedFromStack();
            if (node != null)
            {
                for (String key : node.getKeys())
                {
                    for (CoreInstance value : node.getValueForMetaPropertyToMany(key))
                    {
                        this.stack.push(value);
                    }
                }
            }
            this.next = node;
        }

        private CoreInstance getNextUnvisitedFromStack()
        {
            while (this.stack.notEmpty())
            {
                CoreInstance node = this.stack.pop();
                if (this.visited.add(node))
                {
                    return node;
                }
            }
            return null;
        }
    }
}
