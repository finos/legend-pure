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
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.AbstractSimpleBinaryWriter;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FileWriters
{
    private FileWriters()
    {
        // static factory
    }

    public static FileWriter fromDirectory(Path directory)
    {
        return new FileSystemFileWriter(directory);
    }

    public static FileWriter fromInMemoryByteArrayMap(Map<String, ? super byte[]> fileBytes)
    {
        return new MapFileWriter<>(fileBytes, ByteArrayOutputStream::toByteArray);
    }

    public static FileWriter fromInMemoryByteListMap(Map<String, ? super ByteList> fileBytes)
    {
        return new MapFileWriter<>(fileBytes, baos -> ByteLists.mutable.with(baos.toByteArray()));
    }

    public static FileWriter fromJarOutputStream(JarOutputStream stream)
    {
        return new JarEntryFileWriter(stream);
    }

    private static class FileSystemFileWriter implements FileWriter
    {
        private final Path root;

        private FileSystemFileWriter(Path root)
        {
            this.root = root;
        }

        @Override
        public Writer getWriter(String path)
        {
            try
            {
                Path fullPath = this.root.resolve(path);
                Files.createDirectories(fullPath.getParent());
                return BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(fullPath)));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error getting writer for " + path, e);
            }
        }
    }

    private static class MapFileWriter<T> implements FileWriter
    {
        private final Map<String, ? super T> fileContents;
        private final Function<? super ByteArrayOutputStream, ? extends T> contentFn;

        private MapFileWriter(Map<String, ? super T> fileContents, Function<? super ByteArrayOutputStream, ? extends T> contentFn)
        {
            this.fileContents = fileContents;
            this.contentFn = contentFn;
        }

        @Override
        public Writer getWriter(String path)
        {
            return new AbstractSimpleBinaryWriter()
            {
                private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

                @Override
                public synchronized void writeByte(byte b)
                {
                    this.stream.write(b);
                }

                @Override
                public synchronized void writeBytes(byte[] bytes, int offset, int length)
                {
                    checkByteArray(bytes, offset, length);
                    this.stream.write(bytes, offset, length);
                }

                @Override
                public synchronized void close()
                {
                    storeContent(path, this.stream);
                }
            };
        }

        private void storeContent(String path, ByteArrayOutputStream stream)
        {
            T content = this.contentFn.apply(stream);
            synchronized (this.fileContents)
            {
                this.fileContents.put(path, content);
            }
        }
    }

    private static class JarEntryFileWriter implements FileWriter
    {
        private final JarOutputStream stream;

        private JarEntryFileWriter(JarOutputStream stream)
        {
            this.stream = stream;
        }

        @Override
        public Writer getWriter(String path)
        {
            try
            {
                this.stream.putNextEntry(new JarEntry(path));
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error getting writer for " + path, e);
            }
            return new AbstractSimpleBinaryWriter()
            {
                @Override
                public synchronized void writeByte(byte b)
                {
                    try
                    {
                        JarEntryFileWriter.this.stream.write(b);
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public synchronized void writeBytes(byte[] bytes, int offset, int length)
                {
                    checkByteArray(bytes, offset, length);
                    try
                    {
                        JarEntryFileWriter.this.stream.write(bytes, offset, length);
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public synchronized void close()
                {
                    try
                    {
                        JarEntryFileWriter.this.stream.closeEntry();
                    }
                    catch (IOException e)
                    {
                        throw new UncheckedIOException(e);
                    }
                }
            };
        }
    }
}
