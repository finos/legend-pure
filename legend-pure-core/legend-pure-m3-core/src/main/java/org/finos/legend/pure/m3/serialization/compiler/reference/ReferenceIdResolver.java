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

import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface ReferenceIdResolver
{
    /**
     * Version of the {@link ReferenceIdExtension} that this resolver is associated with.
     *
     * @return extension version
     */
    int version();

    /**
     * Resolve the reference for the given id. If the id is invalid, the implementing class should throw an
     * {@link InvalidReferenceIdException}. If the id cannot be resolved, the implementing class should throw an
     * {@link UnresolvableReferenceIdException}.
     *
     * @param referenceId reference id
     * @return reference instance
     * @throws InvalidReferenceIdException if the id is invalid
     * @throws UnresolvableReferenceIdException if the id cannot be resolved
     */
    CoreInstance resolveReference(String referenceId) throws InvalidReferenceIdException, UnresolvableReferenceIdException;
}
