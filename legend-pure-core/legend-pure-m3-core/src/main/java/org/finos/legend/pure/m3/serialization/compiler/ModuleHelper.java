// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler;

import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class ModuleHelper
{
    static final String ROOT_MODULE_NAME = "root";

    public static String resolveModuleName(String moduleName)
    {
        return (moduleName == null) ? ROOT_MODULE_NAME : moduleName;
    }

    public static boolean isRootModule(String moduleName)
    {
        return ROOT_MODULE_NAME.equals(moduleName);
    }

    public static boolean isNonRootModule(String moduleName)
    {
        return (moduleName != null) && !isRootModule(moduleName);
    }

    public static String getElementModule(CoreInstance element)
    {
        return getSourceModule(element.getSourceInformation());
    }

    public static String getSourceModule(Source source)
    {
        return (source == null) ? null : getSourceModule(source.getId());
    }

    public static String getSourceModule(SourceInformation sourceInfo)
    {
        return (sourceInfo == null) ? null : getSourceModule(sourceInfo.getSourceId());
    }

    public static String getSourceModule(String sourceId)
    {
        if (sourceId == null)
        {
            return null;
        }
        String module = CompositeCodeStorage.getSourceRepoName(sourceId);
        return (module == null) ? ROOT_MODULE_NAME : module;
    }

    public static boolean isElementInModule(CoreInstance element, String moduleName)
    {
        return isSourceInModule(element.getSourceInformation(), moduleName);
    }

    public static boolean isSourceInModule(Source source, String moduleName)
    {
        return (source != null) && isSourceInModule(source.getId(), moduleName);
    }

    public static boolean isSourceInModule(SourceInformation sourceInfo, String moduleName)
    {
        return (sourceInfo != null) && isSourceInModule(sourceInfo.getSourceId(), moduleName);
    }

    public static boolean isSourceInModule(String sourceId, String moduleName)
    {
        return CompositeCodeStorage.isSourceInRepository(sourceId, ROOT_MODULE_NAME.equals(moduleName) ? null : moduleName);
    }
}
