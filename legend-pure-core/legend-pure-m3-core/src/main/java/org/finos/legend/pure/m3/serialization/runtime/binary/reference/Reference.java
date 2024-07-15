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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface Reference
{
    Predicate<Reference> IS_RESOLVED = Reference::isResolved;
    Function<Reference, CoreInstance> GET_RESOLVED_INSTANCE = Reference::getResolvedInstance;

    /**
     * Attempt to resolve the reference and return whether the attempt
     * was successful. If the attempt was successful, then the resolved
     * instance can be accessed with the getResolvedInstance method. If
     * the attempt was unsuccessful, a message explaining the failure
     * can be accessed with the getFailureMessage method.  If the failure
     * is permanent, then an UnresolvableReferenceException will be thrown.
     *
     * @param repository       model repository
     * @param processorSupport processor support
     * @return whether the attempt to resolve was successful
     * @throws UnresolvableReferenceException if the reference is permanently unresolvable
     */
    boolean resolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException;

    /**
     * Return whether the reference has been resolved.
     *
     * @return whether the reference has been resolved
     */
    boolean isResolved();

    /**
     * Get the resolved instance if the reference has been resolved.
     * Returns null if the reference has not been resolved.
     *
     * @return resolved instance (or null)
     */
    CoreInstance getResolvedInstance();

    /**
     * Get the failure message if there was a failure to resolve
     * the instance. Returns null if there has been no attempt to
     * resolve the reference or if the reference has been successfully
     * resolved.
     *
     * @return failure message
     */
    String getFailureMessage();
}
