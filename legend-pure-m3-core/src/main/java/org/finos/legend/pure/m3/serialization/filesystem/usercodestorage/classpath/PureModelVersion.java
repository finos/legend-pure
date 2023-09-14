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
import java.util.Optional;
import java.util.Properties;

public class PureModelVersion
{
    public static final Optional<String> PURE_MODEL_VERSION = version("pure_model_version");
    public static final String PURE_MODEL_VERSION_SPEC = String.format("{\"pure-model\" : %s}", PURE_MODEL_VERSION);

    // Note: the version contains three states:
    // 1. A value which is the VCS version of the model
    // 2. Empty meaning an underlying error in getting the version or there is no version associated in the underlying VCS
    // 3. 0 meaning there is no version but don't handle this as an error
    // Here we want to not error out upstream hence we return 0 if no error is returned from the class
    private static Optional<String> version(String name)
    {
        try
        {
            Properties properties = new Properties();
            URL url = PureModelVersion.class.getResource("/" + name);
            try (InputStream stream = url.openStream())
            {
                properties.load(stream);
                String versionAsString = properties.getProperty("version");
                return (versionAsString == null) ? Optional.of("0") : Optional.of(versionAsString);
            }
        }
        catch (Exception ignore)
        {
            return Optional.of("0");
        }
    }
}
