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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;

class ClassLoaderURLFileNode extends ClassLoaderFileNode
{
    private final URL url;

    ClassLoaderURLFileNode(String path, URL url)
    {
        super(path);
        this.url = url;
    }

    @Override
    protected void writeToStringMessage(StringBuilder message)
    {
        message.append(" url=");
        message.append(this.url);
    }

    @Override
    InputStream getContent() throws IOException
    {
        return this.url.openStream();
    }

    @Override
    byte[] getContentAsBytes() throws IOException
    {
        URLConnection connection = this.url.openConnection();
        int sizeEstimate = estimateSize(connection);
        int defaultSize = 8192;
        try (InputStream stream = connection.getInputStream())
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream((sizeEstimate > 0) ? sizeEstimate : defaultSize);
            int bufferSize = (sizeEstimate > 0) ? Math.min(defaultSize, sizeEstimate) : defaultSize;
            byte[] buffer = new byte[bufferSize];
            for (int read = stream.read(buffer, 0, bufferSize); read != -1; read = stream.read(buffer, 0, bufferSize))
            {
                bytes.write(buffer, 0, read);
            }
            return bytes.toByteArray();
        }
    }

    private int estimateSize(URLConnection connection)
    {
        try
        {
            switch (this.url.getProtocol())
            {
                case "jar":
                {
                    if (!(connection instanceof JarURLConnection))
                    {
                        return -1;
                    }
                    JarEntry entry = ((JarURLConnection)connection).getJarEntry();
                    long size = entry.getSize();
                    return (size > 0) ? (int)size : -1;
                }
                case "file":
                {
                    Path path = Paths.get(this.url.toURI());
                    return (int)Files.size(path);
                }
                default:
                {
                    return -1;
                }
            }
        }
        catch (IOException|URISyntaxException e)
        {
            return -1;
        }
    }
}
