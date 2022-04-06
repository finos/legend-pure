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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.serialization.runtime.Source;

import java.util.SortedMap;
import java.util.TreeMap;

class ReposWithBadDependencies
{
    // TEMPORARY WORKAROUND FOR BAD COMPILE DEPENDENCIES - NEED TO CLEAN UP ASSOCIATIONS
    static SortedMap<String, ? extends RichIterable<? extends Source>> combineReposWithBadDependencies(SortedMap<String, ? extends RichIterable<? extends Source>> compiledSourcesByRepo)
    {
        if (Iterate.noneSatisfy(compiledSourcesByRepo.keySet(), r -> "model_legacy".equals(r) || "model_candidate".equals(r)))
        {
            return compiledSourcesByRepo;
        }

        SortedMap<String, RichIterable<? extends Source>> newMap = new TreeMap<>(compiledSourcesByRepo.comparator());
        MutableList<Source> combinedModelSources = Lists.mutable.empty();
        compiledSourcesByRepo.forEach((repo, sources) ->
        {
            switch (repo)
            {
                case "model":
                case "model_legacy":
                case "model_candidate":
                {
                    combinedModelSources.addAllIterable(sources);
                    break;
                }
                default:
                {
                    newMap.put(repo, sources);
                }
            }
        });
        if (combinedModelSources.notEmpty())
        {
            newMap.put("model", combinedModelSources);
        }
        return newMap;
    }
}
