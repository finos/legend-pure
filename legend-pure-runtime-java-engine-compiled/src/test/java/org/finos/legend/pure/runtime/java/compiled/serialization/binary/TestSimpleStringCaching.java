package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.runtime.java.compiled.serialization.GraphSerializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Serialized;

public class TestSimpleStringCaching extends TestStringCaching<SimpleStringCache>
{
    private static final String CACHE_PATH = "metadata/strings.idx";

    @Override
    protected SimpleStringCache buildCache()
    {
        Serialized serialized = GraphSerializer.serializeAll(repository.getTopLevels(), runtime.getProcessorSupport(), false);
        return SimpleStringCache.fromSerialized(serialized);
    }

    @Override
    protected void serialize(SimpleStringCache cache, FileWriter fileWriter)
    {
        try (Writer writer = fileWriter.getWriter(CACHE_PATH))
        {
            cache.write(writer);
        }
    }

    @Override
    protected StringIndex buildIndex(FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(CACHE_PATH))
        {
            return EagerStringIndex.fromReader(reader);
        }
    }
}
