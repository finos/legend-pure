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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.sorted.mutable.TreeSortedMap;
import org.finos.legend.pure.m3.serialization.runtime.Source;

import java.util.SortedMap;

public class ReposWithBadDependencies
{
    public static SortedMap<String, RichIterable<? extends Source>> combineReposWithBadDependencies(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo)
    {
        TreeSortedMap<String, RichIterable<? extends Source>> newMap = new TreeSortedMap<>(compiledSourcesByRepo.comparator());
        if (compiledSourcesByRepo.size() > 0)
        {
            //START TEMPORARY WORKAROUND FOR BAD COMPILE DEPENDENCIES - NEED TO CLEAN UP ASSOCIATIONS
            ListIterable reposToCombine = Lists.mutable.of("model", "model_legacy", "model_candidate");
            for (String group : compiledSourcesByRepo.keySet())
            {
                if (reposToCombine.contains(group))
                {
                    RichIterable<? extends Source> vals = newMap.get("model");
                    if (vals == null || vals.isEmpty())
                    {
                        newMap.put("model", compiledSourcesByRepo.get(group));
                    }
                    else
                    {
                        newMap.put("model", Lists.mutable.<Source>withAll(vals).withAll(compiledSourcesByRepo.get(group)));
                    }
                }
                else
                {
                    newMap.put(group, compiledSourcesByRepo.get(group));
                }
            }
            //END TEMPORARY WORKAROUND FOR BAD COMPILE DEPENDENCIES
        }
        return newMap;
    }
}
