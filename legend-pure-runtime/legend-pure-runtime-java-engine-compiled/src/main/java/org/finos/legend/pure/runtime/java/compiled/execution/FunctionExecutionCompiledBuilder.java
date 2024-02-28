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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.finos.legend.pure.m3.statelistener.ExecutionActivityListener;
import org.finos.legend.pure.m3.statelistener.VoidExecutionActivityListener;
import org.finos.legend.pure.runtime.java.compiled.statelistener.JavaCompilerEventObserver;
import org.finos.legend.pure.runtime.java.compiled.statelistener.VoidJavaCompilerEventObserver;

/**
 * Configure the compiled execution environment
 */
public class FunctionExecutionCompiledBuilder
{
    private boolean includePureStackTrace = false;

    private ExecutionActivityListener executionActivityListener = VoidExecutionActivityListener.VOID_EXECUTION_ACTIVITY_LISTENER;
    private JavaCompilerEventObserver javaCompilerEventObserver = VoidJavaCompilerEventObserver.VOID_JAVA_COMPILER_EVENT_OBSERVER;

    public FunctionExecutionCompiledBuilder shouldIncludePureStackTrace()
    {
        this.includePureStackTrace = true;
        return this;
    }

    public FunctionExecutionCompiledBuilder shouldIncludePureStackTrace(boolean value)
    {
        this.includePureStackTrace = value;
        return this;
    }

    public FunctionExecutionCompiledBuilder withExecutionListener(ExecutionActivityListener executionListener)
    {
        this.executionActivityListener = executionListener;
        return this;
    }

    public FunctionExecutionCompiledBuilder withJavaCompilerEventObserver(JavaCompilerEventObserver observer)
    {
        this.javaCompilerEventObserver = observer;
        return this;
    }

    public FunctionExecutionCompiled build()
    {
        return FunctionExecutionCompiled.createFunctionExecutionCompiled(this.executionActivityListener, this.includePureStackTrace, this.javaCompilerEventObserver);
    }
}
