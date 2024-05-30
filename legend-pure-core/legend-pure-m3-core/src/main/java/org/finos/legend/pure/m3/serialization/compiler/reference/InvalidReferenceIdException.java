// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.reference;

/**
 * Exception arising when a reference id cannot be resolved because the id is invalid.
 */
public class InvalidReferenceIdException extends ReferenceIdResolutionException
{
    public InvalidReferenceIdException(String referenceId, String message, Throwable cause)
    {
        super(referenceId, message, cause);
    }

    public InvalidReferenceIdException(String referenceId, String message)
    {
        super(referenceId, message);
    }

    public InvalidReferenceIdException(String referenceId, Throwable cause)
    {
        this(referenceId, buildMessage(referenceId), cause);
    }

    public InvalidReferenceIdException(String referenceId)
    {
        this(referenceId, buildMessage(referenceId));
    }

    private static String buildMessage(String referenceId)
    {
        return (referenceId == null) ?
               "Invalid reference id: null" :
               ("Invalid reference id: \"" + referenceId + "\"");
    }
}
