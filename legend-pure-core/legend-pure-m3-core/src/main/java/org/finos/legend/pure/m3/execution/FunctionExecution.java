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

package org.finos.legend.pure.m3.execution;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.OutputStream;

public interface FunctionExecution
{
    /**
     * Initialize the function execution with the given
     * Pure runtime.
     *
     * @param runtime Pure runtime
     */
    void init(PureRuntime runtime, Message message);

    /**
     * Start the execution of the given function applied
     * to the given arguments and return the result.
     *
     * @param func function to execute
     * @param arguments function arguments
     * @return function execution result
     */
    CoreInstance start(CoreInstance func, ListIterable<? extends CoreInstance> arguments);

    void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream outputStream, OutputWriter writer);

    /**
     * Get the function execution console.
     *
     * @return function execution console
     */
    Console getConsole();

    boolean isFullyInitializedForExecution();

    void resetEventHandlers();

    ProcessorSupport getProcessorSupport();

    PureRuntime getRuntime();

    OutputWriter newOutputWriter();
}
