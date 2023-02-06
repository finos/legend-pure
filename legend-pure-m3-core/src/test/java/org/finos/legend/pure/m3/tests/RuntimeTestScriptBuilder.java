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

package org.finos.legend.pure.m3.tests;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;

public final class RuntimeTestScriptBuilder extends RuntimeActionRunner
{
    public RuntimeTestScriptBuilder deleteSource(String sourceId)
    {
        this.addAction(new DeleteSourceAction(Lists.immutable.of(sourceId)));
        return this;
    }

    public RuntimeTestScriptBuilder deleteSources(ListIterable<String> sourceIds)
    {
        this.addAction(new DeleteSourceAction(sourceIds));
        return this;
    }

    public RuntimeTestScriptBuilder revertSource(String sourceId)
    {
        this.addAction(new RevertSourceAction(sourceId));
        return this;
    }

    public RuntimeTestScriptBuilder createInMemorySources(MapIterable<String,String> sources)
    {
        this.addAction(new CreateInMemorySource(sources));
        return this;
    }

    public RuntimeTestScriptBuilder createInMemorySource(String sourceId, String source)
    {
        this.addAction(new CreateInMemorySource(Maps.immutable.of(sourceId, source)));
        return this;
    }

    public RuntimeTestScriptBuilder updateSource(String sourceId, String source)
    {
        this.addAction(new UpdateSource(Maps.immutable.of(sourceId, source)));
        return this;
    }

    public RuntimeTestScriptBuilder compile()
    {
        this.addAction(new Compile());
        return this;
    }

    public RuntimeTestScriptBuilder compileWithExpectedCompileFailure(String message, String sourceId, int lineNumber, int columnNumber)
    {
        this.addAction(new ExpectedCompilationException(new Compile(), message, sourceId, lineNumber, columnNumber));
        return this;
    }

    public RuntimeTestScriptBuilder compileWithExpectedParserFailure(String message, String sourceId, int lineNumber, int columnNumber)
    {
        this.addAction(new ExpectedParserException(new Compile(), message, sourceId, lineNumber, columnNumber));
        return this;
    }

    public RuntimeTestScriptBuilder compileWithExpectedCompileFailureAndAssertions(String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
    {
        this.addAction(new ExpectedCompilationExceptionAndAssertions(new Compile(), message, sourceId, lineNumber, columnNumber, processed, notProcessed, removed));
        return this;
    }

    public RuntimeTestScriptBuilder compileWithExpectedParserFailureAndAssertions(String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
    {
        this.addAction(new ExpectedParserExceptionAndAssertions(new Compile(), message, sourceId, lineNumber, columnNumber, processed, notProcessed, removed));
        return this;
    }

    public RuntimeTestScriptBuilder compileIgnoreExceptions()
    {
        this.addAction(new IgnoreAnyException(new Compile()));
        return this;
    }

    public RuntimeTestScriptBuilder executeFunction(String functionId)
    {
        this.addAction(new ExecuteFunction(functionId));
        return this;
    }

    public RuntimeTestScriptBuilder executeFunctionWithExpectedExecutionFailure(String functionId, String message, String sourceId, int lineNumber, int columnNumber)
    {
        this.addAction(new ExpectedExecutionException(new ExecuteFunction(functionId), message, sourceId, lineNumber, columnNumber));
        return this;
    }

    public RuntimeTestScriptBuilder executeFunctionWithExpectedExecutionFailureandAssertions(String functionId, String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
    {
        this.addAction(new ExpectedExecutionExceptionAndAssertions(new ExecuteFunction(functionId), message, sourceId, lineNumber, columnNumber,  processed, notProcessed, removed));
        return this;
    }


    public RuntimeTestScriptBuilder executeFunctionWithExpectedExecutionFailure(String functionId)
    {
        this.addAction(new ExpectedExecutionException(new ExecuteFunction(functionId), null, null, 0, 0));
        return this;
    }

    public RuntimeTestScriptBuilder f9()
    {
        return this;
    }


    private class DeleteSourceAction implements RuntimeAction
    {
        private final ListIterable<String> sourceIds;

        private DeleteSourceAction(ListIterable<String> sourceIds)
        {
            this.sourceIds = sourceIds;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            for (String sourceId : this.sourceIds)
            {
                pureRuntime.delete(sourceId);
            }
        }
    }

    private class CreateInMemorySource implements RuntimeAction
    {
        private final MapIterable<String,String> sources;

        private CreateInMemorySource(MapIterable<String,String> sources)
        {
            this.sources = sources;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            for (String sourceId : this.sources.keysView())
            {
                pureRuntime.createInMemorySource(sourceId, this.sources.get(sourceId));
            }
        }
    }

    private class UpdateSource implements RuntimeAction
    {
        private final MapIterable<String,String> sources;

        private UpdateSource(MapIterable<String,String> sources)
        {
            this.sources = sources;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            for (String sourceId : this.sources.keysView())
            {
                pureRuntime.modify(sourceId, this.sources.get(sourceId));
            }
        }
    }

    private class ExecuteFunction implements RuntimeAction
    {
        private final String functionId;

        private ExecuteFunction(String functionId)
        {
            this.functionId = functionId;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            pureRuntime.compile();
            CoreInstance function = pureRuntime.getFunction(this.functionId);
            if (function == null)
            {
                throw new RuntimeException("The function '" + this.functionId + "' can't be found");
            }
            functionExecution.getConsole().clear();
            functionExecution.start(function, Lists.immutable.<CoreInstance>of());
        }
    }

    private class Compile implements RuntimeAction
    {

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            pureRuntime.compile();
        }
    }


    private class RevertSourceAction implements RuntimeAction
    {
        private final String sourceId;

        private RevertSourceAction(String sourceId)
        {
            this.sourceId = sourceId;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            pureRuntime.revert(this.sourceId);
        }
    }


    private class ExpectedCompilationException extends ExpectedException
    {
        private ExpectedCompilationException(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureCompilationException.class);
        }
    }

    private class ExpectedParserException extends ExpectedException
    {
        private ExpectedParserException(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureParserException.class);
        }
    }
    private class ExpectedExecutionException extends ExpectedException
    {
        private ExpectedExecutionException(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureExecutionException.class);
        }
    }

    private class ExpectedCompilationExceptionAndAssertions extends ExpectedExceptionAndAssertions
    {
        private ExpectedCompilationExceptionAndAssertions(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureCompilationException.class, processed, notProcessed, removed);
        }
    }

    private class ExpectedParserExceptionAndAssertions extends ExpectedExceptionAndAssertions
    {
        private ExpectedParserExceptionAndAssertions(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureParserException.class, processed, notProcessed, removed);
        }
    }

    private class ExpectedExecutionExceptionAndAssertions extends ExpectedExceptionAndAssertions
    {
        private ExpectedExecutionExceptionAndAssertions(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
        {
            super(action, message, sourceId, lineNumber, columnNumber, PureExecutionException.class, processed, notProcessed, removed);
        }
    }

    private class ExpectedException implements RuntimeAction
    {
        private final RuntimeAction action;

        private final String message;
        private final String sourceId;
        private final int lineNumber;
        private final int columnNumber;
        private final Class exceptionClass;

        private ExpectedException(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber, Class exceptionClass)
        {
            this.action = action;
            this.message = message;
            this.sourceId = sourceId;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.exceptionClass = exceptionClass;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            try
            {
                this.action.execute(pureRuntime, functionExecution);
                Assert.fail();
            }
            catch (Exception e)
            {
                if (this.message == null)
                {
                    Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Compilation error at (resource:"));
                }
                else
                {
                    AbstractPureTestWithCoreCompiledPlatform.assertPureException(this.exceptionClass, this.message,
                            this.sourceId, this.lineNumber, this.columnNumber, e);
                }
            }
        }
    }

    private class ExpectedExceptionAndAssertions implements RuntimeAction
    {
        private final RuntimeAction action;

        private final String message;
        private final String sourceId;
        private final int lineNumber;
        private final int columnNumber;
        private final Class exceptionClass;

        private final ListIterable<String> processed;
        private final ListIterable<String> notProcessed;
        private final ListIterable<String> removed;

        private ExpectedExceptionAndAssertions(RuntimeAction action, String message, String sourceId, int lineNumber, int columnNumber, Class exceptionClass, ListIterable<String> processed, ListIterable<String> notProcessed, ListIterable<String> removed)
        {
            this.action = action;
            this.message = message;
            this.sourceId = sourceId;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
            this.exceptionClass = exceptionClass;
            this.processed = processed;
            this.notProcessed = notProcessed;
            this.removed = removed;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            try
            {
                this.action.execute(pureRuntime, functionExecution);
                Assert.fail();
            }
            catch (Exception e)
            {
                if (this.message == null)
                {
                    Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("Compilation error at (resource:"));
                }
                else
                {
                    AbstractPureTestWithCoreCompiledPlatform.assertPureException(this.exceptionClass, this.message,
                            this.sourceId, this.lineNumber, this.columnNumber, e);
                }
                for(String path : this.processed)
                {
                    CoreInstance instance = pureRuntime.getCoreInstance(path);
                    Assert.assertTrue(instance.hasBeenProcessed());
                }

                for(String path : this.notProcessed)
                {
                    CoreInstance instance = pureRuntime.getCoreInstance(path);
                    Assert.assertFalse(instance.hasBeenProcessed());
                }

                for(String path : this.removed)
                {
                    CoreInstance instance = pureRuntime.getCoreInstance(path);
                    Assert.assertNull(instance);
                }
            }
        }
    }

    private class IgnoreAnyException implements RuntimeAction
    {
        private final RuntimeAction action;

        private IgnoreAnyException(RuntimeAction action)
        {
            this.action = action;
        }

        @Override
        public void execute(PureRuntime pureRuntime, FunctionExecution functionExecution)
        {
            try
            {
                this.action.execute(pureRuntime, functionExecution);
            }
            catch (Exception e)
            {
                //Ignore
            }
        }
    }
}
