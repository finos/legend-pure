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
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.tools.SafeAppendable;

/**
 * An exception raised when something goes wrong during Pure execution.
 */
public class PureExecutionException extends PureException
{
    private final MutableStack<CoreInstance> callStack;

    public PureExecutionException(SourceInformation sourceInformation, String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        this(sourceInformation, info, cause, null);
    }

    public PureExecutionException(SourceInformation sourceInformation, String info, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, info, null);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, String info)
    {
        this(sourceInformation, info, (MutableStack<CoreInstance>) null);
    }

    public PureExecutionException(SourceInformation sourceInformation, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation, null, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation, Throwable cause)
    {
        this(sourceInformation, cause, null);
    }

    public PureExecutionException(String info, Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(info, cause);
        this.callStack = callStack;
    }

    public PureExecutionException(String info, Throwable cause)
    {
        this(info, cause, null);
    }

    public PureExecutionException(SourceInformation sourceInformation, MutableStack<CoreInstance> callStack)
    {
        super(sourceInformation);
        this.callStack = callStack;
    }

    public PureExecutionException(SourceInformation sourceInformation)
    {
        this(sourceInformation, (MutableStack<CoreInstance>) null);
    }

    public PureExecutionException(String info, MutableStack<CoreInstance> callStack)
    {
        super(info);
        this.callStack = callStack;
    }

    public PureExecutionException(String info)
    {
        this(info, (MutableStack<CoreInstance>) null);
    }

    public PureExecutionException(Throwable cause, MutableStack<CoreInstance> callStack)
    {
        super(cause);
        this.callStack = callStack;
    }

    public PureExecutionException(Throwable cause)
    {
        this(cause, null);
    }

    public PureExecutionException()
    {
        super();
        this.callStack = null;
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
        if ((this.callStack != null) && this.callStack.notEmpty())
        {
            SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
            safeAppendable.append('\n').append(indent).append("Full Stack:");
            this.callStack.toList().reverseForEach(x ->
            {
                safeAppendable.append('\n').append(indent).append("    ");
                try
                {
                    CoreInstance func = x.getValueForMetaPropertyToOne(M3Properties.func);
                    if (func == null)
                    {
                        safeAppendable.append("NULL / TODO");
                    }
                    else if (processorSupport.instance_instanceOf(func, M3Paths.ConcreteFunctionDefinition) || processorSupport.instance_instanceOf(func, M3Paths.NativeFunction))
                    {
                        FunctionDescriptor.writeFunctionDescriptor(safeAppendable, func, false, processorSupport);
                    }
                    else if (processorSupport.instance_instanceOf(func, M3Paths.LambdaFunction))
                    {
                        FunctionType.print(safeAppendable.append("Lambda "), Function.computeFunctionType(func, processorSupport), processorSupport);
                    }
                    else if (Property.isProperty(func, processorSupport))
                    {
                        safeAppendable.append("Property ").append(Property.getPropertyName(func));
                        FunctionType.print(safeAppendable, Function.computeFunctionType(func, processorSupport), processorSupport);
                    }
                    else if (processorSupport.instance_instanceOf(func, M3Paths.Column))
                    {
                        safeAppendable.append("Column ").append(func.getValueForMetaPropertyToOne(M3Properties.name).getName());
                        FunctionType.print(safeAppendable, Function.computeFunctionType(func, processorSupport), processorSupport);
                    }
                    else
                    {
                        safeAppendable.append("Unknown Function Type '").append(func.getClassifier().getName()).append("'");
                    }
                }
                catch (Exception ignore)
                {
                    safeAppendable.append("Error Printing Function");
                }
                writeSourceInformationMessage(safeAppendable.append("     <-     "), x.getSourceInformation(), false);
            });
        }
        return appendable;
    }
}
