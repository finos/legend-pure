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

package org.finos.legend.pure.m3.navigation.linearization;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

interface TypeSupport
{
    /**
     * Get the direct generalizations of the given type in the
     * order they are declared.
     *
     * @param type type
     * @param processorSupport
     * @return direct generalizations of type in order
     */
    ListIterable<CoreInstance> getDirectGeneralizations(CoreInstance type, ProcessorSupport processorSupport);

    /**
     * Get all (direct and indirect) generalizations of the given
     * type in resolution order.  This method gives the type manager
     * the opportunity to get the generalizations from a cache, if
     * present.  Otherwise, generator can be used to calculate the
     * generalizations.
     *
     * @param type      type
     * @param generator generalization generator
     * @param processorSupport
     * @return all generalizations of type in resolution order
     */
    ImmutableList<CoreInstance> getGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator, ProcessorSupport processorSupport);

    /**
     * Return whether two types are equal.
     *
     * @param type1 first type
     * @param type2 second type
     * @param processorSupport
     * @return whether type1 and type2 are equal
     */
    boolean check_typeEquality(CoreInstance type1, CoreInstance type2, ProcessorSupport processorSupport);
}
