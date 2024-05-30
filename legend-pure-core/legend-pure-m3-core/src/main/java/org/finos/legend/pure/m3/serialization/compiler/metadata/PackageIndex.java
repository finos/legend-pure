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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.tools.GraphTools;

import java.util.function.Consumer;

class PackageIndex
{
    private final ImmutableList<ConcreteElementMetadata> topLevelElements;
    private final MapIterable<String, PackageInfo> packages;

    private PackageIndex(ImmutableList<ConcreteElementMetadata> topLevelElements, MapIterable<String, PackageInfo> packages)
    {
        this.topLevelElements = topLevelElements;
        this.packages = packages;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof PackageIndex))
        {
            return false;
        }

        PackageIndex that = (PackageIndex) other;
        return this.topLevelElements.equals(that.topLevelElements) && this.packages.equals(that.packages);
    }

    @Override
    public int hashCode()
    {
        return this.topLevelElements.hashCode() + (37 * this.packages.hashCode());
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName());
        this.topLevelElements.asLazy().collect(PackageableElementMetadata::getPath).appendString(builder, " topLevel=[", ", ", "]");
        this.packages.keysView().appendString(builder, " packages=[", ", ", "]>");
        return builder.toString();
    }

    ImmutableList<ConcreteElementMetadata> getTopLevelElements()
    {
        return this.topLevelElements;
    }

    boolean hasPackage(String packagePath)
    {
        return this.packages.containsKey(packagePath);
    }

    RichIterable<String> getAllPackagePaths()
    {
        return this.packages.keysView();
    }

    RichIterable<PackageableElementMetadata> getAllPackageMetadata()
    {
        return this.packages.valuesView().collect(PackageInfo::getMetadata);
    }

    PackageableElementMetadata getPackageMetadata(String packagePath)
    {
        PackageInfo info = this.packages.get(packagePath);
        return (info == null) ? null : info.getMetadata();
    }

    ImmutableList<PackageableElementMetadata> getPackageChildren(String packagePath)
    {
        PackageInfo info = this.packages.get(packagePath);
        return (info == null) ? null : info.getChildren();
    }

    void forEachPackage(Consumer<? super PackageableElementMetadata> consumer)
    {
        this.packages.forEachValue(pi -> consumer.accept(pi.getMetadata()));
    }

    void forEachVirtualPackage(Consumer<? super VirtualPackageMetadata> consumer)
    {
        this.packages.forEachValue(pi ->
        {
            PackageableElementMetadata metadata = pi.getMetadata();
            if (metadata instanceof VirtualPackageMetadata)
            {
                consumer.accept((VirtualPackageMetadata) metadata);
            }
        });
    }

    private static class PackageInfo
    {
        private final PackageableElementMetadata metadata;
        private final ImmutableList<PackageableElementMetadata> children;

        private PackageInfo(PackageableElementMetadata metadata, ImmutableList<PackageableElementMetadata> children)
        {
            this.metadata = metadata;
            this.children = children;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other)
            {
                return true;
            }

            if (!(other instanceof PackageInfo))
            {
                return false;
            }

            PackageInfo that = (PackageInfo) other;
            return this.metadata.equals(that.metadata) && this.children.equals(that.children);
        }

        @Override
        public int hashCode()
        {
            return this.metadata.hashCode() + (31 * this.children.hashCode());
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder("<").append(getClass().getSimpleName())
                    .append(" package=").append(this.metadata.getPath());
            this.children.asLazy().collect(PackageableElementMetadata::getPath).appendString(builder, " children=[", ", ", "]>");
            return builder.toString();
        }

        PackageableElementMetadata getMetadata()
        {
            return this.metadata;
        }

        ImmutableList<PackageableElementMetadata> getChildren()
        {
            return this.children;
        }
    }

    static PackageIndex buildIndex(ElementIndex elementIndex)
    {
        // Build initial indexes
        MutableList<ConcreteElementMetadata> topLevelElements = Lists.mutable.empty();
        MutableMap<String, MutableList<PackageableElementMetadata>> packageChildren = Maps.mutable.empty();
        elementIndex.forEachElement(element ->
        {
            String path = element.getPath();
            if (M3Paths.Package.equals(element.getClassifierPath()))
            {
                packageChildren.getIfAbsentPut(path, Lists.mutable::empty);
            }

            String pkg = getParent(path);
            if (pkg == null)
            {
                topLevelElements.add(element);
            }
            else
            {
                packageChildren.getIfAbsentPut(pkg, Lists.mutable::empty).add(element);
            }
        });

        // Fill in virtual packages
        MutableSet<String> allPackagePaths = Sets.mutable.ofInitialCapacity(packageChildren.size());
        packageChildren.forEachKey(packagePath ->
        {
            while ((packagePath != null) && allPackagePaths.add(packagePath))
            {
                packagePath = getParent(packagePath);
            }
        });
        MutableMap<String, PackageableElementMetadata> packagesByPath = Maps.mutable.ofInitialCapacity(allPackagePaths.size());
        allPackagePaths.forEach(packagePath ->
        {
            ConcreteElementMetadata existing = elementIndex.getElement(packagePath);
            if (existing == null)
            {
                VirtualPackageMetadata packageMetadata = new VirtualPackageMetadata(packagePath);
                packagesByPath.put(packagePath, packageMetadata);
                String parent = getParent(packagePath);
                if (parent != null)
                {
                    packageChildren.getIfAbsentPut(parent, Lists.mutable::empty).add(packageMetadata);
                }
            }
            else if (M3Paths.Package.equals(existing.getClassifierPath()))
            {
                packagesByPath.put(packagePath, existing);
            }
            else
            {
                StringBuilder builder = new StringBuilder("Multiple elements with path ").append(packagePath);
                builder.append(": instance of ").append(existing.getClassifierPath()).append(" at ");
                existing.getSourceInformation().appendMessage(builder);
                builder.append(" and instance of ").append(M3Paths.Package);
                throw new RuntimeException(builder.toString());
            }
        });

        // Validate and build final index
        MutableMap<String, PackageInfo> result = Maps.mutable.ofInitialCapacity(packageChildren.size());
        packageChildren.forEachKeyValue((pkg, list) -> result.put(pkg, new PackageInfo(packagesByPath.get(pkg), Lists.immutable.withAll(list.sortThisBy(PackageableElementMetadata::getPath)))));
        return new PackageIndex(topLevelElements.sortThisBy(ConcreteElementMetadata::getPath).toImmutable(), result);
    }

    private static String getParent(String path)
    {
        // Check if it's a top-level element, which has no package
        if (PackageableElement.DEFAULT_PATH_SEPARATOR.equals(path) || GraphTools.isTopLevelName(path))
        {
            return null;
        }

        int index = path.lastIndexOf(PackageableElement.DEFAULT_PATH_SEPARATOR);
        return (index == -1) ? M3Paths.Root : path.substring(0, index);
    }
}
