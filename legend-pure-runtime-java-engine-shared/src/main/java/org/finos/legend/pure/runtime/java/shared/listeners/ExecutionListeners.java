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

package org.finos.legend.pure.runtime.java.shared.listeners;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.exception.PureExecutionException;

import java.util.Objects;

public class ExecutionListeners
{
    private MutableList<ExecutionEndListener> executionEndListeners = Lists.mutable.of();
    private MutableList<IdentifableExecutionEndListner> identifableExecutionEndListeners = Lists.mutable.of();

    public void registerExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.executionEndListeners.add(executionEndListener);
    }

    public void unregisterExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.executionEndListeners.remove(executionEndListener);
    }

    public void registerIdentifableExecutionEndListener(IdentifableExecutionEndListner executionEndListener)
    {
        validateNewExecutionEndListener(executionEndListener);
        this.identifableExecutionEndListeners.add(executionEndListener);
    }

    private void validateNewExecutionEndListener(IdentifableExecutionEndListner executionEndListener)
    {
        if (this.identifableExecutionEndListeners.anySatisfy(l -> Objects.equals(l.getId(), executionEndListener.getId())))
        {
            throw new PureExecutionException("IdentifableExecutionEndListner with Id: " + executionEndListener.getId() + " is already registered");
        }
    }

    public void unRegisterIdentifableExecutionEndListener(String eventId)
    {
        this.identifableExecutionEndListeners.removeIf(l -> Objects.equals(eventId, l.getId()));
    }

    private ListIterable<ExecutionEndListener> allEndListeners()
    {
        return Lists.mutable.withAll(this.executionEndListeners).withAll(this.identifableExecutionEndListeners);
    }

    private void clearAllEndListners()
    {
        this.executionEndListeners = Lists.mutable.empty();
        this.identifableExecutionEndListeners = Lists.mutable.empty();
    }

    public void executionEnd(Exception exception)
    {
        RichIterable<ExecutionEndListenerState> executionEndStates = this.allEndListeners().collect(executionEndListener ->
        {
            try
            {
                return executionEndListener.executionEnd(exception);
            }
            catch (Exception ex)
            {
                //Ignore, need to make sure we get through all possible listeners
//                    ex.printStackTrace();
                return new ExecutionEndListenerState(true, ex.getMessage());
            }
        });

        RichIterable<String> exceptionalState = executionEndStates.select(ExecutionEndListenerState.UNEXPECTED_END_STATE).collect(ExecutionEndListenerState.TO_END_LISTENER_STATE_MESSAGE).toList();
        if (!exceptionalState.isEmpty())
        {
            throw new ExecutionEndListenerStateException("Error: " + exceptionalState.makeString());
        }

        clearAllEndListners();
    }
}
