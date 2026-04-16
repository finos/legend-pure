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

package org.finos.legend.pure.m3.pct.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.pure.m3.pct.shared.model.PCTManifest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Loads a {@link PCTManifest} from a JSON resource.
 *
 * <p>This is the single parsing point for PCT manifest files. Both the
 * interpreted and compiled engines delegate to this loader, then convert
 * the resulting POJO into the engine-specific Pure class representation.
 */
public class PCTManifestLoader
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PCTManifestLoader()
    {
        // utility class
    }

    /**
     * Load and validate a PCT manifest from the classpath.
     *
     * @param manifestPath classpath resource path (absolute or relative)
     * @return parsed and validated manifest
     * @throws IllegalArgumentException if the file is not found or is malformed
     */
    public static PCTManifest loadFromClasspath(String manifestPath)
    {
        String resourcePath = manifestPath.startsWith("/") ? manifestPath.substring(1) : manifestPath;
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath))
        {
            if (is == null)
            {
                throw new IllegalArgumentException("PCT manifest file not found on classpath: " + manifestPath);
            }
            return loadFromStream(is, manifestPath);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("IO error while processing PCT manifest: " + manifestPath, e);
        }
    }

    /**
     * Load and validate a PCT manifest from an arbitrary {@link InputStream}.
     *
     * <p>This is useful for the Pure IDE / code storage integration where
     * resources are loaded via {@code RepositoryCodeStorage.getContent()}.
     *
     * @param inputStream the input stream to read from (will be closed)
     * @param displayPath path shown in error messages
     * @return parsed and validated manifest
     * @throws IllegalArgumentException if the content is malformed
     */
    public static PCTManifest loadFromStream(InputStream inputStream, String displayPath) throws IOException
    {
        PCTManifest manifest = MAPPER.readValue(inputStream, PCTManifest.class);
        validate(manifest, displayPath);
        return manifest;
    }

    private static void validate(PCTManifest manifest, String displayPath)
    {
        if (manifest.adapter == null || manifest.adapter.isEmpty())
        {
            throw new IllegalArgumentException("PCT manifest '" + displayPath + "' is missing required field 'adapter'");
        }
        if (manifest.exclusions != null)
        {
            for (int i = 0; i < manifest.exclusions.size(); i++)
            {
                PCTManifest.PCTManifestExclusion ex = manifest.exclusions.get(i);
                if (ex.test == null || ex.test.isEmpty())
                {
                    throw new IllegalArgumentException("PCT manifest '" + displayPath + "': exclusion[" + i + "] is missing 'test'");
                }
                if (ex.expectedError == null)
                {
                    throw new IllegalArgumentException("PCT manifest '" + displayPath + "': exclusion[" + i + "] is missing 'expectedError'");
                }
            }
        }
    }
}
