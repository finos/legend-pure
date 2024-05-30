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
 * Base class for all exceptions related to reference ids.
 */
public class ReferenceIdException extends RuntimeException
{
    public ReferenceIdException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ReferenceIdException(String message)
    {
        super(message);
    }

    public ReferenceIdException(Throwable cause)
    {
        super(cause);
    }
}
