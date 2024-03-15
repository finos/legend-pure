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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs;

import org.eclipse.collections.impl.utility.StringIterate;

public class VersionControlledCodeStorageTools
{
    private VersionControlledCodeStorageTools()
    {
        // Static utility class
    }

    public static String stripTrunkBranchesTags(String path)
    {
        if (StringIterate.isEmpty(path))
        {
            return path;
        }

        boolean startsWithSlash = (path.charAt(0) == '/');
        int searchIndex =  startsWithSlash ? 1 : 0;
        if (path.startsWith("trunk/", searchIndex))
        {
            return path.substring(6);
        }
        if (path.startsWith("branches/", searchIndex) || path.startsWith("tags/", searchIndex))
        {
            int index1 = path.indexOf('/', searchIndex);
            int index2 = path.indexOf('/', index1 + 1);
            int slashIndex = (index2 == -1) ? index1 : index2;
            return path.substring(startsWithSlash ? slashIndex : (slashIndex + 1));
        }
        return path;
    }

}
