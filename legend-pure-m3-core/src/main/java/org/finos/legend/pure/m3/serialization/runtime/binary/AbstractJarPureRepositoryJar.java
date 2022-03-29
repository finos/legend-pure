// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

abstract class AbstractJarPureRepositoryJar extends AbstractPureRepositoryJar
{
    private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
    private static final ImmutableSet<String> SKIP_FILES = Sets.immutable.with(PureRepositoryJarTools.DEFINITION_INDEX_NAME, PureRepositoryJarTools.REFERENCE_INDEX_NAME);

    AbstractJarPureRepositoryJar(PureRepositoryJarMetadata metadata)
    {
        super(metadata);
    }

    @Override
    public byte[] readFile(String filePath)
    {
        try (JarInputStream stream = getJarInputStream())
        {
            return readFileFromJarInputStream(filePath, stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading " + filePath, e);
        }
    }

    @Override
    public void readAllFiles(MutableMap<String, byte[]> fileBytes)
    {
        try (JarInputStream stream = getJarInputStream())
        {
            readAllFilesFromJarInputStream(stream, fileBytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error reading all files", e);
        }
    }

    @Override
    protected void readFilesFromNonEmptySet(SetIterable<String> filePaths, MutableMap<String, byte[]> fileBytes)
    {
        try (JarInputStream jarStream = getJarInputStream())
        {
            readFilesFromJarInputStream(filePaths, jarStream, fileBytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException(filePaths.toSortedList().makeString("Error reading files: ", ", ", ""), e);
        }
    }

    protected byte[] readFileFromJarInputStream(String filePath, JarInputStream jarStream) throws IOException
    {
        for (JarEntry entry = jarStream.getNextJarEntry(); entry != null; entry = jarStream.getNextJarEntry())
        {
            if (filePath.equals(entry.getName()))
            {
                int expectedSize = getAndCheckExpectedSize(entry);
                int bufferSize = (expectedSize < 0) ? DEFAULT_READ_BUFFER_SIZE : Math.min(DEFAULT_READ_BUFFER_SIZE, expectedSize);
                ByteArrayOutputStream outStream = new ByteArrayOutputStream((expectedSize < 0) ? 8192 : expectedSize);
                writeToOutputStream(jarStream, outStream, new byte[bufferSize]);
                return outStream.toByteArray();
            }
        }
        throw new IllegalArgumentException("Could not find file: " + filePath);
    }

    protected void readFilesFromJarInputStream(SetIterable<String> filePaths, JarInputStream jarStream, MutableMap<String, byte[]> fileBytes) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        MutableSet<String> filesToRead = filePaths.toSet();
        for (JarEntry entry = jarStream.getNextJarEntry(); (entry != null) && filesToRead.notEmpty(); entry = jarStream.getNextJarEntry())
        {
            String filePath = entry.getName();
            if (filesToRead.remove(filePath))
            {
                bytes.reset();
                writeToOutputStream(jarStream, bytes, buffer);
                fileBytes.put(filePath, bytes.toByteArray());
            }
        }
    }

    protected void readAllFilesFromJarInputStream(JarInputStream jarStream, MutableMap<String, byte[]> fileBytes) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[DEFAULT_READ_BUFFER_SIZE];
        for (JarEntry entry = jarStream.getNextJarEntry(); entry != null; entry = jarStream.getNextJarEntry())
        {
            String filePath = entry.getName();
            if (!SKIP_FILES.contains(filePath))
            {
                bytes.reset();
                writeToOutputStream(jarStream, bytes, buffer);
                fileBytes.put(filePath, bytes.toByteArray());
            }
        }
    }

    protected abstract JarInputStream getJarInputStream() throws IOException;

    private static int getAndCheckExpectedSize(JarEntry entry)
    {
        long expectedSize = entry.getSize();  //NOSONAR JARs are trusted
        if (expectedSize > Integer.MAX_VALUE)
        {
            throw new RuntimeException("Required array size too large");
        }
        return (int)expectedSize;
    }

    private static void writeToOutputStream(InputStream inStream, OutputStream outStream, byte[] buffer) throws IOException
    {
        int bufferSize = buffer.length;
        for (int read = inStream.read(buffer, 0, bufferSize); read != -1; read = inStream.read(buffer, 0, bufferSize))
        {
            outStream.write(buffer, 0, read);
        }
    }
}
