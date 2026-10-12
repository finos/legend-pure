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

package org.finos.legend.pure.rust;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when an error occurs during Pure evaluation in the Rust engine.
 */
public class PureRustEvaluationException extends PureRustException
{
    public enum Kind
    {
        EXECUTION_ERROR,
        ASSERTION_FAILED,
        CONSTRAINT_VIOLATION,
        /**
         * The function path or mangled native key is unknown to the Rust side.
         */
        FUNCTION_NOT_FOUND,
        /**
         * A language feature or value kind whose runtime is not available yet.
         */
        UNSUPPORTED,
        /**
         * A native registered under its mangled key whose implementation has not been fished yet.
         */
        NOT_IMPLEMENTED,
    }

    public enum ConstraintKind
    {
        /** Class invariant. */
        CLASS,
        /** Function pre-condition. */
        PRE,
        /** Function post-condition. */
        POST,
    }

    private final Kind kind;
    private final String info;
    private final SourceInformation sourceInformation;
    private final List<String> callStack;
    private final String constraintId;
    private final ConstraintKind constraintKind;
    private final String ownerFqn;

    public PureRustEvaluationException(String message)
    {
        super(message);
        this.kind = Kind.EXECUTION_ERROR;
        this.info = message;
        this.sourceInformation = null;
        this.callStack = Collections.emptyList();
        this.constraintId = null;
        this.constraintKind = null;
        this.ownerFqn = null;
    }

    /**
     * A failure as the Rust evaluator reports it, through whichever transport
     * carried it: every field of the boundary's failure payload, the source
     * span as its parts.
     *
     * <p>All string params except {@code message} and {@code kind} may
     * be {@code null}; {@code callStack} may be {@code null} or empty.
     * A {@code null} {@code sourceId} is no span, and the four positions are
     * then ignored. Unknown {@code kind} / {@code constraintKindStr} strings
     * fall back to {@link Kind#EXECUTION_ERROR} / {@code null} respectively.
     */
    public PureRustEvaluationException(String message,
                                       String info,
                                       String kindStr,
                                       String sourceId,
                                       int startLine,
                                       int startColumn,
                                       int endLine,
                                       int endColumn,
                                       String[] callStack,
                                       String constraintId,
                                       String constraintKindStr,
                                       String ownerFqn)
    {
        super(message);
        this.kind = parseKind(kindStr);
        this.info = (info == null) ? message : info;
        this.sourceInformation = (sourceId == null) ? null : new SourceInformation(sourceId, startLine, startColumn, endLine, endColumn);
        this.callStack = callStack == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(java.util.Arrays.asList(callStack));
        this.constraintId = constraintId;
        this.constraintKind = parseConstraintKind(constraintKindStr);
        this.ownerFqn = ownerFqn;
    }

    /**
     * A failure of kind {@link Kind#UNSUPPORTED} raised on the Java side.
     *
     * @param what the unsupported thing, completing "Unsupported: ..."
     * @return the exception to throw
     */
    public static PureRustEvaluationException unsupported(String what)
    {
        return new PureRustEvaluationException("Unsupported: " + what, what, Kind.UNSUPPORTED.name(), null, 0, 0, 0, 0, null, null, null, null);
    }

    /**
     * The failure text without the kind prefix that {@link #getMessage()}
     * carries: what Pure's {@code PureException.getInfo()} reports for the
     * same failure.
     */
    public String getInfo()
    {
        return this.info;
    }

    public Kind getKind()
    {
        return this.kind;
    }

    /**
     * @return where the failure happened, start (the anchor) and end, or
     *         {@code null} when it has no span
     */
    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    /**
     * @return Pure-level call stack as a list of
     *         {@code "<function-name> <- file:line:col"} strings.
     *         Innermost frame first; empty when no frames were captured.
     */
    public List<String> getCallStack()
    {
        return this.callStack;
    }

    /**
     * @return constraint identifier (rule name) for
     *         {@link Kind#CONSTRAINT_VIOLATION}; {@code null} otherwise.
     */
    public String getConstraintId()
    {
        return this.constraintId;
    }

    /**
     * @return constraint axis for {@link Kind#CONSTRAINT_VIOLATION};
     *         {@code null} otherwise.
     */
    public ConstraintKind getConstraintKind()
    {
        return this.constraintKind;
    }

    /**
     * @return {@code ::}-joined FQN of the constraint-owning Class /
     *         Function for {@link Kind#CONSTRAINT_VIOLATION};
     *         {@code null} otherwise.
     */
    public String getOwnerFqn()
    {
        return this.ownerFqn;
    }

    private static Kind parseKind(String s)
    {
        if (s == null)
        {
            return Kind.EXECUTION_ERROR;
        }
        try
        {
            return Kind.valueOf(s);
        }
        catch (IllegalArgumentException ignored)
        {
            return Kind.EXECUTION_ERROR;
        }
    }

    private static ConstraintKind parseConstraintKind(String s)
    {
        if (s == null)
        {
            return null;
        }
        try
        {
            return ConstraintKind.valueOf(s);
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
    }
}
