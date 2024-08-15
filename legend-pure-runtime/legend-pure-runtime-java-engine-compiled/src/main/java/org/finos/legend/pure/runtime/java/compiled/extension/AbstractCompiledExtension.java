// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.extension;

import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public abstract class AbstractCompiledExtension implements CompiledExtension
{
    protected StringJavaSource loadExtraJavaSource(String packageName, String className, String resourceName)
    {
        String code = loadTextResource(resourceName);
        return StringJavaSource.newStringJavaSource(packageName, className, code, false);
    }

    protected String loadTextResource(String resourceName)
    {
        return loadTextResource(getClass().getClassLoader(), resourceName);
    }

    protected static StringJavaSource loadExtraJavaSource(String packageName, String className, ClassLoader classLoader, String resourceName)
    {
        String code = loadTextResource(classLoader, resourceName);
        return StringJavaSource.newStringJavaSource(packageName, className, code, false);
    }

    protected static String loadTextResource(ClassLoader classLoader, String resourceName)
    {
        URL url = classLoader.getResource(resourceName);
        if (url == null)
        {
            throw new RuntimeException("Cannot find resource: " + resourceName);
        }
        try (InputStream stream = url.openStream())
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8096];
            int read;
            while ((read = stream.read(buffer)) != -1)
            {
                if (read > 0)
                {
                    bytes.write(buffer, 0, read);
                }
            }
            return bytes.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
