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

package org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.SafeAppendable;

public class PureCompiledExecutionException extends PureExecutionException
{
    private final MutableList<SourceInformation> stackTraceElements = Lists.mutable.empty();

    public PureCompiledExecutionException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause);
        addStackTraceElement(sourceInformation);
    }

    @SuppressWarnings("unused")
    public PureCompiledExecutionException(SourceInformation sourceInformation, String info)
    {
        super(sourceInformation, info, null);
        addStackTraceElement(sourceInformation);
    }

    public void addStackTraceElement(SourceInformation sourceInformation)
    {
        this.stackTraceElements.add(sourceInformation);
    }

    @Override
    public <T extends Appendable> T printPureStackTrace(T appendable, String indent)
    {
        writePureStackTrace(appendable, indent);
        return appendable;
    }

    private void writeSourceInformationMessage(SourceInformation sourceInformation, SafeAppendable appendable)
    {
        if (sourceInformation == null)
        {
            appendable.append("??");
        }
        else
        {
            printSourceInformationWithoutSourceId(appendable.append("resource:").append(sourceInformation.getSourceId()), sourceInformation);
        }
    }

    private void writePureStackTrace(Appendable appendable, String indent)
    {
        if (this.stackTraceElements.notEmpty())
        {
            SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
            this.stackTraceElements.forEachWithIndex((si, i) ->
            {
                if (indent != null)
                {
                    safeAppendable.append(indent).append(i + 1).append(": ");
                }
                writeSourceInformationMessage(si, safeAppendable);
                safeAppendable.append('\n');
            });
        }
    }
}
