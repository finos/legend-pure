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

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when an error occurs during Pure evaluation in the Rust engine.
 *
 * <p>The Rust side surfaces structured information through the rich
 * constructor: a top-level {@link Kind}, optional source location and
 * call stack, and (for constraint violations) the constraint
 * identifier, axis, and owning element. The legacy message-only
 * constructor stays for backwards compatibility and is used as the
 * fallback path when the cdylib is older than this JAR.
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
    private final String sourceInfo;
    private final List<String> callStack;
    private final String constraintId;
    private final ConstraintKind constraintKind;
    private final String ownerFqn;

    /**
     * Legacy constructor preserved for backwards compatibility.
     *
     * <p>Older cdylibs and direct callers without structured info land
     * here; defaults the structured fields to {@code null} / empty
     * with {@link Kind#EXECUTION_ERROR} as the safest assumption.
     */
    public PureRustEvaluationException(String message)
    {
        super(message);
        this.kind = Kind.EXECUTION_ERROR;
        this.sourceInfo = null;
        this.callStack = Collections.emptyList();
        this.constraintId = null;
        this.constraintKind = null;
        this.ownerFqn = null;
    }

    /**
     * Rich constructor invoked from the Rust JNI side.
     *
     * <p>All string params except {@code message} and {@code kind} may
     * be {@code null}; {@code callStack} may be {@code null} or empty.
     * Unknown {@code kind} / {@code constraintKindStr} strings fall
     * back to {@link Kind#EXECUTION_ERROR} / {@code null} respectively,
     * matching the cdylib-older-than-JAR forward-compatibility policy.
     */
    public PureRustEvaluationException(String message,
                                       String kindStr,
                                       String sourceInfo,
                                       String[] callStack,
                                       String constraintId,
                                       String constraintKindStr,
                                       String ownerFqn)
    {
        super(message);
        this.kind = parseKind(kindStr);
        this.sourceInfo = sourceInfo;
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
        return new PureRustEvaluationException("Unsupported: " + what, Kind.UNSUPPORTED.name(), null, null, null, null, null);
    }

    public Kind getKind()
    {
        return this.kind;
    }

    /**
     * @return rendered source location as {@code file:line:column}, or
     *         {@code null} when no source anchor is available
     *         (FFI-boundary errors, panics).
     */
    public String getSourceInfo()
    {
        return this.sourceInfo;
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
