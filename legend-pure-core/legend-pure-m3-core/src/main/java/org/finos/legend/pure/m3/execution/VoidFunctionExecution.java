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

/**
 * Function execution which does nothing.
 */
public class VoidFunctionExecution implements FunctionExecution
{
    public static final VoidFunctionExecution VOID_FUNCTION_EXECUTION = new VoidFunctionExecution();

    private VoidFunctionExecution()
    {
        // Singleton
    }

    @Override
    public void init(PureRuntime runtime, Message message)
    {
    }

    @Override
    public CoreInstance start(CoreInstance func, ListIterable<? extends CoreInstance> arguments)
    {
        return null;
    }

    @Override
    public void start(CoreInstance func, ListIterable<? extends CoreInstance> arguments, OutputStream outputStream, OutputWriter writer)
    {

    }

    @Override
    public Console getConsole()
    {
        return null;
    }

    @Override
    public boolean isFullyInitializedForExecution()
    {
        return false;
    }

    @Override
    public void resetEventHandlers()
    {
    }

    @Override
    public ProcessorSupport getProcessorSupport()
    {
        return null;
    }

    @Override
    public PureRuntime getRuntime()
    {
        return null;
    }

    @Override
    public OutputWriter newOutputWriter()
    {
        return null;
    }
}
