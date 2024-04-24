// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;

class DistributedBinaryFullGraphSerializer extends DistributedBinaryGraphSerializer
{
    DistributedBinaryFullGraphSerializer(PureRuntime runtime)
    {
        super(runtime, null);
    }

    @Override
    protected void collectInstancesForSerialization(SerializationCollector serializationCollector)
    {
        MutableSet<CoreInstance> stubClassifiers = AnyStubHelper.getStubClasses().collect(this.processorSupport::package_getByUserPath, Sets.mutable.empty());
        MutableSet<CoreInstance> primitiveTypes = PrimitiveUtilities.getPrimitiveTypes(this.processorSupport).toSet();
        GraphNodeIterable.fromModelRepository(this.runtime.getModelRepository(), instance ->
        {
            CoreInstance classifier = instance.getClassifier();
            if (stubClassifiers.contains(classifier))
            {
                return GraphWalkFilterResult.REJECT_AND_CONTINUE;
            }
            if (primitiveTypes.contains(classifier))
            {
                return GraphWalkFilterResult.REJECT_AND_STOP;
            }
            return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
        }).forEach(serializationCollector::collectInstanceForSerialization);
    }
}
