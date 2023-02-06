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

import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.utility.StringIterate;

import java.util.regex.Pattern;

public final class ModelCodeRepository extends SVNCodeRepository
{
    private static final String REPO_NAME_PREFIX = "model";

    private final String modelName;
    private final ImmutableSet<String> visibleModels;

    ModelCodeRepository(String modelName, Iterable<String> visibleModels)
    {
        super(getRepositoryName(modelName), Pattern.compile("(model"+("".equals(modelName) ?"":"_")+modelName+"::(domain|mapping|store|producers|consumers|external)||(apps::model"+("".equals(modelName) ?"":"_")+modelName+"))(::.*)?"));
        this.modelName = modelName;
        this.visibleModels = Sets.immutable.withAll(visibleModels);
    }

    public String getModelName()
    {
        return this.modelName;
    }

    @Override
    public boolean isVisible(CodeRepository other)
    {
        if (this == other)
        {
            return true;
        }

        if ((other != null && other.getName().startsWith("platform")) || (other instanceof SystemCodeRepository) || CodeRepositoryProviderHelper.isCoreRepository(other))
        {
            return true;
        }

        return (other instanceof ModelCodeRepository) && this.visibleModels.contains(((ModelCodeRepository)other).getModelName());
    }

    public SetIterable<String> getVisibleModels()
    {
        return this.visibleModels;
    }

    private static String getRepositoryName(String modelName)
    {
        return StringIterate.isEmpty(modelName) ? REPO_NAME_PREFIX : (REPO_NAME_PREFIX + "_" + modelName);
    }
}
