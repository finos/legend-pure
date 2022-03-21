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

package org.finos.legend.pure.m3.navigation._package;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.list.fixed.ArrayAdapter;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecifications;

public class _Package
{
    public static final ImmutableSet<String> SPECIAL_TYPES = PrimitiveUtilities.getPrimitiveTypeNames().newWith(M3Paths.Package);

    public static CoreInstance getByUserPath(String path, ProcessorSupport processorSupport)
    {
        return getByUserPath(PackageableElement.splitUserPath(path), processorSupport);
    }

    public static CoreInstance getByUserPath(ListIterable<String> path, ProcessorSupport processorSupport)
    {
        if ((path.size() == 1) && isTopLevelName(path.get(0)))
        {
            return processorSupport.repository_getTopLevel(path.get(0));
        }

        CoreInstance element = processorSupport.repository_getTopLevel(M3Paths.Root);
        if (element == null)
        {
            throw new RuntimeException("Cannot find " + M3Paths.Root);
        }
        for (String name : path)
        {
            element = findInPackage(element, name);
            if (element == null)
            {
                return null;
            }
        }
        return element;
    }

    public static CoreInstance getByUserPath(String[] path, ProcessorSupport processorSupport)
    {
        return getByUserPath(ArrayAdapter.adapt(path), processorSupport);
    }

    public static CoreInstance findInPackage(CoreInstance pkg, String name)
    {
        // TODO eliminate duplicate import group names, then use an id index lookup
//        return pkg.getValueInValueForMetaPropertyToMany(M3Properties.children, name);
        ListIterable<? extends CoreInstance> childrenWithName = pkg.getValueInValueForMetaPropertyToManyByIndex(M3Properties.children, IndexSpecifications.getCoreInstanceNameIndexSpec(), name);
        return childrenWithName.getFirst();
    }

    public static MutableList<String> convertM3PathToM4(String m3UserPath)
    {
        return convertM3PathToM4(PackageableElement.splitUserPath(m3UserPath));
    }

    public static MutableList<String> convertM3PathToM4(ListIterable<String> m3Path)
    {
        int size = m3Path.size();

        // Top level elements are handled differently
        if ((size == 1) && isTopLevelName(m3Path.get(0)))
        {
            return Lists.mutable.withAll(m3Path);
        }

        MutableList<String> m4Path = Lists.mutable.ofInitialCapacity((size * 2) + 1);
        m4Path.add(M3Paths.Root);
        m3Path.forEach(m3Element -> m4Path.with(M3Properties.children).with(m3Element));
        return m4Path;
    }

    public static CoreInstance findOrCreatePackageFromUserPath(String path, ModelRepository repository, ProcessorSupport processorSupport)
    {
        return findOrCreatePackageFromUserPath(PackageableElement.splitUserPath(path), repository, processorSupport);
    }

    public static CoreInstance findOrCreatePackageFromUserPath(ListIterable<String> path, ModelRepository repository, ProcessorSupport processorSupport)
    {
        return findOrCreatePackageFromUserPath(path, repository, processorSupport, true);
    }

    public static CoreInstance findOrCreateEphemeralPackageFromUserPath(ListIterable<String> path, ModelRepository repository, ProcessorSupport processorSupport)
    {
        return findOrCreatePackageFromUserPath(path, repository, processorSupport, false);
    }

    private static CoreInstance findOrCreatePackageFromUserPath(ListIterable<String> path, ModelRepository repository, ProcessorSupport processorSupport, boolean persistent)
    {
        CoreInstance parent = repository.getTopLevel(M3Paths.Root);
        if (parent == null)
        {
            throw new RuntimeException("Cannot find Root in model repository");
        }

        if (path.size() == 1 && M3Paths.Root.equals(path.get(0)))
        {
            return parent;
        }

        CoreInstance packageClass = null;
        for (String name : path)
        {
            if ("".equals(name))
            {
                throw new RuntimeException("\"\" is an invalid package name.");
            }
            synchronized (parent)
            {
                CoreInstance child = findInPackage(parent, name);
                if (child == null)
                {
                    if (packageClass == null)
                    {
                        packageClass = processorSupport.package_getByUserPath(M3Paths.Package);
                        if (packageClass == null)
                        {
                            throw new RuntimeException("Cannot find class " + M3Paths.Package);
                        }
                    }
                    child = repository.newCoreInstance(name, packageClass, null, persistent);
                    child.addKeyValue(M3PropertyPaths._package, parent);
                    child.setKeyValues(M3PropertyPaths.children, Lists.immutable.empty());
                    child.addKeyValue(M3PropertyPaths.name, persistent ? repository.newStringCoreInstance_cached(name) : repository.newStringCoreInstance(name));
                    parent.addKeyValue(M3PropertyPaths.children, child);
                }
                parent = child;
            }
        }
        return parent;
    }

    private static boolean isTopLevelName(String name)
    {
        return SPECIAL_TYPES.contains(name) || M3Paths.Root.equals(name);
    }
}
