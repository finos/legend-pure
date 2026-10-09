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

package org.finos.legend.pure.lsp;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Conversions between {@code file:} URIs and local paths that hold on every platform.
 * <p>
 * The same file can legitimately be spelled several ways as a URI. On Windows, VS Code percent-encodes
 * the drive colon and lower-cases the drive letter ({@code file:///d%3A/repo/x.pure}), {@link Path#toUri()}
 * does neither ({@code file:///D:/repo/x.pure}), and {@link java.io.File#toURI()} also drops the empty
 * authority ({@code file:/D:/repo/x.pure}). Nor is the path component of a {@code file:} URI a usable local
 * path there: {@code /D:/repo/x.pure} is not a valid Windows path. So a URI must go through
 * {@link #toPath} to become a path, and through {@link #key} to be compared with, or used to look up,
 * another URI.
 */
public final class FileUris
{
    private static final String FILE_SCHEME = "file:";

    private FileUris()
    {
    }

    /**
     * The local path named by a {@code file:} URI, or null if the string is not a {@code file:} URI or
     * does not name a path on this platform.
     */
    public static Path toPath(String uri)
    {
        if (!isFileUri(uri))
        {
            return null;
        }
        try
        {
            return Paths.get(new URI(uri));
        }
        catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException | SecurityException ignore)
        {
            return null;
        }
    }

    /**
     * The local path named by either a {@code file:} URI or a local path string, or null if the string is
     * neither.
     */
    public static Path toPathFromUriOrPath(String uriOrPath)
    {
        if (uriOrPath == null)
        {
            return null;
        }
        if (isFileUri(uriOrPath))
        {
            return toPath(uriOrPath);
        }
        try
        {
            return Paths.get(uriOrPath);
        }
        catch (IllegalArgumentException ignore)
        {
            return null;
        }
    }

    /**
     * A key under which every spelling of the same {@code file:} URI collides, for use wherever URIs from
     * different sources (the client, {@link Path#toUri()}, the debug adapter) are compared or used to look
     * each other up. Any other string is its own key. The drive letter is folded because clients do not
     * agree on its case; the rest of the path is compared as spelled.
     */
    public static String key(String uri)
    {
        Path path = toPath(uri);
        if (path == null)
        {
            return uri;
        }
        String pathString = path.normalize().toString();
        if ((pathString.length() >= 2) && (pathString.charAt(1) == ':'))
        {
            pathString = Character.toUpperCase(pathString.charAt(0)) + pathString.substring(1);
        }
        return FILE_SCHEME + pathString;
    }

    private static boolean isFileUri(String uri)
    {
        return (uri != null) && uri.regionMatches(true, 0, FILE_SCHEME, 0, FILE_SCHEME.length());
    }
}
