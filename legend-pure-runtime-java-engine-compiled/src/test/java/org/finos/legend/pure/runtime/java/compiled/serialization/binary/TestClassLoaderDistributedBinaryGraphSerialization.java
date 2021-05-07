package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

public class TestClassLoaderDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path jarPath;
    private JarOutputStream jarOutputStream;
    private URLClassLoader classLoader;

    @After
    public void cleanUpJarStream() throws IOException
    {
        if (this.jarOutputStream != null)
        {
            this.jarOutputStream.close();
        }
    }

    @After
    public void cleanUpClassLoader() throws IOException
    {
        if (this.classLoader != null)
        {
            this.classLoader.close();
        }
    }

    @Override
    protected FileWriter getFileWriter() throws IOException
    {
        this.jarPath = this.temporaryFolder.newFile("distMetadata.jar").toPath();
        this.jarOutputStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(this.jarPath)));
        return FileWriters.fromJarOutputStream(this.jarOutputStream);
    }

    @Override
    protected FileReader getFileReader() throws IOException
    {
        this.jarOutputStream.close();
        this.classLoader = new URLClassLoader(new URL[] {this.jarPath.toUri().toURL()}, Thread.currentThread().getContextClassLoader());
        return FileReaders.fromClassLoader(this.classLoader);
    }
}
