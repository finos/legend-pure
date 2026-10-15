// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m4.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helpers for writing generated/serialized content to disk without disturbing files whose content is unchanged.
 * <p>
 * Code generators and serializers in this codebase are typically re-run on every build, even when their output
 * would be byte-for-byte identical to what is already on disk. A naive {@code Files.write} unconditionally
 * truncates and rewrites the target, which bumps its last-modified time and makes build tools (and downstream
 * consumers relying on file timestamps, e.g. Maven's own up-to-date checks or an IDE's incremental compiler)
 * think the file changed when it didn't. {@link #writeIfModified} avoids this by comparing the newly produced
 * content against the existing file and leaving the file untouched when they match.
 */
public final class FileWriteTools
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWriteTools.class);
    private static final int COMPARE_BUFFER_SIZE = 8192;

    private FileWriteTools()
    {
    }

    @FunctionalInterface
    public interface IOConsumer<T>
    {
        void accept(T t) throws IOException;
    }

    /**
     * Write {@code content} to {@code targetPath}, leaving the file untouched if it already contains exactly
     * {@code content}.
     *
     * @param targetPath path to write to
     * @param content    bytes to write
     * @return true if the target was created or replaced, false if it was left untouched
     * @throws IOException if an I/O error occurs
     */
    public static boolean writeIfModified(Path targetPath, byte[] content) throws IOException
    {
        return writeIfModified(targetPath, stream -> stream.write(content));
    }

    /**
     * Write to {@code targetPath} atomically, skipping the replace when the new content is byte-for-byte
     * identical to the existing file.
     * <p>
     * Content is first written to a sibling {@code <name>.tmp} file by invoking {@code writer} with an
     * {@link OutputStream} for that file. Once the write is complete:
     * <ol>
     *   <li>If the target file already exists <em>and</em> has the same size and identical content as the
     *       tmp file, the tmp file is deleted and the target is left untouched.</li>
     *   <li>Otherwise the tmp file is moved over the target using {@link StandardCopyOption#ATOMIC_MOVE} +
     *       {@link StandardCopyOption#REPLACE_EXISTING}. If the filesystem does not support atomic moves a
     *       non-atomic replacement is used as a fallback, which is still safer than writing directly because
     *       a reader will at worst see the previous complete file rather than a partially-written one.</li>
     * </ol>
     * <p>
     * If {@code writer} throws, the exception propagates immediately and the target file is left completely
     * untouched (only the, now orphaned, tmp file may remain).
     * <p>
     * <strong>Concurrency note:</strong> the content comparison and the subsequent {@code Files.move} are
     * <em>not</em> performed as a single atomic operation. A second thread (or process) could replace the
     * target file between the comparison and the move. In that window this method might overwrite a newer
     * version of the file with the same bytes it just decided were already present, or might skip a
     * replacement that is now necessary. This is acceptable when writers produce deterministic,
     * content-identical output for the same logical input — which is the intended use-case (serializing
     * generated code/compiler artefacts) — but callers that require a compare-and-swap guarantee must
     * implement their own external synchronisation.
     *
     * @param targetPath path to write to
     * @param writer     callback that writes the new content to the given stream
     * @return true if the target was created or replaced, false if it was left untouched
     * @throws IOException if an I/O error occurs
     */
    public static boolean writeIfModified(Path targetPath, IOConsumer<OutputStream> writer) throws IOException
    {
        Path parent = targetPath.getParent();
        if (parent != null)
        {
            Files.createDirectories(parent);
        }
        Path tmpPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(tmpPath)))
        {
            writer.accept(stream);
        }

        if (Files.exists(targetPath) && contentEquals(tmpPath, targetPath))
        {
            LOGGER.debug("Skipping replace of {} - content unchanged", targetPath);
            Files.delete(tmpPath);
            return false;
        }

        try
        {
            Files.move(tmpPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            LOGGER.warn("Atomic move not supported on this filesystem; falling back to non-atomic replace for {}", targetPath);
            Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    /**
     * Returns {@code true} iff {@code a} and {@code b} have the same size and identical byte content. Uses a
     * fixed-size read buffer to avoid loading large files into memory all at once.
     */
    private static boolean contentEquals(Path a, Path b) throws IOException
    {
        if (Files.size(a) != Files.size(b))
        {
            return false;
        }
        byte[] bufA = new byte[COMPARE_BUFFER_SIZE];
        byte[] bufB = new byte[COMPARE_BUFFER_SIZE];
        try (InputStream inA = new BufferedInputStream(Files.newInputStream(a), COMPARE_BUFFER_SIZE);
             InputStream inB = new BufferedInputStream(Files.newInputStream(b), COMPARE_BUFFER_SIZE))
        {
            int nA;
            while ((nA = inA.read(bufA)) != -1)
            {
                int nB = 0;
                while (nB < nA)
                {
                    int read = inB.read(bufB, nB, nA - nB);
                    if (read == -1)
                    {
                        return false; // b ended early - shouldn't happen after size check
                    }
                    nB += read;
                }
                for (int i = 0; i < nA; i++)
                {
                    if (bufA[i] != bufB[i])
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
