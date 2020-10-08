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
        StringBuilder builder = new StringBuilder((this.info == null) ? 128 : (this.info.length() + 128));

        try
        {
            String exceptionName = getExceptionName();
            builder.append((exceptionName == null) ? "Error" : exceptionName);
            builder.append(" at ");
            writeSourceInformationMessage(builder, true);
            if (hasAdditionalMessageInfo())
            {
                builder.append(", ");
                writeAdditionalMessageInfo(builder);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return builder.toString();
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
        StringBuilder builder = new StringBuilder(256);
        printPureStackTrace(builder, indent);
        return builder.toString();
    }

    /**
     * Print the Pure stack trace from this exception to
     * System.out.
     */
    public void printPureStackTrace()
    {
        printPureStackTrace((String)null);
    }

    /**
     * Print the Pure stack trace from this exception to
     * System.out.  Each line is indented with the given
     * indent string (where null indicates no indentation).
     *
     * @param indent
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
     */
    public void printPureStackTrace(Appendable appendable)
    {
        printPureStackTrace(appendable, null);
    }

    /**
     * Print the Pure stack trace from this exception to the
     * given appendable.  Each line is indented with the given
     * indent string (where null indicates no indentation).
     *
     * @param appendable appendable to print stack trace to
     * @param indent     indentation string for each line of the stack trace
     */
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
     * @throws IOException
     */
    protected void writeAdditionalMessageInfo(Appendable appendable) throws IOException
    {
        appendable.append('"');
        appendable.append(this.info);
        appendable.append('"');
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

    private void writeSourceInformationMessage(Appendable appendable, boolean includeParens) throws IOException
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

            appendable.append("resource:");
            appendable.append(this.sourceInformation.getSourceId());

            printSourceInformationWithoutSourceId(appendable, this.sourceInformation);

            if (includeParens)
            {
                appendable.append(')');
            }
        }
    }

    public static void printSourceInformationWithoutSourceId(Appendable appendable, SourceInformation sourceInformation) throws IOException
    {
        int line = sourceInformation.getLine();
        int endLine = sourceInformation.getEndLine();
        int column = sourceInformation.getColumn();
        int endColumn = sourceInformation.getEndColumn();
        if (line != -1)
        {
            if (line == endLine || endLine == -1)
            {
                appendable.append(" line:");
                appendable.append(Integer.toString(line));
                if (column != -1)
                {
                    appendable.append(" column:");
                    appendable.append(Integer.toString(column));
                }
            }
            else
            {
                appendable.append(" lines:");
                appendable.append(Integer.toString(line));
                if (column != -1)
                {
                    appendable.append('c');
                    appendable.append(Integer.toString(column));
                }
                appendable.append('-');
                appendable.append(Integer.toString(endLine));
                if (endColumn != -1)
                {
                    appendable.append('c');
                    appendable.append(Integer.toString(endColumn));
                }
            }
        }
    }

    private int writePureStackTrace(Appendable appendable, String indent) throws IOException
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
            appendable.append(indent);
            appendable.append(Integer.toString(stackLevel));
            appendable.append(": ");
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
                return (PureException)t;
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
