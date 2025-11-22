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

public interface ReferenceIdProvider
{
    /**
     * Version of the {@link ReferenceIdExtension} that this provider is associated with.
     *
     * @return extension version
     */
    int version();

    /**
     * Return whether the instance has a reference id.
     *
     * @param instance instance
     * @return whether instance has a reference id
     */
    boolean hasReferenceId(CoreInstance instance);

    /**
     * Get an id for the given reference instance. If no id can be found or computed for the reference instance, then
     * the implementing class should throw a {@link ReferenceIdProvisionException} with an explanation of why not.
     *
     * @param reference reference instance
     * @return reference id
     * @throws ReferenceIdProvisionException if no id can be found or computed
     */
    String getReferenceId(CoreInstance reference) throws ReferenceIdProvisionException;
}
