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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class StoreValidator implements MatchRunner<Store>
{
    @Override
    public String getClassName()
    {
        return M2MappingPaths.Store;
    }

    @Override
    public void run(Store store, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        validateInclusionHierarchy(store, Lists.mutable.<Store>empty());
    }

    private static void validateInclusionHierarchy(Store store, MutableList<Store> visited)
    {
        ListIterable<? extends Store> includedStores = ListHelper.wrapListIterable(store._includes());
        if (includedStores.notEmpty())
        {
            MutableSet<CoreInstance> includesSet = UnifiedSet.newSet(includedStores.size());
            visited.add(store);
            for (Store includedStore : includedStores)
            {
                // Validate that a single store is not directly included more than once
                if (!includesSet.add(includedStore))
                {
                    StringBuilder message = new StringBuilder();
                    PackageableElement.writeUserPathForPackageableElement(message, includedStore);
                    message.append(" is included multiple times in ");
                    PackageableElement.writeUserPathForPackageableElement(message, store);
                    throw new PureCompilationException(store.getSourceInformation(), message.toString());
                }

                // Validate that there are no include loops
                if (visited.contains(includedStore))
                {
                    CoreInstance rootStore = visited.getFirst();
                    StringBuilder message = new StringBuilder("Circular include in ");
                    PackageableElement.writeUserPathForPackageableElement(message, rootStore);
                    message.append(": ");
                    for (Store visitedStore : visited)
                    {
                        PackageableElement.writeUserPathForPackageableElement(message, visitedStore);
                        message.append(" -> ");
                    }
                    PackageableElement.writeUserPathForPackageableElement(message, includedStore);
                    throw new PureCompilationException(rootStore.getSourceInformation(), message.toString());
                }

                // Recurse
                validateInclusionHierarchy(includedStore, visited);
            }
            visited.remove(visited.size() - 1);
        }
    }
}
