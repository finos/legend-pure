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

import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

public class TestJarDistributedBinaryGraphSerialization extends TestDistributedBinaryGraphSerialization
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Path jarPath;
    private JarOutputStream jarOutputStream;
    private ZipFile jarZipFile;

    @After
    public void cleanUpJarStream() throws IOException
    {
        if (this.jarOutputStream != null)
        {
            this.jarOutputStream.close();
        }
    }

    @After
    public void cleanUpZipFile() throws IOException
    {
        if (this.jarZipFile != null)
        {
            this.jarZipFile.close();
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
        this.jarZipFile = new ZipFile(this.jarPath.toFile());
        return FileReaders.fromZipFile(this.jarZipFile);
    }
}
