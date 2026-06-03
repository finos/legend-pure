// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

class LegendDebugFunctionExecution extends FunctionExecutionInterpreted
{
    private final Set<String> debuggableSourceIds;
    private final LinkedHashSet<String> evaluationImports = new LinkedHashSet<>();
    private final ForkJoinPool executionPool;
    private final ThreadLocal<Boolean> pausesSuppressed = ThreadLocal.withInitial(() -> false);

    private volatile CompletableFuture<CoreInstance> currentExecution;
    private volatile CompletableFuture<CoreInstance> resultHandler;
    private volatile LegendDebugState debugState;

    LegendDebugFunctionExecution(Set<String> debuggableSourceIds)
    {
        this.debuggableSourceIds = debuggableSourceIds == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new TreeSet<>(debuggableSourceIds));
        this.executionPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                new LspDebugForkJoinWorkerThreadFactory(), null, false);
    }

    LegendDebugState getDebugState()
    {
        return this.debugState;
    }

    synchronized void addEvaluationImports(Collection<String> imports)
    {
        if (imports != null)
        {
            this.evaluationImports.addAll(imports);
        }
    }

    synchronized List<String> getEvaluationImports()
    {
        return new ArrayList<>(this.evaluationImports);
    }

    void startDebug(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        CompletableFuture<CoreInstance> handler = new CompletableFuture<>();
        this.resultHandler = handler;

        if (this.currentExecution == null)
        {
            CompletableFuture<CoreInstance> execution = CompletableFuture.supplyAsync(
                    () -> this.start(function, arguments),
                    this.executionPool);
            this.currentExecution = execution;
            execution.whenComplete((value, error) ->
            {
                CompletableFuture<CoreInstance> activeHandler = this.resultHandler;
                if (error == null)
                {
                    activeHandler.complete(value);
                }
                else
                {
                    activeHandler.completeExceptionally(error);
                }
                this.currentExecution = null;
            });
        }
        else
        {
            LegendDebugState state = this.debugState;
            if (state == null)
            {
                handler.completeExceptionally(new IllegalStateException("Debug execution is not paused"));
            }
            else
            {
                state.release();
            }
        }

        try
        {
            handler.join();
        }
        catch (CompletionException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    void abortDebug()
    {
        LegendDebugState state = this.debugState;
        if (state != null)
        {
            state.abort();
        }
        else if (this.currentExecution != null)
        {
            this.cancelExecution();
        }
    }

    <T> T withPausesSuppressed(Supplier<T> supplier)
    {
        Boolean previous = this.pausesSuppressed.get();
        this.pausesSuppressed.set(Boolean.TRUE);
        try
        {
            return supplier.get();
        }
        finally
        {
            this.pausesSuppressed.set(previous);
        }
    }

    void clearDebugState(LegendDebugState state)
    {
        if (this.debugState == state)
        {
            this.debugState = null;
        }
    }

    @Override
    public CoreInstance executeFunction(boolean limitScope,
            Function<?> function,
            ListIterable<? extends CoreInstance> params,
            Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
            Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters,
            VariableContext varContext,
            MutableStack<CoreInstance> functionExpressionCallStack,
            Profiler profiler,
            InstantiationContext instantiationContext,
            ExecutionSupport executionSupport)
    {
        pauseIfDebuggable(varContext, functionExpressionCallStack);
        return super.executeFunction(limitScope, function, params, resolvedTypeParameters, resolvedMultiplicityParameters,
                varContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }

    private void pauseIfDebuggable(VariableContext varContext, MutableStack<CoreInstance> functionExpressionCallStack)
    {
        if (this.pausesSuppressed.get() || functionExpressionCallStack == null || functionExpressionCallStack.isEmpty())
        {
            return;
        }

        SourceInformation sourceInformation = functionExpressionCallStack.peek().getSourceInformation();
        if (sourceInformation == null || !this.debuggableSourceIds.contains(sourceInformation.getSourceId()))
        {
            return;
        }

        LegendDebugState state = new LegendDebugState(this, varContext, functionExpressionCallStack);
        setDebugState(state);
        state.await();
        if (state.aborted())
        {
            throw new PureExecutionException("Aborting execution...", functionExpressionCallStack);
        }
    }

    private void setDebugState(LegendDebugState state)
    {
        if (state != null && this.debugState != null)
        {
            throw new IllegalStateException("Debug session already exists");
        }

        this.debugState = state;
        if (state != null)
        {
            CompletableFuture<CoreInstance> handler = this.resultHandler;
            if (handler != null)
            {
                handler.complete(null);
            }
        }
    }
}
