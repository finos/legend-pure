package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class TestDirectoryDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    protected FileWriter getFileWriter()
    {
        return FileWriters.fromDirectory(this.temporaryFolder.getRoot().toPath());
    }

    @Override
    protected FileReader getFileReader()
    {
        return FileReaders.fromDirectory(this.temporaryFolder.getRoot().toPath());
    }
}
