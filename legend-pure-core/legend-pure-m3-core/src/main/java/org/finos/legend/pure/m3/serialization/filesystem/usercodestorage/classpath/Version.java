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

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

public class Version
{
    public static final String DEFAULT_MODEL_VERSION = "unknown";
    // To test/develop against the comp servers -Dpure.emulateVersion=platform_version_of_compserver
    private static final String EMULATE_VERSION = System.getProperty("pure.emulateVersion");

    public static final String SERVER = EMULATE_VERSION == null ? version("/rev_server", "revision", "unknown") : EMULATE_VERSION;
    public static final String MODEL = version("/pure_model_version", "version", DEFAULT_MODEL_VERSION);
    public static final String PLATFORM = EMULATE_VERSION == null ? version("/org/finos/legend/pure/platform.properties", "version", null) : EMULATE_VERSION;

    protected static String version(String resourceName, String property, String defaultVersion)
    {
        try
        {
            URL url = Version.class.getResource(resourceName);
            if (url == null)
            {
                return defaultVersion;
            }

            Properties properties = new Properties();
            try (InputStream inStream = url.openStream())
            {
                properties.load(inStream);
            }
            return properties.getProperty(property, defaultVersion);
        }
        catch (Exception e)
        {
            return defaultVersion;
        }
    }
}
