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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.regex.Pattern;

public class SearchTools
{
    private SearchTools()
    {
        // utility class
    }

    public static RichIterable<CoreInstance> findInPackages(String name, Iterable<? extends Package> packages)
    {
        return find(name, PackageTreeIterable.newPackageTreeIterable(packages));
    }

    public static RichIterable<CoreInstance> findInAllPackages(String name, ModelRepository repository)
    {
        return find(name, PackageTreeIterable.newRootPackageTreeIterable(repository));
    }

    public static RichIterable<CoreInstance> findInPackages(Pattern pattern, Iterable<? extends Package> packages)
    {
        return find(pattern, PackageTreeIterable.newPackageTreeIterable(packages));
    }

    public static RichIterable<CoreInstance> findInAllPackages(Pattern pattern, ModelRepository repository)
    {
        return find(pattern, PackageTreeIterable.newRootPackageTreeIterable(repository));
    }

    public static RichIterable<CoreInstance> find(String name, PackageTreeIterable packageIterable)
    {
        MutableList<CoreInstance> toReturn = Lists.mutable.empty();
        for (Package pkg : packageIterable)
        {
            CoreInstance element = _Package.findInPackage(pkg, name);
            if (element != null)
            {
                toReturn.add(element);
            }
        }
        return toReturn;
    }

    public static RichIterable<CoreInstance> find(Pattern pattern, PackageTreeIterable packageIterable)
    {
        MutableList<CoreInstance> toReturn = Lists.mutable.empty();
        for (Package pkg : packageIterable)
        {
            for (CoreInstance child : pkg._children())
            {
                if (pattern.matcher(child.getName()).matches())
                {
                    toReturn.add(child);
                }
            }
        }
        return toReturn;
    }
}
