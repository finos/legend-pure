// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.m3.compiler.postprocessing.observer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.Arrays;
import java.util.function.Consumer;

public class CombinedPostProcessorObserver implements PostProcessorObserver
{
    private final ImmutableList<PostProcessorObserver> observers;

    private CombinedPostProcessorObserver(ImmutableList<PostProcessorObserver> observers)
    {
        this.observers = observers;
    }

    @Override
    public void startProcessing(CoreInstance instance)
    {
        forEachObserver(o -> o.startProcessing(instance));
    }

    @Override
    public void finishProcessing(CoreInstance instance)
    {
        forEachObserver(o -> o.finishProcessing(instance));
    }

    @Override
    public void finishProcessing(CoreInstance instance, Exception e)
    {
        forEachObserver(o -> o.finishProcessing(instance, e));
    }

    private void forEachObserver(Consumer<? super PostProcessorObserver> consumer)
    {
        RuntimeException exception = null;
        for (PostProcessorObserver observer : this.observers)
        {
            try
            {
                consumer.accept(observer);
            }
            catch (RuntimeException e)
            {
                if (exception == null)
                {
                    exception = e;
                }
                else
                {
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null)
        {
            throw exception;
        }
    }

    public static PostProcessorObserver combine(PostProcessorObserver... observers)
    {
        switch (observers.length)
        {
            case 0:
            {
                return new VoidPostProcessorObserver();
            }
            case 1:
            {
                return (observers[0] == null) ? new VoidPostProcessorObserver() : observers[0];
            }
            default:
            {
                return combine(Arrays.asList(observers));
            }
        }
    }

    public static PostProcessorObserver combine(Iterable<? extends PostProcessorObserver> observers)
    {
        MutableList<PostProcessorObserver> list = Lists.mutable.empty();
        observers.forEach(o ->
        {
            if (o instanceof CombinedPostProcessorObserver)
            {
                list.addAllIterable(((CombinedPostProcessorObserver) o).observers);
            }
            else if ((o != null) && !(o instanceof VoidPostProcessorObserver))
            {
                list.add(o);
            }
        });
        switch (list.size())
        {
            case 0:
            {
                return new VoidPostProcessorObserver();
            }
            case 1:
            {
                return list.get(0);
            }
            default:
            {
                return new CombinedPostProcessorObserver(list.toImmutable());
            }
        }
    }
}
