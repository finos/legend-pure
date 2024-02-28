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

import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

class PureManifest extends Manifest
{
    private static final Name PURE_MANIFEST_VERSION = new Name("Pure-Manifest-Version");
    private static final Name PURE_PLATFORM_VERSION = new Name("Pure-Platform-Version");
    private static final Name PURE_MODEL_VERSION = new Name("Pure-Model-Version");
    private static final Name PURE_REPOSITORY_NAME = new Name("Pure-Repository-Name");

    private PureManifest(String platformVersion, String modelVersion, String repositoryName)
    {
        Attributes mainAttributes = getMainAttributes();
        mainAttributes.put(Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(PURE_MANIFEST_VERSION, "1.0");
        if (platformVersion != null)
        {
            mainAttributes.putValue("Created-By", "Pure " + platformVersion);
        }
        setPurePlatformVersion(platformVersion);
        setPureModelVersion(modelVersion);
        setPureRepositoryName(repositoryName);
    }

    private PureManifest(Manifest manifest)
    {
        super(manifest);
    }

    public boolean hasPurePlatformVersion()
    {
        return hasMainAttribute(PURE_PLATFORM_VERSION);
    }

    public String getPurePlatformVersion()
    {
        return getMainAttribute(PURE_PLATFORM_VERSION);
    }

    private void setPurePlatformVersion(String platformVersion)
    {
        setMainAttribute(PURE_PLATFORM_VERSION, platformVersion);
    }

    public boolean hasPureModelVersion()
    {
        return hasMainAttribute(PURE_MODEL_VERSION);
    }

    public String getPureModelVersion()
    {
        return getMainAttribute(PURE_MODEL_VERSION);
    }

    private void setPureModelVersion(String modelVersion)
    {
        setMainAttribute(PURE_MODEL_VERSION, modelVersion);
    }

    public boolean hasPureRepositoryName()
    {
        return hasMainAttribute(PURE_REPOSITORY_NAME);
    }

    public String getPureRepositoryName()
    {
        return getMainAttribute(PURE_REPOSITORY_NAME);
    }

    private void setPureRepositoryName(String repositoryName)
    {
        setMainAttribute(PURE_REPOSITORY_NAME, repositoryName);
    }

    private boolean hasMainAttribute(Name name)
    {
        return getMainAttributes().containsKey(name);
    }

    private String getMainAttribute(Name name)
    {
        return getMainAttributes().getValue(name);
    }

    private void setMainAttribute(Name name, String value)
    {
        if (value == null)
        {
            getMainAttributes().remove(name);
        }
        else
        {
            getMainAttributes().put(name, value);
        }
    }

    public static boolean isPureManifest(Manifest manifest)
    {
        Attributes mainAttributes = manifest.getMainAttributes();
        return mainAttributes.containsKey(PURE_MANIFEST_VERSION);
    }

    public static PureManifest create(String platformVersion, String modelVersion, String repositoryName)
    {
        return new PureManifest(platformVersion, modelVersion, repositoryName);
    }

    public static PureManifest create(Manifest manifest)
    {
        if ((manifest == null) || !isPureManifest(manifest))
        {
            throw new IllegalArgumentException("Invalid Pure manifest: " + manifest);
        }
        return new PureManifest(manifest);
    }
}
