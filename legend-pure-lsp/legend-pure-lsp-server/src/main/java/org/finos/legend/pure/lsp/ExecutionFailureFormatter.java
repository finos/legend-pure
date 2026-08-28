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

package org.finos.legend.pure.lsp;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.exception.PureException;

/**
 * Renders a full Pure stack trace for an execution failure - the same causal-chain/pure-stack-trace
 * logic used by the non-debug {@code legend/execute} path - so debug execution failures
 * (LegendDebugSession/DebugService) surface as much detail as a normal execution failure instead of
 * a bare exception message.
 */
public final class ExecutionFailureFormatter
{
    private ExecutionFailureFormatter()
    {
    }

    public static String format(Exception e, String capturedOutput, ProcessorSupport processorSupport)
    {
        StringBuilder builder = new StringBuilder();
        if (capturedOutput != null && !capturedOutput.isEmpty())
        {
            builder.append(capturedOutput);
            if (!capturedOutput.endsWith("\n"))
            {
                builder.append('\n');
            }
        }

        PureException pureException = PureException.findPureException(e);
        if (pureException != null)
        {
            PureException original = pureException.getOriginatingPureException();
            if (original == null)
            {
                original = pureException;
            }
            if (processorSupport != null && pureException instanceof PureExecutionException)
            {
                builder.append(original.getMessage()).append('\n');
                StringBuffer buffer = new StringBuffer();
                ((PureExecutionException) pureException).printPureStackTrace(buffer, "", processorSupport);
                builder.append(buffer);
            }
            else if (pureException.hasPureStackTrace())
            {
                builder.append(original.getMessage()).append('\n')
                        .append(pureException.getPureStackTrace("    "));
            }
            else
            {
                builder.append(original.getMessage());
            }
        }
        else
        {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            builder.append(writer);
        }

        return builder.toString();
    }

    public static String format(Exception e, ProcessorSupport processorSupport)
    {
        return format(e, null, processorSupport);
    }
}
