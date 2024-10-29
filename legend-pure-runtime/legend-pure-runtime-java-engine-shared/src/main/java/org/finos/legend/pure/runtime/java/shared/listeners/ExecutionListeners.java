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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.exception.PureExecutionException;

import java.util.Objects;
import java.util.function.Consumer;

public class ExecutionListeners
{
    private MutableList<ExecutionEndListener> executionEndListeners = Lists.mutable.of();
    private MutableList<IdentifiableExecutionEndListener> identifiableExecutionEndListeners = Lists.mutable.of();

    public void registerExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.executionEndListeners.add(executionEndListener);
    }

    public void unregisterExecutionEndListener(ExecutionEndListener executionEndListener)
    {
        this.executionEndListeners.remove(executionEndListener);
    }

    @Deprecated
    public void registerIdentifableExecutionEndListener(IdentifiableExecutionEndListener executionEndListener)
    {
        registerIdentifiableExecutionEndListener(executionEndListener);
    }

    public void registerIdentifiableExecutionEndListener(IdentifiableExecutionEndListener executionEndListener)
    {
        validateNewExecutionEndListener(executionEndListener);
        this.identifiableExecutionEndListeners.add(executionEndListener);
    }

    private void validateNewExecutionEndListener(IdentifiableExecutionEndListener executionEndListener)
    {
        if (this.identifiableExecutionEndListeners.anySatisfy(l -> Objects.equals(l.getId(), executionEndListener.getId())))
        {
            throw new PureExecutionException("IdentifiableExecutionEndListener with Id: " + executionEndListener.getId() + " is already registered", Stacks.mutable.empty());
        }
    }

    @Deprecated
    public void unRegisterIdentifableExecutionEndListener(String eventId)
    {
        unRegisterIdentifiableExecutionEndListener(eventId);
    }

    public void unRegisterIdentifiableExecutionEndListener(String eventId)
    {
        this.identifiableExecutionEndListeners.removeIf(l -> Objects.equals(eventId, l.getId()));
    }

    private void clearAllEndListeners()
    {
        this.executionEndListeners = Lists.mutable.empty();
        this.identifiableExecutionEndListeners = Lists.mutable.empty();
    }

    public void executionEnd(Exception exception)
    {
        MutableList<ExecutionEndListenerState> unexpected = Lists.mutable.empty();
        forEachEndListener(listener ->
        {
            ExecutionEndListenerState state = invokeEndListener(listener, exception);
            if ((state != null) && state.isUnexpectedEnd())
            {
                unexpected.add(state);
            }
        });
        clearAllEndListeners();
        if (unexpected.notEmpty())
        {
            MutableList<Throwable> throwables = Lists.mutable.empty();
            StringBuilder builder = new StringBuilder();
            unexpected.forEach(state ->
            {
                if (state.hasMessage())
                {
                    builder.append((builder.length() == 0) ? "Error: " : "\n\t").append(state.getMessage());
                }
                if (state.hasThrowable())
                {
                    throwables.add(state.getThrowable());
                }
            });
            ExecutionEndListenerStateException toThrow = new ExecutionEndListenerStateException(builder.toString(), Stacks.mutable.empty());
            throwables.forEach(toThrow::addSuppressed);
            throw toThrow;
        }
    }

    private void forEachEndListener(Consumer<? super ExecutionEndListener> consumer)
    {
        this.executionEndListeners.forEach(consumer);
        this.identifiableExecutionEndListeners.forEach(consumer);
    }

    private static ExecutionEndListenerState invokeEndListener(ExecutionEndListener listener, Exception exception)
    {
        try
        {
            return listener.executionEnd(exception);
        }
        catch (Exception e)
        {
            return new ExecutionEndListenerState(e);
        }
    }
}
