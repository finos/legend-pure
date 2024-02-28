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

package org.finos.legend.pure.m4.exception;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.io.IOException;

/**
 * Base class for all Pure exceptions.
 */
public abstract class PureException extends RuntimeException
{
    private final SourceInformation sourceInformation;
    private final String info;

    protected PureException(SourceInformation sourceInformation, String info, Throwable cause)
    {
        super(cause);
        this.sourceInformation = sourceInformation;
        this.info = info;
    }

    protected PureException(SourceInformation sourceInformation, String info)
    {
        this(sourceInformation, info, null);
    }

    protected PureException(SourceInformation sourceInformation, Throwable cause)
    {
        this(sourceInformation, null, cause);
    }

    protected PureException(String info, Throwable cause)
    {
        this(null, info, cause);
    }

    protected PureException(SourceInformation sourceInformation)
    {
        this(sourceInformation, null, null);
    }

    protected PureException(String info)
    {
        this(null, info, null);
    }

    protected PureException(Throwable cause)
    {
        this(null, null, cause);
    }

    protected PureException()
    {
        this(null, null, null);
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public String getInfo()
    {
        return this.info;
    }

    /**
     * Get the originating Pure exception.  That is, the Pure
     * exception which has no other Pure exception as a cause,
     * either direct or indirect.  That may be this exception
     * itself, but it will never be null.
     *
     * @return originating Pure exception
     */
    public PureException getOriginatingPureException()
    {
        PureException pureCause = getPureCause();
        return (pureCause == null) ? this : pureCause.getOriginatingPureException();
    }

    @Override
    public String getMessage()
    {
        return printMessage(new StringBuilder((this.info == null) ? 128 : (this.info.length() + 128))).toString();
    }

    public <T extends Appendable> T printMessage(T appendable)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        String exceptionName = getExceptionName();
        safeAppendable.append((exceptionName == null) ? "Error" : exceptionName);
        writeSourceInformationMessage(safeAppendable.append(" at "), true);
        if (hasAdditionalMessageInfo())
        {
            writeAdditionalMessageInfo(safeAppendable.append(", "));
        }
        return appendable;
    }

    /**
     * Get the message from the originating Pure exception.
     *
     * @return originating Pure exception message
     */
    public String getOriginatingPureMessage()
    {
        return getOriginatingPureException().getMessage();
    }

    @Override
    public String toString()
    {
        return getMessage();
    }

    /**
     * Return whether the exception has a Pure stack trace.
     * That is, it or at least one of its Pure causes has
     * some source information.
     *
     * @return whether there is a Pure stack trace
     */
    public boolean hasPureStackTrace()
    {
        if (this.sourceInformation != null)
        {
            return true;
        }
        PureException pureCause = getPureCause();
        return (pureCause != null) && pureCause.hasPureStackTrace();
    }

    /**
     * Get the Pure stack trace from this exception.
     *
     * @return stack trace string
     */
    public String getPureStackTrace()
    {
        return getPureStackTrace(null);
    }

    public SourceInformation[] getPureStackSourceInformation()
    {
        return getPureStackSourceInformation(1);
    }

    private SourceInformation[] getPureStackSourceInformation(int depth)
    {
        PureException pureCause = getPureCause();
        SourceInformation[] stack = (pureCause == null) ? new SourceInformation[depth] : pureCause.getPureStackSourceInformation(depth + 1);
        stack[stack.length - depth] = getSourceInformation();
        return stack;
    }

    /**
     * Get the Pure stack trace from this exception.  Each
     * line is indented with the given indent string (where
     * null indicates no indentation).
     *
     * @param indent indentation string for each line of the stack trace
     * @return stack trace string
     */
    public String getPureStackTrace(String indent)
    {
        return printPureStackTrace(new StringBuilder(256), indent).toString();
    }

    /**
     * Print the Pure stack trace from this exception to
     * System.out.
     */
    public void printPureStackTrace()
    {
        printPureStackTrace((String) null);
    }

    /**
     * Print the Pure stack trace from this exception to
     * System.out.  Each line is indented with the given
     * indent string (where null indicates no indentation).
     *
     * @param indent indentation string for each line of the stack trace
     */
    public void printPureStackTrace(String indent)
    {
        printPureStackTrace(System.out, indent);
    }

    /**
     * Print the Pure stack trace from this exception to the
     * given appendable.
     *
     * @param appendable appendable to print stack trace to
     * @return the appendable
     */
    public <T extends Appendable> T printPureStackTrace(T appendable)
    {
        return printPureStackTrace(appendable, null);
    }

    /**
     * Print the Pure stack trace from this exception to the
     * given appendable.  Each line is indented with the given
     * indent string (where null indicates no indentation).
     *
     * @param appendable appendable to print stack trace to
     * @param indent     indentation string for each line of the stack trace
     * @return the appendable
     */
    public <T extends Appendable> T printPureStackTrace(T appendable, String indent)
    {
        writePureStackTrace(SafeAppendable.wrap(appendable), indent);
        return appendable;
    }

    /**
     * Get the exception name.  This is used in generating
     * the exception message.
     *
     * @return exception name
     */
    public abstract String getExceptionName();

    /**
     * Return whether there is additional information to include
     * in the exception message.
     *
     * @return whether there is additional message info
     */
    protected boolean hasAdditionalMessageInfo()
    {
        return this.info != null;
    }

    /**
     * Write additional information to include in the exception
     * message.
     *
     * @param appendable exception message appendable
     */
    protected void writeAdditionalMessageInfo(SafeAppendable appendable)
    {
        appendable.append('"').append(this.info).append('"');
    }

    /**
     * Get the nearest cause of this exception which is
     * a Pure exception.
     *
     * @return nearest Pure exception cause
     */
    protected PureException getPureCause()
    {
        return findPureException(getCause());
    }

    private void writeSourceInformationMessage(SafeAppendable appendable, boolean includeParens)
    {
        if (this.sourceInformation == null)
        {
            appendable.append("??");
        }
        else
        {
            if (includeParens)
            {
                appendable.append('(');
            }
            appendable.append("resource:").append(this.sourceInformation.getSourceId());
            printSourceInformationWithoutSourceId(appendable, this.sourceInformation);
            if (includeParens)
            {
                appendable.append(')');
            }
        }
    }

    @Deprecated
    public static void printSourceInformationWithoutSourceId(Appendable appendable, SourceInformation sourceInformation) throws IOException
    {
        printSourceInformationWithoutSourceId(SafeAppendable.wrap(appendable), sourceInformation);
    }

    protected static void printSourceInformationWithoutSourceId(SafeAppendable appendable, SourceInformation sourceInformation)
    {
        int line = sourceInformation.getLine();
        int endLine = sourceInformation.getEndLine();
        int column = sourceInformation.getColumn();
        int endColumn = sourceInformation.getEndColumn();
        if (line != -1)
        {
            if (line == endLine || endLine == -1)
            {
                appendable.append(" line:").append(line);
                if (column != -1)
                {
                    appendable.append(" column:").append(column);
                }
            }
            else
            {
                appendable.append(" lines:").append(line);
                if (column != -1)
                {
                    appendable.append('c').append(column);
                }
                appendable.append('-').append(endLine);
                if (endColumn != -1)
                {
                    appendable.append('c').append(endColumn);
                }
            }
        }
    }

    private int writePureStackTrace(SafeAppendable appendable, String indent)
    {
        int stackLevel;

        PureException pureCause = getPureCause();
        if (pureCause == null)
        {
            stackLevel = 1;
        }
        else
        {
            stackLevel = pureCause.writePureStackTrace(appendable, indent);
        }

        if (indent != null)
        {
            appendable.append(indent).append(stackLevel).append(": ");
        }
        writeSourceInformationMessage(appendable, false);
        appendable.append('\n');

        return stackLevel + 1;
    }

    /**
     * Find the top instance of PureException in the causal chain
     * of throwable.  If throwable is itself an instance of
     * PureException, then it will be returned.  Otherwise, this
     * function searches down the causal chain until it finds an
     * instance of PureException.  If no instance can be found,
     * it returns null.
     *
     * @param throwable throwable
     * @return nearest PureException or null
     */
    public static PureException findPureException(Throwable throwable)
    {
        for (Throwable t = throwable; t != null; t = t.getCause())
        {
            if (t instanceof PureException)
            {
                return (PureException) t;
            }
        }
        return null;
    }

    /**
     * Return whether an instance of PureException can be found in
     * the causal chain of throwable.
     *
     * @param throwable throwable
     * @return whether there is a PureException in the causal chain
     */
    public static boolean canFindPureException(Throwable throwable)
    {
        return findPureException(throwable) != null;
    }
}
