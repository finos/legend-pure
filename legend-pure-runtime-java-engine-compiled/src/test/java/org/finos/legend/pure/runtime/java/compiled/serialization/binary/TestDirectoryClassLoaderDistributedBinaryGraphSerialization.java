package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class TestDirectoryClassLoaderDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private URLClassLoader classLoader;

    @After
    public void cleanUpClassLoader() throws IOException
    {
        if (this.classLoader != null)
        {
            this.classLoader.close();
        }
    }

    @Override
    protected FileWriter getFileWriter()
    {
        return FileWriters.fromDirectory(this.temporaryFolder.getRoot().toPath());
    }

    @Override
    protected FileReader getFileReader() throws IOException
    {
        this.classLoader = new URLClassLoader(new URL[] {this.temporaryFolder.getRoot().toURI().toURL()}, Thread.currentThread().getContextClassLoader());
        return FileReaders.fromClassLoader(this.classLoader);
    }
}

