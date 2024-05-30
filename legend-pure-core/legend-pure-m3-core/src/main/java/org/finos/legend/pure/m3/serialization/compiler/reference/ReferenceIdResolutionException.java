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
 * Base class for all exceptions related to reference id resolution.
 */
public class ReferenceIdResolutionException extends ReferenceIdException
{
    private final String referenceId;

    public ReferenceIdResolutionException(String referenceId, String message, Throwable cause)
    {
        super(message, cause);
        this.referenceId = referenceId;
    }

    public ReferenceIdResolutionException(String referenceId, String message)
    {
        super(message);
        this.referenceId = referenceId;
    }

    /**
     * Get the reference id involved in the exception. Note that this may be null.
     *
     * @return reference id (possibly null)
     */
    public String getReferenceId()
    {
        return this.referenceId;
    }
}
