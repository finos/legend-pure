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

import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipException;

public class PureRepositoryJarTools
{
    public static final String PURE_JAR_EXTENSION = ".par";
    public static final String META_INF_DIR_NAME = "META-INF";
    public static final String DEFINITION_INDEX_FILENAME = "definition-index.json";
    public static final String REFERENCE_INDEX_FILENAME = "reference-index.json";
    public static final String DEFINITION_INDEX_NAME = META_INF_DIR_NAME + "/" + DEFINITION_INDEX_FILENAME;
    public static final String REFERENCE_INDEX_NAME = META_INF_DIR_NAME + "/" + REFERENCE_INDEX_FILENAME;

    public static final Function<String, String> PURE_PATH_TO_BINARY_PATH = new Function<String, String>()
    {
        @Override
        public String valueOf(String purePath)
        {
            return purePathToBinaryPath(purePath);
        }
    };

    public static final Function<String, String> BINARY_PATH_TO_PURE_PATH = new Function<String, String>()
    {
        @Override
        public String valueOf(String binPath)
        {
            return binaryPathToPurePath(binPath);
        }
    };

    public static String purePathToBinaryPath(String purePath)
    {
        if (!CodeStorageTools.isPureFilePath(purePath))
        {
            throw new IllegalArgumentException("Invalid Pure path: " + purePath);
        }
        int start = (purePath.charAt(0) == '/') ? 1 : 0;
        int end = purePath.length() - 3;
        StringBuilder binaryPath = new StringBuilder(end + 2 - start);
        binaryPath.append(purePath, start, end);
        binaryPath.append('c');
        return binaryPath.toString();
    }

    public static boolean isPureBinaryPath(String path)
    {
        return (path != null) && path.endsWith(".pc");
    }

    public static String binaryPathToPurePath(String binPath)
    {
        if (!isPureBinaryPath(binPath))
        {
            throw new IllegalArgumentException("Invalid binary Pure path: " + binPath);
        }
        StringBuilder purePath = new StringBuilder(binPath.length() + 3);
        purePath.append('/');
        purePath.append(binPath, 0, binPath.length() - 1);
        purePath.append("ure");
        return purePath.toString();
    }

    public static boolean hasPureJarExtension(Path path)
    {
        return (path != null) && hasPureJarExtension(path.getFileName().toString());
    }

    public static boolean hasPureJarExtension(String path)
    {
        if (path == null)
        {
            return false;
        }
        int extLength = PURE_JAR_EXTENSION.length();
        int offset = path.length() - extLength;
        return (offset > 0) && path.regionMatches(true, offset, PURE_JAR_EXTENSION, 0, extLength);
    }

    public static boolean isPureJar(JarInputStream jarStream)
    {
        Manifest manifest = jarStream.getManifest();
        return (manifest != null) && PureManifest.isPureManifest(manifest);
    }

    public static boolean isPureJar(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            return false;
        }
        try (InputStream stream = Files.newInputStream(path))
        {
            return isPureJar(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPureJar(URL url)
    {
        try (InputStream stream = url.openStream())
        {
            return isPureJar(stream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean isPureJar(InputStream stream) throws IOException
    {
        try (JarInputStream jarStream = new JarInputStream(new BufferedInputStream(stream)))
        {
            return isPureJar(jarStream);
        }
        catch (ZipException e)
        {
            // Not a valid zip file, so not a Pure jar
            return false;
        }
    }
}
