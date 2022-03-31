package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.helper.AnyStubHelper;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphNodeIterable.NodeFilterResult;

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
                return NodeFilterResult.REJECT_AND_CONTINUE;
            }
            if (primitiveTypes.contains(classifier))
            {
                return NodeFilterResult.REJECT_AND_STOP;
            }
            return NodeFilterResult.ACCEPT_AND_CONTINUE;
        }).forEach(serializationCollector::collectInstanceForSerialization);
    }
}
