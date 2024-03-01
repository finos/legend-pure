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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.impl.utility.Iterate;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FileTools
{
    private FileTools()
    {
        // Utility class
    }

    /**
     * Get the basic file attributes of a path.  Returns
     * null if the path does not exist.
     *
     * @param path path
     * @return basic file attributes
     */
    public static BasicFileAttributes getBasicFileAttributes(Path path)
    {
        try
        {
            return Files.readAttributes(path, BasicFileAttributes.class);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /**
     * Fully delete a path.  If the path is a directory, its
     * contents are deleted recursively.  Note that this is
     * not an atomic operation, so if an error occurs some
     * content may be deleted and other content not.
     *
     * @param path path to delete
     * @throws IOException
     */
    public static void delete(Path path) throws IOException
    {
        delete(path, false);
    }

    /**
     * Fully delete a path.  If the path is a directory, its
     * contents are deleted recursively.  Note that this is
     * not an atomic operation, so if an error occurs some
     * content may be deleted and other content not.  If
     * force is true and a path to be deleted is not writable,
     * an attempt will be made to make it writable before
     * deleting it.
     *
     * @param path  path to delete
     * @param force whether to force delete
     * @throws IOException
     */
    public static void delete(Path path, boolean force) throws IOException
    {
        BasicFileAttributes attributes = getBasicFileAttributes(path);
        if (attributes != null)
        {
            if (attributes.isDirectory())
            {
                deleteDirectoryContents(path, force);
            }
            try
            {
                Files.delete(path);
            }
            catch (IOException e)
            {
                // If we're not forcing, simply throw.
                if (!force)
                {
                    throw e;
                }
                // If the file is not writable, try to make it writable; then retry the delete.
                if (!Files.isWritable(path))
                {
                    if (path.toFile().setWritable(true))
                    {
                        Files.delete(path);
                    }
                }
                else
                {
                    Files.delete(path);
                }
            }
        }
    }

    /**
     * Delete the contents of a directory without deleting the
     * directory itself.  If the directory contains subdirectories,
     * they and their contents will be deleted recursively.  Note
     * that this is not an atomic operation, so if an error occurs
     * some content may be deleted and other content not.
     *
     * @param directory directory to delete contents of
     * @throws IOException
     */
    public static void deleteDirectoryContents(Path directory) throws IOException
    {
        deleteDirectoryContents(directory, false);
    }

    /**
     * Delete the contents of a directory without deleting the
     * directory itself.  If the directory contains subdirectories,
     * they and their contents will be deleted recursively.  Note
     * that this is not an atomic operation, so if an error occurs
     * some content may be deleted and other content not.  If
     * force is true and a path to be deleted is not writable,
     * an attempt will be made to make it writable before
     * deleting it.
     *
     * @param directory directory to delete contents of
     * @param force     whether to force delete
     * @throws IOException
     */
    public static void deleteDirectoryContents(Path directory, boolean force) throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory))
        {
            for (Path entry : dirStream)
            {
                delete(entry, force);
            }
        }
    }

    /**
     * Return whether a directory is empty.  Note that this assumes
     * the path is a directory.
     *
     * @param directory directory
     * @return whether directory is empty
     * @throws IOException
     */
    public static boolean isDirectoryEmpty(Path directory) throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory))
        {
            return Iterate.isEmpty(dirStream);
        }
    }

    public static Optional<Path> findOldestModified(Path... directory) throws IOException
    {
        directory = Arrays.asList(directory)
                .stream()
                .filter(x -> x == null)
                .collect(Collectors.toList()).toArray(new Path[]{});

        if (directory.length == 0)
        {
            return Optional.empty();
        }
        else
        {
            Optional<Path> first = findOldestModified(directory[0]);
            for (int i = 1; i < directory.length; i++)
            {
                Optional<Path> next = findOldestModified(directory[i]);
                if (!first.isPresent()  || (next.isPresent() && next.get().toFile().lastModified() < first.get().toFile().lastModified()))
                {
                    first = next;
                }
            }

            return first;
        }
    }

    public static Optional<Path> findOldestModified(Path directory) throws IOException
    {
        return findOldestModified(directory, f -> true);
    }

    public static Optional<Path> findOldestModified(Path directory, Predicate<Path> fileFilter) throws IOException
    {
        final AtomicLong minModifiedTime = new AtomicLong(0);
        final AtomicReference<Path> minModifiedPath = new AtomicReference<>();

        if (Files.exists(directory))
        {
            Files.walkFileTree(Paths.get(directory.toAbsolutePath().toString()), new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    if (!Files.isDirectory(file) && Files.exists(file))
                    {
                        if (fileFilter.test(file))
                        {
                            long fileLastModifiedValue = attrs.lastModifiedTime().toMillis();

                            minModifiedTime.compareAndSet(0, fileLastModifiedValue);
                            if (fileLastModifiedValue <= minModifiedTime.get())
                            {
                                minModifiedTime.set(fileLastModifiedValue);
                                minModifiedPath.set(file);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return Optional.ofNullable(minModifiedPath.get());
    }
}
