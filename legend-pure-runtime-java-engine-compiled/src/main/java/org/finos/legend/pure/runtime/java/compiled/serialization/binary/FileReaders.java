// Copyright 2021 Goldman Sachs
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

import org.eclipse.collections.api.list.primitive.ByteList;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileReaders
{
    private FileReaders()
    {
        // static factory
    }

    public static FileReader fromClassLoader(ClassLoader classLoader)
    {
        return new ClassLoaderFileReader(classLoader);
    }

    public static FileReader fromDirectory(Path directory)
    {
        return new FileSystemFileReader(directory);
    }

    public static FileReader fromInMemoryByteArrays(Map<String, ? extends byte[]> fileBytes)
    {
        return new InMemoryBytesFileReader<>(fileBytes, BinaryReaders::newBinaryReader);
    }

    public static FileReader fromInMemoryByteLists(Map<String, ? extends ByteList> fileBytes)
    {
        return new InMemoryBytesFileReader<>(fileBytes, BinaryReaders::newBinaryReader);
    }

    public static FileReader fromInMemoryByteBuffers(Map<String, ? extends ByteBuffer> fileBytes)
    {
        return new InMemoryBytesFileReader<>(fileBytes, BinaryReaders::newBinaryReader);
    }

    public static FileReader fromZipFile(ZipFile zipFile)
    {
        return new ZipFileReader(zipFile);
    }

    private static class ClassLoaderFileReader implements FileReader
    {
        private final ClassLoader classLoader;

        private ClassLoaderFileReader(ClassLoader classLoader)
        {
            this.classLoader = classLoader;
        }

        @Override
        public Reader getReader(String path)
        {
            InputStream stream = this.classLoader.getResourceAsStream(path);
            if (stream == null)
            {
                throw new RuntimeException("Cannot find file '" + path + "' in the class path");
            }
            return BinaryReaders.newBinaryReader(stream);
        }
    }

    private static class FileSystemFileReader implements FileReader
    {
        private final Path root;

        private FileSystemFileReader(Path root)
        {
            this.root = root;
        }

        @Override
        public Reader getReader(String path)
        {
            Path fullPath = this.root.resolve(path);
            try
            {
                return BinaryReaders.newBinaryReader(Files.newByteChannel(fullPath, Collections.emptySet()));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error accessing file '" + path + "'", e);
            }
        }
    }

    private static class InMemoryBytesFileReader<T> implements FileReader
    {
        private final Map<String, ? extends T> bytesByPath;
        private final Function<? super T, ? extends Reader> readerFn;

        private InMemoryBytesFileReader(Map<String, ? extends T> bytesByPath, Function<? super T, ? extends Reader> readerFn)
        {
            this.bytesByPath = bytesByPath;
            this.readerFn = readerFn;
        }

        @Override
        public Reader getReader(String path)
        {
            T bytes = this.bytesByPath.get(path);
            if (bytes == null)
            {
                throw new RuntimeException("Cannot find file '" + path + "'");
            }
            return this.readerFn.apply(bytes);
        }
    }

    private static class ZipFileReader implements FileReader
    {
        private final ZipFile zipFile;

        private ZipFileReader(ZipFile zipFile)
        {
            this.zipFile = zipFile;
        }

        @Override
        public Reader getReader(String path)
        {
            try
            {
                ZipEntry entry = this.zipFile.getEntry(path);
                return BinaryReaders.newBinaryReader(new BufferedInputStream(this.zipFile.getInputStream(entry)));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Error accessing file '" + path + "'", e);
            }
        }
    }
}
