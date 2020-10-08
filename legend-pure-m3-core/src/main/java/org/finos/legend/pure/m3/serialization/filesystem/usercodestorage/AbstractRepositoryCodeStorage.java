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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage;

public abstract class AbstractRepositoryCodeStorage implements RepositoryCodeStorage
{
    protected abstract boolean hasRepo(String repoName);

    protected static String relativizeToRepo(String repoName, String path)
    {
        return relativizeToRepo(repoName, path, path.startsWith(CodeStorage.ROOT_PATH) ? CodeStorage.ROOT_PATH.length() : 0);
    }

    protected static String relativizeToRepo(String repoName, String path, int startIndex)
    {
        if (!path.startsWith(repoName, startIndex))
        {
            throw new IllegalArgumentException("Invalid path for /" + repoName + ": " + path);
        }
        int index = repoName.length() + startIndex;
        if (path.length() <= index)
        {
            return "";
        }
        if (path.charAt(index) != '/')
        {
            throw new IllegalArgumentException("Invalid path for /" + repoName + ": " + path);
        }
        return path.substring(index + 1);
    }
}
