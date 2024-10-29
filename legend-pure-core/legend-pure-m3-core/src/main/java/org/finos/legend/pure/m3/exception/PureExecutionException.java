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

package org.finos.legend.pure.m3.exception;

import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * An exception raised when something goes wrong during Pure execution.
 */
public class PureExecutionException extends PureException
{
    private MutableStack<CoreInstance> callStack;

    public PureExecutionException(SourceInformation sourceInformation, String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, String info, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, null);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, null, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(info, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation);
        this.callStack = callStack;
    }

    public PureExecutionException(String info, MutableStack<CoreInstance> callStack)
    {
        super(info);
        this.callStack = callStack;
    }

    public PureExecutionException(Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(cause);
        this.callStack = callStack;
    }

    public PureExecutionException()
    {
        super();
    }

    @Override
    public String getExceptionName()
    {
        return "Execution error";
    }

    public MutableStack<CoreInstance> getCallStack()
    {
        return this.callStack;
    }

    public <T extends Appendable> T printPureStackTrace(T appendable, String indent, ProcessorSupport processorSupport)
    {
        super.printPureStackTrace(appendable, indent);
        try
        {
            appendable.append("\n");
            appendable.append(indent);
            appendable.append("Full Stack:\n");
            getCallStack().toList().reverseThis().forEach((Consumer<? super CoreInstance>)
                    x ->
                    {
                        try
                        {
                            appendable.append(indent);
                            appendable.append("    ");
                            CoreInstance func = x.getValueForMetaPropertyToOne(M3Properties.func);
                            if (func != null)
                            {
                                FunctionDescriptor.writeFunctionDescriptor(appendable, func, false, processorSupport);
                            }
                            else
                            {
                                appendable.append("NULL / TODO");
                            }
                            appendable.append("     <-     ");
                            writeSourceInformationMessage(SafeAppendable.wrap(appendable), x.getSourceInformation(), false);
                            appendable.append("\n");
                        }
                        catch (IOException e)
                        {
                            throw new RuntimeException(e);
                        }
                    });
            return appendable;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
