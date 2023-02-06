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

package org.finos.legend.pure.m3.serialization.filesystem.repository;

import java.util.regex.Pattern;

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;

public final class AppCodeRepository extends SVNCodeRepository
{
    private static final String REPO_NAME_PREFIX = "app";

    private final ImmutableSet<String> visibleRepoNames;

    AppCodeRepository(String appName, String appPackageName, String appGroup, Iterable<String> visibleRepos)
    {
        super(getRepositoryName(appGroup, appName), getPattern(appGroup, appPackageName));
        if (!isValidRepositoryName(appGroup))
        {
            throw new IllegalArgumentException("Invalid app group: " + appGroup);
        }
        this.visibleRepoNames = (visibleRepos == null) ? Sets.immutable.<String>empty() : Sets.immutable.withAll(visibleRepos);
    }

    AppCodeRepository(String appName, String appGroup, Iterable<String> visibleRepos)
    {
        this(appName, appName, appGroup, visibleRepos);
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        return (this == other) ||
                (other instanceof ModelCodeRepository) ||
                (other instanceof SystemCodeRepository) ||
                CodeRepositoryProviderHelper.isCoreRepository(other) ||
                (other != null && other.getName().startsWith("platform")) ||
                this.visibleRepoNames.contains(other.getName());
    }

    private static String getRepositoryName(String appGroup, String appName)
    {
        return REPO_NAME_PREFIX + "_" + appGroup + "_" + appName;
    }

    private static Pattern getPattern(String appGroup, String appPackageName)
    {
        return Pattern.compile("apps::" + appGroup + "::" + appPackageName + "(::.*)?");
    }
}
