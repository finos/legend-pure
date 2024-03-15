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

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

class NetTimingPostProcessorObserver extends TimingPostProcessorObserver
{
    private final MutableStack<InstanceWithStartTime> stack = Stacks.mutable.empty();

    NetTimingPostProcessorObserver()
    {
    }

    @Override
    protected void noteProcessingStart(CoreInstance instance, long startNanoTime)
    {
        if (this.stack.notEmpty())
        {
            InstanceWithStartTime previous = this.stack.peek();
            recordDuration(previous.instance, startNanoTime - previous.startNanoTime);
        }
        this.stack.push(new InstanceWithStartTime(instance, startNanoTime));
    }

    @Override
    protected long noteProcessingEnd(CoreInstance instance, long endNanoTime)
    {
        if (this.stack.isEmpty())
        {
            return 0L;
        }

        InstanceWithStartTime current = this.stack.pop();
        if (current.instance != instance)
        {
            // Something has gone wrong: try to recover
            if (this.stack.noneSatisfy(iwst -> iwst.instance == instance))
            {
                // Can't find this instance in the stack: set things back the way they were and return 0 duration
                this.stack.push(current);
                return 0L;
            }

            // Pop the stack until we find the current instance
            while (current.instance != instance)
            {
                current = this.stack.pop();
            }
        }

        if (this.stack.notEmpty())
        {
            this.stack.peek().startNanoTime = endNanoTime;
        }
        return endNanoTime - current.startNanoTime;
    }

    private static class InstanceWithStartTime
    {
        private final CoreInstance instance;
        private long startNanoTime;

        private InstanceWithStartTime(CoreInstance instance, long startNanoTime)
        {
            this.instance = instance;
            this.startNanoTime = startNanoTime;
        }
    }
}
