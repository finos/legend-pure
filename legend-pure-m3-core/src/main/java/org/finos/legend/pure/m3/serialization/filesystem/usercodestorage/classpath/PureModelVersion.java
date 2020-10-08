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

import java.net.URL;
import java.util.Properties;

public class PureModelVersion
{
    public static final long PURE_MODEL_VERSION = version("pure_model_version");
    public static final String PURE_MODEL_VERSION_SPEC = String.format("{\"pure-model\" : %d}", PURE_MODEL_VERSION);

    private static long version(String name)
    {
        try
        {
            Properties properties = new Properties();
            URL url = PureModelVersion.class.getResource("/" + name);
            properties.load(url.openStream());
            String versionAsString = properties.getProperty("version");
            return (versionAsString == null) ? 0L : Long.parseLong(versionAsString);
        }
        catch (Exception e)
        {
            return 0L;
        }
    }
}
