package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.RichIterable;

abstract class StringCacheOrIndex
{
    public abstract RichIterable<String> getClassifierIds();

    protected static int classifierIdStringIndexToId(int index)
    {
        return -index - 1;
    }

    protected static int classifierIdStringIdToIndex(int id)
    {
        return -(id + 1);
    }

    protected static int otherStringIndexToId(int index)
    {
        return index + 1;
    }

    protected static int otherStringIdToIndex(int id)
    {
        return id - 1;
    }
}
