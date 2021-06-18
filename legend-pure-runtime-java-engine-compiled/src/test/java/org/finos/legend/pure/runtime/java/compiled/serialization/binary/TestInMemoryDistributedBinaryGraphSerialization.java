package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

public class TestInMemoryDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    private final MutableMap<String, byte[]> filesBytes = Maps.mutable.empty();

    @Override
    protected FileWriter getFileWriter()
    {
        return FileWriters.fromInMemoryByteArrayMap(this.filesBytes);
    }

    @Override
    protected FileReader getFileReader()
    {
        return FileReaders.fromInMemoryByteArrays(this.filesBytes);
    }
}
