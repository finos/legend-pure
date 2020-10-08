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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;

import java.io.IOException;

public class PureCompiledExecutionException extends PureExecutionException
{
    private final MutableList<SourceInformation> stackTraceElements = Lists.mutable.empty();

    public PureCompiledExecutionException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(sourceInformation, info, cause);
        this.addStackTraceElement(sourceInformation);
    }

    @SuppressWarnings("unused")
    public PureCompiledExecutionException(SourceInformation sourceInformation, String info)
    {
        super(sourceInformation, info, null);
        this.addStackTraceElement(sourceInformation);
    }

    public void addStackTraceElement(SourceInformation sourceInformation)
    {
        this.stackTraceElements.add(sourceInformation);
    }

    @Override
    public void printPureStackTrace(Appendable appendable, String indent)

    {
        try
        {
            writePureStackTrace(appendable, indent);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void writeSourceInformationMessage(SourceInformation sourceInformation, Appendable appendable, boolean includeParens) throws IOException
    {
        if (sourceInformation == null)
        {
            appendable.append("??");
        }
        else
        {
            if (includeParens)
            {
                appendable.append('(');
            }


            appendable.append("resource:");
            appendable.append(sourceInformation.getSourceId());

            printSourceInformationWithoutSourceId(appendable, sourceInformation);

            if (includeParens)
            {
                appendable.append(')');
            }
        }
    }

    private void writePureStackTrace(Appendable appendable, String indent) throws IOException
    {
        int stackLevel = 1;
        PureException pureCause = getPureCause();
        if (pureCause == null)
        {
            stackLevel = 1;
        }
        for (SourceInformation si : this.stackTraceElements)
        {
            if (indent != null)
            {
                appendable.append(indent);
                appendable.append(Integer.toString(stackLevel++));
                appendable.append(": ");
            }
            writeSourceInformationMessage(si, appendable, false);
            appendable.append('\n');
        }
    }
}
