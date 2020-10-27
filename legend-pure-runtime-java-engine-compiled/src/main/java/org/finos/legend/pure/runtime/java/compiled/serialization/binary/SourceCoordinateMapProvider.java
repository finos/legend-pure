package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.map.MutableMap;

public interface SourceCoordinateMapProvider {
    MutableMap<String, DistributedBinaryGraphDeserializer.SourceCoordinates> getMap(int instanceCount, String classifier);
}
