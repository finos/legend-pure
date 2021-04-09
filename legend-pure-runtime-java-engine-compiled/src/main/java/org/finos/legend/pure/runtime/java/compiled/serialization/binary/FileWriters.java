package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.AbstractBinaryWriter;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public static FileWriter newJarOutputStream(JarOutputStream stream)
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
                throw new RuntimeException("Error getting writer for " + path, e);
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
            return new AbstractBinaryWriter()
            {
                private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

                @Override
                public synchronized void close()
                {
                    storeContent(path, this.stream);
                }

                @Override
                protected void write(byte b)
                {
                    this.stream.write(b);
                }

                @Override
                protected void write(byte[] bytes, int offset, int length)
                {
                    this.stream.write(bytes, offset, length);
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
                throw new RuntimeException("Error getting writer for " + path, e);
            }
            return new AbstractBinaryWriter()
            {
                @Override
                public synchronized void close()
                {
                    try
                    {
                        JarEntryFileWriter.this.stream.closeEntry();
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                protected void write(byte b)
                {
                    try
                    {
                        JarEntryFileWriter.this.stream.write(b);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                protected void write(byte[] bytes, int offset, int length)
                {
                    try
                    {
                        JarEntryFileWriter.this.stream.write(bytes, offset, length);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }
}
