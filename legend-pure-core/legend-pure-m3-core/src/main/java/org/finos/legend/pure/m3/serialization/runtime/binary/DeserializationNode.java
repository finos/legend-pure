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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.collection.MutableCollection;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.Reference;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface DeserializationNode
{
    boolean isTopLevel();

    boolean isPackaged();

    boolean isAnonymous();

    void initializeInstance(ModelRepository repository, ProcessorSupport processorSupport);

    ReferenceResolutionResult resolveReferences(ModelRepository repository, ProcessorSupport processorSupport);

    void populateResolvedProperties();

    void collectUnresolvedReferences(MutableCollection<Reference> target);

    CoreInstance getInstance();

    class ReferenceResolutionResult
    {
        private final int newlyResolved;
        private final int unresolved;

        public ReferenceResolutionResult(int newlyResolved, int unresolved)
        {
            this.newlyResolved = newlyResolved;
            this.unresolved = unresolved;
        }

        public int getNewlyResolved()
        {
            return this.newlyResolved;
        }

        public int getUnresolved()
        {
            return this.unresolved;
        }

        public ReferenceResolutionResult join(ReferenceResolutionResult other)
        {
            return new ReferenceResolutionResult(this.newlyResolved + other.newlyResolved, this.unresolved + other.unresolved);
        }

        public ReferenceResolutionResult join(ReferenceResolutionResult... others)
        {
            if ((others == null) || (others.length == 0))
            {
                return this;
            }
            int newNewlyResolved = this.newlyResolved;
            int newUnresolved = this.unresolved;
            for (int i = 0; i < others.length; i++)
            {
                newNewlyResolved += others[i].newlyResolved;
                newUnresolved += others[i].unresolved;
            }
            return new ReferenceResolutionResult(newNewlyResolved, newUnresolved);
        }
    }
}
